import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface RotationTransitionProps extends VisibilityTransitionProps {
  initialRotation?: number;
  finalRotation?: number;
  transformOrigin?: string;
}

/**
 * RotationTransition - Rotation transition
 *
 * @example
 * ```tsx
 * <RotationTransition show={isVisible} initialRotation={-180} finalRotation={0}>
 *   <div>Content</div>
 * </RotationTransition>
 * ```
 */
export const RotationTransition: React.FC<RotationTransitionProps> = ({
  children,
  show = true,
  initialRotation = -180,
  finalRotation = 0,
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
          initial={{ rotate: initialRotation }}
          animate={{ rotate: finalRotation }}
          exit={{ rotate: initialRotation }}
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
