/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import api from './api';

// Gli interceptor non sono esportati: li raggiungiamo dalla request/response chain di axios
// (handlers interni) per verificarne il comportamento senza avviare richieste HTTP reali.
const requestFulfilled = (api.interceptors.request as any).handlers[0].fulfilled as (c: any) => any;
const responseRejected = (api.interceptors.response as any).handlers[0].rejected as (e: any) => any;

/**
 * Suite di test per gli interceptor dell'istanza axios (gestione autenticazione)
 */
describe('api interceptors', () => {
  // Pulisce lo storage prima di ogni test
  beforeEach(() => sessionStorage.clear());

  /**
   * Verifica l'aggiunta del token JWT agli header della richiesta se presente
   */
  it('attaches the Bearer token from sessionStorage', () => {
    sessionStorage.setItem('bitpub_token', 'tok123');
    const config = requestFulfilled({ headers: {} });
    expect(config.headers.Authorization).toBe('Bearer tok123');
  });

  /**
   * Verifica che la richiesta proceda senza token se non esiste
   */
  it('leaves the request unauthenticated when no token is stored', () => {
    const config = requestFulfilled({ headers: {} });
    expect(config.headers.Authorization).toBeUndefined();
  });

  /**
   * Verifica la rimozione del token e logout su errore 401 Unauthorized
   */
  it('clears the token on a 401 response', async () => {
    sessionStorage.setItem('bitpub_token', 'expired');
    // window.location non e' navigabile in jsdom: verifichiamo l'effetto osservabile (token rimosso).
    await expect(responseRejected({ response: { status: 401 } })).rejects.toBeTruthy();
    expect(sessionStorage.getItem('bitpub_token')).toBeNull();
  });

  /**
   * Verifica che errori diversi dal 401 (es. 500) non eliminino il token
   */
  it('passes through non-401 errors without clearing the token', async () => {
    sessionStorage.setItem('bitpub_token', 'keep');
    await expect(responseRejected({ response: { status: 500 } })).rejects.toBeTruthy();
    expect(sessionStorage.getItem('bitpub_token')).toBe('keep');
  });
});
