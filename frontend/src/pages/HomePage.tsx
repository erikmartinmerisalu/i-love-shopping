import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchHome } from '../api/catalog';
import PageMeta from '../components/PageMeta';
import ProductCard from '../components/ProductCard';
import { SITE } from '../config/site';
import type { HomeData } from '../types/catalog';

export default function HomePage() {
  const [data, setData] = useState<HomeData | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const home = await fetchHome();
        if (!cancelled) {
          setData(home);
        }
      } catch {
        if (!cancelled) {
          setError('Could not load featured products.');
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
  }, []);

  return (
    <>
      <PageMeta title="Modern Lighting Shop" description={SITE.description} />

      <section className="relative overflow-hidden border-b border-white/10">
        <div
          className="pointer-events-none absolute inset-0 bg-gradient-to-br from-sky-950/50 via-gray-950/20 to-indigo-950/40"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -left-32 top-0 h-72 w-72 rounded-full bg-sky-500/20 blur-3xl"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute -right-24 top-10 h-80 w-80 rounded-full bg-indigo-500/15 blur-3xl"
          aria-hidden="true"
        />
        <div className="page-container relative py-16 lg:py-24">
          <p className="text-sm font-semibold uppercase tracking-widest text-sky-300">New season</p>
          <h1 className="mt-3 max-w-3xl text-4xl font-bold leading-tight sm:text-5xl lg:text-6xl">
            Light every room with confidence
          </h1>
          <p className="mt-4 max-w-2xl text-lg text-gray-300">
            {SITE.tagline}. Browse curated collections, read ratings, and checkout securely as a guest or member.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to="/products"
              className="rounded-lg bg-primary px-6 py-3 font-semibold text-white hover:bg-primary-focus transition"
            >
              Shop all products
            </Link>
            <Link
              to="/about"
              className="rounded-lg border border-white/20 bg-white/5 px-6 py-3 font-semibold text-gray-200 hover:bg-white/10 transition"
            >
              Our story
            </Link>
          </div>
        </div>
      </section>

      <section className="page-container py-12">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold">Featured products</h2>
            <p className="mt-1 text-sm text-gray-400">Top-rated picks from our catalog</p>
          </div>
          <Link to="/products" className="text-sm text-sky-300 hover:text-sky-200">
            View all →
          </Link>
        </div>

        {loading && <p className="mt-8 text-gray-400">Loading featured products…</p>}
        {error && <p className="mt-8 text-red-300">{error}</p>}

        {!loading && data && (
          <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
            {data.featuredProducts.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>

      {data?.collections.map((collection) => (
        <section
          key={collection.category.slug}
          className="border-t border-white/10 bg-gray-950/40"
          aria-labelledby={`collection-${collection.category.slug}`}
        >
          <div className="page-container py-12">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <h2 id={`collection-${collection.category.slug}`} className="text-2xl font-bold">
                  {collection.category.name}
                </h2>
                <p className="mt-1 max-w-3xl text-sm text-gray-400">{collection.category.description}</p>
              </div>
              <Link
                to={`/products?category=${collection.category.slug}`}
                className="text-sm text-sky-300 hover:text-sky-200"
              >
                Browse collection →
              </Link>
            </div>
            <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
              {collection.products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          </div>
        </section>
      ))}
    </>
  );
}
