import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { fetchOrder, type OrderDto } from '../api/orders';
import PageMeta from '../components/PageMeta';
import OrderItemReviews from '../components/OrderItemReviews';
import { useAuth } from '../context/AuthContext';
import { estimateDeliveryDate, formatDeliveryDate } from '../config/site';

type LocationState = {
  email?: string;
};

export default function OrderConfirmationPage() {
  const { orderNumber } = useParams();
  const location = useLocation();
  const { token } = useAuth();
  const state = (location.state as LocationState | null) ?? {};

  const [order, setOrder] = useState<OrderDto | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!orderNumber) {
      setError('Missing order reference');
      setLoading(false);
      return;
    }

    let cancelled = false;
    const load = async () => {
      try {
        const result = await fetchOrder(token, orderNumber, state.email);
        if (!cancelled) {
          setOrder(result);
        }
      } catch {
        if (!cancelled) {
          setError('Could not load order details. Check your email for the confirmation.');
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    void load();
  }, [orderNumber, token, state.email]);

  const estimatedDelivery = formatDeliveryDate(
    order?.estimatedDeliveryAt ? new Date(order.estimatedDeliveryAt) : estimateDeliveryDate()
  );

  return (
    <>
      <PageMeta title="Order confirmed" description="Your ESTValgus order has been placed successfully." />

      <div className="page-container-narrow py-12">
        {loading && <p className="text-gray-400">Loading confirmation…</p>}

        {error && !loading && (
          <div className="rounded-xl border border-amber-700/50 bg-amber-900/20 p-6">
            <h1 className="text-2xl font-bold">Order placed</h1>
            <p className="mt-2 text-gray-300">{error}</p>
            {orderNumber && (
              <p className="mt-4 font-mono text-sm text-sky-300">Reference: {orderNumber}</p>
            )}
          </div>
        )}

        {order && (
          <div className="space-y-6">
            <div className="rounded-xl border border-green-500/40 bg-green-500/10 p-6">
              <h1 className="text-2xl font-bold text-green-100">Thank you for your order</h1>
              <p className="mt-2 text-sm text-green-100/90">
                Order reference{' '}
                <span className="font-mono font-semibold">{order.orderNumber}</span>
              </p>
              <p className="mt-3 text-sm">
                Estimated delivery: <strong>{estimatedDelivery}</strong>
                {order.deliveryOptionName ? ` · ${order.deliveryOptionName}` : ''}
              </p>
              <p className="mt-2 text-sm text-gray-300">
                A confirmation email has been sent to {order.email}.
              </p>
            </div>

            <section className="rounded-xl border border-gray-800 bg-gray-900 p-6">
              <h2 className="text-lg font-semibold">Order summary</h2>
              <ul className="mt-4 space-y-2 text-sm">
                {order.items.map((item) => (
                  <li key={`${item.productId}-${item.productName}`} className="flex justify-between gap-4">
                    <span>
                      {item.productName} × {item.quantity}
                    </span>
                    <span className="font-semibold text-primary">€{Number(item.lineTotal).toFixed(2)}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-4 space-y-2 border-t border-gray-700 pt-4 text-sm">
                <div className="flex justify-between text-gray-300">
                  <span>Merchandise</span>
                  <span>€{Number(order.totalAmount).toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-gray-300">
                  <span>{order.deliveryOptionName ?? 'Shipping'}</span>
                  <span>€{Number(order.shippingAmount ?? 0).toFixed(2)}</span>
                </div>
                <div className="flex justify-between font-bold">
                  <span>Total</span>
                  <span className="text-primary">
                    €{(Number(order.totalAmount) + Number(order.shippingAmount ?? 0)).toFixed(2)}
                  </span>
                </div>
              </div>
            </section>

            <OrderItemReviews order={order} />

            <div className="flex flex-wrap gap-3">
              <Link to="/products" className="rounded-lg bg-primary px-5 py-2.5 font-semibold text-white hover:bg-primary-focus">
                Continue shopping
              </Link>
              <Link to={`/orders/${order.orderNumber}`} className="rounded-lg border border-gray-700 px-5 py-2.5 hover:bg-gray-800">
                View order details
              </Link>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
