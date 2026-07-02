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

export default api;
