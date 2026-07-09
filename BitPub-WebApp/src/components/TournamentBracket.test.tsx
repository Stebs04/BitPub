import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TournamentBracket from './TournamentBracket';
import type { TournamentMatchRecord } from '../services/api';

function m(over: Partial<TournamentMatchRecord>): TournamentMatchRecord {
  return {
    id: 'x', round: 0, matchIndex: 0,
    player1Id: null, player1Name: null, player2Id: null, player2Name: null,
    winnerId: null, winnerName: null, score: null, nextMatchId: null, ...over,
  };
}

describe('TournamentBracket', () => {
  it('renders nothing when there are no matches', () => {
    const { container } = render(<TournamentBracket matches={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('labels rounds Semifinali / Finale for a 4-player bracket', () => {
    const matches = [
      m({ id: 's0', round: 0, matchIndex: 0, player1Id: 'a', player1Name: 'A', player2Id: 'b', player2Name: 'B' }),
      m({ id: 's1', round: 0, matchIndex: 1, player1Id: 'c', player1Name: 'C', player2Id: 'd', player2Name: 'D' }),
      m({ id: 'f', round: 1, matchIndex: 0 }),
    ];
    render(<TournamentBracket matches={matches} />);
    expect(screen.getByText('Semifinali')).toBeInTheDocument();
    expect(screen.getByText('Finale')).toBeInTheDocument();
  });

  it('calls onSetWinner when an editor clicks a player slot', async () => {
    const onSetWinner = vi.fn();
    const match = m({ id: 's0', player1Id: 'a', player1Name: 'Alice', player2Id: 'b', player2Name: 'Bob' });
    render(<TournamentBracket matches={[match]} canEdit onSetWinner={onSetWinner} />);

    await userEvent.click(screen.getByText('Alice'));
    expect(onSetWinner).toHaveBeenCalledWith(expect.objectContaining({ id: 's0' }), 'a');
  });

  it('shows a Gioca button for the logged player and calls onStartMatch', async () => {
    const onStartMatch = vi.fn();
    const match = m({ id: 's0', player1Id: 'me', player1Name: 'Me', player2Id: 'b', player2Name: 'Bob' });
    render(<TournamentBracket matches={[match]} currentUserId="me" onStartMatch={onStartMatch} />);

    const play = screen.getByRole('button', { name: /Gioca/i });
    await userEvent.click(play);
    expect(onStartMatch).toHaveBeenCalledWith(expect.objectContaining({ id: 's0' }));
  });
});
