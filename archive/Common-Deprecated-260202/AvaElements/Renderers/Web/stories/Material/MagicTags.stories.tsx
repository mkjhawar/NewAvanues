/**
 * Material Chips Stories
 *
 * Interactive documentation for all Flutter Parity Chip components:
 * - FilterChip
 * - ActionChip
 * - ChoiceChip
 * - InputChip
 *
 * @since 1.0.0 (Week 5-6: Web Testing Framework)
 */

import type { Meta, StoryObj } from '@storybook/react';
import { useState } from 'react';
import { MagicFilter } from '../../src/flutterparity/material/chips/FilterChip';
import { Stack } from '@mui/material';

// ============================================================================
// FILTERCHIP STORIES
// ============================================================================

const filterChipMeta: Meta<typeof FilterChip> = {
  title: 'Material/Chips/FilterChip',
  component: FilterChip,
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        component:
          'A FilterChip is a selectable chip that can be used to filter content. ' +
          'Shows a checkmark when selected, matching Flutter Material Design 3.',
      },
    },
  },
  argTypes: {
    label: {
      control: 'text',
      description: 'The text label displayed on the chip',
    },
    selected: {
      control: 'boolean',
      description: 'Whether the chip is selected',
    },
    enabled: {
      control: 'boolean',
      description: 'Whether the chip is enabled',
    },
    showCheckmark: {
      control: 'boolean',
      description: 'Whether to show checkmark when selected',
    },
    onSelected: {
      action: 'selected',
      description: 'Callback when chip is clicked',
    },
  },
};

export default filterChipMeta;
type FilterChipStory = StoryObj<typeof FilterChip>;

// ============================================================================
// BASIC EXAMPLES
// ============================================================================

export const FilterChipDefault: FilterChipStory = {
  args: {
    label: 'Filter',
    selected: false,
    enabled: true,
  },
};

export const FilterChipSelected: FilterChipStory = {
  args: {
    label: 'Active Filter',
    selected: true,
    enabled: true,
  },
};

export const FilterChipDisabled: FilterChipStory = {
  args: {
    label: 'Disabled Filter',
    selected: false,
    enabled: false,
  },
};

export const FilterChipSelectedDisabled: FilterChipStory = {
  args: {
    label: 'Selected & Disabled',
    selected: true,
    enabled: false,
  },
};

export const FilterChipNoCheckmark: FilterChipStory = {
  args: {
    label: 'No Checkmark',
    selected: true,
    showCheckmark: false,
  },
};

// ============================================================================
// INTERACTIVE EXAMPLES
// ============================================================================

export const FilterChipInteractive: FilterChipStory = {
  render: (args) => {
    const [selected, setSelected] = useState(false);
    return (
      <MagicFilter
        {...args}
        selected={selected}
        onSelected={setSelected}
      />
    );
  },
  args: {
    label: 'Click to Toggle',
    enabled: true,
  },
};

export const FilterChipGroup: FilterChipStory = {
  render: () => {
    const [filters, setFilters] = useState({
      electronics: false,
      clothing: false,
      books: false,
      sports: false,
    });

    const toggleFilter = (key: keyof typeof filters) => {
      setFilters((prev) => ({
        ...prev,
        [key]: !prev[key],
      }));
    };

    return (
      <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
        <MagicFilter
          label="Electronics"
          selected={filters.electronics}
          onSelected={() => toggleFilter('electronics')}
        />
        <MagicFilter
          label="Clothing"
          selected={filters.clothing}
          onSelected={() => toggleFilter('clothing')}
        />
        <MagicFilter
          label="Books"
          selected={filters.books}
          onSelected={() => toggleFilter('books')}
        />
        <MagicFilter
          label="Sports"
          selected={filters.sports}
          onSelected={() => toggleFilter('sports')}
        />
      </Stack>
    );
  },
};

// ============================================================================
// STATE MATRIX
// ============================================================================

export const FilterChipAllStates: FilterChipStory = {
  render: () => (
    <Stack spacing={2}>
      <div>
        <h4 style={{ margin: '0 0 8px 0' }}>Enabled States</h4>
        <Stack direction="row" spacing={1}>
          <MagicFilter label="Unselected" selected={false} enabled={true} />
          <MagicFilter label="Selected" selected={true} enabled={true} />
        </Stack>
      </div>

      <div>
        <h4 style={{ margin: '0 0 8px 0' }}>Disabled States</h4>
        <Stack direction="row" spacing={1}>
          <MagicFilter label="Unselected" selected={false} enabled={false} />
          <MagicFilter label="Selected" selected={true} enabled={false} />
        </Stack>
      </div>

      <div>
        <h4 style={{ margin: '0 0 8px 0' }}>Without Checkmark</h4>
        <Stack direction="row" spacing={1}>
          <MagicFilter label="Unselected" selected={false} showCheckmark={false} />
          <MagicFilter label="Selected" selected={true} showCheckmark={false} />
        </Stack>
      </div>
    </Stack>
  ),
};

