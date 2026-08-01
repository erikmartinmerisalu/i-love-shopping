import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import {
  addCartItem,
  clearCartApi,
  fetchCart,
  mergeGuestCart,
  removeCartItem,
  updateCartItem,
  CartApiError,
} from '../api/cart';
import { useAuth } from './AuthContext';
import type { CartItemDto } from '../types/cart';
import { resolveProductImageUrl } from '../utils/productImageUrl';
import CustomDesign from '../assets/Custom_Design.png';

export interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  stockQuantity: number;
  image: string;
}

interface CartContextType {
  cartItems: CartItem[];
  addToCart: (productId: number, quantity?: number) => Promise<void>;
  removeFromCart: (productId: number) => Promise<void>;
  updateQuantity: (productId: number, quantity: number) => Promise<void>;
  clearCart: () => Promise<void>;
  refreshCart: () => Promise<void>;
  totalPrice: number;
  totalItems: number;
  cartError: string | null;
  clearCartError: () => void;
  isCartLoading: boolean;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

const mapItems = (items: CartItemDto[]): CartItem[] =>
  items.map((item) => ({
    id: item.productId,
    name: item.name,
    price: Number(item.price),
    quantity: item.quantity,
    stockQuantity: item.stockQuantity,
    image: resolveProductImageUrl(item.imageUrl, CustomDesign),
  }));

export const CartProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { token, isAuthenticated, isGuest, isLoading: authLoading } = useAuth();
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [cartError, setCartError] = useState<string | null>(null);
  const [isCartLoading, setIsCartLoading] = useState(false);
  const mergedForToken = useRef<string | null>(null);

  const applyCart = useCallback((items: CartItemDto[], nextTotalPrice: number, nextTotalItems: number) => {
    setCartItems(mapItems(items));
    setTotalPrice(Number(nextTotalPrice));
    setTotalItems(nextTotalItems);
  }, []);

  const refreshCart = useCallback(async () => {
    setIsCartLoading(true);
    try {
      const cart = await fetchCart(token);
      applyCart(cart.items, cart.totalPrice, cart.totalItems);
      setCartError(null);
    } catch (error) {
      const message = error instanceof CartApiError ? error.message : 'Failed to load cart';
      setCartError(message);
    } finally {
      setIsCartLoading(false);
    }
  }, [token, applyCart]);

  useEffect(() => {
    if (authLoading) {
      return;
    }

    const sync = async () => {
      if (isAuthenticated && token) {
        if (mergedForToken.current !== token) {
          try {
            const cart = await mergeGuestCart(token);
            mergedForToken.current = token;
            applyCart(cart.items, cart.totalPrice, cart.totalItems);
            setCartError(null);
            return;
          } catch {
            mergedForToken.current = token;
          }
        }
      } else {
        mergedForToken.current = null;
      }

      if (isAuthenticated || isGuest) {
        await refreshCart();
      } else {
        setCartItems([]);
        setTotalPrice(0);
        setTotalItems(0);
      }
    };

    void sync();
  }, [authLoading, isAuthenticated, isGuest, token, refreshCart, applyCart]);

  const addToCart = async (productId: number, quantity = 1) => {
    setCartError(null);
    try {
      const cart = await addCartItem(token, productId, quantity);
      applyCart(cart.items, cart.totalPrice, cart.totalItems);
    } catch (error) {
      const message = error instanceof CartApiError ? error.message : 'Could not add to cart';
      setCartError(message);
      throw error;
    }
  };

  const removeFromCart = async (productId: number) => {
    setCartError(null);
    try {
      const cart = await removeCartItem(token, productId);
      applyCart(cart.items, cart.totalPrice, cart.totalItems);
    } catch (error) {
      const message = error instanceof CartApiError ? error.message : 'Could not remove item';
      setCartError(message);
      throw error;
    }
  };

  const updateQuantity = async (productId: number, quantity: number) => {
    setCartError(null);
    try {
      const cart = await updateCartItem(token, productId, quantity);
      applyCart(cart.items, cart.totalPrice, cart.totalItems);
    } catch (error) {
      const message = error instanceof CartApiError ? error.message : 'Could not update quantity';
      setCartError(message);
      throw error;
    }
  };

  const clearCart = async () => {
    setCartError(null);
    try {
      const cart = await clearCartApi(token);
      applyCart(cart.items, cart.totalPrice, cart.totalItems);
    } catch (error) {
      const message = error instanceof CartApiError ? error.message : 'Could not clear cart';
      setCartError(message);
      throw error;
    }
  };

  return (
    <CartContext.Provider
      value={{
        cartItems,
        addToCart,
        removeFromCart,
        updateQuantity,
        clearCart,
        refreshCart,
        totalPrice,
        totalItems,
        cartError,
        clearCartError: () => setCartError(null),
        isCartLoading,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within CartProvider');
  }
  return context;
};
