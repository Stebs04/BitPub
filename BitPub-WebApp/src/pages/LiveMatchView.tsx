import React, { useEffect, useRef, useState } from 'react';
import { Activity, Wifi, WifiOff, Trophy, Zap } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import api from '../services/api';
import { notificationService } from '../services/notificationService';

interface GameState {
  matchId: string;
  gameTypeId: string;
  status: string;
  teamAName: string;
  teamBName: string;
  scoreTeamA: number;
  scoreTeamB: number;
  currentEventMessage: string;
  winnerName?: string;
}

interface LogEntry {
  id: number;
  time: string;
  gameTypeId: string;
  message: string;
  color: string;
}

let logId = 0;

const GAME_COLORS: Record<string, string> = {
  foosball: 'from-blue-600 to-cyan-600',
  calciobalilla: 'from-blue-600 to-cyan-600',
  freccette: 'from-rose-600 to-pink-600',
  darts: 'from-rose-600 to-pink-600',
  biliardo: 'from-amber-600 to-yellow-600',
  billiards: 'from-amber-600 to-yellow-600',
  unknown: 'from-slate-600 to-slate-500',
};

const getGameGradient = (gameTypeId: string = '') => {
  const key = Object.keys(GAME_COLORS).find(k => gameTypeId.toLowerCase().includes(k));
  return key ? GAME_COLORS[key] : GAME_COLORS.unknown;
};

const getEventLabel = (type: string, state: GameState): string => {
  switch (type) {
    case 'MATCH_START': return `🏁 Partita iniziata: ${state.teamAName} vs ${state.teamBName}`;
    case 'MATCH_END':   return `🏆 Fine partita! Vince ${state.winnerName ?? '—'}`;
    case 'GOAL':        return `⚽ GOAL!`;
    case 'BALL_POCKETED': return `🎱 Palla imbucata`;
    case 'DART_HIT':    return `🎯 Freccia lanciata`;
    case 'FOUL':        return `⛔ Fallo!`;
    default:            return `⚡ ${type}`;
  }
};

const getEventColor = (type: string): string => {
  if (type === 'GOAL')          return 'text-emerald-400';
  if (type === 'BALL_POCKETED') return 'text-amber-400';
  if (type === 'DART_HIT')      return 'text-rose-400';
  if (type === 'FOUL')          return 'text-red-500';
  if (type === 'MATCH_END')     return 'text-yellow-300 font-bold';
  if (type === 'MATCH_START')   return 'text-emerald-300 font-bold';
  return 'text-slate-400';
};

/**
 * LiveMatchView — connects to MQTT broker over WebSocket and displays
 * real-time game state: scores, player names, event log.
 */
