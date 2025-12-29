import React from 'react';
import { Switch, FormControlLabel, SwitchProps } from '@mui/material';

/**
 * MagicSwitch - React/Material-UI Switch Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicSwitchProps {
  isOn: boolean;
  onChange: (isOn: boolean) => void;
  label?: string;
  enabled?: boolean;
  className?: string;
}

export const MagicSwitch: React.FC<MagicSwitchProps> = ({
  isOn,
  onChange,
  label,
  enabled = true,
  className
}) => {
  const switchProps: SwitchProps = {
    checked: isOn,
    onChange: (e) => onChange(e.target.checked),
    disabled: !enabled,
    className
  };

  const switchElement = <Switch {...switchProps} />;

  return label ? (
    <FormControlLabel
      control={switchElement}
      label={label}
      disabled={!enabled}
    />
  ) : switchElement;
};

export default MagicSwitch;
