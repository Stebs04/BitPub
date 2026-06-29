import { useState, useEffect } from 'react';
import axios from 'axios';
import { Gamepad2, Target, CircleDashed } from 'lucide-react';

const API_BASE = '/api';

function App() {
  const [autoplayStates, setAutoplayStates] = useState<Record<string, boolean>>({});

  const localeId = 'LOCALE_001';

  useEffect(() => {
    // Fetch initial autoplay states
    ['foosball-1', 'darts-1', 'billiards-1'].forEach(id => {
      axios.get(`${API_BASE}/autoplay/${id}`).then(res => {
        setAutoplayStates(prev => ({ ...prev, [id]: res.data }));
      }).catch(err => console.error(err));
    });
  }, []);

  const toggleAutoplay = async (gameType: string, gameInstanceId: string) => {
    const currentState = autoplayStates[gameInstanceId] || false;
    const newState = !currentState;
    try {
      await axios.post(`${API_BASE}/autoplay/${gameType}/${localeId}/${gameInstanceId}?enabled=${newState}`);
      setAutoplayStates(prev => ({ ...prev, [gameInstanceId]: newState }));
    } catch (error) {
      console.error('Failed to toggle autoplay', error);
    }
  };

  const triggerEvent = async (gameType: string, gameInstanceId: string, eventType: string, payload: any = {}) => {
    try {
      await axios.post(`${API_BASE}/simulators/${gameType}/${localeId}/${gameInstanceId}/event?eventType=${eventType}&matchId=manual-match`, payload);
      alert(`Event ${eventType} triggered successfully!`);
    } catch (error) {
      console.error('Failed to trigger event', error);
      alert('Failed to trigger event');
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-8 font-sans">
      <header className="mb-12 text-center">
        <h1 className="text-5xl font-extrabold bg-gradient-to-r from-cyan-400 to-blue-600 bg-clip-text text-transparent mb-4">
          BitPub Simulators Dashboard
        </h1>
        <p className="text-slate-400 text-lg">Interactive hardware simulation panel</p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-7xl mx-auto">
        {/* Foosball Card */}
        <div className="bg-slate-800 rounded-2xl p-6 shadow-xl border border-slate-700 hover:border-blue-500 transition-colors">
          <div className="flex items-center mb-6">
            <Gamepad2 className="w-8 h-8 text-blue-400 mr-3" />
            <h2 className="text-2xl font-bold">Calciobalilla</h2>
          </div>
          <div className="space-y-4">
            <button onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'MATCH_START')} className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 rounded-lg font-semibold transition-colors">
              Start Match
            </button>
            <div className="flex gap-4">
              <button onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'GOAL', { team: 'RED' })} className="flex-1 py-2 bg-red-600 hover:bg-red-500 rounded-lg font-semibold transition-colors">
                Goal RED
              </button>
              <button onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'GOAL', { team: 'BLUE' })} className="flex-1 py-2 bg-blue-600 hover:bg-blue-500 rounded-lg font-semibold transition-colors">
                Goal BLUE
              </button>
            </div>
            <button onClick={() => triggerEvent('calciobalilla', 'foosball-1', 'MATCH_END')} className="w-full py-2 bg-slate-600 hover:bg-slate-500 rounded-lg font-semibold transition-colors">
              End Match
            </button>
            <div className="mt-6 pt-6 border-t border-slate-700 flex items-center justify-between">
              <span className="font-semibold text-slate-300">Autoplay Mode</span>
              <button onClick={() => toggleAutoplay('calciobalilla', 'foosball-1')} className={`px-4 py-2 rounded-full font-bold transition-colors ${autoplayStates['foosball-1'] ? 'bg-purple-600 text-white' : 'bg-slate-700 text-slate-400'}`}>
                {autoplayStates['foosball-1'] ? 'ON' : 'OFF'}
              </button>
            </div>
          </div>
        </div>

        {/* Darts Card */}
        <div className="bg-slate-800 rounded-2xl p-6 shadow-xl border border-slate-700 hover:border-rose-500 transition-colors">
          <div className="flex items-center mb-6">
            <Target className="w-8 h-8 text-rose-400 mr-3" />
            <h2 className="text-2xl font-bold">Freccette</h2>
          </div>
          <div className="space-y-4">
            <button onClick={() => triggerEvent('freccette', 'darts-1', 'MATCH_START')} className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 rounded-lg font-semibold transition-colors">
              Start Match
            </button>
            <button onClick={() => triggerEvent('freccette', 'darts-1', 'DART_HIT', { score: 20, multiplier: 1 })} className="w-full py-2 bg-rose-600 hover:bg-rose-500 rounded-lg font-semibold transition-colors">
              Hit 20 (Single)
            </button>
            <button onClick={() => triggerEvent('freccette', 'darts-1', 'DART_HIT', { score: 20, multiplier: 3 })} className="w-full py-2 bg-rose-700 hover:bg-rose-600 rounded-lg font-semibold transition-colors">
              Hit 20 (Triple)
            </button>
            <button onClick={() => triggerEvent('freccette', 'darts-1', 'DART_HIT', { score: 50, multiplier: 1 })} className="w-full py-2 bg-red-600 hover:bg-red-500 rounded-lg font-semibold transition-colors">
              Bullseye
            </button>
            <button onClick={() => triggerEvent('freccette', 'darts-1', 'MATCH_END')} className="w-full py-2 bg-slate-600 hover:bg-slate-500 rounded-lg font-semibold transition-colors">
              End Match
            </button>
            <div className="mt-6 pt-6 border-t border-slate-700 flex items-center justify-between">
              <span className="font-semibold text-slate-300">Autoplay Mode</span>
              <button onClick={() => toggleAutoplay('freccette', 'darts-1')} className={`px-4 py-2 rounded-full font-bold transition-colors ${autoplayStates['darts-1'] ? 'bg-purple-600 text-white' : 'bg-slate-700 text-slate-400'}`}>
                {autoplayStates['darts-1'] ? 'ON' : 'OFF'}
              </button>
            </div>
          </div>
        </div>

        {/* Billiards Card */}
        <div className="bg-slate-800 rounded-2xl p-6 shadow-xl border border-slate-700 hover:border-amber-500 transition-colors">
          <div className="flex items-center mb-6">
            <CircleDashed className="w-8 h-8 text-amber-400 mr-3" />
            <h2 className="text-2xl font-bold">Biliardo</h2>
          </div>
          <div className="space-y-4">
            <button onClick={() => triggerEvent('biliardo', 'billiards-1', 'MATCH_START')} className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 rounded-lg font-semibold transition-colors">
              Start Match
            </button>
            <button onClick={() => triggerEvent('biliardo', 'billiards-1', 'BALL_POCKETED', { pocketId: 1, ballNumber: 8, ballColor: 'BLACK' })} className="w-full py-2 bg-zinc-700 hover:bg-zinc-600 rounded-lg font-semibold transition-colors">
              Pocket 8 Ball (Black)
            </button>
            <button onClick={() => triggerEvent('biliardo', 'billiards-1', 'BALL_POCKETED', { pocketId: 4, ballNumber: 3, ballColor: 'RED' })} className="w-full py-2 bg-amber-600 hover:bg-amber-500 rounded-lg font-semibold transition-colors">
              Pocket 3 Ball (Red)
            </button>
            <button onClick={() => triggerEvent('biliardo', 'billiards-1', 'MATCH_END')} className="w-full py-2 bg-slate-600 hover:bg-slate-500 rounded-lg font-semibold transition-colors">
              End Match
            </button>
            <div className="mt-6 pt-6 border-t border-slate-700 flex items-center justify-between">
              <span className="font-semibold text-slate-300">Autoplay Mode</span>
              <button onClick={() => toggleAutoplay('biliardo', 'billiards-1')} className={`px-4 py-2 rounded-full font-bold transition-colors ${autoplayStates['billiards-1'] ? 'bg-purple-600 text-white' : 'bg-slate-700 text-slate-400'}`}>
                {autoplayStates['billiards-1'] ? 'ON' : 'OFF'}
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}

export default App;
