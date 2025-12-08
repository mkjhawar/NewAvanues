/**
 * Flutter Parity Data Components - Type Definitions
 *
 * Comprehensive type definitions for data display components.
 *
 * @since 3.0.0-flutter-parity-web
 * @module flutterparity/data/types
 */

import React from 'react';

// ============================================================================
// BASE TYPES
// ============================================================================

export interface BaseDataComponentProps {
  className?: string;
  style?: React.CSSProperties;
  testId?: string;
}

// ============================================================================
// TREND TYPES
// ============================================================================

export interface Trend {
  value: number;
  isUpGood?: boolean;
  label?: string;
}

export type TrendDirection = 'up' | 'down' | 'neutral';

// ============================================================================
// DATA LIST TYPES
// ============================================================================

export interface DataItem {
  key: string;
  value: React.ReactNode;
  icon?: React.ReactNode;
  helpText?: string;
}

export interface DataListProps extends BaseDataComponentProps {
  items: DataItem[];
  layout?: 'horizontal' | 'vertical';
  divider?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

// ============================================================================
// DESCRIPTION LIST TYPES
// ============================================================================

export interface DescriptionItem {
  term: string;
  description: React.ReactNode;
}

export interface DescriptionListProps extends BaseDataComponentProps {
  items: DescriptionItem[];
  layout?: 'horizontal' | 'vertical';
  termWidth?: string;
  divider?: boolean;
}

// ============================================================================
// STAT TYPES
// ============================================================================

export interface StatProps extends BaseDataComponentProps {
  label: string;
  value: string | number;
  prefix?: React.ReactNode;
  suffix?: string;
  helpText?: string;
  trend?: Trend;
  size?: 'sm' | 'md' | 'lg';
  color?: string;
  icon?: React.ReactNode;
}

export interface StatGroupProps extends BaseDataComponentProps {
  children: React.ReactNode;
  columns?: number;
  spacing?: number;
  divider?: boolean;
}

// ============================================================================
// KPI TYPES
// ============================================================================

export interface KPIProps extends BaseDataComponentProps {
  title: string;
  value: string | number;
  target?: string | number;
  trend?: Trend;
  unit?: string;
  progress?: number;
  color?: string;
  icon?: React.ReactNode;
  status?: 'success' | 'warning' | 'danger' | 'info';
}

// ============================================================================
// METRIC CARD TYPES
// ============================================================================

export interface MetricCardProps extends BaseDataComponentProps {
  title: string;
  value: string | number;
  change?: number;
  changeLabel?: string;
  icon?: React.ReactNode;
  color?: string;
  isUpGood?: boolean;
  footer?: React.ReactNode;
}

// ============================================================================
// LEADERBOARD TYPES
// ============================================================================

export interface RankingItem {
  rank: number;
  name: string;
  avatar?: string;
  score: number;
  change?: number;
  metadata?: Record<string, any>;
}

export interface RankingProps extends BaseDataComponentProps {
  item: RankingItem;
  showChange?: boolean;
  showAvatar?: boolean;
  highlighted?: boolean;
  onClick?: () => void;
}

export interface LeaderboardProps extends BaseDataComponentProps {
  items: RankingItem[];
  showChange?: boolean;
  showAvatar?: boolean;
  highlightRank?: number;
  onItemClick?: (item: RankingItem) => void;
  maxItems?: number;
}

// ============================================================================
// ZOOM TYPES
// ============================================================================

export interface ZoomProps extends BaseDataComponentProps {
  children: React.ReactNode;
  minZoom?: number;
  maxZoom?: number;
  initialZoom?: number;
  step?: number;
  enablePinch?: boolean;
  enableWheel?: boolean;
  onZoomChange?: (zoom: number) => void;
}

// ============================================================================
// VIRTUAL SCROLL TYPES
// ============================================================================

export interface VirtualScrollProps<T = any> extends BaseDataComponentProps {
  items: T[];
  itemHeight: number;
  height: number;
  renderItem: (item: T, index: number) => React.ReactNode;
  overscan?: number;
  onScroll?: (scrollTop: number) => void;
}

// ============================================================================
// INFINITE SCROLL TYPES
// ============================================================================

export interface InfiniteScrollProps extends BaseDataComponentProps {
  children: React.ReactNode;
  hasMore: boolean;
  loadMore: () => void | Promise<void>;
  loader?: React.ReactNode;
  threshold?: number;
  scrollableTarget?: string;
  inverse?: boolean;
}

// ============================================================================
// QR CODE TYPES
// ============================================================================

export type QRCodeLevel = 'L' | 'M' | 'Q' | 'H';

export interface QRCodeProps extends BaseDataComponentProps {
  value: string;
  size?: number;
  level?: QRCodeLevel;
  bgColor?: string;
  fgColor?: string;
  includeMargin?: boolean;
  imageSettings?: {
    src: string;
    height: number;
    width: number;
    excavate?: boolean;
  };
}
