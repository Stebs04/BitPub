/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import DashboardPage from './DashboardPage';
import { useAuthStore } from '../store/authStore';
import api, { matchApi, getGlobalStats } from '../services/api';

// Mock dei servizi API per isolare i test
vi.mock('../services/api', () => ({
  default: { get: vi.fn() },
  matchApi: { get: vi.fn() },
  getGlobalStats: vi.fn(),
}));

describe('DashboardPage', () => {
  beforeEach(() => vi.clearAllMocks());

  // Verifica che l'admin della piattaforma veda le statistiche di sistema
  it('renders global system tiles for a PLATFORM_ADMIN', async () => {
    (getGlobalStats as any).mockResolvedValue({
      data: { totalLocales: 3, totalUsers: 42, activeMatches: 5, activeTournaments: 2 },
    });
    useAuthStore.getState().login('tok', { id: 'a1', username: 'admin', role: 'PLATFORM_ADMIN', localeId: null });
    render(<DashboardPage />);

    expect(await screen.findByText('Monitoraggio del Sistema')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();      // totalUsers
    expect(screen.getByText('Locali Registrati')).toBeInTheDocument();
  });

  // Verifica che il giocatore veda le proprie statistiche
  it('renders played/won tiles for a PLAYER', async () => {
    (matchApi.get as any).mockResolvedValue({ data: [{ id: 'm1', status: 'COMPLETED', teams: [] }] });
    (api.get as any).mockResolvedValue({ data: [] });
    useAuthStore.getState().login('tok', { id: 'p1', username: 'bob', role: 'PLAYER', localeId: null });
    render(<DashboardPage />);

    expect(await screen.findByText('Partite Giocate')).toBeInTheDocument();
    expect(screen.getByText('Vittorie')).toBeInTheDocument();
  });
});
