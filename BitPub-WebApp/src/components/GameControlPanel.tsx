import React, { useState } from 'react';
import { Loader2, Target, Circle, Crosshair } from 'lucide-react';

// Classificazione del gioco dal gameTypeId (stesso criterio a sottostringa usato altrove nella WebApp).
export type GameKind = 'FOOSBALL' | 'BILLIARDS' | 'DARTS' | 'UNKNOWN';

export const classifyGame = (gameTypeId: string = ''): GameKind => {
  const g = gameTypeId.toLowerCase();
  if (g.includes('calcio') || g.includes('foosball') || g.includes('balilla')) return 'FOOSBALL';
  if (g.includes('biliard') || g.includes('billiard') || g.includes('pool')) return 'BILLIARDS';
  if (g.includes('frecc') || g.includes('dart')) return 'DARTS';
  return 'UNKNOWN';
};

interface Props {
  gameTypeId: string;
  isMyTurn: boolean;
  finished: boolean;
  sending: boolean;
  breakDone: boolean;
  solidPlayerName?: string | null;
  stripedPlayerName?: string | null;
  throwsRemaining: number;
  myName: string;
  onShoot: () => void;
  onBreak: () => void;
  onThrow: (sector: number, multiplier: number) => void;
}

/**
 * GameControlPanel — joypad arcade interattivo del giocatore. Renderizza un pannello di
 * controllo diverso in base al tipo di gioco (Calciobalilla, Biliardo, Freccette) e disabilita
 * i controlli quando non e' il turno dell'utente o la partita e' finita.
 */
const GameControlPanel: React.FC<Props> = (props) => {
  const kind = classifyGame(props.gameTypeId);
  const locked = !props.isMyTurn || props.finished || props.sending;

  return (
    <div className="rounded-2xl border border-white/10 bg-gradient-to-b from-slate-900 to-slate-950 p-5 shadow-inner">
      {/* Banner turno */}
      <div className={`mb-5 rounded-xl px-4 py-3 text-center font-bold tracking-wide transition-colors ${
        props.finished
          ? 'bg-yellow-500/10 text-yellow-300 border border-yellow-500/30'
          : props.isMyTurn
            ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/40 animate-pulse'
            : 'bg-slate-800/60 text-slate-400 border border-slate-700'
      }`}>
        {props.finished
          ? '🏁 Partita terminata'
          : props.isMyTurn
            ? '🎮 È il tuo turno!'
            : '⏳ In attesa dell\'avversario...'}
      </div>

      {kind === 'FOOSBALL' && <FoosballPanel locked={locked} sending={props.sending} onShoot={props.onShoot} />}
      {kind === 'BILLIARDS' && (
        <BilliardsPanel
          locked={locked}
          sending={props.sending}
          breakDone={props.breakDone}
          solidPlayerName={props.solidPlayerName}
          stripedPlayerName={props.stripedPlayerName}
          myName={props.myName}
          onShoot={props.onShoot}
          onBreak={props.onBreak}
        />
      )}
      {kind === 'DARTS' && (
        <DartsPanel locked={locked} sending={props.sending} throwsRemaining={props.throwsRemaining} onThrow={props.onThrow} />
      )}
      {kind === 'UNKNOWN' && (
        <p className="text-center text-slate-500 text-sm py-6">Controlli non disponibili per questo tipo di gioco.</p>
      )}
    </div>
  );
};

// ─── Calciobalilla ──────────────────────────────────────────────────────────────
const FoosballPanel: React.FC<{ locked: boolean; sending: boolean; onShoot: () => void }> = ({ locked, sending, onShoot }) => (
  <div className="space-y-4">
    <p className="text-center text-xs uppercase tracking-widest text-slate-500 font-semibold">Calciobalilla</p>
    <button
      onClick={onShoot}
      disabled={locked}
      className="group w-full rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-600 py-8 text-2xl font-black text-white shadow-lg shadow-cyan-900/40 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
    >
      {sending ? <Loader2 className="mx-auto h-8 w-8 animate-spin" /> : (
        <span className="flex items-center justify-center gap-3">⚽ TIRA IN PORTA</span>
      )}
    </button>
    <p className="text-center text-xs text-slate-500">Il turno passa sempre all'avversario dopo il tiro.</p>
  </div>
);

