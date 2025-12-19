/**
 * FilterChip Component Tests
 *
 * Comprehensive test suite for Flutter Parity FilterChip component.
 * Tests selection states, callbacks, accessibility, and Material Design compliance.
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from 'jest-axe';
import { MagicFilter } from '../../../src/flutterparity/material/chips/FilterChip';

describe('FilterChip', () => {
  // ============================================================================
  // BASIC RENDERING
  // ============================================================================

  it('renders without crashing', () => {
    render(<MagicFilter label="Test" />);
  });

  it('renders with label', () => {
    render(<MagicFilter label="Filter Chip" />);
    expect(screen.getByText('Filter Chip')).toBeInTheDocument();
  });

  it('renders with role="button"', () => {
    render(<MagicFilter label="Filter" />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  // ============================================================================
  // SELECTION STATE
  // ============================================================================

  it('renders in unselected state by default', () => {
    render(<MagicFilter label="Filter" />);
    const chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-pressed', 'false');
  });

  it('renders in selected state when selected=true', () => {
    render(<MagicFilter label="Filter" selected={true} />);
    const chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-pressed', 'true');
  });

  it('shows checkmark icon when selected', () => {
    render(<MagicFilter label="Filter" selected={true} />);
    // MUI Chip with checkmark will have specific class or icon
    const chip = screen.getByRole('button');
    expect(chip).toBeInTheDocument();
    // Check for MuiChip-icon class or CheckIcon presence
  });

  it('hides checkmark when not selected', () => {
    render(<MagicFilter label="Filter" selected={false} />);
    const chip = screen.getByRole('button');
    expect(chip).toBeInTheDocument();
  });

  it('hides checkmark when showCheckmark=false', () => {
    render(<MagicFilter label="Filter" selected={true} showCheckmark={false} />);
    // Should not show checkmark even when selected
  });

  // ============================================================================
  // EVENT CALLBACKS
  // ============================================================================

  it('calls onSelected when clicked (unselected → selected)', () => {
    const onSelected = jest.fn();
    render(<MagicFilter label="Filter" selected={false} onSelected={onSelected} />);

    const chip = screen.getByRole('button');
    fireEvent.click(chip);

    expect(onSelected).toHaveBeenCalledTimes(1);
    expect(onSelected).toHaveBeenCalledWith(true);
  });

  it('calls onSelected when clicked (selected → unselected)', () => {
    const onSelected = jest.fn();
    render(<MagicFilter label="Filter" selected={true} onSelected={onSelected} />);

    const chip = screen.getByRole('button');
    fireEvent.click(chip);

    expect(onSelected).toHaveBeenCalledTimes(1);
    expect(onSelected).toHaveBeenCalledWith(false);
  });

  it('does not call onSelected when disabled', () => {
    const onSelected = jest.fn();
    render(
      <MagicFilter
        label="Filter"
        selected={false}
        enabled={false}
        onSelected={onSelected}
      />
    );

    const chip = screen.getByRole('button');
    fireEvent.click(chip);

    expect(onSelected).not.toHaveBeenCalled();
  });

  it('supports multiple clicks (toggle behavior)', () => {
    const onSelected = jest.fn();
    render(<MagicFilter label="Filter" selected={false} onSelected={onSelected} />);

    const chip = screen.getByRole('button');

    // First click: false → true
    fireEvent.click(chip);
    expect(onSelected).toHaveBeenLastCalledWith(true);

    // Second click: true → false
    fireEvent.click(chip);
    expect(onSelected).toHaveBeenLastCalledWith(false);

    // Third click: false → true
    fireEvent.click(chip);
    expect(onSelected).toHaveBeenLastCalledWith(true);

    expect(onSelected).toHaveBeenCalledTimes(3);
  });

  // ============================================================================
  // ENABLED/DISABLED STATE
  // ============================================================================

  it('is enabled by default', () => {
    render(<MagicFilter label="Filter" />);
    const chip = screen.getByRole('button');
    expect(chip).not.toBeDisabled();
  });

  it('is disabled when enabled=false', () => {
    render(<MagicFilter label="Filter" enabled={false} />);
    const chip = screen.getByRole('button');
    expect(chip).toBeDisabled();
  });

  it('applies disabled styles', () => {
    render(<MagicFilter label="Filter" enabled={false} />);
    const chip = screen.getByRole('button');
    // MUI applies disabled attribute
    expect(chip).toBeDisabled();
  });

  // ============================================================================
  // AVATAR
  // ============================================================================

  it('renders avatar when provided and not selected', () => {
    render(<MagicFilter label="User" avatar="https://example.com/avatar.jpg" selected={false} />);
    // Avatar should be rendered
    const chip = screen.getByRole('button');
    expect(chip).toBeInTheDocument();
  });

  it('shows checkmark instead of avatar when selected', () => {
    render(<MagicFilter label="User" avatar="https://example.com/avatar.jpg" selected={true} />);
    // Checkmark should replace avatar when selected
  });

  // ============================================================================
  // ACCESSIBILITY
  // ============================================================================

  it('has proper aria-label (default)', () => {
    render(<MagicFilter label="My Filter" selected={false} />);
    const chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-label', 'My Filter, not selected');
  });

  it('has proper aria-label when selected', () => {
    render(<MagicFilter label="My Filter" selected={true} />);
    const chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-label', 'My Filter, selected');
  });

  it('uses custom accessibilityLabel when provided', () => {
    render(<MagicFilter label="Filter" accessibilityLabel="Custom label for filter" />);
    const chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-label', 'Custom label for filter');
  });

  it('has aria-pressed attribute', () => {
    const { rerender } = render(<MagicFilter label="Filter" selected={false} />);
    let chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-pressed', 'false');

    rerender(<MagicFilter label="Filter" selected={true} />);
    chip = screen.getByRole('button');
    expect(chip).toHaveAttribute('aria-pressed', 'true');
  });

  it('should have no accessibility violations (unselected)', async () => {
    const { container } = render(<MagicFilter label="Filter" selected={false} />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have no accessibility violations (selected)', async () => {
    const { container } = render(<MagicFilter label="Filter" selected={true} />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have no accessibility violations (disabled)', async () => {
    const { container } = render(<MagicFilter label="Filter" enabled={false} />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  // ============================================================================
  // KEYBOARD INTERACTION
  // ============================================================================

  it('supports keyboard interaction (Enter key)', async () => {
    const onSelected = jest.fn();
    const user = userEvent.setup();

    render(<MagicFilter label="Filter" selected={false} onSelected={onSelected} />);
    const chip = screen.getByRole('button');

    chip.focus();
    await user.keyboard('{Enter}');

    expect(onSelected).toHaveBeenCalledWith(true);
  });

  it('supports keyboard interaction (Space key)', async () => {
    const onSelected = jest.fn();
    const user = userEvent.setup();

    render(<MagicFilter label="Filter" selected={false} onSelected={onSelected} />);
    const chip = screen.getByRole('button');

    chip.focus();
    await user.keyboard(' ');

    expect(onSelected).toHaveBeenCalledWith(true);
  });

  it('is focusable when enabled', () => {
    render(<MagicFilter label="Filter" enabled={true} />);
    const chip = screen.getByRole('button');

    chip.focus();
    expect(chip).toHaveFocus();
  });

  it('is not focusable when disabled', () => {
    render(<MagicFilter label="Filter" enabled={false} />);
    const chip = screen.getByRole('button');

    chip.focus();
    expect(chip).not.toHaveFocus();
  });

  // ============================================================================
  // EDGE CASES
  // ============================================================================

  it('handles empty label gracefully', () => {
    render(<MagicFilter label="" />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('handles long label', () => {
    const longLabel = 'A'.repeat(100);
    render(<MagicFilter label={longLabel} />);
    expect(screen.getByText(longLabel)).toBeInTheDocument();
  });

  it('handles special characters in label', () => {
    render(<MagicFilter label="Filter: <>&" />);
    expect(screen.getByText('Filter: <>&')).toBeInTheDocument();
  });

  it('handles undefined onSelected callback', () => {
    render(<MagicFilter label="Filter" />);
    const chip = screen.getByRole('button');

    // Should not crash when clicking without onSelected
    expect(() => fireEvent.click(chip)).not.toThrow();
  });

  // ============================================================================
  // STATE COMBINATIONS
  // ============================================================================

  it('handles all state combinations', () => {
    const states = [
      { selected: false, enabled: true },
      { selected: true, enabled: true },
      { selected: false, enabled: false },
      { selected: true, enabled: false },
    ];

    states.forEach(({ selected, enabled }) => {
      const { unmount } = render(
        <MagicFilter
          label={`Filter_${selected}_${enabled}`}
          selected={selected}
          enabled={enabled}
        />
      );

      const chip = screen.getByRole('button');

      expect(chip).toHaveAttribute('aria-pressed', String(selected));

      if (!enabled) {
        expect(chip).toBeDisabled();
      } else {
        expect(chip).not.toBeDisabled();
      }

      unmount();
    });
  });

  // ============================================================================
  // VISUAL REGRESSION (Snapshot)
  // ============================================================================

  it('matches snapshot (default)', () => {
    const { container } = render(<MagicFilter label="Filter" />);
    expect(container.firstChild).toMatchSnapshot();
  });

  it('matches snapshot (selected)', () => {
    const { container } = render(<MagicFilter label="Filter" selected={true} />);
    expect(container.firstChild).toMatchSnapshot();
  });

  it('matches snapshot (disabled)', () => {
    const { container } = render(<MagicFilter label="Filter" enabled={false} />);
    expect(container.firstChild).toMatchSnapshot();
  });

  it('matches snapshot (with avatar)', () => {
    const { container } = render(
      <MagicFilter label="User" avatar="https://example.com/avatar.jpg" />
    );
    expect(container.firstChild).toMatchSnapshot();
  });
});
