/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
import { useAuthStore } from '../store/authStore';
import api from '../services/api';

// Configurazione dei mock per il routing e per le chiamate di rete
const navigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => navigate,
}));
vi.mock('../services/api', () => ({ default: { post: vi.fn() } }));

/**
 * Funzione di utilità per compilare e inviare il modulo di login
 */
async function fillAndSubmit() {
  await userEvent.type(screen.getByPlaceholderText(/username/i), 'alice');
  await userEvent.type(screen.getByPlaceholderText(/password/i), 'pw');
  await userEvent.click(screen.getByRole('button', { name: /Sign In/i }));
}

/**
 * Suite di test per il componente LoginPage
 */
describe('LoginPage', () => {
  // Ripristina lo stato di autenticazione e resetta i mock prima di ciascun test
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().logout();
  });

  /**
   * Verifica il corretto reindirizzamento alla home in caso di successo
   */
  it('logs in and navigates home on success', async () => {
    (api.post as any).mockResolvedValue({ data: { token: 'tok', id: 'u1', role: 'PLAYER', localeId: null } });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await fillAndSubmit();

    expect(api.post).toHaveBeenCalledWith('/auth/login', { username: 'alice', password: 'pw' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.username).toBe('alice');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  /**
   * Verifica la visualizzazione dell'errore e il mantenimento dello stato in caso di fallimento
   */
  it('shows an error and stays logged out on failure', async () => {
    (api.post as any).mockRejectedValue(new Error('401'));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await fillAndSubmit();

    expect(await screen.findByText(/Credenziali non valide/i)).toBeInTheDocument();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
