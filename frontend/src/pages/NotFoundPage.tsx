import { Link } from 'react-router-dom';
import PageMeta from '../components/PageMeta';

export default function NotFoundPage() {
  return (
    <>
      <PageMeta title="Page not found" description="The page you requested could not be found." />

      <div className="page-container-narrow flex min-h-[50vh] flex-col items-center justify-center py-16 text-center">
        <p className="text-6xl font-bold text-primary">404</p>
        <h1 className="mt-4 text-3xl font-bold">Page not found</h1>
        <p className="mt-3 text-gray-400">
          The page you are looking for does not exist or may have moved.
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-3">
          <Link to="/" className="rounded-lg bg-primary px-5 py-2.5 font-semibold text-white hover:bg-primary-focus">
            Go home
          </Link>
          <Link to="/products" className="rounded-lg border border-gray-700 px-5 py-2.5 font-semibold hover:bg-gray-800">
            Browse shop
          </Link>
          <Link to="/search?q=lamp" className="rounded-lg border border-gray-700 px-5 py-2.5 font-semibold hover:bg-gray-800">
            Search products
          </Link>
        </div>
      </div>
    </>
  );
}
