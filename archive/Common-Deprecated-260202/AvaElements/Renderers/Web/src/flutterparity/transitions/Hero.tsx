import React from 'react';
import { motion } from 'framer-motion';
import { getAdjustedDuration, getEasing, EasingFunction } from './types';

export interface HeroProps {
  layoutId: string;
  children: React.ReactNode;
  duration?: number;
  easing?: EasingFunction;
  className?: string;
}

/**
 * Hero - Shared element transition
 *
 * Uses Framer Motion's layoutId for smooth transitions between views.
 * Multiple elements with the same layoutId will animate between each other.
 *
 * @example
 * ```tsx
 * // In list view
 * <Hero layoutId="item-1">
 *   <img src="thumb.jpg" />
 * </Hero>
 *
 * // In detail view
 * <Hero layoutId="item-1">
 *   <img src="full.jpg" />
 * </Hero>
 * ```
 */
export const Hero: React.FC<HeroProps> = ({
  layoutId,
  children,
  duration = 0.3,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  return (
    <motion.div
      layoutId={layoutId}
      transition={{
        duration: adjustedDuration,
        ease: getEasing(easing),
      }}
      className={className}
    >
      {children}
    </motion.div>
  );
};
