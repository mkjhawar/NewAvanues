import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface FadeTransitionProps extends VisibilityTransitionProps {
  initialOpacity?: number;
  finalOpacity?: number;
}

/**
 * FadeTransition - Fade in/out between states
 *
 * @example
 * ```tsx
 * <FadeTransition show={isVisible} duration={0.3}>
 *   <div>Content</div>
 * </FadeTransition>
 * ```
 */
export const FadeTransition: React.FC<FadeTransitionProps> = ({
  children,
  show = true,
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  initialOpacity = 0,
  finalOpacity = 1,
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: initialOpacity }}
          animate={{ opacity: finalOpacity }}
          exit={{ opacity: initialOpacity }}
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
