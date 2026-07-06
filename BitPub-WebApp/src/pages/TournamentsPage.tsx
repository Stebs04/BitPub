import React, { useEffect, useState } from 'react';
import { Swords, CalendarDays, CheckCircle2, MapPin, Plus, Play, StopCircle, Trophy, ChevronDown } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { useAuthStore } from '../store/authStore';
import {
  getAllTournaments,
  getTournamentRegistrationsByPlayer,
  registerToTournament,
  getOnlineLocales,
  getTournamentRankings,
  createTournament,
  startTournament,
  endTournament,
} from '../services/api';
import type {
  TournamentRecord,
  TournamentRegistrationRecord,
  LocaleRecord,
  TournamentRankingRecord,
} from '../services/api';

const GAME_TYPE_LABELS: Record<string, string> = {
  foosball: 'Calciobalilla',
  darts: 'Freccette',
  billiards: 'Biliardo',
};

const GAME_TYPE_OPTIONS = Object.entries(GAME_TYPE_LABELS); // [id, label]

const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  UPCOMING: { label: 'In arrivo', className: 'bg-slate-500/20 text-slate-300 border-slate-500/30' },
  ACTIVE: { label: 'In corso', className: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' },
  COMPLETED: { label: 'Concluso', className: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30' },
};

/**
 * Pagina Tornei: il PLAYER sfoglia e si iscrive; PLATFORM_ADMIN / LOCALE_ADMIN creano,
 * avviano e concludono i tornei. La classifica di ogni torneo e' espandibile e viene
 * sincronizzata dai risultati dei match (statistics-service) lato backend.
 */
const TournamentsPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const role = user?.role;
  const isManager = role === 'PLATFORM_ADMIN' || role === 'LOCALE_ADMIN';

  const [tournaments, setTournaments] = useState<TournamentRecord[]>([]);
  const [myRegistrations, setMyRegistrations] = useState<TournamentRegistrationRecord[]>([]);
  const [onlineLocales, setOnlineLocales] = useState<LocaleRecord[]>([]);
  const [selectedLocaleId, setSelectedLocaleId] = useState('');
  const [loading, setLoading] = useState(true);
  const [registeringId, setRegisteringId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Classifica espandibile per torneo
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [rankings, setRankings] = useState<Record<string, TournamentRankingRecord[]>>({});

  // Form creazione torneo (solo manager)
  const [newName, setNewName] = useState('');
  const [newGameTypeId, setNewGameTypeId] = useState('foosball');
  const [newTeamBased, setNewTeamBased] = useState(false);
  const [newLocaleIds, setNewLocaleIds] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);

  const loadTournaments = () => getAllTournaments().then((res) => setTournaments(res.data || []));

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    Promise.all([getAllTournaments(), getTournamentRegistrationsByPlayer(user.id), getOnlineLocales()])
      .then(([tournamentsRes, registrationsRes, localesRes]) => {
        if (cancelled) return;
        setTournaments(tournamentsRes.data || []);
        setMyRegistrations(registrationsRes.data || []);
        setOnlineLocales(localesRes.data || []);
        if (localesRes.data?.length) setSelectedLocaleId(localesRes.data[0].id);
      })
      .catch((err) => console.error('Error fetching tournaments:', err))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [user]);

  const registeredTournamentIds = new Set(myRegistrations.map((r) => r.tournamentId));

  const handleRegister = async (tournamentId: string) => {
    if (!user || !selectedLocaleId) return;
    setError(null);
    setRegisteringId(tournamentId);
    try {
      const res = await registerToTournament(tournamentId, {
        participantId: user.id,
        participantName: user.username,
        localeId: selectedLocaleId,
      });
      setMyRegistrations((prev) => [...prev, res.data]);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Iscrizione non riuscita.');
    } finally {
      setRegisteringId(null);
    }
  };

  const toggleRankings = async (tournamentId: string) => {
    if (expandedId === tournamentId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(tournamentId);
    try {
      const res = await getTournamentRankings(tournamentId);
      setRankings((prev) => ({ ...prev, [tournamentId]: res.data || [] }));
    } catch {
      setRankings((prev) => ({ ...prev, [tournamentId]: [] }));
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName || !newGameTypeId) return;
    setError(null);
    setCreating(true);
    try {
      await createTournament({ name: newName, gameTypeId: newGameTypeId, teamBased: newTeamBased, localeIds: newLocaleIds });
      setNewName('');
      setNewTeamBased(false);
      setNewLocaleIds([]);
      await loadTournaments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Creazione torneo non riuscita.');
    } finally {
      setCreating(false);
    }
  };

  const handleStart = async (id: string) => {
    try {
      await startTournament(id);
      await loadTournaments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Avvio non riuscito.');
    }
  };

  const handleEnd = async (id: string) => {
    try {
      await endTournament(id);
      await loadTournaments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Chiusura non riuscita.');
    }
  };

  const toggleLocaleInForm = (id: string) =>
    setNewLocaleIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  if (loading) {
    return (
      <div className="p-8 animate-slide-up">
        <h1 className="text-3xl font-bold text-white mb-6">Tornei</h1>
        <div className="text-white">Caricamento dati...</div>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-8 animate-slide-up space-y-6">
      <div className="flex items-center gap-3">
        <div className="bg-gradient-to-br from-accent-pink to-rose-600 p-2.5 rounded-xl">
          <Swords className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-white">Tornei</h1>
          <p className="text-slate-400 text-sm">
            {isManager ? 'Crea e gestisci i tornei della piattaforma' : 'Sfoglia i tornei disponibili e iscriviti'}
          </p>
        </div>
      </div>

      {error && (
        <div className="glass-panel border border-red-500/30 bg-red-500/10 px-4 py-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      {/* Form creazione torneo (manager) */}
      {isManager && (
        <Card>
          <CardHeader>
            <CardTitle>Nuovo Torneo</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="flex flex-col md:flex-row gap-4">
                <Input label="Nome torneo" value={newName} onChange={(e) => setNewName(e.target.value)} required />
                <div className="w-full flex flex-col gap-1.5">
                  <label className="text-sm font-medium text-slate-300">Tipo di gioco</label>
                  <select
                    className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
                    value={newGameTypeId}
                    onChange={(e) => setNewGameTypeId(e.target.value)}
                  >
                    {GAME_TYPE_OPTIONS.map(([id, label]) => (
                      <option key={id} value={id}>{label}</option>
                    ))}
                  </select>
                </div>
                <label className="flex items-center gap-2 text-sm text-slate-300 shrink-0 md:pt-7">
                  <input type="checkbox" checked={newTeamBased} onChange={(e) => setNewTeamBased(e.target.checked)} />
                  A squadre
                </label>
              </div>

              {onlineLocales.length > 0 && (
                <div>
                  <label className="text-sm font-medium text-slate-300">Locali coinvolti</label>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {onlineLocales.map((l) => (
                      <button
                        type="button"
                        key={l.id}
                        onClick={() => toggleLocaleInForm(l.id)}
                        className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-all ${
                          newLocaleIds.includes(l.id)
                            ? 'bg-brand/20 text-brand-light border-brand/40'
                            : 'bg-slate-800 text-slate-400 border-slate-700 hover:text-white'
                        }`}
                      >
                        {l.name}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <Button type="submit" disabled={creating}>
                <Plus className="w-4 h-4 mr-2" /> {creating ? 'Creazione...' : 'Crea Torneo'}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Selettore locale iscrizione (player) */}
      {!isManager && onlineLocales.length > 0 && (
        <Card>
          <CardContent className="flex flex-col md:flex-row md:items-center gap-3">
            <label className="text-sm font-medium text-slate-300 shrink-0">Iscriviti come giocatore del locale:</label>
            <select
              className="flex h-11 w-full md:w-72 rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
              value={selectedLocaleId}
              onChange={(e) => setSelectedLocaleId(e.target.value)}
            >
              {onlineLocales.map((l) => (
                <option key={l.id} value={l.id}>{l.name}</option>
              ))}
            </select>
          </CardContent>
        </Card>
      )}

      {tournaments.length === 0 && (
        <div className="glass-panel p-12 text-center">
          <p className="text-slate-400 text-lg">Nessun torneo disponibile al momento.</p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {tournaments.map((t) => {
          const isRegistered = registeredTournamentIds.has(t.id);
          const status = STATUS_LABELS[t.status] || STATUS_LABELS.UPCOMING;
          const canRegister = t.status === 'UPCOMING' && !isRegistered && onlineLocales.length > 0;
          const board = rankings[t.id] || [];

          return (
            <Card key={t.id}>
              <CardHeader>
                <div className="flex justify-between items-center">
                  <CardTitle>{t.name}</CardTitle>
                  <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${status.className}`}>
                    {status.label}
                  </span>
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-slate-400">{GAME_TYPE_LABELS[t.gameTypeId] || t.gameTypeId} · {t.teamBased ? 'A squadre' : 'Individuale'}</p>
                {(t.localeIds?.length ?? 0) > 0 && (
                  <p className="text-sm text-slate-500 flex items-center gap-2">
                    <MapPin className="w-4 h-4" /> {t.localeIds!.length} {t.localeIds!.length === 1 ? 'locale coinvolto' : 'locali coinvolti'}
                  </p>
                )}
                {t.startDate && (
                  <p className="text-sm text-slate-500 flex items-center gap-2">
                    <CalendarDays className="w-4 h-4" /> Inizio: {new Date(t.startDate).toLocaleDateString('it-IT')}
                  </p>
                )}

                <div className="flex flex-wrap items-center gap-2 pt-1">
                  {/* Iscrizione (player o chiunque) */}
                  {isRegistered ? (
                    <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                      <CheckCircle2 className="w-4 h-4" /> Sei iscritto
                    </div>
                  ) : (
                    !isManager && (
                      <Button
                        size="sm"
                        disabled={!canRegister || registeringId === t.id}
                        onClick={() => handleRegister(t.id)}
                      >
                        {registeringId === t.id ? 'Iscrizione...' : 'Iscriviti'}
                      </Button>
                    )
                  )}

                  {/* Controlli manager */}
                  {isManager && t.status === 'UPCOMING' && (
                    <Button size="sm" onClick={() => handleStart(t.id)}>
                      <Play className="w-4 h-4 mr-1" /> Avvia
                    </Button>
                  )}
                  {isManager && t.status === 'ACTIVE' && (
                    <Button size="sm" onClick={() => handleEnd(t.id)}>
                      <StopCircle className="w-4 h-4 mr-1" /> Concludi
                    </Button>
                  )}

                  {/* Classifica */}
                  <button
                    onClick={() => toggleRankings(t.id)}
                    className="flex items-center gap-1 text-sm text-brand-light hover:text-white transition-colors"
                  >
                    <Trophy className="w-4 h-4" /> Classifica
                    <ChevronDown className={`w-4 h-4 transition-transform ${expandedId === t.id ? 'rotate-180' : ''}`} />
                  </button>
                </div>

                {expandedId === t.id && (
                  <div className="pt-2 border-t border-white/5">
                    {board.length === 0 ? (
                      <p className="text-sm text-slate-500">Nessun dato in classifica. Iscrivi giocatori e avvia il torneo.</p>
                    ) : (
                      <table className="w-full text-left text-sm mt-2">
                        <thead>
                          <tr className="text-xs text-slate-500 uppercase">
                            <th className="py-1 px-2 w-10">#</th>
                            <th className="py-1 px-2">Partecipante</th>
                            <th className="py-1 px-2 text-center">Punti</th>
                            <th className="py-1 px-2 text-center">V</th>
                            <th className="py-1 px-2 text-center">Partite</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                          {board.map((r) => (
                            <tr key={r.id}>
                              <td className="py-2 px-2 font-bold text-brand-light">{r.currentRank}</td>
                              <td className="py-2 px-2 text-white">{r.participantName}</td>
                              <td className="py-2 px-2 text-center text-brand-light font-semibold">{r.score}</td>
                              <td className="py-2 px-2 text-center text-emerald-400">{r.matchesWon}</td>
                              <td className="py-2 px-2 text-center text-slate-400">{r.matchesPlayed}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
};

export default TournamentsPage;
