/**
 * Container Component Stories
 *
 * Interactive documentation and visual testing for Container component.
 * Demonstrates all props, variants, and use cases.
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import type { Meta, StoryObj } from '@storybook/react';
import { Container } from '../../src/flutterparity/layout/Container';
import { Alignment, BoxDecoration } from '../../src/flutterparity/layout/types';

const meta: Meta<typeof Container> = {
  title: 'Layout/Container',
  component: Container,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          'A Flutter Parity Container component that combines common styling widgets. ' +
          'Provides padding, margins, borders, background color, size constraints, and alignment.',
      },
    },
  },
  argTypes: {
    width: {
      control: 'number',
      description: 'Width in pixels or string (%, vh, etc.)',
      table: {
        type: { summary: 'number | string' },
        defaultValue: { summary: 'undefined' },
      },
    },
    height: {
      control: 'number',
      description: 'Height in pixels or string (%, vh, etc.)',
    },
    padding: {
      control: 'number',
      description: 'Uniform padding or object with specific sides',
    },
    margin: {
      control: 'number',
      description: 'Uniform margin or object with specific sides',
    },
    alignment: {
      control: 'select',
      options: Object.values(Alignment),
      description: 'Alignment of child within container',
    },
  },
};

export default meta;
type Story = StoryObj<typeof Container>;

// ============================================================================
// BASIC EXAMPLES
// ============================================================================

export const Default: Story = {
  args: {
    width: 200,
    height: 200,
    decoration: {
      color: '#f5f5f5',
      border: { width: 1, color: '#ccc', style: 'solid' },
    },
    child: <div>Default Container</div>,
  },
};

export const WithPadding: Story = {
  args: {
    width: 300,
    padding: 24,
    decoration: {
      color: '#e3f2fd',
      borderRadius: 8,
    },
    child: <div>Container with padding</div>,
  },
};

export const WithMargin: Story = {
  args: {
    width: 250,
    margin: 16,
    decoration: {
      color: '#fff3e0',
      border: { width: 2, color: '#ff9800', style: 'solid' },
    },
    child: <div>Container with margin</div>,
  },
};

// ============================================================================
// ALIGNMENT EXAMPLES
// ============================================================================

export const AlignCenter: Story = {
  args: {
    width: 300,
    height: 200,
    alignment: Alignment.Center,
    decoration: {
      color: '#f0f4ff',
      border: { width: 1, color: '#2196f3', style: 'dashed' },
    },
    child: <div style={{ background: '#2196f3', color: 'white', padding: '8px' }}>Centered</div>,
  },
};

export const AlignTopLeft: Story = {
  args: {
    width: 300,
    height: 200,
    alignment: Alignment.TopLeft,
    decoration: {
      color: '#e8f5e9',
      border: { width: 1, color: '#4caf50', style: 'solid' },
    },
    child: <div style={{ background: '#4caf50', color: 'white', padding: '8px' }}>Top Left</div>,
  },
};

export const AlignBottomRight: Story = {
  args: {
    width: 300,
    height: 200,
    alignment: Alignment.BottomRight,
    decoration: {
      color: '#fce4ec',
      border: { width: 1, color: '#e91e63', style: 'solid' },
    },
    child: <div style={{ background: '#e91e63', color: 'white', padding: '8px' }}>Bottom Right</div>,
  },
};

// ============================================================================
// DECORATION EXAMPLES
// ============================================================================

export const WithBorder: Story = {
  args: {
    width: 300,
    height: 150,
    decoration: {
      color: '#ffffff',
      border: { width: 3, color: '#9c27b0', style: 'solid' },
      borderRadius: 12,
    },
    child: <div>Container with rounded border</div>,
  },
};

export const WithShadow: Story = {
  args: {
    width: 300,
    height: 150,
    decoration: {
      color: '#ffffff',
      borderRadius: 8,
      boxShadow: {
        color: 'rgba(0, 0, 0, 0.2)',
        blurRadius: 16,
        spreadRadius: 4,
        offset: { dx: 0, dy: 4 },
      },
    },
    child: <div>Container with shadow</div>,
  },
};

export const ColorfulBackground: Story = {
  args: {
    width: 300,
    height: 200,
    decoration: {
      color: '#673ab7',
      borderRadius: 16,
    },
    alignment: Alignment.Center,
    child: <div style={{ color: 'white', fontSize: '18px', fontWeight: 'bold' }}>Colorful!</div>,
  },
};

// ============================================================================
// CONSTRAINTS EXAMPLES
// ============================================================================

export const WithConstraints: Story = {
  args: {
    constraints: {
      minWidth: 200,
      maxWidth: 400,
      minHeight: 100,
      maxHeight: 300,
    },
    decoration: {
      color: '#fff9c4',
      border: { width: 1, color: '#fbc02d', style: 'solid' },
    },
    child: <div>Container with min/max constraints</div>,
  },
};

// ============================================================================
// COMPLEX EXAMPLES
// ============================================================================

export const CardStyle: Story = {
  args: {
    width: 350,
    padding: 20,
    margin: 16,
    decoration: {
      color: '#ffffff',
      borderRadius: 12,
      boxShadow: {
        color: 'rgba(0, 0, 0, 0.1)',
        blurRadius: 10,
        spreadRadius: 2,
        offset: { dx: 0, dy: 2 },
      },
    },
    child: (
      <div>
        <h3 style={{ margin: '0 0 12px 0' }}>Card Title</h3>
        <p style={{ margin: 0, color: '#666' }}>
          This is a card-style container with shadow, border radius, and padding.
        </p>
      </div>
    ),
  },
};

export const ButtonStyle: Story = {
  args: {
    width: 200,
    padding: 16,
    decoration: {
      color: '#2196f3',
      borderRadius: 8,
    },
    alignment: Alignment.Center,
    child: <div style={{ color: 'white', fontWeight: 'bold', cursor: 'pointer' }}>Click Me</div>,
  },
};

export const NestedContainers: Story = {
  render: () => (
    <Container
      width={400}
      height={300}
      padding={16}
      decoration={{
        color: '#f5f5f5',
        border: { width: 2, color: '#333', style: 'solid' },
      }}
      alignment={Alignment.Center}
    >
      <Container
        width={200}
        height={150}
        decoration={{
          color: '#2196f3',
          borderRadius: 8,
        }}
        alignment={Alignment.Center}
      >
        <Container
          width={100}
          height={75}
          decoration={{
            color: '#ffffff',
            borderRadius: 4,
          }}
          alignment={Alignment.Center}
        >
          <div style={{ fontSize: '12px' }}>Nested!</div>
        </Container>
      </Container>
    </Container>
  ),
};

// ============================================================================
// INTERACTIVE PLAYGROUND
// ============================================================================

export const Playground: Story = {
  args: {
    width: 300,
    height: 200,
    padding: 16,
    margin: 8,
    decoration: {
      color: '#e3f2fd',
      border: { width: 1, color: '#2196f3', style: 'solid' },
      borderRadius: 8,
      boxShadow: {
        color: 'rgba(33, 150, 243, 0.2)',
        blurRadius: 8,
        spreadRadius: 1,
        offset: { dx: 0, dy: 2 },
      },
    },
    alignment: Alignment.Center,
    child: <div>Customize me!</div>,
  },
};
