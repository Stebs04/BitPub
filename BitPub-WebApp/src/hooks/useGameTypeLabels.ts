/**
 * Autore: Luca Franzon 20054744
 */

import { useEffect, useState } from 'react';
import { getGameTypes } from '../services/api';

// Cache condivisa gameTypeId -> nome leggibile, popolata una sola volta dal catalogo (/catalog/games).
// Sostituisce le vecchie mappe GAME_TYPE_LABELS hard-coded sparse nelle pagine: un nuovo gioco creato
// dal GAME_ADMIN appare automaticamente ovunque, senza modifiche al codice.
let cache: Record<string, string> | null = null;
let inflight: Promise<Record<string, string>> | null = null;
const listeners = new Set<(m: Record<string, string>) => void>();

// Gestisce il caricamento dei tipi di gioco interrogando le API backend se non sono presenti in cache
function load(): Promise<Record<string, string>> {
  if (cache) return Promise.resolve(cache);
  if (!inflight) {
    inflight = getGameTypes()
      .then((res) => {
        cache = Object.fromEntries(res.data.map((g) => [g.id, g.name || g.id]));
        listeners.forEach((l) => l(cache!));
        return cache!;
      })
      .catch(() => (cache = {}));
  }
  return inflight;
}

// Aggiunge/aggiorna un gioco nella cache (es. da un evento MQTT bitpub/config/games/+) senza rifare la fetch.
export function upsertGameTypeLabel(id: string, name: string) {
  cache = { ...(cache ?? {}), [id]: name || id };
  listeners.forEach((l) => l(cache!));
}

/** Mappa reattiva gameTypeId -> nome. Vuota finche' il catalogo non e' caricato; usare `labels[id] ?? id`. */
export function useGameTypeLabels(): Record<string, string> {
  const [labels, setLabels] = useState<Record<string, string>>(cache ?? {});
  useEffect(() => {
    load().then(setLabels);
    listeners.add(setLabels);
    return () => { listeners.delete(setLabels); };
  }, []);
  return labels;
}
