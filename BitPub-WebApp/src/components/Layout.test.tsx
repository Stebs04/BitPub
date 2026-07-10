/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Layout from './Layout';
import { useAuthStore } from '../store/authStore';

function renderAs(role: 'PLAYER' | 'PLATFORM_ADMIN' | 'LOCALE_ADMIN') {
  useAuthStore.getState().login('tok', { id: 'u1', username: 'sam', role, localeId: 'loc1' });
  return render(<MemoryRouter><Layout /></MemoryRouter>);
}

/**
 * Suite di test per il componente Layout e la sua navigazione.
 * Assicura che le diverse voci di menu appaiano a seconda del ruolo (PLAYER, PLATFORM_ADMIN, ecc.).
 */
describe('Layout navigation', () => {
  beforeEach(() => useAuthStore.getState().logout());

  it('shows player-only items for a PLAYER', () => {
    renderAs('PLAYER');
    expect(screen.getByText('Gioca')).toBeInTheDocument();
    expect(screen.getByText('Le mie Statistiche')).toBeInTheDocument();
    expect(screen.queryByText('Live Match')).not.toBeInTheDocument();
    expect(screen.queryByText('Locali')).not.toBeInTheDocument();
  });

  it('shows admin items for a PLATFORM_ADMIN', () => {
    renderAs('PLATFORM_ADMIN');
    expect(screen.getByText('Live Match')).toBeInTheDocument();
    expect(screen.getByText('Locali')).toBeInTheDocument();
    expect(screen.getByText('Utenti')).toBeInTheDocument();
    expect(screen.queryByText('Gioca')).not.toBeInTheDocument();
  });

  it('logout clears authentication', async () => {
    renderAs('PLAYER');
    await userEvent.click(screen.getByRole('button', { name: /Logout/i }));
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });
});
