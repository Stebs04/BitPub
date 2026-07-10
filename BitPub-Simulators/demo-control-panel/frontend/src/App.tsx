/**
 * autore Timothy Giolito 20054431
 *
 * Componente radice dell'applicazione React.
 * Gestisce lo stato globale dei simulatori, la comunicazione con le API di backend
 * e l'interfaccia utente principale per il controllo delle partite e dell'autoplay.
 */
import { useState, useEffect } from 'react';
import axios from 'axios';
import { Gamepad2, Users, Play, Square, RefreshCw, Zap } from 'lucide-react';

const API_BASE = '/api';
const LOCALE_ID = 'LOCALE_001';

// ─── Types (mirror GenericSimulator.GameConfig from the demo backend) ───────────
interface SensorRule {
  type: string;
  actuator: boolean;
  scoreIncrement: number;
  successProbability: number;
}
interface GameConfig {
  id: string;      // gameTypeId
  name: string;
  winScoreTarget: number;
  sensors: SensorRule[];
}

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

// ponytail: 1 istanza per tipo di gioco, id per convenzione "{gameTypeId}-1".
// La macchina corrispondente deve esistere nel locale; se servono piu' istanze,
// leggerle da /api/v1/locales/online (richiede proxy gateway lato backend demo).
const instanceId = (gameTypeId: string) => `${gameTypeId}-1`;

// Sensori giocabili: eventi di punteggio, non attuatori e non i controlli di partita.
const scoringSensors = (g: GameConfig) =>
  (g.sensors ?? []).filter(
    s => !s.actuator && s.type !== 'MATCH_START' && s.type !== 'MATCH_END'
  );

let logCounter = 0;

