/**
 * Phase 3 Button Components Tests
 * Tests for SplitButton, LoadingButton, and CloseButton
 *
 * @since 3.1.0-phase3
 */

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SplitButton, LoadingButton, CloseButton } from '../src/flutterparity/material/buttons';

describe('Phase 3 Button Components', () => {
  describe('SplitButton', () => {
    it('renders with text and icon', () => {
      render(
        <SplitButton
          text="Save"
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
            { label: 'Save as Draft', value: 'save-draft' },
          ]}
        />
      );
      expect(screen.getByText('Save')).toBeInTheDocument();
    });

    it('calls onPressed when main button is clicked', () => {
      const handlePress = jest.fn();
      render(
        <SplitButton
          text="Save"
          onPressed={handlePress}
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
          ]}
        />
      );

      fireEvent.click(screen.getByText('Save'));
      expect(handlePress).toHaveBeenCalledTimes(1);
    });

    it('opens menu when dropdown button is clicked', async () => {
      render(
        <SplitButton
          text="Save"
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
            { label: 'Save as Draft', value: 'save-draft' },
          ]}
        />
      );

      const dropdownButton = screen.getByLabelText('select action');
      fireEvent.click(dropdownButton);

      await waitFor(() => {
        expect(screen.getByText('Save and Close')).toBeInTheDocument();
        expect(screen.getByText('Save as Draft')).toBeInTheDocument();
      });
    });

    it('calls onMenuItemPressed when menu item is clicked', async () => {
      const handleMenuPress = jest.fn();
      render(
        <SplitButton
          text="Save"
          onMenuItemPressed={handleMenuPress}
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
          ]}
        />
      );

      const dropdownButton = screen.getByLabelText('select action');
      fireEvent.click(dropdownButton);

      await waitFor(() => {
        fireEvent.click(screen.getByText('Save and Close'));
      });

      expect(handleMenuPress).toHaveBeenCalledWith('save-close');
    });

    it('disables both buttons when enabled is false', () => {
      render(
        <SplitButton
          text="Save"
          enabled={false}
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
          ]}
        />
      );

      const buttons = screen.getAllByRole('button');
      buttons.forEach(button => {
        expect(button).toBeDisabled();
      });
    });

    it('has proper accessibility attributes', () => {
      render(
        <SplitButton
          text="Save"
          accessibilityLabel="Save document"
          menuItems={[
            { label: 'Save and Close', value: 'save-close' },
          ]}
        />
      );

      expect(screen.getByLabelText('Save document')).toBeInTheDocument();
      expect(screen.getByLabelText('select action')).toHaveAttribute('aria-haspopup', 'menu');
    });
  });

  describe('LoadingButton', () => {
    it('renders with text', () => {
      render(<LoadingButton text="Submit" />);
      expect(screen.getByText('Submit')).toBeInTheDocument();
    });

    it('shows loading indicator when loading is true', () => {
      render(<LoadingButton text="Submit" loading={true} />);

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('aria-busy', 'true');
    });

    it('is disabled when loading', () => {
      render(<LoadingButton text="Submit" loading={true} />);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
    });

    it('calls onPressed when clicked and not loading', () => {
      const handlePress = jest.fn();
      render(<LoadingButton text="Submit" onPressed={handlePress} />);

      fireEvent.click(screen.getByText('Submit'));
      expect(handlePress).toHaveBeenCalledTimes(1);
    });

    it('does not call onPressed when loading', () => {
      const handlePress = jest.fn();
      render(<LoadingButton text="Submit" loading={true} onPressed={handlePress} />);

      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(handlePress).not.toHaveBeenCalled();
    });

    it('shows loadingText when provided and loading', () => {
      render(
        <LoadingButton
          text="Submit"
          loading={true}
          loadingText="Submitting..."
        />
      );

      expect(screen.getByText('Submitting...')).toBeInTheDocument();
    });

    it('has proper accessibility attributes', () => {
      render(
        <LoadingButton
          text="Submit"
          loading={true}
          accessibilityLabel="Submit form"
        />
      );

      const button = screen.getByLabelText('Submit form');
      expect(button).toHaveAttribute('aria-busy', 'true');
    });
  });

  describe('CloseButton', () => {
    it('renders close icon', () => {
      render(<CloseButton />);
      expect(screen.getByLabelText('Close')).toBeInTheDocument();
    });

    it('calls onPressed when clicked', () => {
      const handlePress = jest.fn();
      render(<CloseButton onPressed={handlePress} />);

      fireEvent.click(screen.getByLabelText('Close'));
      expect(handlePress).toHaveBeenCalledTimes(1);
    });

    it('is disabled when enabled is false', () => {
      render(<CloseButton enabled={false} />);

      const button = screen.getByLabelText('Close');
      expect(button).toBeDisabled();
    });

    it('uses custom accessibility label', () => {
      render(<CloseButton accessibilityLabel="Dismiss dialog" />);
      expect(screen.getByLabelText('Dismiss dialog')).toBeInTheDocument();
    });

    it('applies size prop correctly', () => {
      const { rerender } = render(<CloseButton size="small" />);
      let button = screen.getByLabelText('Close');
      expect(button).toHaveClass('MuiIconButton-sizeSmall');

      rerender(<CloseButton size="medium" />);
      button = screen.getByLabelText('Close');
      expect(button).toHaveClass('MuiIconButton-sizeMedium');

      rerender(<CloseButton size="large" />);
      button = screen.getByLabelText('Close');
      expect(button).toHaveClass('MuiIconButton-sizeLarge');
    });

    it('applies edge prop correctly', () => {
      const { rerender } = render(<CloseButton edge="start" />);
      let button = screen.getByLabelText('Close');
      expect(button).toHaveClass('MuiIconButton-edgeStart');

      rerender(<CloseButton edge="end" />);
      button = screen.getByLabelText('Close');
      expect(button).toHaveClass('MuiIconButton-edgeEnd');
    });

    it('has proper ARIA attributes', () => {
      render(<CloseButton />);
      const button = screen.getByLabelText('Close');
      expect(button).toHaveAttribute('type', 'button');
    });
  });
});
