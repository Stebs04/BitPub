/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import LeaderboardPage from './LeaderboardPage';
import { useAuthStore } from '../store/authStore';
import { statsApi, getGameTypes } from '../services/api';
import { notificationService } from '../services/notificationService';

vi.mock('../services/api', () => ({
  statsApi: { get: vi.fn() },
  getGameTypes: vi.fn(),
  backfillStats: vi.fn(),
}));
vi.mock('../services/notificationService', () => ({
  notificationService: { subscribe: vi.fn(() => () => {}) },
}));
vi.mock('../hooks/useGameTypeLabels', () => ({ upsertGameTypeLabel: vi.fn() }));

// Voce di classifica di esempio per i test
const entry = {
  id: 'e1', playerName: 'alice', gameTypeId: 'foosball',
  wins: 3, losses: 1, totalPoints: 30, matchesPlayed: 4, lastUpdated: null,
};

describe('LeaderboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().login('tok', { id: 'u1', username: 'alice', role: 'PLAYER', localeId: null });
    (getGameTypes as any).mockResolvedValue({ data: [{ id: 'foosball', name: 'Calcio Balilla' }] });
    (statsApi.get as any).mockResolvedValue({ data: [entry] });
  });

  // Verifica il caricamento delle schede dei giochi e la visualizzazione della classifica
  it('loads catalog tabs and shows the leaderboard rows', async () => {
    render(<LeaderboardPage />);
    expect(await screen.findByText('alice')).toBeInTheDocument();
    expect(screen.getAllByText('Calcio Balilla').length).toBeGreaterThan(0);
  });

  // Verifica l'aggiornamento in tempo reale al ricevimento di un messaggio MQTT
  it('updates live when a statistics MQTT message arrives', async () => {
    (statsApi.get as any).mockResolvedValue({ data: [] });   // parte vuota
    render(<LeaderboardPage />);
    // attende che il tab del catalogo sia attivo (header col nome gioco) prima di iniettare l'update
    await screen.findByText(/Calcio Balilla — Top Giocatori/i);

    // handler del topic bitpub/statistics/update/+
    const call = (notificationService.subscribe as any).mock.calls
      .find((c: any[]) => c[0] === 'bitpub/statistics/update/+');
    act(() => call[1]({ gameTypeId: 'foosball', entries: [entry] }));

    expect(await screen.findByText('alice')).toBeInTheDocument();
  });
});
