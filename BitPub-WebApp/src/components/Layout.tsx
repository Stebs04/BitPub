import React from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { Gamepad2, LayoutDashboard, Trophy, MapPin, LogOut, Activity, Users, Boxes, BarChart3, Swords, Play } from 'lucide-react';
import { cn } from './Button';

const Layout: React.FC = () => {
  const logout = useAuthStore((state) => state.logout);
  const user = useAuthStore((state) => state.user);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isPlayer = user?.role === 'PLAYER';

  const navItems = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    // Il PLAYER gioca dalla scheda "Gioca"; Live Match e Locali restano agli amministratori.
    ...(isPlayer
      ? [{ to: '/games', icon: Play, label: 'Gioca' }]
      : [{ to: '/live', icon: Activity, label: 'Live Match' }]),
    { to: '/leaderboard', icon: Trophy, label: 'Leaderboard' },
    ...(isPlayer
      ? [
          { to: '/stats', icon: BarChart3, label: 'Le mie Statistiche' },
          { to: '/tournaments', icon: Swords, label: 'Tornei' },
        ]
      : [{ to: '/locales', icon: MapPin, label: 'Locali' }]),
    // Gestione tornei: PLATFORM_ADMIN e LOCALE_ADMIN (il PLAYER ha gia' Tornei sopra).
    ...(user?.role === 'PLATFORM_ADMIN' || user?.role === 'LOCALE_ADMIN'
      ? [{ to: '/tournaments', icon: Swords, label: 'Tornei' }]
      : []),
    ...(user?.role === 'GAME_ADMIN'
      ? [{ to: '/catalog', icon: Boxes, label: 'Catalogo' }]
      : []),
    ...(user?.role === 'PLATFORM_ADMIN'
      ? [{ to: '/admin/users', icon: Users, label: 'Utenti' }]
      : []),
  ];


  return (
    <div className="flex h-screen bg-brand-dark overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 flex-shrink-0 glass-panel m-4 flex flex-col justify-between">
        <div>
          <div className="p-6 flex items-center gap-3">
            <div className="bg-brand p-2 rounded-xl">
              <Gamepad2 className="w-6 h-6 text-white" />
            </div>
            <span className="text-xl font-bold text-white tracking-wide">BitPub</span>
          </div>
          
          <nav className="px-4 mt-6 flex flex-col gap-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => cn(
                  "flex items-center gap-3 px-4 py-3 rounded-xl transition-all font-medium",
                  isActive 
                    ? "bg-brand/20 text-brand-light border border-brand/30" 
                    : "text-slate-400 hover:text-white hover:bg-slate-800"
                )}
              >
                <item.icon className="w-5 h-5" />
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>

        <div className="p-4">
          <div className="mb-4 px-4">
            <p className="text-sm text-slate-400">Loggato come:</p>
            <p className="font-semibold text-white truncate">{user?.username || 'Utente'}</p>
          </div>
          <button 
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-red-400 hover:text-white hover:bg-red-500/20 transition-all font-medium"
          >
            <LogOut className="w-5 h-5" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto p-4 md:p-8">
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;
