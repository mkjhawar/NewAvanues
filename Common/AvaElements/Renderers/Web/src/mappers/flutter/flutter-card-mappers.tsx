/**
 * Flutter Parity Card Mappers - Web/React
 *
 * Maps Flutter specialized card components to React/Material-UI equivalents.
 * Includes PricingCard, FeatureCard, TestimonialCard, ProductCard, ArticleCard,
 * ImageCard, HoverCard, and ExpandableCard.
 *
 * @module FlutterCardMappers
 * @since 3.2.0
 */

import React, { useState } from 'react';
import {
  Card,
  CardContent,
  CardMedia,
  CardActions,
  CardHeader,
  CardActionArea,
  Typography,
  Button,
  IconButton,
  Avatar,
  Rating,
  Chip,
  Box,
  Collapse,
  Divider,
  Stack,
  Badge,
  List,
  ListItem,
} from '@mui/material';
import {
  CheckCircle,
  ExpandMore as ExpandMoreIcon,
  Bookmark,
  BookmarkBorder,
  ShoppingCart,
  Favorite,
  FavoriteBorder,
  FormatQuote,
} from '@mui/icons-material';
import type { FlutterParityComponent } from '../../types';

/**
 * Component Type Definitions (to be added to types/index.ts)
 */

export interface PricingCardComponent extends FlutterParityComponent {
  type: 'PricingCard';
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
  onPressed?: () => void;
}

export interface FeatureCardComponent extends FlutterParityComponent {
  type: 'FeatureCard';
  icon: string;
  iconSize?: number;
  iconColor?: string;
  title: string;
  description: string;
  actionText?: string;
  actionIcon?: string;
  layout?: 'Vertical' | 'Horizontal';
  iconPosition?: 'Top' | 'Left' | 'Right' | 'Bottom';
  onPressed?: () => void;
  onActionPressed?: () => void;
}

export interface TestimonialCardComponent extends FlutterParityComponent {
  type: 'TestimonialCard';
  quote: string;
  authorName: string;
  authorTitle?: string;
  avatarUrl?: string;
  avatarInitials?: string;
  rating?: number;
  showQuoteIcon?: boolean;
  onPressed?: () => void;
}

export interface ProductCardComponent extends FlutterParityComponent {
  type: 'ProductCard';
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
  onPressed?: () => void;
  onAddToCart?: () => void;
  onWishlist?: (bookmarked: boolean) => void;
}

export interface ArticleCardComponent extends FlutterParityComponent {
  type: 'ArticleCard';
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
  onPressed?: () => void;
  onBookmark?: (bookmarked: boolean) => void;
}

export interface ImageCardComponent extends FlutterParityComponent {
  type: 'ImageCard';
  imageUrl: string;
  title?: string;
  subtitle?: string;
  overlayPosition?: 'Top' | 'Center' | 'Bottom';
  showGradient?: boolean;
  gradientColor?: string;
  aspectRatio?: number;
  fit?: 'Cover' | 'Contain' | 'Fill' | 'None';
  actionIcon?: string;
  actionText?: string;
  onPressed?: () => void;
  onActionPressed?: () => void;
}

export interface HoverCardComponent extends FlutterParityComponent {
  type: 'HoverCard';
  imageUrl?: string;
  title: string;
  description?: string;
  elevation?: number;
  hoverElevation?: number;
  scaleOnHover?: number;
  showOverlay?: boolean;
  overlayColor?: string;
  actions?: Array<{ id: string; label: string; icon?: string; enabled?: boolean }>;
  actionsPosition?: 'Top' | 'Center' | 'Bottom';
  onPressed?: () => void;
  onActionPressed?: (actionId: string) => void;
}