// ============================================================================
// REAL-WORLD EXAMPLES
// ============================================================================

export const FilterChipSearchFilters: FilterChipStory = {
  render: () => {
    const [filters, setFilters] = useState({
      onSale: false,
      freeShipping: false,
      inStock: true,
      topRated: false,
    });

    const toggleFilter = (key: keyof typeof filters) => {
      setFilters((prev) => ({
        ...prev,
        [key]: !prev[key],
      }));
    };

    const activeCount = Object.values(filters).filter(Boolean).length;

    return (
      <div>
        <h3>Product Filters ({activeCount} active)</h3>
        <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
          <MagicFilter
            label="On Sale"
            selected={filters.onSale}
            onSelected={() => toggleFilter('onSale')}
          />
          <MagicFilter
            label="Free Shipping"
            selected={filters.freeShipping}
            onSelected={() => toggleFilter('freeShipping')}
          />
          <MagicFilter
            label="In Stock"
            selected={filters.inStock}
            onSelected={() => toggleFilter('inStock')}
          />
          <MagicFilter
            label="Top Rated (⭐ 4.5+)"
            selected={filters.topRated}
            onSelected={() => toggleFilter('topRated')}
          />
        </Stack>

        <div style={{ marginTop: '16px', fontSize: '14px', color: '#666' }}>
          Active filters:{' '}
          {Object.entries(filters)
            .filter(([, value]) => value)
            .map(([key]) => key)
            .join(', ') || 'None'}
        </div>
      </div>
    );
  },
};

export const FilterChipWithAvatar: FilterChipStory = {
  render: () => {
    const [selectedUser, setSelectedUser] = useState<string | null>(null);

    const users = [
      { id: 'user1', name: 'Alice', avatar: 'https://i.pravatar.cc/150?img=1' },
      { id: 'user2', name: 'Bob', avatar: 'https://i.pravatar.cc/150?img=2' },
      { id: 'user3', name: 'Charlie', avatar: 'https://i.pravatar.cc/150?img=3' },
    ];

    return (
      <div>
        <h3>Select a User</h3>
        <Stack direction="row" spacing={1}>
          {users.map((user) => (
            <MagicFilter
              key={user.id}
              label={user.name}
              avatar={user.avatar}
              selected={selectedUser === user.id}
              onSelected={() => setSelectedUser(user.id)}
            />
          ))}
        </Stack>
      </div>
    );
  },
};

// ============================================================================
// ACCESSIBILITY EXAMPLE
// ============================================================================

export const FilterChipAccessibility: FilterChipStory = {
  render: () => (
    <div>
      <h3>Accessibility Features</h3>
      <Stack spacing={2}>
        <div>
          <p style={{ fontSize: '14px', color: '#666' }}>
            All chips have proper ARIA attributes:
          </p>
          <ul style={{ fontSize: '14px', color: '#666' }}>
            <li>role="button"</li>
            <li>aria-pressed="true|false"</li>
            <li>aria-label with state information</li>
            <li>Keyboard accessible (Enter/Space)</li>
          </ul>
        </div>

        <Stack direction="row" spacing={1}>
          <MagicFilter
            label="Standard Filter"
            selected={false}
            accessibilityLabel="Filter products by standard criteria"
          />
          <MagicFilter
            label="Premium Only"
            selected={true}
            accessibilityLabel="Filter to show only premium products, currently active"
          />
        </Stack>
      </Stack>
    </div>
  ),
};

// ============================================================================
// PLAYGROUND
// ============================================================================

export const FilterChipPlayground: FilterChipStory = {
  args: {
    label: 'Customize Me',
    selected: false,
    enabled: true,
    showCheckmark: true,
  },
  render: (args) => {
    const [selected, setSelected] = useState(args.selected);
    return (
      <MagicFilter
        {...args}
        selected={selected}
        onSelected={setSelected}
      />
    );
  },
};

// ============================================================================
// PERFORMANCE TEST
// ============================================================================

export const FilterChipPerformance: FilterChipStory = {
  render: () => {
    const [selected, setSelected] = useState<Record<number, boolean>>({});

    const chips = Array.from({ length: 100 }, (_, i) => i);

    return (
      <div>
        <h3>100 FilterChips (Performance Test)</h3>
        <Stack direction="row" spacing={0.5} flexWrap="wrap" gap={0.5}>
          {chips.map((i) => (
            <MagicFilter
              key={i}
              label={`Filter ${i + 1}`}
              selected={selected[i] || false}
              onSelected={(isSelected) => {
                setSelected((prev) => ({
                  ...prev,
                  [i]: isSelected,
                }));
              }}
            />
          ))}
        </Stack>
      </div>
    );
  },
};
