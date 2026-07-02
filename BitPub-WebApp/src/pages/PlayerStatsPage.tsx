import React, { useEffect, useState } from 'react';
import { BarChart3, Trophy, Swords, Target, TrendingUp } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { useAuthStore } from '../store/authStore';
import { getMatchesByPlayer, getMyLeaderboardStats, getTournamentRegistrationsByPlayer } from '../services/api';
import type { LeaderboardEntryRecord, MatchRecord, TournamentRegistrationRecord } from '../services/api';

const GAME_TYPE_LABELS: Record<string, string> = {
  foosball: 'Calciobalilla',
  darts: 'Freccette',
  billiards: 'Biliardo',
};

/**
 * Statistiche personali del PLAYER: aggrega dati gia' esposti dai servizi esistenti
 * (match-service per lo storico partite, statistics-service per la leaderboard,
 * tournament-service per le iscrizioni) senza duplicare logica di calcolo lato client
 * oltre alla semplice aggregazione per la vista.
 */
const PlayerStatsPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  const [matches, setMatches] = useState<MatchRecord[]>([]);
  const [leaderboardRows, setLeaderboardRows] = useState<LeaderboardEntryRecord[]>([]);
  const [tournamentRegs, setTournamentRegs] = useState<TournamentRegistrationRecord[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    Promise.all([
      getMatchesByPlayer(user.id),
      getMyLeaderboardStats(user.username),
      getTournamentRegistrationsByPlayer(user.id),
    ])
      .then(([matchesRes, leaderboardRes, tournamentsRes]) => {
        if (cancelled) return;
        setMatches(matchesRes.data || []);
        setLeaderboardRows(leaderboardRes.data || []);
        setTournamentRegs(tournamentsRes.data || []);
      })
      .catch((error) => console.error('Error fetching player stats:', error))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [user]);

  const totals = leaderboardRows.reduce(
    (acc, row) => ({
      wins: acc.wins + row.wins,
      losses: acc.losses + row.losses,
      totalPoints: acc.totalPoints + row.totalPoints,
      matchesPlayed: acc.matchesPlayed + row.matchesPlayed,
    }),
    { wins: 0, losses: 0, totalPoints: 0, matchesPlayed: 0 }
  );

  const winRate = totals.matchesPlayed > 0 ? Math.round((totals.wins / totals.matchesPlayed) * 100) : 0;

  if (loading) {
    return (
      <div className="p-8 animate-slide-up">
        <h1 className="text-3xl font-bold text-white mb-6">Le mie Statistiche</h1>
        <div className="text-white">Caricamento dati...</div>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-8 animate-slide-up space-y-8">
      <div className="flex items-center gap-3">
        <div className="bg-gradient-to-br from-brand to-brand-light p-2.5 rounded-xl">
          <BarChart3 className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-white">Le mie Statistiche</h1>
          <p className="text-slate-400 text-sm">Il tuo rendimento su tutti i giochi</p>
        </div>
      </div>

      {/* Tiles riepilogo */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-6">
        <Card>
          <p className="text-slate-400 text-sm mb-1">Partite Giocate</p>
          <p className="text-3xl font-bold text-brand-light">{matches.length}</p>
        </Card>
        <Card>
          <p className="text-slate-400 text-sm mb-1">Vittorie</p>
          <p className="text-3xl font-bold text-emerald-400">{totals.wins}</p>
        </Card>
        <Card>
          <p className="text-slate-400 text-sm mb-1">Sconfitte</p>
          <p className="text-3xl font-bold text-red-400">{totals.losses}</p>
        </Card>
        <Card>
          <p className="text-slate-400 text-sm mb-1">Win Rate</p>
          <p className="text-3xl font-bold text-white">{winRate}%</p>
        </Card>
        <Card>
          <p className="text-slate-400 text-sm mb-1">Tornei Iscritti</p>
          <p className="text-3xl font-bold text-accent-pink">{tournamentRegs.length}</p>
        </Card>
      </div>

      {/* Statistiche per gioco */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="w-5 h-5 text-brand-light" /> Rendimento per Gioco
          </CardTitle>
        </CardHeader>
        <CardContent>
          {leaderboardRows.length === 0 ? (
            <p className="text-slate-500 text-sm">Non hai ancora dati in classifica. Gioca una partita per iniziare!</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-white/5">
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase">Gioco</th>
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Vittorie</th>
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Sconfitte</th>
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Punti</th>
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Partite</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {leaderboardRows.map((row) => (
                    <tr key={row.id}>
                      <td className="py-3 px-3 font-semibold text-white">{GAME_TYPE_LABELS[row.gameTypeId] || row.gameTypeId}</td>
                      <td className="py-3 px-3 text-center text-emerald-400 font-bold">{row.wins}</td>
                      <td className="py-3 px-3 text-center text-red-400 font-bold">{row.losses}</td>
                      <td className="py-3 px-3 text-center text-brand-light font-bold">{row.totalPoints}</td>
                      <td className="py-3 px-3 text-center text-slate-400">{row.matchesPlayed}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Storico partite */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Swords className="w-5 h-5 text-brand-light" /> Storico Partite
          </CardTitle>
        </CardHeader>
        <CardContent>
          {matches.length === 0 ? (
            <p className="text-slate-500 text-sm">Nessuna partita giocata finora.</p>
          ) : (
            <div className="divide-y divide-white/5">
              {matches.slice(0, 15).map((m) => (
                <div key={m.id} className="py-3 flex items-center justify-between text-sm">
                  <span className="text-slate-300 font-medium">{GAME_TYPE_LABELS[m.gameTypeId] || m.gameTypeId}</span>
                  <span className="text-slate-400">
                    {m.teams?.map((t) => `${t.name} ${t.score}`).join(' vs ')}
                  </span>
                  <span className={`flex items-center gap-1 ${m.status === 'COMPLETED' ? 'text-emerald-400' : 'text-yellow-400'}`}>
                    {m.status === 'IN_PROGRESS' && <TrendingUp className="w-3.5 h-3.5" />}
                    {m.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {tournamentRegs.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Trophy className="w-5 h-5 text-brand-light" /> Le mie Iscrizioni ai Tornei
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="divide-y divide-white/5">
              {tournamentRegs.map((r) => (
                <div key={r.id} className="py-3 flex items-center justify-between text-sm">
                  <span className="text-slate-300">{r.participantName}</span>
                  <span className="text-slate-500 text-xs">
                    Iscritto il {r.registeredAt ? new Date(r.registeredAt).toLocaleDateString('it-IT') : '—'}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default PlayerStatsPage;
