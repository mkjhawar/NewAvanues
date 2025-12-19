/**
 * LazyImage Component - Flutter Parity Display
 *
 * Image with lazy loading and placeholder support
 * Uses IntersectionObserver for performance
 *
 * @package com.augmentalis.avaelements.flutter.material.display
 * @since 1.0.0-flutter-parity
 */

import React, { useState, useRef, useEffect } from 'react';
import { BaseDisplayProps } from './types';

export interface LazyImageProps extends BaseDisplayProps {
  /** Image source URL */
  src: string;
  /** Alternative text */
  alt: string;
  /** Placeholder image or color */
  placeholder?: string;
  /** Width */
  width?: number | string;
  /** Height */
  height?: number | string;
  /** Object fit */
  objectFit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down';
  /** On load callback */
  onLoad?: () => void;
  /** On error callback */
  onError?: () => void;
}

export const LazyImage: React.FC<LazyImageProps> = ({
  src,
  alt,
  placeholder,
  width = '100%',
  height = 'auto',
  objectFit = 'cover',
  onLoad,
  onError,
  className,
  accessibilityLabel,
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const [hasError, setHasError] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      { rootMargin: '50px' }
    );

    if (imgRef.current) {
      observer.observe(imgRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const handleLoad = () => {
    setIsLoaded(true);
    onLoad?.();
  };

  const handleError = () => {
    setHasError(true);
    onError?.();
  };

  return (
    <div
      className={className}
      style={{
        width,
        height,
        position: 'relative',
        overflow: 'hidden',
        backgroundColor: placeholder?.startsWith('#') ? placeholder : '#f3f4f6',
      }}
    >
      {!isLoaded && !hasError && placeholder && !placeholder.startsWith('#') && (
        <img
          src={placeholder}
          alt=""
          aria-hidden="true"
          style={{
            width: '100%',
            height: '100%',
            objectFit,
            filter: 'blur(10px)',
          }}
        />
      )}
      {isInView && (
        <img
          ref={imgRef}
          src={src}
          alt={alt}
          aria-label={accessibilityLabel || alt}
          onLoad={handleLoad}
          onError={handleError}
          style={{
            width: '100%',
            height: '100%',
            objectFit,
            opacity: isLoaded ? 1 : 0,
            transition: 'opacity 0.3s ease-in-out',
            position: 'absolute',
            top: 0,
            left: 0,
          }}
        />
      )}
      {hasError && (
        <div
          style={{
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#9ca3af',
          }}
        >
          <svg
            width="48"
            height="48"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
        </div>
      )}
    </div>
  );
};

export default LazyImage;