export interface ExpandableCardComponent extends FlutterParityComponent {
  type: 'ExpandableCard';
  title: string;
  subtitle?: string;
  icon?: string;
  summaryContent?: string;
  expandedContent: string;
  initiallyExpanded?: boolean;
  expanded?: boolean;
  showDivider?: boolean;
  expandIcon?: string;
  collapseIcon?: string;
  animationDuration?: number;
  headerActions?: Array<{ id: string; icon: string; label?: string; enabled?: boolean }>;
  onExpansionChanged?: (expanded: boolean) => void;
  onHeaderActionPressed?: (actionId: string) => void;
}

/**
 * PricingCard Component Mapper
 */
export const PricingCardMapper: React.FC<{ component: PricingCardComponent }> = ({
  component,
}) => {
  const {
    title,
    subtitle,
    price,
    period,
    features = [],
    buttonText,
    buttonEnabled = true,
    highlighted = false,
    ribbonText,
    onPressed,
    key,
  } = component;

  return (
    <Card
      key={key}
      raised={highlighted}
      sx={{
        position: 'relative',
        border: highlighted ? '2px solid' : undefined,
        borderColor: highlighted ? 'primary.main' : undefined,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {ribbonText && (
        <Chip
          label={ribbonText}
          color="secondary"
          size="small"
          sx={{
            position: 'absolute',
            top: 16,
            right: 16,
            zIndex: 1,
          }}
        />
      )}
      <CardContent sx={{ flexGrow: 1 }}>
        <Typography variant="h5" component="h2" gutterBottom align="center">
          {title}
        </Typography>
        {subtitle && (
          <Typography variant="subtitle2" color="text.secondary" align="center" gutterBottom>
            {subtitle}
          </Typography>
        )}
        <Box sx={{ my: 3, textAlign: 'center' }}>
          <Typography variant="h3" component="span" color="primary">
            {price}
          </Typography>
          {period && (
            <Typography variant="body2" color="text.secondary" component="span">
              {' '}
              / {period}
            </Typography>
          )}
        </Box>
        <List disablePadding>
          {features.map((feature, index) => (
            <ListItem key={index} sx={{ py: 1, px: 0 }}>
              <CheckCircle color="primary" sx={{ mr: 1.5, fontSize: 20 }} />
              <Typography variant="body2">{feature}</Typography>
            </ListItem>
          ))}
        </List>
      </CardContent>
      <CardActions sx={{ p: 2, pt: 0 }}>
        <Button
          fullWidth
          variant={highlighted ? 'contained' : 'outlined'}
          size="large"
          disabled={!buttonEnabled}
          onClick={onPressed}
        >
          {buttonText}
        </Button>
      </CardActions>
    </Card>
  );
};

/**
 * FeatureCard Component Mapper
 */
export const FeatureCardMapper: React.FC<{ component: FeatureCardComponent }> = ({
  component,
}) => {
  const {
    icon,
    iconSize = 48,
    iconColor,
    title,
    description,
    actionText,
    layout = 'Vertical',
    onPressed,
    onActionPressed,
    key,
  } = component;

  const isHorizontal = layout === 'Horizontal';

  return (
    <Card key={key} sx={{ height: '100%' }}>
      <CardActionArea onClick={onPressed}>
        <CardContent>
          <Box
            sx={{
              display: 'flex',
              flexDirection: isHorizontal ? 'row' : 'column',
              alignItems: isHorizontal ? 'flex-start' : 'center',
              gap: 2,
            }}
          >
            <Box
              sx={{
                fontSize: iconSize,
                color: iconColor || 'primary.main',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {icon}
            </Box>
            <Box sx={{ textAlign: isHorizontal ? 'left' : 'center', flexGrow: 1 }}>
              <Typography variant="h6" component="h3" gutterBottom>
                {title}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {description}
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </CardActionArea>
      {actionText && (
        <CardActions>
          <Button size="small" onClick={onActionPressed}>
            {actionText}
          </Button>
        </CardActions>
      )}
    </Card>
  );
};

/**
 * TestimonialCard Component Mapper
 */
export const TestimonialCardMapper: React.FC<{ component: TestimonialCardComponent }> = ({
  component,
}) => {
  const {
    quote,
    authorName,
    authorTitle,
    avatarUrl,
    avatarInitials,
    rating,
    showQuoteIcon = true,
    onPressed,
    key,
  } = component;

  return (
    <Card key={key} onClick={onPressed} sx={{ height: '100%', cursor: onPressed ? 'pointer' : 'default' }}>
      <CardContent>
        {showQuoteIcon && (
          <FormatQuote sx={{ fontSize: 40, color: 'primary.main', opacity: 0.3 }} />
        )}
        <Typography variant="body1" paragraph sx={{ fontStyle: 'italic' }}>
          "{quote}"
        </Typography>
        {rating && (
          <Box sx={{ mb: 2 }}>
            <Rating value={rating} readOnly size="small" />
          </Box>
        )}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar src={avatarUrl} alt={authorName}>
            {avatarInitials || authorName.charAt(0)}
          </Avatar>
          <Box>
            <Typography variant="subtitle2" fontWeight="bold">
              {authorName}
            </Typography>
            {authorTitle && (
              <Typography variant="caption" color="text.secondary">
                {authorTitle}
              </Typography>
            )}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};

/**
 * ProductCard Component Mapper
 */
export const ProductCardMapper: React.FC<{ component: ProductCardComponent }> = ({
  component,
}) => {
  const {
    imageUrl,
    title,
    description,
    price,
    originalPrice,
    rating,
    reviewCount,
    badgeText,
    badgeColor,
    inStock = true,
    showAddToCart = true,
    showWishlist = true,
    onPressed,
    onAddToCart,
    onWishlist,
    key,
  } = component;

  const [wishlisted, setWishlisted] = useState(false);

  const handleWishlistClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    const newValue = !wishlisted;
    setWishlisted(newValue);
    onWishlist?.(newValue);
  };

  return (
    <Card key={key} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ position: 'relative' }}>
        <CardMedia component="img" height="200" image={imageUrl} alt={title} />
        {badgeText && (
          <Chip
            label={badgeText}
            size="small"
            sx={{
              position: 'absolute',
              top: 8,
              left: 8,
              bgcolor: badgeColor || 'error.main',
              color: 'white',
            }}
          />
        )}
        {showWishlist && (
          <IconButton
            onClick={handleWishlistClick}
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              bgcolor: 'background.paper',
              '&:hover': { bgcolor: 'background.paper' },
            }}
          >
            {wishlisted ? <Favorite color="error" /> : <FavoriteBorder />}
          </IconButton>
        )}
      </Box>
      <CardActionArea onClick={onPressed} sx={{ flexGrow: 1 }}>
        <CardContent>
          <Typography variant="h6" component="h3" gutterBottom noWrap>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" paragraph>
              {description}
            </Typography>
          )}
          {rating !== undefined && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              <Rating value={rating} precision={0.5} size="small" readOnly />
              {reviewCount !== undefined && (
                <Typography variant="caption" color="text.secondary">
                  ({reviewCount})
                </Typography>
              )}
            </Box>
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="h6" color="primary">
              {price}
            </Typography>
            {originalPrice && (
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ textDecoration: 'line-through' }}
              >
                {originalPrice}
              </Typography>
            )}
          </Box>
          {!inStock && (
            <Typography variant="caption" color="error">
              Out of Stock
            </Typography>
          )}
        </CardContent>
      </CardActionArea>
      {showAddToCart && (
        <CardActions>
          <Button
            fullWidth
            variant="contained"
            startIcon={<ShoppingCart />}
            onClick={onAddToCart}
            disabled={!inStock}
          >
            Add to Cart
          </Button>
        </CardActions>
      )}
    </Card>
  );
};

