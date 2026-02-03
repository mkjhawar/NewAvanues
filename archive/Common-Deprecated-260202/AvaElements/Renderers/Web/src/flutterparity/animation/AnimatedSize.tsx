/**
 * AnimatedSize Component
 * Animated width and height changes
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedSizeProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Target width (in pixels, percentage, or 'auto')
   */
  width?: number | string;

  /**
   * Target height (in pixels, percentage, or 'auto')
   */
  height?: number | string;

  /**
   * Minimum width
   */
  minWidth?: number | string;

  /**
   * Maximum width
   */
  maxWidth?: number | string;

  /**
   * Minimum height
   */
  minHeight?: number | string;

  /**
   * Maximum height
   */
  maxHeight?: number | string;

  /**
   * How to clip content that overflows
   * @default 'hidden'
   */
  overflow?: 'visible' | 'hidden' | 'scroll' | 'auto';

  /**
   * Alignment of content during size changes
   * @default 'center'
   */
  alignment?: 'topLeft' | 'topCenter' | 'topRight' | 'centerLeft' | 'center' | 'centerRight' | 'bottomLeft' | 'bottomCenter' | 'bottomRight';
}

const alignmentToFlexbox = (alignment: string) => {
  const map: Record<string, { alignItems: string; justifyContent: string }> = {
    topLeft: { alignItems: 'flex-start', justifyContent: 'flex-start' },
    topCenter: { alignItems: 'flex-start', justifyContent: 'center' },
    topRight: { alignItems: 'flex-start', justifyContent: 'flex-end' },
    centerLeft: { alignItems: 'center', justifyContent: 'flex-start' },
    center: { alignItems: 'center', justifyContent: 'center' },
    centerRight: { alignItems: 'center', justifyContent: 'flex-end' },
    bottomLeft: { alignItems: 'flex-end', justifyContent: 'flex-start' },
    bottomCenter: { alignItems: 'flex-end', justifyContent: 'center' },
    bottomRight: { alignItems: 'flex-end', justifyContent: 'flex-end' },
  };
  return map[alignment] || map.center;
};

export const AnimatedSize: React.FC<AnimatedSizeProps> = ({
  children,
  width,
  height,
  minWidth,
  maxWidth,
  minHeight,
  maxHeight,
  overflow = 'hidden',
  alignment = 'center',
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();
  const flexAlignment = alignmentToFlexbox(alignment);

  const animatedStyles = {
    width,
    height,
    minWidth,
    maxWidth,
    minHeight,
    maxHeight,
  };

  // Remove undefined values
  const cleanStyles = Object.fromEntries(
    Object.entries(animatedStyles).filter(([_, v]) => v !== undefined)
  );

  return (
    <motion.div
      animate={cleanStyles}
      transition={reducedMotion ? { duration: 0 } : transition}
      onAnimationComplete={onAnimationComplete}
      className={className}
      style={{
        overflow,
        display: 'flex',
        ...flexAlignment,
        willChange: 'width, height',
      }}
    >
      {children}
    </motion.div>
  );
};
