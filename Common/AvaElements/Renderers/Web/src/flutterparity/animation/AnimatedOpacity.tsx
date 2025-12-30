/**
 * AnimatedOpacity Component
 * Fade in/out animation
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedOpacityProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Target opacity value (0-1)
   * 0 = fully transparent, 1 = fully opaque
   */
  opacity: number;

  /**
   * Whether to remove element from DOM when opacity is 0
   * @default false
   */
  removeWhenHidden?: boolean;
}

export const AnimatedOpacity: React.FC<AnimatedOpacityProps> = ({
  children,
  opacity,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
  removeWhenHidden = false,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();

  // Clamp opacity between 0 and 1
  const clampedOpacity = Math.max(0, Math.min(1, opacity));

  if (removeWhenHidden && clampedOpacity === 0) {
    return null;
  }

  return (
    <motion.div
      animate={{ opacity: clampedOpacity }}
      transition={reducedMotion ? { duration: 0 } : transition}
      onAnimationComplete={onAnimationComplete}
      className={className}
      style={{ willChange: 'opacity' }}
    >
      {children}
    </motion.div>
  );
};
