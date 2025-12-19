/**
 * AnimatedScale Component
 * Animated scale transformations
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedScaleProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Scale factor (uniform scaling)
   * 0.5 = 50%, 1 = 100%, 2 = 200%
   */
  scale?: number;

  /**
   * Scale factor on X axis
   */
  scaleX?: number;

  /**
   * Scale factor on Y axis
   */
  scaleY?: number;

  /**
   * Transform origin point
   * @default 'center'
   */
  transformOrigin?:
    | 'center'
    | 'top'
    | 'bottom'
    | 'left'
    | 'right'
    | 'top left'
    | 'top right'
    | 'bottom left'
    | 'bottom right'
    | string;

  /**
   * Alignment within parent container
   * Determines where the scaled element is positioned
   * @default 'center'
   */
  alignment?:
    | 'topLeft'
    | 'topCenter'
    | 'topRight'
    | 'centerLeft'
    | 'center'
    | 'centerRight'
    | 'bottomLeft'
    | 'bottomCenter'
    | 'bottomRight';
}

const alignmentToFlexbox = (alignment: string) => {
  const map: Record<string, { alignItems: string; justifyContent: string }> = {
    topLeft: { alignItems: 'flex-start', justifyContent: 'flex-start' },
    topCenter: { alignItems: 'flex-start', justifyContent: 'center' },
    topRight: { alignItems: 'flex-start', justifyContent: 'flex-end' },
    centerLeft: { alignItems: 'center', justifyContent: 'flex-start' },
    center: { alignItems: 'center', justifyContent: 'center' },
    centerRight: { alignItems: 'center', justifyContent: 'flex-end' },
    bottomLeft: { alignItems: 'flex-end', justifyContent: 'flex-start' },
    bottomCenter: { alignItems: 'flex-end', justifyContent: 'center' },
    bottomRight: { alignItems: 'flex-end', justifyContent: 'flex-end' },
  };
  return map[alignment] || map.center;
};

export const AnimatedScale: React.FC<AnimatedScaleProps> = ({
  children,
  scale,
  scaleX,
  scaleY,
  transformOrigin = 'center',
  alignment = 'center',
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();
  const flexAlignment = alignmentToFlexbox(alignment);

  // Build scale transform
  const animatedStyles: Record<string, any> = {};

  if (scale !== undefined) {
    animatedStyles.scale = scale;
  } else {
    if (scaleX !== undefined) animatedStyles.scaleX = scaleX;
    if (scaleY !== undefined) animatedStyles.scaleY = scaleY;
  }

  return (
    <motion.div
      style={{
        display: 'flex',
        width: '100%',
        height: '100%',
        ...flexAlignment,
      }}
      className={className}
    >
      <motion.div
        animate={animatedStyles}
        transition={reducedMotion ? { duration: 0 } : transition}
        onAnimationComplete={onAnimationComplete}
        style={{
          transformOrigin,
          willChange: 'transform',
        }}
      >
        {children}
      </motion.div>
    </motion.div>
  );
};
