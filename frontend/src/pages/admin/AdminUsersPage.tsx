import { useEffect, useState } from 'react';
import { fetchAdminUsers, updateAdminUser, type AdminUser } from '../../api/admin';
import { useAuth } from '../../context/AuthContext';
import PageMeta from '../../components/PageMeta';

export default function AdminUsersPage() {
  const { token } = useAuth();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = async () => {
    try {
      setUsers(await fetchAdminUsers(token));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not load users');
    }
  };

  useEffect(() => {
    void load();
  }, [token]);

  const toggleRole = async (user: AdminUser) => {
    const nextRole = user.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN';
    try {
      await updateAdminUser(token, user.id, { role: nextRole });
      setMessage(`Updated ${user.email} to ${nextRole}`);
      await load();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Update failed');
    }
  };

  const toggleEnabled = async (user: AdminUser) => {
    try {
      await updateAdminUser(token, user.id, { enabled: !user.enabled });
      setMessage(`${user.enabled ? 'Disabled' : 'Enabled'} ${user.email}`);
      await load();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Update failed');
    }
  };

  return (
    <>
      <PageMeta title="Admin users" />
      <h2 className="text-2xl font-bold">Users</h2>
      <p className="mt-1 text-sm text-gray-400">Assign roles and manage account access.</p>
      {message && <p className="mt-4 text-sm text-sky-200">{message}</p>}
      {error && <p className="mt-4 text-sm text-red-300">{error}</p>}

      <div className="mt-6 overflow-x-auto rounded-xl border border-white/10">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-gray-900/80 text-gray-300">
            <tr>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">2FA</th>
              <th className="px-4 py-3">Enabled</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-t border-white/5">
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">{user.role}</td>
                <td className="px-4 py-3">{user.twoFactorEnabled ? 'Yes' : 'No'}</td>
                <td className="px-4 py-3">{user.enabled ? 'Yes' : 'No'}</td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => toggleRole(user)}
                      className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                    >
                      Make {user.role === 'ADMIN' ? 'customer' : 'admin'}
                    </button>
                    <button
                      type="button"
                      onClick={() => toggleEnabled(user)}
                      className="rounded bg-gray-800 px-3 py-1 text-xs hover:bg-gray-700"
                    >
                      {user.enabled ? 'Disable' : 'Enable'}
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
