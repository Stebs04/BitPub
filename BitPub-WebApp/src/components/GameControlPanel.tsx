import React from 'react';
import { Loader2, Gamepad2 } from 'lucide-react';
import type { SensorDefinitionRecord } from '../services/api';

interface Props {
  sensors: SensorDefinitionRecord[]; // sensori del gioco (dal catalogo)
  finished: boolean;
  sending: boolean;
  onAction: (sensorType: string) => void;
}

// Eventi di controllo non giocabili: gestiti dal ciclo di vita del match, non premuti dal player.
const CONTROL_EVENTS = new Set(['MATCH_START', 'MATCH_END']);

/**
 * GameControlPanel — joypad 100% data-driven. Renderizza un pulsante per ogni sensore giocabile
 * definito nel catalogo del gioco (niente pannelli hard-coded per tipo). Alla pressione invia
 * l'azione al backend, che delega l'esito (RNG) al GenericSimulator.
 */
const GameControlPanel: React.FC<Props> = ({ sensors, finished, sending, onAction }) => {
  const locked = finished || sending;
  const playable = (sensors || []).filter((s) => !s.actuator && !CONTROL_EVENTS.has(s.type.toUpperCase()));

  return (
    <div className="rounded-2xl border border-white/10 bg-gradient-to-b from-slate-900 to-slate-950 p-5 shadow-inner">
      <div className={`mb-5 rounded-xl px-4 py-3 text-center font-bold tracking-wide transition-colors ${
        finished
          ? 'bg-yellow-500/10 text-yellow-300 border border-yellow-500/30'
          : 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/40'
      }`}>
        {finished ? '🏁 Partita terminata' : '🎮 Partita in corso'}
      </div>

      {playable.length === 0 ? (
        <p className="text-center text-slate-500 text-sm py-6">
          Nessun evento giocabile configurato per questo gioco.
        </p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {playable.map((sensor) => (
            <button
              key={sensor.id}
              onClick={() => onAction(sensor.type)}
              disabled={locked}
              title={sensor.description}
              className="group flex flex-col items-center gap-1 rounded-2xl bg-gradient-to-br from-blue-600 to-cyan-600 py-6 text-lg font-black text-white shadow-lg shadow-cyan-900/40 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:scale-100"
            >
              {sending ? (
                <Loader2 className="h-6 w-6 animate-spin" />
              ) : (
                <>
                  <Gamepad2 className="h-6 w-6 opacity-90" />
                  <span>{sensor.type}</span>
                </>
              )}
            </button>
          ))}
        </div>
      )}
      <p className="mt-4 text-center text-xs text-slate-500">
        L'esito di ogni azione è deciso dal simulatore in base alla probabilità configurata nel catalogo.
      </p>
    </div>
  );
};

export default GameControlPanel;