function App() {
  const [games, setGames] = useState<GameConfig[]>([]);
  const [gameStates, setGameStates] = useState<Record<string, GameState>>({});
  const [autoplayStates, setAutoplayStates] = useState<Record<string, boolean>>({});
  const [logs, setLogs] = useState<Record<string, EventLog[]>>({});

  const loadGames = async () => {
    try {
      const res = await axios.get<GameConfig[]>(`${API_BASE}/games`);
      setGames(res.data);
      setGameStates(prev => {
        const next = { ...prev };
        res.data.forEach(g => {
          if (!next[g.id]) next[g.id] = { teamAName: '', teamBName: '', active: false };
        });
        return next;
      });
      res.data.forEach(g => {
        axios.get(`${API_BASE}/autoplay/${instanceId(g.id)}`)
          .then(r => setAutoplayStates(prev => ({ ...prev, [g.id]: r.data })))
          .catch(() => {});
      });
    } catch {
      setGames([]);
    }
  };

  useEffect(() => { loadGames(); }, []);

  const addLog = (gameId: string, msg: string, color = 'text-slate-300') => {
    const now = new Date().toLocaleTimeString('it-IT');
    setLogs(prev => ({
      ...prev,
      [gameId]: [{ id: ++logCounter, msg, time: now, color }, ...(prev[gameId] ?? [])].slice(0, 20),
    }));
  };

  const updateName = (gameId: string, field: 'teamAName' | 'teamBName', value: string) => {
    setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], [field]: value } }));
  };

  const startMatch = async (gameId: string) => {
    const { teamAName, teamBName } = gameStates[gameId];
    if (!teamAName.trim() || !teamBName.trim()) {
      alert('Inserisci i nomi di entrambi i giocatori/squadre!');
      return;
    }
    try {
      await axios.post(
        `${API_BASE}/simulators/${gameId}/${LOCALE_ID}/${instanceId(gameId)}/start-match`,
        { teamAName: teamAName.trim(), teamBName: teamBName.trim() }
      );
      setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], active: true } }));
      addLog(gameId, `🏁 Partita iniziata: ${teamAName} vs ${teamBName}`, 'text-emerald-400');
    } catch {
      alert('Errore avvio partita');
    }
  };

  const triggerEvent = async (
    gameId: string, eventType: string, payload: Record<string, unknown>, logMsg: string
  ) => {
    try {
      await axios.post(
        `${API_BASE}/simulators/${gameId}/${LOCALE_ID}/${instanceId(gameId)}/event?eventType=${eventType}&matchId=manual-match`,
        payload
      );
      addLog(gameId, logMsg, 'text-amber-400');
    } catch {
      addLog(gameId, `❌ Errore: ${eventType}`, 'text-red-400');
    }
  };

  const scoreSensor = (game: GameConfig, sensor: SensorRule, team: string) =>
    triggerEvent(
      game.id, sensor.type,
      { team, scoreIncrement: sensor.scoreIncrement, winScoreTarget: game.winScoreTarget },
      `⚡ ${sensor.type} (+${sensor.scoreIncrement}) → ${team}`
    );

  const endMatch = async (gameId: string) => {
    await triggerEvent(gameId, 'MATCH_END', {}, '🏆 Partita terminata — leaderboard aggiornata!');
    setGameStates(prev => ({ ...prev, [gameId]: { ...prev[gameId], active: false } }));
  };

  const toggleAutoplay = async (gameId: string) => {
    const cur = autoplayStates[gameId] || false;
    try {
      await axios.post(`${API_BASE}/autoplay/${gameId}/${LOCALE_ID}/${instanceId(gameId)}?enabled=${!cur}`);
      setAutoplayStates(prev => ({ ...prev, [gameId]: !cur }));
      addLog(gameId, `🤖 Autoplay: ${!cur ? 'ON' : 'OFF'}`, 'text-purple-400');
    } catch { /* ignore */ }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 font-sans pb-12">
      <header className="sticky top-0 z-10 bg-slate-900/90 backdrop-blur border-b border-slate-800 px-8 py-4 flex items-center gap-4">
        <div className="bg-gradient-to-br from-cyan-500 to-blue-600 p-2 rounded-xl">
          <Zap className="w-6 h-6 text-white" />
        </div>
        <div className="flex-1">
          <h1 className="text-2xl font-extrabold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent">
            BitPub Simulators Dashboard
          </h1>
          <p className="text-slate-400 text-sm">Joypad generati dal catalogo giochi (data-driven)</p>
        </div>
        <button
          onClick={loadGames}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-sm font-medium text-slate-300 transition-all"
        >
          <RefreshCw className="w-4 h-4" /> Ricarica giochi
        </button>
      </header>

      {games.length === 0 ? (
        <div className="max-w-md mx-auto mt-24 text-center text-slate-400">
          <Gamepad2 className="w-12 h-12 mx-auto mb-4 text-slate-600" />
          <p className="text-lg font-semibold">Nessun gioco disponibile</p>
          <p className="text-sm mt-2">
            Il catalogo non ha ancora pubblicato configurazioni (bitpub/config/games/#).
            Avvia lo stack e riprova.
          </p>
        </div>
      ) : (
        <main className="px-6 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8 max-w-7xl mx-auto">
          {games.map(game => (
            <GameCard
              key={game.id}
              game={game}
              gameState={gameStates[game.id] ?? { teamAName: '', teamBName: '', active: false }}
              logs={logs[game.id] ?? []}
              autoplay={!!autoplayStates[game.id]}
              onUpdateName={updateName}
              onStartMatch={startMatch}
              onEndMatch={endMatch}
              onToggleAutoplay={toggleAutoplay}
              onScore={scoreSensor}
            />
          ))}
        </main>
      )}
    </div>
  );
}

// ─── GameCard ───────────────────────────────────────────────────────────────────
interface GameCardProps {
  game: GameConfig;
  gameState: GameState;
  logs: EventLog[];
  autoplay: boolean;
  onUpdateName: (id: string, field: 'teamAName' | 'teamBName', v: string) => void;
  onStartMatch: (id: string) => void;
  onEndMatch: (id: string) => void;
  onToggleAutoplay: (id: string) => void;
  onScore: (game: GameConfig, sensor: SensorRule, team: string) => void;
}

