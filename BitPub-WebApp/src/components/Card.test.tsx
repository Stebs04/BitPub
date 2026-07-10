/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card, CardTitle, CardContent } from './Card';

/**
 * Suite di test per i componenti della Card.
 * Si assicura che il contenuto venga renderizzato correttamente e che le classi CSS vengano propagate.
 */
describe('Card', () => {
  it('renders children and forwards className', () => {
    render(<Card className="extra"><span>body</span></Card>);
    const el = screen.getByText('body').parentElement!;
    expect(el).toHaveTextContent('body');
    expect(el.className).toContain('extra');
  });

  it('CardTitle renders an h3 heading', () => {
    render(<CardTitle>Stats</CardTitle>);
    expect(screen.getByRole('heading', { level: 3, name: 'Stats' })).toBeInTheDocument();
  });

  it('CardContent renders its children', () => {
    render(<CardContent><p>inner</p></CardContent>);
    expect(screen.getByText('inner')).toBeInTheDocument();
  });
});
