export type OrderItemDto = {
  productId: number | null;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  canReview?: boolean;
  reviewStatus?: string | null;
};

export type OrderStatusHistoryDto = {
  status: string;
  note: string | null;
  createdAt: string | null;
};

export type OrderDto = {
  id: number;
  orderNumber: string;
  status: string;
  paymentMethod: string;
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  postalCode: string;
  country: string;
  totalAmount: number;
  shippingAmount?: number | null;
  deliveryOptionId?: number | null;
  deliveryOptionName?: string | null;
  estimatedDeliveryAt?: string | null;
  createdAt: string | null;
  items: OrderItemDto[];
  statusHistory: OrderStatusHistoryDto[];
};

export type CheckoutPayload = {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  postalCode: string;
  country: string;
  paymentMethod: string;
  deliveryOptionId: number;
};

export type CheckoutErrorBody = {
  success?: boolean;
  message?: string;
  fieldErrors?: Record<string, string>;
};

export class CheckoutApiError extends Error {
  fieldErrors: Record<string, string>;

  constructor(message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'CheckoutApiError';
    this.fieldErrors = fieldErrors;
  }
}

export async function placeOrder(
  accessToken: string | null,
  payload: CheckoutPayload
): Promise<OrderDto> {
  let response: Response;
  try {
    response = await fetch('/api/orders', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      credentials: 'include',
      body: JSON.stringify(payload),
    });
  } catch {
    throw new CheckoutApiError('Network error. Please check your connection and try again.');
  }

  const data = (await response.json().catch(() => ({}))) as CheckoutErrorBody & OrderDto;

  if (!response.ok) {
    throw new CheckoutApiError(
      typeof data.message === 'string' && data.message.trim()
        ? data.message
        : 'Could not place order',
      data.fieldErrors ?? {}
    );
  }

  return data as OrderDto;
}

export type DeliveryOptionDto = {
  id: number;
  name: string;
  price: number;
  estimatedDays: number;
  active: boolean;
};

export async function fetchActiveDeliveryOptions(): Promise<DeliveryOptionDto[]> {
  let response: Response;
  try {
    response = await fetch('/api/delivery-options', { credentials: 'include' });
  } catch {
    throw new CheckoutApiError('Network error. Please check your connection and try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new CheckoutApiError(
      typeof data?.message === 'string' ? data.message : 'Could not load shipping options'
    );
  }
  return data as DeliveryOptionDto[];
}

export async function fetchOrders(
  accessToken: string | null,
  params: { status?: string; sort?: string } = {}
): Promise<OrderDto[]> {
  const query = new URLSearchParams();
  if (params.status) {
    query.set('status', params.status);
  }
  if (params.sort) {
    query.set('sort', params.sort);
  }
  const suffix = query.toString() ? `?${query}` : '';

  let response: Response;
  try {
    response = await fetch(`/api/orders${suffix}`, {
      headers: {
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      credentials: 'include',
    });
  } catch {
    throw new CheckoutApiError('Network error. Please check your connection and try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new CheckoutApiError(
      typeof data?.message === 'string' ? data.message : 'Could not load orders'
    );
  }
  return data as OrderDto[];
}

export async function fetchOrder(
  accessToken: string | null,
  orderNumber: string,
  email?: string
): Promise<OrderDto> {
  const query = email ? `?email=${encodeURIComponent(email)}` : '';
  let response: Response;
  try {
    response = await fetch(`/api/orders/${encodeURIComponent(orderNumber)}${query}`, {
      headers: {
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      credentials: 'include',
    });
  } catch {
    throw new CheckoutApiError('Network error. Please check your connection and try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new CheckoutApiError(
      typeof data?.message === 'string' ? data.message : 'Could not load order'
    );
  }
  return data as OrderDto;
}

export async function cancelOrder(
  accessToken: string | null,
  orderNumber: string,
  email?: string
): Promise<OrderDto> {
  const query = email ? `?email=${encodeURIComponent(email)}` : '';
  let response: Response;
  try {
    response = await fetch(`/api/orders/${encodeURIComponent(orderNumber)}/cancel${query}`, {
      method: 'POST',
      headers: {
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      credentials: 'include',
    });
  } catch {
    throw new CheckoutApiError('Network error. Please check your connection and try again.');
  }

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new CheckoutApiError(
      typeof data?.message === 'string' ? data.message : 'Could not cancel order'
    );
  }
  return data as OrderDto;
}
