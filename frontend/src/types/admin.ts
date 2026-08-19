export type AdminProductPayload = {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  brand: string;
  categoryId: number;
  sku?: string;
  active: boolean;
  featured: boolean;
  weightKg?: number | null;
  weightLb?: number | null;
  lengthCm?: number | null;
  lengthIn?: number | null;
  widthCm?: number | null;
  widthIn?: number | null;
  heightCm?: number | null;
  heightIn?: number | null;
};

export type AdminCategoryPayload = {
  name: string;
  slug: string;
  description?: string;
};

export type AdminRefundPayload = {
  amount: number;
  reason?: string;
};

export const ORDER_STATUSES = [
  'PENDING_PAYMENT',
  'PAID',
  'SHIPPED',
  'FULFILLED',
  'REFUNDED',
  'FAILED',
  'CANCELLED',
] as const;

export type OrderStatusValue = (typeof ORDER_STATUSES)[number];