/**
 * ArticleCard Component Mapper
 */
export const ArticleCardMapper: React.FC<{ component: ArticleCardComponent }> = ({
  component,
}) => {
  const {
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
    onPressed,
    onBookmark,
    key,
  } = component;

  const [bookmarked, setBookmarked] = useState(initialBookmarked);

  const handleBookmarkClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    const newValue = !bookmarked;
    setBookmarked(newValue);
    onBookmark?.(newValue);
  };

  return (
    <Card key={key} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardMedia component="img" height="180" image={imageUrl} alt={title} />
      <CardActionArea onClick={onPressed} sx={{ flexGrow: 1 }}>
        <CardContent>
          {category && (
            <Chip label={category} size="small" color="primary" sx={{ mb: 1 }} />
          )}
          <Typography variant="h6" component="h3" gutterBottom>
            {title}
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            {excerpt}
          </Typography>
          {tags.length > 0 && (
            <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap', gap: 0.5 }}>
              {tags.map((tag, index) => (
                <Chip key={index} label={tag} size="small" variant="outlined" />
              ))}
            </Stack>
          )}
        </CardContent>
      </CardActionArea>
      <Divider />
      <CardActions sx={{ justifyContent: 'space-between' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Avatar src={authorAvatar} alt={authorName} sx={{ width: 24, height: 24 }}>
            {authorName.charAt(0)}
          </Avatar>
          <Box>
            <Typography variant="caption" display="block">
              {authorName}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {publishedDate}
              {readTime && ` • ${readTime}`}
            </Typography>
          </Box>
        </Box>
        {showBookmark && (
          <IconButton size="small" onClick={handleBookmarkClick}>
            {bookmarked ? <Bookmark color="primary" /> : <BookmarkBorder />}
          </IconButton>
        )}
      </CardActions>
    </Card>
  );
};

/**
 * ImageCard Component Mapper
 */
export const ImageCardMapper: React.FC<{ component: ImageCardComponent }> = ({ component }) => {
  const {
    imageUrl,
    title,
    subtitle,
    overlayPosition = 'Bottom',
    showGradient = true,
    aspectRatio,
    actionText,
    onPressed,
    onActionPressed,
    key,
  } = component;

  const hasOverlay = title || subtitle || actionText;

  const overlayAlignment = {
    Top: 'flex-start',
    Center: 'center',
    Bottom: 'flex-end',
  }[overlayPosition];

  return (
    <Card key={key} sx={{ height: '100%', position: 'relative' }}>
      <CardActionArea onClick={onPressed} sx={{ height: '100%' }}>
        <CardMedia
          component="img"
          image={imageUrl}
          alt={title || 'Image'}
          sx={{
            height: aspectRatio ? `calc(100% / ${aspectRatio})` : 300,
            objectFit: 'cover',
          }}
        />
        {hasOverlay && (
          <Box
            sx={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              top: 0,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: overlayAlignment,
              background: showGradient
                ? overlayPosition === 'Bottom'
                  ? 'linear-gradient(to top, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0) 100%)'
                  : overlayPosition === 'Top'
                  ? 'linear-gradient(to bottom, rgba(0,0,0,0.7) 0%, rgba(0,0,0,0) 100%)'
                  : 'rgba(0,0,0,0.5)'
                : 'transparent',
              padding: 2,
            }}
          >
            {title && (
              <Typography variant="h5" color="white" fontWeight="bold">
                {title}
              </Typography>
            )}
            {subtitle && (
              <Typography variant="body2" color="white">
                {subtitle}
              </Typography>
            )}
            {actionText && (
              <Button
                variant="contained"
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  onActionPressed?.();
                }}
                sx={{ mt: 1, alignSelf: 'flex-start' }}
              >
                {actionText}
              </Button>
            )}
          </Box>
        )}
      </CardActionArea>
    </Card>
  );
};

