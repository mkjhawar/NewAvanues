import React from 'react';

export interface CenterProps {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * Center - Center content in parent
 *
 * Centers its child both horizontally and vertically within the available space.
 */
export const Center: React.FC<CenterProps> = ({
  children,
  className = '',
  style = {},
}) => {
  return (
    <div
      className={className}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: '100%',
        height: '100%',
        ...style,
      }}
    >
      {children}
    </div>
  );
};
