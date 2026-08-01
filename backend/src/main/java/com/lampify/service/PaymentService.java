package com.lampify.service;

import com.lampify.dto.PaymentIntentResponse;
import com.lampify.dto.PaymentResultResponse;
import com.lampify.dto.SandboxPaymentRequest;
import com.lampify.entity.Order;
import com.lampify.entity.OrderStatus;
import com.lampify.entity.PaymentTransaction;
import com.lampify.entity.PaymentTransactionStatus;
import com.lampify.messaging.PaymentEventPublisher;
import com.lampify.messaging.PaymentStatusEvent;
import com.lampify.repository.OrderRepository;
import com.lampify.repository.PaymentTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${app.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderService orderService,
            PaymentEventPublisher paymentEventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderService = orderService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @PostConstruct
    void initStripe() {
        if (isStripeConfigured()) {
            Stripe.apiKey = stripeSecretKey.trim();
            log.info("Stripe payment provider enabled (use test keys / sandbox)");
        } else {
            log.info("Stripe keys not set — payment sandbox simulation is active");
        }
    }

    public boolean isStripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank()
                && stripePublishableKey != null && !stripePublishableKey.isBlank();
    }

    @Transactional
    public PaymentIntentResponse createPaymentIntent(String orderNumber) {
        Order order = requirePayableOrder(orderNumber);

        if (isStripeConfigured() && "STRIPE".equals(order.getPaymentMethod().name())) {
            return createStripeIntent(order);
        }

        String providerId = "sandbox_" + UUID.randomUUID().toString().replace("-", "");
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setProvider("PAYPAL".equals(order.getPaymentMethod().name()) ? "PAYPAL_SANDBOX" : "CARD_SANDBOX");
        tx.setProviderPaymentId(providerId);
        tx.setStatus(PaymentTransactionStatus.REQUIRES_PAYMENT);
        tx.setAmount(order.getTotalAmount());
        tx.setCurrency("EUR");
        paymentTransactionRepository.save(tx);

        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setOrderNumber(order.getOrderNumber());
        response.setMode("sandbox");
        response.setProviderPaymentId(providerId);
        response.setAmount(order.getTotalAmount());
        response.setCurrency("EUR");
        response.setPaymentMethod(order.getPaymentMethod().name());
        return response;
    }

    @Transactional
    public PaymentResultResponse confirmSandboxPayment(SandboxPaymentRequest request) {
        String scenario = request.getScenario().trim().toLowerCase(Locale.ROOT);
        Order order = requirePayableOrder(request.getOrderNumber());

        PaymentTransaction tx = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    PaymentTransaction created = new PaymentTransaction();
                    created.setOrder(order);
                    created.setProvider("CARD_SANDBOX");
                    created.setProviderPaymentId("sandbox_" + UUID.randomUUID().toString().replace("-", ""));
                    created.setStatus(PaymentTransactionStatus.REQUIRES_PAYMENT);
                    created.setAmount(order.getTotalAmount());
                    created.setCurrency("EUR");
                    return paymentTransactionRepository.save(created);
                });

        return switch (scenario) {
            case "success" -> completeSuccess(tx, order, "Sandbox payment succeeded");
            case "insufficient_funds" -> completeFailure(
                    tx, order, "insufficient_funds", "Insufficient funds", PaymentTransactionStatus.FAILED);
            case "invalid_card" -> completeFailure(
                    tx, order, "invalid_card_number", "Invalid card number", PaymentTransactionStatus.FAILED);
            case "expired_card" -> completeFailure(
                    tx, order, "expired_card", "Expired card", PaymentTransactionStatus.FAILED);
            case "timeout" -> completeFailure(
                    tx, order, "payment_gateway_timeout", "Payment gateway timeout", PaymentTransactionStatus.TIMED_OUT);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown payment scenario. Use success, insufficient_funds, invalid_card, expired_card, or timeout");
        };
    }

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        if (!isStripeConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe is not configured");
        }

        Event event;
        try {
            if (stripeWebhookSecret != null && !stripeWebhookSecret.isBlank()) {
                event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret.trim());
            } else {
                event = com.stripe.net.ApiResource.GSON.fromJson(payload, Event.class);
                log.warn("Stripe webhook accepted without signature verification (webhook secret unset)");
            }
        } catch (SignatureVerificationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        }

        if (!"payment_intent.succeeded".equals(event.getType())
                && !"payment_intent.payment_failed".equals(event.getType())) {
            return;
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        PaymentIntent intent = null;
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            intent = paymentIntent;
        } else {
            String raw = event.getDataObjectDeserializer().getRawJson();
            if (raw != null) {
                intent = com.stripe.net.ApiResource.GSON.fromJson(raw, PaymentIntent.class);
            }
        }

        if (intent == null) {
            log.warn("Stripe webhook {} missing PaymentIntent payload", event.getType());
            return;
        }

        applyStripePaymentIntent(intent, event.getType());
    }

    @Transactional
    public PaymentResultResponse syncStripePayment(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        PaymentTransaction tx = paymentTransactionRepository.findByOrderIdOrderByCreatedAtDesc(order.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No payment session found"));

        if (!isStripeConfigured() || tx.getProviderPaymentId() == null || !tx.getProviderPaymentId().startsWith("pi_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a Stripe payment session");
        }

        try {
            PaymentIntent intent = PaymentIntent.retrieve(tx.getProviderPaymentId());
            applyStripePaymentIntent(intent, null);
            return buildResultFromOrder(orderNumber, tx.getProviderPaymentId());
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe sync failed: " + exception.getMessage());
        }
    }

    private PaymentIntentResponse createStripeIntent(Order order) {
        try {
            long amountCents = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency("eur")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .setDescription("ESTValgus order " + order.getOrderNumber())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            PaymentTransaction tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setProvider("STRIPE");
            tx.setProviderPaymentId(intent.getId());
            tx.setStatus(PaymentTransactionStatus.REQUIRES_PAYMENT);
            tx.setAmount(order.getTotalAmount());
            tx.setCurrency("EUR");
            paymentTransactionRepository.save(tx);

            PaymentIntentResponse response = new PaymentIntentResponse();
            response.setOrderNumber(order.getOrderNumber());
            response.setMode("stripe");
            response.setPublishableKey(stripePublishableKey.trim());
            response.setClientSecret(intent.getClientSecret());
            response.setProviderPaymentId(intent.getId());
            response.setAmount(order.getTotalAmount());
            response.setCurrency("EUR");
            response.setPaymentMethod(order.getPaymentMethod().name());
            return response;
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not create Stripe payment: " + exception.getMessage());
        }
    }

    private void applyStripePaymentIntent(PaymentIntent intent, String eventType) {
        String orderNumber = intent.getMetadata() != null ? intent.getMetadata().get("orderNumber") : null;
        PaymentTransaction tx = paymentTransactionRepository.findByProviderPaymentId(intent.getId()).orElse(null);

        if (orderNumber == null && tx != null) {
            orderNumber = tx.getOrder().getOrderNumber();
        }
        if (orderNumber == null) {
            log.warn("Stripe payment intent {} has no order metadata", intent.getId());
            return;
        }

        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            return;
        }

        if (tx == null) {
            tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setProvider("STRIPE");
            tx.setProviderPaymentId(intent.getId());
            tx.setAmount(order.getTotalAmount());
            tx.setCurrency("EUR");
        }

        boolean succeeded = "succeeded".equals(intent.getStatus())
                || "payment_intent.succeeded".equals(eventType);
        boolean failed = "payment_intent.payment_failed".equals(eventType)
                || "canceled".equals(intent.getStatus());

        if (succeeded) {
            completeSuccess(tx, order, "Stripe payment succeeded");
            return;
        }

        if (failed) {
            String code = "card_declined";
            String message = "Payment failed";
            if (intent.getLastPaymentError() != null) {
                if (intent.getLastPaymentError().getCode() != null) {
                    code = mapStripeFailureCode(intent.getLastPaymentError().getCode());
                }
                if (intent.getLastPaymentError().getMessage() != null) {
                    message = intent.getLastPaymentError().getMessage();
                }
            }
            PaymentTransactionStatus txStatus =
                    "payment_gateway_timeout".equals(code)
                            ? PaymentTransactionStatus.TIMED_OUT
                            : PaymentTransactionStatus.FAILED;
            completeFailure(tx, order, code, message, txStatus);
        }
    }

    private String mapStripeFailureCode(String stripeCode) {
        if (stripeCode == null) {
            return "card_declined";
        }
        return switch (stripeCode) {
            case "insufficient_funds" -> "insufficient_funds";
            case "expired_card" -> "expired_card";
            case "incorrect_number", "invalid_number" -> "invalid_card_number";
            case "processing_error" -> "payment_gateway_timeout";
            default -> stripeCode;
        };
    }

    private PaymentResultResponse completeSuccess(PaymentTransaction tx, Order order, String note) {
        tx.setStatus(PaymentTransactionStatus.SUCCEEDED);
        tx.setFailureCode(null);
        tx.setFailureMessage(null);
        paymentTransactionRepository.save(tx);
        var orderDto = orderService.markPaymentSucceeded(order.getOrderNumber(), note);

        paymentEventPublisher.publish(new PaymentStatusEvent(
                order.getOrderNumber(),
                order.getEmail(),
                true,
                orderDto.getStatus(),
                PaymentTransactionStatus.SUCCEEDED.name(),
                null,
                "Payment succeeded",
                order.getTotalAmount()));

        PaymentResultResponse response = new PaymentResultResponse();
        response.setSuccess(true);
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderStatus(orderDto.getStatus());
        response.setPaymentStatus(PaymentTransactionStatus.SUCCEEDED.name());
        response.setMessage("Payment succeeded");
        response.setOrder(orderDto);
        return response;
    }

    private PaymentResultResponse completeFailure(
            PaymentTransaction tx,
            Order order,
            String code,
            String message,
            PaymentTransactionStatus txStatus) {
        tx.setStatus(txStatus);
        tx.setFailureCode(code);
        tx.setFailureMessage(message);
        paymentTransactionRepository.save(tx);
        var orderDto = orderService.markPaymentFailed(order.getOrderNumber(), code, message);
        String userMessage = userFacingFailureMessage(code, message);

        paymentEventPublisher.publish(new PaymentStatusEvent(
                order.getOrderNumber(),
                order.getEmail(),
                false,
                orderDto.getStatus(),
                txStatus.name(),
                code,
                userMessage,
                order.getTotalAmount()));

        PaymentResultResponse response = new PaymentResultResponse();
        response.setSuccess(false);
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderStatus(orderDto.getStatus());
        response.setPaymentStatus(txStatus.name());
        response.setFailureCode(code);
        response.setMessage(userMessage);
        response.setOrder(orderDto);
        return response;
    }

    private PaymentResultResponse buildResultFromOrder(String orderNumber, String providerPaymentId) {
        var orderDto = orderService.getOrderByNumber(orderNumber);
        PaymentTransaction tx = paymentTransactionRepository.findByProviderPaymentId(providerPaymentId).orElse(null);
        PaymentResultResponse response = new PaymentResultResponse();
        response.setOrderNumber(orderNumber);
        response.setOrderStatus(orderDto.getStatus());
        response.setSuccess("PAID".equals(orderDto.getStatus()));
        response.setPaymentStatus(tx != null ? tx.getStatus().name() : "UNKNOWN");
        response.setFailureCode(tx != null ? tx.getFailureCode() : null);
        response.setMessage(response.isSuccess() ? "Payment succeeded" : "Payment not completed");
        response.setOrder(orderDto);
        return response;
    }

    private String userFacingFailureMessage(String code, String fallback) {
        return switch (code != null ? code : "") {
            case "insufficient_funds" -> "Insufficient funds";
            case "invalid_card_number", "invalid_card" -> "Invalid card number";
            case "expired_card" -> "Expired card";
            case "payment_gateway_timeout" -> "Payment gateway timeout";
            default -> fallback != null ? fallback : "Payment failed";
        };
    }

    private Order requirePayableOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already paid");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is not awaiting payment (status=" + order.getStatus() + ")");
        }
        return order;
    }
}
