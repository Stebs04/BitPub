/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';

/**
 * Suite di test per lo store di autenticazione
 */
describe('authStore', () => {
  // Reset dello storage e del token prima di ogni test
  beforeEach(() => {
    sessionStorage.clear();
    useAuthStore.getState().logout();
  });

  /**
   * Verifica che il login salvi il token, l'utente e persista in sessionStorage
   */
  it('login stores token + user and persists to sessionStorage', () => {
    const user = { id: 'u1', username: 'alice', role: 'PLAYER' as const, localeId: null };
    useAuthStore.getState().login('tok', user);

    const s = useAuthStore.getState();
    expect(s.isAuthenticated).toBe(true);
    expect(s.user?.username).toBe('alice');
    expect(sessionStorage.getItem('bitpub_token')).toBe('tok');
  });

  /**
   * Verifica che il logout rimuova correttamente lo stato e la sessione
   */
  it('logout clears state and sessionStorage', () => {
    useAuthStore.getState().login('tok', { id: 'u1', username: 'a', role: 'PLAYER', localeId: null });
    useAuthStore.getState().logout();

    const s = useAuthStore.getState();
    expect(s.isAuthenticated).toBe(false);
    expect(s.user).toBeNull();
    expect(sessionStorage.getItem('bitpub_token')).toBeNull();
  });

  /**
   * Verifica la corretta deduzione dei ruoli dell'utente in memoria
   */
  it('isPlatformAdmin / hasRole reflect the current user role', () => {
    useAuthStore.getState().login('tok', { id: 'u1', username: 'boss', role: 'PLATFORM_ADMIN', localeId: null });
    expect(useAuthStore.getState().isPlatformAdmin()).toBe(true);
    expect(useAuthStore.getState().hasRole('PLATFORM_ADMIN')).toBe(true);
    expect(useAuthStore.getState().hasRole('PLAYER')).toBe(false);
  });

  /**
   * Verifica la conservazione e l'accesso al localeId per il ruolo LOCALE_ADMIN
   */
  it('preserves localeId for a LOCALE_ADMIN', () => {
    useAuthStore.getState().login('tok', { id: 'u9', username: 'mgr', role: 'LOCALE_ADMIN', localeId: 'loc1' });
    expect(useAuthStore.getState().user?.localeId).toBe('loc1');
    expect(useAuthStore.getState().isPlatformAdmin()).toBe(false);
  });
});
