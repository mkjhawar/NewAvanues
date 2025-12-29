import React from 'react';
import { Container as MuiContainer } from '@mui/material';
import { ContainerProps } from '../types';

export const Container: React.FC<ContainerProps> = ({
  children,
  maxWidth = 'lg',
  disableGutters = false,
  sx,
  ...props
}) => {
  return (
    <MuiContainer maxWidth={maxWidth} disableGutters={disableGutters} sx={sx} {...props}>
      {children}
    </MuiContainer>
  );
};
