/**
 * Autore: Luca Franzon 20054744
 */
import React, { useEffect, useState } from 'react';
import api, { matchApi, getGlobalStats } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { Activity } from 'lucide-react';

const LOCALE_ADMIN = 'LOCALE_ADMIN';
const PLATFORM_ADMIN = 'PLATFORM_ADMIN';

// Interfaccia per le tessere statistiche della dashboard
interface StatTile {
  label: string;
  value: number;
  color: string;
}

const DashboardPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const role = user?.role as string | undefined;

  const [tiles, setTiles] = useState<StatTile[]>([]);
  const [recentMatches, setRecentMatches] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [systemTiles, setSystemTiles] = useState<StatTile[]>([]);

  useEffect(() => {
    if (!user) return;

    // Recupera i dati della dashboard in base al ruolo dell'utente
    const fetchDashboardData = async () => {
      try {
        if (role === 'PLAYER') {
          // Statistiche per il giocatore (partite, vittorie, tornei)
          const [matchesRes, tournamentsRes] = await Promise.all([
            matchApi.get(`/by-player/${user.id}`),
            api.get(`/tournaments/by-player/${user.id}`),
          ]);
          const matches = matchesRes.data || [];
          const wins = matches.filter((m: any) =>
            m.status === 'COMPLETED' &&
            m.teams?.some((t: any) => t.playerIds?.includes(user.id) && t.score === Math.max(...m.teams.map((tt: any) => tt.score)))
          ).length;
          setTiles([
            { label: 'Partite Giocate', value: matches.length, color: 'text-brand-light' },
            { label: 'Vittorie', value: wins, color: 'text-emerald-400' },
            { label: 'Tornei Iscritti', value: tournamentsRes.data?.length || 0, color: 'text-accent-pink' },
          ]);
          setRecentMatches(matches.slice(0, 5));
        } else if (role === LOCALE_ADMIN) {
          const localesRes = await api.get(`/locales/by-admin/${user.id}`);
          const locales = localesRes.data || [];
          const allGames = locales.flatMap((l: any) => l.games || []);
          const activeGames = allGames.filter((g: any) => g.active);
          setTiles([
            { label: 'Locali Gestiti', value: locales.length, color: 'text-brand-light' },
            { label: 'Macchine Installate', value: allGames.length, color: 'text-emerald-400' },
            { label: 'Macchine Attive', value: activeGames.length, color: 'text-accent-pink' },
          ]);
        } else if (role === 'GAME_ADMIN') {
          const catalogRes = await api.get('/catalog/games');
          const gameTypes = catalogRes.data || [];
          const totalSensors = gameTypes.reduce((sum: number, g: any) => sum + (g.sensors?.length || 0), 0);
          setTiles([
            { label: 'Tipologie di Gioco', value: gameTypes.length, color: 'text-brand-light' },
            { label: 'Sensori Configurati', value: totalSensors, color: 'text-emerald-400' },
          ]);
        } else if (role === PLATFORM_ADMIN) {
          // PLATFORM_ADMIN — monitoraggio del sistema e statistiche globali della piattaforma
          const globalRes = await getGlobalStats();
          const stats = globalRes.data;

          setSystemTiles([
            { label: 'Locali Registrati', value: stats.totalLocales, color: 'text-brand-light' },
            { label: 'Utenti Registrati', value: stats.totalUsers, color: 'text-emerald-400' },
            { label: 'Partite Attive', value: stats.activeMatches, color: 'text-accent-pink' },
            { label: 'Tornei in Corso', value: stats.activeTournaments, color: 'text-yellow-400' },
          ]);
        }
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [user, role]);

  // Mostra stato di caricamento se i dati non sono pronti
  if (loading) {
    return (
      <div className="p-8 animate-slide-up">
        <h1 className="text-3xl font-bold text-white mb-6">Dashboard</h1>
        <div className="text-white">Caricamento dati...</div>
      </div>
    );
  }

  return (
    <div className="p-8 animate-slide-up">
      <h1 className="text-3xl font-bold text-white mb-6">Dashboard</h1>
      {tiles.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tiles.map((tile) => (
            <div key={tile.label} className="glass-panel p-6">
              <h2 className="text-xl font-semibold text-white mb-2">{tile.label}</h2>
              <p className={`text-4xl font-bold ${tile.color}`}>{tile.value}</p>
            </div>
          ))}
        </div>
      )}

      {role === PLATFORM_ADMIN && (
        <div className="mt-8">
          <div className="flex items-center gap-3 mb-4">
            <Activity className="w-6 h-6 text-brand-light" />
            <h2 className="text-2xl font-bold text-white">Monitoraggio del Sistema</h2>
          </div>
          <p className="text-slate-400 mb-4">Statistiche globali della piattaforma, aggregate da tutti i locali.</p>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {systemTiles.map((tile) => (
              <div key={tile.label} className="glass-panel p-6">
                <h3 className="text-lg font-semibold text-white mb-2">{tile.label}</h3>
                <p className={`text-4xl font-bold ${tile.color}`}>{tile.value}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {role === 'PLAYER' && recentMatches.length > 0 && (
        <div className="glass-panel mt-6 overflow-hidden">
          <div className="px-6 py-4 border-b border-white/5">
            <h2 className="text-lg font-semibold text-white">Le tue ultime partite</h2>
          </div>
          <div className="divide-y divide-white/5">
            {recentMatches.map((m) => (
              <div key={m.id} className="px-6 py-3 flex items-center justify-between text-sm">
                <span className="text-slate-300">{m.gameTypeId}</span>
                <span className="text-slate-400">
                  {m.teams?.map((t: any) => `${t.name} ${t.score}`).join(' vs ')}
                </span>
                <span className={m.status === 'COMPLETED' ? 'text-emerald-400' : 'text-yellow-400'}>{m.status}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
