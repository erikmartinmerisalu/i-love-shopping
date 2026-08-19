import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  createAdminCategory,
  deleteAdminCategory,
  fetchAdminCategories,
  updateAdminCategory,
} from '../../api/admin';
import type { AdminCategoryPayload } from '../../types/admin';
import type { Category } from '../../types/catalog';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';

const inputClass =
  'w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-white placeholder:text-gray-500';

const emptyCategory = (): AdminCategoryPayload => ({
  name: '',
  slug: '',
  description: '',
});

export default function AdminCategoriesPage() {
  const { token } = useAuth();
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [createForm, setCreateForm] = useState<AdminCategoryPayload>(emptyCategory());
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<AdminCategoryPayload>(emptyCategory());
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      setCategories(await fetchAdminCategories(token));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load categories');
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    setError('');
    try {
      await createAdminCategory(token, createForm);
      setCreateForm(emptyCategory());
      setMessage('Category created');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (category: Category) => {
    setEditingId(category.id);
    setEditForm({
      name: category.name,
      slug: category.slug,
      description: category.description ?? '',
    });
  };

  const handleUpdate = async (event: FormEvent) => {
    event.preventDefault();
    if (editingId === null) {
      return;
    }
    setSaving(true);
    setMessage('');
    setError('');
    try {
      await updateAdminCategory(token, editingId, editForm);
      setEditingId(null);
      setMessage('Category updated');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (category: Category) => {
    if (!window.confirm(`Delete category "${category.name}"? Products must be moved first.`)) {
      return;
    }
    setError('');
    try {
      await deleteAdminCategory(token, category.id);
      if (editingId === category.id) {
        setEditingId(null);
      }
      setMessage(`Deleted "${category.name}"`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  return (
    <>
      <PageMeta title="Admin categories" />
      <h2 className="text-2xl font-bold">Categories</h2>
      <p className="mt-1 text-sm text-gray-400">Manage catalog taxonomy and slugs.</p>
      {message && <p className="mt-4 text-sm text-emerald-300">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <form
        onSubmit={handleCreate}
        className="mt-6 grid gap-3 rounded-xl border border-white/10 bg-gray-900/70 p-4 sm:grid-cols-2"
      >
        <h3 className="sm:col-span-2 text-lg font-semibold">Add category</h3>
        <label className="block">
          <span className="mb-1 block text-xs text-gray-400">Name</span>
          <input
            required
            className={inputClass}
            value={createForm.name}
            onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs text-gray-400">Slug</span>
          <input
            required
            className={inputClass}
            value={createForm.slug}
            onChange={(e) => setCreateForm((f) => ({ ...f, slug: e.target.value.toLowerCase() }))}
            placeholder="floor-lamps"
          />
        </label>
        <label className="block sm:col-span-2">
          <span className="mb-1 block text-xs text-gray-400">Description</span>
          <textarea
            rows={2}
            className={inputClass}
            value={createForm.description ?? ''}
            onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
          />
        </label>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
          >
            Create category
          </button>
        </div>
      </form>

      <div className="mt-6 grid gap-3">
        {categories.map((category) => (
          <article key={category.id} className="rounded-xl border border-white/10 bg-gray-900/70 p-4">
            {editingId === category.id ? (
              <form onSubmit={handleUpdate} className="grid gap-3 sm:grid-cols-2">
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Name</span>
                  <input
                    required
                    className={inputClass}
                    value={editForm.name}
                    onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))}
                  />
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Slug</span>
                  <input
                    required
                    className={inputClass}
                    value={editForm.slug}
                    onChange={(e) => setEditForm((f) => ({ ...f, slug: e.target.value.toLowerCase() }))}
                  />
                </label>
                <label className="block sm:col-span-2">
                  <span className="mb-1 block text-xs text-gray-400">Description</span>
                  <textarea
                    rows={2}
                    className={inputClass}
                    value={editForm.description ?? ''}
                    onChange={(e) => setEditForm((f) => ({ ...f, description: e.target.value }))}
                  />
                </label>
                <div className="flex flex-wrap gap-2 sm:col-span-2">
                  <button
                    type="submit"
                    disabled={saving}
                    className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => setEditingId(null)}
                    className="rounded-lg bg-gray-800 px-4 py-2 text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h3 className="font-semibold">{category.name}</h3>
                    <span className="text-xs text-gray-400">{category.slug}</span>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => startEdit(category)}
                      className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={() => handleDelete(category)}
                      className="rounded bg-red-900/60 px-3 py-1 text-xs text-red-200 hover:bg-red-900"
                    >
                      Delete
                    </button>
                  </div>
                </div>
                {category.description && (
                  <p className="mt-2 text-sm text-gray-400">{category.description}</p>
                )}
              </>
            )}
          </article>
        ))}
      </div>
    </>
  );
}
