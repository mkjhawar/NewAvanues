import React from 'react';
import { AnimatedErrorProps } from './types';

export const AnimatedError: React.FC<AnimatedErrorProps> = ({
  size = 48,
  color = '#F44336',
  strokeWidth = 3,
  delay = 0,
  className,
}) => {
  return (
    <>
      <style>
        {`
          @keyframes error-circle-draw {
            0% {
              stroke-dasharray: 0 100;
            }
            100% {
              stroke-dasharray: 100 100;
            }
          }

          @keyframes error-x-draw {
            0% {
              stroke-dasharray: 0 100;
            }
            100% {
              stroke-dasharray: 100 100;
            }
          }

          .animated-error-circle {
            stroke-dasharray: 0 100;
            animation: error-circle-draw 0.4s ease-out forwards;
          }

          .animated-error-x {
            stroke-dasharray: 0 100;
            animation: error-x-draw 0.3s ease-out forwards;
          }
        `}
      </style>
      <svg
        width={size}
        height={size}
        viewBox="0 0 24 24"
        className={className}
        role="img"
        aria-label="Error"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          className="animated-error-circle"
          style={{ animationDelay: `${delay}s` }}
        />
        <path
          d="M8 8l8 8"
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          className="animated-error-x"
          style={{ animationDelay: `${delay + 0.4}s` }}
        />
        <path
          d="M16 8l-8 8"
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          className="animated-error-x"
          style={{ animationDelay: `${delay + 0.4}s` }}
        />
      </svg>
    </>
  );
};
