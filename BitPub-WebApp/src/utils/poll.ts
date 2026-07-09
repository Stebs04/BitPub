// Ripete fetchFn ogni intervalMs finche' `done` e' true o si esauriscono i tentativi,
// poi restituisce l'ultimo valore letto.
//
// Serve perche' create/delete passano dall'Edge (HTTP 202, MQTT fire-and-forget): il DB
// cloud si allinea in modo asincrono, quindi un singolo fetch immediato legge dati stantii
// (effetto "mostra sempre il penultimo"). Il polling rilegge finche' il cambiamento atteso
// non compare.
//
// ponytail: tetto fisso di tentativi, nessun backoff esponenziale. Se l'Edge diventa piu'
// lento, alzare `tries`.
export async function pollUntil<T>(
  fetchFn: () => Promise<T>,
  done: (value: T) => boolean,
  tries = 8,
  intervalMs = 500,
): Promise<T> {
  let value = await fetchFn();
  for (let i = 0; i < tries && !done(value); i++) {
    await new Promise((r) => setTimeout(r, intervalMs));
    value = await fetchFn();
  }
  return value;
}
