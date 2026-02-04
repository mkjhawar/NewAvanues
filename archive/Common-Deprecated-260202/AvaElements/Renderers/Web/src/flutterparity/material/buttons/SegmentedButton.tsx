/**
 * SegmentedButton - Flutter Parity Material Design
 *
 * A Material Design segmented button for single or multi-selection.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { ToggleButtonGroup, ToggleButton } from '@mui/material';
import type { SegmentedButtonProps } from './types';

export const SegmentedButton: React.FC<SegmentedButtonProps> = ({
  segments,
  selected,
  onSelectionChanged,
  multiSelect = false,
  emptySelectionAllowed = false,
  enabled = true,
}) => {
  const handleChange = (_event: React.MouseEvent<HTMLElement>, newSelected: string | string[]) => {
    if (!enabled) return;

    // Handle empty selection
    if (!emptySelectionAllowed && (!newSelected || (Array.isArray(newSelected) && newSelected.length === 0))) {
      return;
    }

    const selectedArray = Array.isArray(newSelected) ? newSelected : [newSelected].filter(Boolean);
    onSelectionChanged?.(selectedArray);
  };

  return (
    <ToggleButtonGroup
      value={multiSelect ? selected : selected[0] || null}
      onChange={handleChange}
      exclusive={!multiSelect}
      disabled={!enabled}
      sx={{
        '& .MuiToggleButton-root': {
          textTransform: 'none',
          paddingX: 2,
          paddingY: 1,
          borderColor: 'divider',
          '&.Mui-selected': {
            backgroundColor: 'primary.light',
            color: 'primary.dark',
            '&:hover': {
              backgroundColor: 'primary.main',
            },
          },
        },
      }}
    >
      {segments.map((segment) => (
        <ToggleButton
          key={segment.value}
          value={segment.value}
          disabled={!segment.enabled}
          aria-label={segment.label || segment.value}
        >
          {segment.icon}
          {segment.label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
};

export default SegmentedButton;
