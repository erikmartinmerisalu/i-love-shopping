import { useEffect, useState } from 'react';
import { approveAdminReview, fetchAdminReviews, rejectAdminReview } from '../../api/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';
import StarRating from '../../components/StarRating';
import { ADMIN_REVIEWS_CHANGED } from '../../components/AdminReviewsNav';
import type { Review } from '../../types/reviews';

export default function AdminReviewsPage() {
  const { token } = useAuth();
  const [reviews, setReviews] = useState<Review[]>([]);
  const [status, setStatus] = useState('PENDING');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [workingId, setWorkingId] = useState<number | null>(null);

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
    setWorkingId(id);
    try {
      if (action === 'approve') {
        await approveAdminReview(token, id);
        setMessage('Review approved');
      } else {
        await rejectAdminReview(token, id);
        setMessage('Review rejected');
      }
      await load();
      window.dispatchEvent(new Event(ADMIN_REVIEWS_CHANGED));
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Moderation failed');
    } finally {
      setWorkingId(null);
    }
  };

  return (
    <>
      <PageMeta title="Admin reviews" />
      <h2 className="text-2xl font-bold">Review moderation</h2>
      <p className="mt-1 text-sm text-gray-400">Approve or reject customer reviews before they appear on the storefront.</p>

      <div className="mt-4 flex flex-wrap gap-2" role="tablist" aria-label="Review status">
        {(
          [
            { value: 'PENDING', label: 'Pending' },
            { value: 'APPROVED', label: 'Approved' },
            { value: 'REJECTED', label: 'Rejected' },
          ] as const
        ).map((tab) => (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={status === tab.value}
            onClick={() => setStatus(tab.value)}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium transition ${
              status === tab.value
                ? 'bg-primary/20 text-sky-200'
                : 'bg-gray-900 text-gray-400 hover:text-white'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {message && <p className="mt-4 text-sm text-sky-200">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <div className="mt-6 grid gap-5 lg:grid-cols-2">
        {reviews.length === 0 ? (
          <p className="text-sm text-gray-400 lg:col-span-2">No {status.toLowerCase()} reviews.</p>
        ) : (
          reviews.map((review) => (
            <article
              key={review.id}
              className={`flex min-h-[12rem] h-full flex-col rounded-2xl border p-6 ${
                status === 'PENDING'
                  ? 'border-sky-400/30 bg-gray-900/80 shadow-[0_0_32px_rgba(56,189,248,0.18)] ring-1 ring-inset ring-sky-300/15'
                  : 'border-white/10 bg-gray-900/70'
              }`}
            >
              <div className="flex flex-1 flex-col gap-5 sm:flex-row sm:items-stretch sm:justify-between">
                <div className="min-w-0 flex-1">
                  <StarRating rating={review.rating} size="md" />
                  <p className="mt-2 text-base font-semibold text-white">
                    {review.authorUsername || review.authorName}
                  </p>
                  <p className="mt-1 text-sm text-sky-200">
                    {review.productName || `Product #${review.productId}`}
                  </p>
                  <p className="mt-1 text-xs text-gray-500">
                    {new Date(review.createdAt).toLocaleString()}
                  </p>
                  <p className="mt-4 text-base leading-relaxed text-gray-200">{review.body}</p>
                </div>
                {status === 'PENDING' && (
                  <div className="flex shrink-0 flex-col justify-center gap-3 sm:w-44">
                    <button
                      type="button"
                      disabled={workingId === review.id}
                      onClick={() => void moderate(review.id, 'approve')}
                      className="rounded-xl bg-green-600 px-5 py-3 text-base font-semibold text-white shadow-[0_0_18px_rgba(22,163,74,0.45)] hover:bg-green-500 disabled:opacity-50"
                    >
                      {workingId === review.id ? 'Saving…' : 'Approve'}
                    </button>
                    <button
                      type="button"
                      disabled={workingId === review.id}
                      onClick={() => void moderate(review.id, 'reject')}
                      className="rounded-xl bg-red-700 px-5 py-3 text-base font-semibold text-white shadow-[0_0_18px_rgba(185,28,28,0.4)] hover:bg-red-600 disabled:opacity-50"
                    >
                      Reject
                    </button>
                  </div>
                )}
              </div>
            </article>
          ))
        )}
      </div>
    </>
  );
}
