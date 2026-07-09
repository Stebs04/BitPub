import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { UserPlus, Trash2, Users as UsersIcon, Pencil, KeyRound, Check, X } from 'lucide-react';
import { createUser, deleteUser, getAllUsers, updateUserRole, updateUser, updateUserPassword, type UserRecord } from '../services/api';
import { useAuthStore } from '../store/authStore';

// NOTE: bitpub-common Role enum values (verbatim strings expected by the backend)
const ROLES = ['PLAYER', 'LOCALE_ADMIN', 'GAME_ADMIN', 'PLATFORM_ADMIN'];

const UserManagementPage: React.FC = () => {
  const currentUser = useAuthStore((state) => state.user);

  const [users, setUsers] = useState<UserRecord[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [pendingRoleId, setPendingRoleId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editUsername, setEditUsername] = useState('');
  const [editEmail, setEditEmail] = useState('');

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState(ROLES[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setListError(null);
    try {
      const res = await getAllUsers();
      setUsers(res.data || []);
    } catch (err: any) {
      setListError(err?.response?.data?.message || 'Errore nel caricamento degli utenti.');
    } finally {
      setLoadingUsers(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      await createUser({ username, password, email, role });
      setSuccess(`Utente "${username}" creato con ruolo ${role}.`);
      setUsername('');
      setPassword('');
      setEmail('');
      setRole(ROLES[0]);
      fetchUsers();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Errore durante la creazione utente.');
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (userId: string, newRole: string) => {
    setPendingRoleId(userId);
    try {
      await updateUserRole(userId, newRole);
      setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, role: newRole } : u)));
    } catch (err: any) {
      setListError(err?.response?.data?.message || 'Errore durante la modifica del ruolo.');
    } finally {
      setPendingRoleId(null);
    }
  };

  const startEdit = (u: UserRecord) => {
    setEditingId(u.id);
    setEditUsername(u.username);
    setEditEmail(u.email);
    setListError(null);
  };

  const handleSaveEdit = async (userId: string) => {
    try {
      const res = await updateUser(userId, { username: editUsername, email: editEmail });
      setUsers((prev) => prev.map((u) => (u.id === userId ? res.data : u)));
      setEditingId(null);
    } catch (err: any) {
      setListError(err?.response?.data?.message || 'Errore durante la modifica utente.');
    }
  };

  const handleResetPassword = async (userId: string, targetUsername: string) => {
    const newPassword = window.prompt(`Nuova password per "${targetUsername}":`);
    if (!newPassword) return;
    try {
      await updateUserPassword(userId, newPassword);
      setListError(null);
    } catch (err: any) {
      setListError(err?.response?.data?.message || 'Errore durante il reset della password.');
    }
  };

  const handleDelete = async (userId: string, targetUsername: string) => {
    if (!window.confirm(`Eliminare definitivamente l'utente "${targetUsername}"?`)) return;
    try {
      await deleteUser(userId);
      setUsers((prev) => prev.filter((u) => u.id !== userId));
    } catch (err: any) {
      setListError(err?.response?.data?.message || "Errore durante l'eliminazione dell'utente.");
    }
  };

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserPlus className="w-5 h-5" />
            Crea nuovo utente
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
            <Input
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <Input
              label="Email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <div className="w-full flex flex-col gap-1.5">
              <label className="text-sm font-medium text-slate-300">Ruolo</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:border-transparent"
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>

            {error && <span className="text-sm text-red-500">{error}</span>}
            {success && <span className="text-sm text-green-500">{success}</span>}

            <Button type="submit" disabled={loading}>
              {loading ? 'Creazione in corso...' : 'Crea utente'}
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UsersIcon className="w-5 h-5" />
            Utenti della piattaforma
          </CardTitle>
        </CardHeader>
        <CardContent>
          {listError && <p className="text-sm text-red-500 mb-4">{listError}</p>}

          {loadingUsers ? (
            <p className="text-slate-400">Caricamento utenti...</p>
          ) : users.length === 0 ? (
            <p className="text-slate-400">Nessun utente trovato.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead>
                  <tr className="text-slate-400 border-b border-white/10">
                    <th className="py-2 pr-4 font-medium">Username</th>
                    <th className="py-2 pr-4 font-medium">Email</th>
                    <th className="py-2 pr-4 font-medium">Ruolo</th>
                    <th className="py-2 pr-4 font-medium">Azioni</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {users.map((u) => (
                    <tr key={u.id} className="text-slate-200">
                      <td className="py-3 pr-4 font-medium">
                        {editingId === u.id ? (
                          <input
                            value={editUsername}
                            onChange={(e) => setEditUsername(e.target.value)}
                            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800/50 px-3 text-sm text-white"
                          />
                        ) : (
                          u.username
                        )}
                      </td>
                      <td className="py-3 pr-4 text-slate-400">
                        {editingId === u.id ? (
                          <input
                            type="email"
                            value={editEmail}
                            onChange={(e) => setEditEmail(e.target.value)}
                            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800/50 px-3 text-sm text-white"
                          />
                        ) : (
                          u.email
                        )}
                      </td>
                      <td className="py-3 pr-4">
                        <select
                          value={u.role}
                          disabled={pendingRoleId === u.id || u.id === currentUser?.id}
                          onChange={(e) => handleRoleChange(u.id, e.target.value)}
                          className="h-9 rounded-lg border border-slate-700 bg-slate-800/50 px-3 text-sm text-white disabled:opacity-50"
                        >
                          {ROLES.map((r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex gap-2">
                          {editingId === u.id ? (
                            <>
                              <Button type="button" size="sm" title="Salva" onClick={() => handleSaveEdit(u.id)}>
                                <Check className="w-4 h-4" />
                              </Button>
                              <Button type="button" variant="secondary" size="sm" title="Annulla" onClick={() => setEditingId(null)}>
                                <X className="w-4 h-4" />
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button type="button" size="sm" title="Modifica anagrafica" onClick={() => startEdit(u)}>
                                <Pencil className="w-4 h-4" />
                              </Button>
                              <Button type="button" variant="secondary" size="sm" title="Reset password" onClick={() => handleResetPassword(u.id, u.username)}>
                                <KeyRound className="w-4 h-4" />
                              </Button>
                              <Button
                                type="button"
                                variant="danger"
                                size="sm"
                                disabled={u.id === currentUser?.id}
                                title={u.id === currentUser?.id ? 'Non puoi eliminare il tuo stesso account' : 'Elimina utente'}
                                onClick={() => handleDelete(u.id, u.username)}
                              >
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default UserManagementPage;
