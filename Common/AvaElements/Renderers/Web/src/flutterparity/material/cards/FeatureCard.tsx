/**
 * FeatureCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card for highlighting product features with icon, title, and description.
 * Commonly used in landing pages, feature showcases, and marketing materials.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Card, CardContent, Typography, Button, Box, Icon } from '@mui/material';

export type FeatureCardLayout = 'vertical' | 'horizontal';
export type FeatureCardIconPosition = 'top' | 'left' | 'right' | 'bottom';

export interface FeatureCardProps {
  icon: string;
  iconSize?: number;
  iconColor?: string;
  title: string;
  description: string;
  actionText?: string;
  actionIcon?: string;
  layout?: FeatureCardLayout;
  iconPosition?: FeatureCardIconPosition;
  contentDescription?: string;
  onPressed?: () => void;
  onActionPressed?: () => void;
  className?: string;
}

export const FeatureCard: React.FC<FeatureCardProps> = ({
  icon,
  iconSize = 48,
  iconColor,
  title,
  description,
  actionText,
  actionIcon,
  layout = 'vertical',
  iconPosition = 'top',
  contentDescription,
  onPressed,
  onActionPressed,
  className = '',
}) => {
  const ariaLabel = contentDescription || `Feature: ${title}, ${description}`;

  const iconElement = (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: iconSize,
        height: iconSize,
        color: iconColor || 'primary.main',
        mb: layout === 'vertical' && iconPosition === 'top' ? 2 : 0,
        mt: layout === 'vertical' && iconPosition === 'bottom' ? 2 : 0,
        mr: layout === 'horizontal' && iconPosition === 'left' ? 2 : 0,
        ml: layout === 'horizontal' && iconPosition === 'right' ? 2 : 0,
      }}
    >
      <Icon sx={{ fontSize: iconSize }}>{icon}</Icon>
    </Box>
  );

  const contentElement = (
    <Box sx={{ flex: 1 }}>
      <Typography variant="h6" component="h3" fontWeight="bold" gutterBottom>
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" paragraph>
        {description}
      </Typography>
      {actionText && (
        <Button
          variant="text"
          color="primary"
          onClick={onActionPressed}
          endIcon={actionIcon ? <Icon>{actionIcon}</Icon> : undefined}
          sx={{ textTransform: 'none', px: 0 }}
          aria-label={actionText}
        >
          {actionText}
        </Button>
      )}
    </Box>
  );

  const getFlexDirection = () => {
    if (layout === 'horizontal') {
      return iconPosition === 'right' ? 'row-reverse' : 'row';
    }
    return iconPosition === 'bottom' ? 'column-reverse' : 'column';
  };

  return (
    <Card
      elevation={2}
      className={`feature-card ${className}`}
      sx={{
        height: '100%',
        cursor: onPressed ? 'pointer' : 'default',
        transition: 'all 0.3s ease',
        '&:hover': onPressed
          ? {
              transform: 'translateY(-4px)',
              boxShadow: 4,
            }
          : {},
      }}
      onClick={onPressed}
      role="article"
      aria-label={ariaLabel}
    >
      <CardContent sx={{ p: 3, height: '100%' }}>
        <Box
          sx={{
            display: 'flex',
            flexDirection: getFlexDirection(),
            alignItems: layout === 'horizontal' ? 'flex-start' : 'center',
            textAlign: layout === 'vertical' ? 'center' : 'left',
            height: '100%',
          }}
        >
          {iconElement}
          {contentElement}
        </Box>
      </CardContent>
    </Card>
  );
};

export default FeatureCard;
