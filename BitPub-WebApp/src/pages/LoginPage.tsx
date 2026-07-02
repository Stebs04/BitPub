import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    
    // Simulated login for now
    let role = 'PLAYER';
    if (username === 'platformadmin') role = 'PLATFORM_ADMIN';
    else if (username === 'gameadmin') role = 'GAME_ADMIN';
    else if (username === 'localeadmin') role = 'LOCALE_ADMIN';

    login('dummy-token', {
      id: '1',
      username: username || 'admin',
      role: role as any,
    });
    navigate('/');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-dark p-4">
      <div className="glass-panel p-8 max-w-md w-full animate-fade-in">
        <h1 className="text-3xl font-bold text-center text-white mb-6">BitPub Login</h1>
        <p className="text-slate-400 text-center mb-8">Accedi alla piattaforma distribuita</p>
        
        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1">Username</label>
            <input 
              type="text" 
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-brand focus:border-transparent transition-all"
              placeholder="Inserisci il tuo username"
              required
            />
          </div>
          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-300 mb-1">Password</label>
            <input 
              type="password" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:ring-2 focus:ring-brand focus:border-transparent transition-all"
              placeholder="Inserisci la tua password"
              required
            />
          </div>
          <button 
            type="submit"
            className="w-full bg-brand hover:bg-brand-light text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg hover:shadow-brand/50 mt-4">
            Sign In
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
