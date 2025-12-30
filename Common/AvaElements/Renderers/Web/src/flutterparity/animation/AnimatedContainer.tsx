/**
 * AnimatedContainer Component
 * Container with animated style properties
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedContainerProps extends BaseAnimationProps {
  children: React.ReactNode;

  // Style properties that can be animated
  width?: number | string;
  height?: number | string;
  padding?: number | string;
  margin?: number | string;
  backgroundColor?: string;
  borderRadius?: number | string;
  border?: string;
  boxShadow?: string;
  opacity?: number;
  transform?: string;

  // Layout properties
  display?: string;
  flexDirection?: string;
  alignItems?: string;
  justifyContent?: string;
  gap?: number | string;
}

export const AnimatedContainer: React.FC<AnimatedContainerProps> = ({
  children,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
  // Extract style props
  width,
  height,
  padding,
  margin,
  backgroundColor,
  borderRadius,
  border,
  boxShadow,
  opacity,
  transform,
  display,
  flexDirection,
  alignItems,
  justifyContent,
  gap,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();

  // Style object with all animated properties
  const animatedStyles = {
    width,
    height,
    padding,
    margin,
    backgroundColor,
    borderRadius,
    border,
    boxShadow,
    opacity,
    transform,
    display,
    flexDirection,
    alignItems,
    justifyContent,
    gap,
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
