/**
 * Popover Component - Flutter Parity Display
 *
 * Floating content anchored to a trigger element
 * Matches Flutter Popover/Tooltip behavior
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React, { useState, useRef, useEffect } from 'react';
import { BaseDisplayProps } from './types';

export interface PopoverProps extends BaseDisplayProps {
  /** Trigger element */
  trigger: React.ReactNode;
  /** Content to show in popover */
  content: React.ReactNode;
  /** Placement relative to trigger */
  placement?: 'top' | 'bottom' | 'left' | 'right';
  /** Open/close state (controlled) */
  open?: boolean;
  /** Callback when open state changes */
  onOpenChange?: (open: boolean) => void;
  /** Trigger mode */
  triggerMode?: 'click' | 'hover';
  /** Close on outside click */
  closeOnOutsideClick?: boolean;
  /** Offset from trigger */
  offset?: number;
}

export const Popover: React.FC<PopoverProps> = ({
  trigger,
  content,
  placement = 'bottom',
  open: controlledOpen,
  onOpenChange,
  triggerMode = 'click',
  closeOnOutsideClick = true,
  offset = 8,
  className,
  accessibilityLabel,
}) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const isControlled = controlledOpen !== undefined;
  const isOpen = isControlled ? controlledOpen : internalOpen;

  const triggerRef = useRef<HTMLDivElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);

  const setOpen = (value: boolean) => {
    if (!isControlled) {
      setInternalOpen(value);
    }
    onOpenChange?.(value);
  };

  useEffect(() => {
    if (!closeOnOutsideClick || !isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        triggerRef.current &&
        popoverRef.current &&
        !triggerRef.current.contains(event.target as Node) &&
        !popoverRef.current.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen, closeOnOutsideClick]);

  const handleTriggerClick = () => {
    if (triggerMode === 'click') {
      setOpen(!isOpen);
    }
  };

  const handleMouseEnter = () => {
    if (triggerMode === 'hover') {
      setOpen(true);
    }
  };

  const handleMouseLeave = () => {
    if (triggerMode === 'hover') {
      setOpen(false);
    }
  };

  const getPopoverStyle = (): React.CSSProperties => {
    const base: React.CSSProperties = {
      position: 'absolute',
      zIndex: 1000,
      backgroundColor: 'white',
      borderRadius: 8,
      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
      padding: 12,
    };

    switch (placement) {
      case 'top':
        return { ...base, bottom: `calc(100% + ${offset}px)`, left: '50%', transform: 'translateX(-50%)' };
      case 'bottom':
        return { ...base, top: `calc(100% + ${offset}px)`, left: '50%', transform: 'translateX(-50%)' };
      case 'left':
        return { ...base, right: `calc(100% + ${offset}px)`, top: '50%', transform: 'translateY(-50%)' };
      case 'right':
        return { ...base, left: `calc(100% + ${offset}px)`, top: '50%', transform: 'translateY(-50%)' };
      default:
        return base;
    }
  };

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <div
        ref={triggerRef}
        onClick={handleTriggerClick}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        style={{ display: 'inline-block' }}
      >
        {trigger}
      </div>
      {isOpen && (
        <div
          ref={popoverRef}
          className={className}
          role="tooltip"
          aria-label={accessibilityLabel}
          style={getPopoverStyle()}
        >
          {content}
        </div>
      )}
    </div>
  );
};

export default Popover;
