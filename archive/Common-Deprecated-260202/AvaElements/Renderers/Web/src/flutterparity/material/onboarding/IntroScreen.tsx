import React, { useState } from 'react';

export interface IntroPage {
  title: string;
  description: string;
  imageUrl?: string;
  icon?: string;
  backgroundColor?: string;
}

export interface IntroScreenProps {
  pages: IntroPage[];
  initialPage?: number;
  showSkip?: boolean;
  showNext?: boolean;
  showDone?: boolean;
  skipLabel?: string;
  nextLabel?: string;
  doneLabel?: string;
  indicatorColor?: string;
  activeIndicatorColor?: string;
  onSkip?: () => void;
  onDone?: () => void;
  onPageChange?: (page: number) => void;
}

export const IntroScreen: React.FC<IntroScreenProps> = ({
  pages,
  initialPage = 0,
  showSkip = true,
  showNext = true,
  showDone = true,
  skipLabel = 'Skip',
  nextLabel = 'Next',
  doneLabel = 'Get Started',
  indicatorColor = '#ccc',
  activeIndicatorColor = '#007AFF',
  onSkip,
  onDone,
  onPageChange
}) => {
  const [currentPage, setCurrentPage] = useState(initialPage);

  const goToPage = (page: number) => {
    setCurrentPage(page);
    onPageChange?.(page);
  };

  const isLastPage = currentPage === pages.length - 1;
  const page = pages[currentPage];

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      backgroundColor: page?.backgroundColor || '#fff'
    }}>
      {/* Page Content */}
      <div style={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 40,
        textAlign: 'center'
      }}>
        {page?.imageUrl && (
          <img src={page.imageUrl} alt="" style={{ width: 200, height: 200, marginBottom: 32 }} />
        )}
        {page?.icon && (
          <span style={{ fontSize: 80, marginBottom: 32 }}>{page.icon}</span>
        )}
        <h1 style={{ fontSize: 28, fontWeight: 700, marginBottom: 16 }}>{page?.title}</h1>
        <p style={{ fontSize: 16, color: '#666', maxWidth: 400 }}>{page?.description}</p>
      </div>

      {/* Indicators */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 24 }}>
        {pages.map((_, i) => (
          <button
            key={i}
            onClick={() => goToPage(i)}
            style={{
              width: i === currentPage ? 24 : 8,
              height: 8,
              borderRadius: 4,
              border: 'none',
              backgroundColor: i === currentPage ? activeIndicatorColor : indicatorColor,
              cursor: 'pointer',
              transition: 'all 0.3s'
            }}
          />
        ))}
      </div>

      {/* Navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '0 24px 40px' }}>
        {showSkip && !isLastPage ? (
          <button onClick={onSkip} style={{ background: 'none', border: 'none', fontSize: 16, cursor: 'pointer' }}>
            {skipLabel}
          </button>
        ) : <div />}

        {isLastPage && showDone ? (
          <button onClick={onDone} style={{
            padding: '12px 32px',
            fontSize: 16,
            fontWeight: 600,
            backgroundColor: activeIndicatorColor,
            color: '#fff',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer'
          }}>
            {doneLabel}
          </button>
        ) : showNext && (
          <button onClick={() => goToPage(currentPage + 1)} style={{
            padding: '12px 32px',
            fontSize: 16,
            fontWeight: 600,
            backgroundColor: activeIndicatorColor,
            color: '#fff',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer'
          }}>
            {nextLabel}
          </button>
        )}
      </div>
    </div>
  );
};

export default IntroScreen;
