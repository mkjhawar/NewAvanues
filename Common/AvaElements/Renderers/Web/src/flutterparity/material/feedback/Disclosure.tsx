import React, { useState } from 'react';
import { DisclosureProps } from './types';

export const Disclosure: React.FC<DisclosureProps> = ({
  title,
  children,
  defaultOpen = false,
  icon,
  className,
  onToggle,
}) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  const handleToggle = () => {
    const newState = !isOpen;
    setIsOpen(newState);
    onToggle?.(newState);
  };

  return (
    <div className={`disclosure ${className || ''}`} style={{ borderRadius: '4px' }}>
      <button
        onClick={handleToggle}
        aria-expanded={isOpen}
        className="disclosure-header"
        style={{
          width: '100%',
          padding: '12px 16px',
          backgroundColor: 'var(--disclosure-header-bg, #f5f5f5)',
          border: '1px solid var(--disclosure-border, #e0e0e0)',
          borderRadius: isOpen ? '4px 4px 0 0' : '4px',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          fontSize: '16px',
          fontWeight: 600,
          color: 'var(--text-primary, #212121)',
          transition: 'background-color 0.2s',
        }}
      >
        <span
          className="disclosure-chevron"
          style={{
            transform: isOpen ? 'rotate(90deg)' : 'rotate(0deg)',
            transition: 'transform 0.2s ease-out',
            fontSize: '12px',
            flexShrink: 0,
          }}
        >
          ▶
        </span>
        {icon && (
          <span className="disclosure-icon" style={{ flexShrink: 0 }}>
            {icon}
          </span>
        )}
        <span style={{ flex: 1, textAlign: 'left' }}>{title}</span>
      </button>
      <div
        className="disclosure-content"
        style={{
          maxHeight: isOpen ? '1000px' : '0',
          overflow: 'hidden',
          transition: 'max-height 0.3s ease-out',
          backgroundColor: 'var(--disclosure-content-bg, white)',
          border: isOpen ? '1px solid var(--disclosure-border, #e0e0e0)' : 'none',
          borderTop: 'none',
          borderRadius: '0 0 4px 4px',
        }}
      >
        <div style={{ padding: isOpen ? '16px' : '0 16px' }}>{children}</div>
      </div>
    </div>
  );
};
