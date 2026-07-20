import { Navigate } from "react-router-dom";
import type { ReactNode } from "react";
import { useAuth } from "../context/AuthContext";

type ProtectedRouteProps = {
  children: ReactNode;
  allowGuest?: boolean;
};

const ProtectedRoute = ({ children, allowGuest = false }: ProtectedRouteProps) => {
  const { isAuthenticated, isGuest, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-950 text-white flex items-center justify-center">
        <p className="text-slate-400">Restoring session...</p>
      </div>
    );
  }

  const canAccess = isAuthenticated || (allowGuest && isGuest);

  if (!canAccess) {
    return <Navigate to="/login" replace />;
  }

  return children;
};

export default ProtectedRoute;
