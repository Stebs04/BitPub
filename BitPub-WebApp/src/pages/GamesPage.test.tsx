import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import GamesPage from './GamesPage';

// GamesPage e' solo un wrapper di <PlayFlow>: verifico l'inoltro, PlayFlow ha i suoi test.
vi.mock('../components/PlayFlow', () => ({ default: () => <div>play-flow</div> }));

describe('GamesPage', () => {
  it('renders PlayFlow', () => {
    render(<GamesPage />);
    expect(screen.getByText('play-flow')).toBeInTheDocument();
  });
});
