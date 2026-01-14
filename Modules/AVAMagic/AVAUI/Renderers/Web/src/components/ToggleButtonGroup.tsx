import React from 'react';
import {
  ToggleButton,
  ToggleButtonGroup as MuiToggleButtonGroup,
  Icon,
  Box,
} from '@mui/material';

export interface ToggleButtonGroupProps {
  options: Array<{ icon?: string; label: string }>;
  selectedIndices?: number[];
  multiSelect?: boolean;
  orientation?: 'horizontal' | 'vertical';
  disabled?: boolean;
  onChange?: (indices: number[]) => void;
}

export const ToggleButtonGroup: React.FC<ToggleButtonGroupProps> = ({
  options,
  selectedIndices = [],
  multiSelect = false,
  orientation = 'horizontal',
  disabled = false,
  onChange,
}) => {
  const handleChange = (
    _event: React.MouseEvent<HTMLElement>,
    newValue: number | number[] | null
  ) => {
    if (multiSelect) {
      onChange?.(newValue as number[] || []);
    } else {
      onChange?.(newValue !== null ? [newValue as number] : []);
    }
  };

  const value = multiSelect
    ? selectedIndices
    : selectedIndices.length > 0
    ? selectedIndices[0]
    : null;

  return (
    <MuiToggleButtonGroup
      value={value}
      exclusive={!multiSelect}
      onChange={handleChange}
      orientation={orientation}
      disabled={disabled}
    >
      {options.map((option, index) => (
        <ToggleButton key={index} value={index}>
          {option.icon && option.icon !== option.label && (
            <Icon sx={{ mr: option.label ? 0.5 : 0 }}>{option.icon}</Icon>
          )}
          {option.label}
        </ToggleButton>
      ))}
    </MuiToggleButtonGroup>
  );
};

export default ToggleButtonGroup;