/**
 * HoverCard Component Mapper
 */
export const HoverCardMapper: React.FC<{ component: HoverCardComponent }> = ({ component }) => {
  const {
    imageUrl,
    title,
    description,
    elevation = 1,
    hoverElevation = 4,
    scaleOnHover = 1.0,
    showOverlay = true,
    actions = [],
    onPressed,
    onActionPressed,
    key,
  } = component;

  const [hovered, setHovered] = useState(false);

  return (
    <Card
      key={key}
      elevation={hovered ? hoverElevation : elevation}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      sx={{
        height: '100%',
        transition: 'all 0.3s ease-in-out',
        transform: hovered ? `scale(${scaleOnHover})` : 'scale(1)',
        cursor: 'pointer',
      }}
      onClick={onPressed}
    >
      <Box sx={{ position: 'relative', height: '100%' }}>
        {imageUrl && <CardMedia component="img" height="200" image={imageUrl} alt={title} />}
        <CardContent>
          <Typography variant="h6" component="h3" gutterBottom>
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary">
              {description}
            </Typography>
          )}
        </CardContent>
        {showOverlay && hovered && actions.length > 0 && (
          <Box
            sx={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              bgcolor: 'rgba(0,0,0,0.6)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 1,
            }}
          >
            {actions.map((action) => (
              <IconButton
                key={action.id}
                color="primary"
                onClick={(e) => {
                  e.stopPropagation();
                  onActionPressed?.(action.id);
                }}
                disabled={!action.enabled}
                sx={{ bgcolor: 'background.paper' }}
              >
                {action.icon}
              </IconButton>
            ))}
          </Box>
        )}
      </Box>
    </Card>
  );
};

