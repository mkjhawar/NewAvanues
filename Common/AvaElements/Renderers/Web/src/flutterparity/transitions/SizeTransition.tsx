import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface SizeTransitionProps extends VisibilityTransitionProps {
  initialSize?: { width?: number | string; height?: number | string };
  finalSize?: { width?: number | string; height?: number | string };
  axis?: 'horizontal' | 'vertical' | 'both';
}

/**
 * SizeTransition - Size change transition
 *
 * @example
 * ```tsx
 * <SizeTransition
 *   show={isVisible}
 *   axis="vertical"
 *   initialSize={{ height: 0 }}
 *   finalSize={{ height: 'auto' }}
 * >
 *   <div>Content</div>
 * </SizeTransition>
 * ```
 */
export const SizeTransition: React.FC<SizeTransitionProps> = ({
  children,
  show = true,
  initialSize = {},
  finalSize = {},
  axis = 'both',
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  const getInitial = () => {
    const initial: any = {};
    if (axis === 'horizontal' || axis === 'both') {
      initial.width = initialSize.width ?? 0;
    }
    if (axis === 'vertical' || axis === 'both') {
      initial.height = initialSize.height ?? 0;
    }
    return initial;
  };

  const getFinal = () => {
    const final: any = {};
    if (axis === 'horizontal' || axis === 'both') {
      final.width = finalSize.width ?? 'auto';
    }
    if (axis === 'vertical' || axis === 'both') {
      final.height = finalSize.height ?? 'auto';
    }
    return final;
  };

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={getInitial()}
          animate={getFinal()}
          exit={getInitial()}
          transition={{
            duration: adjustedDuration,
            delay,
            ease: getEasing(easing),
          }}
          style={{ overflow: 'hidden' }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
};
