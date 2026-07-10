/**
 * Autore: Luca Franzon 20054744
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GameControlPanel from './GameControlPanel';
import type { SensorDefinitionRecord } from '../services/api';

function sensor(over: Partial<SensorDefinitionRecord>): SensorDefinitionRecord {
  return { id: 'x', type: 'GOAL', description: '', actuator: false, scoreIncrement: 1, successProbability: 1, ...over };
}

/**
 * Suite di test per il componente GameControlPanel.
 * Verifica che vengano renderizzati solo gli eventi giocabili e che le interazioni funzionino.
 */
describe('GameControlPanel', () => {
  it('renders one button per playable sensor, excluding actuators and control events', () => {
    const sensors = [
      sensor({ id: '1', type: 'GOAL' }),
      sensor({ id: '2', type: 'SAVE', actuator: true }),      // attuatore: escluso
      sensor({ id: '3', type: 'MATCH_START' }),               // evento di controllo: escluso
      sensor({ id: '4', type: 'MISS' }),
    ];
    render(<GameControlPanel sensors={sensors} finished={false} sending={false} onAction={() => {}} />);

    expect(screen.getByRole('button', { name: /GOAL/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /MISS/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /SAVE/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /MATCH_START/ })).not.toBeInTheDocument();
  });

  it('fires onAction with the sensor type on click', async () => {
    const onAction = vi.fn();
    render(<GameControlPanel sensors={[sensor({ type: 'GOAL' })]} finished={false} sending={false} onAction={onAction} />);
    await userEvent.click(screen.getByRole('button', { name: /GOAL/ }));
    expect(onAction).toHaveBeenCalledWith('GOAL');
  });

  it('disables buttons when finished', async () => {
    const onAction = vi.fn();
    render(<GameControlPanel sensors={[sensor({ type: 'GOAL' })]} finished={true} sending={false} onAction={onAction} />);
    const btn = screen.getByRole('button', { name: /GOAL/ });
    expect(btn).toBeDisabled();
    await userEvent.click(btn);
    expect(onAction).not.toHaveBeenCalled();
  });

  it('shows an empty message when no playable sensors exist', () => {
    render(<GameControlPanel sensors={[sensor({ type: 'SAVE', actuator: true })]} finished={false} sending={false} onAction={() => {}} />);
    expect(screen.getByText(/Nessun evento giocabile/i)).toBeInTheDocument();
  });
});
