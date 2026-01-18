import React from 'react';
import * as MuiIcons from '@mui/icons-material';
import { IconProps } from '../types';

export const Icon: React.FC<IconProps> = ({
  name = 'Home',
  size = 'medium',
  color = 'inherit',
  sx,
  ...props
}) => {
  // Dynamically get icon component
  const IconComponent = (MuiIcons as any)[name] || MuiIcons.Help;

  return <IconComponent fontSize={size} color={color} sx={sx} {...props} />;
};
