import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export type AlignmentValue =
  | 'flex-start'
  | 'center'
  | 'flex-end'
  | 'stretch'
  | 'baseline';

export interface AlignTransitionProps extends VisibilityTransitionProps {
  initialAlign?: AlignmentValue;
  finalAlign?: AlignmentValue;
  axis?: 'horizontal' | 'vertical';
}

/**
 * AlignTransition - Alignment change transition
 *
 * Animates changes in flexbox alignment.
 *
 * @example
 * ```tsx
 * <AlignTransition
 *   show={isVisible}
 *   initialAlign="flex-start"
 *   finalAlign="center"
 *   axis="vertical"
 * >
 *   <div>Content</div>
 * </AlignTransition>
 * ```
 */
export const AlignTransition: React.FC<AlignTransitionProps> = ({
  children,
  show = true,
  initialAlign = 'flex-start',
  finalAlign = 'center',
  axis = 'vertical',
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  const getAlignStyles = (align: AlignmentValue) => {
    if (axis === 'vertical') {
      return {
        display: 'flex',
        flexDirection: 'column' as const,
        alignItems: align,
      };
    } else {
      return {
        display: 'flex',
        flexDirection: 'row' as const,
        justifyContent: align,
      };
    }
  };

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={getAlignStyles(initialAlign)}
          animate={getAlignStyles(finalAlign)}
          exit={getAlignStyles(initialAlign)}
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
