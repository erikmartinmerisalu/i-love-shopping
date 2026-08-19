import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

type CartToggleButtonProps = {
  sidebarOpen: boolean;
  onToggleSidebar: () => void;
};

export default function CartToggleButton({ sidebarOpen, onToggleSidebar }: CartToggleButtonProps) {
  const navigate = useNavigate();
  const { cartItems, totalItems, totalPrice } = useCart();
  const [previewOpen, setPreviewOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setPreviewOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const previewItems = cartItems.slice(0, 3);
  const remaining = cartItems.length - previewItems.length;

  const handleCartClick = () => {
    if (sidebarOpen) {
      onToggleSidebar();
      return;
    }
    if (cartItems.length === 0) {
      onToggleSidebar();
      return;
    }
    setPreviewOpen((current) => !current);
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={handleCartClick}
        className={`relative rounded-lg px-4 py-2.5 text-sm font-semibold text-white transition ${
          sidebarOpen
            ? 'bg-primary-focus ring-2 ring-sky-400/50'
            : 'bg-primary hover:bg-primary-focus'
        }`}
        aria-expanded={sidebarOpen || previewOpen}
        aria-pressed={sidebarOpen}
        aria-label={`Shopping cart, ${totalItems} items${sidebarOpen ? ', panel open' : ''}`}
      >
        {sidebarOpen ? 'Close cart' : 'Cart'}
        {totalItems > 0 && !sidebarOpen && (
          <span className="absolute -top-2 -right-2 flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-xs font-bold">
            {totalItems}
          </span>
        )}
      </button>

      {previewOpen && !sidebarOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 rounded-xl border border-gray-700 bg-gray-900 shadow-2xl">
          <div className="border-b border-gray-800 px-4 py-3">
            <h2 className="text-sm font-semibold">Quick peek</h2>
          </div>

          {cartItems.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-gray-400">Your cart is empty</p>
          ) : (
            <>
              <ul className="max-h-64 space-y-3 overflow-y-auto px-4 py-3">
                {previewItems.map((item) => (
                  <li key={item.id} className="flex gap-3">
                    <img src={item.image} alt={item.name} className="h-12 w-12 rounded object-cover" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{item.name}</p>
                      <p className="text-xs text-gray-400">
                        €{item.price.toFixed(2)} × {item.quantity}
                      </p>
                    </div>
                  </li>
                ))}
                {remaining > 0 && (
                  <li className="text-xs text-gray-400">
                    + {remaining} more item{remaining === 1 ? '' : 's'}
                  </li>
                )}
              </ul>
              <div className="space-y-2 border-t border-gray-800 px-4 py-3">
                <div className="flex justify-between text-sm font-semibold">
                  <span>Subtotal</span>
                  <span className="text-primary">€{totalPrice.toFixed(2)}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setPreviewOpen(false);
                    onToggleSidebar();
                  }}
                  className="w-full rounded-lg border border-gray-700 py-2 text-sm text-gray-200 hover:bg-gray-800"
                >
                  Open cart panel
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setPreviewOpen(false);
                    navigate('/checkout');
                  }}
                  className="w-full rounded-lg bg-primary py-2 text-sm font-semibold text-white hover:bg-primary-focus"
                >
                  Checkout
                </button>
              </div>
            </>
          )}

          {cartItems.length === 0 && (
            <div className="border-t border-gray-800 px-4 py-3">
              <Link
                to="/products"
                onClick={() => setPreviewOpen(false)}
                className="block text-center text-sm text-sky-300 hover:text-sky-200"
              >
                Browse products
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
