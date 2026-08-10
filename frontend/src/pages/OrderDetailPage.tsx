import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  cancelOrder,
  CheckoutApiError,
  fetchOrder,
  type OrderDto,
} from '../api/orders';

const formatStatus = (status: string) => status.replaceAll('_', ' ');

const OrderDetailPage = () => {
  const { orderNumber = '' } = useParams();
  const [searchParams] = useSearchParams();
  const emailHint = searchParams.get('email') || undefined;
  const navigate = useNavigate();
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
    <div className="min-h-screen bg-gray-950 text-white">
      <header className="bg-gray-900 border-b border-gray-800 px-6 py-4 flex items-center justify-between">
        <h1 className="text-xl font-bold tracking-wide">Order details</h1>
        <div className="flex gap-3 text-sm">
          <button onClick={() => navigate('/orders')} className="text-sky-300 hover:text-sky-200">
            All orders
          </button>
          <button onClick={() => navigate('/products')} className="text-sky-300 hover:text-sky-200">
            Shop
          </button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-6 py-8 space-y-6">
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
            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
              <div className="flex flex-wrap justify-between gap-2">
                <div>
                  <p className="font-mono text-sky-300">{order.orderNumber}</p>
                  <p className="text-sm text-gray-400 mt-1">
                    {order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}
                  </p>
                </div>
                <p className="text-sm uppercase tracking-wide">{formatStatus(order.status)}</p>
              </div>
              <p className="text-sm text-gray-300">
                Payment: {order.paymentMethod} · Total{' '}
                <span className="text-primary font-semibold">
                  €{Number(order.totalAmount).toFixed(2)}
                </span>
              </p>
              {order.status === 'PENDING_PAYMENT' && (
                <button
                  type="button"
                  disabled={cancelling}
                  onClick={handleCancel}
                  className="rounded-lg border border-red-500/50 bg-red-900/30 px-4 py-2 text-sm font-semibold text-red-200 hover:bg-red-900/50 disabled:opacity-50"
                >
                  {cancelling ? 'Cancelling…' : 'Cancel order (restore stock)'}
                </button>
              )}
            </section>

            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
              <h2 className="text-lg font-semibold">Shipping</h2>
              <p className="text-sm text-gray-300">
                {order.fullName}
                <br />
                {order.addressLine1}
                {order.addressLine2 ? (
                  <>
                    <br />
                    {order.addressLine2}
                  </>
                ) : null}
                <br />
                {order.postalCode} {order.city}
                <br />
                {order.country}
                <br />
                {order.phone} · {order.email}
              </p>
            </section>

            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-3">
              <h2 className="text-lg font-semibold">Items</h2>
              <ul className="space-y-2 text-sm">
                {order.items.map((item) => (
                  <li
                    key={`${item.productId}-${item.productName}`}
                    className="flex justify-between gap-4"
                  >
                    <span>
                      {item.productName} × {item.quantity}
                    </span>
                    <span className="text-primary font-semibold">
                      €{Number(item.lineTotal).toFixed(2)}
                    </span>
                  </li>
                ))}
              </ul>
            </section>

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
      </main>
    </div>
  );
};

export default OrderDetailPage;
