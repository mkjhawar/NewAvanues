import React, { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { PopupProps } from './types';

export const Popup: React.FC<PopupProps> = ({
  isOpen,
  onClose,
  children,
  title,
  showBackdrop = true,
  closeOnBackdropClick = true,
  className,
}) => {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (closeOnBackdropClick && e.target === e.currentTarget) {
      onClose();
    }
  };

  return createPortal(
    <div
      className="popup-backdrop"
      onClick={handleBackdropClick}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: showBackdrop
          ? 'var(--backdrop-color, rgba(0, 0, 0, 0.5))'
          : 'transparent',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '16px',
      }}
    >
      <div
        className={`popup-content ${className || ''}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? 'popup-title' : undefined}
        style={{
          backgroundColor: 'var(--popup-bg, white)',
          borderRadius: '8px',
          boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
          maxWidth: '600px',
          maxHeight: '90vh',
          overflow: 'auto',
          position: 'relative',
        }}
      >
        {title && (
          <div
            id="popup-title"
            style={{
              padding: '20px 24px',
              borderBottom: '1px solid var(--divider-color, #e0e0e0)',
              fontSize: '20px',
              fontWeight: 600,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <span>{title}</span>
            <button
              onClick={onClose}
              aria-label="Close"
              style={{
                background: 'none',
                border: 'none',
                fontSize: '28px',
                cursor: 'pointer',
                color: 'var(--text-secondary, #666)',
                padding: '0 4px',
                lineHeight: 1,
              }}
            >
              ×
            </button>
          </div>
        )}
        <div style={{ padding: title ? '24px' : '0' }}>{children}</div>
      </div>
    </div>,
    document.body
  );
};
