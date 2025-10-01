import React from 'react';
import { XMarkIcon } from '@heroicons/react/20/solid';
import { format } from 'date-fns';

import { useGetAuditLogDetailQuery } from '../../store/api/auditApi';
// import type { AuditLogDetail } from '../../store/api/auditApi';

interface AuditLogDetailModalProps {
  entryId: string | null;
  isOpen: boolean;
  onClose: () => void;
}

export const AuditLogDetailModal: React.FC<AuditLogDetailModalProps> = ({
  entryId,
  isOpen,
  onClose,
}) => {
  const { data: detail, error, isLoading } = useGetAuditLogDetailQuery(entryId!, {
    skip: !entryId || !isOpen,
  });

  const formatTimestamp = (timestamp: string) => {
    try {
      return format(new Date(timestamp), 'MMM dd, yyyy HH:mm:ss.SSS');
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

  const renderAdditionalData = (data: Record<string, unknown>) => {
    return Object.entries(data).map(([key, value]) => (
      <div key={key} className="py-2 border-b border-gray-100 last:border-b-0">
        <div className="flex justify-between">
          <span className="text-sm font-medium text-gray-700 capitalize">
            {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}:
          </span>
          <span className="text-sm text-gray-900 max-w-xs truncate text-right">
            {typeof value === 'object' ? JSON.stringify(value) : String(value)}
          </span>
        </div>
      </div>
    ));
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-500 bg-opacity-75 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900">
              Audit Log Details
            </h3>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-500"
            >
              <XMarkIcon className="h-6 w-6" />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="px-6 py-4 max-h-[calc(90vh-120px)] overflow-y-auto">
          {isLoading && (
            <div className="flex items-center justify-center py-12">
              <div className="inline-flex items-center">
                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-indigo-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span className="text-sm text-gray-500">Loading details...</span>
              </div>
            </div>
          )}

          {error && (
            <div className="text-center py-12">
              <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100">
                <svg className="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.5 0L4.268 19c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <h3 className="mt-2 text-sm font-medium text-gray-900">Failed to load details</h3>
              <p className="mt-1 text-sm text-gray-500">
                {typeof error === 'object' && 'data' in error && error.data && typeof error.data === 'object'
                  ? (error.data as { message?: string }).message ?? 'Unable to retrieve audit log details'
                  : 'Unable to retrieve audit log details'}
              </p>
            </div>
          )}

          {detail && (
            <div className="space-y-6">
              {/* Basic Information */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Basic Information</h4>
                <div className="bg-gray-50 rounded-md p-4 space-y-3">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm font-medium text-gray-700">ID:</span>
                      <div className="text-sm text-gray-900 font-mono break-all">{detail.id}</div>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Timestamp:</span>
                      <div className="text-sm text-gray-900">{formatTimestamp(detail.timestamp)}</div>
                    </div>
                    {detail.correlationId && (
                      <div className="md:col-span-2">
                        <span className="text-sm font-medium text-gray-700">Correlation ID:</span>
                        <div className="text-sm text-gray-900 font-mono break-all">{detail.correlationId}</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Actor Information */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Actor</h4>
                <div className="bg-gray-50 rounded-md p-4 space-y-3">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm font-medium text-gray-700">Display Name:</span>
                      <div className="text-sm text-gray-900">{detail.actorDisplayName}</div>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Type:</span>
                      <div className="text-sm text-gray-900">{detail.actorType}</div>
                    </div>
                    {detail.actorId && (
                      <div className="md:col-span-2">
                        <span className="text-sm font-medium text-gray-700">Actor ID:</span>
                        <div className="text-sm text-gray-900 font-mono break-all">{detail.actorId}</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Action Information */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Action</h4>
                <div className="bg-gray-50 rounded-md p-4 space-y-3">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm font-medium text-gray-700">Type:</span>
                      <div className="text-sm text-gray-900">{detail.actionType}</div>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Outcome:</span>
                      <div>
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getOutcomeBadgeClass(detail.outcome)}`}>
                          {detail.outcome}
                        </span>
                      </div>
                    </div>
                    <div className="md:col-span-2">
                      <span className="text-sm font-medium text-gray-700">Description:</span>
                      <div className="text-sm text-gray-900">{detail.actionDescription}</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Resource Information */}
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Resource</h4>
                <div className="bg-gray-50 rounded-md p-4 space-y-3">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <span className="text-sm font-medium text-gray-700">Type:</span>
                      <div className="text-sm text-gray-900">{detail.resourceType}</div>
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-700">Sensitivity:</span>
                      <div>
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getSensitivityBadgeClass(detail.sensitivity)}`}>
                          {detail.sensitivity}
                        </span>
                      </div>
                    </div>
                    {detail.resourceDisplayName && (
                      <div>
                        <span className="text-sm font-medium text-gray-700">Display Name:</span>
                        <div className="text-sm text-gray-900">{detail.resourceDisplayName}</div>
                      </div>
                    )}
                    {detail.resourceId && (
                      <div>
                        <span className="text-sm font-medium text-gray-700">Resource ID:</span>
                        <div className="text-sm text-gray-900 font-mono break-all">{detail.resourceId}</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Technical Information */}
              {(detail.ipAddress || detail.userAgent) && (
                <div>
                  <h4 className="text-sm font-medium text-gray-900 mb-3">Technical Details</h4>
                  <div className="bg-gray-50 rounded-md p-4 space-y-3">
                    {detail.ipAddress && (
                      <div>
                        <span className="text-sm font-medium text-gray-700">IP Address:</span>
                        <div className="text-sm text-gray-900 font-mono">{detail.ipAddress}</div>
                      </div>
                    )}
                    {detail.userAgent && (
                      <div>
                        <span className="text-sm font-medium text-gray-700">User Agent:</span>
                        <div className="text-sm text-gray-900 break-all">{detail.userAgent}</div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Additional Data */}
              {detail.additionalData && Object.keys(detail.additionalData).length > 0 && (
                <div>
                  <h4 className="text-sm font-medium text-gray-900 mb-3">Additional Data</h4>
                  <div className="bg-gray-50 rounded-md p-4">
                    {renderAdditionalData(detail.additionalData)}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
