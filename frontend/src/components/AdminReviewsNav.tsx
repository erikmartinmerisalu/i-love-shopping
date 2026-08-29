import { useCallback, useEffect, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { fetchAdminReviews } from '../api/admin';
import { useAuth } from '../context/AuthContext';

export const ADMIN_REVIEWS_CHANGED = 'admin-reviews-changed';

const navClass = ({ isActive }: { isActive: boolean }) =>
  `relative flex w-full items-center rounded-lg px-3 py-2 pr-8 text-left text-sm font-medium transition ${
    isActive ? 'bg-primary/20 text-sky-200' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
  }`;

export default function AdminReviewsNav() {
  const { token } = useAuth();
  const location = useLocation();
  const [pendingCount, setPendingCount] = useState(0);

  const loadPending = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const pending = await fetchAdminReviews(token, 'PENDING');
      setPendingCount(Array.isArray(pending) ? pending.length : 0);
    } catch {
      setPendingCount(0);
    }
  }, [token]);

  useEffect(() => {
    void loadPending();
  }, [loadPending, location.pathname]);

  useEffect(() => {
    const refresh = () => {
      void loadPending();
    };
    window.addEventListener('focus', refresh);
    window.addEventListener(ADMIN_REVIEWS_CHANGED, refresh);
    const timer = window.setInterval(refresh, 15000);
    return () => {
      window.removeEventListener('focus', refresh);
      window.removeEventListener(ADMIN_REVIEWS_CHANGED, refresh);
      window.clearInterval(timer);
    };
  }, [loadPending]);

  return (
    <li>
      <NavLink
        to="/admin/reviews"
        className={navClass}
        aria-label={
          pendingCount > 0
            ? `Pending reviews, ${pendingCount} waiting for approval`
            : 'Pending reviews'
        }
      >
        Pending reviews
        {pendingCount > 0 && (
          <span
            className="absolute right-2 top-1/2 flex h-5 min-w-5 -translate-y-1/2 items-center justify-center rounded-full bg-red-600 px-1 text-[11px] font-bold leading-none text-white"
            aria-hidden="true"
          >
            {pendingCount > 99 ? '99+' : pendingCount}
          </span>
        )}
      </NavLink>
    </li>
  );
}
