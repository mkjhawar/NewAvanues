import React, { useState, useEffect } from 'react';

export interface ProductItem {
  id: string;
  imageUrl: string;
  title: string;
  price: string;
  originalPrice?: string;
  rating?: number;
  badge?: string;
}

export interface ProductCarouselProps {
  products: ProductItem[];
  autoPlay?: boolean;
  autoPlayInterval?: number;
  showIndicators?: boolean;
  showArrows?: boolean;
  onProductTap?: (product: ProductItem) => void;
}

export const ProductCarousel: React.FC<ProductCarouselProps> = ({
  products,
  autoPlay = false,
  autoPlayInterval = 3000,
  showIndicators = true,
  showArrows = true,
  onProductTap
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const itemsPerView = 4;
  const maxIndex = Math.max(0, products.length - itemsPerView);

  useEffect(() => {
    if (!autoPlay) return;
    const timer = setInterval(() => {
      setCurrentIndex(prev => prev >= maxIndex ? 0 : prev + 1);
    }, autoPlayInterval);
    return () => clearInterval(timer);
  }, [autoPlay, autoPlayInterval, maxIndex]);

  return (
    <div style={{ position: 'relative' }}>
      <div style={{ overflow: 'hidden' }}>
        <div style={{
          display: 'flex',
          gap: 16,
          transform: `translateX(-${currentIndex * (200 + 16)}px)`,
          transition: 'transform 0.3s ease'
        }}>
          {products.map(product => (
            <div
              key={product.id}
              onClick={() => onProductTap?.(product)}
              style={{
                minWidth: 200,
                cursor: 'pointer',
                backgroundColor: '#fff',
                borderRadius: 8,
                overflow: 'hidden',
                boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
              }}
            >
              <div style={{ position: 'relative' }}>
                <img src={product.imageUrl} alt={product.title} style={{ width: '100%', height: 150, objectFit: 'cover' }} />
                {product.badge && (
                  <span style={{
                    position: 'absolute',
                    top: 8,
                    left: 8,
                    padding: '4px 8px',
                    backgroundColor: '#FF3B30',
                    color: '#fff',
                    fontSize: 12,
                    borderRadius: 4
                  }}>{product.badge}</span>
                )}
              </div>
              <div style={{ padding: 12 }}>
                <h4 style={{ margin: '0 0 8px', fontSize: 14 }}>{product.title}</h4>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                  <span style={{ fontWeight: 700, color: '#007AFF' }}>{product.price}</span>
                  {product.originalPrice && (
                    <span style={{ textDecoration: 'line-through', color: '#999', fontSize: 12 }}>{product.originalPrice}</span>
                  )}
                </div>
                {product.rating && (
                  <div style={{ marginTop: 8, color: '#FFD700' }}>{'★'.repeat(Math.floor(product.rating))}</div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
      {showArrows && (
        <>
          <button onClick={() => setCurrentIndex(Math.max(0, currentIndex - 1))}
            style={{ position: 'absolute', left: -20, top: '50%', transform: 'translateY(-50%)', width: 40, height: 40, borderRadius: '50%', border: 'none', backgroundColor: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', cursor: 'pointer' }}>
            ←
          </button>
          <button onClick={() => setCurrentIndex(Math.min(maxIndex, currentIndex + 1))}
            style={{ position: 'absolute', right: -20, top: '50%', transform: 'translateY(-50%)', width: 40, height: 40, borderRadius: '50%', border: 'none', backgroundColor: '#fff', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', cursor: 'pointer' }}>
            →
          </button>
        </>
      )}
    </div>
  );
};

export default ProductCarousel;
