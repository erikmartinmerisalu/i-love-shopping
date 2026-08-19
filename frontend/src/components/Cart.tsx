import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchCartRecommendations, type CartRecommendationDto } from '../api/cart';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { resolveProductImageUrl } from '../utils/productImageUrl';
import CustomDesign from '../assets/Custom_Design.png';

interface CartProps {
  onClose?: () => void;
}

const Cart = ({ onClose }: CartProps) => {
  const navigate = useNavigate();
  const { token } = useAuth();
  const {
    cartItems,
    removeFromCart,
    updateQuantity,
    totalPrice,
    totalItems,
    clearCart,
    cartError,
    addToCart,
  } = useCart();
  const [actionError, setActionError] = useState<string | null>(null);
  const [recommendations, setRecommendations] = useState<CartRecommendationDto[]>([]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      if (cartItems.length === 0) {
        setRecommendations([]);
        return;
      }
      const items = await fetchCartRecommendations(token, 3);
      if (!cancelled) {
        setRecommendations(items);
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [cartItems, token]);

  const handleUpdate = async (productId: number, quantity: number) => {
    setActionError(null);
    try {
      await updateQuantity(productId, quantity);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Could not update quantity');
    }
  };

  const handleRemove = async (productId: number) => {
    setActionError(null);
    try {
      await removeFromCart(productId);
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Could not remove item');
    }
  };

  const handleClear = async () => {
    setActionError(null);
    try {
      await clearCart();
    } catch (error) {
      setActionError(error instanceof Error ? error.message : 'Could not clear cart');
    }
  };

  const handleCheckout = () => {
    onClose?.();
    navigate('/checkout');
  };

  const displayError = actionError || cartError;

  return (
    <div className="flex h-full w-full flex-col text-white">
      <div className="flex shrink-0 items-center justify-between border-b border-white/10 px-4 py-2.5">
        <div>
          <h2 className="text-base font-bold">Your order</h2>
          <p className="text-[11px] text-gray-400">
            {totalItems} item{totalItems === 1 ? '' : 's'}
          </p>
        </div>
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            className="rounded border border-white/10 px-2 py-1 text-xs text-gray-300 hover:bg-white/5"
            aria-label="Close cart panel"
          >
            Hide
          </button>
        )}
      </div>

      {displayError && (
        <div className="mx-3 mt-2 shrink-0 rounded border border-red-700 bg-red-900/40 px-2 py-1.5 text-xs text-red-200">
          {displayError}
        </div>
      )}

      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2">
        {cartItems.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <p className="text-sm text-gray-400">Your cart is empty</p>
            <button
              type="button"
              onClick={() => navigate('/products')}
              className="mt-3 text-xs text-sky-300 hover:text-sky-200"
            >
              Browse products →
            </button>
          </div>
        ) : (
          <ul className="space-y-2.5">
            {cartItems.map((item) => (
              <li
                key={item.id}
                className="rounded-xl border border-white/10 bg-gray-900/60 px-3 py-3"
              >
                <div className="flex items-center gap-3">
                  <img
                    src={item.image}
                    alt={item.name}
                    className="h-16 w-16 shrink-0 rounded-md object-cover"
                  />
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate text-sm font-semibold leading-snug">{item.name}</h3>
                    <p className="mt-0.5 text-xs leading-tight text-gray-500">
                      €{item.price.toFixed(2)} × {item.quantity}
                    </p>
                    <p
                      className="mt-1 inline-flex rounded-md bg-primary/20 px-2 py-0.5 text-base font-bold tabular-nums text-sky-100 ring-1 ring-primary/40"
                      aria-label={`Line total for ${item.name}`}
                    >
                      €{(item.price * item.quantity).toFixed(2)}
                    </p>
                    {item.stockQuantity <= 0 && (
                      <p className="mt-0.5 text-xs text-red-400">Out of stock</p>
                    )}
                  </div>
                  <div
                    className="flex shrink-0 flex-col items-center"
                    role="group"
                    aria-label={`Quantity for ${item.name}`}
                  >
                    <div className="flex items-stretch overflow-hidden rounded-lg border-2 border-primary/50 bg-gray-950 shadow-[0_0_0_1px_rgba(14,165,233,0.15)]">
                      <button
                        type="button"
                        onClick={() => void handleUpdate(item.id, item.quantity - 1)}
                        className="flex h-10 w-10 items-center justify-center bg-gray-800 text-xl font-bold leading-none text-white transition hover:bg-primary hover:text-white"
                        aria-label={`Decrease quantity of ${item.name}`}
                      >
                        −
                      </button>
                      <div
                        className="flex min-w-[2.75rem] items-center justify-center border-x-2 border-primary/40 bg-primary/25 px-2"
                        aria-live="polite"
                        aria-atomic="true"
                      >
                        <span className="text-lg font-bold tabular-nums text-sky-100">
                          {item.quantity}
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => void handleUpdate(item.id, item.quantity + 1)}
                        disabled={item.quantity >= item.stockQuantity}
                        className="flex h-10 w-10 items-center justify-center bg-gray-800 text-xl font-bold leading-none text-white transition hover:bg-primary hover:text-white disabled:cursor-not-allowed disabled:opacity-35"
                        aria-label={`Increase quantity of ${item.name}`}
                      >
                        +
                      </button>
                    </div>
                    <span className="mt-1 text-[10px] font-medium uppercase tracking-wide text-gray-500">
                      Qty
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => void handleRemove(item.id)}
                    className="shrink-0 rounded-md p-1.5 text-sm font-medium text-red-400 hover:bg-red-500/10 hover:text-red-300"
                    aria-label={`Remove ${item.name}`}
                  >
                    ✕
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {recommendations.length > 0 && (
        <div className="shrink-0 border-t border-white/10 px-3 py-2">
          <h3 className="text-[10px] font-semibold uppercase tracking-wide text-gray-500">
            You may also like
          </h3>
          <ul className="mt-1.5 flex gap-2 overflow-x-auto pb-0.5">
            {recommendations.map((item) => (
              <li
                key={item.productId}
                className="flex w-[5.5rem] shrink-0 flex-col rounded-md border border-white/10 bg-gray-900/50 p-1.5"
              >
                <img
                  src={resolveProductImageUrl(item.imageUrl, CustomDesign)}
                  alt={item.name}
                  className="mx-auto h-9 w-9 rounded object-cover"
                />
                <p className="mt-1 line-clamp-2 text-[10px] leading-tight text-gray-300">{item.name}</p>
                <p className="mt-0.5 text-[10px] font-semibold text-primary">
                  €{Number(item.price).toFixed(2)}
                </p>
                <button
                  type="button"
                  onClick={() => void addToCart(item.productId, 1)}
                  className="mt-1 rounded bg-primary px-1 py-0.5 text-[10px] font-semibold hover:bg-primary-focus"
                >
                  Add
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {cartItems.length > 0 && (
        <div className="shrink-0 space-y-2 border-t border-white/10 bg-gray-950/80 px-3 py-3">
          <div className="flex items-center justify-between text-sm font-bold">
            <span>Subtotal</span>
            <span className="text-lg text-primary">€{totalPrice.toFixed(2)}</span>
          </div>
          <button
            type="button"
            onClick={handleCheckout}
            className="w-full rounded-lg bg-primary py-2 text-sm font-semibold text-white hover:bg-primary-focus transition"
          >
            Checkout
          </button>
          <button
            type="button"
            onClick={() => void handleClear()}
            className="w-full rounded-lg border border-white/10 py-1.5 text-xs font-semibold text-gray-300 hover:bg-white/5 transition"
          >
            Clear cart
          </button>
        </div>
      )}
    </div>
  );
};

export default Cart;
