/**
 * Flutter Parity Container Component
 * React implementation of Flutter's Container widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { ContainerProps } from './types';
import {
  sizeToCSS,
  spacingToCSS,
  boxDecorationToCSS,
  alignmentToJustifyContent,
  alignmentToAlignItems,
} from './helpers';

/**
 * Container - A convenience widget that combines common styling widgets
 *
 * A Container is a box that can have padding, margins, borders, background color,
 * and constraints. It can also align and size itself based on its child.
 *
 * Web implementation: div with CSS styling
 * iOS equivalent: Custom view with modifiers in SwiftUI
 * Android equivalent: FrameLayout with styling
 *
 * Features:
 * - Padding and margin
 * - Background color, borders, shadows
 * - Size constraints
 * - Child alignment
 * - Border radius
 * - Gradients
 */
export const Container: React.FC<ContainerProps> = ({
  width,
  height,
  padding,
  margin,
  decoration,
  alignment,
  constraints,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    // Size
    width: sizeToCSS(width),
    height: sizeToCSS(height),

    // Spacing
    padding: spacingToCSS(padding),
    margin: spacingToCSS(margin),

    // Decoration
    ...boxDecorationToCSS(decoration),

    // Alignment (if has child)
    ...(child && alignment
      ? {
          display: 'flex',
          justifyContent: alignmentToJustifyContent(alignment),
          alignItems: alignmentToAlignItems(alignment),
        }
      : {}),

    // Constraints
    ...(constraints
      ? {
          minWidth: constraints.minWidth > 0 ? `${constraints.minWidth}px` : undefined,
          maxWidth: isFinite(constraints.maxWidth) ? `${constraints.maxWidth}px` : undefined,
          minHeight: constraints.minHeight > 0 ? `${constraints.minHeight}px` : undefined,
          maxHeight: isFinite(constraints.maxHeight) ? `${constraints.maxHeight}px` : undefined,
        }
      : {}),

    // Box-sizing for predictable sizing
    boxSizing: 'border-box',
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};
