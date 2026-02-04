/**
 * Container Component Tests
 *
 * Comprehensive test suite for Flutter Parity Container component.
 * Tests all props, styles, and edge cases.
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Container } from '../../../src/flutterparity/layout/Container';
import { Alignment, BoxDecoration } from '../../../src/flutterparity/layout/types';

expect.extend(toHaveNoViolations);

describe('Container', () => {
  // ============================================================================
  // BASIC RENDERING
  // ============================================================================

  it('renders without crashing', () => {
    render(<Container />);
  });

  it('renders with child content', () => {
    render(
      <Container testID="container">
        <div>Test Content</div>
      </Container>
    );
    expect(screen.getByTestId('container')).toBeInTheDocument();
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('renders with testID prop', () => {
    render(<Container testID="my-container" />);
    expect(screen.getByTestId('my-container')).toBeInTheDocument();
  });

  // ============================================================================
  // SIZE PROPERTIES
  // ============================================================================

  it('applies width as number', () => {
    render(<Container testID="container" width={200} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ width: '200px' });
  });

  it('applies width as string', () => {
    render(<Container testID="container" width="50%" />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ width: '50%' });
  });

  it('applies height as number', () => {
    render(<Container testID="container" height={150} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ height: '150px' });
  });

  it('applies both width and height', () => {
    render(<Container testID="container" width={300} height={200} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      width: '300px',
      height: '200px',
    });
  });

  // ============================================================================
  // PADDING PROPERTIES
  // ============================================================================

  it('applies uniform padding', () => {
    render(<Container testID="container" padding={16} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ padding: '16px' });
  });

  it('applies padding object (all sides)', () => {
    render(
      <Container
        testID="container"
        padding={{ top: 8, right: 12, bottom: 16, left: 20 }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ padding: '8px 12px 16px 20px' });
  });

  it('applies symmetric padding', () => {
    render(
      <Container
        testID="container"
        padding={{ vertical: 10, horizontal: 20 }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ padding: '10px 20px' });
  });

  // ============================================================================
  // MARGIN PROPERTIES
  // ============================================================================

  it('applies uniform margin', () => {
    render(<Container testID="container" margin={24} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ margin: '24px' });
  });

  it('applies margin object', () => {
    render(
      <Container
        testID="container"
        margin={{ top: 10, right: 15, bottom: 20, left: 25 }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ margin: '10px 15px 20px 25px' });
  });

  // ============================================================================
  // DECORATION PROPERTIES
  // ============================================================================

  it('applies background color from decoration', () => {
    const decoration: BoxDecoration = {
      color: '#ff5722',
    };
    render(<Container testID="container" decoration={decoration} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ backgroundColor: '#ff5722' });
  });

  it('applies border from decoration', () => {
    const decoration: BoxDecoration = {
      border: { width: 2, color: '#000', style: 'solid' },
    };
    render(<Container testID="container" decoration={decoration} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      borderWidth: '2px',
      borderColor: '#000',
      borderStyle: 'solid',
    });
  });

  it('applies borderRadius from decoration', () => {
    const decoration: BoxDecoration = {
      borderRadius: 12,
    };
    render(<Container testID="container" decoration={decoration} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ borderRadius: '12px' });
  });

  it('applies box shadow from decoration', () => {
    const decoration: BoxDecoration = {
      boxShadow: {
        color: 'rgba(0,0,0,0.3)',
        blurRadius: 8,
        spreadRadius: 2,
        offset: { dx: 4, dy: 4 },
      },
    };
    render(<Container testID="container" decoration={decoration} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      boxShadow: '4px 4px 8px 2px rgba(0,0,0,0.3)',
    });
  });

  // ============================================================================
  // ALIGNMENT PROPERTIES
  // ============================================================================

  it('applies center alignment', () => {
    render(
      <Container testID="container" alignment={Alignment.Center}>
        <div>Child</div>
      </Container>
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
    });
  });

  it('applies top-left alignment', () => {
    render(
      <Container testID="container" alignment={Alignment.TopLeft}>
        <div>Child</div>
      </Container>
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      justifyContent: 'flex-start',
      alignItems: 'flex-start',
    });
  });

  it('applies bottom-right alignment', () => {
    render(
      <Container testID="container" alignment={Alignment.BottomRight}>
        <div>Child</div>
      </Container>
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      justifyContent: 'flex-end',
      alignItems: 'flex-end',
    });
  });

  // ============================================================================
  // CONSTRAINTS PROPERTIES
  // ============================================================================

  it('applies min/max width constraints', () => {
    render(
      <Container
        testID="container"
        constraints={{ minWidth: 100, maxWidth: 500, minHeight: 0, maxHeight: Infinity }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      minWidth: '100px',
      maxWidth: '500px',
    });
  });

  it('applies min/max height constraints', () => {
    render(
      <Container
        testID="container"
        constraints={{ minWidth: 0, maxWidth: Infinity, minHeight: 80, maxHeight: 400 }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({
      minHeight: '80px',
      maxHeight: '400px',
    });
  });

  it('ignores zero minWidth/minHeight', () => {
    render(
      <Container
        testID="container"
        constraints={{ minWidth: 0, maxWidth: Infinity, minHeight: 0, maxHeight: Infinity }}
      />
    );
    const element = screen.getByTestId('container');
    expect(element.style.minWidth).toBeFalsy();
    expect(element.style.minHeight).toBeFalsy();
  });

  // ============================================================================
  // COMPLEX COMBINATIONS
  // ============================================================================

  it('applies multiple styles simultaneously', () => {
    const decoration: BoxDecoration = {
      color: '#2196f3',
      borderRadius: 8,
      border: { width: 1, color: '#1976d2', style: 'solid' },
    };

    render(
      <Container
        testID="container"
        width={400}
        height={300}
        padding={20}
        margin={10}
        decoration={decoration}
        alignment={Alignment.Center}
        constraints={{ minWidth: 200, maxWidth: 600, minHeight: 100, maxHeight: 500 }}
      >
        <div>Complex Container</div>
      </Container>
    );

    const element = screen.getByTestId('container');

    // Size
    expect(element).toHaveStyle({ width: '400px', height: '300px' });

    // Spacing
    expect(element).toHaveStyle({ padding: '20px', margin: '10px' });

    // Decoration
    expect(element).toHaveStyle({
      backgroundColor: '#2196f3',
      borderRadius: '8px',
      borderWidth: '1px',
      borderColor: '#1976d2',
      borderStyle: 'solid',
    });

    // Alignment
    expect(element).toHaveStyle({
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
    });

    // Constraints
    expect(element).toHaveStyle({
      minWidth: '200px',
      maxWidth: '600px',
      minHeight: '100px',
      maxHeight: '500px',
    });
  });

  // ============================================================================
  // EDGE CASES
  // ============================================================================

  it('renders without any props', () => {
    render(<Container />);
    // Should not crash
  });

  it('handles null/undefined child gracefully', () => {
    render(<Container testID="container">{null}</Container>);
    expect(screen.getByTestId('container')).toBeInTheDocument();
  });

  it('applies box-sizing: border-box', () => {
    render(<Container testID="container" />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ boxSizing: 'border-box' });
  });

  it('handles zero width/height', () => {
    render(<Container testID="container" width={0} height={0} />);
    const element = screen.getByTestId('container');
    expect(element).toHaveStyle({ width: '0px', height: '0px' });
  });

  // ============================================================================
  // ACCESSIBILITY
  // ============================================================================

  it('should have no accessibility violations (basic)', async () => {
    const { container } = render(
      <Container testID="container">
        <p>Accessible content</p>
      </Container>
    );
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have no accessibility violations (complex)', async () => {
    const decoration: BoxDecoration = {
      color: '#ffffff',
      border: { width: 1, color: '#cccccc', style: 'solid' },
    };

    const { container } = render(
      <Container
        testID="container"
        padding={16}
        decoration={decoration}
      >
        <h2>Heading</h2>
        <p>Paragraph content</p>
      </Container>
    );
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  // ============================================================================
  // SNAPSHOT TESTS (Optional - for visual regression)
  // ============================================================================

  it('matches snapshot with default props', () => {
    const { container } = render(<Container />);
    expect(container.firstChild).toMatchSnapshot();
  });

  it('matches snapshot with all props', () => {
    const decoration: BoxDecoration = {
      color: '#f5f5f5',
      borderRadius: 12,
      border: { width: 2, color: '#e0e0e0', style: 'solid' },
    };

    const { container } = render(
      <Container
        width={500}
        height={400}
        padding={24}
        margin={16}
        decoration={decoration}
        alignment={Alignment.Center}
        constraints={{ minWidth: 300, maxWidth: 700, minHeight: 200, maxHeight: 600 }}
      >
        <div>Snapshot Content</div>
      </Container>
    );
    expect(container.firstChild).toMatchSnapshot();
  });
});
