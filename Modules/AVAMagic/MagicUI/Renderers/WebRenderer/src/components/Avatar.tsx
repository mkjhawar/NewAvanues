import React from 'react';
import { Avatar as MuiAvatar } from '@mui/material';
import { AvatarProps } from '../types';

export const Avatar: React.FC<AvatarProps> = ({
  initials,
  src,
  alt,
  size = 40,
  variant = 'circular',
  sx,
  ...props
}) => {
  return (
    <MuiAvatar
      src={src}
      alt={alt}
      variant={variant}
      sx={{
        width: size,
        height: size,
        ...sx
      }}
      {...props}
    >
      {!src && initials}
    </MuiAvatar>
  );
};
