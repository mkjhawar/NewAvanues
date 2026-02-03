/**
 * Flutter Parity FittedBox Component
 * React implementation of Flutter's FittedBox widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { FittedBoxProps, BoxFit, Clip } from './types';
import { boxFitToStyles } from './helpers';

/**
 * FittedBox - Scales and positions child within itself
 *
 * Scales and positions its child within itself according to fit strategy.
 * Maintains aspect ratio (except for BoxFit.Fill).
 *
 * Web implementation: CSS object-fit or flexbox scaling
 * iOS equivalent: aspectRatio() + scaledToFit/Fill in SwiftUI
 * Android equivalent: ImageView scaleType
 *
 * Features:
 * - Multiple fit strategies (fill, contain, cover, etc.)
 * - Maintains aspect ratio
 * - Configurable alignment
 * - Optional clipping
 *
 * BoxFit strategies:
 * - Fill: Distort to fill exactly (no aspect ratio)
 * - Contain: Scale to fit inside (maintains aspect ratio)
 * - Cover: Scale to cover (maintains aspect ratio, may clip)
 * - FitWidth: Match width, overflow/underflow height
 * - FitHeight: Match height, overflow/underflow width
 * - None: No scaling
 * - ScaleDown: Like None, but scale down if too large
 */
export const FittedBox: React.FC<FittedBoxProps> = ({
  fit = BoxFit.Contain,
  alignment,
  clipBehavior = Clip.None,
  child,
  key,
  testID,
}) => {
  const containerStyle: React.CSSProperties = {
    display: 'flex',
    width: '100%',
    height: '100%',
    overflow: clipBehavior === Clip.None ? 'visible' : 'hidden',
    boxSizing: 'border-box',
  };

  const childContainerStyle: React.CSSProperties = {
    ...boxFitToStyles(fit, alignment),
  };

  return (
    <div style={containerStyle} data-testid={testID} key={key}>
      <div style={childContainerStyle}>{child}</div>
    </div>
  );
};
