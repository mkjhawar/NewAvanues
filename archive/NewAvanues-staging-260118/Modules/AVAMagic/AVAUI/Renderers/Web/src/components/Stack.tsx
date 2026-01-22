import React from 'react';
import { Stack as MuiStack } from '@mui/material';

export interface StackProps {
  children?: React.ReactNode;
  direction?: 'row' | 'row-reverse' | 'column' | 'column-reverse';
  spacing?: number;
  alignItems?: 'flex-start' | 'center' | 'flex-end' | 'stretch' | 'baseline';
  justifyContent?: 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly';
  divider?: React.ReactNode;
  useFlexGap?: boolean;
  flexWrap?: 'nowrap' | 'wrap' | 'wrap-reverse';
}

export const Stack: React.FC<StackProps> = ({
  children,
  direction = 'column',
  spacing = 0,
  alignItems,
  justifyContent,
  divider,
  useFlexGap = false,
  flexWrap,
}) => {
  return (
    <MuiStack
      direction={direction}
      spacing={spacing}
      alignItems={alignItems}
      justifyContent={justifyContent}
      divider={divider}
      useFlexGap={useFlexGap}
      flexWrap={flexWrap}
    >
      {children}
    </MuiStack>
  );
};

export default Stack;
