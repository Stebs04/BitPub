import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TournamentsPage from './TournamentsPage';
import { useAuthStore } from '../store/authStore';
import * as api from '../services/api';
import { notificationService } from '../services/notificationService';

vi.mock('../services/api', () => ({
  getAllTournaments: vi.fn(),
  getTournamentRegistrationsByPlayer: vi.fn(),
  registerToTournament: vi.fn(),
  getOnlineLocales: vi.fn(),
  getTournamentRankings: vi.fn(),
  createTournament: vi.fn(), updateTournament: vi.fn(), deleteTournament: vi.fn(),
  endTournament: vi.fn(), generateTournamentBracket: vi.fn(), updateTournamentMatchResult: vi.fn(),
  getGameTypes: vi.fn(),
}));
vi.mock('../services/notificationService', () => ({
  notificationService: { subscribe: vi.fn(() => () => {}) },
}));
vi.mock('../components/PlayFlow', () => ({ default: () => <div>play-flow</div> }));

const tournament = {
  id: 't1', name: 'Coppa Estate', gameTypeId: 'foosball', teamBased: false,
  status: 'UPCOMING', maxParticipants: 8, registrations: [], bracket: [], localeIds: [],
};

describe('TournamentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().login('tok', { id: 'u1', username: 'alice', role: 'PLAYER', localeId: null });
    (api.getAllTournaments as any).mockResolvedValue({ data: [tournament] });
    (api.getTournamentRegistrationsByPlayer as any).mockResolvedValue({ data: [] });
    (api.getOnlineLocales as any).mockResolvedValue({ data: [{ id: 'loc1', name: 'Bar Centrale' }] });
    (api.getGameTypes as any).mockResolvedValue({ data: [{ id: 'foosball', name: 'Calcio Balilla' }] });
  });

  it('lists tournaments and registers a player', async () => {
    (api.registerToTournament as any).mockResolvedValue({ data: { id: 'r1', tournamentId: 't1' } });
    render(<TournamentsPage />);
    expect(await screen.findByText('Coppa Estate')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Iscrivi giocatore/i }));
    await waitFor(() =>
      expect(api.registerToTournament).toHaveBeenCalledWith('t1', expect.objectContaining({ participantId: 'u1', team: false })),
    );
    expect(await screen.findByText('Sei iscritto')).toBeInTheDocument();
  });

  // Live: tournament-service pubblica il TournamentDto su bitpub/tournaments/+/state (avvio/avanzamento).
  it('adds a new tournament from an MQTT state message', async () => {
    render(<TournamentsPage />);
    await screen.findByText('Coppa Estate');

    const handler = (notificationService.subscribe as any).mock.calls
      .find((c: any[]) => c[0] === 'bitpub/tournaments/+/state')[1];
    act(() => handler({ ...tournament, id: 't2', name: 'Coppa Inverno' }));

    expect(await screen.findByText('Coppa Inverno')).toBeInTheDocument();
  });
});
