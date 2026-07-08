import axios from 'axios';

const authInterceptor = (config: any) => {
  // ponytail: sessionStorage per isolamento tab / incognito
  const token = sessionStorage.getItem('bitpub_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

const on401 = (error: any) => {
  if (error?.response?.status === 401) {
    sessionStorage.removeItem('bitpub_token');
    // ponytail: redirect diretto, logout() è già implicito (sessionStorage svuotato sopra)
    window.location.href = '/login';
  }
  return Promise.reject(error);
};

// Gateway (8080) — routes with /api/v1/** prefix: auth, users, locales, catalog, tournaments, statistics
const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
});
api.interceptors.request.use(authInterceptor);
api.interceptors.response.use(undefined, on401);

// Gateway route for match-service is NOT under /api/v1 (see gateway-service application.yml: Path=/api/matches/**)
export const matchApi = axios.create({
  baseURL: 'http://localhost:8080/api/matches',
});
matchApi.interceptors.request.use(authInterceptor);
matchApi.interceptors.response.use(undefined, on401);

// Edge node (per-locale, on the local network) — reachable even when the cloud is down.
// In-match actions and tournament results are sent here so the Edge can buffer them during a
// cloud outage and replay them (idempotently) when the cloud returns. Reads/auth/admin still
// go through the gateway and are expected to fail during an outage.
// ponytail: single configurable Edge URL for the demo; resolve the Edge per-locale for multi-locale.
export const edgeApi = axios.create({
  baseURL: (import.meta.env.VITE_EDGE_URL ?? 'http://localhost:8089') + '/edge',
});
edgeApi.interceptors.request.use(authInterceptor);
edgeApi.interceptors.response.use(undefined, on401);

// Statistics reads (leaderboard) through the gateway, same /api/v1 convention
export const statsApi = axios.create({
  baseURL: 'http://localhost:8080/api/v1/statistics',
});
statsApi.interceptors.request.use(authInterceptor);
statsApi.interceptors.response.use(undefined, on401);

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

// ── Catalogo giochi (GAME_ADMIN) ───────────────────────────────────────────────
// Un evento simulato del gioco e' modellato da SensorDefinition (sostituisce il MAC hardware):
// `type` = tipologia di evento MQTT (GOAL, DART_HIT, BALL_POCKETED, MATCH_START, ...).
// Il campo booleano lato backend e' `isActuator` (Lombok) -> Jackson lo serializza come `actuator`.
export interface SensorDefinitionRecord {
  id: string;
  type: string;
  description: string;
  actuator: boolean;
  scoreIncrement: number;      // punti guadagnati quando il sensore va a segno
  successProbability: number;  // 0.0-1.0: probabilità RNG di successo
}

export interface GameTypeRecord {
  id: string;
  name: string;
  description: string;
  winScoreTarget: number;      // punti necessari per vincere
  sensors: SensorDefinitionRecord[];
}

export interface CreateGameTypePayload {
  name: string;
  description: string;
  winScoreTarget: number;
}

export interface AddSensorPayload {
  type: string;
  description: string;
  actuator: boolean;
  scoreIncrement: number;
  successProbability: number;
}

export const getGameTypes = () => api.get<GameTypeRecord[]>('/catalog/games');
export const getGameTypeById = (id: string) => api.get<GameTypeRecord>(`/catalog/games/${id}`);
export const createGameType = (payload: CreateGameTypePayload) => api.post<GameTypeRecord>('/catalog/games', payload);
export const updateGameType = (id: string, payload: CreateGameTypePayload) => api.put<GameTypeRecord>(`/catalog/games/${id}`, payload);
export const getSensorsByGameType = (gameTypeId: string) => api.get<SensorDefinitionRecord[]>(`/catalog/games/${gameTypeId}/sensors`);
export const addSensorToGameType = (gameTypeId: string, payload: AddSensorPayload) =>
  api.post<SensorDefinitionRecord>(`/catalog/games/${gameTypeId}/sensors`, payload);
export const deleteSensor = (gameTypeId: string, sensorId: string) =>
  api.delete(`/catalog/games/${gameTypeId}/sensors/${sensorId}`);

// ── Esplorazione locali / matchmaking PLAYER ────────────────────────────────────
export interface GameInstanceRecord {
  id: string;
  localInstanceId: string;
  gameTypeId: string;
  localeId: string;
  active: boolean;
}

