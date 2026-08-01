import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js';
import { loadStripe, type Stripe } from '@stripe/stripe-js';
import {
  confirmSandboxPayment,
  createPaymentIntent,
  PaymentApiError,
  syncStripePayment,
  type PaymentIntentResponse,
  type PaymentResultResponse,
} from '../api/payments';
import type { OrderDto } from '../api/orders';
import {
  scenarioFromTestCard,
  validateCardFields,
  type CardFieldErrors,
} from '../utils/cardValidation';
import TestCardInfoBox from './TestCardInfoBox';

type Props = {
  order: OrderDto;
  onPaid: (result: PaymentResultResponse) => void;
  onFailed: (result: PaymentResultResponse) => void;
};

const fieldClass = (hasError: boolean) =>
  `w-full rounded-lg border bg-gray-950 px-3 py-2 text-sm outline-none focus:ring-2 ${
    hasError
      ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
      : 'border-gray-700 focus:border-sky-400 focus:ring-sky-400/20'
  }`;

const StripeCheckoutForm = ({
  orderNumber,
  onPaid,
  onFailed,
}: {
  orderNumber: string;
  onPaid: (result: PaymentResultResponse) => void;
  onFailed: (result: PaymentResultResponse) => void;
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!stripe || !elements) {
      return;
    }

    setSubmitting(true);
    setError(null);

    const { error: confirmError } = await stripe.confirmPayment({
      elements,
      redirect: 'if_required',
    });

    if (confirmError) {
      const mapped =
        confirmError.code === 'insufficient_funds'
          ? 'Insufficient funds'
          : confirmError.code === 'expired_card'
            ? 'Expired card'
            : confirmError.code === 'invalid_number' || confirmError.code === 'incorrect_number'
              ? 'Invalid card number'
              : confirmError.message || 'Payment validation failed';
      setError(mapped);
      setSubmitting(false);
      return;
    }

    try {
      const result = await syncStripePayment(orderNumber);
      if (result.success) {
        onPaid(result);
      } else {
        onFailed(result);
      }
    } catch (err) {
      setError(err instanceof PaymentApiError ? err.message : 'Could not confirm payment status');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="rounded-lg border border-gray-700 bg-white p-4">
        <PaymentElement />
      </div>
      {error && (
        <div className="rounded-lg border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      )}
      <button
        type="submit"
        disabled={!stripe || submitting}
        className="w-full rounded-lg bg-primary py-3 font-semibold text-white disabled:opacity-50"
      >
        {submitting ? 'Processing…' : 'Pay securely with Stripe'}
      </button>
    </form>
  );
};

const PaymentStep = ({ order, onPaid, onFailed }: Props) => {
  const [intent, setIntent] = useState<PaymentIntentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [stripePromise, setStripePromise] = useState<Promise<Stripe | null> | null>(null);

  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cvv, setCvv] = useState('');
  const [cardErrors, setCardErrors] = useState<CardFieldErrors>({});

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const next = await createPaymentIntent(order.orderNumber);
        if (cancelled) {
          return;
        }
        setIntent(next);
        if (next.mode === 'stripe' && next.publishableKey) {
          setStripePromise(loadStripe(next.publishableKey));
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof PaymentApiError ? err.message : 'Could not start payment');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [order.orderNumber]);

  const stripeOptions = useMemo(
    () =>
      intent?.clientSecret
        ? {
            clientSecret: intent.clientSecret,
            appearance: { theme: 'stripe' as const },
          }
        : undefined,
    [intent?.clientSecret]
  );

  const handleSandboxPay = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    const fieldErrors = validateCardFields(cardNumber, expiry, cvv);
    if (Object.keys(fieldErrors).length > 0) {
      setCardErrors(fieldErrors);
      setError('Please fix card validation errors before submitting');
      return;
    }
    setCardErrors({});

    const scenario = scenarioFromTestCard(cardNumber);
    if (!scenario) {
      setError(
        'Use a sandbox test card: 4242… success, 4000…9995 insufficient funds, 4000…0002 invalid, 4000…0069 expired, 4000…0010 timeout'
      );
      return;
    }

    setSubmitting(true);
    try {
      const result = await confirmSandboxPayment(order.orderNumber, scenario);
      if (result.success) {
        onPaid(result);
      } else {
        onFailed(result);
      }
    } catch (err) {
      setError(err instanceof PaymentApiError ? err.message : 'Payment failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="rounded-xl border border-gray-800 bg-gray-900 p-6 text-sm text-gray-300">
        Preparing secure payment…
      </div>
    );
  }

  if (!intent) {
    return (
      <div className="rounded-xl border border-red-700 bg-red-900/40 p-4 text-sm text-red-200">
        {error || 'Payment session unavailable'}
      </div>
    );
  }

  if (intent.mode === 'stripe' && stripePromise && stripeOptions) {
    return (
      <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
        <h2 className="text-lg font-semibold">Pay with Stripe</h2>
      <p className="text-sm text-gray-400">
          Card details stay in Stripe Payment Element — never posted to ESTValgus. Amount: €
          {Number(intent.amount).toFixed(2)}
        </p>
        <TestCardInfoBox />
        <Elements stripe={stripePromise} options={stripeOptions}>
          <StripeCheckoutForm
            orderNumber={order.orderNumber}
            onPaid={onPaid}
            onFailed={onFailed}
          />
        </Elements>
        <p className="text-xs text-gray-500">
          Callbacks: <code>POST /api/payments/webhook/stripe</code> · CLI:{' '}
          <code>stripe listen --forward-to localhost:8080/api/payments/webhook/stripe</code>
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
      <h2 className="text-lg font-semibold">
        {intent.paymentMethod === 'PAYPAL' ? 'PayPal sandbox' : 'Secure card form'}
      </h2>
      <p className="text-sm text-gray-400">
        Provider-style form: number, expiry, and CVV validated in the browser. Only a scenario
        token is sent to the API — card data is not stored on our servers.
      </p>

      <TestCardInfoBox />

      {error && (
        <div className="rounded-lg border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-200">
          {error}
        </div>
      )}

      <form onSubmit={handleSandboxPay} className="space-y-4" noValidate>
        <label className="block space-y-1 text-sm">
          <span className="text-gray-300">Card number</span>
          <input
            className={fieldClass(!!cardErrors.cardNumber)}
            value={cardNumber}
            onChange={(e) => setCardNumber(e.target.value)}
            inputMode="numeric"
            autoComplete="cc-number"
            placeholder="4242 4242 4242 4242"
          />
          {cardErrors.cardNumber && (
            <span className="text-xs text-red-400">{cardErrors.cardNumber}</span>
          )}
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block space-y-1 text-sm">
            <span className="text-gray-300">Expiry (MM/YY)</span>
            <input
              className={fieldClass(!!cardErrors.expiry)}
              value={expiry}
              onChange={(e) => setExpiry(e.target.value)}
              autoComplete="cc-exp"
              placeholder="12/30"
            />
            {cardErrors.expiry && (
              <span className="text-xs text-red-400">{cardErrors.expiry}</span>
            )}
          </label>
          <label className="block space-y-1 text-sm">
            <span className="text-gray-300">CVV</span>
            <input
              className={fieldClass(!!cardErrors.cvv)}
              value={cvv}
              onChange={(e) => setCvv(e.target.value)}
              autoComplete="cc-csc"
              placeholder="123"
            />
            {cardErrors.cvv && <span className="text-xs text-red-400">{cardErrors.cvv}</span>}
          </label>
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-primary py-3 font-semibold text-white hover:bg-primary-focus transition disabled:opacity-50"
        >
          {submitting ? 'Processing…' : `Pay €${Number(intent.amount).toFixed(2)} securely`}
        </button>
      </form>
    </section>
  );
};

export default PaymentStep;
