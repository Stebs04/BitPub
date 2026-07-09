import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import LocalesPage from './LocalesPage';
import { useAuthStore } from '../store/authStore';
import api, { getLocaleGameUsage } from '../services/api';

vi.mock('../services/api', () => ({
  default: { get: vi.fn() },
  getLocaleGameUsage: vi.fn(),
  deleteLocale: vi.fn(), createLocale: vi.fn(), addGameInstance: vi.fn(),
  toggleGameInstance: vi.fn(), deleteGameInstance: vi.fn(),
}));
vi.mock('../hooks/useGameTypeLabels', () => ({ useGameTypeLabels: () => ({}) }));

const locales = [
  { id: 'loc1', name: 'Bar Centrale', address: 'Via 1', adminId: 'a1',
    games: [{ id: 'g1', localInstanceId: 'calcio-1', gameTypeId: 'foosball', active: true }] },
];

describe('LocalesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (getLocaleGameUsage as any).mockResolvedValue({ data: [] });
    useAuthStore.getState().login('tok', { id: 'admin', username: 'root', role: 'PLATFORM_ADMIN', localeId: null });
  });

  it('lists locales and their game machines for a PLATFORM_ADMIN', async () => {
    (api.get as any).mockImplementation((url: string) =>
      Promise.resolve({ data: url === '/locales' ? locales : [] }));
    render(<LocalesPage />);

    expect(await screen.findByText('Bar Centrale')).toBeInTheDocument();
    expect(screen.getByText(/calcio-1/)).toBeInTheDocument();
    expect(screen.getByText('Nuovo Locale')).toBeInTheDocument();   // form solo per PLATFORM_ADMIN
  });

  it('shows the empty state when there are no locales', async () => {
    (api.get as any).mockResolvedValue({ data: [] });
    render(<LocalesPage />);
    expect(await screen.findByText('Nessun locale trovato.')).toBeInTheDocument();
  });
});
