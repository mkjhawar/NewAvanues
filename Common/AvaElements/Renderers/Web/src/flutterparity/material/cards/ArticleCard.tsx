/**
 * ArticleCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card for displaying blog/news articles with image, title, excerpt, and metadata.
 * Commonly used in news feeds, blog listings, and content grids.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { Card, CardContent, CardMedia, Typography, Avatar, Box, IconButton, Chip } from '@mui/material';
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder';
import BookmarkIcon from '@mui/icons-material/Bookmark';

export interface ArticleCardProps {
  imageUrl: string;
  title: string;
  excerpt: string;
  authorName: string;
  authorAvatar?: string;
  publishedDate: string;
  readTime?: string;
  category?: string;
  tags?: string[];
  showBookmark?: boolean;
  bookmarked?: boolean;
  contentDescription?: string;
  onPressed?: () => void;
  onBookmark?: (isBookmarked: boolean) => void;
  className?: string;
}

export const ArticleCard: React.FC<ArticleCardProps> = ({
  imageUrl,
  title,
  excerpt,
  authorName,
  authorAvatar,
  publishedDate,
  readTime,
  category,
  tags = [],
  showBookmark = true,
  bookmarked: initialBookmarked = false,
  contentDescription,
  onPressed,
  onBookmark,
  className = '',
}) => {
  const [isBookmarked, setIsBookmarked] = useState(initialBookmarked);

  const handleBookmarkToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    const newValue = !isBookmarked;
    setIsBookmarked(newValue);
    onBookmark?.(newValue);
  };

  const metadataText = [publishedDate, readTime].filter(Boolean).join(' • ');

  const ariaLabel =
    contentDescription ||
    `Article: ${title}, by ${authorName}, published ${publishedDate}${readTime ? ', ' + readTime : ''}${isBookmarked ? ', bookmarked' : ''}`;

  return (
    <Card
      elevation={2}
      className={`article-card ${className}`}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        cursor: onPressed ? 'pointer' : 'default',
        transition: 'all 0.3s ease',
        position: 'relative',
        '&:hover': {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        },
      }}
      onClick={onPressed}
      role="article"
      aria-label={ariaLabel}
    >
      <Box sx={{ position: 'relative' }}>
        <CardMedia
          component="img"
          height="200"
          image={imageUrl}
          alt={title}
          sx={{ objectFit: 'cover' }}
        />

        {category && (
          <Chip
            label={category}
            size="small"
            sx={{
              position: 'absolute',
              bottom: 12,
              left: 12,
              backgroundColor: 'primary.main',
              color: 'white',
              fontWeight: 'bold',
            }}
          />
        )}

        {showBookmark && (
          <IconButton
            onClick={handleBookmarkToggle}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              backgroundColor: 'rgba(255, 255, 255, 0.9)',
              '&:hover': {
                backgroundColor: 'rgba(255, 255, 255, 1)',
              },
            }}
            aria-label={isBookmarked ? 'Remove bookmark' : 'Bookmark article'}
          >
            {isBookmarked ? (
              <BookmarkIcon color="primary" />
            ) : (
              <BookmarkBorderIcon />
            )}
          </IconButton>
        )}
      </Box>

      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2 }}>
        <Typography variant="h6" component="h3" fontWeight="bold" gutterBottom>
          {title}
        </Typography>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{
            mb: 2,
            flex: 1,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            display: '-webkit-box',
            WebkitLineClamp: 3,
            WebkitBoxOrient: 'vertical',
          }}
        >
          {excerpt}
        </Typography>

        {tags.length > 0 && (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mb: 2 }}>
            {tags.map((tag, index) => (
              <Chip
                key={index}
                label={tag}
                size="small"
                variant="outlined"
                sx={{ fontSize: '0.75rem' }}
              />
            ))}
          </Box>
        )}

        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 'auto' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Avatar
              src={authorAvatar}
              alt={authorName}
              sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}
            >
              {!authorAvatar && authorName.charAt(0)}
            </Avatar>
            <Box>
              <Typography variant="caption" fontWeight="bold" display="block">
                {authorName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {metadataText}
              </Typography>
            </Box>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

export default ArticleCard;
