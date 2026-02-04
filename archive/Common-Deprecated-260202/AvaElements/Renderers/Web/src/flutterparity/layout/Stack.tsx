/**
 * Flutter Parity Stack Component
 * React implementation of Flutter's Stack and Positioned widgets
 *
 * @since 3.0.0-flutter-parity-web
 */

import React from 'react';
import { StackProps, PositionedProps, StackFit, Clip } from './types';
import { alignmentToJustifyContent, alignmentToAlignItems } from './helpers';

/**
 * Stack - Layered positioning layout
 *
 * A widget that positions its children relative to the edges of its box.
 * Children are painted in order (first child at bottom, last child on top).
 *
 * Web implementation: CSS Grid with grid-area or absolute positioning
 * iOS equivalent: ZStack in SwiftUI
 * Android equivalent: FrameLayout
 *
 * Features:
 * - Layered children (z-index based on order)
 * - Absolute positioning with Positioned children
 * - Default alignment for non-positioned children
 * - Optional clipping behavior
 */
export const Stack: React.FC<StackProps> = ({
  alignment,
  fit = StackFit.Loose,
  clipBehavior = Clip.HardEdge,
  children,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    position: 'relative',
    display: 'flex',
    justifyContent: alignmentToJustifyContent(alignment),
    alignItems: alignmentToAlignItems(alignment),

    // Fit behavior
    ...(fit === StackFit.Expand
      ? { width: '100%', height: '100%' }
      : fit === StackFit.Passthrough
      ? {}
      : { width: 'fit-content', height: 'fit-content' }),

    // Clip behavior
    overflow: clipBehavior === Clip.None ? 'visible' : 'hidden',
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {children}
    </div>
  );
};

/**
 * Positioned - Absolute positioning within Stack
 *
 * Controls where a child of a Stack is positioned. Must be a direct child of Stack.
 * A Positioned widget must be a descendant of a Stack.
 *
 * Web implementation: CSS position: absolute
 * iOS equivalent: position() modifier in SwiftUI ZStack
 * Android equivalent: FrameLayout.LayoutParams with gravity
 *
 * Features:
 * - Absolute positioning (top, right, bottom, left)
 * - Optional width/height overrides
 * - Responsive to Stack's size
 */
export const Positioned: React.FC<PositionedProps> = ({
  top,
  right,
  bottom,
  left,
  width,
  height,
  child,
  key,
  testID,
}) => {
  const style: React.CSSProperties = {
    position: 'absolute',
    top: top !== undefined ? `${top}px` : undefined,
    right: right !== undefined ? `${right}px` : undefined,
    bottom: bottom !== undefined ? `${bottom}px` : undefined,
    left: left !== undefined ? `${left}px` : undefined,
    width: width !== undefined ? `${width}px` : undefined,
    height: height !== undefined ? `${height}px` : undefined,
  };

  return (
    <div style={style} data-testid={testID} key={key}>
      {child}
    </div>
  );
};

/**
 * Positioned.fill - Fills the entire Stack
 */
export const PositionedFill: React.FC<{ child: React.ReactNode }> = ({ child }) => (
  <Positioned top={0} right={0} bottom={0} left={0} child={child} />
);

/**
 * Positioned.directional - RTL-aware positioning
 */
export const PositionedDirectional: React.FC<{
  start?: number;
  end?: number;
  top?: number;
  bottom?: number;
  width?: number;
  height?: number;
  child: React.ReactNode;
  textDirection?: 'ltr' | 'rtl';
}> = ({ start, end, top, bottom, width, height, child, textDirection = 'ltr' }) => {
  const isRTL = textDirection === 'rtl';
  const left = isRTL ? end : start;
  const right = isRTL ? start : end;

  return (
    <Positioned
      top={top}
      right={right}
      bottom={bottom}
      left={left}
      width={width}
      height={height}
      child={child}
    />
  );
};
