/**
 * ButtonBar - Flutter Parity Material Design
 *
 * A horizontal row of Material buttons, typically at the bottom of a dialog.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Box } from '@mui/material';
import type { ButtonBarProps } from './types';

export const ButtonBar: React.FC<ButtonBarProps> = ({
  children,
  alignment = 'end',
  mainAxisSize = 'max',
  layoutBehavior = 'padded',
}) => {
  const getJustifyContent = () => {
    switch (alignment) {
      case 'start':
        return 'flex-start';
      case 'end':
        return 'flex-end';
      case 'center':
        return 'center';
      case 'spaceBetween':
        return 'space-between';
      case 'spaceAround':
        return 'space-around';
      case 'spaceEvenly':
        return 'space-evenly';
      default:
        return 'flex-end';
    }
  };

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        justifyContent: getJustifyContent(),
        alignItems: 'center',
        gap: 1,
        width: mainAxisSize === 'max' ? '100%' : 'auto',
        padding: layoutBehavior === 'padded' ? 1 : 0,
        flexWrap: 'wrap',
      }}
    >
      {children}
    </Box>
  );
};

export default ButtonBar;
