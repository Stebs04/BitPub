import React, { useEffect, useState } from 'react';
import { Swords, CalendarDays, CheckCircle2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import { useAuthStore } from '../store/authStore';
import {
  getAllTournaments,
  getTournamentRegistrationsByPlayer,
  registerToTournament,
  getOnlineLocales,
} from '../services/api';
import type { TournamentRecord, TournamentRegistrationRecord, LocaleRecord } from '../services/api';

const GAME_TYPE_LABELS: Record<string, string> = {
  foosball: 'Calciobalilla',
  darts: 'Freccette',
  billiards: 'Biliardo',
};

const STATUS_LABELS: Record<string, { label: string; className: string }> = {
  UPCOMING: { label: 'In arrivo', className: 'bg-slate-500/20 text-slate-300 border-slate-500/30' },
  ACTIVE: { label: 'In corso', className: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' },
  COMPLETED: { label: 'Concluso', className: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30' },
};

/**
 * Vista PLAYER: elenco tornei e iscrizione. L'iscrizione richiede un localeId (da cui il
 * giocatore partecipa), quindi si sceglie tra i locali attualmente ONLINE.
 */
const TournamentsPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  const [tournaments, setTournaments] = useState<TournamentRecord[]>([]);
  const [myRegistrations, setMyRegistrations] = useState<TournamentRegistrationRecord[]>([]);
  const [onlineLocales, setOnlineLocales] = useState<LocaleRecord[]>([]);
  const [selectedLocaleId, setSelectedLocaleId] = useState('');
  const [loading, setLoading] = useState(true);
  const [registeringId, setRegisteringId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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
          <p className="text-slate-400 text-sm">Sfoglia i tornei disponibili e iscriviti</p>
        </div>
      </div>

      {error && (
        <div className="glass-panel border border-red-500/30 bg-red-500/10 px-4 py-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      {onlineLocales.length > 0 && (
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
                {t.startDate && (
                  <p className="text-sm text-slate-500 flex items-center gap-2">
                    <CalendarDays className="w-4 h-4" /> Inizio: {new Date(t.startDate).toLocaleDateString('it-IT')}
                  </p>
                )}

                {isRegistered ? (
                  <div className="flex items-center gap-2 text-emerald-400 font-semibold text-sm pt-2">
                    <CheckCircle2 className="w-4 h-4" /> Sei iscritto
                  </div>
                ) : (
                  <Button
                    size="sm"
                    disabled={!canRegister || registeringId === t.id}
                    onClick={() => handleRegister(t.id)}
                  >
                    {registeringId === t.id ? 'Iscrizione...' : 'Iscriviti'}
                  </Button>
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
