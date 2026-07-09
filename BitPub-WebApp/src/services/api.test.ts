import { describe, it, expect, beforeEach, vi } from 'vitest';
import api from './api';

// Gli interceptor non sono esportati: li raggiungiamo dalla request/response chain di axios
// (handlers interni) per verificarne il comportamento senza avviare richieste HTTP reali.
const requestFulfilled = (api.interceptors.request as any).handlers[0].fulfilled as (c: any) => any;
const responseRejected = (api.interceptors.response as any).handlers[0].rejected as (e: any) => any;

describe('api interceptors', () => {
  beforeEach(() => sessionStorage.clear());

  it('attaches the Bearer token from sessionStorage', () => {
    sessionStorage.setItem('bitpub_token', 'tok123');
    const config = requestFulfilled({ headers: {} });
    expect(config.headers.Authorization).toBe('Bearer tok123');
  });

  it('leaves the request unauthenticated when no token is stored', () => {
    const config = requestFulfilled({ headers: {} });
    expect(config.headers.Authorization).toBeUndefined();
  });

  it('clears the token on a 401 response', async () => {
    sessionStorage.setItem('bitpub_token', 'expired');
    // window.location non e' navigabile in jsdom: verifichiamo l'effetto osservabile (token rimosso).
    await expect(responseRejected({ response: { status: 401 } })).rejects.toBeTruthy();
    expect(sessionStorage.getItem('bitpub_token')).toBeNull();
  });

  it('passes through non-401 errors without clearing the token', async () => {
    sessionStorage.setItem('bitpub_token', 'keep');
    await expect(responseRejected({ response: { status: 500 } })).rejects.toBeTruthy();
    expect(sessionStorage.getItem('bitpub_token')).toBe('keep');
  });
});
