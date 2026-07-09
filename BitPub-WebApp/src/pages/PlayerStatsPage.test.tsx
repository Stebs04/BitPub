import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import PlayerStatsPage from './PlayerStatsPage';
import { useAuthStore } from '../store/authStore';
import { getMatchesByPlayer, getTournamentRegistrationsByPlayer, getMyLeaderboardStats } from '../services/api';

vi.mock('../services/api', () => ({
  getMatchesByPlayer: vi.fn(),
  getTournamentRegistrationsByPlayer: vi.fn(),
  getMyLeaderboardStats: vi.fn(),
}));
vi.mock('../hooks/useGameTypeLabels', () => ({ useGameTypeLabels: () => ({ foosball: 'Calcio Balilla' }) }));

describe('PlayerStatsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().login('tok', { id: 'u1', username: 'alice', role: 'PLAYER', localeId: null });
  });

  it('derives win/loss totals from match history', async () => {
    (getMatchesByPlayer as any).mockResolvedValue({ data: [
      { id: 'm1', gameTypeId: 'foosball', status: 'COMPLETED', winnerId: 'u1', teams: [] },
      { id: 'm2', gameTypeId: 'foosball', status: 'COMPLETED', winnerId: 'u2', teams: [] },
      { id: 'm3', gameTypeId: 'foosball', status: 'IN_PROGRESS', winnerId: null, teams: [] },
    ]});
    (getTournamentRegistrationsByPlayer as any).mockResolvedValue({ data: [] });
    (getMyLeaderboardStats as any).mockResolvedValue({ data: [] });

    render(<PlayerStatsPage />);

    // 3 partite, 1 vittoria, 1 sconfitta, win rate 50%
    expect(await screen.findByText('Win Rate')).toBeInTheDocument();
    expect(screen.getByText('Win Rate').nextSibling).toHaveTextContent('50%');
    expect(screen.getByText('Vittorie').nextSibling).toHaveTextContent('1');
    expect(screen.getByText('Sconfitte').nextSibling).toHaveTextContent('1');
  });
});
