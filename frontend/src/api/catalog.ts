import type {
  HomeData,
  ProductDetail,
  ProductListResponse,
  ProductSearchParams,
  ProductSuggestion,
  Product,
} from '../types/catalog';

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

export async function fetchProduct(id: number): Promise<ProductDetail> {
  const response = await fetch(`/api/products/${id}`);
  if (!response.ok) {
    throw new Error('Product not found');
  }
  return response.json();
}

export async function fetchRelatedProducts(id: number, limit = 4): Promise<Product[]> {
  const response = await fetch(`/api/products/${id}/related?limit=${limit}`);
  if (!response.ok) {
    return [];
  }
  return response.json();
}

export async function fetchProductSuggestions(query: string, limit = 8): Promise<ProductSuggestion[]> {
  const params = new URLSearchParams({ q: query, limit: String(limit) });
  const response = await fetch(`/api/products/suggest?${params}`);
  if (!response.ok) {
    return [];
  }
  return response.json();
}

export async function fetchHome(): Promise<HomeData> {
  const response = await fetch('/api/home');
  if (!response.ok) {
    throw new Error('Failed to load home page');
  }
  return response.json();
}

export async function fetchCategories() {
  const response = await fetch('/api/categories');
  if (!response.ok) {
    throw new Error('Failed to load categories');
  }
  return response.json();
}
