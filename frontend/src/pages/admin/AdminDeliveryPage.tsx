import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  createDeliveryOption,
  deleteDeliveryOption,
  fetchDeliveryOptions,
  updateDeliveryOption,
  type DeliveryOption,
} from '../../api/admin';
import type { AdminDeliveryOptionPayload } from '../../types/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';

const inputClass =
  'w-full rounded-lg border border-gray-700 bg-gray-950 px-3 py-2 text-sm text-white placeholder:text-gray-500';

const emptyOption = (): AdminDeliveryOptionPayload => ({
  name: '',
  price: 0,
  estimatedDays: 5,
  active: true,
});

const toPayload = (option: DeliveryOption): AdminDeliveryOptionPayload => ({
  name: option.name,
  price: option.price,
  estimatedDays: option.estimatedDays,
  active: option.active,
});

export default function AdminDeliveryPage() {
  const { token } = useAuth();
  const [options, setOptions] = useState<DeliveryOption[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [createForm, setCreateForm] = useState<AdminDeliveryOptionPayload>(emptyOption());
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<AdminDeliveryOptionPayload>(emptyOption());
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      setOptions(await fetchDeliveryOptions(token));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load delivery options');
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
      await createDeliveryOption(token, createForm);
      setCreateForm(emptyOption());
      setMessage('Delivery option created');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed');
    } finally {
      setSaving(false);
    }
  };

  const startEdit = (option: DeliveryOption) => {
    setEditingId(option.id);
    setEditForm(toPayload(option));
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
      await updateDeliveryOption(token, editingId, editForm);
      setEditingId(null);
      setMessage('Delivery option updated');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (option: DeliveryOption) => {
    if (!window.confirm(`Remove delivery option "${option.name}"? Options used by orders are deactivated instead.`)) {
      return;
    }
    setError('');
    try {
      await deleteDeliveryOption(token, option.id);
      if (editingId === option.id) {
        setEditingId(null);
      }
      setMessage(`Removed "${option.name}"`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  };

  return (
    <>
      <PageMeta title="Admin delivery" />
      <h2 className="text-2xl font-bold">Delivery options</h2>
      <p className="mt-1 text-sm text-gray-400">
        Manage shipping methods offered at checkout. Options already used on orders are deactivated instead of deleted.
      </p>
      {message && <p className="mt-4 text-sm text-emerald-300">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <form
        onSubmit={handleCreate}
        className="mt-6 grid gap-3 rounded-xl border border-white/10 bg-gray-900/70 p-4 sm:grid-cols-2"
      >
        <h3 className="sm:col-span-2 text-lg font-semibold">Add delivery option</h3>
        <label className="block">
          <span className="mb-1 block text-xs text-gray-400">Name</span>
          <input
            required
            className={inputClass}
            value={createForm.name}
            onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
            placeholder="Standard shipping"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs text-gray-400">Price (€)</span>
          <input
            required
            type="number"
            min="0"
            step="0.01"
            className={inputClass}
            value={createForm.price}
            onChange={(e) => setCreateForm((f) => ({ ...f, price: Number(e.target.value) }))}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs text-gray-400">Estimated days</span>
          <input
            required
            type="number"
            min="1"
            step="1"
            className={inputClass}
            value={createForm.estimatedDays}
            onChange={(e) => setCreateForm((f) => ({ ...f, estimatedDays: Number(e.target.value) }))}
          />
        </label>
        <label className="flex items-center gap-2 pt-6 text-sm">
          <input
            type="checkbox"
            checked={createForm.active}
            onChange={(e) => setCreateForm((f) => ({ ...f, active: e.target.checked }))}
            className="rounded border-gray-600 bg-gray-800 text-primary"
          />
          Active at checkout
        </label>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={saving}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-primary-focus disabled:opacity-50"
          >
            Create option
          </button>
        </div>
      </form>

      <div className="mt-6 grid gap-3">
        {options.map((option) => (
          <article key={option.id} className="rounded-xl border border-white/10 bg-gray-900/70 p-4">
            {editingId === option.id ? (
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
                  <span className="mb-1 block text-xs text-gray-400">Price (€)</span>
                  <input
                    required
                    type="number"
                    min="0"
                    step="0.01"
                    className={inputClass}
                    value={editForm.price}
                    onChange={(e) => setEditForm((f) => ({ ...f, price: Number(e.target.value) }))}
                  />
                </label>
                <label className="block">
                  <span className="mb-1 block text-xs text-gray-400">Estimated days</span>
                  <input
                    required
                    type="number"
                    min="1"
                    step="1"
                    className={inputClass}
                    value={editForm.estimatedDays}
                    onChange={(e) => setEditForm((f) => ({ ...f, estimatedDays: Number(e.target.value) }))}
                  />
                </label>
                <label className="flex items-center gap-2 pt-6 text-sm">
                  <input
                    type="checkbox"
                    checked={editForm.active}
                    onChange={(e) => setEditForm((f) => ({ ...f, active: e.target.checked }))}
                    className="rounded border-gray-600 bg-gray-800 text-primary"
                  />
                  Active at checkout
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
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="font-semibold">{option.name}</h3>
                  <p className="text-sm text-gray-400">
                    €{Number(option.price).toFixed(2)} · {option.estimatedDays} day
                    {option.estimatedDays === 1 ? '' : 's'} · {option.active ? 'Active' : 'Inactive'}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => startEdit(option)}
                    className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(option)}
                    className="rounded bg-red-900/60 px-3 py-1 text-xs text-red-200 hover:bg-red-900"
                  >
                    Delete
                  </button>
                </div>
              </div>
            )}
          </article>
        ))}
      </div>
    </>
  );
}
