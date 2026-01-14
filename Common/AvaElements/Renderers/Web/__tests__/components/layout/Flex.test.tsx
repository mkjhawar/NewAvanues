/**
 * Flex/Row/Column Component Tests
 *
 * Comprehensive test suite for Flutter Parity Flex, Row, and Column components.
 * Tests all alignment, direction, and sizing combinations.
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import { Flex, Row, Column } from '../../../src/flutterparity/layout/Flex';
import {
  FlexDirection,
  MainAxisAlignment,
  MainAxisSize,
  CrossAxisAlignment,
  TextDirection,
  VerticalDirection,
} from '../../../src/flutterparity/layout/types';

describe('Flex Component', () => {
  // ============================================================================
  // BASIC RENDERING
  // ============================================================================

  it('renders without crashing', () => {
    render(<Flex direction={FlexDirection.Horizontal} />);
  });

  it('renders with children', () => {
    render(
      <Flex direction={FlexDirection.Horizontal} testID="flex">
        <div>Child 1</div>
        <div>Child 2</div>
      </Flex>
    );
    expect(screen.getByTestId('flex')).toBeInTheDocument();
    expect(screen.getByText('Child 1')).toBeInTheDocument();
    expect(screen.getByText('Child 2')).toBeInTheDocument();
  });

  // ============================================================================
  // DIRECTION
  // ============================================================================

  it('applies horizontal direction (row)', () => {
    render(<Flex direction={FlexDirection.Horizontal} testID="flex" />);
    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({ flexDirection: 'row' });
  });

  it('applies vertical direction (column)', () => {
    render(<Flex direction={FlexDirection.Vertical} testID="flex" />);
    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({ flexDirection: 'column' });
  });

  it('applies RTL for horizontal direction', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        textDirection={TextDirection.RTL}
        testID="flex"
      />
    );
    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({ flexDirection: 'row-reverse' });
  });

  it('applies reversed vertical direction', () => {
    render(
      <Flex
        direction={FlexDirection.Vertical}
        verticalDirection={VerticalDirection.Up}
        testID="flex"
      />
    );
    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({ flexDirection: 'column-reverse' });
  });

  // ============================================================================
  // MAIN AXIS ALIGNMENT
  // ============================================================================

  it('applies MainAxisAlignment.Start', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.Start}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'flex-start' });
  });

  it('applies MainAxisAlignment.Center', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.Center}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'center' });
  });

  it('applies MainAxisAlignment.End', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.End}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'flex-end' });
  });

  it('applies MainAxisAlignment.SpaceBetween', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.SpaceBetween}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'space-between' });
  });

  it('applies MainAxisAlignment.SpaceAround', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.SpaceAround}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'space-around' });
  });

  it('applies MainAxisAlignment.SpaceEvenly', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.SpaceEvenly}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ justifyContent: 'space-evenly' });
  });

  // ============================================================================
  // CROSS AXIS ALIGNMENT
  // ============================================================================

  it('applies CrossAxisAlignment.Start', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        crossAxisAlignment={CrossAxisAlignment.Start}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ alignItems: 'flex-start' });
  });

  it('applies CrossAxisAlignment.Center (default)', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        crossAxisAlignment={CrossAxisAlignment.Center}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ alignItems: 'center' });
  });

  it('applies CrossAxisAlignment.End', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        crossAxisAlignment={CrossAxisAlignment.End}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ alignItems: 'flex-end' });
  });

  it('applies CrossAxisAlignment.Stretch', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        crossAxisAlignment={CrossAxisAlignment.Stretch}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ alignItems: 'stretch' });
  });

  it('applies CrossAxisAlignment.Baseline', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        crossAxisAlignment={CrossAxisAlignment.Baseline}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ alignItems: 'baseline' });
  });

  // ============================================================================
  // MAIN AXIS SIZE
  // ============================================================================

  it('applies MainAxisSize.Max for horizontal (width: 100%)', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisSize={MainAxisSize.Max}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ width: '100%' });
  });

  it('applies MainAxisSize.Max for vertical (height: 100%)', () => {
    render(
      <Flex
        direction={FlexDirection.Vertical}
        mainAxisSize={MainAxisSize.Max}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ height: '100%' });
  });

  it('applies MainAxisSize.Min for horizontal (width: fit-content)', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisSize={MainAxisSize.Min}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ width: 'fit-content' });
  });

  it('applies MainAxisSize.Min for vertical (height: fit-content)', () => {
    render(
      <Flex
        direction={FlexDirection.Vertical}
        mainAxisSize={MainAxisSize.Min}
        testID="flex"
      />
    );
    expect(screen.getByTestId('flex')).toHaveStyle({ height: 'fit-content' });
  });

  // ============================================================================
  // COMPLEX COMBINATIONS
  // ============================================================================

  it('combines all horizontal properties', () => {
    render(
      <Flex
        direction={FlexDirection.Horizontal}
        mainAxisAlignment={MainAxisAlignment.SpaceBetween}
        mainAxisSize={MainAxisSize.Max}
        crossAxisAlignment={CrossAxisAlignment.Stretch}
        testID="flex"
      >
        <div>Item 1</div>
        <div>Item 2</div>
        <div>Item 3</div>
      </Flex>
    );

    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({
      display: 'flex',
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'stretch',
      width: '100%',
    });
  });

  it('combines all vertical properties', () => {
    render(
      <Flex
        direction={FlexDirection.Vertical}
        mainAxisAlignment={MainAxisAlignment.Center}
        mainAxisSize={MainAxisSize.Min}
        crossAxisAlignment={CrossAxisAlignment.End}
        testID="flex"
      >
        <div>Item A</div>
        <div>Item B</div>
      </Flex>
    );

    const element = screen.getByTestId('flex');
    expect(element).toHaveStyle({
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'flex-end',
      height: 'fit-content',
    });
  });

  // ============================================================================
  // ACCESSIBILITY
  // ============================================================================

  it('should have no accessibility violations', async () => {
    const { container } = render(
      <Flex direction={FlexDirection.Horizontal} testID="flex">
        <button>Button 1</button>
        <button>Button 2</button>
      </Flex>
    );
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});

// ============================================================================
// ROW COMPONENT TESTS
// ============================================================================

describe('Row Component', () => {
  it('renders as horizontal flex', () => {
    render(<Row testID="row" />);
    const element = screen.getByTestId('row');
    expect(element).toHaveStyle({ flexDirection: 'row' });
  });

  it('passes props to Flex', () => {
    render(
      <Row
        mainAxisAlignment={MainAxisAlignment.Center}
        crossAxisAlignment={CrossAxisAlignment.Start}
        testID="row"
      />
    );
    const element = screen.getByTestId('row');
    expect(element).toHaveStyle({
      justifyContent: 'center',
      alignItems: 'flex-start',
    });
  });

  it('renders children in horizontal layout', () => {
    render(
      <Row testID="row">
        <div>A</div>
        <div>B</div>
        <div>C</div>
      </Row>
    );
    expect(screen.getByTestId('row')).toBeInTheDocument();
    expect(screen.getByText('A')).toBeInTheDocument();
    expect(screen.getByText('B')).toBeInTheDocument();
    expect(screen.getByText('C')).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = render(
      <Row>
        <p>Paragraph 1</p>
        <p>Paragraph 2</p>
      </Row>
    );
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});

// ============================================================================
// COLUMN COMPONENT TESTS
// ============================================================================

describe('Column Component', () => {
  it('renders as vertical flex', () => {
    render(<Column testID="column" />);
    const element = screen.getByTestId('column');
    expect(element).toHaveStyle({ flexDirection: 'column' });
  });

  it('passes props to Flex', () => {
    render(
      <Column
        mainAxisAlignment={MainAxisAlignment.End}
        crossAxisAlignment={CrossAxisAlignment.Stretch}
        testID="column"
      />
    );
    const element = screen.getByTestId('column');
    expect(element).toHaveStyle({
      justifyContent: 'flex-end',
      alignItems: 'stretch',
    });
  });

  it('renders children in vertical layout', () => {
    render(
      <Column testID="column">
        <div>Top</div>
        <div>Middle</div>
        <div>Bottom</div>
      </Column>
    );
    expect(screen.getByTestId('column')).toBeInTheDocument();
    expect(screen.getByText('Top')).toBeInTheDocument();
    expect(screen.getByText('Middle')).toBeInTheDocument();
    expect(screen.getByText('Bottom')).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = render(
      <Column>
        <h1>Heading</h1>
        <p>Content</p>
      </Column>
    );
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
