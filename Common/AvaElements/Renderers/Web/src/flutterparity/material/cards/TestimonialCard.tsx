/**
 * TestimonialCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card for displaying user testimonials with avatar, quote, and author info.
 * Commonly used in marketing pages, reviews sections, and social proof displays.
 *
 * @since 3.0.0-flutter-parity
 */

import React from 'react';
import { Card, CardContent, Typography, Avatar, Box, Rating } from '@mui/material';
import FormatQuoteIcon from '@mui/icons-material/FormatQuote';

export interface TestimonialCardProps {
  quote: string;
  authorName: string;
  authorTitle?: string;
  avatarUrl?: string;
  avatarInitials?: string;
  rating?: number;
  showQuoteIcon?: boolean;
  quoteIcon?: string;
  contentDescription?: string;
  onPressed?: () => void;
  className?: string;
}

export const TestimonialCard: React.FC<TestimonialCardProps> = ({
  quote,
  authorName,
  authorTitle,
  avatarUrl,
  avatarInitials,
  rating,
  showQuoteIcon = true,
  quoteIcon,
  contentDescription,
  onPressed,
  className = '',
}) => {
  const ariaLabel =
    contentDescription ||
    `Testimonial from ${authorName}${rating ? `, rated ${rating} out of 5 stars` : ''}: ${quote}`;

  const isRatingValid = rating === undefined || (rating >= 1 && rating <= 5);

  return (
    <Card
      elevation={2}
      className={`testimonial-card ${className}`}
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
      <CardContent sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
        {showQuoteIcon && (
          <Box sx={{ mb: 2 }}>
            <FormatQuoteIcon sx={{ fontSize: 40, color: 'primary.main', opacity: 0.3 }} />
          </Box>
        )}

        <Typography
          variant="body1"
          color="text.primary"
          sx={{
            flex: 1,
            mb: 3,
            fontStyle: 'italic',
            lineHeight: 1.6,
          }}
        >
          "{quote}"
        </Typography>

        {rating !== undefined && isRatingValid && (
          <Box sx={{ mb: 2 }}>
            <Rating value={rating} readOnly size="small" aria-label={`${rating} out of 5 stars`} />
          </Box>
        )}

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar
            src={avatarUrl}
            alt={authorName}
            sx={{
              width: 48,
              height: 48,
              bgcolor: 'primary.main',
            }}
          >
            {!avatarUrl && (avatarInitials || authorName.charAt(0))}
          </Avatar>
          <Box>
            <Typography variant="subtitle2" fontWeight="bold">
              {authorName}
            </Typography>
            {authorTitle && (
              <Typography variant="body2" color="text.secondary">
                {authorTitle}
              </Typography>
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default TestimonialCard;
