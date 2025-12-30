import React from 'react';

export interface IndexedStackProps {
  children: React.ReactNode[];
  index: number;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * IndexedStack - Stack with indexed visibility
 *
 * Displays only one child at a time based on the current index.
 * All children remain in the DOM but are hidden when not active.
 */
export const IndexedStack: React.FC<IndexedStackProps> = ({
  children,
  index,
  className = '',
  style = {},
}) => {
  const validIndex = Math.max(0, Math.min(index, children.length - 1));

  return (
    <div
      className={className}
      style={{
        position: 'relative',
        width: '100%',
        height: '100%',
        ...style,
      }}
    >
      {React.Children.map(children, (child, i) => (
        <div
          key={i}
          style={{
            display: i === validIndex ? 'block' : 'none',
            width: '100%',
            height: '100%',
          }}
          role="tabpanel"
          aria-hidden={i !== validIndex}
        >
          {child}
        </div>
      ))}
    </div>
  );
};
