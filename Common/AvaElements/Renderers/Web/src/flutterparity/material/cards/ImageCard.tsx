/**
 * ImageCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card with prominent image and optional overlay text/actions.
 * Commonly used in galleries, portfolios, and visual content grids.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Card, CardMedia, Typography, Box, Button, Icon } from '@mui/material';

export type ImageCardOverlayPosition = 'top' | 'center' | 'bottom';
export type ImageCardFit = 'cover' | 'contain' | 'fill' | 'none';

export interface ImageCardProps {
  imageUrl: string;
  title?: string;
  subtitle?: string;
  overlayPosition?: ImageCardOverlayPosition;
  showGradient?: boolean;
  gradientColor?: string;
  aspectRatio?: number;
  fit?: ImageCardFit;
  actionIcon?: string;
  actionText?: string;
  contentDescription?: string;
  onPressed?: () => void;
  onActionPressed?: () => void;
  className?: string;
}

export const ImageCard: React.FC<ImageCardProps> = ({
  imageUrl,
  title,
  subtitle,
  overlayPosition = 'bottom',
  showGradient = true,
  gradientColor,
  aspectRatio,
  fit = 'cover',
  actionIcon,
  actionText,
  contentDescription,
  onPressed,
  onActionPressed,
  className = '',
}) => {
  const hasOverlay = title || subtitle || actionText;

  const ariaLabel =
    contentDescription ||
    `Image card${title ? ': ' + title : ''}${subtitle ? ', ' + subtitle : ''}`;

  const getGradientDirection = () => {
    switch (overlayPosition) {
      case 'top':
        return 'to bottom';
      case 'center':
        return 'to bottom';
      case 'bottom':
      default:
        return 'to top';
    }
  };

  const getOverlayAlignment = () => {
    switch (overlayPosition) {
      case 'top':
        return 'flex-start';
      case 'center':
        return 'center';
      case 'bottom':
      default:
        return 'flex-end';
    }
  };

  const getObjectFit = () => {
    switch (fit) {
      case 'contain':
        return 'contain';
      case 'fill':
        return 'fill';
      case 'none':
        return 'none';
      case 'cover':
      default:
        return 'cover';
    }
  };

  const handleActionClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onActionPressed?.();
  };

  return (
    <Card
      elevation={2}
      className={`image-card ${className}`}
      sx={{
        height: '100%',
        cursor: onPressed ? 'pointer' : 'default',
        transition: 'all 0.3s ease',
        position: 'relative',
        overflow: 'hidden',
        '&:hover': {
          transform: 'scale(1.02)',
          boxShadow: 4,
        },
      }}
      onClick={onPressed}
      role="article"
      aria-label={ariaLabel}
    >
      <Box
        sx={{
          position: 'relative',
          width: '100%',
          height: '100%',
          paddingTop: aspectRatio ? `${(1 / aspectRatio) * 100}%` : 0,
        }}
      >
        <CardMedia
          component="img"
          image={imageUrl}
          alt={title || 'Image'}
          sx={{
            position: aspectRatio ? 'absolute' : 'relative',
            top: 0,
            left: 0,
            width: '100%',
            height: aspectRatio ? '100%' : 300,
            objectFit: getObjectFit(),
          }}
        />

        {hasOverlay && (
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: getOverlayAlignment(),
              p: 3,
              background:
                showGradient
                  ? `linear-gradient(${getGradientDirection()}, ${gradientColor || 'rgba(0, 0, 0, 0.7)'} 0%, transparent 100%)`
                  : 'transparent',
            }}
          >
            {title && (
              <Typography
                variant="h5"
                component="h3"
                fontWeight="bold"
                color="white"
                sx={{
                  textShadow: '0 2px 4px rgba(0, 0, 0, 0.5)',
                  mb: subtitle || actionText ? 1 : 0,
                }}
              >
                {title}
              </Typography>
            )}

            {subtitle && (
              <Typography
                variant="body2"
                color="white"
                sx={{
                  textShadow: '0 1px 2px rgba(0, 0, 0, 0.5)',
                  mb: actionText ? 2 : 0,
                }}
              >
                {subtitle}
              </Typography>
            )}

            {actionText && (
              <Button
                variant="contained"
                color="primary"
                onClick={handleActionClick}
                startIcon={actionIcon ? <Icon>{actionIcon}</Icon> : undefined}
                sx={{
                  alignSelf: 'flex-start',
                  textTransform: 'none',
                }}
                aria-label={actionText}
              >
                {actionText}
              </Button>
            )}
          </Box>
        )}
      </Box>
    </Card>
  );
};

export default ImageCard;
