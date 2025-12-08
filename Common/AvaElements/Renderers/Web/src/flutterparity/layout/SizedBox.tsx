/**
 * Flutter Parity SizedBox Component
 * React implementation of Flutter's SizedBox widget
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { SizedBoxProps } from './types';
import { sizeToCSS } from './helpers';

/**
 * SizedBox - Fixed or constrained size box
 *
 * A box with a specified size. If given a child, forces the child to have
 * specific width/height. If no child, acts as spacing/constraint.
 *
 * Web implementation: div with fixed width/height
 * iOS equivalent: frame() modifier in SwiftUI
 * Android equivalent: View with fixed layout_width/layout_height
 *
 * Features:
 * - Fixed width and/or height
 * - Can be used as spacer (no child)
 * - expand() variant fills parent
 * - shrink() variant collapses to zero
 * - Respects parent constraints
 *
 * Common use cases:
 * 1. Fixed sizing: SizedBox(width: 200, height: 100, child: ...)
 * 2. Vertical spacer: SizedBox(height: 20)
 * 3. Horizontal spacer: SizedBox(width: 20)
 * 4. Fill parent: SizedBox.expand(child: ...)
 */
export const SizedBox: React.FC<SizedBoxProps> = ({
  width,
  height,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    width: sizeToCSS(width),
    height: sizeToCSS(height),
    boxSizing: 'border-box',
    flexShrink: 0, // Prevent shrinking in flex layouts
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};

/**
 * SizedBox.expand - Fills parent completely
 *
 * Creates a SizedBox that will become as large as its parent allows.
 */
export const SizedBoxExpand: React.FC<{ child?: React.ReactNode }> = ({ child }) => (
  <SizedBox
    width={{ type: 'fill' }}
    height={{ type: 'fill' }}
    child={child}
  />
);

/**
 * SizedBox.shrink - Collapses to zero size
 *
 * Creates a SizedBox that will become as small as possible (0x0).
 */
export const SizedBoxShrink: React.FC = () => (
  <SizedBox
    width={{ type: 'dp', value: 0 }}
    height={{ type: 'dp', value: 0 }}
  />
);

/**
 * SizedBox.square - Square box
 *
 * Creates a square SizedBox with the given dimension.
 */
export const SizedBoxSquare: React.FC<{
  dimension: number;
  child?: React.ReactNode;
}> = ({ dimension, child }) => (
  <SizedBox
    width={{ type: 'dp', value: dimension }}
    height={{ type: 'dp', value: dimension }}
    child={child}
  />
);
