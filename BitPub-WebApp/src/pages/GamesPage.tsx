import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Gamepad2, Server, Play, Loader2, Users, Trophy, ArrowLeft, RefreshCw } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import { useAuthStore } from '../store/authStore';
import { getOnlineLocales, joinMatchLobby, getMatch } from '../services/api';
import type { LocaleRecord, GameInstanceRecord, MatchRecord } from '../services/api';
import { notificationService } from '../services/notificationService';

// Stato di gioco pubblicato da match-service su MQTT (bitpub/match/{localeId}/{gameInstanceId}/state).
interface GameState {
  matchId: string;
  gameTypeId: string;
  status: string; // WAITING | PLAYING | FINISHED
  teamAName: string;
  teamBName: string;
  scoreTeamA: number;
  scoreTeamB: number;
  currentEventMessage: string;
  winnerName?: string;
}

const eventLabel = (type: string, s: GameState): string => {
  switch (type) {
    case 'MATCH_START': return `🏁 Partita iniziata: ${s.teamAName} vs ${s.teamBName}`;
    case 'MATCH_END':   return `🏆 Fine partita! Vince ${s.winnerName ?? '—'}`;
    case 'GOAL':        return '⚽ GOAL!';
    case 'BALL_POCKETED': return '🎱 Palla imbucata';
    case 'DART_HIT':    return '🎯 Freccia lanciata';
    case 'FOUL':        return '⛔ Fallo!';
    case 'WAITING_FOR_PLAYERS': return '⏳ In attesa di giocatori';
    default:            return `⚡ ${type}`;
  }
};

/**
 * GamesPage — scheda "Gioca" del PLAYER. Esplora i locali ONLINE in tempo reale, sceglie un
 * gioco attivo ed entra in lobby (WAITING_FOR_PLAYERS). L'intero flusso lobby -> partita live
 * avviene qui in-page tramite lo stream MQTT condiviso (notificationService), senza passare
 * dalla vista Live Match riservata al monitoraggio degli amministratori.
 */
const GamesPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  const [locales, setLocales] = useState<LocaleRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [joiningGameId, setJoiningGameId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Partita corrente del giocatore (dalla creazione lobby in poi) e relativo stato live via MQTT.
  const [activeMatch, setActiveMatch] = useState<MatchRecord | null>(null);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchLocales = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getOnlineLocales();
      setLocales(res.data || []);
    } catch (err) {
      console.error('Error fetching online locales:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLocales();
  }, [fetchLocales]);

  // Sottoscrizione MQTT allo stato della partita corrente: aggiorna punteggi e fa scattare
  // la transizione dalla sala d'attesa alla partita live appena il secondo giocatore entra.
  useEffect(() => {
    if (!activeMatch || !activeMatch.localeId) return;
    const topic = `bitpub/match/${activeMatch.localeId}/${activeMatch.gameInstanceId}/state`;
    const unsubscribe = notificationService.subscribe(topic, (payload: GameState) => {
      setGameState(payload);
      if (payload.status !== 'WAITING') {
        setActiveMatch((prev) => (prev && prev.status === 'WAITING_FOR_PLAYERS' ? { ...prev, status: 'IN_PROGRESS' } : prev));
      }
    });
    return unsubscribe;
  }, [activeMatch]);

  // Fallback: finche' la lobby resta in attesa, ripolla la partita nel caso il messaggio MQTT
  // di transizione vada perso (il broker e' QoS 0).
  useEffect(() => {
    if (!activeMatch || activeMatch.status !== 'WAITING_FOR_PLAYERS') {
      if (pollRef.current) clearInterval(pollRef.current);
      return;
    }
    const poll = setInterval(async () => {
      try {
        const res = await getMatch(activeMatch.id);
        if (res.data.status !== 'WAITING_FOR_PLAYERS') {
          setActiveMatch(res.data);
        }
      } catch {
        // partita non piu' disponibile: interrompi il polling
        if (pollRef.current) clearInterval(pollRef.current);
      }
    }, 4000);
    pollRef.current = poll;
    return () => clearInterval(poll);
  }, [activeMatch]);

  const handleJoin = async (instance: GameInstanceRecord) => {
    if (!user) return;
    setError(null);
    setJoiningGameId(instance.id);
    try {
      const res = await joinMatchLobby(instance.id, user.username);
      setActiveMatch(res.data);
      setGameState(null);
    } catch (err: any) {
      setError(err?.response?.data?.message || "Impossibile entrare in partita: il gioco potrebbe non essere piu' attivo.");
    } finally {
      setJoiningGameId(null);
    }
  };

  const leaveMatch = () => {
    setActiveMatch(null);
    setGameState(null);
    fetchLocales();
  };

  // ── Vista partita/lobby ─────────────────────────────────────────────────────
  if (activeMatch) {
    const isWaiting = activeMatch.status === 'WAITING_FOR_PLAYERS' && (!gameState || gameState.status === 'WAITING');
    const teamA = gameState?.teamAName || activeMatch.teams?.[0]?.name || 'Tu';
    const teamB = gameState?.teamBName || activeMatch.teams?.[1]?.name || 'Avversario';
    const isFinished = gameState?.status === 'FINISHED';

    return (
      <div className="p-6 md:p-8 animate-slide-up space-y-6">
        <button onClick={leaveMatch} className="flex items-center gap-2 text-slate-400 hover:text-white text-sm font-medium">
          <ArrowLeft className="w-4 h-4" /> Torna ai giochi
        </button>

        {isWaiting ? (
          <div className="glass-panel p-12 text-center border border-brand/30 bg-brand/5">
            <Loader2 className="w-12 h-12 text-brand-light mx-auto mb-4 animate-spin" />
            <h2 className="text-2xl font-bold text-white mb-2">In attesa di un secondo giocatore...</h2>
            <p className="text-slate-400 flex items-center justify-center gap-2">
              <Users className="w-4 h-4" /> {activeMatch.gameTypeId} — {teamA} è pronto
            </p>
            <p className="text-slate-500 text-sm mt-4">La partita inizierà automaticamente al collegamento dell'avversario.</p>
          </div>
        ) : (
          <div className="glass-panel overflow-hidden">
            <div className="h-1.5 bg-gradient-to-r from-brand to-brand-light" />
            <div className="p-8 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs text-slate-500 uppercase tracking-wider font-semibold">{gameState?.gameTypeId ?? activeMatch.gameTypeId}</p>
                  <p className="text-sm text-slate-400 font-mono">{activeMatch.gameInstanceId}</p>
                </div>
                <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${isFinished ? 'bg-yellow-500/20 text-yellow-300 border border-yellow-500/30' : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'}`}>
                  {isFinished ? '✔ Finita' : '🔴 LIVE'}
                </span>
              </div>

              <div className="flex items-center justify-between gap-4">
                <div className="flex-1 text-center">
                  <p className="text-base font-semibold text-slate-300 mb-1 truncate">{teamA}</p>
                  <p className={`text-6xl font-black ${isFinished && gameState?.winnerName === teamA ? 'text-yellow-400' : 'text-white'}`}>{gameState?.scoreTeamA ?? 0}</p>
                </div>
                <div className="text-3xl font-black text-slate-600">:</div>
                <div className="flex-1 text-center">
                  <p className="text-base font-semibold text-slate-300 mb-1 truncate">{teamB}</p>
                  <p className={`text-6xl font-black ${isFinished && gameState?.winnerName === teamB ? 'text-yellow-400' : 'text-white'}`}>{gameState?.scoreTeamB ?? 0}</p>
                </div>
              </div>

              {gameState?.currentEventMessage && (
                <div className="bg-slate-800/50 rounded-lg px-4 py-3 text-sm text-slate-300 text-center">
                  {eventLabel(gameState.currentEventMessage, gameState)}
                </div>
              )}

              {isFinished && gameState?.winnerName && (
                <div className="flex items-center justify-center gap-2 bg-yellow-500/10 border border-yellow-500/30 rounded-xl px-4 py-3">
                  <Trophy className="w-5 h-5 text-yellow-400 shrink-0" />
                  <p className="text-yellow-300 font-bold">Vince: {gameState.winnerName}</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    );
  }

  // ── Vista esplorazione locali/giochi ────────────────────────────────────────
  return (
    <div className="p-6 md:p-8 animate-slide-up">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="bg-gradient-to-br from-brand to-brand-light p-2.5 rounded-xl">
            <Gamepad2 className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-white">Gioca</h1>
            <p className="text-slate-400 text-sm">Scegli un gioco in un locale online ed entra in partita</p>
          </div>
        </div>
        <button
          onClick={fetchLocales}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-sm font-medium text-slate-300 transition-all disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Aggiorna
        </button>
      </div>

      {error && (
        <div className="mb-6 glass-panel border border-red-500/30 bg-red-500/10 px-4 py-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-white">Caricamento locali online...</div>
      ) : locales.length === 0 ? (
        <div className="glass-panel p-12 text-center">
          <p className="text-slate-400 text-lg">Nessun locale online al momento. Riprova più tardi.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {locales.map((locale) => {
            const activeGames = (locale.games || []).filter((g) => g.active);
            return (
              <Card key={locale.id}>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle>{locale.name}</CardTitle>
                    <span className="px-3 py-1 bg-emerald-500/20 text-emerald-400 rounded-full text-xs font-semibold">ONLINE</span>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-slate-400 mb-4">{locale.address}</p>
                  <div className="space-y-3">
                    {activeGames.length === 0 && (
                      <p className="text-sm text-slate-500 px-1">Nessun gioco attivo in questo locale.</p>
                    )}
                    {activeGames.map((game) => (
                      <div key={game.id} className="flex items-center justify-between gap-3 text-slate-300 bg-white/5 p-3 rounded-lg">
                        <div className="flex items-center gap-3">
                          <Server className="w-5 h-5 text-brand" />
                          <span>{game.localInstanceId} <span className="text-slate-500">({game.gameTypeId})</span></span>
                        </div>
                        <Button size="sm" onClick={() => handleJoin(game)} disabled={joiningGameId === game.id}>
                          {joiningGameId === game.id ? <Loader2 className="w-4 h-4 animate-spin" /> : (<><Play className="w-4 h-4 mr-1.5" /> Gioca</>)}
                        </Button>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default GamesPage;
