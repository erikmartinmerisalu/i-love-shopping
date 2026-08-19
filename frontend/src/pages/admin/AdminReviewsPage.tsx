import { useEffect, useState } from 'react';
import { approveAdminReview, fetchAdminReviews, rejectAdminReview } from '../../api/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';
import StarRating from '../../components/StarRating';
import type { Review } from '../../types/reviews';

export default function AdminReviewsPage() {
  const { token } = useAuth();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [status, setStatus] = useState('PENDING');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = async () => {
    try {
      setReviews(await fetchAdminReviews(token, status));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load reviews');
    }
  };

  useEffect(() => {
    void load();
  }, [token, status]);

  const moderate = async (id: number, action: 'approve' | 'reject') => {
    try {
      if (action === 'approve') {
        await approveAdminReview(token, id);
        setMessage('Review approved');
      } else {
        await rejectAdminReview(token, id);
        setMessage('Review rejected');
      }
      await load();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Moderation failed');
    }
  };

  return (
    <>
      <PageMeta title="Admin reviews" />
      <h2 className="text-2xl font-bold">Review moderation</h2>
      <p className="mt-1 text-sm text-gray-400">Approve or reject customer reviews before they appear on the storefront.</p>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        <label className="text-sm text-gray-400">
          Status{' '}
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="ml-2 rounded border border-gray-700 bg-gray-900 px-2 py-1 text-white"
          >
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </label>
      </div>

      {message && <p className="mt-4 text-sm text-sky-200">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <div className="mt-6 space-y-4">
        {reviews.length === 0 ? (
          <p className="text-sm text-gray-400">No {status.toLowerCase()} reviews.</p>
        ) : (
          reviews.map((review) => (
            <article key={review.id} className="rounded-xl border border-white/10 bg-gray-900/70 p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <StarRating rating={review.rating} size="sm" />
                  <p className="mt-1 text-sm text-gray-300">{review.authorName}</p>
                  <p className="mt-1 text-xs text-gray-500">
                    Product #{review.productId} · {new Date(review.createdAt).toLocaleString()}
                  </p>
                </div>
                {status === 'PENDING' && (
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => void moderate(review.id, 'approve')}
                      className="rounded bg-green-700 px-3 py-1 text-xs font-medium hover:bg-green-600"
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => void moderate(review.id, 'reject')}
                      className="rounded bg-red-800 px-3 py-1 text-xs font-medium hover:bg-red-700"
                    >
                      Reject
                    </button>
                  </div>
                )}
              </div>
              <p className="mt-3 text-sm leading-relaxed text-gray-200">{review.body}</p>
            </article>
          ))
        )}
      </div>
    </>
  );
}
