import React, { useEffect, useState, useCallback } from 'react';
import { Trophy, Gamepad2, RefreshCw, Medal, DatabaseBackup } from 'lucide-react';
import { statsApi, backfillStats, getGameTypes } from '../services/api';
import { notificationService } from '../services/notificationService';
import { upsertGameTypeLabel } from '../hooks/useGameTypeLabels';
import { useAuthStore } from '../store/authStore';

interface LeaderboardEntry {
  id: string;
  playerName: string;
  gameTypeId: string;
  wins: number;
  losses: number;
  totalPoints: number;
  matchesPlayed: number;
  lastUpdated: string | null;
}

// Tab dei giochi: caricati dinamicamente dal catalogo (/catalog/games). id = gameTypeId reale.
interface GameTab { id: string; label: string; }

const RANK_COLORS: Record<number, string> = {
  0: 'text-yellow-400',
  1: 'text-slate-300',
  2: 'text-orange-400',
};

const RANK_ICONS: Record<number, string> = {
  0: '🥇',
  1: '🥈',
  2: '🥉',
};

const LeaderboardPage: React.FC = () => {
  const currentUsername = useAuthStore((state) => state.user?.username);
  const isPlatformAdmin = useAuthStore((state) => state.user?.role === 'PLATFORM_ADMIN');
  const [backfilling, setBackfilling] = useState(false);
  const [tabs, setTabs] = useState<GameTab[]>([]);
  const [activeTab, setActiveTab] = useState<string>('');
  const [data, setData] = useState<Record<string, LeaderboardEntry[]>>({});
  const [loading, setLoading] = useState(false);
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);

  // Tab dinamici dal catalogo: id = gameTypeId reale, nessuna mappa statica.
  useEffect(() => {
    getGameTypes()
      .then(res => {
        const loaded = res.data.map(g => ({ id: g.id, label: g.name || g.id }));
        setTabs(loaded);
        setActiveTab(prev => prev || loaded[0]?.id || '');
      })
      .catch(() => setTabs([]));
  }, []);

  // Nuovo gioco creato dal GAME_ADMIN: il catalog-service pubblica il GameType su bitpub/config/games/{id}.
  // Aggiunge/aggiorna la relativa tab (e la cache condivisa delle label) senza ricaricare la pagina.
  useEffect(() => {
    const unsubscribe = notificationService.subscribe(
      'bitpub/config/games/+',
      (payload: { id?: string; name?: string }, topic: string) => {
        const id = payload.id ?? topic.split('/')[3];
        if (!id) return;
        const label = payload.name || id;
        setTabs(prev =>
          prev.some(t => t.id === id)
            ? prev.map(t => (t.id === id ? { id, label } : t))
            : [...prev, { id, label }]
        );
        setActiveTab(prev => prev || id);
        upsertGameTypeLabel(id, label);
      }
    );
    return unsubscribe;
  }, []);

  const fetchLeaderboard = useCallback(async (gameTypeId: string) => {
    if (!gameTypeId) return;
    setLoading(true);
    try {
      const res = await statsApi.get<LeaderboardEntry[]>(`/leaderboard/${gameTypeId}`);
      setData(prev => ({ ...prev, [gameTypeId]: res.data }));
      setLastRefresh(new Date());
    } catch {
      // Keep stale data on error
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch on tab change and on mount
  useEffect(() => {
    fetchLeaderboard(activeTab);
  }, [activeTab, fetchLeaderboard]);

  // Aggiornamento live della leaderboard: lo statistics-service pubblica la classifica di ogni gioco
  // sul proprio topic bitpub/statistics/update/{gameTypeId}. Il wildcard '+' cattura tutti i giochi
  // con una sola sottoscrizione; il gameTypeId nel payload instrada l'update alla sezione giusta.
  useEffect(() => {
    const unsubscribe = notificationService.subscribe(
      'bitpub/statistics/update/+',
      (payload: { gameTypeId?: string; entries?: LeaderboardEntry[] }) => {
        if (!payload.gameTypeId || !payload.entries) return;
        setData((prev) => ({ ...prev, [payload.gameTypeId!]: payload.entries! }));
        setLastRefresh(new Date());
      }
    );
    return unsubscribe;
  }, []);

  const handleBackfill = async () => {
    setBackfilling(true);
    try {
      await backfillStats();
      await fetchLeaderboard(activeTab);
    } catch {
      // Errore gia' loggato dall'interceptor; mantiene i dati correnti
    } finally {
      setBackfilling(false);
    }
  };

  const currentEntries = data[activeTab] ?? [];
  const currentTab = tabs.find(t => t.id === activeTab);

  return (
    <div className="p-6 md:p-8 animate-slide-up">
      {/* Page header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
        <div className="flex items-center gap-3">
          <div className="bg-gradient-to-br from-yellow-500 to-orange-600 p-2.5 rounded-xl shadow-lg shadow-orange-900/30">
            <Trophy className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white">Leaderboard</h1>
            <p className="text-slate-400 text-sm">Classifiche reali per gioco</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {lastRefresh && (
            <span className="text-xs text-slate-500">
              Aggiornato: {lastRefresh.toLocaleTimeString('it-IT')}
            </span>
          )}
          {isPlatformAdmin && (
            <button
              id="leaderboard-backfill"
              onClick={handleBackfill}
              disabled={backfilling}
              title="Ricostruisce la classifica dai match gia' conclusi"
              className="flex items-center gap-2 px-4 py-2 bg-brand/10 hover:bg-brand/20 border border-brand/40 rounded-xl text-sm font-medium text-brand-light transition-all disabled:opacity-50"
            >
              <DatabaseBackup className={`w-4 h-4 ${backfilling ? 'animate-pulse' : ''}`} />
              Rigenera
            </button>
          )}
          <button
            id="leaderboard-refresh"
            onClick={() => fetchLeaderboard(activeTab)}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-sm font-medium text-slate-300 transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Aggiorna
          </button>
        </div>
      </div>

      {/* Game tabs (dinamici dal catalogo) */}
      <div className="flex flex-wrap gap-2 mb-6">
        {tabs.map(tab => (
          <button
            key={tab.id}
            id={`leaderboard-tab-${tab.id}`}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold border transition-all ${
              activeTab === tab.id
                ? 'bg-brand/10 border-brand/50 text-brand-light'
                : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-white hover:bg-slate-700'
            }`}
          >
            <Gamepad2 className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Leaderboard table */}
      <div className="glass-panel overflow-hidden">
        {/* Table header */}
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3 bg-brand/5">
          <Gamepad2 className="w-4 h-4 text-brand-light" />
          <h2 className="text-lg font-semibold text-white">{currentTab?.label ?? 'Gioco'} — Top Giocatori</h2>
          {currentEntries.length > 0 && (
            <span className="ml-auto text-xs text-slate-400">{currentEntries.length} giocatori</span>
          )}
        </div>

        {/* Empty state */}
        {!loading && currentEntries.length === 0 && (
          <div className="py-16 text-center">
            <Medal className="w-12 h-12 text-slate-600 mx-auto mb-4" />
            <p className="text-slate-400 text-lg font-semibold">Nessun dato disponibile</p>
            <p className="text-slate-500 text-sm mt-2">
              Gioca una partita per apparire in classifica!
            </p>
          </div>
        )}

        {/* Table */}
        {currentEntries.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/5">
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider w-16">Pos</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">Giocatore</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Vittorie</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Sconfitte</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Punti Totali</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Media</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Partite</th>
                  <th className="py-3 px-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-center">Win Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {currentEntries.map((entry, idx) => {
                  const winRate = entry.matchesPlayed > 0
                    ? Math.round((entry.wins / entry.matchesPlayed) * 100)
                    : 0;
                  const rankColor = RANK_COLORS[idx] ?? 'text-slate-300';
                  const rankIcon = RANK_ICONS[idx];
                  const isMe = !!currentUsername && entry.playerName.toLowerCase() === currentUsername.toLowerCase();

                  return (
                    <tr
                      key={entry.id}
                      className={`hover:bg-white/5 transition-colors group ${isMe ? 'bg-brand/10' : ''}`}
                    >
                      {/* Rank */}
                      <td className="py-4 px-4">
                        <div className={`text-xl font-black ${rankColor}`}>
                          {rankIcon ?? <span className="text-base text-slate-500">#{idx + 1}</span>}
                        </div>
                      </td>

                      {/* Player name */}
                      <td className="py-4 px-4">
                        <span className="font-semibold text-white text-base">{entry.playerName}</span>
                        {isMe && <span className="ml-2 text-xs font-semibold text-brand-light">(Tu)</span>}
                      </td>

                      {/* Wins */}
                      <td className="py-4 px-4 text-center">
                        <span className="inline-flex items-center justify-center w-10 h-10 bg-emerald-500/10 text-emerald-400 font-bold text-sm rounded-full">
                          {entry.wins}
                        </span>
                      </td>

                      {/* Losses */}
                      <td className="py-4 px-4 text-center">
                        <span className="inline-flex items-center justify-center w-10 h-10 bg-red-500/10 text-red-400 font-bold text-sm rounded-full">
                          {entry.losses}
                        </span>
                      </td>

                      {/* Total points */}
                      <td className="py-4 px-4 text-center">
                        <span className="font-bold text-brand-light text-lg">
                          {entry.totalPoints.toLocaleString('it-IT')}
                        </span>
                      </td>

                      {/* Average points */}
                      <td className="py-4 px-4 text-center text-slate-400 font-medium">
                        {entry.matchesPlayed > 0 ? Math.round(entry.totalPoints / entry.matchesPlayed) : 0}
                      </td>

                      {/* Matches played */}
                      <td className="py-4 px-4 text-center text-slate-400 font-medium">
                        {entry.matchesPlayed}
                      </td>

                      {/* Win rate */}
                      <td className="py-4 px-4 text-center">
                        <div className="flex flex-col items-center gap-1">
                          <span className={`text-sm font-bold ${winRate >= 50 ? 'text-emerald-400' : 'text-slate-400'}`}>
                            {winRate}%
                          </span>
                          <div className="w-16 h-1.5 bg-slate-700 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all ${winRate >= 50 ? 'bg-emerald-500' : 'bg-slate-500'}`}
                              style={{ width: `${winRate}%` }}
                            />
                          </div>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default LeaderboardPage;
