import React from 'react';
import { AnimatedCheckProps } from './types';

export const AnimatedCheck: React.FC<AnimatedCheckProps> = ({
  size = 48,
  color = '#4CAF50',
  strokeWidth = 3,
  delay = 0,
  className,
}) => {
  return (
    <>
      <style>
        {`
          @keyframes circle-draw {
            0% {
              stroke-dasharray: 0 100;
            }
            100% {
              stroke-dasharray: 100 100;
            }
          }

          @keyframes check-draw {
            0% {
              stroke-dasharray: 0 100;
            }
            100% {
              stroke-dasharray: 100 100;
            }
          }

          .animated-check-circle {
            stroke-dasharray: 0 100;
            animation: circle-draw 0.4s ease-out forwards;
          }

          .animated-check-path {
            stroke-dasharray: 0 100;
            animation: check-draw 0.3s ease-out forwards;
          }
        `}
      </style>
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        className={className}
        role="img"
        aria-label="Success"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          className="animated-check-circle"
          style={{ animationDelay: `${delay}s` }}
        />
        <path
          d="M6 12l4 4 8-8"
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeLinejoin="round"
          className="animated-check-path"
          style={{ animationDelay: `${delay + 0.4}s` }}
        />
      </svg>
    </>
  );
};
