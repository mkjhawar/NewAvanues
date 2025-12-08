import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { getAdjustedDuration, getEasing, EasingFunction } from './types';

export interface AnimatedCrossFadeProps {
  showFirst: boolean;
  firstChild: React.ReactNode;
  secondChild: React.ReactNode;
  duration?: number;
  delay?: number;
  easing?: EasingFunction;
  className?: string;
}

/**
 * AnimatedCrossFade - Cross-fade between two children
 *
 * @example
 * ```tsx
 * <AnimatedCrossFade
 *   showFirst={isFirstVisible}
 *   firstChild={<div>First</div>}
 *   secondChild={<div>Second</div>}
 *   duration={0.3}
 * />
 * ```
 */
export const AnimatedCrossFade: React.FC<AnimatedCrossFadeProps> = ({
  showFirst,
  firstChild,
  secondChild,
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  return (
    <div className={className} style={{ position: 'relative' }}>
      <AnimatePresence mode="wait">
        {showFirst ? (
          <motion.div
            key="first"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{
              duration: adjustedDuration,
              delay,
              ease: getEasing(easing),
            }}
          >
            {firstChild}
          </motion.div>
        ) : (
          <motion.div
            key="second"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{
              duration: adjustedDuration,
              delay,
              ease: getEasing(easing),
            }}
          >
            {secondChild}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};
