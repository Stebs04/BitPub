import React, { useCallback, useEffect, useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '../components/Card';
import Button from '../components/Button';
import Input from '../components/Input';
import { Boxes, Plus, Cpu, Radio, ChevronRight, Gamepad2, Pencil, Trash2, Save, X } from 'lucide-react';
import {
  getGameTypes,
  createGameType,
  updateGameType,
  addSensorToGameType,
  deleteSensor,
  type GameTypeRecord,
  type SensorDefinitionRecord,
} from '../services/api';

// Preset di eventi simulati piu' comuni: velocizza la definizione del catalogo
// senza costringere il GAME_ADMIN a ricordare le stringhe MQTT a memoria.
const EVENT_PRESETS: { type: string; description: string }[] = [
  { type: 'MATCH_START', description: 'Inizio partita' },
  { type: 'MATCH_END', description: 'Fine partita' },
  { type: 'GOAL', description: 'Goal rilevato' },
  { type: 'DART_HIT', description: 'Punteggio freccetta' },
  { type: 'BALL_POCKETED', description: 'Palla imbucata' },
  { type: 'FOUL', description: 'Fallo commesso' },
];

const emptySensorForm = { type: '', description: '', actuator: false, scoreIncrement: 1, successProbability: 1 };

const GameCatalogPage: React.FC = () => {
  const [gameTypes, setGameTypes] = useState<GameTypeRecord[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form nuovo tipo di gioco
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newWinScoreTarget, setNewWinScoreTarget] = useState(10);
  const [creating, setCreating] = useState(false);

  // Form nuovo evento simulato (sensore) per il tipo selezionato
  const [sensorForm, setSensorForm] = useState(emptySensorForm);
  const [addingSensor, setAddingSensor] = useState(false);

  // Modifica inline nome/descrizione del tipo selezionato
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editWinScoreTarget, setEditWinScoreTarget] = useState(10);
  const [savingEdit, setSavingEdit] = useState(false);

  const fetchGameTypes = useCallback(async (preserveSelection = true) => {
    try {
      const res = await getGameTypes();
      const data = res.data || [];
      setGameTypes(data);
      setSelectedId((prev) => {
        if (preserveSelection && prev && data.some((g) => g.id === prev)) return prev;
        return data.length > 0 ? data[0].id : null;
      });
      setError(null);
    } catch (err) {
      console.error('Errore caricamento catalogo:', err);
      setError('Impossibile caricare il catalogo giochi.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchGameTypes(false);
  }, [fetchGameTypes]);

  const selected = gameTypes.find((g) => g.id === selectedId) || null;

  const handleCreateGameType = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newDescription.trim()) return;
    setCreating(true);
    try {
      const res = await createGameType({ name: newName.trim(), description: newDescription.trim(), winScoreTarget: newWinScoreTarget });
      setNewName('');
      setNewDescription('');
      setNewWinScoreTarget(10);
      await fetchGameTypes();
      if (res.data?.id) setSelectedId(res.data.id);
    } catch (err: any) {
      const msg = err?.response?.status === 409
        ? 'Esiste gia’ un tipo di gioco con questo nome.'
        : 'Errore nella creazione del tipo di gioco.';
      alert(msg);
    } finally {
      setCreating(false);
    }
  };

  const handleAddSensor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected || !sensorForm.type.trim() || !sensorForm.description.trim()) return;
    setAddingSensor(true);
    try {
      await addSensorToGameType(selected.id, {
        type: sensorForm.type.trim().toUpperCase().replace(/\s+/g, '_'),
        description: sensorForm.description.trim(),
        actuator: sensorForm.actuator,
        scoreIncrement: sensorForm.scoreIncrement,
        successProbability: sensorForm.successProbability,
      });
      setSensorForm(emptySensorForm);
      await fetchGameTypes();
    } catch (err) {
      console.error('Errore aggiunta evento:', err);
      alert('Errore nell’aggiunta dell’evento simulato.');
    } finally {
      setAddingSensor(false);
    }
  };

  const applyPreset = (preset: { type: string; description: string }) => {
    setSensorForm((prev) => ({ ...prev, type: preset.type, description: preset.description }));
  };

  const startEdit = () => {
    if (!selected) return;
    setEditName(selected.name);
    setEditDescription(selected.description);
    setEditWinScoreTarget(selected.winScoreTarget ?? 10);
    setEditing(true);
  };

  const cancelEdit = () => setEditing(false);

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected || !editName.trim() || !editDescription.trim()) return;
    setSavingEdit(true);
    try {
      await updateGameType(selected.id, { name: editName.trim(), description: editDescription.trim(), winScoreTarget: editWinScoreTarget });
      setEditing(false);
      await fetchGameTypes();
    } catch (err: any) {
      const msg = err?.response?.status === 409
        ? 'Esiste gia’ un tipo di gioco con questo nome.'
        : 'Errore nel salvataggio delle modifiche.';
      alert(msg);
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDeleteSensor = async (sensor: SensorDefinitionRecord) => {
    if (!selected) return;
    if (!window.confirm(`Rimuovere l’evento "${sensor.type}"?`)) return;
    try {
      await deleteSensor(selected.id, sensor.id);
      await fetchGameTypes();
    } catch (err) {
      console.error('Errore rimozione evento:', err);
      alert('Errore nella rimozione dell’evento.');
    }
  };

  if (loading) {
    return (
      <div className="p-8 animate-slide-up">
        <h1 className="text-3xl font-bold text-white mb-6">Catalogo Giochi</h1>
        <div className="text-white">Caricamento dati...</div>
      </div>
    );
  }

  return (
    <div className="p-8 animate-slide-up">
      <div className="flex items-center gap-3 mb-2">
        <Boxes className="w-8 h-8 text-brand-light" />
        <h1 className="text-3xl font-bold text-white">Catalogo Giochi</h1>
      </div>
      <p className="text-slate-400 mb-8">
        Definisci le tipologie di gioco e gli eventi simulati che ogni gioco deve generare.
        Gli eventi sostituiscono la vecchia configurazione hardware (MAC address).
      </p>

      {error && (
        <div className="mb-6 rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Colonna sinistra: creazione + elenco tipi di gioco */}
        <div className="lg:col-span-1 flex flex-col gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Nuovo Tipo di Gioco</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateGameType} className="flex flex-col gap-4">
                <Input
                  label="Nome"
                  placeholder="es. Biliardo"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  required
                />
                <Input
                  label="Descrizione"
                  placeholder="es. Biliardo simulato 8-ball"
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  required
                />
                <Input
                  label="Punti per vincere (winScoreTarget)"
                  type="number"
                  min={1}
                  value={newWinScoreTarget}
                  onChange={(e) => setNewWinScoreTarget(Number(e.target.value))}
                  required
                />
                <Button type="submit" disabled={creating}>
                  <Plus className="w-4 h-4 mr-2" /> {creating ? 'Creazione...' : 'Crea Gioco'}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Tipologie ({gameTypes.length})</CardTitle>
            </CardHeader>
            <CardContent>
              {gameTypes.length === 0 && (
                <p className="text-sm text-slate-500">Nessun tipo di gioco definito.</p>
              )}
              <div className="flex flex-col gap-2">
                {gameTypes.map((gt) => {
                  const active = gt.id === selectedId;
                  return (
                    <button
                      key={gt.id}
                      onClick={() => { setSelectedId(gt.id); setEditing(false); }}
                      className={`flex items-center justify-between gap-3 rounded-xl px-4 py-3 text-left transition-all border ${
                        active
                          ? 'bg-brand/20 text-brand-light border-brand/30'
                          : 'bg-white/5 text-slate-300 border-transparent hover:bg-slate-800'
                      }`}
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <Gamepad2 className="w-5 h-5 shrink-0" />
                        <div className="min-w-0">
                          <p className="font-medium truncate">{gt.name}</p>
                          <p className="text-xs text-slate-500 truncate">
                            {(gt.sensors?.length || 0)} eventi
                          </p>
                        </div>
                      </div>
                      <ChevronRight className="w-4 h-4 shrink-0 opacity-60" />
                    </button>
                  );
                })}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Colonna destra: dettaglio tipo selezionato + eventi simulati */}
        <div className="lg:col-span-2">
          {!selected ? (
            <div className="glass-panel p-12 text-center">
              <p className="text-slate-400 text-lg">Seleziona o crea un tipo di gioco per configurarne gli eventi.</p>
            </div>
          ) : (
            <Card>
              <CardHeader>
                {editing ? (
                  <form onSubmit={handleSaveEdit} className="flex flex-col gap-3">
                    <Input
                      label="Nome"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      required
                    />
                    <Input
                      label="Descrizione"
                      value={editDescription}
                      onChange={(e) => setEditDescription(e.target.value)}
                      required
                    />
                    <Input
                      label="Punti per vincere (winScoreTarget)"
                      type="number"
                      min={1}
                      value={editWinScoreTarget}
                      onChange={(e) => setEditWinScoreTarget(Number(e.target.value))}
                      required
                    />
                    <div className="flex gap-2">
                      <Button type="submit" size="sm" disabled={savingEdit}>
                        <Save className="w-4 h-4 mr-2" /> {savingEdit ? 'Salvataggio...' : 'Salva'}
                      </Button>
                      <Button type="button" size="sm" variant="secondary" onClick={cancelEdit}>
                        <X className="w-4 h-4 mr-2" /> Annulla
                      </Button>
                    </div>
                  </form>
                ) : (
                  <div className="flex items-center justify-between gap-3">
                    <div className="min-w-0">
                      <CardTitle>{selected.name}</CardTitle>
                      <p className="text-slate-400 text-sm mt-1">{selected.description}</p>
                      <p className="text-xs text-brand-light mt-1">🏆 Vittoria a {selected.winScoreTarget} punti</p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className="px-3 py-1 bg-brand/20 text-brand-light rounded-full text-xs font-semibold">
                        {(selected.sensors?.length || 0)} eventi
                      </span>
                      <button
                        onClick={startEdit}
                        className="text-slate-400 hover:text-white"
                        title="Modifica nome/descrizione"
                      >
                        <Pencil className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                )}
              </CardHeader>
              <CardContent>
                {/* Elenco eventi simulati gia' definiti */}
                <div className="space-y-2 mb-6">
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">Eventi Simulati</p>
                  {(!selected.sensors || selected.sensors.length === 0) && (
                    <p className="text-sm text-slate-500 px-1">Nessun evento definito per questo gioco.</p>
                  )}
                  {(selected.sensors || []).map((s: SensorDefinitionRecord) => (
                    <div key={s.id} className="flex items-center justify-between gap-3 bg-white/5 p-3 rounded-lg">
                      <div className="flex items-center gap-3 min-w-0">
                        {s.actuator
                          ? <Radio className="w-5 h-5 text-accent-pink shrink-0" />
                          : <Cpu className="w-5 h-5 text-brand shrink-0" />}
                        <div className="min-w-0">
                          <p className="font-mono text-sm text-white truncate">{s.type}</p>
                          <p className="text-xs text-slate-500 truncate">{s.description}</p>
                          {!s.actuator && (
                            <p className="text-xs text-emerald-400/80">
                              +{s.scoreIncrement} pt · {Math.round((s.successProbability ?? 0) * 100)}% successo
                            </p>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-3 shrink-0">
                        <span className={`text-xs font-semibold ${s.actuator ? 'text-accent-pink' : 'text-emerald-400'}`}>
                          {s.actuator ? 'Attuatore' : 'Sensore'}
                        </span>
                        <button
                          onClick={() => handleDeleteSensor(s)}
                          className="text-slate-400 hover:text-red-400"
                          title="Rimuovi evento"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Form aggiunta nuovo evento simulato */}
                <div className="pt-4 border-t border-white/5">
                  <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-3">Aggiungi Evento</p>

                  <div className="flex flex-wrap gap-2 mb-4">
                    {EVENT_PRESETS.map((p) => (
                      <button
                        key={p.type}
                        type="button"
                        onClick={() => applyPreset(p)}
                        className="px-3 py-1.5 rounded-full text-xs font-mono border border-slate-700 bg-slate-800/50 text-slate-300 hover:border-brand hover:text-brand-light transition-colors"
                      >
                        {p.type}
                      </button>
                    ))}
                  </div>

                  <form onSubmit={handleAddSensor} className="flex flex-col gap-4">
                    <div className="flex flex-col md:flex-row gap-4">
                      <Input
                        label="Tipo evento (MQTT)"
                        placeholder="es. GOAL"
                        value={sensorForm.type}
                        onChange={(e) => setSensorForm((prev) => ({ ...prev, type: e.target.value }))}
                        required
                      />
                      <Input
                        label="Descrizione"
                        placeholder="es. Goal rilevato"
                        value={sensorForm.description}
                        onChange={(e) => setSensorForm((prev) => ({ ...prev, description: e.target.value }))}
                        required
                      />
                    </div>
                    <div className="flex flex-col md:flex-row gap-4">
                      <Input
                        label="Punti guadagnati (scoreIncrement)"
                        type="number"
                        min={0}
                        value={sensorForm.scoreIncrement}
                        onChange={(e) => setSensorForm((prev) => ({ ...prev, scoreIncrement: Number(e.target.value) }))}
                      />
                      <Input
                        label="Probabilità successo (0.0 - 1.0)"
                        type="number"
                        min={0}
                        max={1}
                        step={0.05}
                        value={sensorForm.successProbability}
                        onChange={(e) => setSensorForm((prev) => ({ ...prev, successProbability: Number(e.target.value) }))}
                      />
                    </div>
                    <div className="flex items-center justify-between gap-4">
                      <label className="flex items-center gap-2 text-sm text-slate-300 cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={sensorForm.actuator}
                          onChange={(e) => setSensorForm((prev) => ({ ...prev, actuator: e.target.checked }))}
                          className="h-4 w-4 rounded border-slate-600 bg-slate-800 accent-brand"
                        />
                        E&apos; un attuatore (es. display/led), non un sensore di evento
                      </label>
                      <Button type="submit" size="sm" className="shrink-0" disabled={addingSensor}>
                        <Plus className="w-4 h-4 mr-2" /> {addingSensor ? 'Aggiunta...' : 'Aggiungi Evento'}
                      </Button>
                    </div>
                  </form>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
};

export default GameCatalogPage;
