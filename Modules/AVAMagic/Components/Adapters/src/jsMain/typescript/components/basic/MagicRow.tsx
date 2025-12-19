import React from 'react';
import { Stack, StackProps } from '@mui/material';

/**
 * MagicRow - React Horizontal Stack Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum HorizontalAlignment {
  START = 'flex-start',
  CENTER = 'center',
  END = 'flex-end',
  SPACE_BETWEEN = 'space-between',
  SPACE_AROUND = 'space-around'
}

export enum VerticalAlignment {
  TOP = 'flex-start',
  CENTER = 'center',
  BOTTOM = 'flex-end'
}

export interface MagicRowProps {
  children: React.ReactNode;
  spacing?: number;
  horizontalAlignment?: HorizontalAlignment;
  verticalAlignment?: VerticalAlignment;
  wrap?: boolean;
  className?: string;
}

export const MagicRow: React.FC<MagicRowProps> = ({
  children,
  spacing = 2,
  horizontalAlignment = HorizontalAlignment.START,
  verticalAlignment = VerticalAlignment.CENTER,
  wrap = false,
  className
}) => {
  const stackProps: StackProps = {
    direction: 'row',
    spacing,
    className,
    sx: {
      justifyContent: horizontalAlignment,
      alignItems: verticalAlignment,
      flexWrap: wrap ? 'wrap' : 'nowrap'
    }
  };

  return <Stack {...stackProps}>{children}</Stack>;
};

export default MagicRow;
