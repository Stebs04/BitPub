import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';

describe('authStore', () => {
  beforeEach(() => {
    sessionStorage.clear();
    useAuthStore.getState().logout();
  });

  it('login stores token + user and persists to sessionStorage', () => {
    const user = { id: 'u1', username: 'alice', role: 'PLAYER' as const, localeId: null };
    useAuthStore.getState().login('tok', user);

    const s = useAuthStore.getState();
    expect(s.isAuthenticated).toBe(true);
    expect(s.user?.username).toBe('alice');
    expect(sessionStorage.getItem('bitpub_token')).toBe('tok');
  });

  it('logout clears state and sessionStorage', () => {
    useAuthStore.getState().login('tok', { id: 'u1', username: 'a', role: 'PLAYER', localeId: null });
    useAuthStore.getState().logout();

    const s = useAuthStore.getState();
    expect(s.isAuthenticated).toBe(false);
    expect(s.user).toBeNull();
    expect(sessionStorage.getItem('bitpub_token')).toBeNull();
  });

  it('isPlatformAdmin / hasRole reflect the current user role', () => {
    useAuthStore.getState().login('tok', { id: 'u1', username: 'boss', role: 'PLATFORM_ADMIN', localeId: null });
    expect(useAuthStore.getState().isPlatformAdmin()).toBe(true);
    expect(useAuthStore.getState().hasRole('PLATFORM_ADMIN')).toBe(true);
    expect(useAuthStore.getState().hasRole('PLAYER')).toBe(false);
  });

  it('preserves localeId for a LOCALE_ADMIN', () => {
    useAuthStore.getState().login('tok', { id: 'u9', username: 'mgr', role: 'LOCALE_ADMIN', localeId: 'loc1' });
    expect(useAuthStore.getState().user?.localeId).toBe('loc1');
    expect(useAuthStore.getState().isPlatformAdmin()).toBe(false);
  });
});
