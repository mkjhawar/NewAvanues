import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface ScaleTransitionProps extends VisibilityTransitionProps {
  initialScale?: number;
  finalScale?: number;
  transformOrigin?: string;
}

/**
 * ScaleTransition - Scale up/down transition
 *
 * @example
 * ```tsx
 * <ScaleTransition show={isVisible} initialScale={0.8} transformOrigin="center">
 *   <div>Content</div>
 * </ScaleTransition>
 * ```
 */
export const ScaleTransition: React.FC<ScaleTransitionProps> = ({
  children,
  show = true,
  initialScale = 0,
  finalScale = 1,
  transformOrigin = 'center',
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
          initial={{ scale: initialScale }}
          animate={{ scale: finalScale }}
          exit={{ scale: initialScale }}
          transition={{
            duration: adjustedDuration,
            delay,
            ease: getEasing(easing),
          }}
          style={{ transformOrigin }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};
