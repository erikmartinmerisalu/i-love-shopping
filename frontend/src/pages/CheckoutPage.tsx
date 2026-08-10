import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { CheckoutApiError, placeOrder, type OrderDto } from '../api/orders';
import type { PaymentResultResponse } from '../api/payments';
import PaymentStep from '../components/PaymentStep';
import {
  validateCheckoutForm,
  type CheckoutFieldErrors,
  type CheckoutFormValues,
} from '../utils/checkoutValidation';

const PAYMENT_OPTIONS = [
  { value: 'STRIPE', label: 'Stripe (test keys or sandbox fallback)' },
  { value: 'PAYPAL', label: 'PayPal sandbox simulation' },
  { value: 'CARD', label: 'Card (secure form — no card data stored)' },
] as const;

const emptyForm = (): CheckoutFormValues => ({
  fullName: '',
  email: '',
  phone: '',
  addressLine1: '',
  addressLine2: '',
  city: '',
  postalCode: '',
  country: '',
  paymentMethod: 'CARD',
});

const fieldClass = (hasError: boolean) =>
  `w-full rounded-lg border bg-gray-950 px-3 py-2 text-sm outline-none focus:ring-2 ${
    hasError
      ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20'
      : 'border-gray-700 focus:border-sky-400 focus:ring-sky-400/20'
  }`;