export interface LocaleRecord {
  id: string;
  name: string;
  address: string;
  adminId: string;
  games: GameInstanceRecord[];
}

export interface TeamRecord {
  id: string;
  name: string;
  playerIds: string[];
  score: number;
}

export interface MatchRecord {
  id: string;
  gameInstanceId: string;
  localeId: string;
  gameTypeId: string;
  status: string; // WAITING_FOR_PLAYERS | IN_PROGRESS | COMPLETED | CANCELLED
  teamBased?: boolean; // true = partita a squadre (si registra il Team, non i singoli)
  startTime: string | null;
  endTime: string | null;
  teams: TeamRecord[];
  winnerId?: string | null; // userId del vincitore (null = pareggio o partita non terminata)
  currentTurnUserId?: string | null;
  breakDone?: boolean;
  solidTeamId?: string | null;
  stripedTeamId?: string | null;
}

// Azione di gioco data-driven: il giocatore preme il sensore `sensorType`; l'esito (RNG) lo
// decide il GenericSimulator lato backend in base alla successProbability configurata nel catalogo.
export interface GameActionPayload {
  sensorType: string;
}

// Locali ONLINE in tempo reale (almeno una macchina simulata attiva), non l'elenco statico completo.
export const getOnlineLocales = () => api.get<LocaleRecord[]>('/locales/online');

// Entra in lobby su una gameInstance: crea/aggiorna la partita (WAITING_FOR_PLAYERS -> IN_PROGRESS).
// tournamentMatchId valorizzato = partita di torneo: il match-service ammette solo i due
// giocatori abbinati nello scontro del tabellone (verifica avversario, 403 se non abbinato).
export const joinMatchLobby = (gameInstanceId: string, username: string, tournamentMatchId?: string) =>
  matchApi.post<MatchRecord>('/lobby', { gameInstanceId, username, tournamentMatchId });

export const getWaitingLobby = (gameInstanceId: string) =>
  matchApi.get<MatchRecord>(`/lobby/${gameInstanceId}`);

export const getMatch = (matchId: string) => matchApi.get<MatchRecord>(`/${matchId}`);

// Invia un'azione di gioco: l'Edge la pubblica sul cloud via MQTT (QoS1) e risponde subito 202.
// eventId = chiave di idempotenza (il cloud ignora un replay). Lo stato aggiornato NON torna nella
// risposta: arriva alla WebApp via il topic MQTT di match-state. Un'azione fuori turno viene
// scartata e loggata lato cloud (nessun 403 sincrono).
export const postGameAction = (matchId: string, action: GameActionPayload) =>
  edgeApi.post<void>(`/matches/${matchId}/action`, { ...action, eventId: crypto.randomUUID() });

export const getMatchesByPlayer = (playerId: string) => matchApi.get<MatchRecord[]>(`/by-player/${playerId}`);

// Backfill statistiche (PLATFORM_ADMIN): ricostruisce la leaderboard dai match gia' conclusi.
export const backfillStats = () => matchApi.post<number>('/admin/backfill-stats');

// Termina subito una partita in corso: il backend calcola il vincitore in base al punteggio
// piu' alto (piu' basso a freccette, dove si parte da 501 e si scende a 0) e aggiorna la leaderboard.
export const endMatch = (matchId: string) => matchApi.post<MatchRecord>(`/${matchId}/end`);

// ── Statistiche personali PLAYER ────────────────────────────────────────────────
export interface LeaderboardEntryRecord {
  id: string;
  playerName: string;
  gameTypeId: string;
  localeId: string | null;
  teamBased?: boolean;
  wins: number;
  losses: number;
  totalPoints: number;
  matchesPlayed: number;
  lastUpdated: string | null;
}

export const getMyLeaderboardStats = (playerName: string) =>
  statsApi.get<LeaderboardEntryRecord[]>(`/leaderboard/me/${encodeURIComponent(playerName)}`);

// ── Giochi piu' utilizzati in un locale ─────────────────────────────────────────
export interface GameUsageRecord {
  gameTypeId: string;
  matchesPlayed: number;
  players: number;
}

export const getLocaleGameUsage = (localeId: string) =>
  statsApi.get<GameUsageRecord[]>(`/locale/${localeId}/games-usage`);

