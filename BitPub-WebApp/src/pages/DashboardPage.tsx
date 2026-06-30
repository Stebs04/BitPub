import React, { useEffect, useState } from 'react';
import api from '../services/api';

const DashboardPage: React.FC = () => {
  const [activeMatches, setActiveMatches] = useState<number>(0);
  const [activeTournaments, setActiveTournaments] = useState<number>(0);
  const [registeredPlayers, setRegisteredPlayers] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [matchesRes, tournamentsRes, usersRes] = await Promise.all([
          api.get('http://localhost:8080/api/matches/active'),
          api.get('/tournaments/active'),
          api.get('/users')
        ]);
        
        const allMatches = matchesRes.data || [];
        const activeMatchesList = allMatches.filter((m: any) => m.status === 'IN_PROGRESS');
        
        const allUsers = usersRes.data || [];
        const playersCount = allUsers.filter((u: any) => u.role === 'PLAYER').length;
        
        setActiveMatches(activeMatchesList.length);
        setActiveTournaments(tournamentsRes.data?.length || 0);
        setRegisteredPlayers(playersCount);
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
          <h2 className="text-xl font-semibold text-white mb-2">Giocatori Registrati</h2>
          <p className="text-4xl font-bold text-emerald-400">{registeredPlayers}</p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