const CheckoutPage = () => {
  const navigate = useNavigate();
  const { cartItems, totalPrice, totalItems, refreshCart } = useCart();
  const { user, token, isGuest, isAuthenticated } = useAuth();

  const [form, setForm] = useState<CheckoutFormValues>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<CheckoutFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [placedOrder, setPlacedOrder] = useState<OrderDto | null>(null);
  const [paymentResult, setPaymentResult] = useState<PaymentResultResponse | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [paymentSessionKey, setPaymentSessionKey] = useState(0);

  const handlePaymentSuccess = (result: PaymentResultResponse) => {
    setPaymentError(null);
    setPaymentResult(result);
  };

  const handlePaymentFailed = (result: PaymentResultResponse) => {
    const detail = result.failureCode ? `${result.message} (${result.failureCode})` : result.message;
    setPaymentError(detail || 'Payment could not be completed. Please try again.');
    setPaymentSessionKey((current) => current + 1);
  };

  useEffect(() => {
    setForm((current) => ({
      ...current,
      email: isAuthenticated && user?.email ? user.email : current.email,
      fullName:
        isAuthenticated && user?.username && !current.fullName
          ? user.username
          : current.fullName,
    }));
  }, [isAuthenticated, user?.email, user?.username]);

  const updateField = <K extends keyof CheckoutFormValues>(key: K, value: CheckoutFormValues[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setFieldErrors((current) => {
      if (!current[key]) {
        return current;
      }
      const next = { ...current };
      delete next[key];
      return next;
    });
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (cartItems.length === 0) {
      setFormError('Your cart is empty');
      return;
    }

    const clientErrors = validateCheckoutForm(form);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setFormError('Please fix the highlighted fields');
      return;
    }

    setSubmitting(true);
    try {
      const order = await placeOrder(token, {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        addressLine1: form.addressLine1.trim(),
        addressLine2: form.addressLine2.trim() || undefined,
        city: form.city.trim(),
        postalCode: form.postalCode.trim(),
        country: form.country.trim(),
        paymentMethod: form.paymentMethod,
      });
      setPlacedOrder(order);
      setFieldErrors({});
      await refreshCart();
    } catch (error) {
      if (error instanceof CheckoutApiError) {
        setFieldErrors(error.fieldErrors);
        setFormError(error.message);
      } else {
        setFormError('Network error. Please check your connection and try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (paymentResult?.success) {
    return (
      <div className="min-h-screen bg-gray-950 text-white">
        <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
          <h1 className="text-xl font-bold tracking-wide">Payment successful</h1>
          <button
            onClick={() => navigate('/products')}
            className="text-sm text-sky-300 hover:text-sky-200"
          >
            Continue shopping
          </button>
        </header>
        <main className="max-w-2xl mx-auto px-6 py-10 space-y-6">
          <div className="rounded-xl border border-green-500/40 bg-green-500/10 p-5 text-green-100">
            <p className="font-semibold text-lg">{paymentResult.message}</p>
            <p className="mt-2 text-sm">
              Order <span className="font-mono">{paymentResult.orderNumber}</span> ·{' '}
              {paymentResult.orderStatus.replaceAll('_', ' ')}
            </p>
            {paymentResult.order?.email && (
              <p className="mt-2 text-sm">
                A confirmation email has been sent to{' '}
                <span className="font-medium">{paymentResult.order.email}</span>.
              </p>
            )}
          </div>
          {paymentResult.order && (
            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
              <h2 className="text-lg font-semibold">Order summary</h2>
              <ul className="space-y-2 text-sm">
                {paymentResult.order.items.map((item) => (
                  <li key={`${item.productId}-${item.productName}`} className="flex justify-between gap-4">
                    <span>
                      {item.productName} × {item.quantity}
                    </span>
                    <span className="text-primary font-semibold">
                      €{Number(item.lineTotal).toFixed(2)}
                    </span>
                  </li>
                ))}
              </ul>
              <div className="border-t border-gray-700 pt-3 flex justify-between font-bold">
                <span>Total</span>
                <span className="text-primary">
                  €{Number(paymentResult.order.totalAmount).toFixed(2)}
                </span>
              </div>
            </section>
          )}
        </main>
      </div>
    );
  }

  if (placedOrder) {
    return (
      <div className="min-h-screen bg-gray-950 text-white">
        <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
          <h1 className="text-xl font-bold tracking-wide">Pay for order</h1>
          <button
            onClick={() => navigate('/products')}
            className="text-sm text-sky-300 hover:text-sky-200"
          >
            Back to shop
          </button>
        </header>
        <main className="max-w-2xl mx-auto px-6 py-10 space-y-6">
          <div className="rounded-xl border border-sky-500/40 bg-sky-500/10 p-5 text-sky-100 text-sm">
            Order <span className="font-mono font-semibold">{placedOrder.orderNumber}</span> is ready
            for payment. Complete payment below — stock is reserved until payment succeeds or fails. A
            confirmation email will be sent to {placedOrder.email} once payment succeeds.
          </div>

          {paymentError && (
            <div className="rounded-xl border border-red-700 bg-red-900/40 px-4 py-3 text-sm text-red-200">
              <p className="font-semibold">Payment failed</p>
              <p className="mt-1">{paymentError}</p>
              <p className="mt-2 text-xs text-red-300/90">
                Correct your payment details and try again below — you stay on this page until payment
                succeeds.
              </p>
            </div>
          )}

          <PaymentStep
            key={paymentSessionKey}
            order={placedOrder}
            onPaid={handlePaymentSuccess}
            onPaymentFailed={handlePaymentFailed}
          />

          <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
            <h2 className="text-lg font-semibold">Order summary</h2>
            <ul className="space-y-2 text-sm">
              {placedOrder.items.map((item) => (
                <li key={`${item.productId}-${item.productName}`} className="flex justify-between gap-4">
                  <span>
                    {item.productName} × {item.quantity}
                  </span>
                  <span className="text-primary font-semibold">
                    €{Number(item.lineTotal).toFixed(2)}
                  </span>
                </li>
              ))}
            </ul>
            <div className="border-t border-gray-700 pt-3 flex justify-between font-bold">
              <span>Total</span>
              <span className="text-primary">€{Number(placedOrder.totalAmount).toFixed(2)}</span>
            </div>
          </section>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-white">
      <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <h1 className="text-xl font-bold tracking-wide">Checkout</h1>
        <button
          onClick={() => navigate('/products')}
          className="text-sm text-sky-300 hover:text-sky-200"
        >
          ← Back to shop
        </button>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10 grid gap-8 lg:grid-cols-[1.4fr_1fr]">
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          {formError && (
            <div className="rounded-xl border border-red-700 bg-red-900/40 px-4 py-3 text-sm text-red-200">
              {formError}
            </div>
          )}

          <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
            <h2 className="text-lg font-semibold">Contact</h2>
            {isGuest && (
              <p className="text-xs text-gray-400">
                Checking out as guest. Email is required for confirmation.
              </p>
            )}
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block space-y-1 text-sm sm:col-span-2">
                <span className="text-gray-300">Full name</span>
                <input
                  className={fieldClass(!!fieldErrors.fullName)}
                  value={form.fullName}
                  onChange={(e) => updateField('fullName', e.target.value)}
                  autoComplete="name"
                />
                {fieldErrors.fullName && (
                  <span className="text-xs text-red-400">{fieldErrors.fullName}</span>
                )}
              </label>
              <label className="block space-y-1 text-sm">
                <span className="text-gray-300">Email</span>
                <input
                  type="email"
                  className={fieldClass(!!fieldErrors.email)}
                  value={form.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  autoComplete="email"
                  readOnly={isAuthenticated && !!user?.email}
                />
                {fieldErrors.email && (
                  <span className="text-xs text-red-400">{fieldErrors.email}</span>
                )}
              </label>
              <label className="block space-y-1 text-sm">
                <span className="text-gray-300">Phone</span>
                <input
                  className={fieldClass(!!fieldErrors.phone)}
                  value={form.phone}
                  onChange={(e) => updateField('phone', e.target.value)}
                  autoComplete="tel"
                  placeholder="+372 5555 5555"
                />
                {fieldErrors.phone && (
                  <span className="text-xs text-red-400">{fieldErrors.phone}</span>
                )}
              </label>
            </div>
          </section>

          <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
            <h2 className="text-lg font-semibold">Shipping address</h2>
            <label className="block space-y-1 text-sm">
              <span className="text-gray-300">Address line 1</span>
              <input
                className={fieldClass(!!fieldErrors.addressLine1)}
                value={form.addressLine1}
                onChange={(e) => updateField('addressLine1', e.target.value)}
                autoComplete="address-line1"
              />
              {fieldErrors.addressLine1 && (
                <span className="text-xs text-red-400">{fieldErrors.addressLine1}</span>
              )}
            </label>
            <label className="block space-y-1 text-sm">
              <span className="text-gray-300">Address line 2 (optional)</span>
              <input
                className={fieldClass(!!fieldErrors.addressLine2)}
                value={form.addressLine2}
                onChange={(e) => updateField('addressLine2', e.target.value)}
                autoComplete="address-line2"
              />
              {fieldErrors.addressLine2 && (
                <span className="text-xs text-red-400">{fieldErrors.addressLine2}</span>
              )}
            </label>
            <div className="grid gap-4 sm:grid-cols-3">
              <label className="block space-y-1 text-sm">
                <span className="text-gray-300">City</span>
                <input
                  className={fieldClass(!!fieldErrors.city)}
                  value={form.city}
                  onChange={(e) => updateField('city', e.target.value)}
                  autoComplete="address-level2"
                />
                {fieldErrors.city && (
                  <span className="text-xs text-red-400">{fieldErrors.city}</span>
                )}
              </label>
              <label className="block space-y-1 text-sm">
                <span className="text-gray-300">Postal code</span>
                <input
                  className={fieldClass(!!fieldErrors.postalCode)}
                  value={form.postalCode}
                  onChange={(e) => updateField('postalCode', e.target.value)}
                  autoComplete="postal-code"
                />
                {fieldErrors.postalCode && (
                  <span className="text-xs text-red-400">{fieldErrors.postalCode}</span>
                )}
              </label>
              <label className="block space-y-1 text-sm">
                <span className="text-gray-300">Country</span>
                <input
                  className={fieldClass(!!fieldErrors.country)}
                  value={form.country}
                  onChange={(e) => updateField('country', e.target.value)}
                  autoComplete="country-name"
                  placeholder="Estonia"
                />
                {fieldErrors.country && (
                  <span className="text-xs text-red-400">{fieldErrors.country}</span>
                )}
              </label>
            </div>
          </section>

          <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
            <h2 className="text-lg font-semibold">Payment method</h2>
            <p className="text-xs text-gray-400">
              After placing the order you will complete payment on a secure form. Stripe uses
              provider Elements when keys are set. CARD uses the test-card sandbox. PayPal uses a
              separate sandbox approval flow — no card details required.
            </p>
            <div className="space-y-2">
              {PAYMENT_OPTIONS.map((option) => (
                <label
                  key={option.value}
                  className={`flex items-center gap-3 rounded-lg border px-3 py-3 text-sm cursor-pointer ${
                    form.paymentMethod === option.value
                      ? 'border-sky-500 bg-sky-500/10'
                      : 'border-gray-700 hover:border-gray-500'
                  }`}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value={option.value}
                    checked={form.paymentMethod === option.value}
                    onChange={() => updateField('paymentMethod', option.value)}
                    className="accent-sky-400"
                  />
                  {option.label}
                </label>
              ))}
            </div>
            {fieldErrors.paymentMethod && (
              <span className="text-xs text-red-400">{fieldErrors.paymentMethod}</span>
            )}
          </section>

          <button
            type="submit"
            disabled={submitting || cartItems.length === 0}
            className="w-full rounded-lg bg-primary py-3 font-semibold text-white hover:bg-primary-focus transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {submitting ? 'Placing order…' : 'Place order & continue to payment'}
          </button>
        </form>

        <aside className="rounded-xl border border-gray-800 bg-gray-900 p-6 h-fit space-y-4 lg:sticky lg:top-8">
          <h2 className="text-lg font-semibold">Order summary</h2>
          {cartItems.length === 0 ? (
            <p className="text-gray-400 text-sm">Your cart is empty.</p>
          ) : (
            <>
              <ul className="space-y-3 text-sm">
                {cartItems.map((item) => (
                  <li key={item.id} className="flex gap-3">
                    <img
                      src={item.image}
                      alt=""
                      className="h-14 w-14 rounded object-cover opacity-80"
                    />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium truncate">{item.name}</p>
                      <p className="text-gray-400">
                        €{item.price.toFixed(2)} × {item.quantity}
                      </p>
                    </div>
                    <span className="text-primary font-semibold whitespace-nowrap">
                      €{(item.price * item.quantity).toFixed(2)}
                    </span>
                  </li>
                ))}
              </ul>
              <div className="border-t border-gray-700 pt-3 flex justify-between font-bold">
                <span>
                  {totalItems} item{totalItems === 1 ? '' : 's'}
                </span>
                <span className="text-primary">€{totalPrice.toFixed(2)}</span>
              </div>
            </>
          )}
        </aside>
      </main>
    </div>
  );
};

export default CheckoutPage;
