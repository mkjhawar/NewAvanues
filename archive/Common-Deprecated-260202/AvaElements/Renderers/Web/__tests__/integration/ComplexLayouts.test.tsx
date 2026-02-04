/**
 * Complex Layout Integration Tests
 *
 * Tests combinations of layout components working together.
 * Validates real-world usage patterns and edge cases.
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';
import { Container } from '../../src/flutterparity/layout/Container';
import { Row, Column } from '../../src/flutterparity/layout/Flex';
import { MagicFilter } from '../../src/flutterparity/material/chips/FilterChip';
import { Alignment, MainAxisAlignment, CrossAxisAlignment } from '../../src/flutterparity/layout/types';

describe('Complex Layout Integration Tests', () => {
  // ============================================================================
  // NESTED LAYOUTS
  // ============================================================================

  it('renders nested Containers correctly', () => {
    render(
      <Container testID="outer" width={400} height={300}>
        <Container testID="middle" width={300} height={200}>
          <Container testID="inner" width={200} height={100}>
            <div>Nested Content</div>
          </Container>
        </Container>
      </Container>
    );

    expect(screen.getByTestId('outer')).toBeInTheDocument();
    expect(screen.getByTestId('middle')).toBeInTheDocument();
    expect(screen.getByTestId('inner')).toBeInTheDocument();
    expect(screen.getByText('Nested Content')).toBeInTheDocument();
  });

  it('renders Row within Container', () => {
    render(
      <Container testID="container" width={400} padding={16}>
        <Row testID="row" mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
          <div>Item 1</div>
          <div>Item 2</div>
          <div>Item 3</div>
        </Row>
      </Container>
    );

    const container = screen.getByTestId('container');
    const row = screen.getByTestId('row');

    expect(container).toContainElement(row);
    expect(screen.getByText('Item 1')).toBeInTheDocument();
    expect(screen.getByText('Item 2')).toBeInTheDocument();
    expect(screen.getByText('Item 3')).toBeInTheDocument();
  });

  it('renders Column within Container', () => {
    render(
      <Container testID="container" width={300} height={400}>
        <Column testID="column" mainAxisAlignment={MainAxisAlignment.Center}>
          <div>Top</div>
          <div>Middle</div>
          <div>Bottom</div>
        </Column>
      </Container>
    );

    const container = screen.getByTestId('container');
    const column = screen.getByTestId('column');

    expect(container).toContainElement(column);
    expect(screen.getByText('Top')).toBeInTheDocument();
    expect(screen.getByText('Middle')).toBeInTheDocument();
    expect(screen.getByText('Bottom')).toBeInTheDocument();
  });

  // ============================================================================
  // CARD-LIKE LAYOUTS
  // ============================================================================

  it('renders card layout with header, body, and footer', () => {
    render(
      <Container
        testID="card"
        width={400}
        padding={0}
        decoration={{
          color: '#ffffff',
          borderRadius: 12,
          boxShadow: {
            color: 'rgba(0, 0, 0, 0.1)',
            blurRadius: 8,
            spreadRadius: 2,
            offset: { dx: 0, dy: 2 },
          },
        }}
      >
        <Column mainAxisAlignment={MainAxisAlignment.Start}>
          {/* Header */}
          <Container testID="header" padding={16} decoration={{ color: '#f5f5f5' }}>
            <div>Card Header</div>
          </Container>

          {/* Body */}
          <Container testID="body" padding={16}>
            <div>Card Body Content</div>
          </Container>

          {/* Footer */}
          <Container testID="footer" padding={16} decoration={{ color: '#f5f5f5' }}>
            <Row mainAxisAlignment={MainAxisAlignment.End}>
              <div>Cancel</div>
              <div>Submit</div>
            </Row>
          </Container>
        </Column>
      </Container>
    );

    expect(screen.getByTestId('card')).toBeInTheDocument();
    expect(screen.getByTestId('header')).toBeInTheDocument();
    expect(screen.getByTestId('body')).toBeInTheDocument();
    expect(screen.getByTestId('footer')).toBeInTheDocument();

    expect(screen.getByText('Card Header')).toBeInTheDocument();
    expect(screen.getByText('Card Body Content')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
    expect(screen.getByText('Submit')).toBeInTheDocument();
  });

  // ============================================================================
  // FILTER BAR LAYOUT
  // ============================================================================

  it('renders filter bar with chips in Row layout', () => {
    render(
      <Container testID="filter-bar" width="100%" padding={16}>
        <Row mainAxisAlignment={MainAxisAlignment.Start} testID="chip-row">
          <FilterChip label="Filter 1" selected={false} />
          <FilterChip label="Filter 2" selected={true} />
          <FilterChip label="Filter 3" selected={false} />
        </Row>
      </Container>
    );

    expect(screen.getByTestId('filter-bar')).toBeInTheDocument();
    expect(screen.getByTestId('chip-row')).toBeInTheDocument();
    expect(screen.getByText('Filter 1')).toBeInTheDocument();
    expect(screen.getByText('Filter 2')).toBeInTheDocument();
    expect(screen.getByText('Filter 3')).toBeInTheDocument();
  });

  // ============================================================================
  // GRID-LIKE LAYOUTS (using Row/Column)
  // ============================================================================

  it('renders 2x2 grid layout', () => {
    render(
      <Container testID="grid" width={400} height={400}>
        <Column mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
          {/* Row 1 */}
          <Row testID="row1" mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
            <Container testID="cell-1-1" width={180} height={180} decoration={{ color: '#e3f2fd' }}>
              <div>Cell 1,1</div>
            </Container>
            <Container testID="cell-1-2" width={180} height={180} decoration={{ color: '#f3e5f5' }}>
              <div>Cell 1,2</div>
            </Container>
          </Row>

          {/* Row 2 */}
          <Row testID="row2" mainAxisAlignment={MainAxisAlignment.SpaceBetween}>
            <Container testID="cell-2-1" width={180} height={180} decoration={{ color: '#fff3e0' }}>
              <div>Cell 2,1</div>
            </Container>
            <Container testID="cell-2-2" width={180} height={180} decoration={{ color: '#e8f5e9' }}>
              <div>Cell 2,2</div>
            </Container>
          </Row>
        </Column>
      </Container>
    );

    expect(screen.getByTestId('grid')).toBeInTheDocument();
    expect(screen.getByTestId('cell-1-1')).toBeInTheDocument();
    expect(screen.getByTestId('cell-1-2')).toBeInTheDocument();
    expect(screen.getByTestId('cell-2-1')).toBeInTheDocument();
    expect(screen.getByTestId('cell-2-2')).toBeInTheDocument();

    expect(screen.getByText('Cell 1,1')).toBeInTheDocument();
    expect(screen.getByText('Cell 1,2')).toBeInTheDocument();
    expect(screen.getByText('Cell 2,1')).toBeInTheDocument();
    expect(screen.getByText('Cell 2,2')).toBeInTheDocument();
  });

  // ============================================================================
  // SIDEBAR LAYOUT
  // ============================================================================

  it('renders sidebar layout (left sidebar + main content)', () => {
    render(
      <Container testID="app" width="100%" height="100vh">
        <Row testID="layout" mainAxisAlignment={MainAxisAlignment.Start}>
          {/* Sidebar */}
          <Container
            testID="sidebar"
            width={250}
            height="100%"
            decoration={{ color: '#263238' }}
          >
            <Column mainAxisAlignment={MainAxisAlignment.Start}>
              <div style={{ color: 'white' }}>Menu Item 1</div>
              <div style={{ color: 'white' }}>Menu Item 2</div>
              <div style={{ color: 'white' }}>Menu Item 3</div>
            </Column>
          </Container>

          {/* Main Content */}
          <Container testID="main-content" width="100%" padding={24}>
            <div>Main Content Area</div>
          </Container>
        </Row>
      </Container>
    );

    expect(screen.getByTestId('app')).toBeInTheDocument();
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('main-content')).toBeInTheDocument();

    expect(screen.getByText('Menu Item 1')).toBeInTheDocument();
    expect(screen.getByText('Main Content Area')).toBeInTheDocument();
  });

  // ============================================================================
  // CENTERED CONTENT LAYOUT
  // ============================================================================

  it('renders centered content layout', () => {
    render(
      <Container
        testID="page"
        width="100%"
        height="100vh"
        alignment={Alignment.Center}
        decoration={{ color: '#f5f5f5' }}
      >
        <Container
          testID="centered-card"
          width={400}
          padding={32}
          decoration={{
            color: '#ffffff',
            borderRadius: 12,
            boxShadow: {
              color: 'rgba(0, 0, 0, 0.15)',
              blurRadius: 12,
              spreadRadius: 2,
              offset: { dx: 0, dy: 4 },
            },
          }}
        >
          <Column mainAxisAlignment={MainAxisAlignment.Center}>
            <h2 style={{ margin: '0 0 16px 0' }}>Login</h2>
            <div>Email input</div>
            <div>Password input</div>
            <div>Submit button</div>
          </Column>
        </Container>
      </Container>
    );

    expect(screen.getByTestId('page')).toBeInTheDocument();
    expect(screen.getByTestId('centered-card')).toBeInTheDocument();
    expect(screen.getByText('Login')).toBeInTheDocument();
  });

  // ============================================================================
  // ACCESSIBILITY TESTS
  // ============================================================================

  it('complex card layout has no accessibility violations', async () => {
    const { container } = render(
      <Container
        width={400}
        padding={16}
        decoration={{
          color: '#ffffff',
          borderRadius: 8,
        }}
      >
        <Column mainAxisAlignment={MainAxisAlignment.Start}>
          <h3>Product Details</h3>
          <p>Product description goes here.</p>

          <Row mainAxisAlignment={MainAxisAlignment.Start}>
            <FilterChip label="Sale" selected={true} />
            <FilterChip label="New" selected={false} />
          </Row>

          <Row mainAxisAlignment={MainAxisAlignment.End}>
            <button>Add to Cart</button>
          </Row>
        </Column>
      </Container>
    );

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  // ============================================================================
  // PERFORMANCE TESTS
  // ============================================================================

  it('renders deeply nested layout efficiently', () => {
    const start = performance.now();

    render(
      <Container>
        <Column>
          {Array.from({ length: 10 }, (_, i) => (
            <Row key={i}>
              {Array.from({ length: 5 }, (_, j) => (
                <Container key={j} width={50} height={50}>
                  <div>{`${i}-${j}`}</div>
                </Container>
              ))}
            </Row>
          ))}
        </Column>
      </Container>
    );

    const end = performance.now();
    const renderTime = end - start;

    // Should render in less than 100ms
    expect(renderTime).toBeLessThan(100);
  });
});
