import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
import { useAuthStore } from '../store/authStore';
import api from '../services/api';

const navigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => navigate,
}));
vi.mock('../services/api', () => ({ default: { post: vi.fn() } }));

async function fillAndSubmit() {
  await userEvent.type(screen.getByPlaceholderText(/username/i), 'alice');
  await userEvent.type(screen.getByPlaceholderText(/password/i), 'pw');
  await userEvent.click(screen.getByRole('button', { name: /Sign In/i }));
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().logout();
  });

  it('logs in and navigates home on success', async () => {
    (api.post as any).mockResolvedValue({ data: { token: 'tok', id: 'u1', role: 'PLAYER', localeId: null } });
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await fillAndSubmit();

    expect(api.post).toHaveBeenCalledWith('/auth/login', { username: 'alice', password: 'pw' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.username).toBe('alice');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('shows an error and stays logged out on failure', async () => {
    (api.post as any).mockRejectedValue(new Error('401'));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await fillAndSubmit();

    expect(await screen.findByText(/Credenziali non valide/i)).toBeInTheDocument();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });
});
