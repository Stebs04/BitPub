import { useState, useEffect } from 'react';
import axios from 'axios';
import { Gamepad2, Target, CircleDashed, Users, Play, Square, RefreshCw, Zap } from 'lucide-react';

const API_BASE = '/api';
const LOCALE_ID = 'LOCALE_001';

// ─── Types ────────────────────────────────────────────────────────────────────
type GameId = 'foosball-1' | 'darts-1' | 'billiards-1';
type GameType = 'calciobalilla' | 'freccette' | 'biliardo';

interface GameState {
  teamAName: string;
  teamBName: string;
  active: boolean;
}

interface EventLog {
  id: number;
  msg: string;
  time: string;
  color: string;
}

// Ball colors for billiards (Italian style 8-ball)
const BILLIARD_BALLS: { n: number; color: string; label: string }[] = [
  { n: 1, color: '#f59e0b', label: '1' },
  { n: 2, color: '#3b82f6', label: '2' },
  { n: 3, color: '#ef4444', label: '3' },
  { n: 4, color: '#8b5cf6', label: '4' },
  { n: 5, color: '#f97316', label: '5' },
  { n: 6, color: '#10b981', label: '6' },
  { n: 7, color: '#ec4899', label: '7' },
  { n: 8, color: '#1e293b', label: '8' },
  { n: 9, color: '#fcd34d', label: '9' },
  { n: 10, color: '#60a5fa', label: '10' },
  { n: 11, color: '#f87171', label: '11' },
  { n: 12, color: '#a78bfa', label: '12' },
  { n: 13, color: '#fb923c', label: '13' },
  { n: 14, color: '#34d399', label: '14' },
  { n: 15, color: '#f472b6', label: '15' },
];

// Dart sectors (1-20 + bullseye)
const DART_SECTORS = [20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5];

let logCounter = 0;

