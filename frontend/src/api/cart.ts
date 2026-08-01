import type { CartDto } from '../types/cart';

export class CartApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CartApiError';
  }
}

const cartHeaders = (accessToken: string | null): HeadersInit => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }
  return headers;
};

async function parseCartResponse(response: Response): Promise<CartDto> {
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message =
      typeof data?.message === 'string' && data.message.trim()
        ? data.message
        : 'Cart request failed';
    throw new CartApiError(message);
  }
  return data as CartDto;
}

export async function fetchCart(accessToken: string | null): Promise<CartDto> {
  const response = await fetch('/api/cart', {
    method: 'GET',
    headers: cartHeaders(accessToken),
    credentials: 'include',
  });
  return parseCartResponse(response);
}

export async function addCartItem(
  accessToken: string | null,
  productId: number,
  quantity = 1
): Promise<CartDto> {
  const response = await fetch('/api/cart/items', {
    method: 'POST',
    headers: cartHeaders(accessToken),
    credentials: 'include',
    body: JSON.stringify({ productId, quantity }),
  });
  return parseCartResponse(response);
}

export async function updateCartItem(
  accessToken: string | null,
  productId: number,
  quantity: number
): Promise<CartDto> {
  const response = await fetch(`/api/cart/items/${productId}`, {
    method: 'PUT',
    headers: cartHeaders(accessToken),
    credentials: 'include',
    body: JSON.stringify({ quantity }),
  });
  return parseCartResponse(response);
}

export async function removeCartItem(
  accessToken: string | null,
  productId: number
): Promise<CartDto> {
  const response = await fetch(`/api/cart/items/${productId}`, {
    method: 'DELETE',
    headers: cartHeaders(accessToken),
    credentials: 'include',
  });
  return parseCartResponse(response);
}

export async function clearCartApi(accessToken: string | null): Promise<CartDto> {
  const response = await fetch('/api/cart', {
    method: 'DELETE',
    headers: cartHeaders(accessToken),
    credentials: 'include',
  });
  return parseCartResponse(response);
}

export async function mergeGuestCart(accessToken: string): Promise<CartDto> {
  const response = await fetch('/api/cart/merge', {
    method: 'POST',
    headers: cartHeaders(accessToken),
    credentials: 'include',
  });
  return parseCartResponse(response);
}
