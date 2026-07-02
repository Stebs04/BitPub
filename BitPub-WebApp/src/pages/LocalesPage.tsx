import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { MapPin, Server, Plus, Power } from 'lucide-react';
import api from '../services/api';
import { useAuthStore } from '../store/authStore';

// NOTE: the backend Role enum (bitpub-common) uses "LOCALE_ADMIN", not "LOCAL_ADMIN"
// as typed in authStore.ts — compare against the real value coming from the API.
const LOCALE_ADMIN = 'LOCALE_ADMIN';
const PLATFORM_ADMIN = 'PLATFORM_ADMIN';

interface GameInstance {
  id: string;
  localInstanceId: string;
  gameTypeId: string;
  active: boolean;
}

interface Locale {
  id: string;
  name: string;
  address: string;
  adminId: string;
  games: GameInstance[];
}

interface GameType {
  id: string;
  name: string;
}

interface UserOption {
  id: string;
  username: string;
}

const LocalesPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const role = user?.role as string | undefined;
  const isLocaleAdmin = role === LOCALE_ADMIN;
  const isPlatformAdmin = role === PLATFORM_ADMIN;

  const [locales, setLocales] = useState<Locale[]>([]);
  const [gameTypes, setGameTypes] = useState<GameType[]>([]);
  const [localeAdmins, setLocaleAdmins] = useState<UserOption[]>([]);
  const [loading, setLoading] = useState(true);

  const [newLocaleName, setNewLocaleName] = useState('');
  const [newLocaleAddress, setNewLocaleAddress] = useState('');
  const [newLocaleAdminId, setNewLocaleAdminId] = useState('');
  const [newInstance, setNewInstance] = useState<Record<string, { localInstanceId: string; gameTypeId: string }>>({});

  const fetchLocales = useCallback(async () => {
    if (!user) return;
    try {
      const res = isLocaleAdmin
        ? await api.get<Locale[]>(`/locales/by-admin/${user.id}`)
        : await api.get<Locale[]>('/locales');
      setLocales(res.data || []);
    } catch (error) {
      console.error('Error fetching locales:', error);
    } finally {
      setLoading(false);
    }
  }, [user, isLocaleAdmin]);

  useEffect(() => {
    fetchLocales();
    if (isLocaleAdmin) {
      api.get<GameType[]>('/catalog/games').then(res => setGameTypes(res.data || [])).catch(() => {});
    }
    if (isPlatformAdmin) {
      api.get<UserOption[]>(`/users/by-role/${LOCALE_ADMIN}`).then(res => setLocaleAdmins(res.data || [])).catch(() => {});
    }
  }, [fetchLocales, isLocaleAdmin, isPlatformAdmin]);

  const handleCreateLocale = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newLocaleName || !newLocaleAddress || !newLocaleAdminId) return;
    try {
      await api.post('/locales', { name: newLocaleName, address: newLocaleAddress, adminId: newLocaleAdminId });
      setNewLocaleName('');
      setNewLocaleAddress('');
      setNewLocaleAdminId('');
      fetchLocales();
    } catch (error) {
      console.error('Error creating locale:', error);
    }
  };

  const handleAddGameInstance = async (localeId: string) => {
    const form = newInstance[localeId];
    if (!form?.localInstanceId || !form?.gameTypeId) return;
    try {
      await api.post(`/locales/${localeId}/games`, form);
      setNewInstance(prev => ({ ...prev, [localeId]: { localInstanceId: '', gameTypeId: '' } }));
      fetchLocales();
    } catch (error) {
      console.error('Error adding game instance:', error);
    }
  };

  const handleToggleGameInstance = async (localeId: string, instance: GameInstance) => {
    try {
      await api.patch(`/locales/${localeId}/games/${instance.id}/status?active=${!instance.active}`);
      fetchLocales();
    } catch (error) {
      console.error('Error toggling game instance:', error);
    }
  };

  if (loading) {
    return (
      <div className="p-8 animate-slide-up">
        <h1 className="text-3xl font-bold text-white mb-6">Gestione Locali</h1>
        <div className="text-white">Caricamento dati...</div>
      </div>
    );
  }

  return (
    <div className="p-8 animate-slide-up">
      <div className="flex items-center gap-3 mb-8">
        <MapPin className="w-8 h-8 text-brand-light" />
        <h1 className="text-3xl font-bold text-white">Gestione Locali</h1>
      </div>

      {isPlatformAdmin && (
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Nuovo Locale</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateLocale} className="flex flex-col md:flex-row gap-4 items-end">
              <Input label="Nome" value={newLocaleName} onChange={(e) => setNewLocaleName(e.target.value)} required />
              <Input label="Indirizzo" value={newLocaleAddress} onChange={(e) => setNewLocaleAddress(e.target.value)} required />
              <div className="w-full flex flex-col gap-1.5">
                <label className="text-sm font-medium text-slate-300">Admin Locale</label>
                <select
                  className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
                  value={newLocaleAdminId}
                  onChange={(e) => setNewLocaleAdminId(e.target.value)}
                  required
                >
                  <option value="">Seleziona...</option>
                  {localeAdmins.map(a => (
                    <option key={a.id} value={a.id}>{a.username}</option>
                  ))}
                </select>
              </div>
              <Button type="submit" className="shrink-0">
                <Plus className="w-4 h-4 mr-2" /> Crea Locale
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {locales.length === 0 && (
        <div className="glass-panel p-12 text-center">
          <p className="text-slate-400 text-lg">Nessun locale trovato.</p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {locales.map((locale) => (
          <Card key={locale.id}>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>{locale.name}</CardTitle>
                <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-xs font-semibold">
                  ONLINE
                </span>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-slate-400 mb-4">{locale.address}</p>
              <div className="space-y-3">
                {(locale.games || []).map((game) => (
                  <div key={game.id} className="flex items-center justify-between gap-3 text-slate-300 bg-white/5 p-3 rounded-lg">
                    <div className="flex items-center gap-3">
                      <Server className="w-5 h-5 text-brand" />
                      <span>{game.localInstanceId} <span className="text-slate-500">({game.gameTypeId})</span></span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className={`text-xs font-semibold ${game.active ? 'text-emerald-400' : 'text-slate-500'}`}>
                        {game.active ? 'Attivo' : 'Disattivo'}
                      </span>
                      {isLocaleAdmin && (
                        <button
                          onClick={() => handleToggleGameInstance(locale.id, game)}
                          className="text-slate-400 hover:text-white"
                          title="Attiva/disattiva dispositivo simulato"
                        >
                          <Power className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                {(!locale.games || locale.games.length === 0) && (
                  <p className="text-sm text-slate-500 px-1">Nessuna macchina installata.</p>
                )}
              </div>

              {isLocaleAdmin && (
                <div className="mt-4 pt-4 border-t border-white/5 flex flex-col md:flex-row gap-3 items-end">
                  <Input
                    label="ID Macchina"
                    placeholder="es. calciobalilla-2"
                    value={newInstance[locale.id]?.localInstanceId || ''}
                    onChange={(e) => setNewInstance(prev => ({ ...prev, [locale.id]: { ...prev[locale.id], localInstanceId: e.target.value, gameTypeId: prev[locale.id]?.gameTypeId || '' } }))}
                  />
                  <div className="w-full flex flex-col gap-1.5">
                    <label className="text-sm font-medium text-slate-300">Tipo di Gioco</label>
                    <select
                      className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
                      value={newInstance[locale.id]?.gameTypeId || ''}
                      onChange={(e) => setNewInstance(prev => ({ ...prev, [locale.id]: { ...prev[locale.id], gameTypeId: e.target.value, localInstanceId: prev[locale.id]?.localInstanceId || '' } }))}
                    >
                      <option value="">Seleziona...</option>
                      {gameTypes.map(gt => (
                        <option key={gt.id} value={gt.name}>{gt.name}</option>
                      ))}
                    </select>
                  </div>
                  <Button size="sm" className="shrink-0" onClick={() => handleAddGameInstance(locale.id)}>
                    <Plus className="w-4 h-4 mr-2" /> Aggiungi
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default LocalesPage;
