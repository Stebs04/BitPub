import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserManagementPage from './UserManagementPage';
import { useAuthStore } from '../store/authStore';
import * as api from '../services/api';

vi.mock('../services/api', () => ({
  getAllUsers: vi.fn(),
  createUser: vi.fn(),
  deleteUser: vi.fn(),
  updateUserRole: vi.fn(),
  updateUser: vi.fn(),
  updateUserPassword: vi.fn(),
}));

const users = [
  { id: 'u1', username: 'alice', email: 'a@x.it', role: 'PLAYER' },
  { id: 'u2', username: 'bob', email: 'b@x.it', role: 'LOCALE_ADMIN' },
];

describe('UserManagementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.getState().login('tok', { id: 'admin', username: 'root', role: 'PLATFORM_ADMIN', localeId: null });
    (api.getAllUsers as any).mockResolvedValue({ data: users });
  });

  it('lists platform users', async () => {
    render(<UserManagementPage />);
    expect(await screen.findByText('alice')).toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
  });

  it('creates a user via the form', async () => {
    (api.createUser as any).mockResolvedValue({ data: {} });
    const { container } = render(<UserManagementPage />);
    await screen.findByText('alice');

    // Input non associa label a input: username = primo input del form, poi password/email
    await userEvent.type(container.querySelectorAll('input')[0], 'carl');
    await userEvent.type(container.querySelector('input[type=password]')!, 'pw');
    await userEvent.type(container.querySelector('input[type=email]')!, 'c@x.it');
    await userEvent.click(screen.getByRole('button', { name: /Crea utente/i }));

    await waitFor(() =>
      expect(api.createUser).toHaveBeenCalledWith({ username: 'carl', password: 'pw', email: 'c@x.it', role: 'PLAYER' }),
    );
  });
});
