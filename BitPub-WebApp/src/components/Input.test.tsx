import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Input from './Input';

describe('Input', () => {
  it('renders a label bound to the field', () => {
    render(<Input label="Username" />);
    expect(screen.getByText('Username')).toBeInTheDocument();
  });

  it('shows the error message when provided', () => {
    render(<Input label="Email" error="Campo obbligatorio" />);
    expect(screen.getByText('Campo obbligatorio')).toBeInTheDocument();
  });

  it('forwards typed input to onChange', async () => {
    const onChange = vi.fn();
    render(<Input onChange={onChange} />);
    await userEvent.type(screen.getByRole('textbox'), 'hi');
    expect(onChange).toHaveBeenCalled();
  });
});
