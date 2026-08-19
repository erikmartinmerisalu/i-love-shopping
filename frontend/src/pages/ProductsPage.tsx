import CatalogBrowsePage from '../components/CatalogBrowsePage';
import PageMeta from '../components/PageMeta';

export default function ProductsPage() {
  return (
    <>
      <PageMeta title="Shop All Lamps" description="Browse lighting products with filters, ratings, and sorting." />
      <CatalogBrowsePage mode="catalog" />
    </>
  );
}
