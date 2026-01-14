/**
 * Flutter Parity ConstrainedBox Component
 * React implementation of Flutter's ConstrainedBox widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { BoxConstraints } from './types';

/**
 * ConstrainedBox Props
 */
export interface ConstrainedBoxProps {
  /** Additional constraints to impose on the child */
  constraints: BoxConstraints;
  /** Child element to constrain */
  child: React.ReactNode;
  /** Optional key for React reconciliation */
  key?: string;
  /** Optional test ID for testing */
  testID?: string;
}

/**
 * ConstrainedBox - Imposes additional constraints on child
 *
 * A widget that imposes additional constraints on its child. The constraints
 * from the parent and the ConstrainedBox are combined, with the tighter
 * constraint winning.
 *
 * Web implementation: CSS min/max width/height
 * iOS equivalent: frame() modifier with min/max dimensions in SwiftUI
 * Android equivalent: ConstraintLayout with dimension constraints
 *
 * Features:
 * - Minimum and maximum width/height constraints
 * - Combines with parent constraints (tighter wins)
 * - Supports infinity for unbounded constraints
 *
 * Example:
 * ```tsx
 * // Minimum size constraint
 * <ConstrainedBox
 *   constraints={{
 *     minWidth: 200,
 *     minHeight: 100
 *   }}
 * >
 *   <div>Content will be at least 200x100</div>
 * </ConstrainedBox>
 *
 * // Maximum size constraint
 * <ConstrainedBox
 *   constraints={{
 *     maxWidth: 300,
 *     maxHeight: 200
 *   }}
 * >
 *   <img src="large_image.jpg" />
 * </ConstrainedBox>
 *
 * // Exact size (tight constraints)
 * <ConstrainedBox
 *   constraints={{
 *     minWidth: 150,
 *     maxWidth: 150,
 *     minHeight: 150,
 *     maxHeight: 150
 *   }}
 * >
 *   <div>Content will be exactly 150x150</div>
 * </ConstrainedBox>
 * ```
 */
export const ConstrainedBox: React.FC<ConstrainedBoxProps> = ({
  constraints,
  child,
  key,
  testID,
}) => {
  const {
    minWidth = 0,
    maxWidth = Infinity,
    minHeight = 0,
    maxHeight = Infinity,
  } = constraints;

  const style: React.CSSProperties = {
    // Width constraints
    minWidth: minWidth > 0 ? `${minWidth}px` : undefined,
    maxWidth: isFinite(maxWidth) ? `${maxWidth}px` : undefined,

    // Height constraints
    minHeight: minHeight > 0 ? `${minHeight}px` : undefined,
    maxHeight: isFinite(maxHeight) ? `${maxHeight}px` : undefined,

    // Box sizing to ensure padding/border don't affect size
    boxSizing: 'border-box',

    // Display inline-block to respect width/height on inline elements
    display: 'inline-block',
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};

/**
 * Factory methods for common constraint patterns
 */

/**
 * Create ConstrainedBox with tight constraints (exact size)
 *
 * @param width - Exact width
 * @param height - Exact height
 * @param child - Child element
 * @returns ConstrainedBox component
 */
export const ConstrainedBoxTight = (
  width: number,
  height: number,
  child: React.ReactNode
): React.ReactElement => (
  <ConstrainedBox
    constraints={{
      minWidth: width,
      maxWidth: width,
      minHeight: height,
      maxHeight: height,
    }}
  >
    {child}
  </ConstrainedBox>
);

/**
 * Create ConstrainedBox with expand constraints (fill available space)
 *
 * @param child - Child element
 * @returns ConstrainedBox component
 */
export const ConstrainedBoxExpand = (
  child: React.ReactNode
): React.ReactElement => (
  <ConstrainedBox
    constraints={{
      minWidth: Infinity,
      minHeight: Infinity,
      maxWidth: Infinity,
      maxHeight: Infinity,
    }}
  >
    {child}
  </ConstrainedBox>
);

/**
 * Create ConstrainedBox with loose constraints (no minimum)
 *
 * @param maxWidth - Maximum width
 * @param maxHeight - Maximum height
 * @param child - Child element
 * @returns ConstrainedBox component
 */
export const ConstrainedBoxLoose = (
  maxWidth: number,
  maxHeight: number,
  child: React.ReactNode
): React.ReactElement => (
  <ConstrainedBox
    constraints={{
      minWidth: 0,
      maxWidth,
      minHeight: 0,
      maxHeight,
    }}
  >
    {child}
  </ConstrainedBox>
);

export default ConstrainedBox;
