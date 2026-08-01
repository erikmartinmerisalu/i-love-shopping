import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';

interface CartProps {
  onClose?: () => void;
}

const Cart: React.FC<CartProps> = ({ onClose }) => {
  const navigate = useNavigate();
  const {
    cartItems,
    removeFromCart,
    updateQuantity,
    totalPrice,
    clearCart,
    cartError,
  } = useCart();
  const [isOpen, setIsOpen] = useState(true);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleClose = () => {
    setIsOpen(false);
    onClose?.();
  };

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
    handleClose();
    navigate('/checkout');
  };

  if (!isOpen) {
    return null;
  }

  const displayError = actionError || cartError;

  return (
    <div className="fixed right-0 top-0 h-screen w-80 bg-gray-900 shadow-xl border-l border-gray-700 flex flex-col text-white z-50">
      <div className="p-6 border-b border-gray-700 flex justify-between items-center">
        <h2 className="text-2xl font-bold">Shopping Cart</h2>
        <button
          onClick={handleClose}
          className="text-gray-400 hover:text-white text-2xl"
        >
          ✕
        </button>
      </div>

      {displayError && (
        <div className="mx-4 mt-4 rounded-lg border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-200">
          {displayError}
        </div>
      )}

      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {cartItems.length === 0 ? (
          <p className="text-gray-400 text-center py-8">Your cart is empty</p>
        ) : (
          cartItems.map((item) => (
            <div
              key={item.id}
              className="bg-gray-800 rounded-lg p-4 space-y-3"
            >
              <div className="flex gap-3">
                <img
                  src={item.image}
                  alt={item.name}
                  className="w-24 h-24 object-cover rounded opacity-80"
                />
                <div className="flex-1">
                  <h3 className="font-semibold text-sm">{item.name}</h3>
                  <p className="text-primary font-bold text-sm">
                    €{item.price.toFixed(2)}
                  </p>
                  {item.stockQuantity <= 0 ? (
                    <p className="text-xs text-red-400 mt-1">Out of stock</p>
                  ) : (
                    <p className="text-xs text-gray-400 mt-1">
                      {item.stockQuantity} in stock
                    </p>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 bg-gray-700 rounded">
                  <button
                    onClick={() => handleUpdate(item.id, item.quantity - 1)}
                    className="p-2 px-3 hover:bg-gray-600 text-lg"
                  >
                    −
                  </button>
                  <span className="px-3 font-semibold text-base">{item.quantity}</span>
                  <button
                    onClick={() => handleUpdate(item.id, item.quantity + 1)}
                    disabled={item.quantity >= item.stockQuantity}
                    className="p-2 px-3 hover:bg-gray-600 text-lg disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    +
                  </button>
                </div>
                <button
                  onClick={() => handleRemove(item.id)}
                  className="text-red-400 hover:text-red-300 text-sm font-semibold"
                >
                  Remove
                </button>
              </div>

              <div className="border-t border-gray-700 pt-2">
                <p className="text-gray-300 text-sm">
                  Subtotal: €{(item.price * item.quantity).toFixed(2)}
                </p>
              </div>
            </div>
          ))
        )}
      </div>

      {cartItems.length > 0 && (
        <div className="border-t border-gray-700 p-6 space-y-3">
          <div className="flex justify-between items-center text-lg font-bold">
            <span>Total:</span>
            <span className="text-primary text-xl">€{totalPrice.toFixed(2)}</span>
          </div>
          <button
            onClick={handleCheckout}
            className="w-full bg-primary text-white font-semibold py-3 rounded-lg hover:bg-primary-focus transition"
          >
            Checkout
          </button>
          <button
            onClick={handleClear}
            className="w-full bg-gray-700 text-white font-semibold py-2 rounded-lg hover:bg-gray-600 transition"
          >
            Clear Cart
          </button>
        </div>
      )}
    </div>
  );
};

export default Cart;
