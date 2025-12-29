import React from 'react';
import { Card as MuiCard, CardContent } from '@mui/material';
import { CardProps } from '../types';

export const Card: React.FC<CardProps> = ({
  children,
  elevation = 1,
  variant = 'elevation',
  sx,
  ...props
}) => {
  return (
    <MuiCard elevation={elevation} variant={variant} sx={sx} {...props}>
      <CardContent>{children}</CardContent>
    </MuiCard>
  );
};