// ─── App ──────────────────────────────────────────────────────────────────────
function App() {
  const [gameStates, setGameStates] = useState<Record<GameId, GameState>>({
    'foosball-1': { teamAName: '', teamBName: '', active: false },
    'darts-1':    { teamAName: '', teamBName: '', active: false },
    'billiards-1':{ teamAName: '', teamBName: '', active: false },
  });
  const [autoplayStates, setAutoplayStates] = useState<Record<string, boolean>>({});
  const [logs, setLogs] = useState<Record<GameId, EventLog[]>>({
    'foosball-1': [],
    'darts-1': [],
    'billiards-1': [],
  });
  const [pocketedBalls, setPocketedBalls] = useState<Record<GameId, Set<number>>>({
    'foosball-1': new Set(),
    'darts-1': new Set(),
    'billiards-1': new Set(),
  });

  useEffect(() => {
    (['foosball-1', 'darts-1', 'billiards-1'] as GameId[]).forEach(id => {
      axios.get(`${API_BASE}/autoplay/${id}`).then(res => {
        setAutoplayStates(prev => ({ ...prev, [id]: res.data }));
      }).catch(() => {});
    });
  }, []);

  const addLog = (gameId: GameId, msg: string, color = 'text-slate-300') => {
    const now = new Date().toLocaleTimeString('it-IT');
    setLogs(prev => ({
      ...prev,
      [gameId]: [{ id: ++logCounter, msg, time: now, color }, ...prev[gameId]].slice(0, 20),
    }));
  };

  const updateName = (gameId: GameId, field: 'teamAName' | 'teamBName', value: string) => {
    setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], [field]: value } }));
  };

  const startMatch = async (gameType: GameType, gameId: GameId) => {
    const { teamAName, teamBName } = gameStates[gameId];
    if (!teamAName.trim() || !teamBName.trim()) {
      alert('Inserisci i nomi di entrambi i giocatori/squadre!');
      return;
    }
    try {
      await axios.post(
        `${API_BASE}/simulators/${gameType}/${LOCALE_ID}/${gameId}/start-match`,
        { teamAName: teamAName.trim(), teamBName: teamBName.trim() }
      );
      setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], active: true } }));
      setPocketedBalls(prev => ({ ...prev, [gameId]: new Set() }));
      addLog(gameId, `🏁 Partita iniziata: ${teamAName} vs ${teamBName}`, 'text-emerald-400');
    } catch {
      alert('Errore avvio partita');
    }
  };

  const triggerEvent = async (
    gameType: GameType,
    gameId: GameId,
    eventType: string,
    payload: Record<string, unknown> = {},
    logMsg?: string
  ) => {
    try {
      await axios.post(
        `${API_BASE}/simulators/${gameType}/${LOCALE_ID}/${gameId}/event?eventType=${eventType}&matchId=manual-match`,
        payload
      );
      addLog(gameId, logMsg || `⚡ ${eventType}`, getEventColor(eventType));
    } catch {
      addLog(gameId, `❌ Errore: ${eventType}`, 'text-red-400');
    }
  };

  const endMatch = async (gameType: GameType, gameId: GameId) => {
    await triggerEvent(gameType, gameId, 'MATCH_END', {}, '🏆 Partita terminata — leaderboard aggiornata!');
    setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], active: false } }));
  };

  const toggleAutoplay = async (gameType: string, gameId: GameId) => {
    const cur = autoplayStates[gameId] || false;
    try {
      await axios.post(`${API_BASE}/autoplay/${gameType}/${LOCALE_ID}/${gameId}?enabled=${!cur}`);
      setAutoplayStates(prev => ({ ...prev, [gameId]: !cur }));
      addLog(gameId, `🤖 Autoplay: ${!cur ? 'ON' : 'OFF'}`, 'text-purple-400');
    } catch { /* ignore */ }
  };

  const getEventColor = (type: string) => {
    if (type === 'GOAL') return 'text-emerald-400';
    if (type === 'BALL_POCKETED') return 'text-amber-400';
    if (type === 'DART_HIT') return 'text-rose-400';
    if (type === 'FOUL') return 'text-red-500';
    if (type === 'MATCH_END') return 'text-yellow-400';
    return 'text-slate-300';
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans pb-12">
      <header className="sticky top-0 z-10 bg-slate-900/90 backdrop-blur border-b border-slate-800 px-8 py-4 flex items-center gap-4">
        <div className="bg-gradient-to-br from-cyan-500 to-blue-600 p-2 rounded-xl">
          <Zap className="w-6 h-6 text-white" />
        </div>
        <div>
          <h1 className="text-2xl font-extrabold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">
            BitPub Simulators Dashboard
          </h1>
          <p className="text-slate-400 text-sm">Pannello di controllo interattivo sensori simulati</p>
        </div>
      </header>

      <main className="px-6 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8 max-w-7xl mx-auto">
        {/* ── CALCIOBALILLA ─────────────────────────────────────────────────── */}
        <GameCard
          title="Calciobalilla"
          icon={<Gamepad2 className="w-6 h-6 text-blue-400" />}
          accentColor="blue"
          gameId="foosball-1"
          gameType="calciobalilla"
          gameState={gameStates['foosball-1']}
          logs={logs['foosball-1']}
          autoplay={autoplayStates['foosball-1']}
          onUpdateName={updateName}
          onStartMatch={startMatch}
          onEndMatch={endMatch}
          onToggleAutoplay={toggleAutoplay}
        >
          {gameStates['foosball-1'].active && (
            <div className="space-y-3">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Segna Goal</p>
              <div className="grid grid-cols-2 gap-3">
                <button
                  id="foosball-goal-red"
                  onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'GOAL', { team: gameStates['foosball-1'].teamAName }, `⚽ Goal di ${gameStates['foosball-1'].teamAName}!`)}
                  className="py-3 px-4 bg-red-600 hover:bg-red-500 active:scale-95 rounded-xl font-bold transition-all text-sm shadow-lg shadow-red-900/30"
                >
                  ⚽ {gameStates['foosball-1'].teamAName || 'RED'}
                </button>
                <button
                  id="foosball-goal-blue"
                  onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'GOAL', { team: gameStates['foosball-1'].teamBName }, `⚽ Goal di ${gameStates['foosball-1'].teamBName}!`)}
                  className="py-3 px-4 bg-blue-600 hover:bg-blue-500 active:scale-95 rounded-xl font-bold transition-all text-sm shadow-lg shadow-blue-900/30"
                >
                  ⚽ {gameStates['foosball-1'].teamBName || 'BLUE'}
                </button>
              </div>
            </div>
          )}
        </GameCard>

        {/* ── FRECCETTE ──────────────────────────────────────────────────────── */}
        <GameCard
          title="Freccette"
          icon={<Target className="w-6 h-6 text-rose-400" />}
          accentColor="rose"
          gameId="darts-1"
          gameType="freccette"
          gameState={gameStates['darts-1']}
          logs={logs['darts-1']}
          autoplay={autoplayStates['darts-1']}
          onUpdateName={updateName}
          onStartMatch={startMatch}
          onEndMatch={endMatch}
          onToggleAutoplay={toggleAutoplay}
        >
          {gameStates['darts-1'].active && (
            <DartBoard
              gameId="darts-1"
              gameType="freccette"
              onTrigger={triggerEvent}
            />
          )}
        </GameCard>

        {/* ── BILIARDO ───────────────────────────────────────────────────────── */}
        <GameCard
          title="Biliardo"
          icon={<CircleDashed className="w-6 h-6 text-amber-400" />}
          accentColor="amber"
          gameId="billiards-1"
          gameType="biliardo"
          gameState={gameStates['billiards-1']}
          logs={logs['billiards-1']}
          autoplay={autoplayStates['billiards-1']}
          onUpdateName={updateName}
          onStartMatch={startMatch}
          onEndMatch={endMatch}
          onToggleAutoplay={toggleAutoplay}
        >
          {gameStates['billiards-1'].active && (
            <BilliardsBoard
              gameId="billiards-1"
              gameType="biliardo"
              pocketedBalls={pocketedBalls['billiards-1']}
              onPocket={(ball) => {
                setPocketedBalls(prev => {
                  const s = new Set(prev['billiards-1']);
                  s.add(ball);
                  return { ...prev, 'billiards-1': s };
                });
                triggerEvent('biliardo', 'billiards-1', 'BALL_POCKETED', { pocketId: 1, ballNumber: ball, ballColor: BILLIARD_BALLS[ball - 1]?.color ?? 'WHITE' }, `🎱 Imbucata palla ${ball}`);
              }}
              onFoul={() => triggerEvent('biliardo', 'billiards-1', 'FOUL', { reason: 'scratch' }, '⛔ Fallo!')}
            />
          )}
        </GameCard>
      </main>
    </div>
  );
}

