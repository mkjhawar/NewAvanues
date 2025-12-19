import React from 'react';
import { Box, BoxProps } from '@mui/material';

/**
 * Image - Responsive Image Component
 *
 * A flexible image component with various fit modes and accessibility support.
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum ImageFit {
  /** Scale to fit container while maintaining aspect ratio */
  CONTAIN = 'contain',
  /** Scale to cover container while maintaining aspect ratio (may crop) */
  COVER = 'cover',
  /** Stretch to fill container (may distort) */
  FILL = 'fill',
  /** Do not scale */
  NONE = 'none',
  /** Scale down to fit (never scale up) */
  SCALE_DOWN = 'scale-down'
}

export interface ImageProps {
  /** Image source URL */
  src: string;
  /** Alternative text for accessibility */
  alt: string;
  /** How the image should fit within its container */
  fit?: ImageFit;
  /** Width in pixels or CSS value */
  width?: number | string;
  /** Height in pixels or CSS value */
  height?: number | string;
  /** Whether the image is clickable */
  clickable?: boolean;
  /** Click handler */
  onClick?: () => void;
  /** Border radius in pixels */
  borderRadius?: number;
  /** Whether to show loading placeholder */
  loading?: 'eager' | 'lazy';
  /** Error handler */
  onError?: (event: React.SyntheticEvent<HTMLImageElement>) => void;
  /** Load handler */
  onLoad?: (event: React.SyntheticEvent<HTMLImageElement>) => void;
  /** Additional CSS class name */
  className?: string;
  /** Custom styles */
  sx?: BoxProps['sx'];
}

/**
 * Image component with responsive sizing and accessibility
 *
 * @example
 * ```tsx
 * // Basic image
 * <Image src="/logo.png" alt="Company Logo" />
 *
 * // Fixed size with cover fit
 * <Image
 *   src="/banner.jpg"
 *   alt="Banner"
 *   width={800}
 *   height={400}
 *   fit="cover"
 * />
 *
 * // Clickable rounded image
 * <Image
 *   src="/profile.jpg"
 *   alt="User Profile"
 *   width={100}
 *   height={100}
 *   borderRadius={50}
 *   clickable
 *   onClick={() => console.log('Profile clicked')}
 * />
 *
 * // Lazy loaded image
 * <Image
 *   src="/large-image.jpg"
 *   alt="Large Image"
 *   loading="lazy"
 * />
 * ```
 */
export const Image: React.FC<ImageProps> = ({
  src,
  alt,
  fit = ImageFit.CONTAIN,
  width = 'auto',
  height = 'auto',
  clickable = false,
  onClick,
  borderRadius = 0,
  loading = 'lazy',
  onError,
  onLoad,
  className,
  sx
}) => {
  const customSx: BoxProps['sx'] = {
    width: typeof width === 'number' ? `${width}px` : width,
    height: typeof height === 'number' ? `${height}px` : height,
    objectFit: fit,
    borderRadius: borderRadius ? `${borderRadius}px` : 0,
    cursor: clickable || onClick ? 'pointer' : 'default',
    display: 'block',
    maxWidth: '100%',
    ...sx
  };

  return (
    <Box
      component="img"
      src={src}
      alt={alt}
      loading={loading}
      onClick={onClick}
      onError={onError}
      onLoad={onLoad}
      className={className}
      sx={customSx}
    />
  );
};

export default Image;
