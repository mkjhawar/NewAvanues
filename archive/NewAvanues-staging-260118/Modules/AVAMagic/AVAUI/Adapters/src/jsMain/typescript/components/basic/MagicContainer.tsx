import React from 'react';
import { Box, BoxProps } from '@mui/material';

/**
 * MagicContainer - React Container Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export interface MagicContainerProps {
  children: React.ReactNode;
  padding?: number;
  maxWidth?: number | string;
  centered?: boolean;
  className?: string;
}

export const MagicContainer: React.FC<MagicContainerProps> = ({
  children,
  padding = 2,
  maxWidth,
  centered = false,
  className
}) => {
  const boxProps: BoxProps = {
    className,
    sx: {
      padding,
      maxWidth: maxWidth || '100%',
      margin: centered ? '0 auto' : undefined,
      width: '100%'
    }
  };

  return <Box {...boxProps}>{children}</Box>;
};

export default MagicContainer;