// ─── GameCard ─────────────────────────────────────────────────────────────────
interface GameCardProps {
  title: string;
  icon: React.ReactNode;
  accentColor: 'blue' | 'rose' | 'amber';
  gameId: GameId;
  gameType: GameType;
  gameState: GameState;
  logs: EventLog[];
  autoplay: boolean;
  onUpdateName: (id: GameId, field: 'teamAName' | 'teamBName', v: string) => void;
  onStartMatch: (gt: GameType, id: GameId) => void;
  onEndMatch: (gt: GameType, id: GameId) => void;
  onToggleAutoplay: (gt: string, id: GameId) => void;
  children?: React.ReactNode;
}

const ACCENT: Record<string, string> = {
  blue:  'border-blue-500/40 hover:border-blue-400',
  rose:  'border-rose-500/40 hover:border-rose-400',
  amber: 'border-amber-500/40 hover:border-amber-400',
};
const ACCENT_BADGE: Record<string, string> = {
  blue:  'bg-blue-500/10 text-blue-300 border-blue-500/30',
  rose:  'bg-rose-500/10 text-rose-300 border-rose-500/30',
  amber: 'bg-amber-500/10 text-amber-300 border-amber-500/30',
};

function GameCard({ title, icon, accentColor, gameId, gameType, gameState, logs, autoplay, onUpdateName, onStartMatch, onEndMatch, onToggleAutoplay, children }: GameCardProps) {
  return (
    <div className={`flex flex-col bg-slate-800/60 backdrop-blur rounded-2xl border ${ACCENT[accentColor]} transition-colors shadow-xl`}>
      {/* Header */}
      <div className="flex items-center gap-3 px-6 py-4 border-b border-slate-700/50">
        <div className="p-2 bg-slate-700 rounded-lg">{icon}</div>
        <h2 className="text-xl font-bold flex-1">{title}</h2>
        {gameState.active && (
          <span className={`text-xs font-semibold px-2 py-1 rounded-full border ${ACCENT_BADGE[accentColor]}`}>
            🔴 LIVE
          </span>
        )}
      </div>

      <div className="flex flex-col gap-4 p-6 flex-1">
        {/* Setup Phase — player names */}
        {!gameState.active && (
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
              <Users className="w-3.5 h-3.5" /> Setup Giocatori
            </div>
            <input
              id={`${gameId}-teamA`}
              type="text"
              placeholder="Giocatore / Squadra A"
              value={gameState.teamAName}
              onChange={e => onUpdateName(gameId, 'teamAName', e.target.value)}
              className="w-full px-3 py-2 rounded-lg bg-slate-700 border border-slate-600 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-400 transition-colors"
            />
            <input
              id={`${gameId}-teamB`}
              type="text"
              placeholder="Giocatore / Squadra B"
              value={gameState.teamBName}
              onChange={e => onUpdateName(gameId, 'teamBName', e.target.value)}
              className="w-full px-3 py-2 rounded-lg bg-slate-700 border border-slate-600 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-400 transition-colors"
            />
            <button
              id={`${gameId}-start`}
              onClick={() => onStartMatch(gameType, gameId)}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 active:scale-95 rounded-xl font-bold transition-all flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/30"
            >
              <Play className="w-4 h-4" /> Inizia Partita
            </button>
          </div>
        )}

        {/* Active match header */}
        {gameState.active && (
          <div className="flex items-center justify-between bg-slate-700/50 rounded-xl px-4 py-3">
            <span className="text-sm font-semibold text-emerald-300">
              {gameState.teamAName} <span className="text-slate-500 mx-1">vs</span> {gameState.teamBName}
            </span>
            <button
              id={`${gameId}-end`}
              onClick={() => onEndMatch(gameType, gameId)}
              className="flex items-center gap-1 text-xs px-3 py-1.5 bg-slate-600 hover:bg-red-700 rounded-lg font-semibold transition-colors"
            >
              <Square className="w-3 h-3" /> Fine
            </button>
          </div>
        )}

        {/* Game-specific action panel */}
        {children}

        {/* Event Log */}
        {logs.length > 0 && (
          <div className="mt-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-2">Log eventi</p>
            <div className="bg-slate-900/50 rounded-xl p-3 max-h-44 overflow-y-auto space-y-1 text-xs">
              {logs.map(l => (
                <div key={l.id} className="flex gap-2">
                  <span className="text-slate-600 shrink-0">{l.time}</span>
                  <span className={l.color}>{l.msg}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Autoplay Toggle */}
        <div className="mt-auto pt-4 border-t border-slate-700/50 flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-medium text-slate-300">
            <RefreshCw className="w-4 h-4" /> Autoplay
          </div>
          <button
            id={`${gameId}-autoplay`}
            onClick={() => onToggleAutoplay(gameType, gameId)}
            className={`px-4 py-1.5 rounded-full font-bold text-sm transition-all ${autoplay ? 'bg-purple-600 text-white shadow-lg shadow-purple-900/40' : 'bg-slate-700 text-slate-400 hover:bg-slate-600'}`}
          >
            {autoplay ? 'ON' : 'OFF'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── DartBoard ────────────────────────────────────────────────────────────────
function DartBoard({ gameId, gameType, onTrigger }: { gameId: GameId; gameType: GameType; onTrigger: (gt: GameType, id: GameId, type: string, payload: Record<string, unknown>, msg: string) => void }) {
  return (
    <div className="space-y-3">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Lancia freccetta</p>

      {/* Bullseye */}
      <button
        id="dart-bullseye"
        onClick={() => onTrigger(gameType, gameId, 'DART_HIT', { score: 50, multiplier: 1 }, '🎯 BULLSEYE! +50')}
        className="w-full py-2 bg-red-700 hover:bg-red-600 rounded-xl font-bold text-sm transition-all active:scale-95"
      >
        🎯 Bullseye (+50)
      </button>

      {/* Sector grid */}
      <div className="grid grid-cols-5 gap-1.5">
        {DART_SECTORS.map(s => (
          <button
            key={s}
            id={`dart-single-${s}`}
            onClick={() => onTrigger(gameType, gameId, 'DART_HIT', { score: s, multiplier: 1 }, `🎯 Settore ${s} (single) +${s}`)}
            className="py-1.5 bg-slate-700 hover:bg-rose-700 rounded-lg text-xs font-bold transition-all active:scale-95"
          >
            {s}
          </button>
        ))}
      </div>

      {/* Multipliers for 20 */}
      <div className="grid grid-cols-2 gap-2 pt-1 border-t border-slate-700">
        <button
          id="dart-double-20"
          onClick={() => onTrigger(gameType, gameId, 'DART_HIT', { score: 20, multiplier: 2 }, '🎯 20 Doppio +40')}
          className="py-2 bg-slate-600 hover:bg-rose-800 rounded-lg text-xs font-bold transition-all"
        >
          20 × Doppio
        </button>
        <button
          id="dart-triple-20"
          onClick={() => onTrigger(gameType, gameId, 'DART_HIT', { score: 20, multiplier: 3 }, '🎯 20 Triplo +60')}
          className="py-2 bg-slate-600 hover:bg-rose-800 rounded-lg text-xs font-bold transition-all"
        >
          20 × Triplo
        </button>
      </div>
    </div>
  );
}

// ─── BilliardsBoard ───────────────────────────────────────────────────────────
function BilliardsBoard({
  pocketedBalls,
  onPocket,
  onFoul,
}: {
  gameId: GameId;
  gameType: GameType;
  pocketedBalls: Set<number>;
  onPocket: (ball: number) => void;
  onFoul: () => void;
}) {
  return (
    <div className="space-y-3">
      <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Imbuca palline</p>

      {/* Ball grid */}
      <div className="grid grid-cols-5 gap-2">
        {BILLIARD_BALLS.map(({ n, color, label }) => {
          const pocketed = pocketedBalls.has(n);
          return (
            <button
              key={n}
              id={`billiards-ball-${n}`}
              disabled={pocketed}
              onClick={() => onPocket(n)}
              className={`aspect-square rounded-full flex items-center justify-center text-xs font-black transition-all active:scale-90 shadow-lg border-2 ${
                pocketed
                  ? 'opacity-20 cursor-not-allowed border-transparent'
                  : 'hover:scale-110 border-white/20'
              }`}
              style={{ backgroundColor: pocketed ? '#1e293b' : color, color: n === 8 ? '#fff' : '#111' }}
              title={`Palla ${n}${pocketed ? ' (imbucata)' : ''}`}
            >
              {label}
            </button>
          );
        })}
      </div>

      {/* Foul button */}
      <button
        id="billiards-foul"
        onClick={onFoul}
        className="w-full py-2 bg-red-900/50 hover:bg-red-800 border border-red-700 rounded-xl text-sm font-bold text-red-300 transition-all active:scale-95"
      >
        ⛔ Fallo (−1 punto)
      </button>
    </div>
  );
}

export default App;
