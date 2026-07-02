import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { UserPlus } from 'lucide-react';
import { createUser } from '../services/api';

// NOTE: bitpub-common Role enum values (verbatim strings expected by the backend)
const ROLES = ['PLAYER', 'LOCALE_ADMIN', 'GAME_ADMIN', 'PLATFORM_ADMIN'];

const UserManagementPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState(ROLES[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

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
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Errore durante la creazione utente.');
    } finally {
      setLoading(false);
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
    </div>
  );
};

export default UserManagementPage;
