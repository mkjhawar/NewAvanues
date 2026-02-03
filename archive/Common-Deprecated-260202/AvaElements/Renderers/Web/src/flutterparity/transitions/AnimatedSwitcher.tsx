import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { TransitionMode, getAdjustedDuration, getEasing, EasingFunction } from './types';

export interface AnimatedSwitcherProps {
  children: React.ReactNode;
  switchKey: string | number;
  duration?: number;
  delay?: number;
  mode?: TransitionMode;
  easing?: EasingFunction;
  transitionType?: 'fade' | 'scale' | 'slide';
  className?: string;
}

/**
 * AnimatedSwitcher - Animated child swap
 *
 * Uses key-based switching to animate between different children.
 *
 * @example
 * ```tsx
 * <AnimatedSwitcher switchKey={currentId} mode="wait" transitionType="fade">
 *   <ComponentBasedOnId id={currentId} />
 * </AnimatedSwitcher>
 * ```
 */
export const AnimatedSwitcher: React.FC<AnimatedSwitcherProps> = ({
  children,
  switchKey,
  duration = 0.3,
  delay = 0,
  mode = 'wait',
  easing = 'easeInOut',
  transitionType = 'fade',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  const getVariants = () => {
    switch (transitionType) {
      case 'fade':
        return {
          initial: { opacity: 0 },
          animate: { opacity: 1 },
          exit: { opacity: 0 },
        };
      case 'scale':
        return {
          initial: { opacity: 0, scale: 0.8 },
          animate: { opacity: 1, scale: 1 },
          exit: { opacity: 0, scale: 0.8 },
        };
      case 'slide':
        return {
          initial: { opacity: 0, x: -20 },
          animate: { opacity: 1, x: 0 },
          exit: { opacity: 0, x: 20 },
        };
      default:
        return {
          initial: { opacity: 0 },
          animate: { opacity: 1 },
          exit: { opacity: 0 },
        };
    }
  };

  const variants = getVariants();

  return (
    <AnimatePresence mode={mode === 'sync' ? 'sync' : 'wait'}>
      <motion.div
        key={switchKey}
        initial={variants.initial}
        animate={variants.animate}
        exit={variants.exit}
        transition={{
          duration: adjustedDuration,
          delay,
          ease: getEasing(easing),
        }}
        className={className}
      >
        {children}
      </motion.div>
    </AnimatePresence>
  );
};
