import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchProducts } from '../api/catalog';
import { useCart } from '../context/CartContext';
import DualRangeSlider from './DualRangeSlider';
import ProductCard from './ProductCard';
import type { Product, ProductFacets } from '../types/catalog';
import { SORT_OPTIONS } from '../types/catalog';

type CatalogBrowsePageProps = {
  mode: 'catalog' | 'search';
};

export default function CatalogBrowsePage({ mode }: CatalogBrowsePageProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const { addToCart, cartItems, clearCartError } = useCart();

  const initialQuery = mode === 'search' ? (searchParams.get('q') ?? '') : (searchParams.get('search') ?? '');
  const initialCategory = searchParams.get('category');

  const [products, setProducts] = useState<Product[]>([]);
  const [facets, setFacets] = useState<ProductFacets | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [searchInput, setSearchInput] = useState(initialQuery);
  const [debouncedSearch, setDebouncedSearch] = useState(initialQuery);
  const [selectedCategorySlug, setSelectedCategorySlug] = useState<string | null>(initialCategory);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(searchParams.get('brand'));
  const [sort, setSort] = useState(searchParams.get('sort') ?? 'relevance');
  const [page, setPage] = useState(Number(searchParams.get('page') ?? '0'));
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [priceRange, setPriceRange] = useState<[number, number] | null>(null);
  const [debouncedPriceRange, setDebouncedPriceRange] = useState<[number, number] | null>(null);
  const [catalogPriceBounds, setCatalogPriceBounds] = useState<{ min: number; max: number } | null>(null);

  const [notification, setNotification] = useState('');
  const [showToast, setShowToast] = useState(false);
  const [brokenImageIds, setBrokenImageIds] = useState<Set<number>>(() => new Set());
  const hideToastTimeout = useRef<number | null>(null);
  const isFirstLoad = useRef(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedPriceRange(priceRange), 300);
    return () => window.clearTimeout(timer);
  }, [priceRange]);

  useEffect(() => {
    if (mode === 'search') {
      const params = new URLSearchParams();
      if (debouncedSearch) {
        params.set('q', debouncedSearch);
      }
      if (selectedCategorySlug) {
        params.set('category', selectedCategorySlug);
      }
      if (selectedBrand) {
        params.set('brand', selectedBrand);
      }
      if (sort !== 'relevance') {
        params.set('sort', sort);
      }
      if (page > 0) {
        params.set('page', String(page));
      }
      setSearchParams(params, { replace: true });
    }
  }, [debouncedSearch, selectedCategorySlug, selectedBrand, sort, page, mode, setSearchParams]);

  const boundsMin = catalogPriceBounds?.min ?? facets?.minPrice ?? 0;
  const boundsMax = catalogPriceBounds?.max ?? facets?.maxPrice ?? 100;
  const activeMin = priceRange?.[0] ?? boundsMin;
  const activeMax = priceRange?.[1] ?? boundsMax;

  const loadProducts = useCallback(async () => {
    if (isFirstLoad.current) {
      setLoading(true);
    }
    setError('');
    try {
      const response = await fetchProducts({
        search: debouncedSearch || undefined,
        category: selectedCategorySlug ?? undefined,
        brand: selectedBrand ?? undefined,
        minPrice: debouncedPriceRange?.[0],
        maxPrice: debouncedPriceRange?.[1],
        sort,
        page,
        size: 12,
      });
      setProducts(response.products);
      setFacets(response.facets);
      setTotalElements(response.totalElements);
      setTotalPages(response.totalPages);
      setCatalogPriceBounds((current) =>
        current ?? { min: response.facets.minPrice, max: response.facets.maxPrice }
      );
    } catch {
      setError('Could not load products. Make sure the backend is running.');
      setProducts([]);
    } finally {
      setLoading(false);
      isFirstLoad.current = false;
    }
  }, [debouncedSearch, selectedCategorySlug, selectedBrand, debouncedPriceRange, sort, page]);

  useEffect(() => {
    void loadProducts();
  }, [loadProducts]);

  const showToastMessage = (message: string) => {
    if (hideToastTimeout.current !== null) {
      window.clearTimeout(hideToastTimeout.current);
    }
    setNotification(message);
    setShowToast(true);
    hideToastTimeout.current = window.setTimeout(() => setShowToast(false), 2000);
  };

  const handleAddToCart = async (product: Product) => {
    clearCartError();
    if (product.stockQuantity <= 0) {
      showToastMessage('Out of stock');
      return;
    }
    const existingItem = cartItems.find((item) => item.id === product.id);
    const currentQty = existingItem?.quantity ?? 0;
    if (currentQty >= product.stockQuantity) {
      showToastMessage(`Only ${product.stockQuantity} left in stock`);
      return;
    }
    try {
      await addToCart(product.id);
      showToastMessage(`${product.name} added to cart`);
    } catch (err) {
      showToastMessage(err instanceof Error ? err.message : 'Could not add to cart');
    }
  };

  const clearFilters = () => {
    setSearchInput('');
    setSelectedCategorySlug(null);
    setSelectedBrand(null);
    setPriceRange(null);
    setSort('relevance');
    setPage(0);
  };

  return (
    <div className="page-container py-8 sm:py-10">
      <header className="mb-8">
        <h1 className="text-3xl font-bold">{mode === 'search' ? 'Search results' : 'Shop all products'}</h1>
        <p className="mt-2 text-sm text-gray-400">
          {loading ? 'Loading…' : `${totalElements} product${totalElements === 1 ? '' : 's'} found`}
          {mode === 'search' && debouncedSearch ? ` for “${debouncedSearch}”` : ''}
        </p>
      </header>

      <div className="flex flex-col gap-8 lg:flex-row">
        <aside className="lg:w-72 xl:w-80 shrink-0">
          <div className="surface-panel sticky top-28 max-h-[calc(100vh-7rem)] space-y-6 overflow-y-auto p-5">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-300">Categories</h2>
              <div className="mt-3 space-y-1">
                <button
                  type="button"
                  onClick={() => {
                    setSelectedCategorySlug(null);
                    setPage(0);
                  }}
                  className={`w-full rounded px-3 py-2 text-left text-sm ${
                    selectedCategorySlug === null ? 'bg-primary text-white' : 'hover:bg-gray-800'
                  }`}
                >
                  All
                </button>
                {(facets?.categories ?? []).map((category) => (
                  <button
                    key={category.slug}
                    type="button"
                    onClick={() => {
                      setSelectedCategorySlug(category.slug);
                      setPage(0);
                    }}
                    className={`w-full rounded px-3 py-2 text-left text-sm ${
                      selectedCategorySlug === category.slug ? 'bg-primary text-white' : 'hover:bg-gray-800'
                    }`}
                  >
                    {category.name}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-300">Brand</h2>
              <div className="mt-3 space-y-2">
                {(facets?.brands ?? []).map((brand) => (
                  <label key={brand} className="flex items-center gap-2 text-sm cursor-pointer">
                    <input
                      type="checkbox"
                      checked={selectedBrand === brand}
                      onChange={() => {
                        setSelectedBrand(selectedBrand === brand ? null : brand);
                        setPage(0);
                      }}
                      className="rounded border-gray-600 bg-gray-800 text-primary"
                    />
                    {brand}
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-300">Price</h2>
              <div className="mt-3">
                <DualRangeSlider
                  min={boundsMin}
                  max={boundsMax}
                  valueMin={activeMin}
                  valueMax={activeMax}
                  step={1}
                  onChange={(nextMin, nextMax) => {
                    setPriceRange([Math.min(nextMin, nextMax), Math.max(nextMin, nextMax)]);
                    setPage(0);
                  }}
                  formatLabel={(value) => `€${value.toFixed(0)}`}
                />
              </div>
            </div>

            <button
              type="button"
              onClick={clearFilters}
              className="w-full rounded-lg border border-gray-700 py-2 text-sm hover:bg-gray-800"
            >
              Clear filters
            </button>
          </div>
        </aside>

        <div className="min-w-0 flex-1">
          <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center">
            {mode === 'catalog' && (
              <input
                type="search"
                value={searchInput}
                onChange={(event) => {
                  setSearchInput(event.target.value);
                  setPage(0);
                }}
                placeholder="Filter within catalog…"
                className="flex-1 rounded-lg border border-gray-700 bg-gray-950 px-4 py-2.5 text-sm focus:border-primary focus:outline-none"
              />
            )}
            <select
              value={sort}
              onChange={(event) => {
                setSort(event.target.value);
                setPage(0);
              }}
              className="rounded-lg border border-gray-700 bg-gray-950 px-4 py-2.5 text-sm sm:w-52"
              aria-label="Sort products"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <div className="flex rounded-lg border border-gray-700 p-1" role="group" aria-label="View mode">
              <button
                type="button"
                onClick={() => setViewMode('grid')}
                className={`rounded px-3 py-1.5 text-sm ${viewMode === 'grid' ? 'bg-primary text-white' : ''}`}
              >
                Grid
              </button>
              <button
                type="button"
                onClick={() => setViewMode('list')}
                className={`rounded px-3 py-1.5 text-sm ${viewMode === 'list' ? 'bg-primary text-white' : ''}`}
              >
                List
              </button>
            </div>
          </div>

          {mode === 'search' && (
            <input
              type="search"
              value={searchInput}
              onChange={(event) => {
                setSearchInput(event.target.value);
                setPage(0);
              }}
              placeholder="Search query"
              className="mb-4 w-full rounded-lg border border-gray-700 bg-gray-950 px-4 py-2.5 text-sm focus:border-primary focus:outline-none"
            />
          )}

          <div
            className={`fixed top-24 left-1/2 z-50 -translate-x-1/2 transition ${
              showToast ? 'opacity-100' : 'opacity-0 pointer-events-none'
            }`}
            role="status"
          >
            {notification && (
              <div className="rounded-lg bg-green-600 px-4 py-2 text-sm shadow-lg">{notification}</div>
            )}
          </div>

          {error && (
            <div className="mb-6 rounded-lg border border-red-700 bg-red-900/40 px-4 py-3 text-red-200">{error}</div>
          )}

          {loading && products.length === 0 ? (
            <p className="py-12 text-center text-gray-400">Loading catalog…</p>
          ) : (
            <div
              className={
                viewMode === 'grid'
                  ? 'grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5'
                  : 'flex flex-col gap-4'
              }
            >
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  layout={viewMode}
                  onAddToCart={(item) => void handleAddToCart(item)}
                  brokenImage={brokenImageIds.has(product.id)}
                  onImageError={() =>
                    setBrokenImageIds((current) => new Set(current).add(product.id))
                  }
                />
              ))}
            </div>
          )}

          {!loading && products.length === 0 && !error && (
            <p className="py-12 text-center text-gray-400">No products match your filters</p>
          )}

          {totalPages > 1 && (
            <nav className="mt-10 flex items-center justify-center gap-2" aria-label="Pagination">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => setPage((current) => Math.max(0, current - 1))}
                className="rounded-lg border border-gray-700 px-4 py-2 text-sm disabled:opacity-40"
              >
                Previous
              </button>
              <span className="px-3 text-sm text-gray-400">
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((current) => current + 1)}
                className="rounded-lg border border-gray-700 px-4 py-2 text-sm disabled:opacity-40"
              >
                Next
              </button>
            </nav>
          )}
        </div>
      </div>
    </div>
  );
}
