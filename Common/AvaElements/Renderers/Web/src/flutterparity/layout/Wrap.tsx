/**
 * Flutter Parity Wrap Component
 * React implementation of Flutter's Wrap widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import {
  WrapProps,
  WrapDirection,
  WrapAlignment,
  WrapCrossAlignment,
  VerticalDirection,
} from './types';
import { spacingToValue, wrapAlignmentToCSS, crossAxisAlignmentToCSS } from './helpers';

/**
 * Wrap - Multi-line flex layout
 *
 * A widget that displays its children in multiple horizontal or vertical runs.
 * When there's not enough space for a child, Wrap creates a new run adjacent
 * to the existing children in the cross axis.
 *
 * Web implementation: CSS Flexbox with flex-wrap
 * iOS equivalent: Custom layout (iOS 16+) or manual calculation
 * Android equivalent: FlexboxLayout with flexWrap
 *
 * Features:
 * - Automatic wrapping when content doesn't fit
 * - Main axis alignment (start, end, center, space-between, etc.)
 * - Cross axis alignment (start, end, center)
 * - Run alignment (alignment of wrapped rows/columns)
 * - Configurable spacing between items and runs
 * - Horizontal or vertical direction
 * - RTL support
 *
 * Common use cases:
 * 1. Tag clouds: Wrap(children: [Chip(), Chip(), ...])
 * 2. Responsive grids: Wrap with fixed-width children
 * 3. Toolbar buttons that wrap on narrow screens
 */
export const Wrap: React.FC<WrapProps> = ({
  direction = WrapDirection.Horizontal,
  alignment = WrapAlignment.Start,
  spacing,
  runSpacing,
  runAlignment = WrapAlignment.Start,
  crossAxisAlignment = WrapCrossAlignment.Start,
  verticalDirection = VerticalDirection.Down,
  children,
  key,
  testID,
}) => {
  const isHorizontal = direction === WrapDirection.Horizontal;
  const isReversed = !isHorizontal && verticalDirection === VerticalDirection.Up;

  const mainSpacing = spacingToValue(spacing);
  const crossSpacing = spacingToValue(runSpacing);

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isReversed
      ? 'column-reverse'
      : isHorizontal
      ? 'row'
      : 'column',
    flexWrap: 'wrap',

    // Main axis alignment (within each run)
    justifyContent: wrapAlignmentToCSS(alignment),

    // Cross axis alignment (items within a run)
    alignItems:
      crossAxisAlignment === WrapCrossAlignment.Start
        ? 'flex-start'
        : crossAxisAlignment === WrapCrossAlignment.End
        ? 'flex-end'
        : 'center',

    // Run alignment (alignment of wrapped lines)
    alignContent: wrapAlignmentToCSS(runAlignment),

    // Spacing between items
    gap: isHorizontal
      ? `${crossSpacing}px ${mainSpacing}px` // row-gap column-gap
      : `${mainSpacing}px ${crossSpacing}px`, // row-gap column-gap

    boxSizing: 'border-box',
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {children}
    </div>
  );
};
