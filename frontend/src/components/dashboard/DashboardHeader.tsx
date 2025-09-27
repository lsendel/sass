import React from 'react';
import clsx from 'clsx';
import { RealTimeStatus } from './RealTimeStatus';

interface DashboardHeaderProps {
  userName?: string;
  isRealTimeActive: boolean;
  onRefresh: () => void;
  lastUpdated?: Date;
  className?: string;
}

/**
 * Dashboard header component with welcome message and real-time status.
 * Extracted from main dashboard for better component organization.
 */
export const DashboardHeader: React.FC<DashboardHeaderProps> = ({
  userName,
  isRealTimeActive,
  onRefresh,
  lastUpdated,
  className
}) => {
  return (
    <div className={clsx('flex justify-between items-start', className)}>
      <div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">
          Welcome back{userName && (
            <span className="brand-element">, {userName}</span>
          )}! 👋
        </h1>
        <p className="text-sm text-gray-600">
          Here's your dashboard overview with the latest insights and metrics.
        </p>
      </div>

      <RealTimeStatus
        isActive={isRealTimeActive}
        onRefresh={onRefresh}
        lastUpdated={lastUpdated}
      />
    </div>
  );
};

/**
 * Simplified version for loading state.
 */
export const DashboardHeaderSkeleton: React.FC = () => {
  return (
    <div className="flex justify-between items-start">
      <div>
        <div className="h-8 bg-gray-200 rounded w-64 mb-2 animate-pulse" />
        <div className="h-4 bg-gray-200 rounded w-96 animate-pulse" />
      </div>
      <div className="text-right">
        <div className="h-4 bg-gray-200 rounded w-32 mb-1 animate-pulse" />
        <div className="h-4 bg-gray-200 rounded w-24 animate-pulse" />
      </div>
    </div>
  );
};