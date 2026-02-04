/**
 * ProgressStepper Component
 * Multi-step progress indicator with horizontal and vertical orientations
 */

import React from 'react';
import { Step, Orientation } from './types';

export interface ProgressStepperProps {
  steps: Step[];
  currentStep: number;
  orientation?: Orientation;
  showLabels?: boolean;
  onStepClick?: (index: number) => void;
  className?: string;
}

export const ProgressStepper: React.FC<ProgressStepperProps> = ({
  steps,
  currentStep,
  orientation = 'horizontal',
  showLabels = true,
  onStepClick,
  className = '',
}) => {
  const isHorizontal = orientation === 'horizontal';

  const getStepStatus = (index: number): 'completed' | 'current' | 'error' | 'upcoming' => {
    if (steps[index].error) return 'error';
    if (steps[index].completed) return 'completed';
    if (index === currentStep) return 'current';
    return 'upcoming';
  };

  const getStepColor = (status: string): string => {
    switch (status) {
      case 'completed':
        return '#4caf50';
      case 'current':
        return '#2196f3';
      case 'error':
        return '#f44336';
      case 'upcoming':
      default:
        return '#e0e0e0';
    }
  };

  return (
    <div
      className={`progress-stepper ${orientation} ${className}`}
      style={{
        display: 'flex',
        flexDirection: isHorizontal ? 'row' : 'column',
        gap: isHorizontal ? '0' : '16px',
        width: isHorizontal ? '100%' : 'auto',
      }}
    >
      {steps.map((step, index) => {
        const status = getStepStatus(index);
        const color = getStepColor(status);
        const isClickable = onStepClick && (status === 'completed' || index < currentStep);

        return (
          <div
            key={index}
            className={`progress-step ${status}`}
            style={{
              display: 'flex',
              flexDirection: isHorizontal ? 'column' : 'row',
              alignItems: isHorizontal ? 'center' : 'flex-start',
              flex: isHorizontal ? 1 : 'none',
              position: 'relative',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                width: isHorizontal ? '100%' : 'auto',
              }}
            >
              {/* Connector Line (before) */}
              {index > 0 && (
                <div
                  style={{
                    position: 'absolute',
                    [isHorizontal ? 'left' : 'top']: isHorizontal ? '0' : '16px',
                    [isHorizontal ? 'right' : 'bottom']: isHorizontal ? '50%' : 'auto',
                    [isHorizontal ? 'top' : 'left']: isHorizontal ? '16px' : '0',
                    [isHorizontal ? 'height' : 'width']: '2px',
                    [isHorizontal ? 'width' : 'height']: isHorizontal ? 'auto' : '32px',
                    background: getStepColor(getStepStatus(index - 1)),
                    zIndex: 0,
                  }}
                />
              )}

              {/* Step Circle */}
              <div
                onClick={() => isClickable && onStepClick?.(index)}
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  background: color,
                  border: status === 'current' ? `3px solid ${color}` : 'none',
                  boxShadow: status === 'current' ? `0 0 0 4px rgba(33, 150, 243, 0.2)` : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 600,
                  fontSize: '14px',
                  cursor: isClickable ? 'pointer' : 'default',
                  position: 'relative',
                  zIndex: 1,
                  flexShrink: 0,
                  transition: 'all 0.3s ease',
                }}
              >
                {step.icon ? (
                  step.icon
                ) : status === 'completed' ? (
                  '✓'
                ) : status === 'error' ? (
                  '✕'
                ) : (
                  index + 1
                )}
              </div>

              {/* Labels */}
              {showLabels && (
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px',
                    marginTop: isHorizontal ? '8px' : '0',
                    textAlign: isHorizontal ? 'center' : 'left',
                  }}
                >
                  <div
                    style={{
                      fontSize: '14px',
                      fontWeight: status === 'current' ? 600 : 400,
                      color: status === 'upcoming' ? '#999' : '#333',
                    }}
                  >
                    {step.label}
                  </div>
                  {step.description && (
                    <div
                      style={{
                        fontSize: '12px',
                        color: '#666',
                      }}
                    >
                      {step.description}
                    </div>
                  )}
                </div>
              )}

              {/* Connector Line (after) */}
              {index < steps.length - 1 && (
                <div
                  style={{
                    position: 'absolute',
                    [isHorizontal ? 'left' : 'top']: isHorizontal ? '50%' : '48px',
                    [isHorizontal ? 'right' : 'bottom']: isHorizontal ? '0' : 'auto',
                    [isHorizontal ? 'top' : 'left']: isHorizontal ? '16px' : '32px',
                    [isHorizontal ? 'height' : 'width']: '2px',
                    [isHorizontal ? 'width' : 'height']: isHorizontal ? 'auto' : 'calc(100% - 32px)',
                    background: status === 'completed' ? color : '#e0e0e0',
                    zIndex: 0,
                  }}
                />
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
