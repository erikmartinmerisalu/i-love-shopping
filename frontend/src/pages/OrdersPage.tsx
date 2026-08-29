import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { CheckoutApiError, fetchOrders, type OrderDto } from '../api/orders';

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'PENDING_PAYMENT', label: 'Pending payment' },
  { value: 'PAID', label: 'Paid' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'FULFILLED', label: 'Fulfilled' },
] as const;

const SORT_OPTIONS = [
  { value: 'date_desc', label: 'Newest first' },
  { value: 'date_asc', label: 'Oldest first' },
] as const;

const formatStatus = (status: string) => status.replaceAll('_', ' ');

const OrdersPage = () => {
  const navigate = useNavigate();
  const { token, isAuthenticated, isGuest } = useAuth();
  const [orders, setOrders] = useState<OrderDto[]>([]);
  const [status, setStatus] = useState('');
  const [sort, setSort] = useState('date_desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadOrders = useCallback(async () => {
    if (!isAuthenticated || !token) {
      setLoading(false);
      setError('Please log in to view your orders.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await fetchOrders(token, {
        status: status || undefined,
        sort,
      });
      setOrders(data);
    } catch (err) {
      setError(err instanceof CheckoutApiError ? err.message : 'Could not load orders');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated, token, status, sort]);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  if (isGuest || !isAuthenticated) {
    return (
      <div className="page-container-form py-10">
        <h1 className="text-2xl font-bold">My orders</h1>
        <div className="mt-6 rounded-xl border border-amber-500/40 bg-amber-500/10 p-4 text-amber-100 text-sm">
          Order history requires a logged-in account. After a successful payment, guests receive a
          confirmation email with the order number.
        </div>
        <button
          type="button"
          onClick={() => navigate('/login')}
          className="mt-4 rounded-lg bg-primary px-4 py-2 font-semibold"
        >
          Log in
        </button>
      </div>
    );
  }

  return (
    <div className="page-container-form space-y-6 py-10">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">My orders</h1>
        <button
          type="button"
          onClick={() => navigate('/products')}
          className="text-sm text-sky-300 hover:text-sky-200"
        >
          ← Back to shop
        </button>
      </div>

      <div className="space-y-6">
        <div className="flex flex-wrap gap-3">
          <label className="text-sm space-y-1">
            <span className="text-gray-400">Status</span>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="block rounded-lg border border-gray-700 bg-gray-900 px-3 py-2"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value || 'all'} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm space-y-1">
            <span className="text-gray-400">Sort by date</span>
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              className="block rounded-lg border border-gray-700 bg-gray-900 px-3 py-2"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>

        {error && (
          <div className="rounded-xl border border-red-700 bg-red-900/40 px-4 py-3 text-sm text-red-200">
            {error}
          </div>
        )}

        {loading ? (
          <p className="text-gray-400">Loading orders…</p>
        ) : orders.length === 0 ? (
          <p className="text-gray-400">No orders match these filters.</p>
        ) : (
          <ul className="space-y-3">
            {orders.map((order) => (
              <li key={order.orderNumber}>
                <button
                  type="button"
                  onClick={() =>
                    navigate(
                      `/orders/${order.orderNumber}${order.items.some((item) => item.canReview) ? '#reviews' : ''}`,
                    )
                  }
                  className="w-full rounded-xl border border-gray-800 bg-gray-900 p-4 text-left hover:border-sky-500/50 transition"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="font-mono text-sm text-sky-300">{order.orderNumber}</span>
                    <span className="text-xs uppercase tracking-wide text-gray-300">
                      {formatStatus(order.status)}
                    </span>
                  </div>
                  <div className="mt-2 flex flex-wrap justify-between gap-2 text-sm text-gray-400">
                    <span>{order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}</span>
                    <span className="text-primary font-semibold">
                      €{Number(order.totalAmount).toFixed(2)}
                    </span>
                  </div>
                  {order.items.some((item) => item.canReview) && (
                    <p className="mt-2 text-xs font-medium text-sky-300">Not reviewed yet — leave a review →</p>
                  )}
                  {!order.items.some((item) => item.canReview) &&
                    order.items.some((item) => item.reviewStatus === 'PENDING') && (
                      <p className="mt-2 text-xs font-medium text-amber-200">Review submitted — waiting for approval</p>
                    )}
                  {!order.items.some((item) => item.canReview) &&
                    order.items.some((item) => item.reviewStatus === 'APPROVED') &&
                    !order.items.some((item) => item.reviewStatus === 'PENDING') && (
                      <p className="mt-2 text-xs font-medium text-emerald-300">Reviewed</p>
                    )}
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
};

export default OrdersPage;
