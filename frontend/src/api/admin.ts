import type {
  AdminCategoryPayload,
  AdminProductPayload,
  AdminRefundPayload,
} from '../types/admin';
import type { Category, ProductDetail, ProductListResponse } from '../types/catalog';
import type { Review } from '../types/reviews';
import type { OrderDto } from './orders';

const API_BASE = '/api';

const authHeaders = (token: string | null): HeadersInit => {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
};

export type AdminDashboard = {
  totalProducts: number;
  activeProducts: number;
  lowStockProducts: number;
  totalOrders: number;
  pendingOrders: number;
  totalUsers: number;
  adminUsers: number;
};

export type AdminUser = {
  id: number;
  email: string;
  username: string;
  role: string;
  enabled: boolean;
  twoFactorEnabled: boolean;
  createdAt: string;
};

export type DeliveryOption = {
  id: number;
  name: string;
  price: number;
  estimatedDays: number;
  active: boolean;
};

export type BulkUploadResult = {
  created: number;
  updated: number;
  skipped: number;
  errors: string[];
};

export type RefundResult = {
  id: number;
  orderId: number;
  amount: number;
  reason: string | null;
  status: string;
  createdAt: string;
};

async function adminFetch<T>(path: string, token: string | null, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      ...authHeaders(token),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message ?? 'Admin request failed');
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const fetchAdminDashboard = (token: string | null) =>
  adminFetch<AdminDashboard>('/admin/dashboard', token);

export const fetchAdminUsers = (token: string | null, page = 0, size = 20) =>
  adminFetch<AdminUser[]>(`/admin/users?page=${page}&size=${size}`, token);

export const updateAdminUser = (
  token: string | null,
  id: number,
  payload: { role?: string; enabled?: boolean },
) =>
  adminFetch<AdminUser>(`/admin/users/${id}`, token, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const fetchAdminOrders = (token: string | null, page = 0, size = 20, status?: string) => {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) {
    query.set('status', status);
  }
  return adminFetch<OrderDto[]>(`/admin/orders?${query}`, token);
};

export const fetchAdminOrder = (token: string | null, id: number) =>
  adminFetch<OrderDto>(`/admin/orders/${id}`, token);

export const updateAdminOrder = (
  token: string | null,
  id: number,
  payload: { status: string; deliveryOptionId?: number },
) =>
  adminFetch<OrderDto>(`/admin/orders/${id}`, token, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const createAdminRefund = (token: string | null, orderId: number, payload: AdminRefundPayload) =>
  adminFetch<RefundResult>(`/admin/orders/${orderId}/refunds`, token, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const fetchDeliveryOptions = (token: string | null) =>
  adminFetch<DeliveryOption[]>('/admin/delivery-options', token);

export const fetchAdminProducts = (token: string | null, page = 0, size = 20) =>
  adminFetch<ProductListResponse>(`/admin/products?page=${page}&size=${size}`, token);

export const fetchAdminProduct = (token: string | null, id: number) =>
  adminFetch<ProductDetail>(`/admin/products/${id}`, token);

export const createAdminProduct = (token: string | null, payload: AdminProductPayload) =>
  adminFetch<ProductDetail>('/admin/products', token, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const updateAdminProduct = (token: string | null, id: number, payload: AdminProductPayload) =>
  adminFetch<ProductDetail>(`/admin/products/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

export const deleteAdminProduct = (token: string | null, id: number) =>
  adminFetch<void>(`/admin/products/${id}`, token, { method: 'DELETE' });

export const uploadAdminProductImage = async (
  token: string | null,
  productId: number,
  file: File,
  makePrimary = false,
) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(
    `${API_BASE}/admin/products/${productId}/images?makePrimary=${makePrimary}`,
    {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: formData,
    },
  );

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message ?? 'Image upload failed');
  }

  return response.json();
};

export const fetchAdminCategories = (token: string | null) =>
  adminFetch<Category[]>(`/admin/categories`, token);

export const createAdminCategory = (token: string | null, payload: AdminCategoryPayload) =>
  adminFetch<Category>('/admin/categories', token, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const updateAdminCategory = (token: string | null, id: number, payload: AdminCategoryPayload) =>
  adminFetch<Category>(`/admin/categories/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

export const deleteAdminCategory = (token: string | null, id: number) =>
  adminFetch<void>(`/admin/categories/${id}`, token, { method: 'DELETE' });

export const bulkUploadProducts = async (token: string | null, file: File): Promise<BulkUploadResult> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_BASE}/admin/products/bulk`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(body.message ?? 'Bulk upload failed');
  }

  return response.json();
};

export const fetchAdminReviews = (token: string | null, status = 'PENDING') =>
  adminFetch<Review[]>(`/admin/reviews?status=${encodeURIComponent(status)}`, token);

export const approveAdminReview = (token: string | null, id: number) =>
  adminFetch<Review>(`/admin/reviews/${id}/approve`, token, { method: 'POST' });

export const rejectAdminReview = (token: string | null, id: number) =>
  adminFetch<Review>(`/admin/reviews/${id}/reject`, token, { method: 'POST' });
