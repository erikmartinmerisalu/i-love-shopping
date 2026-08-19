import { useEffect, useState } from 'react';
import { fetchAdminDashboard, type AdminDashboard } from '../../api/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';

export default function AdminDashboardPage() {
  const { token } = useAuth();
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const dashboard = await fetchAdminDashboard(token);
        if (!cancelled) {
          setData(dashboard);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not load dashboard');
        }
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <>
      <PageMeta title="Admin dashboard" />
      <h2 className="text-2xl font-bold">Dashboard</h2>
      <p className="mt-1 text-sm text-gray-400">Store overview and quick health checks.</p>

      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      {data && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[
            { label: 'Products', value: data.totalProducts, hint: `${data.activeProducts} active` },
            { label: 'Low stock', value: data.lowStockProducts, hint: '≤ 5 units' },
            { label: 'Orders', value: data.totalOrders, hint: `${data.pendingOrders} pending payment` },
            { label: 'Users', value: data.totalUsers, hint: `${data.adminUsers} admins` },
          ].map((card) => (
            <article key={card.label} className="rounded-xl border border-white/10 bg-gray-900/70 p-5">
              <p className="text-sm text-gray-400">{card.label}</p>
              <p className="mt-2 text-3xl font-bold text-primary">{card.value}</p>
              <p className="mt-1 text-xs text-gray-500">{card.hint}</p>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
