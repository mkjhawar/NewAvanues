import React from 'react';
import { Box } from '@mui/material';

/**
 * MagicImage - React Image Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ImageFit {
  CONTAIN = 'contain',
  COVER = 'cover',
  FILL = 'fill',
  NONE = 'none'
}

export interface MagicImageProps {
  source: string;
  alt?: string;
  fit?: ImageFit;
  width?: number;
  height?: number;
  onClick?: () => void;
  className?: string;
}

export const MagicImage: React.FC<MagicImageProps> = ({
  source,
  alt,
  fit = ImageFit.CONTAIN,
  width,
  height,
  onClick,
  className
}) => {
  return (
    <Box
      component="img"
      src={source}
      alt={alt}
      onClick={onClick}
      className={className}
      sx={{
        width: width ? `${width}px` : 'auto',
        height: height ? `${height}px` : 'auto',
        objectFit: fit,
        cursor: onClick ? 'pointer' : 'default'
      }}
    />
  );
};

export default MagicImage;
