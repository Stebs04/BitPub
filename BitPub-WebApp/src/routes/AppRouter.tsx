import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import DashboardPage from '../pages/DashboardPage';
import LeaderboardPage from '../pages/LeaderboardPage';
import LocalesPage from '../pages/LocalesPage';
import LiveMatchView from '../pages/LiveMatchView';
import UserManagementPage from '../pages/UserManagementPage';
import GameCatalogPage from '../pages/GameCatalogPage';
import PlayerStatsPage from '../pages/PlayerStatsPage';
import TournamentsPage from '../pages/TournamentsPage';
import GamesPage from '../pages/GamesPage';
import Layout from '../components/Layout';
/**
 * Autore: Luca Franzon 20054744
 */

import { useAuthStore } from '../store/authStore';

// Protegge le rotte impedendo l'accesso agli utenti non autenticati
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
};

// Verifica che l'utente abbia il ruolo di amministratore di piattaforma
const PlatformAdminRoute = ({ children }: { children: React.ReactNode }) => {
  const isPlatformAdmin = useAuthStore((state) => state.isPlatformAdmin());
  if (!isPlatformAdmin) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

// Limita l'accesso esclusivamente agli amministratori di gioco
const GameAdminRoute = ({ children }: { children: React.ReactNode }) => {
  const role = useAuthStore((state) => state.user?.role);
  if (role !== 'GAME_ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

// Solo il PLAYER: matchmaking/gioco. Gli altri ruoli vengono rimandati alla home.
const PlayerRoute = ({ children }: { children: React.ReactNode }) => {
  const role = useAuthStore((state) => state.user?.role);
  if (role !== 'PLAYER') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

// Live Match e' una vista di monitoraggio: il PLAYER non deve accedervi (nemmeno via URL).
const NonPlayerRoute = ({ children }: { children: React.ReactNode }) => {
  const role = useAuthStore((state) => state.user?.role);
  if (role === 'PLAYER') {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

// Reindirizza l'utente alla home se ha già effettuato l'accesso
const RedirectIfAuthenticated = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
};

// Gestisce la navigazione e definisce tutte le rotte dell'applicazione
const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <RedirectIfAuthenticated>
              <LoginPage />
            </RedirectIfAuthenticated>
          }
        />

        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="leaderboard" element={<LeaderboardPage />} />
          <Route path="locales" element={<LocalesPage />} />
          <Route
            path="catalog"
            element={
              <GameAdminRoute>
                <GameCatalogPage />
              </GameAdminRoute>
            }
          />
          <Route
            path="live"
            element={
              <NonPlayerRoute>
                <LiveMatchView />
              </NonPlayerRoute>
            }
          />
          <Route
            path="games"
            element={
              <PlayerRoute>
                <GamesPage />
              </PlayerRoute>
            }
          />
          <Route path="stats" element={<PlayerStatsPage />} />
          <Route path="tournaments" element={<TournamentsPage />} />
          <Route
            path="admin/users"
            element={
              <PlatformAdminRoute>
                <UserManagementPage />
              </PlatformAdminRoute>
            }
          />
        </Route>

        {/* Fallback route */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
