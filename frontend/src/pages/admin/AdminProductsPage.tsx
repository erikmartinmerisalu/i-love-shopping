import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  bulkUploadProducts,
  createAdminProduct,
  deleteAdminProduct,
  fetchAdminCategories,
  fetchAdminProduct,
  fetchAdminProducts,
  updateAdminProduct,
  uploadAdminProductImage,
} from '../../api/admin';
import type { AdminProductPayload } from '../../types/admin';
import type { Category, Product, ProductListResponse } from '../../types/catalog';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';
import { resolveProductImageUrl } from '../../utils/productImageUrl';
import CustomDesign from '../../assets/Custom_Design.png';

const inputClass =
  'w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-white placeholder:text-gray-500';

const previewImageUrls = (detail: { imageUrls?: string[]; thumbnailUrls?: string[] }) =>
  detail.thumbnailUrls && detail.thumbnailUrls.length > 0
    ? detail.thumbnailUrls
    : detail.imageUrls ?? [];

const emptyProduct = (categoryId = 0): AdminProductPayload => ({
  name: '',
  description: '',
  price: 0,
  stockQuantity: 0,
  brand: '',
  categoryId,
  sku: '',
  active: true,
  featured: false,
});

const productToPayload = (detail: Awaited<ReturnType<typeof fetchAdminProduct>>): AdminProductPayload => ({
  name: detail.name,
  description: detail.description,
  price: detail.price,
  stockQuantity: detail.stockQuantity,
  brand: detail.brand,
  categoryId: detail.category.id,
  sku: detail.sku ?? '',
  active: detail.active ?? true,
  featured: detail.featured ?? false,
  weightKg: detail.dimensions.weightKg,
  weightLb: detail.dimensions.weightLb,
  lengthCm: detail.dimensions.lengthCm,
  lengthIn: detail.dimensions.lengthIn,
  widthCm: detail.dimensions.widthCm,
  widthIn: detail.dimensions.widthIn,
  heightCm: detail.dimensions.heightCm,
  heightIn: detail.dimensions.heightIn,
});

