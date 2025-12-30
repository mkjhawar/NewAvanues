/**
 * HoverCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card with hover effects and actions that appear on interaction.
 * Commonly used in interactive grids, dashboards, and portfolio displays.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { Card, CardMedia, Typography, Box, IconButton, Icon } from '@mui/material';

export type HoverCardActionsPosition = 'top' | 'center' | 'bottom';

export interface HoverCardAction {
  id: string;
  label: string;
  icon?: string;
  enabled?: boolean;
}

export interface HoverCardProps {
  imageUrl?: string;
  title: string;
  description?: string;
  elevation?: number;
  hoverElevation?: number;
  scaleOnHover?: number;
  showOverlay?: boolean;
  overlayColor?: string;
  actions?: HoverCardAction[];
  actionsPosition?: HoverCardActionsPosition;
  contentDescription?: string;
  onPressed?: () => void;
  onActionPressed?: (actionId: string) => void;
  className?: string;
}

export const HoverCard: React.FC<HoverCardProps> = ({
  imageUrl,
  title,
  description,
  elevation = 1,
  hoverElevation = 4,
  scaleOnHover = 1.0,
  showOverlay = true,
  overlayColor,
  actions = [],
  actionsPosition = 'bottom',
  contentDescription,
  onPressed,
  onActionPressed,
  className = '',
}) => {
  const [isHovered, setIsHovered] = useState(false);

  const handleActionClick = (e: React.MouseEvent, actionId: string) => {
    e.stopPropagation();
    onActionPressed?.(actionId);
  };

  const getActionsAlignment = () => {
    switch (actionsPosition) {
      case 'top':
        return 'flex-start';
      case 'center':
        return 'center';
      case 'bottom':
      default:
        return 'flex-end';
    }
  };

  const ariaLabel =
    contentDescription ||
    `${title}${description ? ', ' + description : ''}${actions.length > 0 ? ', ' + actions.length + ' actions available' : ''}`;

  return (
    <Card
      elevation={isHovered ? hoverElevation : elevation}
      className={`hover-card ${className}`}
      sx={{
        height: '100%',
        cursor: onPressed ? 'pointer' : 'default',
        transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        position: 'relative',
        overflow: 'hidden',
        transform: isHovered ? `scale(${scaleOnHover})` : 'scale(1)',
      }}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onClick={onPressed}
      role="article"
      aria-label={ariaLabel}
    >
      {imageUrl && (
        <CardMedia
          component="img"
          height="200"
          image={imageUrl}
          alt={title}
          sx={{
            objectFit: 'cover',
            transition: 'transform 0.3s ease',
            transform: isHovered ? 'scale(1.1)' : 'scale(1)',
          }}
        />
      )}

      <Box
        sx={{
          position: imageUrl ? 'absolute' : 'relative',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: imageUrl ? getActionsAlignment() : 'flex-start',
          p: 3,
          background:
            showOverlay && isHovered
              ? imageUrl
                ? `linear-gradient(to top, ${overlayColor || 'rgba(0, 0, 0, 0.7)'} 0%, transparent 100%)`
                : 'transparent'
              : 'transparent',
          transition: 'background 0.3s ease',
        }}
      >
        <Box
          sx={{
            opacity: imageUrl && !isHovered ? 0 : 1,
            transform: isHovered ? 'translateY(0)' : 'translateY(20px)',
            transition: 'all 0.3s ease',
          }}
        >
          <Typography
            variant="h6"
            component="h3"
            fontWeight="bold"
            color={imageUrl ? 'white' : 'text.primary'}
            sx={{
              textShadow: imageUrl ? '0 2px 4px rgba(0, 0, 0, 0.5)' : 'none',
              mb: description || actions.length > 0 ? 1 : 0,
            }}
          >
            {title}
          </Typography>

          {description && (
            <Typography
              variant="body2"
              color={imageUrl ? 'white' : 'text.secondary'}
              sx={{
                textShadow: imageUrl ? '0 1px 2px rgba(0, 0, 0, 0.5)' : 'none',
                mb: actions.length > 0 ? 2 : 0,
              }}
            >
              {description}
            </Typography>
          )}

          {actions.length > 0 && (
            <Box
              sx={{
                display: 'flex',
                gap: 1,
                opacity: isHovered ? 1 : 0,
                transform: isHovered ? 'translateY(0)' : 'translateY(10px)',
                transition: 'all 0.3s ease 0.1s',
              }}
            >
              {actions.map((action) => (
                <IconButton
                  key={action.id}
                  onClick={(e) => handleActionClick(e, action.id)}
                  disabled={action.enabled === false}
                  sx={{
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    color: 'primary.main',
                    '&:hover': {
                      backgroundColor: 'rgba(255, 255, 255, 1)',
                    },
                    '&.Mui-disabled': {
                      backgroundColor: 'rgba(255, 255, 255, 0.5)',
                    },
                  }}
                  aria-label={action.label}
                  title={action.label}
                >
                  {action.icon ? <Icon>{action.icon}</Icon> : <Icon>more_vert</Icon>}
                </IconButton>
              ))}
            </Box>
          )}
        </Box>
      </Box>

      {!imageUrl && (
        <Box sx={{ p: 3 }}>
          <Typography variant="h6" component="h3" fontWeight="bold" gutterBottom>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary">
              {description}
            </Typography>
          )}
        </Box>
      )}
    </Card>
  );
};

export default HoverCard;
