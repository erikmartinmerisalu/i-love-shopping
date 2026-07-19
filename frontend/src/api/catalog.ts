import type { Category, ProductListResponse, ProductSearchParams } from '../types/catalog';

const buildQuery = (params: ProductSearchParams): string => {
  const query = new URLSearchParams();

  if (params.search?.trim()) {
    query.set('search', params.search.trim());
  }
  if (params.category) {
    query.set('category', params.category);
  }
  if (params.brand) {
    query.set('brand', params.brand);
  }
  if (params.minPrice != null) {
    query.set('minPrice', String(params.minPrice));
  }
  if (params.maxPrice != null) {
    query.set('maxPrice', String(params.maxPrice));
  }
  if (params.sort) {
    query.set('sort', params.sort);
  }
  if (params.page != null) {
    query.set('page', String(params.page));
  }
  if (params.size != null) {
    query.set('size', String(params.size));
  }

  const serialized = query.toString();
  return serialized ? `?${serialized}` : '';
};

export async function fetchProducts(params: ProductSearchParams = {}): Promise<ProductListResponse> {
  const response = await fetch(`/api/products${buildQuery(params)}`);
  if (!response.ok) {
    throw new Error('Failed to load products');
  }
  return response.json();
}

export async function fetchCategories(): Promise<Category[]> {
  const response = await fetch('/api/categories');
  if (!response.ok) {
    throw new Error('Failed to load categories');
  }
  return response.json();
}
