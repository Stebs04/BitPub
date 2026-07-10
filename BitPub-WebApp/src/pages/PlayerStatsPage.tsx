/**
 * Autore: Luca Franzon 20054744
 */
import React, { useEffect, useState } from 'react';
import { BarChart3, Trophy, Swords, Target } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import { useAuthStore } from '../store/authStore';
import { getMatchesByPlayer, getTournamentRegistrationsByPlayer, getMyLeaderboardStats } from '../services/api';
import type { MatchRecord, TournamentRegistrationRecord, LeaderboardEntryRecord } from '../services/api';
import { useGameTypeLabels } from '../hooks/useGameTypeLabels';

/**
 * Statistiche personali del PLAYER: aggrega dati gia' esposti dai servizi esistenti
 * (match-service per lo storico partite, statistics-service per la leaderboard,
 * tournament-service per le iscrizioni) senza duplicare logica di calcolo lato client
 * oltre alla semplice aggregazione per la vista.
 */
const PlayerStatsPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const gameLabels = useGameTypeLabels();

  // Stati per memorizzare i dati recuperati dalle API
  const [matches, setMatches] = useState<MatchRecord[]>([]);
  const [tournamentRegs, setTournamentRegs] = useState<TournamentRegistrationRecord[]>([]);
  
  // Rendimento per gioco: preso dalla leaderboard globale (stessa logica della classifica),
  // filtrata sul giocatore loggato. La leaderboard e' indicizzata per (playerName, gameTypeId),
  // quindi una sola riga per gioco: niente duplicati lato client.
  const [perGame, setPerGame] = useState<LeaderboardEntryRecord[]>([]);
  const [loading, setLoading] = useState(true);

  // Caricamento asincrono dei dati alla montatura del componente
  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    // Recupero parallelo per ottimizzare i tempi
    Promise.all([
      getMatchesByPlayer(user.id),
      getTournamentRegistrationsByPlayer(user.id),
      getMyLeaderboardStats(user.username),
    ])
      .then(([matchesRes, tournamentsRes, leaderboardRes]) => {
        if (cancelled) return;
        setMatches(matchesRes.data || []);
        setTournamentRegs(tournamentsRes.data || []);
        setPerGame(leaderboardRes.data || []);
      })
      .catch((error) => console.error('Error fetching player stats:', error))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [user]);

  // Statistiche derivate dallo storico partite (unica fonte di verita': winnerId dal match-service).
  // Evita la dipendenza dalla leaderboard, che e' un secondo sistema e puo' non essere popolato.
  const outcome = (m: MatchRecord): 'win' | 'loss' | 'draw' | null => {
    if (m.status !== 'COMPLETED') return null;
    if (!m.winnerId) return 'draw';
    return m.winnerId === user?.id ? 'win' : 'loss';
  };

  // Calcolo totale vittorie e sconfitte
  const totals = matches.reduce(
    (acc, m) => {
      const o = outcome(m);
      if (o === 'win') acc.wins++;
      else if (o === 'loss') acc.losses++;
      return acc;
    },
    { wins: 0, losses: 0 }
  );
  
  // Calcolo della percentuale di vittoria (Win Rate)
  const decided = totals.wins + totals.losses;
  const winRate = decided > 0 ? Math.round((totals.wins / decided) * 100) : 0;

  // Nasconde i tornei conclusi dalla vista "Le mie Iscrizioni" (i dati sono sincronizzati via MQTT).
  const activeRegs = tournamentRegs.filter((r) => r.tournamentStatus !== 'COMPLETED');
  const completedRegs = tournamentRegs.filter((r) => r.tournamentStatus === 'COMPLETED');

  // Vista di caricamento
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
      {/* Intestazione pagina */}
      <div className="flex items-center gap-3">
        <div className="bg-gradient-to-br from-brand to-brand-light p-2.5 rounded-xl">
          <BarChart3 className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-3xl font-bold text-white">Le mie Statistiche</h1>
          <p className="text-slate-400 text-sm">Il tuo rendimento su tutti i giochi</p>
        </div>
      </div>

      {/* Tiles riepilogo metriche globali */}
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
          <p className="text-3xl font-bold text-accent-pink">{activeRegs.length}</p>
        </Card>
      </div>

      {/* Statistiche dettagliate per gioco */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="w-5 h-5 text-brand-light" /> Rendimento per Gioco
          </CardTitle>
        </CardHeader>
        <CardContent>
          {perGame.length === 0 ? (
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
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Media</th>
                    <th className="py-2 px-3 text-xs font-semibold text-slate-500 uppercase text-center">Partite</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {perGame.map((row) => (
                    <tr key={row.gameTypeId}>
                      <td className="py-3 px-3 font-semibold text-white">{gameLabels[row.gameTypeId] || row.gameTypeId}</td>
                      <td className="py-3 px-3 text-center text-emerald-400 font-bold">{row.wins}</td>
                      <td className="py-3 px-3 text-center text-red-400 font-bold">{row.losses}</td>
                      <td className="py-3 px-3 text-center text-brand-light font-bold">{row.totalPoints}</td>
                      <td className="py-3 px-3 text-center text-slate-400">{row.matchesPlayed > 0 ? Math.round(row.totalPoints / row.matchesPlayed) : 0}</td>
                      <td className="py-3 px-3 text-center text-slate-400">{row.matchesPlayed}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Storico di tutte le partite giocate */}
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
          <div className="divide-y divide-white/5 max-h-[480px] overflow-y-auto">
              {matches.map((m) => {
                // Determina il team del giocatore corrente e quello avversario
                const myTeam = m.teams?.find((t) => t.playerIds?.includes(user?.id ?? ''));
                const oppTeam = m.teams?.find((t) => !t.playerIds?.includes(user?.id ?? ''));

                let resultLabel = m.status === 'IN_PROGRESS' ? 'In corso' : m.status;
                let resultClass = 'text-yellow-400';
                if (m.status === 'COMPLETED') {
                  if (!m.winnerId) {
                    resultLabel = 'Pareggio';
                    resultClass = 'text-slate-400';
                  } else if (m.winnerId === user?.id) {
                    resultLabel = '🏆 Vittoria';
                    resultClass = 'text-emerald-400 font-bold';
                  } else {
                    resultLabel = '❌ Sconfitta';
                    resultClass = 'text-red-400 font-bold';
                  }
                }

                // Genera la stringa del punteggio in base alla composizione dei team
                const scoreStr = myTeam && oppTeam
                  ? `${myTeam.score} – ${oppTeam.score}`
                  : m.teams?.map((t) => `${t.name} ${t.score}`).join(' vs ') ?? '—';

                return (
                  <div key={m.id} className="py-3 flex items-center justify-between text-sm gap-3">
                    <span className="text-slate-300 font-medium min-w-[90px]">{gameLabels[m.gameTypeId] || m.gameTypeId}</span>
                    <span className="text-slate-400 flex-1 text-center">{scoreStr}</span>
                    <span className={`text-right ${resultClass}`}>{resultLabel}</span>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Sezione tornei in corso o imminenti */}
      {activeRegs.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Trophy className="w-5 h-5 text-brand-light" /> Le mie Iscrizioni ai Tornei
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="divide-y divide-white/5">
              {activeRegs.map((r) => (
                <div key={r.id} className="py-3 flex items-center justify-between text-sm gap-3">
                  <span className="text-slate-300 font-medium min-w-[90px]">{r.participantName}</span>
                  <span className="text-slate-400 flex-1 text-center">{r.currentStage || '—'}</span>
                  <span className="text-slate-400 min-w-[70px] text-center">{r.matchesPlayed ?? 0} partite</span>
                  <span className="text-brand-light font-bold min-w-[60px] text-right">{r.goalsScored ?? 0} punti</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Sezione storico tornei conclusi */}
      {completedRegs.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Trophy className="w-5 h-5 text-slate-400" /> Storico Tornei
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="divide-y divide-white/5">
              {completedRegs.map((r) => (
                <div key={r.id} className="py-3 flex items-center justify-between text-sm gap-3">
                  <span className="text-slate-300 font-medium min-w-[90px]">{r.participantName}</span>
                  <span className="text-slate-400 flex-1 text-center">{r.currentStage || '—'}</span>
                  <span className="text-slate-400 min-w-[70px] text-center">{r.matchesPlayed ?? 0} partite</span>
                  <span className="text-brand-light font-bold min-w-[60px] text-right">{r.goalsScored ?? 0} punti</span>
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