// ─── Biliardo ──────────────────────────────────────────────────────────────────
const BilliardsPanel: React.FC<{
  locked: boolean;
  sending: boolean;
  breakDone: boolean;
  solidPlayerName?: string | null;
  stripedPlayerName?: string | null;
  myName: string;
  onShoot: () => void;
  onBreak: () => void;
}> = ({ locked, sending, breakDone, solidPlayerName, stripedPlayerName, myName, onShoot, onBreak }) => (
  <div className="space-y-4">
    <p className="text-center text-xs uppercase tracking-widest text-slate-500 font-semibold">Biliardo</p>

    {breakDone && (
      <div className="grid grid-cols-2 gap-3 text-center">
        <div className={`rounded-xl border p-3 ${solidPlayerName === myName ? 'border-amber-400/60 bg-amber-500/10' : 'border-slate-700 bg-slate-800/40'}`}>
          <div className="flex items-center justify-center gap-2 text-amber-300">
            <Circle className="h-4 w-4 fill-amber-400 text-amber-400" />
            <span className="text-sm font-bold">Piene</span>
          </div>
          <p className="mt-1 truncate text-xs text-slate-300">{solidPlayerName ?? '—'}{solidPlayerName === myName && ' (tu)'}</p>
        </div>
        <div className={`rounded-xl border p-3 ${stripedPlayerName === myName ? 'border-amber-400/60 bg-amber-500/10' : 'border-slate-700 bg-slate-800/40'}`}>
          <div className="flex items-center justify-center gap-2 text-amber-200">
            <Circle className="h-4 w-4 text-amber-300" />
            <span className="text-sm font-bold">Spezzate</span>
          </div>
          <p className="mt-1 truncate text-xs text-slate-300">{stripedPlayerName ?? '—'}{stripedPlayerName === myName && ' (tu)'}</p>
        </div>
      </div>
    )}

    {!breakDone ? (
      <button
        onClick={onBreak}
        disabled={locked}
        className="w-full rounded-2xl bg-gradient-to-br from-amber-600 to-yellow-500 py-8 text-2xl font-black text-slate-900 shadow-lg shadow-amber-900/40 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
      >
        {sending ? <Loader2 className="mx-auto h-8 w-8 animate-spin" /> : '💥 SPACCA'}
      </button>
    ) : (
      <button
        onClick={onShoot}
        disabled={locked}
        className="w-full rounded-2xl bg-gradient-to-br from-amber-600 to-orange-600 py-8 text-2xl font-black text-white shadow-lg shadow-amber-900/40 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
      >
        {sending ? <Loader2 className="mx-auto h-8 w-8 animate-spin" /> : '🎱 TIRA'}
      </button>
    )}
    <p className="text-center text-xs text-slate-500">
      {breakDone ? 'Se imbuchi mantieni il turno, altrimenti passa all\'avversario.' : 'La spaccata assegna Piene e Spezzate ai due giocatori.'}
    </p>
  </div>
);

// ─── Freccette ─────────────────────────────────────────────────────────────────
const DART_SECTORS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];

const DartsPanel: React.FC<{
  locked: boolean;
  sending: boolean;
  throwsRemaining: number;
  onThrow: (sector: number, multiplier: number) => void;
}> = ({ locked, sending, throwsRemaining, onThrow }) => {
  const [multiplier, setMultiplier] = useState<number>(1);

  const press = (sector: number, forced?: number) => {
    onThrow(sector, forced ?? multiplier);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-xs uppercase tracking-widest text-slate-500 font-semibold">Freccette</p>
        <div className="flex items-center gap-1.5">
          <span className="text-xs text-slate-500">Tiri:</span>
          {[0, 1, 2].map((i) => (
            <span key={i} className={`h-2.5 w-2.5 rounded-full ${i < throwsRemaining ? 'bg-rose-400' : 'bg-slate-700'}`} />
          ))}
        </div>
      </div>

      {/* Moltiplicatori */}
      <div className="grid grid-cols-3 gap-2">
        {[1, 2, 3].map((m) => (
          <button
            key={m}
            onClick={() => setMultiplier(m)}
            disabled={locked}
            className={`rounded-lg py-2 text-sm font-bold transition-all disabled:opacity-40 ${
              multiplier === m ? 'bg-rose-600 text-white shadow-md shadow-rose-900/40' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}
          >
            {m === 1 ? 'Singolo' : m === 2 ? 'Doppio ×2' : 'Triplo ×3'}
          </button>
        ))}
      </div>

      {/* Tastierino settori 1-20 */}
      <div className="grid grid-cols-5 gap-2">
        {DART_SECTORS.map((s) => (
          <button
            key={s}
            onClick={() => press(s)}
            disabled={locked}
            className="aspect-square rounded-lg bg-slate-800 text-base font-bold text-white transition-all hover:bg-rose-600 active:scale-90 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-slate-800"
          >
            {s}
          </button>
        ))}
      </div>

      {/* Bull / Outer Bull / Miss — moltiplicatore fisso */}
      <div className="grid grid-cols-3 gap-2">
        <button
          onClick={() => press(25, 1)}
          disabled={locked}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-emerald-700/70 py-3 text-sm font-bold text-white transition-all hover:bg-emerald-600 active:scale-95 disabled:opacity-40"
        >
          <Circle className="h-4 w-4" /> Outer 25
        </button>
        <button
          onClick={() => press(50, 1)}
          disabled={locked}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-red-700/80 py-3 text-sm font-bold text-white transition-all hover:bg-red-600 active:scale-95 disabled:opacity-40"
        >
          <Target className="h-4 w-4" /> Bull 50
        </button>
        <button
          onClick={() => press(0, 1)}
          disabled={locked}
          className="flex items-center justify-center gap-1.5 rounded-lg bg-slate-700 py-3 text-sm font-bold text-slate-200 transition-all hover:bg-slate-600 active:scale-95 disabled:opacity-40"
        >
          <Crosshair className="h-4 w-4" /> Fuori
        </button>
      </div>

      {sending && <div className="flex justify-center pt-1"><Loader2 className="h-5 w-5 animate-spin text-rose-400" /></div>}
      <p className="text-center text-xs text-slate-500">Seleziona il moltiplicatore, poi premi il settore. Dopo 3 tiri il turno passa.</p>
    </div>
  );
};

export default GameControlPanel;
