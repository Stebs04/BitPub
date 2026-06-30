import { create } from 'zustand';

interface User {
  id: string;
  username: string;
  role: 'PLAYER' | 'LOCAL_ADMIN' | 'GAME_ADMIN' | 'PLATFORM_ADMIN';
}

interface AuthState {
  token: string | null;
  user: User | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('bitpub_token'),
  user: null, // Should ideally be hydrated from the token or an API call on load
  isAuthenticated: !!localStorage.getItem('bitpub_token'),
  login: (token, user) => {
    localStorage.setItem('bitpub_token', token);
    set({ token, user, isAuthenticated: true });
  },
  logout: () => {
    localStorage.removeItem('bitpub_token');
    set({ token: null, user: null, isAuthenticated: false });
  },
}));