export default function AdminProductsPage() {
  const { token } = useAuth();
  const [data, setData] = useState<ProductListResponse | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [uploadMessage, setUploadMessage] = useState('');
  const [editingId, setEditingId] = useState<number | 'new' | null>(null);
  const [form, setForm] = useState<AdminProductPayload>(emptyProduct());
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      const [products, cats] = await Promise.all([
        fetchAdminProducts(token),
        fetchAdminCategories(token),
      ]);
      setData(products);
      setCategories(cats);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load products');
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  const openCreate = () => {
    const defaultCategory = categories[0]?.id ?? 0;
    setForm(emptyProduct(defaultCategory));
    setImageUrls([]);
    setEditingId('new');
    setMessage('');
    setError('');
  };

  const openEdit = async (product: Product) => {
    setMessage('');
    setError('');
    try {
      const detail = await fetchAdminProduct(token, product.id);
      setForm(productToPayload(detail));
      setImageUrls(previewImageUrls(detail));
      setEditingId(product.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load product');
    }
  };

  const closeEditor = () => {
    setEditingId(null);
    setImageUrls([]);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    setError('');
    try {
      const payload = {
        ...form,
        sku: form.sku?.trim() || undefined,
      };
      if (editingId === 'new') {
        const created = await createAdminProduct(token, payload);
        setMessage(`Created "${created.name}"`);
        setEditingId(created.id);
        setImageUrls(previewImageUrls(created));
      } else if (typeof editingId === 'number') {
        const updated = await updateAdminProduct(token, editingId, payload);
        setMessage(`Saved "${updated.name}"`);
        setImageUrls(previewImageUrls(updated));
      }
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (product: Product) => {
    if (!window.confirm(`Delete "${product.name}"? This cannot be undone.`)) {
      return;
    }
    setError('');
    try {
      await deleteAdminProduct(token, product.id);
      if (editingId === product.id) {
        closeEditor();
      }
      setMessage(`Deleted "${product.name}"`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  const handleImageUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || typeof editingId !== 'number') {
      return;
    }
    try {
      await uploadAdminProductImage(token, editingId, file, imageUrls.length === 0);
      const detail = await fetchAdminProduct(token, editingId);
      setImageUrls(previewImageUrls(detail));
      setMessage('Image uploaded');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Image upload failed');
    } finally {
      event.target.value = '';
    }
  };

  const handleBulkUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    try {
      const result = await bulkUploadProducts(token, file);
      setUploadMessage(`Created ${result.created}, updated ${result.updated}, skipped ${result.skipped}`);
      if (result.errors.length > 0) {
        setUploadMessage((current) => `${current}. ${result.errors.slice(0, 3).join(' ')}`);
      }
      await load();
    } catch (err) {
      setUploadMessage(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      event.target.value = '';
    }
  };

  const setField = <K extends keyof AdminProductPayload>(key: K, value: AdminProductPayload[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  return (
    <>
      <PageMeta title="Admin products" />
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold">Products</h2>
          <p className="mt-1 text-sm text-gray-400">Create, edit, and remove catalog items.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={openCreate}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-focus"
          >
            New product
          </button>
          <label className="cursor-pointer rounded-lg border border-white/20 bg-gray-800 px-4 py-2 text-sm font-semibold text-white hover:bg-gray-700">
            Bulk CSV / JSON
            <input
              type="file"
              accept=".csv,.json,text/csv,application/json"
              className="hidden"
              onChange={handleBulkUpload}
            />
          </label>
        </div>
      </div>

      {uploadMessage && <p className="mt-4 text-sm text-sky-200">{uploadMessage}</p>}
      {message && <p className="mt-4 text-sm text-emerald-300">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      {editingId !== null && (
        <form
          onSubmit={handleSubmit}
          className="mt-6 space-y-4 rounded-xl border border-primary/30 bg-gray-900/80 p-5"
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-lg font-semibold">
              {editingId === 'new' ? 'New product' : `Edit product #${editingId}`}
            </h3>
            <button
              type="button"
              onClick={closeEditor}
              className="text-sm text-gray-400 hover:text-white"
            >
              Close
            </button>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block sm:col-span-2">
              <span className="mb-1 block text-xs font-medium text-gray-400">Name</span>
              <input
                required
                className={inputClass}
                value={form.name}
                onChange={(e) => setField('name', e.target.value)}
              />
            </label>
            <label className="block sm:col-span-2">
              <span className="mb-1 block text-xs font-medium text-gray-400">Description</span>
              <textarea
                required
                rows={3}
                className={inputClass}
                value={form.description}
                onChange={(e) => setField('description', e.target.value)}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-400">Price (€)</span>
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                className={inputClass}
                value={form.price || ''}
                onChange={(e) => setField('price', Number(e.target.value))}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-400">Stock</span>
              <input
                required
                type="number"
                min="0"
                step="1"
                className={inputClass}
                value={form.stockQuantity}
                onChange={(e) => setField('stockQuantity', Number(e.target.value))}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-400">Brand</span>
              <input
                required
                className={inputClass}
                value={form.brand}
                onChange={(e) => setField('brand', e.target.value)}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-400">Category</span>
              <select
                required
                className={inputClass}
                value={form.categoryId || ''}
                onChange={(e) => setField('categoryId', Number(e.target.value))}
              >
                <option value="" disabled>
                  Select category
                </option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-gray-400">SKU (optional)</span>
              <input
                className={inputClass}
                value={form.sku ?? ''}
                onChange={(e) => setField('sku', e.target.value)}
              />
            </label>
            <div className="flex flex-wrap items-center gap-4 pt-6">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setField('active', e.target.checked)}
                  className="rounded border-gray-600 bg-gray-800 text-primary"
                />
                Active in storefront
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={form.featured}
                  onChange={(e) => setField('featured', e.target.checked)}
                  className="rounded border-gray-600 bg-gray-800 text-primary"
                />
                Featured on home
              </label>
            </div>
          </div>

          {typeof editingId === 'number' && (
            <div className="border-t border-white/10 pt-4">
              <p className="text-sm font-medium text-gray-300">Product images</p>
              {imageUrls.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {imageUrls.map((url) => (
                    <img
                      key={url}
                      src={resolveProductImageUrl(url, CustomDesign)}
                      alt={form.name || 'Product image'}
                      className="h-16 w-16 rounded border border-white/10 object-cover"
                    />
                  ))}
                </div>
              )}
              <label className="mt-3 inline-flex cursor-pointer rounded-lg bg-gray-800 px-3 py-2 text-sm hover:bg-gray-700">
                Upload image
                <input type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />
              </label>
            </div>
          )}

          <div className="flex flex-wrap gap-2 border-t border-white/10 pt-4">
            <button
              type="submit"
              disabled={saving || !form.categoryId}
              className="rounded-lg bg-primary px-5 py-2 text-sm font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
            >
              {saving ? 'Saving…' : editingId === 'new' ? 'Create product' : 'Save changes'}
            </button>
          </div>
        </form>
      )}

      <div className="mt-6 overflow-x-auto rounded-xl border border-white/10">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-gray-900/80 text-gray-300">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Brand</th>
              <th className="px-4 py-3">Category</th>
              <th className="px-4 py-3">Price</th>
              <th className="px-4 py-3">Stock</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {(data?.products ?? []).map((product) => (
              <tr key={product.id} className="border-t border-white/5">
                <td className="px-4 py-3">{product.name}</td>
                <td className="px-4 py-3">{product.brand}</td>
                <td className="px-4 py-3">{product.category}</td>
                <td className="px-4 py-3">€{product.price.toFixed(2)}</td>
                <td className="px-4 py-3">{product.stockQuantity}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => openEdit(product)}
                      className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(product)}
                      className="rounded bg-red-900/60 px-3 py-1 text-xs text-red-200 hover:bg-red-900"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
