import React from 'react';
import { format } from 'date-fns';
import { ChevronLeftIcon, ChevronRightIcon, ChevronUpIcon, ChevronDownIcon } from '@heroicons/react/20/solid';

import type { AuditLogResponse, AuditLogEntry } from '../../store/api/auditApi';

interface AuditLogTableProps {
  auditLogs?: AuditLogResponse;
  currentPage: number;
  onPageChange: (page: number) => void;
  onSortChange: (field: string, direction: 'ASC' | 'DESC') => void;
  onRowClick?: (entryId: string) => void;
}

/**
 * Table component for displaying audit log entries with pagination and sorting.
 */
export const AuditLogTable: React.FC<AuditLogTableProps> = ({
  auditLogs,
  currentPage,
  onPageChange,
  onSortChange,
  onRowClick,
}) => {
  const formatTimestamp = (timestamp: string) => {
    try {
      return format(new Date(timestamp), 'MMM dd, yyyy HH:mm:ss');
    } catch {
      return timestamp;
    }
  };

  const getOutcomeBadgeClass = (outcome: string) => {
    switch (outcome) {
      case 'SUCCESS':
        return 'bg-green-100 text-green-800';
      case 'FAILURE':
        return 'bg-red-100 text-red-800';
      case 'PARTIAL':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getSensitivityBadgeClass = (sensitivity: string) => {
    switch (sensitivity) {
      case 'PUBLIC':
        return 'bg-blue-100 text-blue-800';
      case 'INTERNAL':
        return 'bg-yellow-100 text-yellow-800';
      case 'CONFIDENTIAL':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const SortIcon: React.FC<{ field: string; currentSort?: string; direction?: string }> = ({
    field,
    currentSort,
    direction,
  }) => {
    if (currentSort !== field) {
      return <ChevronUpIcon className="h-4 w-4 text-gray-400" />;
    }
    return direction === 'DESC' ? (
      <ChevronDownIcon className="h-4 w-4 text-gray-600" />
    ) : (
      <ChevronUpIcon className="h-4 w-4 text-gray-600" />
    );
  };

  const handleSort = (field: string, currentSort?: string, currentDirection?: string) => {
    if (currentSort === field) {
      // Toggle direction
      onSortChange(field, currentDirection === 'DESC' ? 'ASC' : 'DESC');
    } else {
      // New field, default to DESC
      onSortChange(field, 'DESC');
    }
  };

  if (!auditLogs) {
    return (
      <div className="p-8 text-center text-gray-500">
        No audit logs available
      </div>
    );
  }

  const { content: entries, totalElements, totalPages, first, last } = auditLogs;

  if (entries.length === 0) {
    return (
      <div className="p-8 text-center">
        <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 className="mt-2 text-sm font-medium text-gray-900">No audit logs found</h3>
        <p className="mt-1 text-sm text-gray-500">
          Try adjusting your filters to see more results.
        </p>
      </div>
    );
  }

  return (
    <div>
      {/* Table */}
      <div className="overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('timestamp')}
              >
                <div className="flex items-center space-x-1">
                  <span>Timestamp</span>
                  <SortIcon field="timestamp" />
                </div>
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actor
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:bg-gray-100"
                onClick={() => handleSort('actionType')}
              >
                <div className="flex items-center space-x-1">
                  <span>Action</span>
                  <SortIcon field="actionType" />
                </div>
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Resource
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Outcome
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Sensitivity
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {entries.map((entry: AuditLogEntry) => (
              <tr
                key={entry.id}
                className={`hover:bg-gray-50 ${onRowClick ? 'cursor-pointer' : ''}`}
                onClick={() => onRowClick?.(entry.id)}
              >
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {formatTimestamp(entry.timestamp)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div>
                    <div className="text-sm font-medium text-gray-900">
                      {entry.actorDisplayName}
                    </div>
                    <div className="text-sm text-gray-500">
                      {entry.actorType}
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <div>
                    <div className="text-sm font-medium text-gray-900">
                      {entry.actionType}
                    </div>
                    <div className="text-sm text-gray-500 max-w-xs truncate">
                      {entry.actionDescription}
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div>
                    <div className="text-sm font-medium text-gray-900">
                      {entry.resourceType}
                    </div>
                    {entry.resourceDisplayName && (
                      <div className="text-sm text-gray-500 max-w-xs truncate">
                        {entry.resourceDisplayName}
                      </div>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getOutcomeBadgeClass(entry.outcome)}`}>
                    {entry.outcome}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getSensitivityBadgeClass(entry.sensitivity)}`}>
                    {entry.sensitivity}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
        <div className="flex-1 flex justify-between sm:hidden">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={first}
            className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Previous
          </button>
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={last}
            className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Next
          </button>
        </div>
        <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-gray-700">
              Showing{' '}
              <span className="font-medium">
                {currentPage * auditLogs.size + 1}
              </span>{' '}
              to{' '}
              <span className="font-medium">
                {Math.min((currentPage + 1) * auditLogs.size, totalElements)}
              </span>{' '}
              of{' '}
              <span className="font-medium">{totalElements}</span>{' '}
              results
            </p>
          </div>
          <div>
            <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
              <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={first}
                className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span className="sr-only">Previous</span>
                <ChevronLeftIcon className="h-5 w-5" aria-hidden="true" />
              </button>
              <span className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={last}
                className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span className="sr-only">Next</span>
                <ChevronRightIcon className="h-5 w-5" aria-hidden="true" />
              </button>
            </nav>
          </div>
        </div>
      </div>
    </div>
  );
};