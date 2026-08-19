import type {
  HelpfulVoteResponse,
  Review,
  ReviewEligibleOrder,
  ReviewListResponse,
  SubmitReviewPayload,
} from '../types/reviews';

const API_BASE = '/api';

const authHeaders = (token: string | null): HeadersInit => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
};

async function parseError(response: Response): Promise<string> {
  const body = await response.json().catch(() => ({ message: response.statusText }));
  return body.message ?? 'Request failed';
}

export async function fetchProductReviews(
  productId: number,
  sort: 'helpful' | 'recent' = 'helpful',
): Promise<ReviewListResponse> {
  const response = await fetch(`${API_BASE}/products/${productId}/reviews?sort=${sort}`);
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json() as Promise<ReviewListResponse>;
}

export async function fetchEligibleReviewOrders(
  token: string | null,
  productId: number,
): Promise<ReviewEligibleOrder[]> {
  const response = await fetch(`${API_BASE}/reviews/eligible?productId=${productId}`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json() as Promise<ReviewEligibleOrder[]>;
}

export async function submitReview(
  token: string | null,
  payload: SubmitReviewPayload,
): Promise<Review> {
  const response = await fetch(`${API_BASE}/reviews`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json() as Promise<Review>;
}

export async function toggleReviewHelpful(
  token: string | null,
  reviewId: number,
): Promise<HelpfulVoteResponse> {
  const response = await fetch(`${API_BASE}/reviews/${reviewId}/helpful`, {
    method: 'POST',
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new Error(await parseError(response));
  }
  return response.json() as Promise<HelpfulVoteResponse>;
}
