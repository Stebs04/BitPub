import axios from 'axios';

const authInterceptor = (config: any) => {
  const token = localStorage.getItem('bitpub_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

// Gateway (8080) — routes with /api/v1/** prefix: auth, users, locales, catalog, tournaments, statistics
const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
});
api.interceptors.request.use(authInterceptor);

// Gateway route for match-service is NOT under /api/v1 (see gateway-service application.yml: Path=/api/matches/**)
export const matchApi = axios.create({
  baseURL: 'http://localhost:8080/api/matches',
});
matchApi.interceptors.request.use(authInterceptor);

// Statistics reads (leaderboard) through the gateway, same /api/v1 convention
export const statsApi = axios.create({
  baseURL: 'http://localhost:8080/api/v1/statistics',
});
statsApi.interceptors.request.use(authInterceptor);

export interface CreateUserPayload {
  username: string;
  password: string;
  email: string;
  role: string;
}

export interface UserRecord {
  id: string;
  username: string;
  email: string;
  role: string;
  createdAt: string;
  lastLogin?: string;
}

export interface GlobalStats {
  totalLocales: number;
  totalUsers: number;
  activeMatches: number;
  activeTournaments: number;
}

// Gestione utenti (PLATFORM_ADMIN)
export const createUser = (payload: CreateUserPayload) => api.post('/users', payload);
export const getAllUsers = () => api.get<UserRecord[]>('/users');
export const updateUserRole = (id: string, role: string) => api.patch<UserRecord>(`/users/${id}/role`, null, { params: { role } });
export const deleteUser = (id: string) => api.delete(`/users/${id}`);

// Gestione locali (PLATFORM_ADMIN)
export const deleteLocale = (id: string) => api.delete(`/locales/${id}`);

// Statistiche globali della piattaforma (PLATFORM_ADMIN)
export const getGlobalStats = () => statsApi.get<GlobalStats>('/global');

export default api;