const LiveMatchView: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  // Un LOCALE_ADMIN monitora solo le partite del proprio locale: si sottoscrive
  // esclusivamente al topic MQTT del proprio localeId invece che al wildcard globale.
  // Il localeId puo' arrivare dal claim JWT (token aggiornato) o, se l'admin non ha
  // ancora rifatto login dopo l'assegnazione del locale, via fallback REST (stesso
  // endpoint /locales/by-admin usato da Dashboard e LocalesPage).
  const [ownLocaleId, setOwnLocaleId] = useState<string | null>(user?.localeId ?? null);

  useEffect(() => {
    if (user?.role !== 'LOCALE_ADMIN') {
      setOwnLocaleId(null);
      return;
    }
    if (user.localeId) {
      setOwnLocaleId(user.localeId);
      return;
    }
    api.get(`/locales/by-admin/${user.id}`)
      .then((res) => setOwnLocaleId(res.data?.[0]?.id ?? null))
      .catch(() => setOwnLocaleId(null));
  }, [user]);

  // null = nessuna sottoscrizione: un LOCALE_ADMIN senza locale (ancora) assegnato non deve
  // MAI ricadere sul wildcard globale, altrimenti vedrebbe le partite di tutti i locali.
  const matchStateTopic = user?.role === 'LOCALE_ADMIN'
    ? (ownLocaleId ? `bitpub/match/${ownLocaleId}/+/state` : null)
    : 'bitpub/match/+/+/state';

  const [connected, setConnected] = useState(false);
  const [gameStates, setGameStates] = useState<Record<string, GameState>>({});
  const [eventLog, setEventLog] = useState<LogEntry[]>([]);
  const logEndRef = useRef<HTMLDivElement>(null);

  // Sottoscrizione al broker MQTT (condivisa via notificationService) per lo stato partite in tempo reale.
  useEffect(() => {
    // LOCALE_ADMIN senza locale assegnato: nessun topic sicuro da sottoscrivere, non connettere.
    if (matchStateTopic === null) {
      setConnected(false);
      return;
    }

    const unsubscribeConnection = notificationService.onConnectionChange(setConnected);
    const unsubscribeTopic = notificationService.subscribe(matchStateTopic, (payload: GameState, topic: string) => {
      const instanceId = topic.split('/')[3] ?? topic;
      setGameStates(prev => ({ ...prev, [instanceId]: payload }));

      const entry: LogEntry = {
        id: ++logId,
        time: new Date().toLocaleTimeString('it-IT'),
        gameTypeId: payload.gameTypeId ?? instanceId,
        message: getEventLabel(payload.currentEventMessage, payload),
        color: getEventColor(payload.currentEventMessage),
      };
      setEventLog(prev => [entry, ...prev].slice(0, 50));
    });

    return () => {
      unsubscribeConnection();
      unsubscribeTopic();
    };
  }, [matchStateTopic]);

  // Auto-scroll log
  useEffect(() => {
    logEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [eventLog]);

  const activeGames = Object.entries(gameStates);

  return (
    <div className="p-6 md:p-8 animate-slide-up space-y-8">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="bg-gradient-to-br from-cyan-500 to-blue-600 p-2.5 rounded-xl">
            <Activity className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white">Live Match</h1>
            <p className="text-slate-400 text-sm">Partite in tempo reale via MQTT</p>
          </div>
        </div>
        <div className={`flex items-center gap-2 px-4 py-2 rounded-full border text-sm font-semibold ${connected ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-400' : 'border-red-500/40 bg-red-500/10 text-red-400'}`}>
          {connected ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
          {connected ? 'Connesso MQTT' : 'Disconnesso'}
        </div>
      </div>

      {/* Active game cards */}
      {activeGames.length === 0 ? (
        <div className="glass-panel p-12 text-center">
          <Zap className="w-12 h-12 text-slate-600 mx-auto mb-4" />
          <p className="text-slate-400 text-lg">Nessuna partita attiva.</p>
          <p className="text-slate-500 text-sm mt-2">Avvia una partita dal pannello simulatori per vederla qui in tempo reale.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
          {activeGames.map(([instanceId, state]) => (
            <ScoreCard key={instanceId} instanceId={instanceId} state={state} />
          ))}
        </div>
      )}

      {/* Real-time event log */}
      <div className="glass-panel">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <Zap className="w-5 h-5 text-brand-light" />
          <h2 className="text-lg font-semibold text-white">Log eventi in tempo reale</h2>
          <span className="ml-auto text-xs text-slate-500">{eventLog.length} eventi</span>
        </div>
        <div className="p-4 max-h-72 overflow-y-auto space-y-1.5 font-mono text-xs">
          {eventLog.length === 0 && (
            <p className="text-slate-500 text-center py-4">In attesa di eventi...</p>
          )}
          {eventLog.map(log => (
            <div key={log.id} className="flex gap-3 items-start">
              <span className="text-slate-600 shrink-0 w-16">{log.time}</span>
              <span className="text-slate-500 shrink-0 uppercase text-[10px] font-bold w-20 truncate pt-px">[{log.gameTypeId}]</span>
              <span className={log.color}>{log.message}</span>
            </div>
          ))}
          <div ref={logEndRef} />
        </div>
      </div>
    </div>
  );
};

// ─── ScoreCard ────────────────────────────────────────────────────────────────
function ScoreCard({ instanceId, state }: { instanceId: string; state: GameState }) {
  const gradient = getGameGradient(state.gameTypeId ?? instanceId);
  const isFinished = state.status === 'FINISHED';
  const isWaiting = state.status === 'WAITING';

  return (
    <div className={`glass-panel overflow-hidden`}>
      {/* Gradient bar */}
      <div className={`h-1.5 bg-gradient-to-r ${gradient}`} />

      <div className="p-6 space-y-4">
        {/* Title */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-wider font-semibold">{state.gameTypeId ?? instanceId}</p>
            <p className="text-sm text-slate-400 font-mono">{instanceId}</p>
          </div>
          <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${
            isFinished ? 'bg-yellow-500/20 text-yellow-300 border border-yellow-500/30'
            : isWaiting ? 'bg-slate-500/20 text-slate-300 border border-slate-500/30'
            : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
          }`}>
            {isFinished ? '✔ Finita' : isWaiting ? '⏳ In attesa' : '🔴 LIVE'}
          </span>
        </div>

        {/* Score */}
        <div className="flex items-center justify-between gap-4">
          <div className="flex-1 text-center">
            <p className="text-sm font-semibold text-slate-300 mb-1 truncate">{state.teamAName || 'Team A'}</p>
            <p className={`text-5xl font-black ${isFinished && state.winnerName === state.teamAName ? 'text-yellow-400' : 'text-white'}`}>
              {state.scoreTeamA}
            </p>
          </div>
          <div className="text-2xl font-black text-slate-600">:</div>
          <div className="flex-1 text-center">
            <p className="text-sm font-semibold text-slate-300 mb-1 truncate">{state.teamBName || 'Team B'}</p>
            <p className={`text-5xl font-black ${isFinished && state.winnerName === state.teamBName ? 'text-yellow-400' : 'text-white'}`}>
              {state.scoreTeamB}
            </p>
          </div>
        </div>

        {/* Last event */}
        <div className="bg-slate-800/50 rounded-lg px-3 py-2 text-xs text-slate-400">
          <span className="text-slate-500">Ultimo evento: </span>
          <span className={getEventColor(state.currentEventMessage)}>{state.currentEventMessage}</span>
        </div>

        {/* Winner banner */}
        {isFinished && state.winnerName && (
          <div className="flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/30 rounded-xl px-4 py-3">
            <Trophy className="w-5 h-5 text-yellow-400 shrink-0" />
            <p className="text-yellow-300 font-bold text-sm">Vince: {state.winnerName}</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default LiveMatchView;
