# RabbitMQ in ESTValgus — viva / reviewer notes

## What RabbitMQ is (one sentence)

RabbitMQ is a **message broker**: one part of the app publishes an event, another
part consumes it later — they do not call each other directly.

## Why we use it here

After a payment succeeds or fails, we must:

1. Update order / payment status in the database (must stay in the payment flow)
2. Send an email to the customer (can be slow or fail if SMTP is down)

If the payment API waited for email, a slow mail server would delay the checkout
response. Instead:

- Payment code **publishes** a small `PaymentStatusEvent` to RabbitMQ
- A **listener** reads the queue and sends success/failure email via `EmailService`

So checkout stays fast, and email is **decoupled** (and can retry independently).

## How it works in this project (step by step)

```
[PaymentService]
   completeSuccess / completeFailure
        │
        │  PaymentEventPublisher.publish(event)
        ▼
[RabbitMQ]  exchange: payments.exchange
            routing key: payment.status
            queue: payments.notifications
        │
        │  @RabbitListener
        ▼
[PaymentNotificationListener]
        │
        ▼
[EmailService]  same MAIL_* provider as forgot-password
                → payment success or payment failed email
```

### Key classes

| Piece | Role |
|-------|------|
| `PaymentEventPublisher` | Puts JSON event on the exchange after payment outcome |
| `RabbitConfig` | Declares exchange, queue, binding |
| `PaymentNotificationListener` | Consumes queue → calls `EmailService` |
| `PaymentStatusEvent` | Payload: order number, email, success flag, message, amount |

### Docker

`docker-compose.yml` runs a `rabbitmq` service. Backend connects with:

- `SPRING_RABBITMQ_HOST=rabbitmq` (in Compose)
- Management UI: http://localhost:15672 (user/pass `guest`/`guest` by default)

Toggle: `APP_MESSAGING_ENABLED=false` skips publish/listen (useful for local Maven without Rabbit).

## What to say if asked “what does it do?”

> “When payment finishes, we publish a status event to RabbitMQ. A separate
> consumer listens on a queue and sends the success or failure email. That way
> the payment API does not depend on SMTP being fast or available. Email uses
> the same EmailService and MAIL settings as password reset.”

## What to say if asked “how does it do it?”

> “Spring AMQP `RabbitTemplate.convertAndSend` publishes to a topic exchange
> with routing key `payment.status`. The queue `payments.notifications` is bound
> to that key. `@RabbitListener` on `PaymentNotificationListener` receives the
> event and calls `sendPaymentSuccessEmail` or `sendPaymentFailedEmail`.”

## Demo tips

1. Place order → pay with success test card
2. Watch backend logs: “Published payment status…” then “Received payment status…”
3. Optional: open http://localhost:15672 → Queues → `payments.notifications`
4. If SMTP is unset, email body is logged (same as forgot-password)
