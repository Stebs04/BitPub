/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import GamesPage from './GamesPage';

// Mock del componente PlayFlow
vi.mock('../components/PlayFlow', () => ({ default: () => <div>play-flow</div> }));

describe('GamesPage', () => {
  // Verifica che il componente PlayFlow venga renderizzato correttamente
  it('renders PlayFlow', () => {
    render(<GamesPage />);
    expect(screen.getByText('play-flow')).toBeInTheDocument();
  });
});
