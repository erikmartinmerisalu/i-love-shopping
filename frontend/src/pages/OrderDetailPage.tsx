import { useCallback, useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  cancelOrder,
  CheckoutApiError,
  fetchOrder,
  type OrderDto,
} from '../api/orders';
import OrderItemReviews from '../components/OrderItemReviews';

const formatStatus = (status: string) => status.replaceAll('_', ' ');

const OrderDetailPage = () => {
  const { orderNumber = '' } = useParams();
  const [searchParams] = useSearchParams();
  const emailHint = searchParams.get('email') || undefined;
  const { token, isAuthenticated } = useAuth();

  const [order, setOrder] = useState<OrderDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [guestEmail, setGuestEmail] = useState(emailHint || '');

  const load = useCallback(async () => {
    if (!orderNumber) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const emailForAccess = guestEmail || emailHint;
      const data = await fetchOrder(token, orderNumber, emailForAccess);
      setOrder(data);
    } catch (err) {
      setError(err instanceof CheckoutApiError ? err.message : 'Could not load order');
      setOrder(null);
    } finally {
      setLoading(false);
    }
  }, [orderNumber, token, isAuthenticated, guestEmail, emailHint]);

  useEffect(() => {
    if (isAuthenticated || emailHint) {
      void load();
    } else {
      setLoading(false);
    }
  }, [isAuthenticated, emailHint, load]);

  useEffect(() => {
    if (order && window.location.hash === '#reviews') {
      document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [order]);

  const handleCancel = async () => {
    if (!order || !window.confirm('Cancel this unprocessed order and restore stock?')) {
      return;
    }
    setCancelling(true);
    setError(null);
    try {
      const updated = await cancelOrder(token, order.orderNumber, order.email);
      setOrder(updated);
    } catch (err) {
      setError(err instanceof CheckoutApiError ? err.message : 'Could not cancel order');
    } finally {
      setCancelling(false);
    }
  };

  return (
    <div className="page-container-form space-y-6 py-10">
      <h1 className="text-2xl font-bold">Order details</h1>

      <div className="max-w-4xl space-y-6">
        {!isAuthenticated && !emailHint && (
          <form
            className="rounded-xl border border-gray-800 bg-gray-900 p-4 space-y-3"
            onSubmit={(e) => {
              e.preventDefault();
              void load();
            }}
          >
            <p className="text-sm text-gray-300">
              Enter the email used at checkout to view this guest order.
            </p>
            <input
              type="email"
              value={guestEmail}
              onChange={(e) => setGuestEmail(e.target.value)}
              className="w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm"
              placeholder="you@example.com"
              required
            />
            <button type="submit" className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold">
              View order
            </button>
          </form>
        )}

        {error && (
          <div className="rounded-xl border border-red-700 bg-red-900/40 px-4 py-3 text-sm text-red-200">
            {error}
          </div>
        )}

        {loading ? (
          <p className="text-gray-400">Loading…</p>
        ) : order ? (
          <>
            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-mono text-sky-300">{order.orderNumber}</p>
                  <p className="mt-1 text-sm text-gray-400">
                    {order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm uppercase tracking-wide text-gray-200">{formatStatus(order.status)}</p>
                  {order.items.some((item) => item.canReview) && (
                    <a
                      href="#reviews"
                      className="inline-flex items-center justify-center rounded-lg bg-primary px-3.5 py-2 text-sm font-semibold text-white hover:bg-primary-focus"
                    >
                      Leave a review
                    </a>
                  )}
                </div>
              </div>

              <div className="mt-5 grid gap-5 border-t border-white/10 pt-5 sm:grid-cols-2">
                <div>
                  <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400">Payment</h2>
                  <p className="mt-1.5 text-sm text-gray-300">
                    {order.paymentMethod}
                    {order.deliveryOptionName ? ` · ${order.deliveryOptionName}` : ''}
                    <br />
                    Merchandise{' '}
                    <span className="font-semibold text-primary">
                      €{Number(order.totalAmount).toFixed(2)}
                    </span>
                    {order.shippingAmount != null && (
                      <>
                        {' · '}Shipping{' '}
                        <span className="font-semibold text-primary">
                          €{Number(order.shippingAmount).toFixed(2)}
                        </span>
                      </>
                    )}
                  </p>
                </div>
                <div>
                  <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400">Shipping</h2>
                  <p className="mt-1.5 text-sm leading-relaxed text-gray-300">
                    {order.fullName}
                    <br />
                    {order.addressLine1}
                    {order.addressLine2 ? `, ${order.addressLine2}` : ''}
                    <br />
                    {order.postalCode} {order.city}, {order.country}
                    <br />
                    {order.phone} · {order.email}
                  </p>
                </div>
              </div>

              {order.status === 'PENDING_PAYMENT' && (
                <button
                  type="button"
                  disabled={cancelling}
                  onClick={handleCancel}
                  className="mt-5 rounded-lg border border-red-500/50 bg-red-900/30 px-4 py-2 text-sm font-semibold text-red-200 hover:bg-red-900/50 disabled:opacity-50"
                >
                  {cancelling ? 'Cancelling…' : 'Cancel order (restore stock)'}
                </button>
              )}
            </section>

            <OrderItemReviews
              order={order}
              onReviewed={(productId, reviewStatus) => {
                setOrder((current) => {
                  if (!current) {
                    return current;
                  }
                  return {
                    ...current,
                    items: current.items.map((item) =>
                      item.productId === productId
                        ? { ...item, canReview: false, reviewStatus }
                        : item,
                    ),
                  };
                });
              }}
            />

            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
              <h2 className="text-lg font-semibold">Status updates</h2>
              {order.statusHistory.length === 0 ? (
                <p className="text-sm text-gray-400">No status history yet.</p>
              ) : (
                <ol className="space-y-3 border-l border-gray-700 pl-4">
                  {order.statusHistory.map((entry, index) => (
                    <li key={`${entry.status}-${entry.createdAt}-${index}`} className="text-sm">
                      <p className="font-semibold uppercase tracking-wide text-sky-300">
                        {formatStatus(entry.status)}
                      </p>
                      <p className="text-gray-400">
                        {entry.createdAt ? new Date(entry.createdAt).toLocaleString() : '—'}
                      </p>
                      {entry.note && <p className="text-gray-300 mt-1">{entry.note}</p>}
                    </li>
                  ))}
                </ol>
              )}
            </section>
          </>
        ) : null}
      </div>
    </div>
  );
};

export default OrderDetailPage;
