/**
 * Image Loader Component
 *
 * React component for async image loading with optimization features.
 *
 * Features:
 * - Placeholder and error states
 * - Lazy loading with IntersectionObserver
 * - Responsive images (srcset)
 * - Image optimization hints
 * - Progressive loading
 * - Cache-aware loading
 * - Accessibility support
 *
 * @module ImageLoader
 * @since 2.1.0
 */

import React, { useState, useEffect, useRef, CSSProperties, ImgHTMLAttributes } from 'react';

/**
 * Image source configuration
 */
export interface ImageSource {
  /** Image URL */
  src: string;
  /** Image width (for srcset) */
  width?: number;
}

/**
 * Image loader props
 */
export interface ImageLoaderProps {
  /** Image source URL or array of sources for responsive images */
  src: string | ImageSource[];
  /** Alt text for accessibility */
  alt: string;
  /** Width in pixels */
  width?: number;
  /** Height in pixels */
  height?: number;
  /** Object fit CSS property */
  objectFit?: 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
  /** Placeholder image URL */
  placeholder?: string;
  /** Placeholder component */
  PlaceholderComponent?: React.ComponentType<PlaceholderProps>;
  /** Error component */
  ErrorComponent?: React.ComponentType<ErrorProps>;
  /** Enable lazy loading */
  lazy?: boolean;
  /** Intersection observer root margin */
  rootMargin?: string;
  /** Loading strategy */
  loading?: 'lazy' | 'eager';
  /** Decoding hint */
  decoding?: 'async' | 'auto' | 'sync';
  /** Cross-origin attribute */
  crossOrigin?: 'anonymous' | 'use-credentials';
  /** Referrer policy */
  referrerPolicy?: ImgHTMLAttributes<HTMLImageElement>['referrerPolicy'];
  /** Additional CSS classes */
  className?: string;
  /** Inline styles */
  style?: CSSProperties;
  /** Callback when image loads successfully */
  onLoad?: (event: React.SyntheticEvent<HTMLImageElement>) => void;
  /** Callback when image fails to load */
  onError?: (event: React.SyntheticEvent<HTMLImageElement>) => void;
  /** Callback when image enters viewport (lazy loading) */
  onIntersect?: () => void;
}

/**
 * Placeholder props
 */
export interface PlaceholderProps {
  width?: number;
  height?: number;
  style?: CSSProperties;
}

/**
 * Error props
 */
export interface ErrorProps {
  alt: string;
  width?: number;
  height?: number;
  style?: CSSProperties;
  retry: () => void;
}

/**
 * Default placeholder component
 */
const DefaultPlaceholder: React.FC<PlaceholderProps> = ({ width, height, style }) => {
  return (
    <div
      style={{
        width: width ? `${width}px` : '100%',
        height: height ? `${height}px` : '200px',
        backgroundColor: '#f0f0f0',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#999',
        fontSize: '14px',
        borderRadius: '4px',
        ...style,
      }}
    >
      Loading...
    </div>
  );
};

/**
 * Default error component
 */
const DefaultError: React.FC<ErrorProps> = ({ alt, width, height, style, retry }) => {
  return (
    <div
      style={{
        width: width ? `${width}px` : '100%',
        height: height ? `${height}px` : '200px',
        backgroundColor: '#ffebee',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#c62828',
        fontSize: '14px',
        borderRadius: '4px',
        border: '1px solid #ef5350',
        ...style,
      }}
    >
      <span style={{ marginBottom: '8px' }}>Failed to load image</span>
      <button
        onClick={retry}
        style={{
          padding: '6px 12px',
          backgroundColor: '#c62828',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '12px',
        }}
      >
        Retry
      </button>
    </div>
  );
};

/**
 * Image loading state
 */
enum LoadState {
  IDLE = 'idle',
  LOADING = 'loading',
  LOADED = 'loaded',
  ERROR = 'error',
}

/**
 * Image Loader Component
 *
 * @example
 * ```tsx
 * // Basic usage
 * <ImageLoader
 *   src="https://example.com/image.jpg"
 *   alt="Example image"
 *   width={400}
 *   height={300}
 * />
 *
 * // With lazy loading
 * <ImageLoader
 *   src="https://example.com/image.jpg"
 *   alt="Example image"
 *   lazy
 *   placeholder="https://example.com/placeholder.jpg"
 * />
 *
 * // Responsive images
 * <ImageLoader
 *   src={[
 *     { src: 'image-small.jpg', width: 400 },
 *     { src: 'image-medium.jpg', width: 800 },
 *     { src: 'image-large.jpg', width: 1200 },
 *   ]}
 *   alt="Responsive image"
 * />
 * ```
 */
