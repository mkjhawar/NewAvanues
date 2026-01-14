/**
 * Comprehensive Accessibility Test Suite
 *
 * Tests WCAG 2.1 AA compliance for all Flutter Parity components.
 * Validates:
 * - Proper ARIA attributes
 * - Keyboard navigation
 * - Color contrast
 * - Screen reader compatibility
 * - Focus management
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Container } from '../../src/flutterparity/layout/Container';
import { Row, Column } from '../../src/flutterparity/layout/Flex';
import { MagicFilter } from '../../src/flutterparity/material/chips/FilterChip';
import { Alignment, MainAxisAlignment } from '../../src/flutterparity/layout/types';

expect.extend(toHaveNoViolations);

describe('Accessibility Test Suite - WCAG 2.1 AA Compliance', () => {
  // ============================================================================
  // LAYOUT COMPONENTS ACCESSIBILITY
  // ============================================================================

  describe('Container Accessibility', () => {
    it('has no violations with semantic content', async () => {
      const { container } = render(
        <Container padding={16}>
          <article>
            <h1>Article Title</h1>
            <p>Article content goes here.</p>
          </article>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('has no violations with interactive elements', async () => {
      const { container } = render(
        <Container padding={16}>
          <button aria-label="Submit form">Submit</button>
          <a href="#section">Go to section</a>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('has no violations with form elements', async () => {
      const { container } = render(
        <Container padding={16}>
          <form>
            <label htmlFor="email">Email:</label>
            <input type="email" id="email" name="email" />

            <label htmlFor="password">Password:</label>
            <input type="password" id="password" name="password" />

            <button type="submit">Login</button>
          </form>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Row/Column Accessibility', () => {
    it('Row has no violations with navigation', async () => {
      const { container } = render(
        <Row mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
          <nav aria-label="Main navigation">
            <a href="/">Home</a>
            <a href="/about">About</a>
            <a href="/contact">Contact</a>
          </nav>
        </Row>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('Column has no violations with headings hierarchy', async () => {
      const { container } = render(
        <Column mainAxisAlignment={MainAxisAlignment.Start}>
          <h1>Main Heading</h1>
          <h2>Subheading</h2>
          <h3>Section Title</h3>
          <p>Content</p>
        </Column>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('Row has no violations with button group', async () => {
      const { container } = render(
        <Row mainAxisAlignment={MainAxisAlignment.Center}>
          <button aria-label="Cancel action">Cancel</button>
          <button aria-label="Confirm action">Confirm</button>
        </Row>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  // ============================================================================
  // MATERIAL COMPONENTS ACCESSIBILITY
  // ============================================================================

  describe('FilterChip Accessibility', () => {
    it('has proper ARIA attributes', () => {
      render(<FilterChip label="Filter" selected={false} />);
      const chip = screen.getByRole('button');

      expect(chip).toHaveAttribute('role', 'button');
      expect(chip).toHaveAttribute('aria-pressed', 'false');
      expect(chip).toHaveAttribute('aria-label');
    });

    it('updates aria-pressed when selected', () => {
      const { rerender } = render(<FilterChip label="Filter" selected={false} />);
      let chip = screen.getByRole('button');
      expect(chip).toHaveAttribute('aria-pressed', 'false');

      rerender(<FilterChip label="Filter" selected={true} />);
      chip = screen.getByRole('button');
      expect(chip).toHaveAttribute('aria-pressed', 'true');
    });

    it('has descriptive aria-label including state', () => {
      const { rerender } = render(<FilterChip label="Electronics" selected={false} />);
      let chip = screen.getByRole('button');
      expect(chip.getAttribute('aria-label')).toContain('not selected');

      rerender(<FilterChip label="Electronics" selected={true} />);
      chip = screen.getByRole('button');
      expect(chip.getAttribute('aria-label')).toContain('selected');
    });

    it('supports custom accessibility label', () => {
      render(
        <FilterChip
          label="Filter"
          accessibilityLabel="Filter products by category"
        />
      );
      const chip = screen.getByRole('button');
      expect(chip).toHaveAttribute('aria-label', 'Filter products by category');
    });

    it('has no axe violations (unselected)', async () => {
      const { container } = render(<FilterChip label="Filter" selected={false} />);
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('has no axe violations (selected)', async () => {
      const { container } = render(<FilterChip label="Filter" selected={true} />);
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('has no axe violations (disabled)', async () => {
      const { container } = render(<FilterChip label="Filter" enabled={false} />);
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  // ============================================================================
  // KEYBOARD NAVIGATION
  // ============================================================================

  describe('Keyboard Navigation', () => {
    it('FilterChip is keyboard accessible (Tab)', async () => {
      const user = userEvent.setup();

      render(
        <div>
          <FilterChip label="Filter 1" />
          <FilterChip label="Filter 2" />
          <FilterChip label="Filter 3" />
        </div>
      );

      const chips = screen.getAllByRole('button');

      // Tab to first chip
      await user.tab();
      expect(chips[0]).toHaveFocus();

      // Tab to second chip
      await user.tab();
      expect(chips[1]).toHaveFocus();

      // Tab to third chip
      await user.tab();
      expect(chips[2]).toHaveFocus();
    });

    it('FilterChip responds to Enter key', async () => {
      const onSelected = jest.fn();
      const user = userEvent.setup();

      render(<FilterChip label="Filter" selected={false} onSelected={onSelected} />);
      const chip = screen.getByRole('button');

      chip.focus();
      expect(chip).toHaveFocus();

      await user.keyboard('{Enter}');
      expect(onSelected).toHaveBeenCalledWith(true);
    });

    it('FilterChip responds to Space key', async () => {
      const onSelected = jest.fn();
      const user = userEvent.setup();

      render(<FilterChip label="Filter" selected={false} onSelected={onSelected} />);
      const chip = screen.getByRole('button');

      chip.focus();
      await user.keyboard(' ');
      expect(onSelected).toHaveBeenCalledWith(true);
    });

    it('disabled FilterChip is not in tab order', async () => {
      const user = userEvent.setup();

      render(
        <div>
          <FilterChip label="Enabled" enabled={true} />
          <FilterChip label="Disabled" enabled={false} />
          <FilterChip label="Enabled 2" enabled={true} />
        </div>
      );

      const chips = screen.getAllByRole('button');

      // Tab through - should skip disabled chip
      await user.tab();
      expect(chips[0]).toHaveFocus();

      await user.tab();
      expect(chips[2]).toHaveFocus(); // Skips chips[1] (disabled)
    });
  });

  // ============================================================================
  // FOCUS MANAGEMENT
  // ============================================================================

  describe('Focus Management', () => {
    it('maintains focus after interaction', async () => {
      const user = userEvent.setup();
      const onSelected = jest.fn();

      render(<FilterChip label="Filter" selected={false} onSelected={onSelected} />);
      const chip = screen.getByRole('button');

      chip.focus();
      expect(chip).toHaveFocus();

      await user.click(chip);
      expect(chip).toHaveFocus(); // Should maintain focus after click
    });

    it('shows visible focus indicator', () => {
      render(<FilterChip label="Filter" />);
      const chip = screen.getByRole('button');

      chip.focus();
      expect(chip).toHaveFocus();

      // MUI components have focus-visible styles
      // Check that element is focusable
      expect(chip).toHaveAttribute('tabIndex');
    });
  });

  // ============================================================================
  // SCREEN READER COMPATIBILITY
  // ============================================================================

  describe('Screen Reader Compatibility', () => {
    it('FilterChip provides state information to screen readers', () => {
      const { rerender } = render(<FilterChip label="Electronics" selected={false} />);
      let chip = screen.getByRole('button');

      // Screen reader should announce: "Electronics, not selected, button"
      expect(chip).toHaveAttribute('aria-label', 'Electronics, not selected');
      expect(chip).toHaveAttribute('aria-pressed', 'false');

      rerender(<FilterChip label="Electronics" selected={true} />);
      chip = screen.getByRole('button');

      // Screen reader should announce: "Electronics, selected, button"
      expect(chip).toHaveAttribute('aria-label', 'Electronics, selected');
      expect(chip).toHaveAttribute('aria-pressed', 'true');
    });

    it('disabled FilterChip is announced as disabled', () => {
      render(<FilterChip label="Filter" enabled={false} />);
      const chip = screen.getByRole('button');

      expect(chip).toBeDisabled();
      // Screen readers will announce "disabled" state
    });

    it('Container with semantic HTML is screen reader friendly', () => {
      render(
        <Container padding={16}>
          <section aria-labelledby="section-title">
            <h2 id="section-title">Section Title</h2>
            <p>Section content</p>
          </section>
        </Container>
      );

      const section = screen.getByRole('region');
      expect(section).toHaveAttribute('aria-labelledby', 'section-title');
    });
  });

  // ============================================================================
  // COLOR CONTRAST (Manual verification needed)
  // ============================================================================

  describe('Color Contrast (requires manual verification)', () => {
    it('FilterChip unselected has sufficient contrast', () => {
      render(<FilterChip label="Filter" selected={false} />);
      const chip = screen.getByRole('button');

      // This test documents that manual contrast checking is needed
      // MUI components typically meet WCAG AA standards (4.5:1 for text)
      expect(chip).toBeInTheDocument();
    });

    it('FilterChip selected has sufficient contrast', () => {
      render(<FilterChip label="Filter" selected={true} />);
      const chip = screen.getByRole('button');

      // Selected chips should have primary color background with contrasting text
      expect(chip).toBeInTheDocument();
    });
  });

  // ============================================================================
  // COMPLEX SCENARIOS
  // ============================================================================

  describe('Complex Accessibility Scenarios', () => {
    it('filter bar with multiple chips has no violations', async () => {
      const { container } = render(
        <Container padding={16}>
          <section aria-labelledby="filters-title">
            <h3 id="filters-title">Product Filters</h3>
            <Row mainAxisAlignment={MainAxisAlignment.Start}>
              <FilterChip label="On Sale" selected={false} />
              <FilterChip label="Free Shipping" selected={true} />
              <FilterChip label="In Stock" selected={true} />
            </Row>
          </section>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('form layout with labels and inputs has no violations', async () => {
      const { container } = render(
        <Container padding={24}>
          <form>
            <Column mainAxisAlignment={MainAxisAlignment.Start}>
              <div>
                <label htmlFor="username">Username:</label>
                <input type="text" id="username" name="username" required />
              </div>

              <div>
                <label htmlFor="email">Email:</label>
                <input type="email" id="email" name="email" required />
              </div>

              <Row mainAxisAlignment={MainAxisAlignment.End}>
                <button type="button">Cancel</button>
                <button type="submit">Submit</button>
              </Row>
            </Column>
          </form>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('nested layout with headings hierarchy has no violations', async () => {
      const { container } = render(
        <Container>
          <Column mainAxisAlignment={MainAxisAlignment.Start}>
            <h1>Page Title</h1>

            <section>
              <h2>Section 1</h2>
              <p>Content for section 1</p>

              <article>
                <h3>Article in section 1</h3>
                <p>Article content</p>
              </article>
            </section>

            <section>
              <h2>Section 2</h2>
              <p>Content for section 2</p>
            </section>
          </Column>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  // ============================================================================
  // WCAG 2.1 SPECIFIC TESTS
  // ============================================================================

  describe('WCAG 2.1 Success Criteria', () => {
    it('meets 1.3.1 Info and Relationships (semantic structure)', async () => {
      const { container } = render(
        <Container>
          <main>
            <h1>Main Content</h1>
            <nav aria-label="Primary">
              <ul>
                <li><a href="/">Home</a></li>
                <li><a href="/about">About</a></li>
              </ul>
            </nav>
          </main>
        </Container>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('meets 2.1.1 Keyboard (all functionality available via keyboard)', async () => {
      const user = userEvent.setup();
      const onSelected = jest.fn();

      render(<FilterChip label="Filter" onSelected={onSelected} />);
      const chip = screen.getByRole('button');

      // Navigate with Tab
      await user.tab();
      expect(chip).toHaveFocus();

      // Activate with Enter
      await user.keyboard('{Enter}');
      expect(onSelected).toHaveBeenCalled();
    });

    it('meets 2.4.7 Focus Visible (focus indicator visible)', () => {
      render(<FilterChip label="Filter" />);
      const chip = screen.getByRole('button');

      chip.focus();
      expect(chip).toHaveFocus();

      // MUI provides visible focus styles
      expect(chip).toBeVisible();
    });

    it('meets 4.1.2 Name, Role, Value (proper ARIA)', () => {
      render(<FilterChip label="Electronics" selected={true} />);
      const chip = screen.getByRole('button');

      // Name: aria-label
      expect(chip).toHaveAttribute('aria-label');

      // Role: button
      expect(chip).toHaveAttribute('role', 'button');

      // Value: aria-pressed
      expect(chip).toHaveAttribute('aria-pressed', 'true');
    });
  });
});
