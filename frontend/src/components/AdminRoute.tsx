import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { adminNeedsTwoFactorSetup, useAuth } from '../context/AuthContext';

type AdminRouteProps = {
  children: ReactNode;
};

const AdminRoute = ({ children }: AdminRouteProps) => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center">
        <p className="text-slate-400">Restoring session...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (user.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }

  if (adminNeedsTwoFactorSetup(user)) {
    return <Navigate to="/profile?tab=security&admin2fa=1" replace />;
  }

  return children;
};

export default AdminRoute;
