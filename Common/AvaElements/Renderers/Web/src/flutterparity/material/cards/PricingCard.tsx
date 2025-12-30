/**
 * PricingCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card for displaying pricing tiers with features, price, and CTA button.
 * Commonly used in pricing pages, subscription flows, and feature comparison tables.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Card, CardContent, Typography, Button, List, ListItem, ListItemIcon, ListItemText, Box, Chip } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

export interface PricingCardProps {
  title: string;
  subtitle?: string;
  price: string;
  period?: string;
  currency?: string;
  features: string[];
  featureIcons?: string[];
  buttonText: string;
  buttonEnabled?: boolean;
  highlighted?: boolean;
  ribbonText?: string;
  ribbonColor?: string;
  contentDescription?: string;
  onPressed?: () => void;
  className?: string;
}

export const PricingCard: React.FC<PricingCardProps> = ({
  title,
  subtitle,
  price,
  period,
  currency,
  features = [],
  featureIcons,
  buttonText,
  buttonEnabled = true,
  highlighted = false,
  ribbonText,
  ribbonColor,
  contentDescription,
  onPressed,
  className = '',
}) => {
  const ariaLabel = contentDescription || `${title} pricing tier, ${price}${period ? ' ' + period : ''}${highlighted ? ', featured' : ''}${ribbonText ? ', ' + ribbonText : ''}`;

  return (
    <Card
      elevation={highlighted ? 8 : 2}
      className={`pricing-card ${highlighted ? 'highlighted' : ''} ${className}`}
      sx={{
        position: 'relative',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        border: highlighted ? 2 : 0,
        borderColor: highlighted ? 'primary.main' : 'transparent',
        transition: 'all 0.3s ease',
        '&:hover': {
          transform: highlighted ? 'scale(1.02)' : 'scale(1.01)',
          boxShadow: highlighted ? 12 : 4,
        },
      }}
      role="article"
      aria-label={ariaLabel}
    >
      {ribbonText && (
        <Chip
          label={ribbonText}
          size="small"
          sx={{
            position: 'absolute',
            top: 16,
            right: 16,
            backgroundColor: ribbonColor || 'secondary.main',
            color: 'white',
            fontWeight: 'bold',
            zIndex: 1,
          }}
        />
      )}

      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 3 }}>
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Typography variant="h5" component="h3" fontWeight="bold" gutterBottom>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {subtitle}
            </Typography>
          )}
          <Box sx={{ my: 2 }}>
            <Typography variant="h3" component="div" fontWeight="bold" color={highlighted ? 'primary.main' : 'text.primary'}>
              {currency || ''}{price}
            </Typography>
            {period && (
              <Typography variant="body2" color="text.secondary">
                {period}
              </Typography>
            )}
          </Box>
        </Box>

        <List sx={{ flex: 1, mb: 2 }}>
          {features.map((feature, index) => (
            <ListItem key={index} sx={{ px: 0, py: 0.5 }}>
              <ListItemIcon sx={{ minWidth: 36 }}>
                <CheckCircleIcon color="primary" fontSize="small" />
              </ListItemIcon>
              <ListItemText
                primary={feature}
                primaryTypographyProps={{ variant: 'body2' }}
              />
            </ListItem>
          ))}
        </List>

        <Button
          variant={highlighted ? 'contained' : 'outlined'}
          color="primary"
          fullWidth
          size="large"
          disabled={!buttonEnabled}
          onClick={onPressed}
          sx={{
            mt: 'auto',
            py: 1.5,
            fontWeight: 'bold',
            textTransform: 'none',
          }}
          aria-label={`${buttonText} for ${title} plan`}
        >
          {buttonText}
        </Button>
      </CardContent>
    </Card>
  );
};

export default PricingCard;
