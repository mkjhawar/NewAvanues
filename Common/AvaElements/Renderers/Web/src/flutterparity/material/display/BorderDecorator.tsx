import React from 'react';

export interface BorderSides {
  top?: boolean;
  right?: boolean;
  bottom?: boolean;
  left?: boolean;
}

export interface BorderDecoratorProps {
  width?: number;
  color?: string;
  style?: 'solid' | 'dashed' | 'dotted' | 'double' | 'none';
  radius?: number;
  sides?: BorderSides;
  children?: React.ReactNode;
}

export const BorderDecorator: React.FC<BorderDecoratorProps> = ({
  width = 1,
  color = '#000',
  style = 'solid',
  radius = 0,
  sides = { top: true, right: true, bottom: true, left: true },
  children
}) => {
  const borderValue = `${width}px ${style} ${color}`;

  return (
    <div style={{
      borderTop: sides.top ? borderValue : 'none',
      borderRight: sides.right ? borderValue : 'none',
      borderBottom: sides.bottom ? borderValue : 'none',
      borderLeft: sides.left ? borderValue : 'none',
      borderRadius: radius
    }}>
      {children}
    </div>
  );
};

export default BorderDecorator;
