/**
 * AnimatedAlign Component
 * Animated alignment changes using flexbox
 */

import React from 'react';
import { motion } from 'framer-motion';
import { BaseAnimationProps, getTransitionConfig, prefersReducedMotion } from './types';

export interface AnimatedAlignProps extends BaseAnimationProps {
  children: React.ReactNode;

  /**
   * Alignment of child within container
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

  /**
   * Width of the container
   * @default '100%'
   */
  width?: number | string;

  /**
   * Height of the container
   * @default '100%'
   */
  height?: number | string;

  /**
   * Width factor (0-1) for child element
   * If set, child will take this percentage of container width
   */
  widthFactor?: number;

  /**
   * Height factor (0-1) for child element
   * If set, child will take this percentage of container height
   */
  heightFactor?: number;
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

export const AnimatedAlign: React.FC<AnimatedAlignProps> = ({
  children,
  alignment = 'center',
  width = '100%',
  height = '100%',
  widthFactor,
  heightFactor,
  duration = 0.3,
  delay = 0,
  curve = 'easeInOut',
  onAnimationComplete,
  className,
}) => {
  const transition = getTransitionConfig(curve, duration, delay);
  const reducedMotion = prefersReducedMotion();
  const flexAlignment = alignmentToFlexbox(alignment);

  // Build child wrapper styles if factors are provided
  const childWrapperStyles: React.CSSProperties = {};
  if (widthFactor !== undefined) {
    childWrapperStyles.width = `${widthFactor * 100}%`;
  }
  if (heightFactor !== undefined) {
    childWrapperStyles.height = `${heightFactor * 100}%`;
  }

  const hasFactors = widthFactor !== undefined || heightFactor !== undefined;

  return (
    <motion.div
      animate={{
        width,
        height,
        ...flexAlignment,
      }}
      transition={reducedMotion ? { duration: 0 } : transition}
      onAnimationComplete={onAnimationComplete}
      className={className}
      style={{
        display: 'flex',
      }}
    >
      {hasFactors ? (
        <div style={childWrapperStyles}>
          {children}
        </div>
      ) : (
        children
      )}
    </motion.div>
  );
};
