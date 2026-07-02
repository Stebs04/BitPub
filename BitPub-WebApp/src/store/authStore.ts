import { create } from 'zustand';

export type Role = 'PLAYER' | 'LOCALE_ADMIN' | 'GAME_ADMIN' | 'PLATFORM_ADMIN';

interface User {
  id: string;
  username: string;
  role: Role;
}

interface JwtPayload {
  sub?: string;
  userId?: string;
  role?: string;
}

// Decodifica il payload del JWT (nessuna verifica firma: la validazione avviene lato gateway).
// Usata solo per ripristinare l'utente in memoria dopo un refresh della pagina.
function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

function userFromToken(token: string | null): User | null {
  if (!token) return null;
  const payload = decodeJwtPayload(token);
  if (!payload?.role) return null;
  return {
    id: payload.userId || '',
    username: payload.sub || '',
    role: payload.role as Role,
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

const initialToken = localStorage.getItem('bitpub_token');

export const useAuthStore = create<AuthState>((set, get) => ({
  token: initialToken,
  user: userFromToken(initialToken),
  isAuthenticated: !!initialToken,
  login: (token, user) => {
    localStorage.setItem('bitpub_token', token);
    set({ token, user, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('bitpub_token');
    set({ token: null, user: null, isAuthenticated: false });
  },
  isPlatformAdmin: () => get().user?.role === 'PLATFORM_ADMIN',
  hasRole: (role) => get().user?.role === role,
}));
