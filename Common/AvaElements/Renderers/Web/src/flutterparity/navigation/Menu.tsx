/**
 * Menu Component
 * Dropdown menu with keyboard navigation support
 */

import React, { useState, useRef, useEffect } from 'react';
import { MenuItem, MenuPlacement } from './types';

export interface MenuProps {
  trigger: React.ReactNode;
  items: MenuItem[];
  placement?: MenuPlacement;
  onItemClick?: (item: MenuItem) => void;
  className?: string;
}

export const Menu: React.FC<MenuProps> = ({
  trigger,
  items,
  placement = 'bottom',
  onItemClick,
  className = '',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const menuRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        menuRef.current &&
        !menuRef.current.contains(event.target as Node) &&
        triggerRef.current &&
        !triggerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (!isOpen) return;

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        setFocusedIndex((prev) => (prev + 1) % items.length);
        break;
      case 'ArrowUp':
        event.preventDefault();
        setFocusedIndex((prev) => (prev - 1 + items.length) % items.length);
        break;
      case 'Enter':
        event.preventDefault();
        if (focusedIndex >= 0) {
          handleItemClick(items[focusedIndex]);
        }
        break;
      case 'Escape':
        event.preventDefault();
        setIsOpen(false);
        break;
    }
  };

  const handleItemClick = (item: MenuItem) => {
    if (item.disabled || item.divider) return;
    onItemClick?.(item);
    item.onClick?.();
    setIsOpen(false);
    setFocusedIndex(-1);
  };

  const getMenuPosition = (): React.CSSProperties => {
    const base: React.CSSProperties = {
      position: 'absolute',
      zIndex: 1000,
    };

    switch (placement) {
      case 'top':
        return { ...base, bottom: '100%', left: 0, marginBottom: '4px' };
      case 'left':
        return { ...base, right: '100%', top: 0, marginRight: '4px' };
      case 'right':
        return { ...base, left: '100%', top: 0, marginLeft: '4px' };
      case 'bottom':
      default:
        return { ...base, top: '100%', left: 0, marginTop: '4px' };
    }
  };

  return (
    <div className={`menu-container ${className}`} style={{ position: 'relative' }} onKeyDown={handleKeyDown}>
      <div
        ref={triggerRef}
        onClick={() => setIsOpen(!isOpen)}
        style={{ cursor: 'pointer' }}
        role="button"
        aria-haspopup="true"
        aria-expanded={isOpen}
      >
        {trigger}
      </div>

      {isOpen && (
        <div
          ref={menuRef}
          className="menu-dropdown"
          style={{
            ...getMenuPosition(),
            background: 'white',
            border: '1px solid #e0e0e0',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
            minWidth: '200px',
            padding: '8px 0',
          }}
          role="menu"
        >
          {items.map((item, index) => (
            <div key={item.id}>
              {item.divider ? (
                <div
                  style={{
                    height: '1px',
                    background: '#e0e0e0',
                    margin: '8px 0',
                  }}
                  role="separator"
                />
              ) : (
                <div
                  className={`menu-item ${focusedIndex === index ? 'focused' : ''}`}
                  onClick={() => handleItemClick(item)}
                  onMouseEnter={() => setFocusedIndex(index)}
                  style={{
                    padding: '10px 16px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '12px',
                    cursor: item.disabled ? 'not-allowed' : 'pointer',
                    opacity: item.disabled ? 0.5 : 1,
                    background: focusedIndex === index ? '#f5f5f5' : 'transparent',
                    transition: 'background 0.2s',
                  }}
                  role="menuitem"
                  aria-disabled={item.disabled}
                >
                  {item.icon && <span className="menu-item-icon">{item.icon}</span>}
                  <span className="menu-item-label">{item.label}</span>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