function GameCard({ game, gameState, logs, autoplay, onUpdateName, onStartMatch, onEndMatch, onToggleAutoplay, onScore }: GameCardProps) {
  const gameId = game.id;
  const sensors = scoringSensors(game);
  const teamA = gameState.teamAName || 'A';
  const teamB = gameState.teamBName || 'B';

  return (
    <div className="flex flex-col bg-slate-800/60 backdrop-blur rounded-2xl border border-blue-500/40 hover:border-blue-400 transition-colors shadow-xl">
      <div className="flex items-center gap-3 px-6 py-4 border-b border-slate-700/50">
        <div className="p-2 bg-slate-700 rounded-lg"><Gamepad2 className="w-6 h-6 text-blue-400" /></div>
        <div className="flex-1">
          <h2 className="text-xl font-bold">{game.name || gameId}</h2>
          <p className="text-xs text-slate-500">target {game.winScoreTarget} pt · {sensors.length} sensori</p>
        </div>
        {gameState.active && (
          <span className="text-xs font-semibold px-2 py-1 rounded-full border bg-blue-500/10 text-blue-300 border-blue-500/30">🔴 LIVE</span>
        )}
      </div>

      <div className="flex flex-col gap-4 p-6 flex-1">
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
              onClick={() => onStartMatch(gameId)}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 active:scale-95 rounded-xl font-bold transition-all flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/30"
            >
              <Play className="w-4 h-4" /> Inizia Partita
            </button>
          </div>
        )}

        {gameState.active && (
          <>
            <div className="flex items-center justify-between bg-slate-700/50 rounded-xl px-4 py-3">
              <span className="text-sm font-semibold text-emerald-300">
                {teamA} <span className="text-slate-500 mx-1">vs</span> {teamB}
              </span>
              <button
                id={`${gameId}-end`}
                onClick={() => onEndMatch(gameId)}
                className="flex items-center gap-1 text-xs px-3 py-1.5 bg-slate-600 hover:bg-red-700 rounded-lg font-semibold transition-colors"
              >
                <Square className="w-3 h-3" /> Fine
              </button>
            </div>

            {/* Joypad 100% data-driven: per ogni sensore di punteggio, un pulsante per squadra */}
            <div className="space-y-3">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Eventi di gioco</p>
              {sensors.length === 0 && (
                <p className="text-xs text-slate-500">Nessun sensore di punteggio configurato per questo gioco.</p>
              )}
              {sensors.map(sensor => (
                <div key={sensor.type} className="space-y-1.5">
                  <p className="text-[11px] text-slate-500 font-medium">{sensor.type} <span className="text-slate-600">(+{sensor.scoreIncrement})</span></p>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      id={`${gameId}-${sensor.type}-A`}
                      onClick={() => onScore(game, sensor, teamA)}
                      className="py-2 px-3 bg-red-600 hover:bg-red-500 active:scale-95 rounded-lg font-bold text-sm transition-all"
                    >
                      {teamA}
                    </button>
                    <button
                      id={`${gameId}-${sensor.type}-B`}
                      onClick={() => onScore(game, sensor, teamB)}
                      className="py-2 px-3 bg-blue-600 hover:bg-blue-500 active:scale-95 rounded-lg font-bold text-sm transition-all"
                    >
                      {teamB}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

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

        <div className="mt-auto pt-4 border-t border-slate-700/50 flex items-center justify-between">
          <div className="flex items-center gap-2 text-sm font-medium text-slate-300">
            <RefreshCw className="w-4 h-4" /> Autoplay
          </div>
          <button
            id={`${gameId}-autoplay`}
            onClick={() => onToggleAutoplay(gameId)}
            className={`px-4 py-1.5 rounded-full font-bold text-sm transition-all ${autoplay ? 'bg-purple-600 text-white shadow-lg shadow-purple-900/40' : 'bg-slate-700 text-slate-400 hover:bg-slate-600'}`}
          >
            {autoplay ? 'ON' : 'OFF'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default App;
