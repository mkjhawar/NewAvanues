import React, { useState, useCallback, useRef } from 'react';

export interface ReorderableListViewProps {
  itemCount: number;
  itemBuilder: (index: number, isDragging: boolean) => React.ReactNode;
  onReorder: (oldIndex: number, newIndex: number) => void;
  className?: string;
  style?: React.CSSProperties;
  keyExtractor?: (index: number) => string | number;
}

/**
 * ReorderableListView - Drag-to-reorder list component
 *
 * Allows users to reorder list items by dragging them to new positions.
 */
export const ReorderableListView: React.FC<ReorderableListViewProps> = ({
  itemCount,
  itemBuilder,
  onReorder,
  className = '',
  style = {},
  keyExtractor = (index) => index,
}) => {
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const dragNodeRef = useRef<HTMLDivElement | null>(null);

  const handleDragStart = useCallback((e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', e.currentTarget.innerHTML);

    // Set a custom drag image
    if (e.currentTarget instanceof HTMLElement) {
      dragNodeRef.current = e.currentTarget;
    }
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';

    if (draggedIndex !== null && index !== draggedIndex) {
      setDragOverIndex(index);
    }
  }, [draggedIndex]);

  const handleDragEnter = useCallback((e: React.DragEvent, index: number) => {
    if (draggedIndex !== null && index !== draggedIndex) {
      setDragOverIndex(index);
    }
  }, [draggedIndex]);

  const handleDragLeave = useCallback(() => {
    setDragOverIndex(null);
  }, []);

  const handleDrop = useCallback(
    (e: React.DragEvent, index: number) => {
      e.preventDefault();

      if (draggedIndex !== null && draggedIndex !== index) {
        onReorder(draggedIndex, index);
      }

      setDraggedIndex(null);
      setDragOverIndex(null);
    },
    [draggedIndex, onReorder]
  );

  const handleDragEnd = useCallback(() => {
    setDraggedIndex(null);
    setDragOverIndex(null);
  }, []);

  // Keyboard reordering
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent, index: number) => {
      if (e.ctrlKey || e.metaKey) {
        switch (e.key) {
          case 'ArrowUp':
            e.preventDefault();
            if (index > 0) {
              onReorder(index, index - 1);
            }
            break;
          case 'ArrowDown':
            e.preventDefault();
            if (index < itemCount - 1) {
              onReorder(index, index + 1);
            }
            break;
        }
      }
    },
    [itemCount, onReorder]
  );

  return (
    <div
      className={className}
      style={{
        overflow: 'auto',
        height: '100%',
        ...style,
      }}
      role="list"
    >
      {Array.from({ length: itemCount }, (_, index) => {
        const isDragging = draggedIndex === index;
        const isDragOver = dragOverIndex === index;

        return (
          <div
            key={keyExtractor(index)}
            draggable
            onDragStart={(e) => handleDragStart(e, index)}
            onDragOver={(e) => handleDragOver(e, index)}
            onDragEnter={(e) => handleDragEnter(e, index)}
            onDragLeave={handleDragLeave}
            onDrop={(e) => handleDrop(e, index)}
            onDragEnd={handleDragEnd}
            onKeyDown={(e) => handleKeyDown(e, index)}
            tabIndex={0}
            role="listitem"
            aria-grabbed={isDragging}
            style={{
              opacity: isDragging ? 0.5 : 1,
              borderTop: isDragOver && draggedIndex !== null && draggedIndex < index ? '2px solid #2196F3' : undefined,
              borderBottom: isDragOver && draggedIndex !== null && draggedIndex > index ? '2px solid #2196F3' : undefined,
              cursor: 'move',
              transition: 'opacity 0.2s',
            }}
          >
            {itemBuilder(index, isDragging)}
          </div>
        );
      })}
    </div>
  );
};
