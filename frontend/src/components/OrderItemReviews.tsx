import { useCallback, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { submitReview } from '../api/reviews';
import type { OrderDto, OrderItemDto } from '../api/orders';
import { useAuth } from '../context/AuthContext';
import StatusBanner from './StatusBanner';
import Toast from './Toast';

const REVIEWABLE_STATUSES = new Set(['PAID', 'SHIPPED', 'FULFILLED']);

type Props = {
  order: OrderDto;
  onReviewed?: (productId: number, reviewStatus: string) => void;
  title?: string;
};

const inputClass =
  'w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-white';

const actionButtonClass =
  'inline-flex items-center justify-center rounded-lg px-3.5 py-2 text-sm font-semibold transition';

function ItemReviewForm({
  order,
  item,
  onReviewed,
}: {
  order: OrderDto;
  item: OrderItemDto;
  onReviewed?: Props['onReviewed'];
}) {
  const { token, isAuthenticated } = useAuth();
  const [rating, setRating] = useState(5);
  const [body, setBody] = useState('');
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(item.reviewStatus ?? null);

  const productId = item.productId;
  const canReview = Boolean(item.canReview && productId && isAuthenticated && !submitted);
  const dismissToast = useCallback(() => setToast(''), []);
  const reviewBodyRef = useRef<HTMLTextAreaElement>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!token || !productId) {
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const review = await submitReview(token, {
        productId,
        orderId: order.id,
        rating,
        body: body.trim(),
      });
      setSubmitted(review.status ?? 'PENDING');
      setToast('Your review was submitted');
      onReviewed?.(productId, review.status ?? 'PENDING');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not submit review');
    } finally {
      setSubmitting(false);
    }
  };

  const statusLabel =
    submitted === 'PENDING'
      ? 'Review submitted — waiting for approval'
      : submitted === 'APPROVED'
        ? 'Your review is live'
        : submitted === 'REJECTED'
          ? 'Your previous review was not approved'
          : null;

  const reviewableOrder = REVIEWABLE_STATUSES.has(order.status);
  const pendingReview = canReview || (reviewableOrder && !submitted);

  return (
    <article
      id={productId ? `review-item-${productId}` : undefined}
      className="rounded-lg border border-white/10 bg-gray-950/60 p-4"
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium text-white">{item.productName}</p>
          <p className="text-xs text-gray-400">
            Qty {item.quantity} · €{Number(item.lineTotal).toFixed(2)}
          </p>
          {statusLabel ? (
            <p
              className={`mt-2 inline-flex rounded-full border px-2.5 py-1 text-xs font-semibold ${
                submitted === 'PENDING'
                  ? 'border-amber-400/40 bg-amber-500/10 text-amber-100'
                  : submitted === 'REJECTED'
                    ? 'border-gray-500/40 bg-gray-800 text-gray-300'
                    : 'border-emerald-400/40 bg-emerald-500/10 text-emerald-200'
              }`}
            >
              {statusLabel}
            </p>
          ) : pendingReview && isAuthenticated ? (
            <p className="mt-2 inline-flex rounded-full border border-sky-400/40 bg-sky-500/10 px-2.5 py-1 text-xs font-semibold text-sky-200">
              Not reviewed yet
            </p>
          ) : reviewableOrder && !isAuthenticated ? (
            <p className="mt-2 inline-flex rounded-full border border-amber-400/40 bg-amber-500/10 px-2.5 py-1 text-xs font-semibold text-amber-100">
              Sign in to leave a review
            </p>
          ) : reviewableOrder && !productId ? (
            <p className="mt-2 inline-flex rounded-full border border-gray-500/40 bg-gray-800 px-2.5 py-1 text-xs font-semibold text-gray-300">
              Can’t review — product removed
            </p>
          ) : null}
        </div>
        {productId ? (
          <Link
            to={`/products/${productId}`}
            className={`${actionButtonClass} border border-white/15 bg-gray-800 text-white hover:bg-gray-700`}
          >
            View product
          </Link>
        ) : null}
      </div>

      {toast && <Toast message={toast} onDismiss={dismissToast} />}

      {canReview && (
        <form onSubmit={(event) => void handleSubmit(event)} className="mt-4 space-y-3">
          <label className="block text-sm">
            <span className="text-gray-300">Rating</span>
            <select
              className={`${inputClass} mt-1`}
              value={rating}
              onChange={(event) => setRating(Number(event.target.value))}
            >
              {[5, 4, 3, 2, 1].map((value) => (
                <option key={value} value={value}>
                  {value} star{value === 1 ? '' : 's'}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-sm">
            <span className="text-gray-300">Your review</span>
            <textarea
              ref={reviewBodyRef}
              className={`${inputClass} mt-1`}
              required
              minLength={10}
              rows={3}
              value={body}
              onChange={(event) => setBody(event.target.value)}
              placeholder="How was the brightness, build quality, delivery…?"
            />
          </label>
          <button
            type="submit"
            disabled={submitting}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
          >
            {submitting ? 'Submitting…' : 'Submit review'}
          </button>
        </form>
      )}

      {error && (
        <div className="mt-3">
          <StatusBanner variant="error" title="Could not submit review" message={error} />
        </div>
      )}
    </article>
  );
}

export default function OrderItemReviews({ order, onReviewed, title = 'Items' }: Props) {
  const { isAuthenticated } = useAuth();
  const reviewable = REVIEWABLE_STATUSES.has(order.status);

  return (
    <section id="reviews" className="rounded-xl border border-gray-800 bg-gray-900 p-6 space-y-4">
      <div>
        <h2 className="text-lg font-semibold">{title}</h2>
        {order.status === 'PENDING_PAYMENT' && (
          <p className="mt-1 text-sm text-gray-400">Reviews unlock as soon as payment succeeds.</p>
        )}
        {!isAuthenticated && reviewable && (
          <p className="mt-1 text-sm text-amber-100">
            <Link to="/login" className="text-sky-300 underline underline-offset-2 hover:text-sky-200">
              Sign in
            </Link>{' '}
            with the account that placed this order to leave a review here.
          </p>
        )}
      </div>

      <div className="space-y-3">
        {order.items.map((item) => (
          <ItemReviewForm
            key={`${item.productId}-${item.productName}`}
            order={order}
            item={item}
            onReviewed={onReviewed}
          />
        ))}
      </div>
    </section>
  );
}
