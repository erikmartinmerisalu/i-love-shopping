import { Link } from 'react-router-dom';
import CustomDesign from '../assets/Custom_Design.png';
import type { Product } from '../types/catalog';
import { resolveProductImageUrl } from '../utils/productImageUrl';
import StarRating from './StarRating';

type ProductCardProps = {
  product: Product;
  onAddToCart?: (product: Product) => void;
  layout?: 'grid' | 'list';
  brokenImage?: boolean;
  onImageError?: () => void;
};

export default function ProductCard({
  product,
  onAddToCart,
  layout = 'grid',
  brokenImage = false,
  onImageError,
}: ProductCardProps) {
  const imageSrc = brokenImage
    ? CustomDesign
    : resolveProductImageUrl(product.primaryImageUrl, CustomDesign);

  const imageBlock = (
    <Link to={`/products/${product.id}`} className="block overflow-hidden bg-gray-800">
      <img
        src={imageSrc}
        alt={`${product.name} — ${product.category}`}
        className={`w-full object-cover opacity-90 hover:scale-105 transition duration-300 ${
          layout === 'list' ? 'h-full min-h-[8rem]' : 'h-56 sm:h-64 lg:h-72'
        }`}
        onError={onImageError}
      />
    </Link>
  );

  const body = (
    <div className={`flex flex-col flex-1 ${layout === 'list' ? 'p-4 sm:p-5' : 'p-5'}`}>
      <div className="flex flex-wrap items-center gap-2 mb-2">
        <h2 className="text-lg font-bold">
          <Link to={`/products/${product.id}`} className="hover:text-sky-300 transition">
            {product.name}
          </Link>
        </h2>
        <span className="text-xs bg-primary/20 text-sky-200 px-2 py-0.5 rounded font-medium">
          {product.category}
        </span>
      </div>
      <p className="text-gray-400 text-sm">{product.brand}</p>
      {product.rating > 0 && (
        <div className="mt-2 flex items-center gap-2">
          <StarRating rating={product.rating} />
          {product.reviewCount > 0 && (
            <span className="text-xs text-gray-500">({product.reviewCount})</span>
          )}
        </div>
      )}
      <p className={`text-gray-400 text-sm mt-2 ${layout === 'grid' ? 'flex-1 line-clamp-3' : 'line-clamp-2 sm:line-clamp-none'}`}>
        {product.description}
      </p>
      <div
        className={`mt-4 ${
          layout === 'grid'
            ? 'flex justify-center'
            : 'flex items-center justify-between gap-3 sm:mt-3'
        }`}
      >
        {layout === 'list' && (
          <p className="text-xl font-bold text-primary">€{product.price.toFixed(2)}</p>
        )}
        {onAddToCart && (
          <button
            type="button"
            onClick={() => onAddToCart(product)}
            disabled={product.stockQuantity <= 0}
            className={`rounded-lg bg-primary font-semibold text-white hover:bg-primary-focus transition disabled:opacity-50 disabled:cursor-not-allowed ${
              layout === 'grid'
                ? 'w-full max-w-[240px] px-6 py-3 text-base text-center'
                : 'shrink-0 px-5 py-2.5 text-sm'
            }`}
          >
            {product.stockQuantity <= 0 ? 'Out of stock' : 'Add to cart'}
          </button>
        )}
      </div>
    </div>
  );

  if (layout === 'list') {
    return (
      <article className="bg-gray-900/70 rounded-xl border border-white/10 hover:border-primary/40 transition overflow-hidden grid grid-cols-1 sm:grid-cols-[12rem_1fr]">
        {imageBlock}
        {body}
      </article>
    );
  }

  return (
    <article className="bg-gray-900/70 rounded-xl border border-white/10 hover:border-primary/40 transition overflow-hidden flex flex-col h-full">
      <div className="relative">
        {imageBlock}
        <span className="absolute top-3 right-3 bg-primary text-white px-3 py-1 rounded text-sm font-semibold">
          €{product.price.toFixed(2)}
        </span>
      </div>
      {body}
    </article>
  );
}