// ── Tornei ───────────────────────────────────────────────────────────────────────
export interface TournamentRegistrationRecord {
  id: string;
  tournamentId: string;
  participantId: string;
  participantName: string;
  team?: boolean;          // true = iscrizione a squadre
  members?: string[];      // membri della squadra (o singolo giocatore)
  localeId: string;
  registeredAt: string;
  matchesPlayed?: number;    // partite giocate in QUESTO torneo
  goalsScored?: number;      // gol segnati in QUESTO torneo (via MQTT)
  currentStage?: string;     // fase raggiunta: Finale, Semifinale, ...
  tournamentStatus?: string; // UPCOMING | ACTIVE | COMPLETED
}

export interface RegisterPayload {
  participantId: string;
  participantName: string;
  localeId: string;
  team?: boolean;
  members?: string[];
}

// Singolo scontro del tabellone a eliminazione diretta.
export interface TournamentMatchRecord {
  id: string;
  round: number;
  matchIndex: number;
  player1Id: string | null;
  player1Name: string | null;
  player2Id: string | null;
  player2Name: string | null;
  winnerId: string | null;
  winnerName: string | null;
  score: string | null;
  nextMatchId: string | null;
}

export interface TournamentRecord {
  id: string;
  name: string;
  gameTypeId: string;
  teamBased: boolean;
  localeIds?: string[]; // locali coinvolti nel torneo
  startDate: string | null;
  endDate: string | null;
  status: string; // UPCOMING | ACTIVE | COMPLETED
  maxParticipants?: number | null;
  registrations?: TournamentRegistrationRecord[];
  bracket?: TournamentMatchRecord[]; // tabellone (presente da ACTIVE in poi)
}

export interface TournamentRankingRecord {
  id: string;
  tournamentId: string;
  participantId: string;
  participantName: string;
  goalsScored: number;
  matchesPlayed: number;
  matchesWon: number;
  currentRank: number;
}

export interface CreateTournamentPayload {
  name: string;
  gameTypeId: string;
  teamBased: boolean;
  localeIds: string[];
  maxParticipants?: number;
}

export const getAllTournaments = () => api.get<TournamentRecord[]>('/tournaments');
export const getActiveTournaments = () => api.get<TournamentRecord[]>('/tournaments/active');
export const getTournamentRegistrationsByPlayer = (playerId: string) =>
  api.get<TournamentRegistrationRecord[]>(`/tournaments/by-player/${playerId}`);
export const registerToTournament = (tournamentId: string, payload: RegisterPayload) =>
  api.post<TournamentRegistrationRecord>(`/tournaments/${tournamentId}/register`, payload);

// Classifica del torneo (sincronizzata dai risultati dei match).
export const getTournamentRankings = (tournamentId: string) =>
  api.get<TournamentRankingRecord[]>(`/tournaments/${tournamentId}/rankings`);

// Gestione tornei (PLATFORM_ADMIN / LOCALE_ADMIN)
export const createTournament = (payload: CreateTournamentPayload) =>
  api.post<TournamentRecord>('/tournaments', payload);
export const updateTournament = (id: string, payload: CreateTournamentPayload) =>
  api.put<TournamentRecord>(`/tournaments/${id}`, payload);
export const deleteTournament = (id: string) => api.delete(`/tournaments/${id}`);
export const startTournament = (id: string) => api.put<TournamentRecord>(`/tournaments/${id}/start`);
export const endTournament = (id: string) => api.put<TournamentRecord>(`/tournaments/${id}/end`);

// Tabellone a eliminazione diretta: genera il bracket e registra l'esito di uno scontro (LOCALE_ADMIN).
export const generateTournamentBracket = (id: string) =>
  api.post<TournamentRecord>(`/tournaments/${id}/bracket`);
// Esito di uno scontro del tabellone via l'Edge (bufferizzato se il cloud e' offline).
// L'operazione lato cloud e' gia' idempotente (imposta vincitore e slot successivo a valori
// deterministici), quindi un replay ripete gli stessi valori senza doppie avanzate.
export const updateTournamentMatchResult = (tournamentId: string, matchId: string, winnerId: string, stats?: string) =>
  edgeApi.put<TournamentRecord>(`/tournaments/${tournamentId}/matches/${matchId}/result`, null,
    { params: { winnerId, stats, eventId: crypto.randomUUID() } });

// Locali gestiti (per il form tornei del manager): il LOCALE_ADMIN vede i propri, il PLATFORM_ADMIN tutti.
export const getLocalesByAdmin = (adminId: string) => api.get<LocaleRecord[]>(`/locales/by-admin/${adminId}`);
export const getAllLocales = () => api.get<LocaleRecord[]>('/locales');

export default api;