export const ImageLoader: React.FC<ImageLoaderProps> = ({
  src,
  alt,
  width,
  height,
  objectFit = 'contain',
  placeholder,
  PlaceholderComponent = DefaultPlaceholder,
  ErrorComponent = DefaultError,
  lazy = false,
  rootMargin = '50px',
  loading = 'lazy',
  decoding = 'async',
  crossOrigin,
  referrerPolicy,
  className,
  style,
  onLoad,
  onError,
  onIntersect,
}) => {
  const [loadState, setLoadState] = useState<LoadState>(LoadState.IDLE);
  const [isInView, setIsInView] = useState(!lazy);
  const [retryCount, setRetryCount] = useState(0);
  const imgRef = useRef<HTMLImageElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Intersection Observer for lazy loading
  useEffect(() => {
    if (!lazy || isInView) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setIsInView(true);
            onIntersect?.();
            observer.disconnect();
          }
        });
      },
      {
        rootMargin,
      }
    );

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => {
      observer.disconnect();
    };
  }, [lazy, isInView, rootMargin, onIntersect]);

  // Handle image loading
  useEffect(() => {
    if (!isInView) {
      return;
    }

    setLoadState(LoadState.LOADING);
  }, [isInView]);

  // Generate srcset for responsive images
  const getSrcSet = (): string | undefined => {
    if (typeof src === 'string') {
      return undefined;
    }

    return src.map((source) => `${source.src} ${source.width}w`).join(', ');
  };

  // Get primary src
  const getPrimarySrc = (): string => {
    if (typeof src === 'string') {
      return src;
    }

    // Return largest image as primary
    const sorted = [...src].sort((a, b) => (b.width || 0) - (a.width || 0));
    return sorted[0].src;
  };

  // Handle successful load
  const handleLoad = (event: React.SyntheticEvent<HTMLImageElement>): void => {
    setLoadState(LoadState.LOADED);
    onLoad?.(event);
  };

  // Handle load error
  const handleError = (event: React.SyntheticEvent<HTMLImageElement>): void => {
    setLoadState(LoadState.ERROR);
    onError?.(event);
  };

  // Retry loading
  const retry = (): void => {
    setRetryCount((prev) => prev + 1);
    setLoadState(LoadState.LOADING);
  };

  // Container styles
  const containerStyle: CSSProperties = {
    width: width ? `${width}px` : '100%',
    height: height ? `${height}px` : 'auto',
    position: 'relative',
    overflow: 'hidden',
    ...style,
  };

  // Image styles
  const imageStyle: CSSProperties = {
    width: '100%',
    height: '100%',
    objectFit,
    display: loadState === LoadState.LOADED ? 'block' : 'none',
  };

  // Render placeholder
  if (!isInView || loadState === LoadState.IDLE || loadState === LoadState.LOADING) {
    if (placeholder) {
      return (
        <div ref={containerRef} style={containerStyle} className={className}>
          <img src={placeholder} alt={`${alt} placeholder`} style={imageStyle} />
          {isInView && (
            <img
              ref={imgRef}
              src={getPrimarySrc()}
              srcSet={getSrcSet()}
              alt={alt}
              onLoad={handleLoad}
              onError={handleError}
              loading={loading}
              decoding={decoding}
              crossOrigin={crossOrigin}
              referrerPolicy={referrerPolicy}
              style={{ ...imageStyle, display: 'none' }}
            />
          )}
        </div>
      );
    }

    return (
      <div ref={containerRef} style={containerStyle} className={className}>
        <PlaceholderComponent width={width} height={height} style={style} />
        {isInView && (
          <img
            ref={imgRef}
            src={getPrimarySrc()}
            srcSet={getSrcSet()}
            alt={alt}
            onLoad={handleLoad}
            onError={handleError}
            loading={loading}
            decoding={decoding}
            crossOrigin={crossOrigin}
            referrerPolicy={referrerPolicy}
            style={{ ...imageStyle, display: 'none' }}
          />
        )}
      </div>
    );
  }

  // Render error state
  if (loadState === LoadState.ERROR) {
    return (
      <div ref={containerRef} style={containerStyle} className={className}>
        <ErrorComponent alt={alt} width={width} height={height} style={style} retry={retry} />
      </div>
    );
  }

  // Render loaded image
  return (
    <div ref={containerRef} style={containerStyle} className={className}>
      <img
        ref={imgRef}
        src={getPrimarySrc()}
        srcSet={getSrcSet()}
        alt={alt}
        onLoad={handleLoad}
        onError={handleError}
        loading={loading}
        decoding={decoding}
        crossOrigin={crossOrigin}
        referrerPolicy={referrerPolicy}
        style={imageStyle}
      />
    </div>
  );
};

export default ImageLoader;
