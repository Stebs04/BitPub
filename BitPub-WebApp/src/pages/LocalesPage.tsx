import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { MapPin, Server, Plus, Power, Trash2, BarChart3 } from 'lucide-react';
import api, { deleteLocale, createLocale, addGameInstance, toggleGameInstance, getLocaleGameUsage } from '../services/api';
import type { GameUsageRecord } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useGameTypeLabels } from '../hooks/useGameTypeLabels';
import { pollUntil } from '../utils/poll';

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
  const gameLabels = useGameTypeLabels();

  const [locales, setLocales] = useState<Locale[]>([]);
  const [gameTypes, setGameTypes] = useState<GameType[]>([]);
  const [localeAdmins, setLocaleAdmins] = useState<UserOption[]>([]);
  const [gameUsage, setGameUsage] = useState<Record<string, GameUsageRecord[]>>({});
  const [loading, setLoading] = useState(true);
  // ponytail: un solo lock a livello di pagina blocca ogni mutazione durante il polling
  // post-Edge. Basta per una pagina admin; passare a lock per-locale se serve concorrenza.
  const [busy, setBusy] = useState(false);

  const [newLocaleName, setNewLocaleName] = useState('');
  const [newLocaleAddress, setNewLocaleAddress] = useState('');
  const [newLocaleAdminId, setNewLocaleAdminId] = useState('');
  const [newInstance, setNewInstance] = useState<Record<string, { localInstanceId: string; gameTypeId: string }>>({});

  const fetchLocales = useCallback(async (): Promise<Locale[]> => {
    if (!user) return [];
    try {
      const res = isLocaleAdmin
        ? await api.get<Locale[]>(`/locales/by-admin/${user.id}`)
        : await api.get<Locale[]>('/locales');
      const data = res.data || [];
      setLocales(data);
      return data;
    } catch (error) {
      console.error('Error fetching locales:', error);
      return [];
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

  // Metrica "Giochi piu' utilizzati in un locale": una chiamata per locale allo statistics-service.
  useEffect(() => {
    locales.forEach((l) => {
      getLocaleGameUsage(l.id)
        .then((res) => setGameUsage((prev) => ({ ...prev, [l.id]: res.data || [] })))
        .catch(() => {});
    });
  }, [locales]);

  const handleCreateLocale = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newLocaleName || !newLocaleAddress || !newLocaleAdminId) return;
    const prevLen = locales.length;
    setBusy(true);
    try {
      await createLocale({ name: newLocaleName, address: newLocaleAddress, adminId: newLocaleAdminId });
      setNewLocaleName('');
      setNewLocaleAddress('');
      setNewLocaleAdminId('');
      // Edge 202: attendo che il nuovo locale compaia nel DB cloud (eventual-consistent).
      await pollUntil(fetchLocales, (d) => d.length > prevLen);
    } catch (error) {
      console.error('Error creating locale:', error);
    } finally {
      setBusy(false);
    }
  };

  const handleAddGameInstance = async (localeId: string) => {
    const form = newInstance[localeId];
    if (!form?.localInstanceId || !form?.gameTypeId) return;
    const prevLen = locales.find((l) => l.id === localeId)?.games?.length || 0;
    setBusy(true);
    try {
      await addGameInstance(localeId, form);
      setNewInstance(prev => ({ ...prev, [localeId]: { localInstanceId: '', gameTypeId: '' } }));
      await pollUntil(fetchLocales, (d) => (d.find((l) => l.id === localeId)?.games?.length || 0) > prevLen);
    } catch (error) {
      console.error('Error adding game instance:', error);
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteLocale = async (localeId: string, localeName: string) => {
    if (!window.confirm(`Eliminare definitivamente il locale "${localeName}" e tutti i suoi dispositivi?`)) return;
    const prevLen = locales.length;
    setBusy(true);
    try {
      await deleteLocale(localeId);
      await pollUntil(fetchLocales, (d) => d.length < prevLen);
    } catch (error) {
      console.error('Error deleting locale:', error);
    } finally {
      setBusy(false);
    }
  };

  const handleToggleGameInstance = async (localeId: string, instance: GameInstance) => {
    const next = !instance.active;
    setBusy(true);
    try {
      await toggleGameInstance(localeId, instance.id, next);
      await pollUntil(fetchLocales, (d) =>
        d.find((l) => l.id === localeId)?.games?.find((g) => g.id === instance.id)?.active === next);
    } catch (error) {
      console.error('Error toggling game instance:', error);
    } finally {
      setBusy(false);
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
              <Button type="submit" className="shrink-0" disabled={busy}>
                <Plus className="w-4 h-4 mr-2" /> {busy ? 'Attendere...' : 'Crea Locale'}
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
                <div className="flex items-center gap-2">
                  <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-xs font-semibold">
                    ONLINE
                  </span>
                  {isPlatformAdmin && (
                    <button
                      onClick={() => handleDeleteLocale(locale.id, locale.name)}
                      disabled={busy}
                      className="text-slate-400 hover:text-red-400 disabled:opacity-40"
                      title="Elimina locale"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  )}
                </div>
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
                          disabled={busy}
                          className="text-slate-400 hover:text-white disabled:opacity-40"
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

              {/* Metrica: Giochi piu' utilizzati in questo locale */}
              {(gameUsage[locale.id]?.length ?? 0) > 0 && (
                <div className="mt-4 pt-4 border-t border-white/5">
                  <div className="flex items-center gap-2 mb-3 text-slate-300">
                    <BarChart3 className="w-4 h-4 text-brand" />
                    <span className="text-sm font-semibold">Giochi più utilizzati</span>
                  </div>
                  <div className="space-y-2">
                    {gameUsage[locale.id].map((g) => (
                      <div key={g.gameTypeId} className="flex items-center justify-between text-sm">
                        <span className="text-slate-300">{gameLabels[g.gameTypeId] || g.gameTypeId}</span>
                        <span className="text-slate-500 text-xs">
                          {g.matchesPlayed} partite · {g.players} giocatori
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

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
                  <Button size="sm" className="shrink-0" onClick={() => handleAddGameInstance(locale.id)} disabled={busy}>
                    <Plus className="w-4 h-4 mr-2" /> {busy ? 'Attendere...' : 'Aggiungi'}
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
