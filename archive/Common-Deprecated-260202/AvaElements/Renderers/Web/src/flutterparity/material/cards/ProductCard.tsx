/**
 * ProductCard Component - Flutter Parity Material Design
 *
 * A Material Design 3 card for displaying e-commerce products with image, title, price, and rating.
 * Commonly used in product catalogs, shopping grids, and marketplace listings.
 *
 * @since 3.0.0-flutter-parity
 */

import React, { useState } from 'react';
import { Card, CardContent, CardMedia, Typography, Rating, Box, IconButton, Chip, Button } from '@mui/material';
import FavoriteIcon from '@mui/icons-material/Favorite';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';

export interface ProductCardProps {
  imageUrl: string;
  title: string;
  description?: string;
  price: string;
  originalPrice?: string;
  currency?: string;
  rating?: number;
  reviewCount?: number;
  badgeText?: string;
  badgeColor?: string;
  inStock?: boolean;
  showAddToCart?: boolean;
  showWishlist?: boolean;
  contentDescription?: string;
  onPressed?: () => void;
  onAddToCart?: () => void;
  onWishlist?: (isWishlisted: boolean) => void;
  className?: string;
}

export const ProductCard: React.FC<ProductCardProps> = ({
  imageUrl,
  title,
  description,
  price,
  originalPrice,
  currency,
  rating,
  reviewCount,
  badgeText,
  badgeColor,
  inStock = true,
  showAddToCart = true,
  showWishlist = true,
  contentDescription,
  onPressed,
  onAddToCart,
  onWishlist,
  className = '',
}) => {
  const [isWishlisted, setIsWishlisted] = useState(false);

  const handleWishlistToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    const newValue = !isWishlisted;
    setIsWishlisted(newValue);
    onWishlist?.(newValue);
  };

  const handleAddToCart = (e: React.MouseEvent) => {
    e.stopPropagation();
    onAddToCart?.();
  };

  const hasDiscount = originalPrice && originalPrice !== price;
  const isRatingValid = rating === undefined || (rating >= 0 && rating <= 5);

  const ariaLabel =
    contentDescription ||
    `${title}, ${hasDiscount ? `${price}, was ${originalPrice}` : price}${rating ? `, rated ${rating} out of 5 stars` : ''}${!inStock ? ', out of stock' : ''}${badgeText ? ', ' + badgeText : ''}`;

  return (
    <Card
      elevation={2}
      className={`product-card ${className}`}
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
          sx={{
            objectFit: 'cover',
            opacity: inStock ? 1 : 0.6,
          }}
        />

        {badgeText && (
          <Chip
            label={badgeText}
            size="small"
            sx={{
              position: 'absolute',
              top: 12,
              left: 12,
              backgroundColor: badgeColor || 'error.main',
              color: 'white',
              fontWeight: 'bold',
            }}
          />
        )}

        {showWishlist && (
          <IconButton
            onClick={handleWishlistToggle}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              backgroundColor: 'rgba(255, 255, 255, 0.9)',
              '&:hover': {
                backgroundColor: 'rgba(255, 255, 255, 1)',
              },
            }}
            aria-label={isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
          >
            {isWishlisted ? (
              <FavoriteIcon color="error" />
            ) : (
              <FavoriteBorderIcon />
            )}
          </IconButton>
        )}
      </Box>

      <CardContent sx={{ flex: 1, display: 'flex', flexDirection: 'column', p: 2 }}>
        <Typography variant="h6" component="h3" fontWeight="bold" gutterBottom noWrap>
          {title}
        </Typography>

        {description && (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{
              mb: 2,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              display: '-webkit-box',
              WebkitLineClamp: 2,
              WebkitBoxOrient: 'vertical',
            }}
          >
            {description}
          </Typography>
        )}

        {rating !== undefined && isRatingValid && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <Rating value={rating} precision={0.5} readOnly size="small" aria-label={`${rating} out of 5 stars`} />
            {reviewCount !== undefined && (
              <Typography variant="caption" color="text.secondary">
                ({reviewCount})
              </Typography>
            )}
          </Box>
        )}

        <Box sx={{ mt: 'auto' }}>
          <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, mb: 2 }}>
            <Typography variant="h6" component="div" fontWeight="bold" color={hasDiscount ? 'error.main' : 'text.primary'}>
              {currency || ''}{price}
            </Typography>
            {hasDiscount && (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ textDecoration: 'line-through' }}
              >
                {currency || ''}{originalPrice}
              </Typography>
            )}
          </Box>

          {!inStock && (
            <Typography variant="body2" color="error" fontWeight="bold" sx={{ mb: 1 }}>
              Out of Stock
            </Typography>
          )}

          {showAddToCart && (
            <Button
              variant="contained"
              color="primary"
              fullWidth
              disabled={!inStock}
              onClick={handleAddToCart}
              startIcon={<ShoppingCartIcon />}
              sx={{ textTransform: 'none' }}
              aria-label="Add to cart"
            >
              Add to Cart
            </Button>
          )}
        </Box>
      </CardContent>
    </Card>
  );
};

export default ProductCard;
