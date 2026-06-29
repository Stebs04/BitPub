import React from 'react';

const DashboardPage: React.FC = () => {
  return (
    <div className="p-8 animate-slide-up">
      <h1 className="text-3xl font-bold text-white mb-6">Dashboard</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Partite Attive</h2>
          <p className="text-4xl font-bold text-brand-light">12</p>
        </div>
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Tornei in Corso</h2>
          <p className="text-4xl font-bold text-accent-pink">3</p>
        </div>
        <div className="glass-panel p-6">
          <h2 className="text-xl font-semibold text-white mb-2">Giocatori Online</h2>
          <p className="text-4xl font-bold text-emerald-400">45</p>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
