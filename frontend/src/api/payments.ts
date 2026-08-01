import type { OrderDto } from './orders';

export type PaymentIntentResponse = {
  orderNumber: string;
  mode: 'stripe' | 'sandbox';
  publishableKey?: string | null;
  clientSecret?: string | null;
  providerPaymentId: string;
  amount: number;
  currency: string;
  paymentMethod: string;
};

export type PaymentResultResponse = {
  success: boolean;
  orderNumber: string;
  orderStatus: string;
  paymentStatus: string;
  failureCode?: string | null;
  message: string;
  order: OrderDto;
};

export class PaymentApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'PaymentApiError';
  }
}

async function parseJson(response: Response) {
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new PaymentApiError(
      typeof data?.message === 'string' && data.message.trim()
        ? data.message
        : 'Payment request failed'
    );
  }
  return data;
}

export async function createPaymentIntent(orderNumber: string): Promise<PaymentIntentResponse> {
  let response: Response;
  try {
    response = await fetch('/api/payments/intent', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ orderNumber }),
    });
  } catch {
    throw new PaymentApiError('Network error. Please check your connection and try again.');
  }
  return parseJson(response);
}

export async function confirmSandboxPayment(
  orderNumber: string,
  scenario: string
): Promise<PaymentResultResponse> {
  let response: Response;
  try {
    response = await fetch('/api/payments/sandbox/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ orderNumber, scenario }),
    });
  } catch {
    throw new PaymentApiError('Network error. Please check your connection and try again.');
  }
  return parseJson(response);
}

export async function syncStripePayment(orderNumber: string): Promise<PaymentResultResponse> {
  let response: Response;
  try {
    response = await fetch('/api/payments/stripe/sync', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ orderNumber }),
    });
  } catch {
    throw new PaymentApiError('Network error. Please check your connection and try again.');
  }
  return parseJson(response);
}
