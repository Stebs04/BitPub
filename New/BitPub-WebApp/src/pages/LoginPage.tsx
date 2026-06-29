import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);

  const handleLogin = () => {
    // Simulated login for now
    login('dummy-token', {
      id: '1',
      username: 'admin',
      role: 'PLATFORM_ADMIN',
    });
    navigate('/');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-brand-dark p-4">
      <div className="glass-panel p-8 max-w-md w-full animate-fade-in">
        <h1 className="text-3xl font-bold text-center text-white mb-6">BitPub Login</h1>
        <p className="text-slate-400 text-center mb-8">Accedi alla piattaforma distribuita</p>
        <button 
          onClick={handleLogin}
          className="w-full bg-brand hover:bg-brand-light text-white font-bold py-3 px-4 rounded-xl transition-all shadow-lg hover:shadow-brand/50">
          Sign In
        </button>
      </div>
    </div>
  );
};

export default LoginPage;
