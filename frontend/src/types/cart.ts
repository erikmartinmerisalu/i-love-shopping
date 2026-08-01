export type CartItemDto = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  stockQuantity: number;
  imageUrl: string | null;
  lineTotal: number;
};

export type CartDto = {
  items: CartItemDto[];
  totalPrice: number;
  totalItems: number;
  guest: boolean;
};
