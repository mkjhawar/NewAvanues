/**
 * Flutter Parity Flexible Components (Expanded, Flexible, Spacer)
 * React implementation of Flutter's flex-based sizing widgets
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { ExpandedProps, FlexibleProps, SpacerProps, FlexFit } from './types';

/**
 * Expanded - Fills available space in flex layout
 *
 * A widget that expands a child of a Row, Column, or Flex so that the child
 * fills the available space along the main axis.
 *
 * Web implementation: CSS flex-grow
 * iOS equivalent: layoutPriority modifier in SwiftUI
 * Android equivalent: layout_weight in LinearLayout
 *
 * Features:
 * - Proportional space allocation via flex factor
 * - Must be direct child of Row/Column/Flex
 * - Forces child to fill allocated space
 *
 * IMPORTANT: Must be used within Row, Column, or Flex parent!
 */
export const Expanded: React.FC<ExpandedProps> = ({
  flex = 1,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    flexGrow: flex,
    flexShrink: 1,
    flexBasis: 0, // Critical for proper flex behavior
    minWidth: 0, // Prevent overflow issues
    minHeight: 0,
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};

/**
 * Flexible - Flexible space allocation in flex layout
 *
 * A widget that controls how a child of a Row, Column, or Flex flexes.
 * Unlike Expanded, Flexible allows the child to be smaller than the available space.
 *
 * Web implementation: CSS flex with configurable flex-basis
 * iOS equivalent: frame() with flexible constraints in SwiftUI
 * Android equivalent: layout_weight with wrap_content
 *
 * Features:
 * - FlexFit.Tight: Forces child to fill (like Expanded)
 * - FlexFit.Loose: Allows child to be smaller
 * - Configurable flex factor for proportional allocation
 *
 * IMPORTANT: Must be used within Row, Column, or Flex parent!
 */
export const Flexible: React.FC<FlexibleProps> = ({
  flex = 1,
  fit = FlexFit.Loose,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties =
    fit === FlexFit.Tight
      ? {
          // Tight fit - force fill like Expanded
          flexGrow: flex,
          flexShrink: 1,
          flexBasis: 0,
          minWidth: 0,
          minHeight: 0,
        }
      : {
          // Loose fit - allow smaller size
          flexGrow: flex,
          flexShrink: 1,
          flexBasis: 'auto', // Allow content-based sizing
        };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};

/**
 * Spacer - Creates flexible empty space
 *
 * Creates empty space that expands along the main axis of a Row, Column, or Flex.
 * Equivalent to Expanded with an empty child.
 *
 * Web implementation: CSS flex-grow with empty div
 * iOS equivalent: Spacer() in SwiftUI
 * Android equivalent: View with layout_weight in LinearLayout
 *
 * Features:
 * - Pushes siblings apart
 * - Configurable flex factor
 * - No child required
 *
 * IMPORTANT: Must be used within Row, Column, or Flex parent!
 */
export const Spacer: React.FC<SpacerProps> = ({ flex = 1, key, testID }) => {
  const style: React.CSSProperties = {
    flexGrow: flex,
    flexShrink: 1,
    flexBasis: 0,
    minWidth: 0,
    minHeight: 0,
  };

  return <div style={style} data-testid={testID} key={key} />;
};
