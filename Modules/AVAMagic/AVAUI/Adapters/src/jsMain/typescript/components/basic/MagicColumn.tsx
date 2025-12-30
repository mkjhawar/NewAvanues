import React from 'react';
import { Stack, StackProps } from '@mui/material';

/**
 * MagicColumn - React Vertical Stack Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum HorizontalAlignment {
  START = 'flex-start',
  CENTER = 'center',
  END = 'flex-end'
}

export enum VerticalAlignment {
  TOP = 'flex-start',
  CENTER = 'center',
  BOTTOM = 'flex-end',
  SPACE_BETWEEN = 'space-between',
  SPACE_AROUND = 'space-around'
}

export interface MagicColumnProps {
  children: React.ReactNode;
  spacing?: number;
  horizontalAlignment?: HorizontalAlignment;
  verticalAlignment?: VerticalAlignment;
  className?: string;
}

export const MagicColumn: React.FC<MagicColumnProps> = ({
  children,
  spacing = 2,
  horizontalAlignment = HorizontalAlignment.START,
  verticalAlignment = VerticalAlignment.TOP,
  className
}) => {
  const stackProps: StackProps = {
    direction: 'column',
    spacing,
    className,
    sx: {
      alignItems: horizontalAlignment,
      justifyContent: verticalAlignment
    }
  };

  return <Stack {...stackProps}>{children}</Stack>;
};

export default MagicColumn;
