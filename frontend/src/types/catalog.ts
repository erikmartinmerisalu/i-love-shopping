export type Category = {
  id: number;
  name: string;
  slug: string;
  description: string;
};

export type Product = {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  brand: string;
  rating: number;
  category: string;
  categorySlug: string;
  primaryImageUrl: string | null;
};

export type ProductFacets = {
  categories: Category[];
  brands: string[];
  minPrice: number;
  maxPrice: number;
};

export type ProductListResponse = {
  products: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  facets: ProductFacets;
};

export type ProductSearchParams = {
  search?: string;
  category?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  sort?: string;
  page?: number;
  size?: number;
};

export const SORT_OPTIONS = [
  { value: 'relevance', label: 'Relevance' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
  { value: 'rating', label: 'Rating' },
  { value: 'name', label: 'Name' },
] as const;
