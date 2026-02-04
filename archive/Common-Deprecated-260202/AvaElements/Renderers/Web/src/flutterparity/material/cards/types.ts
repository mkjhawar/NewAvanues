/**
 * Card Component Type Definitions
 *
 * @since 3.0.0-flutter-parity
 */

import type { ReactNode } from 'react';
import type { CardProps as MuiCardProps } from '@mui/material';
import type { PricingCardProps } from './PricingCard';
import type { FeatureCardProps } from './FeatureCard';
import type { TestimonialCardProps } from './TestimonialCard';
import type { ProductCardProps } from './ProductCard';
import type { ArticleCardProps } from './ArticleCard';
import type { ImageCardProps } from './ImageCard';
import type { HoverCardProps } from './HoverCard';
import type { ExpandableCardProps } from './ExpandableCard';

export interface CardProps extends Omit<MuiCardProps, 'elevation' | 'variant'> {
  children?: ReactNode;
  elevation?: number;
  color?: string;
  shadowColor?: string;
  shape?: {
    borderRadius?: number;
  };
  margin?: number;
  clipBehavior?: 'antiAlias' | 'antiAliasWithSaveLayer' | 'hardEdge' | 'none';
  semanticContainer?: boolean;
  borderOnForeground?: boolean;
}

export interface CardThemeData {
  color?: string;
  shadowColor?: string;
  elevation?: number;
  shape?: {
    borderRadius?: number;
  };
  margin?: number;
  clipBehavior?: string;
}

export interface CardThemeProps {
  data: CardThemeData;
  children: ReactNode;
}

// Re-export specialized card component props
export type {
  PricingCardProps,
  FeatureCardProps,
  TestimonialCardProps,
  ProductCardProps,
  ArticleCardProps,
  ImageCardProps,
  HoverCardProps,
  ExpandableCardProps,
};
