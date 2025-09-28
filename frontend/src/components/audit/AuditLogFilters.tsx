import React, { useState } from 'react'
import { CalendarIcon, FunnelIcon, XMarkIcon } from '@heroicons/react/20/solid'
import type { AuditLogFilter } from '../../store/api/auditApi';

interface AuditLogFiltersProps {
  filters: AuditLogFilter;
  onFiltersChange: (filters: AuditLogFilter) => void;
  onClearFilters: () => void;
}

export const AuditLogFilters: React.FC<AuditLogFiltersProps> = ({
  filters,
  onFiltersChange,
  onClearFilters,
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  const updateFilter = (key: keyof AuditLogFilter, value: AuditLogFilter[keyof AuditLogFilter]) => {
    onFiltersChange({
      ...filters,
      [key]: value,
    });
  };

  const hasActiveFilters = Boolean(
    filters.search ??
    filters.dateFrom ??
    filters.dateTo ??
    filters.actionTypes?.length ??
    filters.resourceTypes?.length ??
    filters.outcomes?.length
  );

  const formatDateForInput = (isoString?: string) => {
    if (!isoString) return '';
    try {
      return new Date(isoString).toISOString().slice(0, 16);
    } catch {
      return '';
    }
  };

  const handleDateChange = (key: 'dateFrom' | 'dateTo', value: string) => {
    if (!value) {
      updateFilter(key, undefined);
      return;
    }

    try {
      const date = new Date(value);
      updateFilter(key, date.toISOString());
    } catch {
      console.warn('Invalid date format:', value);
    }
  };

  const actionTypeOptions = [
    'LOGIN',
    'LOGOUT',
    'CREATE',
    'UPDATE',
    'DELETE',
    'VIEW',
    'EXPORT',
    'PAYMENT_CREATED',
    'PAYMENT_PROCESSED',
    'SUBSCRIPTION_CREATED',
    'SUBSCRIPTION_MODIFIED',
  ];

  const resourceTypeOptions = [
    'USER',
    'ORGANIZATION',
    'PAYMENT',
    'SUBSCRIPTION',
    'AUDIT_LOG',
    'AUTHENTICATION',
  ];

  const outcomeOptions = [
    'SUCCESS',
    'FAILURE',
    'PARTIAL',
  ];

  const handleMultiSelectChange = (
    key: 'actionTypes' | 'resourceTypes' | 'outcomes',
    value: string,
    checked: boolean
  ) => {
    const currentValues = filters[key] ?? [];
    const newValues = checked
      ? [...currentValues, value]
      : currentValues.filter(v => v !== value);

    updateFilter(key, newValues.length > 0 ? newValues : undefined);
  };

  return (
    <div className="bg-white border-b border-gray-200">
      {/* Main filter bar */}
      <div className="px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4 flex-1">
            {/* Search */}
            <div className="flex-1 max-w-lg">
              <div className="relative">
                <input
                  type="text"
                  placeholder="Search audit logs..."
                  value={filters.search || ''}
                  onChange={(e) => updateFilter('search', e.target.value || undefined)}
                  className="block w-full rounded-md border-gray-300 pl-10 pr-3 py-2 text-sm placeholder-gray-500 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg className="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
              </div>
            </div>

            {/* Date range */}
            <div className="flex items-center space-x-2">
              <CalendarIcon className="h-4 w-4 text-gray-400" />
              <input
                type="datetime-local"
                placeholder="From"
                value={formatDateForInput(filters.dateFrom)}
                onChange={(e) => handleDateChange('dateFrom', e.target.value)}
                className="block rounded-md border-gray-300 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
              <span className="text-gray-400">to</span>
              <input
                type="datetime-local"
                placeholder="To"
                value={formatDateForInput(filters.dateTo)}
                onChange={(e) => handleDateChange('dateTo', e.target.value)}
                className="block rounded-md border-gray-300 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </div>

          {/* Filter toggle and clear */}
          <div className="flex items-center space-x-2">
            {hasActiveFilters && (
              <button
                onClick={onClearFilters}
                className="inline-flex items-center px-3 py-1.5 border border-gray-300 text-xs font-medium rounded text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
              >
                <XMarkIcon className="h-3 w-3 mr-1" />
                Clear
              </button>
            )}
            <button
              onClick={() => setIsExpanded(!isExpanded)}
              className={`inline-flex items-center px-3 py-1.5 border text-xs font-medium rounded focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 ${
                isExpanded || hasActiveFilters
                  ? 'border-indigo-300 text-indigo-700 bg-indigo-50 hover:bg-indigo-100'
                  : 'border-gray-300 text-gray-700 bg-white hover:bg-gray-50'
              }`}
            >
              <FunnelIcon className="h-3 w-3 mr-1" />
              Filters
              {hasActiveFilters && (
                <span className="ml-1 bg-indigo-600 text-white text-xs rounded-full px-1.5 py-0.5">
                  {[
                    filters.actionTypes?.length,
                    filters.resourceTypes?.length,
                    filters.outcomes?.length,
                    ].filter(Boolean).reduce((a, b) => (a ?? 0) + (b ?? 0), 0)}
                </span>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Expanded filters */}
      {isExpanded && (
        <div className="px-6 pb-4 border-t border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
            {/* Action Types */}
            <fieldset>
              <legend className="block text-sm font-medium text-gray-700 mb-2">
                Action Types
              </legend>
              <div className="space-y-2 max-h-40 overflow-y-auto">
                {actionTypeOptions.map((option) => {
                  const id = `action-type-${option.toLowerCase()}`;
                  return (
                    <label key={option} htmlFor={id} className="flex items-center">
                      <input
                        id={id}
                        type="checkbox"
                        checked={filters.actionTypes?.includes(option) ?? false}
                        onChange={(e) => handleMultiSelectChange('actionTypes', option, e.target.checked)}
                        className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm text-gray-700">{option}</span>
                    </label>
                  );
                })}
              </div>
            </fieldset>

            {/* Resource Types */}
            <fieldset>
              <legend className="block text-sm font-medium text-gray-700 mb-2">
                Resource Types
              </legend>
              <div className="space-y-2 max-h-40 overflow-y-auto">
                {resourceTypeOptions.map((option) => {
                  const id = `resource-type-${option.toLowerCase()}`;
                  return (
                    <label key={option} htmlFor={id} className="flex items-center">
                      <input
                        id={id}
                        type="checkbox"
                        checked={filters.resourceTypes?.includes(option) ?? false}
                        onChange={(e) => handleMultiSelectChange('resourceTypes', option, e.target.checked)}
                        className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm text-gray-700">{option}</span>
                    </label>
                  );
                })}
              </div>
            </fieldset>

            {/* Outcomes */}
            <fieldset>
              <legend className="block text-sm font-medium text-gray-700 mb-2">
                Outcomes
              </legend>
              <div className="space-y-2">
                {outcomeOptions.map((option) => {
                  const id = `outcome-${option.toLowerCase()}`;
                  return (
                    <label key={option} htmlFor={id} className="flex items-center">
                      <input
                        id={id}
                        type="checkbox"
                        checked={filters.outcomes?.includes(option) ?? false}
                        onChange={(e) => handleMultiSelectChange('outcomes', option, e.target.checked)}
                        className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm text-gray-700">{option}</span>
                    </label>
                  );
                })}
              </div>
            </fieldset>
          </div>
        </div>
      )}
    </div>
  );
};
