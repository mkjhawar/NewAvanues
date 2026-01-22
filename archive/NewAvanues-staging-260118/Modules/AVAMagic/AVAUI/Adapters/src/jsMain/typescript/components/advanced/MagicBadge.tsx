import React from 'react';
import { Badge, BadgeProps } from '@mui/material';

/**
 * MagicBadge - React/Material-UI Badge Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicBadgeProps {
  children: React.ReactNode;
  content: string | number;
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
  className?: string;
}

export const MagicBadge: React.FC<MagicBadgeProps> = ({
  children,
  content,
  color = 'error',
  className
}) => {
  const badgeProps: BadgeProps = {
    badgeContent: content,
    color,
    className
  };

  return <Badge {...badgeProps}>{children}</Badge>;
};

export default MagicBadge;
