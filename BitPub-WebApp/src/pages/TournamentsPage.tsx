import React, { useEffect, useState } from 'react';
import {
  Swords, CalendarDays, CheckCircle2, MapPin, Plus, Play, StopCircle, Trophy, ChevronDown,
  Pencil, Trash2, UserPlus, Users, X,
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import TournamentBracket from '../components/TournamentBracket';
import { useAuthStore } from '../store/authStore';
import {
  getAllTournaments,
  getTournamentRegistrationsByPlayer,
  registerToTournament,
  getOnlineLocales,
  getTournamentRankings,
  createTournament,
  updateTournament,
  deleteTournament,
  startTournament,
  endTournament,
  generateTournamentBracket,
  updateTournamentMatchResult,
} from '../services/api';
import type {
  TournamentRecord,
  TournamentRegistrationRecord,
  TournamentMatchRecord,
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

const EMPTY_FORM = { name: '', gameTypeId: 'foosball', teamBased: false, maxParticipants: 8 };
const MAX_PARTICIPANTS_OPTIONS = [4, 8, 16];

/**
 * Pagina Tornei. PLATFORM_ADMIN / LOCALE_ADMIN: CRUD tornei (il LOCALE_ADMIN solo sui propri
 * locali; modifica bloccata se ci sono iscritti). PLAYER: iscrizione come giocatore o come
 * squadra (nome squadra + membri). Classifica espandibile, sincronizzata dai match.
 */
const TournamentsPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const role = user?.role;
  // I tornei sono gestiti solo dal LOCALE_ADMIN (per il proprio locale). Il PLATFORM_ADMIN
  // vede solo elenco e classifiche. Il PLAYER si iscrive.
  const isManager = role === 'LOCALE_ADMIN';
  const isPlayer = role === 'PLAYER';

  const [tournaments, setTournaments] = useState<TournamentRecord[]>([]);
  const [myRegistrations, setMyRegistrations] = useState<TournamentRegistrationRecord[]>([]);
  const [onlineLocales, setOnlineLocales] = useState<LocaleRecord[]>([]);
  const [selectedLocaleId, setSelectedLocaleId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Classifica espandibile per torneo
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [rankings, setRankings] = useState<Record<string, TournamentRankingRecord[]>>({});

  // Form crea/modifica torneo (manager). editingId != null => modifica.
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Iscrizione a squadre: form inline per torneo
  const [teamFormId, setTeamFormId] = useState<string | null>(null);
  const [teamName, setTeamName] = useState('');
  const [teamMembers, setTeamMembers] = useState('');
  const [registeringId, setRegisteringId] = useState<string | null>(null);

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
  const hasEntrants = (t: TournamentRecord) => (t.registrations?.length ?? 0) > 0;

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

  // ── Iscrizione singolo giocatore ──────────────────────────────────────────────
  const handleRegisterPlayer = async (tournamentId: string) => {
    if (!user || !selectedLocaleId) return;
    setError(null);
    setRegisteringId(tournamentId);
    try {
      const res = await registerToTournament(tournamentId, {
        participantId: user.id,
        participantName: user.username,
        localeId: selectedLocaleId,
        team: false,
        members: [user.username],
      });
      setMyRegistrations((prev) => [...prev, res.data]);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Iscrizione non riuscita.');
    } finally {
      setRegisteringId(null);
    }
  };

  // ── Iscrizione squadra ────────────────────────────────────────────────────────
  const handleRegisterTeam = async (tournamentId: string) => {
    if (!user || !selectedLocaleId || !teamName.trim()) return;
    setError(null);
    setRegisteringId(tournamentId);
    const members = Array.from(
      new Set([user.username, ...teamMembers.split(',').map((s) => s.trim()).filter(Boolean)])
    );
    try {
      const res = await registerToTournament(tournamentId, {
        participantId: user.id,
        participantName: teamName.trim(),
        localeId: selectedLocaleId,
        team: true,
        members,
      });
      setMyRegistrations((prev) => [...prev, res.data]);
      setTeamFormId(null);
      setTeamName('');
      setTeamMembers('');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Iscrizione squadra non riuscita.');
    } finally {
      setRegisteringId(null);
    }
  };

  // ── CRUD manager ────────────────────────────────────────────────────────────────
  const startCreate = () => { setEditingId(null); setForm(EMPTY_FORM); };
  const startEdit = (t: TournamentRecord) => {
    setEditingId(t.id);
    setForm({ name: t.name, gameTypeId: t.gameTypeId, teamBased: t.teamBased, maxParticipants: t.maxParticipants ?? 8 });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.gameTypeId) return;
    setError(null);
    setSaving(true);
    try {
      // Il locale del torneo lo assegna il backend (quello del LOCALE_ADMIN); non si sceglie qui.
      const payload = { ...form, localeIds: [] as string[] };
      if (editingId) await updateTournament(editingId, payload);
      else await createTournament(payload);
      startCreate();
      await loadTournaments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Salvataggio torneo non riuscito.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (t: TournamentRecord) => {
    if (!window.confirm(`Eliminare il torneo "${t.name}"?`)) return;
    setError(null);
    try {
      await deleteTournament(t.id);
      if (editingId === t.id) startCreate();
      await loadTournaments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Eliminazione non riuscita.');
    }
  };

  const handleStart = async (id: string) => {
    try { await startTournament(id); await loadTournaments(); }
    catch (err: any) { setError(err?.response?.data?.message || 'Avvio non riuscito.'); }
  };
  const handleEnd = async (id: string) => {
    try { await endTournament(id); await loadTournaments(); }
    catch (err: any) { setError(err?.response?.data?.message || 'Chiusura non riuscita.'); }
  };
  const handleGenerateBracket = async (id: string) => {
    setError(null);
    try { await generateTournamentBracket(id); await loadTournaments(); }
    catch (err: any) { setError(err?.response?.data?.message || 'Generazione tabellone non riuscita.'); }
  };
  // ponytail: window.prompt per le statistiche invece di una modale dedicata — sufficiente per l'esito.
  const handleSetWinner = async (tournamentId: string, match: TournamentMatchRecord, winnerId: string) => {
    const stats = window.prompt('Statistiche / punteggio dello scontro (opzionale):', match.score || '') ?? undefined;
    setError(null);
    try { await updateTournamentMatchResult(tournamentId, match.id, winnerId, stats); await loadTournaments(); }
    catch (err: any) { setError(err?.response?.data?.message || 'Salvataggio esito non riuscito.'); }
  };

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
            {isManager ? 'Crea e gestisci i tornei dei tuoi locali' : 'Sfoglia i tornei disponibili e iscriviti'}
          </p>
        </div>
      </div>

      {error && (
        <div className="glass-panel border border-red-500/30 bg-red-500/10 px-4 py-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      {/* Form crea/modifica torneo (manager) */}
      {isManager && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>{editingId ? 'Modifica Torneo' : 'Nuovo Torneo'}</CardTitle>
              {editingId && (
                <button onClick={startCreate} className="text-slate-400 hover:text-white flex items-center gap-1 text-sm">
                  <X className="w-4 h-4" /> Annulla
                </button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSave} className="space-y-4">
              <div className="flex flex-col md:flex-row gap-4">
                <Input label="Nome torneo" value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required />
                <div className="w-full flex flex-col gap-1.5">
                  <label className="text-sm font-medium text-slate-300">Tipo di gioco</label>
                  <select
                    className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
                    value={form.gameTypeId}
                    onChange={(e) => setForm((p) => ({ ...p, gameTypeId: e.target.value }))}
                  >
                    {GAME_TYPE_OPTIONS.map(([id, label]) => (
                      <option key={id} value={id}>{label}</option>
                    ))}
                  </select>
                </div>
                <div className="w-full md:w-48 flex flex-col gap-1.5">
                  <label className="text-sm font-medium text-slate-300">Max partecipanti</label>
                  <select
                    className="flex h-11 w-full rounded-xl border border-slate-700 bg-slate-800/50 px-4 py-2 text-sm text-white"
                    value={form.maxParticipants}
                    onChange={(e) => setForm((p) => ({ ...p, maxParticipants: Number(e.target.value) }))}
                  >
                    {MAX_PARTICIPANTS_OPTIONS.map((n) => (
                      <option key={n} value={n}>{n}</option>
                    ))}
                  </select>
                </div>
                <label className="flex items-center gap-2 text-sm text-slate-300 shrink-0 md:pt-7">
                  <input type="checkbox" checked={form.teamBased} onChange={(e) => setForm((p) => ({ ...p, teamBased: e.target.checked }))} />
                  A squadre
                </label>
              </div>

              <p className="text-sm text-slate-500">Il torneo sarà associato automaticamente al tuo locale.</p>

              <Button type="submit" disabled={saving}>
                <Plus className="w-4 h-4 mr-2" /> {saving ? 'Salvataggio...' : editingId ? 'Salva modifiche' : 'Crea Torneo'}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Selettore locale iscrizione (player) */}
      {isPlayer && onlineLocales.length > 0 && (
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
          const locked = hasEntrants(t);
          const entrantsCount = t.registrations?.length ?? 0;
          const hasBracket = (t.bracket?.length ?? 0) > 0;
          const canGenerate =
            isManager && t.status === 'UPCOMING' && !hasBracket &&
            !!t.maxParticipants && entrantsCount >= t.maxParticipants;

          return (
            <Card key={t.id}>
              <CardHeader>
                <div className="flex justify-between items-center gap-2">
                  <CardTitle>{t.name}</CardTitle>
                  <div className="flex items-center gap-2">
                    <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${status.className}`}>
                      {status.label}
                    </span>
                    {isManager && (
                      <>
                        <button
                          onClick={() => startEdit(t)}
                          disabled={locked}
                          title={locked ? 'Modifica non consentita: torneo con iscritti' : 'Modifica'}
                          className="text-slate-400 hover:text-brand-light disabled:opacity-30 disabled:cursor-not-allowed"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleDelete(t)} title="Elimina" className="text-slate-400 hover:text-red-400">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-slate-400">{GAME_TYPE_LABELS[t.gameTypeId] || t.gameTypeId} · {t.teamBased ? 'A squadre' : 'Individuale'}</p>
                {t.maxParticipants && (
                  <p className="text-sm text-slate-500 flex items-center gap-2">
                    <Users className="w-4 h-4" /> Iscritti: {entrantsCount} / {t.maxParticipants}
                  </p>
                )}
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
                  {/* Iscrizione player: due tasti — giocatore o squadra */}
                  {isRegistered ? (
                    <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm">
                      <CheckCircle2 className="w-4 h-4" /> Sei iscritto
                    </div>
                  ) : (
                    isPlayer && (
                      <>
                        <Button size="sm" disabled={!canRegister || registeringId === t.id} onClick={() => handleRegisterPlayer(t.id)}>
                          <UserPlus className="w-4 h-4 mr-1" /> Iscrivi giocatore
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          disabled={!canRegister}
                          onClick={() => { setTeamFormId(teamFormId === t.id ? null : t.id); setTeamName(''); setTeamMembers(''); }}
                        >
                          <Users className="w-4 h-4 mr-1" /> Iscrivi squadra
                        </Button>
                      </>
                    )
                  )}

                  {isManager && t.status === 'UPCOMING' && (
                    <Button size="sm" onClick={() => handleStart(t.id)}>
                      <Play className="w-4 h-4 mr-1" /> Avvia
                    </Button>
                  )}
                  {canGenerate && (
                    <Button size="sm" onClick={() => handleGenerateBracket(t.id)}>
                      <Swords className="w-4 h-4 mr-1" /> Genera Tabellone
                    </Button>
                  )}
                  {isManager && t.status === 'ACTIVE' && (
                    <Button size="sm" onClick={() => handleEnd(t.id)}>
                      <StopCircle className="w-4 h-4 mr-1" /> Concludi
                    </Button>
                  )}

                  <button
                    onClick={() => toggleRankings(t.id)}
                    className="flex items-center gap-1 text-sm text-brand-light hover:text-white transition-colors ml-auto"
                  >
                    <Trophy className="w-4 h-4" /> Classifica
                    <ChevronDown className={`w-4 h-4 transition-transform ${expandedId === t.id ? 'rotate-180' : ''}`} />
                  </button>
                </div>

                {/* Form iscrizione squadra */}
                {teamFormId === t.id && (
                  <div className="pt-3 border-t border-white/5 space-y-3">
                    <Input label="Nome squadra" value={teamName} onChange={(e) => setTeamName(e.target.value)} placeholder="es. I Fulmini" />
                    <Input
                      label="Membri (username separati da virgola)"
                      value={teamMembers}
                      onChange={(e) => setTeamMembers(e.target.value)}
                      placeholder="mario, luigi (tu sei già incluso)"
                    />
                    <Button size="sm" disabled={!teamName.trim() || registeringId === t.id} onClick={() => handleRegisterTeam(t.id)}>
                      {registeringId === t.id ? 'Iscrizione...' : 'Conferma squadra'}
                    </Button>
                  </div>
                )}

                {/* Tabellone a eliminazione diretta — resta visibile anche a torneo COMPLETED */}
                {hasBracket && (
                  <TournamentBracket
                    matches={t.bracket!}
                    canEdit={isManager && t.status === 'ACTIVE'}
                    onSetWinner={(m, winnerId) => handleSetWinner(t.id, m, winnerId)}
                  />
                )}

                {/* Classifica */}
                {expandedId === t.id && (
                  <div className="pt-2 border-t border-white/5">
                    {board.length === 0 ? (
                      <p className="text-sm text-slate-500">Nessun dato in classifica. Iscrivi partecipanti e avvia il torneo.</p>
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
