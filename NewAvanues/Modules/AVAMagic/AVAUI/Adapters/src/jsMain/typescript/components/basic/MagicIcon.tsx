import React from 'react';
import { SvgIcon, SvgIconProps } from '@mui/material';
import * as MuiIcons from '@mui/icons-material';

/**
 * MagicIcon - React/Material-UI Icon Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum IconSize {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large'
}

export interface MagicIconProps {
  name: string;
  size?: IconSize;
  color?: string;
  className?: string;
}

export const MagicIcon: React.FC<MagicIconProps> = ({
  name,
  size = IconSize.MEDIUM,
  color,
  className
}) => {
  const IconComponent = (MuiIcons as any)[name];

  if (!IconComponent) {
    return null;
  }

  const iconProps: SvgIconProps = {
    fontSize: size as any,
    className,
    sx: { color }
  };

  return <IconComponent {...iconProps} />;
};

export default MagicIcon;
