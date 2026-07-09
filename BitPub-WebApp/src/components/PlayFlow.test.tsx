import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PlayFlow from './PlayFlow';
import { useAuthStore } from '../store/authStore';
import * as api from '../services/api';
import { notificationService } from '../services/notificationService';

vi.mock('../services/api', () => ({
  getOnlineLocales: vi.fn(),
  joinMatchLobby: vi.fn(),
  getMatch: vi.fn(),
  postGameAction: vi.fn(),
  getGameTypes: vi.fn(),
}));

vi.mock('../services/notificationService', () => ({
  notificationService: { subscribe: vi.fn(() => () => {}) },
}));

const localeWithGame = {
  id: 'loc1', name: 'Bar Centrale', address: 'Via 1', adminId: 'admin1',
  games: [{ id: 'gi1', localInstanceId: 'pool-1', gameTypeId: 'pool', localeId: 'loc1', active: true }],
};

describe('PlayFlow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().login('tok', { id: 'u1', username: 'alice', role: 'PLAYER', localeId: null });
    (api.getGameTypes as any).mockResolvedValue({ data: [] });
  });

  it('shows an empty message when no locale is online', async () => {
    (api.getOnlineLocales as any).mockResolvedValue({ data: [] });
    render(<PlayFlow />);
    expect(await screen.findByText(/Nessun locale online/i)).toBeInTheDocument();
  });

  it('lists online locales and their active games', async () => {
    (api.getOnlineLocales as any).mockResolvedValue({ data: [localeWithGame] });
    render(<PlayFlow />);
    expect(await screen.findByText('Bar Centrale')).toBeInTheDocument();
    expect(screen.getByText(/pool-1/)).toBeInTheDocument();
  });

  it('joins a lobby and shows the waiting room', async () => {
    (api.getOnlineLocales as any).mockResolvedValue({ data: [localeWithGame] });
    (api.joinMatchLobby as any).mockResolvedValue({
      data: { id: 'm1', gameInstanceId: 'gi1', localeId: 'loc1', gameTypeId: 'pool', status: 'WAITING_FOR_PLAYERS', teams: [] },
    });
    render(<PlayFlow />);
    await screen.findByText('Bar Centrale');

    await userEvent.click(screen.getByRole('button', { name: /Gioca/i }));

    expect(await screen.findByText(/In attesa di un secondo giocatore/i)).toBeInTheDocument();
    expect(api.joinMatchLobby).toHaveBeenCalledWith('gi1', 'alice', undefined);
  });

  // Scenario 4: un messaggio MQTT di stato live (goal) aggiorna lo stato React della partita.
  it('updates the live score when an MQTT match-state message arrives', async () => {
    (api.getOnlineLocales as any).mockResolvedValue({ data: [localeWithGame] });
    (api.joinMatchLobby as any).mockResolvedValue({
      data: { id: 'm1', gameInstanceId: 'gi1', localeId: 'loc1', gameTypeId: 'pool', status: 'WAITING_FOR_PLAYERS', teams: [] },
    });
    render(<PlayFlow />);
    await screen.findByText('Bar Centrale');
    await userEvent.click(screen.getByRole('button', { name: /Gioca/i }));
    await screen.findByText(/In attesa di un secondo giocatore/i);

    // Recupera l'handler di sottoscrizione registrato per il topic match-state e simula un GOAL.
    const subscribeMock = notificationService.subscribe as any;
    await waitFor(() => expect(subscribeMock).toHaveBeenCalled());
    const handler = subscribeMock.mock.calls[subscribeMock.mock.calls.length - 1][1];

    act(() => {
      handler({
        matchId: 'm1', gameTypeId: 'pool', status: 'PLAYING',
        teamAName: 'alice', teamBName: 'bob', scoreTeamA: 1, scoreTeamB: 0,
        currentEventMessage: 'GOAL', currentTurnUserId: 'u1',
      });
    });

    expect(await screen.findByText('LIVE', { exact: false })).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();       // punteggio aggiornato dal messaggio MQTT
    expect(screen.queryByText(/In attesa di un secondo giocatore/i)).not.toBeInTheDocument();
  });
});
