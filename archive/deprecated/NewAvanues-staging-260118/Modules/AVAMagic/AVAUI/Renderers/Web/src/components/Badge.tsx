import React from 'react';
import { Badge as MuiBadge, BadgeProps as MuiBadgeProps } from '@mui/material';

export interface BadgeProps {
  content?: number | string;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  variant?: 'standard' | 'dot';
  max?: number;
  invisible?: boolean;
  overlap?: 'rectangular' | 'circular';
  children: React.ReactNode;
}

export const Badge: React.FC<BadgeProps> = ({
  content,
  color = 'primary',
  variant = 'standard',
  max = 99,
  invisible = false,
  overlap = 'rectangular',
  children,
}) => {
  return (
    <MuiBadge
      badgeContent={content}
      color={color}
      variant={variant}
      max={max}
      invisible={invisible}
      overlap={overlap}
    >
      {children}
    </MuiBadge>
  );
};

export default Badge;