/**
 * ExpandableCard Component Mapper
 */
export const ExpandableCardMapper: React.FC<{ component: ExpandableCardComponent }> = ({
  component,
}) => {
  const {
    title,
    subtitle,
    icon,
    summaryContent,
    expandedContent,
    initiallyExpanded = false,
    expanded: controlledExpanded,
    showDivider = true,
    headerActions = [],
    onExpansionChanged,
    onHeaderActionPressed,
    key,
  } = component;

  const [internalExpanded, setInternalExpanded] = useState(initiallyExpanded);
  const isControlled = controlledExpanded !== undefined;
  const expanded = isControlled ? controlledExpanded : internalExpanded;

  const handleExpandClick = () => {
    const newValue = !expanded;
    if (!isControlled) {
      setInternalExpanded(newValue);
    }
    onExpansionChanged?.(newValue);
  };

  return (
    <Card key={key} sx={{ height: '100%' }}>
      <CardHeader
        avatar={icon ? <Avatar>{icon}</Avatar> : undefined}
        title={title}
        subheader={subtitle}
        action={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {headerActions.map((action) => (
              <IconButton
                key={action.id}
                onClick={(e) => {
                  e.stopPropagation();
                  onHeaderActionPressed?.(action.id);
                }}
                disabled={!action.enabled}
                size="small"
              >
                {action.icon}
              </IconButton>
            ))}
            <IconButton onClick={handleExpandClick} sx={{ transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)', transition: '0.3s' }}>
              <ExpandMoreIcon />
            </IconButton>
          </Box>
        }
      />
      {summaryContent && !expanded && (
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            {summaryContent}
          </Typography>
        </CardContent>
      )}
      {showDivider && expanded && <Divider />}
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <CardContent>
          <Typography variant="body2">{expandedContent}</Typography>
        </CardContent>
      </Collapse>
    </Card>
  );
};

/**
 * Export all card mappers
 */
export const FlutterCardMappers = {
  PricingCard: PricingCardMapper,
  FeatureCard: FeatureCardMapper,
  TestimonialCard: TestimonialCardMapper,
  ProductCard: ProductCardMapper,
  ArticleCard: ArticleCardMapper,
  ImageCard: ImageCardMapper,
  HoverCard: HoverCardMapper,
  ExpandableCard: ExpandableCardMapper,
};

export default FlutterCardMappers;
