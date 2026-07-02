import React, { useEffect, useRef, useState } from 'react';
import { Activity, Wifi, WifiOff, Trophy, Zap } from 'lucide-react';

// MQTT over WebSocket (port 9001 as defined in mosquitto.conf)
const MQTT_WS_URL = 'ws://localhost:9001';

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
  const [connected, setConnected] = useState(false);
  const [gameStates, setGameStates] = useState<Record<string, GameState>>({});
  const [eventLog, setEventLog] = useState<LogEntry[]>([]);
  const clientRef = useRef<WebSocket | null>(null);
  const logEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Use raw MQTT over WebSocket via the Paho or native WS approach.
    // We use a simple approach: connect to mosquitto WS port.
    let ws: WebSocket;
    let pingInterval: ReturnType<typeof setInterval>;
    let reconnectTimeout: ReturnType<typeof setTimeout>;
    let unmounted = false;

    // Helper: cast Uint8Array to ArrayBuffer so ws.send() is happy under TS strict mode
    const sendPacket = (data: Uint8Array): void => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(data.buffer.slice(data.byteOffset, data.byteOffset + data.byteLength) as ArrayBuffer);
      }
    };

    const connect = () => {
      try {
        // MQTT protocol over WebSocket using sub-protocol
        ws = new WebSocket(MQTT_WS_URL, ['mqtt']);
        clientRef.current = ws;

        ws.binaryType = 'arraybuffer';

        ws.onopen = () => {
          // Send MQTT CONNECT packet
          const connectPacket = buildMqttConnect('kiosk-' + Math.random().toString(36).slice(2, 8));
          sendPacket(connectPacket);
        };

        ws.onmessage = (event) => {
          const data = new Uint8Array(event.data as ArrayBuffer);
          handleMqttPacket(data, ws);
        };

        ws.onclose = () => {
          setConnected(false);
          clearInterval(pingInterval);
          // Reconnect after 3s, unless the component has unmounted
          if (!unmounted) reconnectTimeout = setTimeout(connect, 3000);
        };

        ws.onerror = () => {
          ws.close();
        };
      } catch {
        if (!unmounted) reconnectTimeout = setTimeout(connect, 3000);
      }
    };

    // MQTT packet builder helpers
    const encodeString = (str: string): Uint8Array => {
      const encoded = new TextEncoder().encode(str);
      const buf = new Uint8Array(2 + encoded.length);
      buf[0] = (encoded.length >> 8) & 0xff;
      buf[1] = encoded.length & 0xff;
      buf.set(encoded, 2);
      return buf;
    };

    const buildMqttConnect = (clientId: string): Uint8Array => {
      const protocol = encodeString('MQTT');
      const cid = encodeString(clientId);
      // Fixed header + variable header + payload
      const varHeader = new Uint8Array([
        ...protocol,
        4,    // protocol level (3.1.1)
        0,    // connect flags: clean session
        0, 60 // keep-alive 60s
      ]);
      const payload = cid;
      const remainingLength = varHeader.length + payload.length;
      return new Uint8Array([0x10, remainingLength, ...varHeader, ...payload]);
    };

    const buildMqttSubscribe = (topic: string, packetId: number): Uint8Array => {
      const topicData = encodeString(topic);
      const varHeader = new Uint8Array([packetId >> 8, packetId & 0xff]);
      const payload = new Uint8Array([...topicData, 0]); // QoS 0
      const remainingLength = varHeader.length + payload.length;
      return new Uint8Array([0x82, remainingLength, ...varHeader, ...payload]);
    };

    const buildMqttPingReq = (): Uint8Array => new Uint8Array([0xc0, 0]);

    const handleMqttPacket = (data: Uint8Array, ws: WebSocket) => {
      const type = (data[0] >> 4) & 0xf;
      if (type === 2) {
        // CONNACK — subscribe to game state topic
        setConnected(true);
        sendPacket(buildMqttSubscribe('bitpub/match/+/+/state', 1));
        pingInterval = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) sendPacket(buildMqttPingReq());
        }, 25000);
      } else if (type === 3) {
        // PUBLISH — parse topic and payload
        parseMqttPublish(data);
      }
    };

    const parseMqttPublish = (data: Uint8Array) => {
      try {
        let idx = 1;
        // Decode remaining length
        let remainingLength = 0;
        let multiplier = 1;
        while (true) {
          const byte = data[idx++];
          remainingLength += (byte & 0x7f) * multiplier;
          multiplier *= 128;
          if ((byte & 0x80) === 0) break;
        }
        // Topic length
        const topicLen = (data[idx] << 8) | data[idx + 1];
        idx += 2;
        const topicBytes = data.slice(idx, idx + topicLen);
        const topic = new TextDecoder().decode(topicBytes);
        idx += topicLen;
        // Payload
        const payloadBytes = data.slice(idx);
        const payloadStr = new TextDecoder().decode(payloadBytes);

        const gameState: GameState = JSON.parse(payloadStr);
        const instanceId = topic.split('/')[3] ?? topic;

        setGameStates(prev => ({ ...prev, [instanceId]: gameState }));

        // Add to event log
        const entry: LogEntry = {
          id: ++logId,
          time: new Date().toLocaleTimeString('it-IT'),
          gameTypeId: gameState.gameTypeId ?? instanceId,
          message: getEventLabel(gameState.currentEventMessage, gameState),
          color: getEventColor(gameState.currentEventMessage),
        };
        setEventLog(prev => [entry, ...prev].slice(0, 50));
      } catch {
        // Ignore malformed packets
      }
    };

    connect();

    return () => {
      unmounted = true;
      clearInterval(pingInterval);
      clearTimeout(reconnectTimeout);
      if (clientRef.current) clientRef.current.close();
    };
  }, []);

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
          <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${isFinished ? 'bg-yellow-500/20 text-yellow-300 border border-yellow-500/30' : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'}`}>
            {isFinished ? '✔ Finita' : '🔴 LIVE'}
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
