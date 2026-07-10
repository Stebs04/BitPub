/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LiveMatchView from './LiveMatchView';
import { useAuthStore } from '../store/authStore';
import { notificationService } from '../services/notificationService';
import api from '../services/api';

vi.mock('../services/api', () => ({ default: { get: vi.fn() }, endMatch: vi.fn() }));
vi.mock('../services/notificationService', () => ({
  notificationService: {
    subscribe: vi.fn(() => () => {}),
    onConnectionChange: vi.fn(() => () => {}),
  },
}));
vi.mock('../hooks/useGameTypeLabels', () => ({ useGameTypeLabels: () => ({}) }));

// Mock della funzione di scroll del log (non implementata in jsdom)
Element.prototype.scrollIntoView = vi.fn();

function renderAs(role: string) {
  useAuthStore.getState().login('tok', { id: 'u1', username: 'sam', role: role as any, localeId: 'loc1' });
  return render(<MemoryRouter><LiveMatchView /></MemoryRouter>);
}

describe('LiveMatchView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (api.get as any).mockResolvedValue({ data: [] });
  });

  // Verifica lo stato iniziale quando non ci sono partite attive
  it('shows the empty state before any match arrives', () => {
    renderAs('PLATFORM_ADMIN');
    expect(screen.getByText(/Nessuna partita attiva/i)).toBeInTheDocument();
  });

  // Verifica l'aggiornamento della scheda punteggio all'arrivo di un evento live MQTT
  it('renders a live score card when an MQTT state message arrives', () => {
    renderAs('PLATFORM_ADMIN');
    const handler = (notificationService.subscribe as any).mock.calls[0][1];

    act(() => {
      handler(
        { matchId: 'm1', gameTypeId: 'foosball', status: 'PLAYING',
          teamAName: 'alice', teamBName: 'bob', scoreTeamA: 1, scoreTeamB: 0,
          currentEventMessage: 'GOAL' },
        'bitpub/match/loc1/gi1/state',
      );
    });

    expect(screen.getByText(/🔴 LIVE/)).toBeInTheDocument();
    expect(screen.getByText('alice')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  // Verifica che il LOCALE_ADMIN senza locale non si iscriva ai topic MQTT
  it('does not subscribe for a LOCALE_ADMIN with no locale assigned', () => {
    useAuthStore.getState().login('tok', { id: 'u1', username: 'sam', role: 'LOCALE_ADMIN', localeId: null });
    render(<MemoryRouter><LiveMatchView /></MemoryRouter>);
    expect(notificationService.subscribe).not.toHaveBeenCalled();
  });
});
