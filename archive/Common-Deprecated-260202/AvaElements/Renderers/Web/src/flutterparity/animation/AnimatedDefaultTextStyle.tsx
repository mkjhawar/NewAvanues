/**
 * AnimatedDefaultTextStyle Component
 * Animated text style changes
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedDefaultTextStyleProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Font size (in pixels or rem)
   */
  fontSize?: number | string;

  /**
   * Font weight (100-900 or 'normal', 'bold', etc.)
   */
  fontWeight?: number | string;

  /**
   * Text color
   */
  color?: string;

  /**
   * Letter spacing
   */
  letterSpacing?: number | string;

  /**
   * Line height
   */
  lineHeight?: number | string;

  /**
   * Text alignment
   */
  textAlign?: 'left' | 'center' | 'right' | 'justify';

  /**
   * Text decoration
   */
  textDecoration?: string;

  /**
   * Text transform
   */
  textTransform?: 'none' | 'uppercase' | 'lowercase' | 'capitalize';

  /**
   * Font family
   */
  fontFamily?: string;
}

export const AnimatedDefaultTextStyle: React.FC<AnimatedDefaultTextStyleProps> = ({
  children,
  fontSize,
  fontWeight,
  color,
  letterSpacing,
  lineHeight,
  textAlign,
  textDecoration,
  textTransform,
  fontFamily,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();

  const animatedStyles = {
    fontSize,
    fontWeight,
    color,
    letterSpacing,
    lineHeight,
    textAlign,
    textDecoration,
    textTransform,
    fontFamily,
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
    >
      {children}
    </motion.div>
  );
};
