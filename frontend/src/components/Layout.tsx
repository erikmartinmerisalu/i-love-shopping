import { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { adminNeedsTwoFactorSetup, useAuth } from '../context/AuthContext';
import Cart from './Cart';
import CartToggleButton from './CartToggleButton';
import QuickSearch from './QuickSearch';
import { SITE } from '../config/site';

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `text-sm font-medium transition ${isActive ? 'text-sky-300' : 'text-gray-300 hover:text-white'}`;

export default function Layout() {
  const navigate = useNavigate();
  const { user, isGuest, isAuthenticated, logout } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const adminHref = adminNeedsTwoFactorSetup(user)
    ? '/profile?tab=security&admin2fa=1'
    : '/admin';
  const [cartOpen, setCartOpen] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const handleAuthAction = async () => {
    if (isAuthenticated && !isGuest) {
      await logout();
    }
    navigate('/login');
  };

  const toggleCart = () => setCartOpen((current) => !current);

  return (
    <div className="min-h-screen flex flex-col text-white">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2"
      >
        Skip to main content
      </a>

      <header className="sticky top-0 z-40 w-full border-b border-white/10 bg-gray-950/80 backdrop-blur-md">
        <div className="page-container">
          <div className="flex flex-wrap items-center gap-x-5 gap-y-3 py-3 lg:flex-nowrap lg:py-4">
            <Link to="/" className="shrink-0 text-xl font-bold text-primary sm:text-2xl">
              {SITE.name}
            </Link>

            <nav className="hidden items-center gap-5 lg:flex" aria-label="Main navigation">
              <NavLink to="/" end className={navLinkClass}>
                Home
              </NavLink>
              <NavLink to="/products" className={navLinkClass}>
                Shop
              </NavLink>
              <NavLink to="/about" className={navLinkClass}>
                About
              </NavLink>
              <NavLink to="/contact" className={navLinkClass}>
                Contact
              </NavLink>
            </nav>

            <div className="order-3 w-full min-w-0 lg:order-none lg:flex lg:flex-1 lg:justify-center lg:px-6">
              <QuickSearch />
            </div>

            <div className="ml-auto flex shrink-0 items-center gap-2 sm:gap-3">
              {isAuthenticated && !isGuest && (
                <>
                  <Link
                    to="/orders"
                    className="hidden rounded-lg bg-gray-800 px-3 py-2 text-sm hover:bg-gray-700 sm:inline-block"
                  >
                    Orders
                  </Link>
                  <Link
                    to="/profile"
                    className="hidden rounded-lg bg-gray-800 px-3 py-2 text-sm hover:bg-gray-700 sm:inline-block"
                  >
                    Profile
                  </Link>
                  {isAdmin && (
                    <Link
                      to={adminHref}
                      className="hidden rounded-lg bg-primary/20 px-3 py-2 text-sm text-sky-200 hover:bg-primary/30 sm:inline-block"
                    >
                      Admin
                    </Link>
                  )}
                </>
              )}
              <CartToggleButton sidebarOpen={cartOpen} onToggleSidebar={toggleCart} />
              <button
                type="button"
                onClick={handleAuthAction}
                className={`rounded-lg px-3 py-2 text-sm font-semibold transition ${
                  isAuthenticated && !isGuest
                    ? 'border border-red-500 bg-red-600 text-white hover:bg-red-500'
                    : 'border border-emerald-500 bg-emerald-600 text-white hover:bg-emerald-500'
                }`}
              >
                {isAuthenticated && !isGuest ? 'Sign out' : 'Sign in'}
              </button>
              <button
                type="button"
                className="rounded-lg border border-gray-700 px-3 py-2 text-sm lg:hidden"
                aria-expanded={mobileNavOpen}
                aria-label="Toggle menu"
                onClick={() => setMobileNavOpen((current) => !current)}
              >
                Menu
              </button>
            </div>
          </div>
        </div>

        {mobileNavOpen && (
          <nav className="page-container border-t border-gray-800 py-3 lg:hidden" aria-label="Mobile navigation">
            <ul className="flex flex-col gap-2">
              <li><NavLink to="/" end className={navLinkClass} onClick={() => setMobileNavOpen(false)}>Home</NavLink></li>
              <li><NavLink to="/products" className={navLinkClass} onClick={() => setMobileNavOpen(false)}>Shop</NavLink></li>
              <li><NavLink to="/about" className={navLinkClass} onClick={() => setMobileNavOpen(false)}>About</NavLink></li>
              <li><NavLink to="/contact" className={navLinkClass} onClick={() => setMobileNavOpen(false)}>Contact</NavLink></li>
              {isAuthenticated && !isGuest && (
                <>
                  <li><NavLink to="/orders" className={navLinkClass} onClick={() => setMobileNavOpen(false)}>Orders</NavLink></li>
                  <li><NavLink to="/profile" className={navLinkClass} onClick={() => setMobileNavOpen(false)}>Profile</NavLink></li>
                </>
              )}
            </ul>
            {user && !isGuest && (
              <p className="mt-3 text-xs text-gray-400">Signed in as {user.username}</p>
            )}
          </nav>
        )}
      </header>

      <div className="flex min-w-0 flex-1">
        <div className="flex min-w-0 flex-1 flex-col">
          <main id="main-content" className="min-w-0 flex-1">
            <Outlet />
          </main>

          <footer className="mt-auto border-t border-white/10 bg-gray-950/80 backdrop-blur-sm" role="contentinfo">
            <div className="page-container grid gap-8 py-10 lg:grid-cols-3">
              <div>
                <h2 className="text-lg font-bold text-primary">{SITE.name}</h2>
                <p className="mt-2 text-sm text-gray-400">{SITE.tagline}</p>
              </div>
              <div>
                <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-300">Explore</h3>
                <ul className="mt-3 space-y-2 text-sm text-gray-400">
                  <li><Link to="/products" className="hover:text-white">All products</Link></li>
                  <li><Link to="/about" className="hover:text-white">About us</Link></li>
                  <li><Link to="/contact" className="hover:text-white">Support</Link></li>
                </ul>
              </div>
              <div>
                <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-300">Connect</h3>
                <p className="mt-3 text-xs text-gray-500">Demo placeholders — not real profiles.</p>
                <ul className="mt-2 space-y-2 text-sm text-gray-500">
                  <li>Instagram (mock)</li>
                  <li>Facebook (mock)</li>
                  <li>LinkedIn (mock)</li>
                </ul>
              </div>
            </div>
            <div className="page-container border-t border-white/10 py-4 text-center text-xs text-gray-500">
              © {new Date().getFullYear()} {SITE.name}. All rights reserved.
            </div>
          </footer>
        </div>

        {cartOpen && (
          <aside
            className="cart-sidebar sticky top-[4.25rem] hidden h-[calc(100dvh-4.25rem)] overflow-hidden lg:flex"
            aria-label="Shopping cart panel"
          >
            <Cart onClose={() => setCartOpen(false)} />
          </aside>
        )}
      </div>

      {cartOpen && (
        <>
          <button
            type="button"
            className="fixed inset-0 z-40 bg-black/40 lg:hidden"
            aria-label="Close cart overlay"
            onClick={() => setCartOpen(false)}
          />
          <aside
            className="cart-sidebar fixed inset-y-0 right-0 z-50 flex lg:hidden"
            aria-label="Shopping cart panel"
          >
            <Cart onClose={() => setCartOpen(false)} />
          </aside>
        </>
      )}
    </div>
  );
}
