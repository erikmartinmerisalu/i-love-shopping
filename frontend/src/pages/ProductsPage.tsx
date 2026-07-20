import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { fetchProducts } from '../api/catalog';
import Cart from '../components/Cart';
import DualRangeSlider from '../components/DualRangeSlider';
import CustomDesign from '../assets/Custom_Design.png';
import type { Product, ProductFacets } from '../types/catalog';
import { SORT_OPTIONS } from '../types/catalog';
import { resolveProductImageUrl } from '../utils/productImageUrl';

const productImageUrl = (url: string | null | undefined): string =>
  resolveProductImageUrl(url, CustomDesign);

const ProductsPage = () => {
  const navigate = useNavigate();
  const { addToCart, totalItems, cartItems } = useCart();
  const { logout, user, isGuest } = useAuth();
  const [cartOpen, setCartOpen] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [facets, setFacets] = useState<ProductFacets | null>(null);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedCategorySlug, setSelectedCategorySlug] = useState<string | null>(null);
  const [selectedBrand, setSelectedBrand] = useState<string | null>(null);
  const [sort, setSort] = useState('relevance');
  const [priceRange, setPriceRange] = useState<[number, number] | null>(null);
  const [debouncedPriceRange, setDebouncedPriceRange] = useState<[number, number] | null>(null);
  const [catalogPriceBounds, setCatalogPriceBounds] = useState<{ min: number; max: number } | null>(
    null
  );

  const [notification, setNotification] = useState('');
  const [showToast, setShowToast] = useState(false);
  const [brokenImageIds, setBrokenImageIds] = useState<Set<number>>(() => new Set());
  const hideToastTimeout = useRef<number | null>(null);
  const clearNotificationTimeout = useRef<number | null>(null);
  const isFirstLoad = useRef(true);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedPriceRange(priceRange), 300);
    return () => window.clearTimeout(timer);
  }, [priceRange]);

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
      });
      setProducts(response.products);
      setFacets(response.facets);
      setTotalElements(response.totalElements);
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
  }, [debouncedSearch, selectedCategorySlug, selectedBrand, debouncedPriceRange, sort]);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  useEffect(() => {
    return () => {
      if (hideToastTimeout.current !== null) {
        window.clearTimeout(hideToastTimeout.current);
      }
      if (clearNotificationTimeout.current !== null) {
        window.clearTimeout(clearNotificationTimeout.current);
      }
    };
  }, []);

  const handleAddToCart = (product: Product) => {
    const existingItem = cartItems.find((item) => item.id === product.id);
    const newQuantity = existingItem ? existingItem.quantity + 1 : 1;

    addToCart({
      id: product.id,
      name: product.name,
      price: product.price,
      image: productImageUrl(product.primaryImageUrl),
    });

    if (hideToastTimeout.current !== null) {
      window.clearTimeout(hideToastTimeout.current);
    }
    if (clearNotificationTimeout.current !== null) {
      window.clearTimeout(clearNotificationTimeout.current);
    }

    setNotification(
      newQuantity > 1
        ? `${product.name} added to cart! (${newQuantity})`
        : `${product.name} added to cart!`
    );
    setShowToast(true);

    hideToastTimeout.current = window.setTimeout(() => {
      setShowToast(false);
      clearNotificationTimeout.current = window.setTimeout(() => {
        setNotification('');
      }, 300);
    }, 2000);
  };

  const clearFilters = () => {
    setSearchInput('');
    setSelectedCategorySlug(null);
    setSelectedBrand(null);
    setPriceRange(null);
    setSort('relevance');
  };

  const handlePriceRangeChange = (nextMin: number, nextMax: number) => {
    const low = Math.min(nextMin, nextMax);
    const high = Math.max(nextMin, nextMax);

    setPriceRange((current) => {
      if (low <= boundsMin && high >= boundsMax) {
        return current === null ? current : null;
      }

      if (current && current[0] === low && current[1] === high) {
        return current;
      }

      return [low, high];
    });
  };

  const handleAuthAction = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gray-950 text-white">
      <header className="bg-gray-900 border-b border-gray-800 sticky top-0 z-40">
        <div className="flex justify-between items-center px-4 sm:px-6 lg:px-10 xl:px-16 py-4 w-full">
          <div>
            <h1 className="text-3xl lg:text-4xl font-bold text-primary">ESTValgus</h1>
            {user && (
              <div className="mt-1 flex flex-wrap items-center gap-2 sm:gap-3">
                <p className="text-sm text-slate-300">Signed in as {user.username}</p>
                {!isGuest && (
                  <button
                    onClick={() => navigate("/profile")}
                    className="bg-gray-800 text-white px-3 py-1.5 rounded-lg text-sm hover:bg-gray-700 transition"
                  >
                    Profile
                  </button>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setCartOpen(true)}
              className="relative bg-primary text-white px-5 py-2 lg:px-6 lg:py-3 rounded-lg hover:bg-primary-focus transition font-semibold flex items-center gap-2"
            >
              🛒 Cart
              {totalItems > 0 && (
                <span className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full w-6 h-6 flex items-center justify-center text-sm font-bold">
                  {totalItems}
                </span>
              )}
            </button>
            <button
              onClick={handleAuthAction}
              className="bg-rose-700 text-white px-4 py-2 rounded-lg hover:bg-rose-600 transition font-semibold border border-rose-500/40 shadow-sm shadow-rose-900/30"
            >
              {isGuest ? "Sign in" : "Logout"}
            </button>
          </div>
        </div>
      </header>

      <div className={`flex gap-6 py-8 px-4 sm:px-6 lg:px-10 xl:px-16 ${cartOpen ? 'pr-80' : ''}`}>
        <div className="flex-none w-[14rem] sm:w-[16rem] md:w-[18rem] lg:w-[20rem] xl:w-[22rem]">
          <div className="bg-gray-900 rounded-lg p-6 border border-gray-800 sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto space-y-8">
            <div>
              <h2 className="text-xl sm:text-2xl font-bold mb-4 tracking-wide">Categories</h2>
              <div className="grid grid-cols-1 gap-2">
                <button
                  onClick={() => setSelectedCategorySlug(null)}
                  className={`w-full text-left rounded transition px-4 py-3 text-sm sm:text-base ${
                    selectedCategorySlug === null
                      ? 'bg-primary text-white'
                      : 'bg-gray-800 hover:bg-gray-700 text-gray-200'
                  }`}
                >
                  All Products
                </button>
                {(facets?.categories ?? []).map((category) => (
                  <button
                    key={category.slug}
                    onClick={() => setSelectedCategorySlug(category.slug)}
                    className={`w-full text-left rounded transition px-4 py-3 text-sm sm:text-base ${
                      selectedCategorySlug === category.slug
                        ? 'bg-primary text-white'
                        : 'bg-gray-800 hover:bg-gray-700 text-gray-200'
                    }`}
                  >
                    {category.name}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-lg font-bold mb-4">Brand</h2>
              <div className="space-y-2">
                {(facets?.brands ?? []).map((brand) => (
                  <label
                    key={brand}
                    className="flex items-center gap-2 text-sm text-gray-200 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedBrand === brand}
                      onChange={() => setSelectedBrand(selectedBrand === brand ? null : brand)}
                      className="rounded border-gray-600 bg-gray-800 text-primary focus:ring-primary"
                    />
                    <span>{brand}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <h2 className="text-lg font-bold mb-4">Price Range</h2>
              <DualRangeSlider
                min={boundsMin}
                max={boundsMax}
                valueMin={activeMin}
                valueMax={activeMax}
                step={1}
                onChange={handlePriceRangeChange}
                formatLabel={(value) => `€${value.toFixed(0)}`}
              />
            </div>

            <button
              onClick={clearFilters}
              className="w-full bg-gray-800 hover:bg-gray-700 text-gray-200 px-4 py-2 rounded-lg text-sm transition"
            >
              Clear filters
            </button>
          </div>
        </div>

        <div className={`flex-1 min-w-0 ${cartOpen ? 'mr-80' : ''}`}>
          <div className="flex flex-col sm:flex-row gap-3 mb-6">
            <input
              type="search"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search products..."
              className="flex-1 bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-primary"
            />
            <select
              value={sort}
              onChange={(e) => setSort(e.target.value)}
              className="bg-gray-900 border border-gray-700 rounded-lg px-4 py-3 text-white focus:outline-none focus:border-primary sm:w-56"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <p className="text-sm text-gray-400 mb-4">
            {loading && products.length === 0
              ? 'Loading products...'
              : `${totalElements} product${totalElements === 1 ? '' : 's'} found`}
          </p>

          <div
            className={`fixed top-24 left-1/2 z-[100] -translate-x-1/2 transition-all duration-300 ${
              showToast
                ? 'opacity-100 translate-y-0'
                : 'opacity-0 -translate-y-2 pointer-events-none'
            }`}
          >
            {notification && (
              <div className="bg-green-500 text-white px-4 py-2 rounded-lg shadow-lg border border-green-400 text-sm flex items-center gap-2">
                <span>✅</span>
                <span>{notification}</span>
              </div>
            )}
          </div>

          {error && (
            <div className="bg-red-900/50 border border-red-700 text-red-200 px-4 py-3 rounded-lg mb-6">
              {error}
            </div>
          )}

          {loading && products.length === 0 ? (
            <div className="text-center py-12 text-gray-400">Loading catalog...</div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6">
              {products.map((product) => (
                <div
                  key={product.id}
                  className="bg-gray-900 rounded-lg overflow-hidden border border-gray-800 hover:border-primary transition shadow-lg hover:shadow-primary/20 flex flex-col"
                >
                  <div className="relative overflow-hidden bg-gray-800 h-80 flex-shrink-0">
                    <img
                      src={
                        brokenImageIds.has(product.id)
                          ? CustomDesign
                          : productImageUrl(product.primaryImageUrl)
                      }
                      alt={product.name}
                      className="w-full h-full object-cover hover:scale-105 transition duration-300 opacity-80"
                      onError={() => {
                        setBrokenImageIds((current) => {
                          if (current.has(product.id)) {
                            return current;
                          }
                          const next = new Set(current);
                          next.add(product.id);
                          return next;
                        });
                      }}
                    />
                    <span className="absolute top-3 right-3 bg-primary text-white px-3 py-1 rounded text-sm font-semibold">
                      €{product.price.toFixed(2)}
                    </span>
                    {product.rating > 0 && (
                      <span className="absolute top-3 left-3 bg-gray-900/80 text-yellow-300 px-2 py-1 rounded text-xs">
                        ★ {product.rating.toFixed(1)}
                      </span>
                    )}
                  </div>

                  <div className="p-6 space-y-4 flex-1 flex flex-col">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="text-lg font-bold">{product.name}</h3>
                      <span className="text-xs bg-primary text-white px-2 py-1 rounded whitespace-nowrap font-semibold">
                        {product.category}
                      </span>
                    </div>
                    <p className="text-gray-400 text-sm">{product.brand}</p>
                    <p className="text-gray-400 text-sm flex-1">{product.description}</p>

                    <button
                      onClick={() => handleAddToCart(product)}
                      className="w-full bg-primary text-white font-semibold py-3 rounded-lg hover:bg-primary-focus transition flex items-center justify-center gap-2 mt-auto"
                    >
                      🛒 Add to Cart
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {!loading && products.length === 0 && !error && (
            <div className="text-center py-12 text-gray-400">
              No products match your filters
            </div>
          )}
        </div>
      </div>

      {cartOpen && <Cart onClose={() => setCartOpen(false)} />}
    </div>
  );
};

export default ProductsPage;
