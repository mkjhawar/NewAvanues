import React, { useState, useEffect } from 'react';

export interface FadeInImageProps {
  src: string;
  placeholder?: React.ReactNode;
  fadeInDuration?: number;
  fadeInCurve?: string;
  width?: number | string;
  height?: number | string;
  fit?: 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
  alt?: string;
  className?: string;
  style?: React.CSSProperties;
  onLoad?: () => void;
  onError?: (error: Error) => void;
}

/**
 * FadeInImage - Image with fade-in effect
 *
 * Displays a placeholder while the image loads, then fades in the image
 * once it's fully loaded.
 */
export const FadeInImage: React.FC<FadeInImageProps> = ({
  src,
  placeholder,
  fadeInDuration = 300,
  fadeInCurve = 'ease-in',
  width,
  height,
  fit = 'cover',
  alt = '',
  className = '',
  style = {},
  onLoad,
  onError,
}) => {
  const [imageLoaded, setImageLoaded] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [imageSrc, setImageSrc] = useState<string | null>(null);

  useEffect(() => {
    setImageLoaded(false);
    setHasError(false);
    setImageSrc(null);

    const img = new Image();

    img.onload = () => {
      setImageSrc(src);
      setImageLoaded(true);
      onLoad?.();
    };

    img.onerror = () => {
      setHasError(true);
      const error = new Error(`Failed to load image: ${src}`);
      onError?.(error);
    };

    img.src = src;

    return () => {
      img.onload = null;
      img.onerror = null;
    };
  }, [src, onLoad, onError]);

  const containerStyle: React.CSSProperties = {
    position: 'relative',
    width: width ?? '100%',
    height: height ?? '100%',
    overflow: 'hidden',
    ...style,
  };

  const imageStyle: React.CSSProperties = {
    width: '100%',
    height: '100%',
    objectFit: fit,
    opacity: imageLoaded ? 1 : 0,
    transition: `opacity ${fadeInDuration}ms ${fadeInCurve}`,
  };

  const placeholderStyle: React.CSSProperties = {
    position: 'absolute',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f0f0f0',
    opacity: imageLoaded ? 0 : 1,
    transition: `opacity ${fadeInDuration}ms ${fadeInCurve}`,
    pointerEvents: imageLoaded ? 'none' : 'auto',
  };

  return (
    <div className={className} style={containerStyle}>
      {/* Placeholder */}
      <div style={placeholderStyle}>
        {hasError ? (
          <div style={{ textAlign: 'center', color: '#999' }}>
            <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
              <path d="M21 5v6.59l-3-3.01-4 4.01-4-4-4 4-3-3.01V5c0-1.1.9-2 2-2h14c1.1 0 2 .9 2 2zm-3 6.42l3 3.01V19c0 1.1-.9 2-2 2H5c-1.1 0-2-.9-2-2v-6.58l3 2.99 4-4 4 4 4-3.99z"/>
            </svg>
            <div style={{ marginTop: '8px', fontSize: '12px' }}>Failed to load</div>
          </div>
        ) : placeholder ? (
          placeholder
        ) : (
          <div style={{ textAlign: 'center', color: '#ccc' }}>
            <svg
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            <div style={{ marginTop: '8px', fontSize: '12px' }}>Loading...</div>
          </div>
        )}
      </div>

      {/* Image */}
      {imageSrc && !hasError && (
        <img
          src={imageSrc}
          alt={alt}
          style={imageStyle}
        />
      )}
    </div>
  );
};
