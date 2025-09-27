import React, { useMemo, useCallback, useRef, useEffect } from 'react';
import { FixedSizeList as List, VariableSizeList, ListChildComponentProps } from 'react-window';
import { AutoSizer } from 'react-virtualized-auto-sizer';

interface VirtualizedListProps<T> {
  items: T[];
  itemHeight: number | ((index: number) => number);
  renderItem: (props: ListChildComponentProps & { item: T }) => JSX.Element;
  className?: string;
  overscan?: number;
  onItemsRendered?: (visibleRange: { startIndex: number; endIndex: number }) => void;
  loadMore?: () => void;
  hasNextPage?: boolean;
  isLoading?: boolean;
  threshold?: number;
}

/**
 * High-performance virtualized list component for rendering large datasets.
 * Uses react-window for optimal performance with thousands of items.
 */
export const VirtualizedList = <T,>({
  items,
  itemHeight,
  renderItem,
  className = '',
  overscan = 5,
  onItemsRendered,
  loadMore,
  hasNextPage = false,
  isLoading = false,
  threshold = 5,
}: VirtualizedListProps<T>) => {
  const listRef = useRef<any>();

  // Determine if we should use FixedSizeList or VariableSizeList
  const isFixedHeight = typeof itemHeight === 'number';

  // Memoize item data to prevent unnecessary re-renders
  const itemData = useMemo(() => ({ items, renderItem }), [items, renderItem]);

  // Enhanced item renderer that includes the actual item data
  const ItemRenderer = useCallback(
    (props: ListChildComponentProps) => {
      const { index, style, data } = props;
      const { items: itemList, renderItem: renderFn } = data;
      const item = itemList[index];

      if (!item) {
        return (
          <div style={style} className="flex items-center justify-center p-4">
            <div className="animate-pulse bg-gray-200 h-4 w-3/4 rounded" />
          </div>
        );
      }

      return renderFn({ ...props, item });
    },
    []
  );

  // Handle infinite loading
  const handleItemsRendered = useCallback(
    ({ visibleStartIndex, visibleStopIndex }: any) => {
      onItemsRendered?.({ startIndex: visibleStartIndex, endIndex: visibleStopIndex });

      // Trigger load more when approaching the end
      if (
        loadMore &&
        hasNextPage &&
        !isLoading &&
        visibleStopIndex >= items.length - threshold
      ) {
        loadMore();
      }
    },
    [onItemsRendered, loadMore, hasNextPage, isLoading, items.length, threshold]
  );

  // Scroll to item method
  const scrollToItem = useCallback(
    (index: number, align: 'auto' | 'smart' | 'center' | 'end' | 'start' = 'auto') => {
      listRef.current?.scrollToItem(index, align);
    },
    []
  );

  // Variable height list component
  const VariableHeightList = useCallback(
    ({ height, width }: { height: number; width: number }) => (
      <VariableSizeList
        ref={listRef}
        height={height}
        width={width}
        itemCount={items.length}
        itemSize={itemHeight as (index: number) => number}
        itemData={itemData}
        overscanCount={overscan}
        onItemsRendered={handleItemsRendered}
        className={className}
      >
        {ItemRenderer}
      </VariableSizeList>
    ),
    [items.length, itemHeight, itemData, overscan, handleItemsRendered, className, ItemRenderer]
  );

  // Fixed height list component
  const FixedHeightList = useCallback(
    ({ height, width }: { height: number; width: number }) => (
      <List
        ref={listRef}
        height={height}
        width={width}
        itemCount={items.length}
        itemSize={itemHeight as number}
        itemData={itemData}
        overscanCount={overscan}
        onItemsRendered={handleItemsRendered}
        className={className}
      >
        {ItemRenderer}
      </List>
    ),
    [items.length, itemHeight, itemData, overscan, handleItemsRendered, className, ItemRenderer]
  );

  return (
    <div className="h-full w-full">
      <AutoSizer>
        {isFixedHeight ? FixedHeightList : VariableHeightList}
      </AutoSizer>

      {/* Loading indicator */}
      {isLoading && (
        <div className="flex items-center justify-center p-4">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" />
          <span className="ml-2 text-sm text-gray-600">Loading more items...</span>
        </div>
      )}
    </div>
  );
};

/**
 * Hook for managing virtualized list state and performance.
 */
export const useVirtualizedList = <T,>(
  items: T[],
  options: {
    itemHeight: number | ((item: T, index: number) => number);
    estimatedItemSize?: number;
    threshold?: number;
  }
) => {
  const { itemHeight, estimatedItemSize = 50, threshold = 5 } = options;
  const [visibleRange, setVisibleRange] = React.useState({ start: 0, end: 0 });

  // Memoize item heights for variable height lists
  const getItemHeight = useCallback(
    (index: number) => {
      const item = items[index];
      if (!item) return estimatedItemSize;

      return typeof itemHeight === 'function'
        ? itemHeight(item, index)
        : itemHeight;
    },
    [items, itemHeight, estimatedItemSize]
  );

  // Calculate total height for optimization
  const totalHeight = useMemo(() => {
    if (typeof itemHeight === 'number') {
      return items.length * itemHeight;
    }

    return items.reduce((total, item, index) => {
      return total + getItemHeight(index);
    }, 0);
  }, [items, itemHeight, getItemHeight]);

  return {
    visibleRange,
    setVisibleRange,
    getItemHeight: typeof itemHeight === 'number' ? itemHeight : getItemHeight,
    totalHeight,
    visibleItems: items.slice(visibleRange.start, visibleRange.end + 1),
  };
};

/**
 * Optimized list item component with React.memo.
 */
export const ListItem = React.memo<{
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
}>(({ children, className = '', onClick }) => {
  return (
    <div
      className={`transition-colors duration-150 ${className}`}
      onClick={onClick}
    >
      {children}
    </div>
  );
});

ListItem.displayName = 'ListItem';