/**
 * AnimatedPositioned Component
 * Animated position changes (absolute positioning)
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedPositionedProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Position from top edge (in pixels or percentage)
   */
  top?: number | string;

  /**
   * Position from left edge (in pixels or percentage)
   */
  left?: number | string;

  /**
   * Position from right edge (in pixels or percentage)
   */
  right?: number | string;

  /**
   * Position from bottom edge (in pixels or percentage)
   */
  bottom?: number | string;

  /**
   * Width of the positioned element
   */
  width?: number | string;

  /**
   * Height of the positioned element
   */
  height?: number | string;

  /**
   * Z-index for stacking order
   */
  zIndex?: number;
}

export const AnimatedPositioned: React.FC<AnimatedPositionedProps> = ({
  children,
  top,
  left,
  right,
  bottom,
  width,
  height,
  zIndex,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();

  const animatedStyles = {
    top,
    left,
    right,
    bottom,
    width,
    height,
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
        position: 'absolute',
        zIndex,
        willChange: 'top, left, right, bottom',
      }}
    >
      {children}
    </motion.div>
  );
};
