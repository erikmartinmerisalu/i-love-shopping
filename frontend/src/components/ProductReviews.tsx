import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  fetchEligibleReviewOrders,
  fetchProductReviews,
  submitReview,
  toggleReviewHelpful,
} from '../api/reviews';
import { useAuth } from '../context/AuthContext';
import StarRating from './StarRating';
import StatusBanner from './StatusBanner';
import Toast from './Toast';
import type { Review, ReviewEligibleOrder } from '../types/reviews';

type ProductReviewsProps = {
  productId: number;
  productRating: number;
  reviewCount: number;
};

export default function ProductReviews({ productId, productRating, reviewCount }: ProductReviewsProps) {
  const { token, isAuthenticated } = useAuth();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [sort, setSort] = useState<'helpful' | 'recent'>('helpful');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [eligibleOrders, setEligibleOrders] = useState<ReviewEligibleOrder[]>([]);
  const [selectedOrderId, setSelectedOrderId] = useState<number | ''>('');
  const [formRating, setFormRating] = useState(5);
  const [formBody, setFormBody] = useState('');
  const [formError, setFormError] = useState('');
  const [toast, setToast] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const dismissToast = useCallback(() => setToast(''), []);

  useEffect(() => {
    setFormError('');
    setToast('');
  }, [productId]);

  const loadReviews = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchProductReviews(productId, sort);
      setReviews(data.reviews);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load reviews');
    } finally {
      setLoading(false);
    }
  }, [productId, sort]);

  useEffect(() => {
    void loadReviews();
  }, [loadReviews]);

  useEffect(() => {
    if (!isAuthenticated || !token) {
      setEligibleOrders([]);
      return;
    }
    let cancelled = false;
    const loadEligible = async () => {
      try {
        const orders = await fetchEligibleReviewOrders(token, productId);
        if (!cancelled) {
          setEligibleOrders(orders);
          if (orders.length === 1) {
            setSelectedOrderId(orders[0].orderId);
          }
        }
      } catch {
        if (!cancelled) {
          setEligibleOrders([]);
        }
      }
    };
    void loadEligible();
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, token, productId]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!token || selectedOrderId === '') {
      return;
    }
    setSubmitting(true);
    setFormError('');
    try {
      await submitReview(token, {
        productId,
        orderId: Number(selectedOrderId),
        rating: formRating,
        body: formBody.trim(),
      });
      setToast('Your review was submitted');
      setFormBody('');
      setEligibleOrders([]);
      setSelectedOrderId('');
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not submit review');
    } finally {
      setSubmitting(false);
    }
  };

  const handleHelpful = async (reviewId: number) => {
    if (!token) {
      return;
    }
    try {
      const result = await toggleReviewHelpful(token, reviewId);
      setReviews((current) =>
        current.map((review) =>
          review.id === reviewId
            ? { ...review, helpfulCount: result.helpfulCount, helpfulByCurrentUser: result.helpfulByCurrentUser }
            : review,
        ),
      );
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not update vote');
    }
  };

  return (
    <section className="mt-16 border-t border-gray-800 pt-10" aria-labelledby="reviews-heading">
      {toast && <Toast message={toast} onDismiss={dismissToast} />}

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 id="reviews-heading" className="text-xl font-bold">Customer reviews</h2>
          {reviewCount > 0 && (
            <p className="mt-1 text-sm text-gray-400">
              <StarRating rating={productRating} size="sm" />
              <span className="ml-2">{reviewCount} verified {reviewCount === 1 ? 'review' : 'reviews'}</span>
            </p>
          )}
        </div>
        <label className="text-sm text-gray-400">
          Sort by{' '}
          <select
            value={sort}
            onChange={(event) => setSort(event.target.value as 'helpful' | 'recent')}
            className="ml-2 rounded border border-gray-700 bg-gray-900 px-2 py-1 text-white"
          >
            <option value="helpful">Most helpful</option>
            <option value="recent">Most recent</option>
          </select>
        </label>
      </div>

      {isAuthenticated && eligibleOrders.length > 0 && (
        <form onSubmit={(event) => void handleSubmit(event)} className="mt-8 rounded-xl border border-gray-800 bg-gray-900/60 p-5">
          <h3 className="font-semibold">Write a review</h3>
          <p className="mt-1 text-sm text-gray-400">Only verified buyers can review this product.</p>

          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <label className="block text-sm">
              Order
              <select
                value={selectedOrderId}
                onChange={(event) => setSelectedOrderId(event.target.value ? Number(event.target.value) : '')}
                required
                className="mt-1 w-full rounded border border-gray-700 bg-gray-950 px-3 py-2"
              >
                <option value="">Select order…</option>
                {eligibleOrders.map((order) => (
                  <option key={order.orderId} value={order.orderId}>
                    {order.orderNumber}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-sm">
              Rating
              <select
                value={formRating}
                onChange={(event) => setFormRating(Number(event.target.value))}
                className="mt-1 w-full rounded border border-gray-700 bg-gray-950 px-3 py-2"
              >
                {[5, 4, 3, 2, 1].map((value) => (
                  <option key={value} value={value}>{value} star{value === 1 ? '' : 's'}</option>
                ))}
              </select>
            </label>
          </div>

          <label className="mt-4 block text-sm">
            Your review
            <textarea
              value={formBody}
              onChange={(event) => setFormBody(event.target.value)}
              required
              minLength={10}
              rows={4}
              className="mt-1 w-full rounded border border-gray-700 bg-gray-950 px-3 py-2"
              placeholder="Share details about quality, brightness, installation…"
            />
          </label>

          <button
            type="submit"
            disabled={submitting}
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
          >
            {submitting ? 'Submitting…' : 'Submit review'}
          </button>
        </form>
      )}

      {!isAuthenticated && (
        <p className="mt-6 text-sm text-gray-400">
          <Link to="/login" className="text-sky-300 hover:text-sky-200">Sign in</Link> to write a review after purchase.
        </p>
      )}

      {formError && (
        <div className="mt-6">
          <StatusBanner variant="error" title="Something went wrong" message={formError} />
        </div>
      )}

      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      {loading ? (
        <p className="mt-6 text-sm text-gray-400">Loading reviews…</p>
      ) : reviews.length === 0 ? (
        <p className="mt-6 text-sm text-gray-400">This product has no reviews yet</p>
      ) : (
        <ul className="mt-8 space-y-6">
          {reviews.map((review) => (
            <li key={review.id} className="rounded-xl border border-gray-800 bg-gray-900/40 p-5">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <StarRating rating={review.rating} size="sm" />
                  <p className="mt-1 text-sm font-medium text-gray-200">{review.authorName}</p>
                </div>
                <time className="text-xs text-gray-500" dateTime={review.createdAt}>
                  {new Date(review.createdAt).toLocaleDateString()}
                </time>
              </div>
              <p className="mt-3 text-gray-300 leading-relaxed">{review.body}</p>
              <button
                type="button"
                onClick={() => void handleHelpful(review.id)}
                disabled={!isAuthenticated}
                className={`mt-4 text-xs ${review.helpfulByCurrentUser ? 'text-sky-300' : 'text-gray-400 hover:text-gray-200'} disabled:opacity-50`}
              >
                {review.helpfulByCurrentUser ? 'Helpful ✓' : 'Mark helpful'} ({review.helpfulCount})
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
