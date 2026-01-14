import React, { useState } from 'react';
import { FormControlLabel, Switch as MuiSwitch } from '@mui/material';
import { SwitchProps } from '../types';

export const Switch: React.FC<SwitchProps> = ({
  id,
  label,
  checked: initialChecked = false,
  onChange,
  disabled = false,
  color = 'primary',
  ...props
}) => {
  const [checked, setChecked] = useState(initialChecked);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newChecked = e.target.checked;
    setChecked(newChecked);
    onChange?.(newChecked);
  };

  const switchControl = (
    <MuiSwitch
      id={id}
      checked={checked}
      onChange={handleChange}
      disabled={disabled}
      color={color}
      {...props}
    />
  );

  if (label) {
    return <FormControlLabel control={switchControl} label={label} />;
  }

  return switchControl;
};
