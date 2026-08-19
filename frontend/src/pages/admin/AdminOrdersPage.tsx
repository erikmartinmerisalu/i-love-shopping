import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  createAdminRefund,
  fetchAdminOrders,
  fetchDeliveryOptions,
  updateAdminOrder,
  type DeliveryOption,
} from '../../api/admin';
import type { OrderDto } from '../../api/orders';
import { ORDER_STATUSES } from '../../types/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';

const inputClass =
  'w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-white';

export default function AdminOrdersPage() {
  const { token } = useAuth();
  const [orders, setOrders] = useState<OrderDto[]>([]);
  const [deliveryOptions, setDeliveryOptions] = useState<DeliveryOption[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [status, setStatus] = useState('');
  const [deliveryOptionId, setDeliveryOptionId] = useState<number | ''>('');
  const [refundAmount, setRefundAmount] = useState('');
  const [refundReason, setRefundReason] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const selectedOrder = orders.find((order) => order.id === selectedId) ?? null;

  const load = useCallback(async () => {
    try {
      const [orderList, options] = await Promise.all([
        fetchAdminOrders(token, 0, 50, statusFilter || undefined),
        fetchDeliveryOptions(token),
      ]);
      setOrders(orderList);
      setDeliveryOptions(options);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load orders');
    }
  }, [token, statusFilter]);

  useEffect(() => {
    void load();
  }, [load]);

  const openOrder = (order: OrderDto) => {
    setSelectedId(order.id);
    setStatus(order.status);
    setDeliveryOptionId('');
    setRefundAmount(order.totalAmount.toFixed(2));
    setRefundReason('');
    setMessage('');
    setError('');
  };

  const handleStatusSave = async (event: FormEvent) => {
    event.preventDefault();
    if (selectedId === null) {
      return;
    }
    setSaving(true);
    setMessage('');
    setError('');
    try {
      const payload: { status: string; deliveryOptionId?: number } = { status };
      if (deliveryOptionId !== '') {
        payload.deliveryOptionId = deliveryOptionId;
      }
      const updated = await updateAdminOrder(token, selectedId, payload);
      setMessage(`Order ${updated.orderNumber} updated to ${updated.status}`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleRefund = async (event: FormEvent) => {
    event.preventDefault();
    if (selectedId === null) {
      return;
    }
    setSaving(true);
    setMessage('');
    setError('');
    try {
      await createAdminRefund(token, selectedId, {
        amount: Number(refundAmount),
        reason: refundReason.trim() || undefined,
      });
      setMessage('Refund recorded and order marked REFUNDED');
      setStatus('REFUNDED');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Refund failed');
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <PageMeta title="Admin orders" />
      <h2 className="text-2xl font-bold">Orders</h2>
      <p className="mt-1 text-sm text-gray-400">Update status, assign delivery, and issue refunds.</p>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <label className="block text-sm">
          <span className="mb-1 block text-xs text-gray-400">Filter by status</span>
          <select
            className={inputClass}
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setSelectedId(null);
            }}
          >
            <option value="">All statuses</option>
            {ORDER_STATUSES.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
      </div>

      {message && <p className="mt-4 text-sm text-emerald-300">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_minmax(18rem,22rem)]">
        <div className="overflow-x-auto rounded-xl border border-white/10">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-gray-900/80 text-gray-300">
              <tr>
                <th className="px-4 py-3">Order</th>
                <th className="px-4 py-3">Customer</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Total</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr
                  key={order.id}
                  className={`border-t border-white/5 ${selectedId === order.id ? 'bg-primary/10' : ''}`}
                >
                  <td className="px-4 py-3 font-mono text-xs">{order.orderNumber}</td>
                  <td className="px-4 py-3">{order.fullName}</td>
                  <td className="px-4 py-3">{order.status}</td>
                  <td className="px-4 py-3">€{order.totalAmount.toFixed(2)}</td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => openOrder(order)}
                      className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                    >
                      Manage
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <aside className="rounded-xl border border-white/10 bg-gray-900/70 p-4">
          {!selectedOrder ? (
            <p className="text-sm text-gray-400">Select an order to update status or issue a refund.</p>
          ) : (
            <div className="space-y-5">
              <div>
                <p className="text-xs uppercase tracking-wide text-gray-500">Order</p>
                <p className="font-mono text-sm">{selectedOrder.orderNumber}</p>
                <p className="mt-1 text-sm text-gray-300">{selectedOrder.fullName}</p>
                <p className="text-sm text-gray-400">{selectedOrder.email}</p>
                <p className="mt-2 text-lg font-bold text-primary">
                  €{selectedOrder.totalAmount.toFixed(2)}
                </p>
              </div>

              <form onSubmit={handleStatusSave} className="space-y-3 border-t border-white/10 pt-4">
                <h3 className="font-semibold">Status &amp; delivery</h3>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Status</span>
                  <select
                    className={inputClass}
                    value={status}
                    onChange={(e) => setStatus(e.target.value)}
                  >
                    {ORDER_STATUSES.map((value) => (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Delivery option (optional)</span>
                  <select
                    className={inputClass}
                    value={deliveryOptionId}
                    onChange={(e) =>
                      setDeliveryOptionId(e.target.value === '' ? '' : Number(e.target.value))
                    }
                  >
                    <option value="">None</option>
                    {deliveryOptions.map((option) => (
                      <option key={option.id} value={option.id}>
                        {option.name} — €{option.price.toFixed(2)} ({option.estimatedDays} days)
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  type="submit"
                  disabled={saving}
                  className="w-full rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                >
                  Save order changes
                </button>
              </form>

              <form onSubmit={handleRefund} className="space-y-3 border-t border-white/10 pt-4">
                <h3 className="font-semibold">Issue refund</h3>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Amount (€)</span>
                  <input
                    required
                    type="number"
                    min="0.01"
                    step="0.01"
                    className={inputClass}
                    value={refundAmount}
                    onChange={(e) => setRefundAmount(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Reason</span>
                  <textarea
                    rows={2}
                    className={inputClass}
                    value={refundReason}
                    onChange={(e) => setRefundReason(e.target.value)}
                  />
                </label>
                <button
                  type="submit"
                  disabled={saving}
                  className="w-full rounded-lg bg-red-800 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-50"
                >
                  Record refund
                </button>
              </form>
            </div>
          )}
        </aside>
      </div>
    </>
  );
}
