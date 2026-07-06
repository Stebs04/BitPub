import React from 'react';
import { Trophy } from 'lucide-react';
import type { TournamentMatchRecord } from '../services/api';

interface Props {
  matches: TournamentMatchRecord[];
  // Se true (LOCALE_ADMIN) permette di scegliere il vincitore cliccando un giocatore.
  canEdit?: boolean;
  onSetWinner?: (match: TournamentMatchRecord, winnerId: string) => void;
}

const ROUND_LABEL = (round: number, totalRounds: number) => {
  const fromEnd = totalRounds - 1 - round;
  if (fromEnd === 0) return 'Finale';
  if (fromEnd === 1) return 'Semifinali';
  if (fromEnd === 2) return 'Quarti';
  return `Round ${round + 1}`;
};

/**
 * Tabellone a eliminazione diretta. Una colonna per round; i match sono impilati verticalmente.
 * ponytail: niente linee SVG di collegamento tra i match — colonne affiancate bastano a leggere
 * l'avanzamento. Aggiungere i connettori solo se richiesto esplicitamente.
 */
const TournamentBracket: React.FC<Props> = ({ matches, canEdit, onSetWinner }) => {
  if (!matches?.length) return null;

  const totalRounds = Math.max(...matches.map((m) => m.round)) + 1;
  const rounds: TournamentMatchRecord[][] = Array.from({ length: totalRounds }, (_, r) =>
    matches.filter((m) => m.round === r).sort((a, b) => a.matchIndex - b.matchIndex)
  );

  const slot = (m: TournamentMatchRecord, playerId: string | null, playerName: string | null) => {
    const decided = !!m.winnerId;
    const isWinner = decided && m.winnerId === playerId;
    const clickable = canEdit && !decided && !!playerId && !!m.player1Id && !!m.player2Id;
    return (
      <button
        type="button"
        disabled={!clickable}
        onClick={() => clickable && onSetWinner?.(m, playerId!)}
        className={[
          'flex items-center justify-between w-full px-3 py-1.5 text-sm text-left transition-colors',
          isWinner ? 'text-emerald-300 font-semibold' : 'text-slate-300',
          clickable ? 'hover:bg-brand-light/10 cursor-pointer' : 'cursor-default',
        ].join(' ')}
      >
        <span className="truncate">{playerName || <span className="text-slate-600">—</span>}</span>
        {isWinner && <Trophy className="w-3.5 h-3.5 shrink-0 ml-2" />}
      </button>
    );
  };

  return (
    <div className="pt-2 border-t border-white/5 overflow-x-auto">
      <div className="flex gap-6 min-w-max py-2">
        {rounds.map((roundMatches, r) => (
          <div key={r} className="flex flex-col justify-around gap-4 min-w-[190px]">
            <p className="text-xs uppercase tracking-wide text-slate-500 font-semibold">
              {ROUND_LABEL(r, totalRounds)}
            </p>
            {roundMatches.map((m) => (
              <div key={m.id} className="rounded-lg border border-slate-700 bg-slate-800/40 divide-y divide-slate-700">
                {slot(m, m.player1Id, m.player1Name)}
                {slot(m, m.player2Id, m.player2Name)}
                {m.score && (
                  <p className="px-3 py-1 text-[11px] text-slate-500 truncate" title={m.score}>
                    {m.score}
                  </p>
                )}
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default TournamentBracket;
