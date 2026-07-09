import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GameCatalogPage from './GameCatalogPage';
import { getGameTypes } from '../services/api';

vi.mock('../services/api', () => ({
  getGameTypes: vi.fn(),
  createGameType: vi.fn(),
  updateGameType: vi.fn(),
  addSensorToGameType: vi.fn(),
  deleteSensor: vi.fn(),
  deleteGameType: vi.fn(),
}));

const games = [
  { id: 'foosball', name: 'Calcio Balilla', description: 'Biliardino', winScoreTarget: 8,
    sensors: [{ id: 's1', type: 'GOAL', description: 'goal', actuator: false, scoreIncrement: 1, successProbability: 1 }] },
  { id: 'darts', name: 'Freccette', description: '501', winScoreTarget: 501, sensors: [] },
];

describe('GameCatalogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (getGameTypes as any).mockResolvedValue({ data: games });
  });

  it('lists game types and shows the first one selected with its sensors', async () => {
    render(<GameCatalogPage />);
    expect(await screen.findByText('Tipologie (2)')).toBeInTheDocument();
    // primo tipo selezionato: dettaglio mostra target vittoria + il sensore definito (descrizione unica)
    expect(screen.getByText(/Vittoria a 8 punti/)).toBeInTheDocument();
    expect(screen.getByText('goal')).toBeInTheDocument();          // descrizione del sensore GOAL
  });

  it('selects another game type on click', async () => {
    render(<GameCatalogPage />);
    await screen.findByText('Tipologie (2)');
    await userEvent.click(screen.getByText('Freccette'));
    expect(screen.getByText('Nessun evento definito per questo gioco.')).toBeInTheDocument();
  });
});
