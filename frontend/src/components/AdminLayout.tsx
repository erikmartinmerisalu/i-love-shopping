import { NavLink, Outlet, Link } from 'react-router-dom';
import { SITE } from '../config/site';

const adminLinkClass = ({ isActive }: { isActive: boolean }) =>
  `block rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive ? 'bg-primary/20 text-sky-200' : 'text-gray-300 hover:bg-gray-800 hover:text-white'
  }`;

export default function AdminLayout() {
  return (
    <div className="min-h-screen bg-gray-950 text-white">
      <header className="border-b border-white/10 bg-gray-900/80">
        <div className="page-container flex flex-wrap items-center justify-between gap-3 py-4">
          <div>
            <p className="text-xs uppercase tracking-widest text-sky-300">Admin</p>
            <h1 className="text-xl font-bold">{SITE.name} Console</h1>
          </div>
          <Link to="/" className="text-sm text-gray-300 hover:text-white">
            Back to storefront
          </Link>
        </div>
      </header>

      <div className="page-container grid gap-6 py-8 lg:grid-cols-[14rem_1fr]">
        <nav className="h-fit rounded-xl border border-white/10 bg-gray-900/60 p-3" aria-label="Admin navigation">
          <ul className="space-y-1">
            <li><NavLink to="/admin" end className={adminLinkClass}>Dashboard</NavLink></li>
            <li><NavLink to="/admin/products" className={adminLinkClass}>Products</NavLink></li>
            <li><NavLink to="/admin/categories" className={adminLinkClass}>Categories</NavLink></li>
            <li><NavLink to="/admin/orders" className={adminLinkClass}>Orders</NavLink></li>
            <li><NavLink to="/admin/users" className={adminLinkClass}>Users</NavLink></li>
            <li><NavLink to="/admin/reviews" className={adminLinkClass}>Reviews</NavLink></li>
          </ul>
        </nav>

        <main className="min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
