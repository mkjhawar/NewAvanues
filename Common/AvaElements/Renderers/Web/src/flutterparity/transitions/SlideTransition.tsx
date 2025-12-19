import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, TransitionDirection, getAdjustedDuration, getEasing } from './types';

export interface SlideTransitionProps extends VisibilityTransitionProps {
  direction?: TransitionDirection;
  distance?: number | string;
}

/**
 * SlideTransition - Slide in from a direction
 *
 * @example
 * ```tsx
 * <SlideTransition show={isVisible} direction="left" distance={100}>
 *   <div>Content</div>
 * </SlideTransition>
 * ```
 */
export const SlideTransition: React.FC<SlideTransitionProps> = ({
  children,
  show = true,
  direction = 'left',
  distance = '100%',
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  const getOffset = () => {
    const offset = typeof distance === 'number' ? `${distance}px` : distance;
    switch (direction) {
      case 'left':
        return { x: `-${offset}` };
      case 'right':
        return { x: offset };
      case 'up':
        return { y: `-${offset}` };
      case 'down':
        return { y: offset };
    }
  };

  const initialOffset = getOffset();

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={initialOffset}
          animate={{ x: 0, y: 0 }}
          exit={initialOffset}
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
