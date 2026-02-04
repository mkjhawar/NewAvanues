import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface PositionedTransitionProps extends VisibilityTransitionProps {
  initialPosition?: { x: number | string; y: number | string };
  finalPosition?: { x: number | string; y: number | string };
}

/**
 * PositionedTransition - Position change transition
 *
 * @example
 * ```tsx
 * <PositionedTransition
 *   show={isVisible}
 *   initialPosition={{ x: -100, y: 50 }}
 *   finalPosition={{ x: 0, y: 0 }}
 * >
 *   <div>Content</div>
 * </PositionedTransition>
 * ```
 */
export const PositionedTransition: React.FC<PositionedTransitionProps> = ({
  children,
  show = true,
  initialPosition = { x: 0, y: 0 },
  finalPosition = { x: 0, y: 0 },
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={initialPosition}
          animate={finalPosition}
          exit={initialPosition}
          transition={{
            duration: adjustedDuration,
            delay,
            ease: getEasing(easing),
          }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};
