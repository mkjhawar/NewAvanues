import React from 'react';
import { Divider, DividerProps } from '@mui/material';

/**
 * MagicDivider - React/Material-UI Divider Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum DividerOrientation {
  HORIZONTAL = 'horizontal',
  VERTICAL = 'vertical'
}

export interface MagicDividerProps {
  orientation?: DividerOrientation;
  thickness?: number;
  color?: string;
  inset?: boolean;
  className?: string;
}

export const MagicDivider: React.FC<MagicDividerProps> = ({
  orientation = DividerOrientation.HORIZONTAL,
  thickness = 1,
  color,
  inset = false,
  className
}) => {
  const dividerProps: DividerProps = {
    orientation: orientation as any,
    className,
    sx: {
      borderBottomWidth: orientation === DividerOrientation.HORIZONTAL ? thickness : undefined,
      borderRightWidth: orientation === DividerOrientation.VERTICAL ? thickness : undefined,
      borderColor: color,
      marginLeft: inset ? 2 : 0,
      marginRight: inset ? 2 : 0
    }
  };

  return <Divider {...dividerProps} />;
};

export default MagicDivider;
