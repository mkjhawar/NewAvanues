import React from 'react';
import { Checkbox, FormControlLabel, CheckboxProps } from '@mui/material';
import { IndeterminateCheckBox } from '@mui/icons-material';

/**
 * MagicCheckbox - React/Material-UI Checkbox Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum CheckboxState {
  CHECKED = 'checked',
  UNCHECKED = 'unchecked',
  INDETERMINATE = 'indeterminate'
}

export interface MagicCheckboxProps {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label?: string;
  enabled?: boolean;
  state?: CheckboxState;
  className?: string;
}

export const MagicCheckbox: React.FC<MagicCheckboxProps> = ({
  checked,
  onCheckedChange,
  label,
  enabled = true,
  state = CheckboxState.UNCHECKED,
  className
}) => {
  const checkboxProps: CheckboxProps = {
    checked: state === CheckboxState.CHECKED || checked,
    indeterminate: state === CheckboxState.INDETERMINATE,
    onChange: (e) => onCheckedChange(e.target.checked),
    disabled: !enabled,
    className
  };

  const checkbox = <Checkbox {...checkboxProps} />;

  return label ? (
    <FormControlLabel
      control={checkbox}
      label={label}
      disabled={!enabled}
    />
  ) : checkbox;
};

export default MagicCheckbox;
