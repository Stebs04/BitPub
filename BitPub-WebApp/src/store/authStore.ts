/**
 * Autore: Luca Franzon 20054744
 */
import { create } from 'zustand';

export type Role = 'PLAYER' | 'LOCALE_ADMIN' | 'GAME_ADMIN' | 'PLATFORM_ADMIN';

interface User {
  id: string;
  username: string;
  role: Role;
  localeId?: string | null;
}

interface JwtPayload {
  sub?: string;
  userId?: string;
  role?: string;
  localeId?: string | null;
}

// Decodifica il payload del JWT (nessuna verifica firma: la validazione avviene lato gateway).
// Usata solo per ripristinare l'utente in memoria dopo un refresh della pagina.
/**
 * Decodifica in base64 il payload del token JWT fornito
 */
function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

/**
 * Converte il token in un oggetto utente estraendone i dati dal payload decodificato
 */
function userFromToken(token: string | null): User | null {
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  if (!payload?.role) return null;
  return {
    id: payload.userId || '',
    username: payload.sub || '',
    role: payload.role as Role,
    localeId: payload.localeId || null,
  };
}

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
  isPlatformAdmin: () => boolean;
  hasRole: (role: Role) => boolean;
}

const initialToken = sessionStorage.getItem('bitpub_token');

/**
 * Store Zustand globale per la gestione dello stato di autenticazione e dell'utente loggato
 */
export const useAuthStore = create<AuthState>((set, get) => ({
  token: initialToken,
  user: userFromToken(initialToken),
  isAuthenticated: !!initialToken,
  /**
   * Effettua il login salvando il token ed aggiornando lo stato
   */
  login: (token, user) => {
    sessionStorage.setItem('bitpub_token', token);
    set({ token, user, isAuthenticated: true });
  },
  /**
   * Effettua il logout rimuovendo i dati dalla sessione e dallo stato
   */
  logout: () => {
    sessionStorage.removeItem('bitpub_token');
    set({ token: null, user: null, isAuthenticated: false });
  },
  /**
   * Verifica rapidamente se l'utente corrente è PLATFORM_ADMIN
   */
  isPlatformAdmin: () => get().user?.role === 'PLATFORM_ADMIN',
  /**
   * Verifica se l'utente corrente possiede il ruolo richiesto
   */
  hasRole: (role) => get().user?.role === role,
}));
