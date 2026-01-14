import React from 'react';

export interface CircleAvatarProps {
  child?: React.ReactNode;
  backgroundImage?: string;
  backgroundColor?: string;
  foregroundColor?: string;
  radius?: number;
  minRadius?: number;
  maxRadius?: number;
  onError?: (error: Error) => void;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * CircleAvatar - Circular avatar image
 *
 * A circle that represents a user with an image, icon, or initials.
 */
export const CircleAvatar: React.FC<CircleAvatarProps> = ({
  child,
  backgroundImage,
  backgroundColor = '#BDBDBD',
  foregroundColor = '#FFFFFF',
  radius = 20,
  minRadius,
  maxRadius,
  onError,
  className = '',
  style = {},
}) => {
  const [imageError, setImageError] = React.useState(false);

  // Calculate actual radius respecting min/max constraints
  let actualRadius = radius;
  if (minRadius !== undefined && actualRadius < minRadius) {
    actualRadius = minRadius;
  }
  if (maxRadius !== undefined && actualRadius > maxRadius) {
    actualRadius = maxRadius;
  }

  const diameter = actualRadius * 2;

  const handleImageError = () => {
    setImageError(true);
    const error = new Error(`Failed to load avatar image: ${backgroundImage}`);
    onError?.(error);
  };

  // Reset error state when image changes
  React.useEffect(() => {
    setImageError(false);
  }, [backgroundImage]);

  const containerStyle: React.CSSProperties = {
    width: diameter,
    height: diameter,
    borderRadius: '50%',
    backgroundColor,
    color: foregroundColor,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
    flexShrink: 0,
    position: 'relative',
    fontSize: actualRadius * 0.8,
    fontWeight: 500,
    ...style,
  };

  return (
    <div className={className} style={containerStyle} role="img" aria-label="Avatar">
      {/* Background image */}
      {backgroundImage && !imageError && (
        <img
          src={backgroundImage}
          alt="Avatar"
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            position: 'absolute',
            top: 0,
            left: 0,
          }}
          onError={handleImageError}
        />
      )}

      {/* Child content (icon, initials, etc.) */}
      {(!backgroundImage || imageError) && child && (
        <div
          style={{
            position: 'relative',
            zIndex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {child}
        </div>
      )}

      {/* Default person icon if no image or child */}
      {(!backgroundImage || imageError) && !child && (
        <svg
          width={actualRadius * 1.2}
          height={actualRadius * 1.2}
          viewBox="0 0 24 24"
          fill="currentColor"
          style={{ position: 'relative', zIndex: 1 }}
        >
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
        </svg>
      )}
    </div>
  );
};

/**
 * Helper function to create CircleAvatar with initials
 */
export const CircleAvatarWithInitials = (
  initials: string,
  options?: Partial<CircleAvatarProps>
): React.ReactElement => (
  <CircleAvatar {...options}>
    <span style={{ fontSize: 'inherit', fontWeight: 'inherit' }}>{initials}</span>
  </CircleAvatar>
);

/**
 * Helper function to create CircleAvatar with icon
 */
export const CircleAvatarWithIcon = (
  icon: React.ReactNode,
  options?: Partial<CircleAvatarProps>
): React.ReactElement => <CircleAvatar {...options}>{icon}</CircleAvatar>;
