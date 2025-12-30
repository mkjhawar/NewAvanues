/**
 * Flutter Parity Align and Center Components
 * React implementation of Flutter's Align and Center widgets
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { AlignProps, CenterProps } from './types';
import { alignmentToJustifyContent, alignmentToAlignItems } from './helpers';

/**
 * Align - Aligns child within itself
 *
 * A widget that aligns its child within itself and optionally sizes itself
 * based on the child's size.
 *
 * Web implementation: Flexbox with justify-content/align-items
 * iOS equivalent: frame() modifier with alignment in SwiftUI
 * Android equivalent: FrameLayout with gravity
 *
 * Features:
 * - 2D alignment using x,y coordinates (-1 to 1)
 * - Optional width/height factors for size-to-child
 * - Fills available space by default
 */
export const Align: React.FC<AlignProps> = ({
  alignment,
  widthFactor,
  heightFactor,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    display: 'flex',
    justifyContent: alignmentToJustifyContent(alignment),
    alignItems: alignmentToAlignItems(alignment),

    // Size behavior based on factors
    width:
      widthFactor !== undefined
        ? 'fit-content' // Size to child width * factor
        : '100%', // Fill available width

    height:
      heightFactor !== undefined
        ? 'fit-content' // Size to child height * factor
        : '100%', // Fill available height

    boxSizing: 'border-box',
  };

  // If factors are provided, wrap child to apply scaling
  const childElement =
    widthFactor !== undefined || heightFactor !== undefined ? (
      <div
        style={{
          transform: `scale(${widthFactor ?? 1}, ${heightFactor ?? 1})`,
          transformOrigin: 'center',
        }}
      >
        {child}
      </div>
    ) : (
      child
    );

  return (
    <div style={style} data-testid={testID} key={key}>
      {childElement}
    </div>
  );
};

/**
 * Center - Centers child within itself
 *
 * A widget that centers its child within itself. Shorthand for Align with
 * alignment = Alignment.center.
 *
 * Web implementation: Flexbox with center alignment
 * iOS equivalent: frame() with .center alignment in SwiftUI
 * Android equivalent: FrameLayout with center gravity
 *
 * Features:
 * - Perfect centering (horizontally and vertically)
 * - Optional width/height factors
 * - Fills available space by default
 */
export const Center: React.FC<CenterProps> = ({
  widthFactor,
  heightFactor,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',

    // Size behavior based on factors
    width: widthFactor !== undefined ? 'fit-content' : '100%',
    height: heightFactor !== undefined ? 'fit-content' : '100%',

    boxSizing: 'border-box',
  };

  // If factors are provided, wrap child to apply scaling
  const childElement =
    widthFactor !== undefined || heightFactor !== undefined ? (
      <div
        style={{
          transform: `scale(${widthFactor ?? 1}, ${heightFactor ?? 1})`,
          transformOrigin: 'center',
        }}
      >
        {child}
      </div>
    ) : (
      child
    );

  return (
    <div style={style} data-testid={testID} key={key}>
      {childElement}
    </div>
  );
};
