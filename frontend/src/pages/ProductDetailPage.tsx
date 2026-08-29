import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchProduct, fetchRelatedProducts } from '../api/catalog';
import { useCart } from '../context/CartContext';
import PageMeta from '../components/PageMeta';
import ProductCard from '../components/ProductCard';
import StarRating from '../components/StarRating';
import ProductReviews from '../components/ProductReviews';
import CustomDesign from '../assets/Custom_Design.png';
import type { Product, ProductDetail } from '../types/catalog';
import { resolveProductImageUrl } from '../utils/productImageUrl';

export default function ProductDetailPage() {
  const { id } = useParams();
  const productId = Number(id);
  const { addToCart, clearCartError } = useCart();

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [related, setRelated] = useState<Product[]>([]);
  const [selectedImage, setSelectedImage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(productId)) {
      setError('Invalid product');
      setLoading(false);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const detail = await fetchProduct(productId);
        const relatedProducts = await fetchRelatedProducts(productId);
        if (!cancelled) {
          setProduct(detail);
          setRelated(relatedProducts);
          setSelectedImage(0);
        }
      } catch {
        if (!cancelled) {
          setError('Product not found');
          setProduct(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };
    void load();
    return () => {
      cancelled = true;
    };
  }, [productId]);

  const handleAddToCart = async () => {
    if (!product) {
      return;
    }
    clearCartError();
    setAdding(true);
    try {
      await addToCart(product.id);
      setToast(`${product.name} added to cart`);
      window.setTimeout(() => setToast(''), 2000);
    } catch (err) {
      setToast(err instanceof Error ? err.message : 'Could not add to cart');
    } finally {
      setAdding(false);
    }
  };

  if (loading) {
    return <p className="page-container py-16 text-gray-400">Loading product…</p>;
  }

  if (error || !product) {
    return (
      <div className="page-container py-16 text-center">
        <h1 className="text-2xl font-bold">Product not found</h1>
        <p className="mt-2 text-gray-400">This item may have been removed from the catalog.</p>
        <Link to="/products" className="mt-6 inline-block text-sky-300 hover:text-sky-200">
          Back to shop
        </Link>
      </div>
    );
  }

  const fullImages = product.imageUrls.length > 0 ? product.imageUrls : [null];
  const thumbImages =
    product.thumbnailUrls && product.thumbnailUrls.length > 0
      ? product.thumbnailUrls
      : fullImages;
  const mainImage = resolveProductImageUrl(fullImages[selectedImage], CustomDesign);

  return (
    <>
      <PageMeta
        title={product.name}
        description={product.description.slice(0, 150)}
      />

      {toast && (
        <div className="fixed top-24 left-1/2 z-50 -translate-x-1/2 rounded-lg bg-green-600 px-4 py-2 text-sm shadow-lg">
          {toast}
        </div>
      )}

      <article className="page-container py-8">
        <nav className="mb-6 text-sm text-gray-400" aria-label="Breadcrumb">
          <Link to="/products" className="hover:text-white">Shop</Link>
          <span aria-hidden="true"> / </span>
          <Link to={`/products?category=${product.category.slug}`} className="hover:text-white">
            {product.category.name}
          </Link>
          <span aria-hidden="true"> / </span>
          <span className="text-gray-200">{product.name}</span>
        </nav>

        <div className="grid gap-10 lg:grid-cols-2">
          <div>
            <div className="overflow-hidden rounded-xl border border-gray-800 bg-gray-900">
              <img
                src={mainImage}
                alt={`${product.name} — main product image`}
                className="h-[20rem] w-full object-cover sm:h-[28rem]"
              />
            </div>
            {fullImages.length > 1 && (
              <div className="mt-4 flex gap-3 overflow-x-auto">
                {thumbImages.map((url, index) => (
                  <button
                    key={`${url ?? 'placeholder'}-${index}`}
                    type="button"
                    onClick={() => setSelectedImage(index)}
                    className={`shrink-0 overflow-hidden rounded-lg border ${
                      index === selectedImage ? 'border-primary' : 'border-gray-700'
                    }`}
                    aria-label={`View image ${index + 1}`}
                  >
                    <img
                      src={resolveProductImageUrl(url, CustomDesign)}
                      alt={`${product.name}, thumbnail ${index + 1}`}
                      className="h-16 w-16 object-cover"
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div>
            <p className="text-sm uppercase tracking-wide text-sky-300">{product.brand}</p>
            <h1 className="mt-2 text-3xl font-bold sm:text-4xl">{product.name}</h1>
            {product.rating > 0 && (
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <StarRating rating={product.rating} size="md" />
                {product.reviewCount > 0 && (
                  <span className="text-sm text-gray-400">
                    ({product.reviewCount} {product.reviewCount === 1 ? 'review' : 'reviews'})
                  </span>
                )}
              </div>
            )}
            <p className="mt-4 text-3xl font-bold text-primary">€{product.price.toFixed(2)}</p>
            <p className="mt-4 text-gray-300 leading-relaxed">{product.description}</p>

            <dl className="mt-6 grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-lg bg-gray-900 p-3 border border-gray-800">
                <dt className="text-gray-400">Availability</dt>
                <dd className="font-medium">
                  {product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'}
                </dd>
              </div>
              <div className="rounded-lg bg-gray-900 p-3 border border-gray-800">
                <dt className="text-gray-400">Category</dt>
                <dd className="font-medium">{product.category.name}</dd>
              </div>
              {product.dimensions.heightCm != null && (
                <div className="rounded-lg bg-gray-900 p-3 border border-gray-800 col-span-2">
                  <dt className="text-gray-400">Dimensions (H × W × L)</dt>
                  <dd className="font-medium">
                    {product.dimensions.heightCm} × {product.dimensions.widthCm} × {product.dimensions.lengthCm} cm
                  </dd>
                </div>
              )}
            </dl>

            <button
              type="button"
              onClick={() => void handleAddToCart()}
              disabled={product.stockQuantity <= 0 || adding}
              className="mt-8 w-full rounded-lg bg-primary py-3 font-semibold text-white hover:bg-primary-focus transition disabled:opacity-50"
            >
              {product.stockQuantity <= 0 ? 'Out of stock' : adding ? 'Adding…' : 'Add to cart'}
            </button>
          </div>
        </div>

        <ProductReviews
          productId={product.id}
          productRating={product.rating}
          reviewCount={product.reviewCount ?? 0}
        />

        {related.length > 0 && (
          <section className="mt-16" aria-labelledby="related-heading">
            <h2 id="related-heading" className="text-2xl font-bold">You may also like</h2>
            <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
              {related.map((item) => (
                <ProductCard key={item.id} product={item} />
              ))}
            </div>
          </section>
        )}
      </article>
    </>
  );
}
