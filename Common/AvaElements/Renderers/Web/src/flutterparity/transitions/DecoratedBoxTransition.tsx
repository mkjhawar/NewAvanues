import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { VisibilityTransitionProps, getAdjustedDuration, getEasing } from './types';

export interface DecoratedBoxTransitionProps extends VisibilityTransitionProps {
  initialStyles?: React.CSSProperties;
  finalStyles?: React.CSSProperties;
}

/**
 * DecoratedBoxTransition - Decorated box animation
 *
 * Animates CSS properties like background, border, shadow, etc.
 *
 * @example
 * ```tsx
 * <DecoratedBoxTransition
 *   show={isVisible}
 *   initialStyles={{
 *     backgroundColor: '#fff',
 *     borderRadius: '0px',
 *     boxShadow: 'none'
 *   }}
 *   finalStyles={{
 *     backgroundColor: '#f0f0f0',
 *     borderRadius: '12px',
 *     boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
 *   }}
 * >
 *   <div>Content</div>
 * </DecoratedBoxTransition>
 * ```
 */
export const DecoratedBoxTransition: React.FC<DecoratedBoxTransitionProps> = ({
  children,
  show = true,
  initialStyles = {},
  finalStyles = {},
  duration = 0.3,
  delay = 0,
  easing = 'easeInOut',
  className,
}) => {
  const adjustedDuration = getAdjustedDuration(duration);

  // Convert CSS properties to camelCase for Framer Motion
  const convertStyles = (styles: React.CSSProperties) => {
    const converted: any = {};
    Object.entries(styles).forEach(([key, value]) => {
      converted[key] = value;
    });
    return converted;
  };

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={convertStyles(initialStyles)}
          animate={convertStyles(finalStyles)}
          exit={convertStyles(initialStyles)}
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
