import React from 'react';
import { Box } from '@mui/material';

/**
 * MagicSpacer - React Spacer Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicSpacerProps {
  size?: number;
  direction?: 'horizontal' | 'vertical';
  className?: string;
}

export const MagicSpacer: React.FC<MagicSpacerProps> = ({
  size = 16,
  direction = 'vertical',
  className
}) => {
  return (
    <Box
      className={className}
      sx={{
        width: direction === 'horizontal' ? size : 0,
        height: direction === 'vertical' ? size : 0,
        flexShrink: 0
      }}
    />
  );
};

export default MagicSpacer;
