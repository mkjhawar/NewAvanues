import React, { useState, useRef, useEffect } from 'react';

export interface PopupMenuItemProps {
  value: string;
  label: React.ReactNode;
  onTap?: () => void;
  enabled?: boolean;
  icon?: React.ReactNode;
}

export interface PopupMenuButtonProps {
  items: PopupMenuItemProps[];
  onSelected?: (value: string) => void;
  icon?: React.ReactNode;
  tooltip?: string;
  initialValue?: string;
  enabled?: boolean;
  offset?: { x: number; y: number };
  className?: string;
  style?: React.CSSProperties;
}

/**
 * PopupMenuButton - Button that shows popup menu
 *
 * Displays a button that, when pressed, shows a menu with selectable items.
 */
export const PopupMenuButton: React.FC<PopupMenuButtonProps> = ({
  items,
  onSelected,
  icon,
  tooltip,
  initialValue,
  enabled = true,
  offset = { x: 0, y: 0 },
  className = '',
  style = {},
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState(initialValue);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close menu when clicking outside
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        menuRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        buttonRef.current &&
        !buttonRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  // Close menu on Escape
  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen]);

  const handleButtonClick = () => {
    if (enabled) {
      setIsOpen(!isOpen);
    }
  };

  const handleItemClick = (item: PopupMenuItemProps) => {
    if (item.enabled !== false) {
      setSelectedValue(item.value);
      setIsOpen(false);
      item.onTap?.();
      onSelected?.(item.value);
      buttonRef.current?.focus();
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent, item: PopupMenuItemProps) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleItemClick(item);
    }
  };

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <button
        ref={buttonRef}
        onClick={handleButtonClick}
        disabled={!enabled}
        title={tooltip}
        className={className}
        style={{
          padding: '8px',
          border: 'none',
          borderRadius: '4px',
          backgroundColor: 'transparent',
          cursor: enabled ? 'pointer' : 'not-allowed',
          opacity: enabled ? 1 : 0.5,
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          ...style,
        }}
        aria-haspopup="true"
        aria-expanded={isOpen}
      >
        {icon || (
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" />
          </svg>
        )}
      </button>

      {isOpen && (
        <div
          ref={menuRef}
          style={{
            position: 'absolute',
            top: `calc(100% + ${offset.y}px)`,
            left: offset.x,
            minWidth: '200px',
            backgroundColor: '#fff',
            border: '1px solid #ddd',
            borderRadius: '4px',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
            zIndex: 1000,
            overflow: 'hidden',
          }}
          role="menu"
        >
          {items.map((item, index) => (
            <div
              key={item.value}
              onClick={() => handleItemClick(item)}
              onKeyDown={(e) => handleKeyDown(e, item)}
              tabIndex={item.enabled !== false ? 0 : -1}
              role="menuitem"
              aria-disabled={item.enabled === false}
              style={{
                padding: '12px 16px',
                display: 'flex',
                alignItems: 'center',
                gap: '12px',
                cursor: item.enabled !== false ? 'pointer' : 'not-allowed',
                backgroundColor: selectedValue === item.value ? '#f5f5f5' : '#fff',
                opacity: item.enabled === false ? 0.5 : 1,
                transition: 'background-color 0.2s',
              }}
              onMouseEnter={(e) => {
                if (item.enabled !== false) {
                  e.currentTarget.style.backgroundColor = '#f5f5f5';
                }
              }}
              onMouseLeave={(e) => {
                if (selectedValue !== item.value) {
                  e.currentTarget.style.backgroundColor = '#fff';
                }
              }}
            >
              {item.icon && <div style={{ flexShrink: 0 }}>{item.icon}</div>}
              <div style={{ flex: 1 }}>{item.label}</div>
              {selectedValue === item.value && (
                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z" />
                </svg>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
