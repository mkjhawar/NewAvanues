/**
 * AnimatedPadding Component
 * Animated padding changes
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedPaddingProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Padding on all sides (shorthand)
   */
  padding?: number | string;

  /**
   * Padding on top
   */
  paddingTop?: number | string;

  /**
   * Padding on right
   */
  paddingRight?: number | string;

  /**
   * Padding on bottom
   */
  paddingBottom?: number | string;

  /**
   * Padding on left
   */
  paddingLeft?: number | string;

  /**
   * Horizontal padding (left + right)
   */
  paddingHorizontal?: number | string;

  /**
   * Vertical padding (top + bottom)
   */
  paddingVertical?: number | string;
}

export const AnimatedPadding: React.FC<AnimatedPaddingProps> = ({
  children,
  padding,
  paddingTop,
  paddingRight,
  paddingBottom,
  paddingLeft,
  paddingHorizontal,
  paddingVertical,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();

  // Build padding object with priority: specific > horizontal/vertical > all
  const animatedStyles: Record<string, any> = {};

  if (padding !== undefined) {
    animatedStyles.padding = padding;
  }

  if (paddingVertical !== undefined) {
    animatedStyles.paddingTop = paddingVertical;
    animatedStyles.paddingBottom = paddingVertical;
  }

  if (paddingHorizontal !== undefined) {
    animatedStyles.paddingLeft = paddingHorizontal;
    animatedStyles.paddingRight = paddingHorizontal;
  }

  // Specific values override shorthand
  if (paddingTop !== undefined) animatedStyles.paddingTop = paddingTop;
  if (paddingRight !== undefined) animatedStyles.paddingRight = paddingRight;
  if (paddingBottom !== undefined) animatedStyles.paddingBottom = paddingBottom;
  if (paddingLeft !== undefined) animatedStyles.paddingLeft = paddingLeft;

  return (
    <motion.div
      animate={animatedStyles}
      transition={reducedMotion ? { duration: 0 } : transition}
      onAnimationComplete={onAnimationComplete}
      className={className}
    >
      {children}
    </motion.div>
  );
};
