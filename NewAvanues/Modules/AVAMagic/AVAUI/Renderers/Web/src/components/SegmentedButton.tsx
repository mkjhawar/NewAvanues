import React from 'react';
import { ToggleButton, ToggleButtonGroup } from '@mui/material';

export interface SegmentedButtonProps {
  segments: { label: string; value: string; disabled?: boolean }[];
  value?: string | string[];
  onChange?: (value: string | string[]) => void;
  exclusive?: boolean;
  disabled?: boolean;
  size?: 'small' | 'medium' | 'large';
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' | 'standard';
  orientation?: 'horizontal' | 'vertical';
}

export const SegmentedButton: React.FC<SegmentedButtonProps> = ({
  segments,
  value,
  onChange,
  exclusive = true,
  disabled = false,
  size = 'medium',
  color = 'primary',
  orientation = 'horizontal',
}) => {
  const handleChange = (_: React.MouseEvent<HTMLElement>, newValue: string | string[] | null) => {
    if (newValue !== null) {
      onChange?.(newValue);
    }
  };

  return (
    <ToggleButtonGroup
      value={value}
      exclusive={exclusive}
      onChange={handleChange}
      disabled={disabled}
      size={size}
      color={color}
      orientation={orientation}
    >
      {segments.map((segment) => (
        <ToggleButton
          key={segment.value}
          value={segment.value}
          disabled={segment.disabled}
        >
          {segment.label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
};

export default SegmentedButton;
