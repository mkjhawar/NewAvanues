/**
 * Display Component Types - Flutter Parity
 *
 * Common types and interfaces for display components
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

export interface BaseDisplayProps {
  /** Custom class name */
  className?: string;
  /** Accessibility label */
  accessibilityLabel?: string;
}

export interface ImageSource {
  /** Image URL */
  url: string;
  /** Alternative text */
  alt?: string;
  /** Thumbnail URL for lazy loading */
  thumbnail?: string;
}

export type Size = 'small' | 'medium' | 'large';
export type Variant = 'circle' | 'rounded' | 'square';
export type AnimationType = 'pulse' | 'wave' | false;
