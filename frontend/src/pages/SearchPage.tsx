import CatalogBrowsePage from '../components/CatalogBrowsePage';
import PageMeta from '../components/PageMeta';

export default function SearchPage() {
  return (
    <>
      <PageMeta title="Search results" description="Search the ESTValgus lighting catalog." />
      <CatalogBrowsePage mode="search" />
    </>
  );
}
