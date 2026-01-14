import React from 'react';

export interface FloatingMenuItem {
  icon: string;
  label?: string;
  backgroundColor?: string;
  onClick?: () => void;
}

export interface FloatingMenuProps {
  isOpen: boolean;
  items: FloatingMenuItem[];
  mainIcon?: string;
  mainIconOpen?: string;
  position?: 'bottom-right' | 'bottom-left' | 'bottom-center' | 'top-right' | 'top-left';
  spacing?: number;
  backgroundColor?: string;
  iconColor?: string;
  onToggle?: () => void;
}

export const FloatingMenu: React.FC<FloatingMenuProps> = ({
  isOpen,
  items,
  mainIcon = '+',
  mainIconOpen = '×',
  position = 'bottom-right',
  spacing = 16,
  backgroundColor = '#007AFF',
  iconColor = '#fff',
  onToggle
}) => {
  const positionStyles: Record<string, React.CSSProperties> = {
    'bottom-right': { bottom: 24, right: 24 },
    'bottom-left': { bottom: 24, left: 24 },
    'bottom-center': { bottom: 24, left: '50%', transform: 'translateX(-50%)' },
    'top-right': { top: 24, right: 24 },
    'top-left': { top: 24, left: 24 }
  };

  return (
    <div style={{ position: 'fixed', zIndex: 1000, ...positionStyles[position] }}>
      {/* Menu Items */}
      {items.map((item, index) => (
        <button
          key={index}
          onClick={item.onClick}
          style={{
            position: 'absolute',
            bottom: isOpen ? (index + 1) * (56 + spacing) : 0,
            left: '50%',
            transform: 'translateX(-50%)',
            width: 48,
            height: 48,
            borderRadius: '50%',
            border: 'none',
            backgroundColor: item.backgroundColor || '#5856D6',
            color: iconColor,
            fontSize: 20,
            cursor: 'pointer',
            opacity: isOpen ? 1 : 0,
            transition: 'all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
          }}
        >
          {item.icon}
        </button>
      ))}

      {/* Main FAB */}
      <button
        onClick={onToggle}
        style={{
          width: 56,
          height: 56,
          borderRadius: '50%',
          border: 'none',
          backgroundColor,
          color: iconColor,
          fontSize: 28,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
          transform: isOpen ? 'rotate(45deg)' : 'rotate(0deg)',
          transition: 'transform 0.3s'
        }}
      >
        {isOpen ? mainIconOpen : mainIcon}
      </button>
    </div>
  );
};

export default FloatingMenu;
