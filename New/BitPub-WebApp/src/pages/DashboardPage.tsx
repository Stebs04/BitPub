import React, { useEffect, useState } from 'react';
import api from '../services/api';

const DashboardPage: React.FC = () => {
  const [activeMatches, setActiveMatches] = useState<number>(0);
  const [activeTournaments, setActiveTournaments] = useState<number>(0);
  const [onlinePlayers, setOnlinePlayers] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [matchesRes, tournamentsRes] = await Promise.all([
          api.get('http://localhost:8080/api/matches/active'),
          api.get('/tournaments/active')
        ]);
        
        const allMatches = matchesRes.data || [];
        const activeMatchesList = allMatches.filter((m: any) => m.status === 'IN_PROGRESS');
        
        let calculatedOnlinePlayers = 0;
        activeMatchesList.forEach((match: any) => {
          if (match.teams && Array.isArray(match.teams)) {
            match.teams.forEach((team: any) => {
              if (team.playerIds && Array.isArray(team.playerIds) && team.playerIds.length > 0) {
                calculatedOnlinePlayers += team.playerIds.length;
              } else {
                // Anonymous match (e.g., from simulator), assume 1 player per team
                calculatedOnlinePlayers += 1;
              }
            });
          }
        });
        
        setActiveMatches(activeMatchesList.length);
        setActiveTournaments(tournamentsRes.data?.length || 0);
        setOnlinePlayers(calculatedOnlinePlayers);
      } catch (error) {
        console.error('Error fetching dashboard data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

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
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Partite Attive</h2>
          <p className="text-4xl font-bold text-brand-light">{activeMatches}</p>
        </div>
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Tornei in Corso</h2>
          <p className="text-4xl font-bold text-accent-pink">{activeTournaments}</p>
        </div>
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Giocatori Online</h2>
          <p className="text-4xl font-bold text-emerald-400">{onlinePlayers}</p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
