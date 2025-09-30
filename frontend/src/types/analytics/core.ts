/**
 * Core Analytics Types
 * Split from analytics.ts to reduce file size and improve maintainability
 */

// Core analytics identifiers
export type AnalyticsId = string & { readonly __brand: unique symbol }
export type DashboardId = string & { readonly __brand: unique symbol }
export type ReportId = string & { readonly __brand: unique symbol }
export type MetricId = string & { readonly __brand: unique symbol }

export const createAnalyticsId = (id: string): AnalyticsId => id as AnalyticsId
export const createDashboardId = (id: string): DashboardId => id as DashboardId
export const createReportId = (id: string): ReportId => id as ReportId
export const createMetricId = (id: string): MetricId => id as MetricId

// Time periods and granularity
export type TimePeriod = 'hour' | 'day' | 'week' | 'month' | 'quarter' | 'year'
export type TimeGranularity = 'minute' | 'hour' | 'day' | 'week' | 'month'

// Analytics data types
export type MetricType = 'counter' | 'gauge' | 'histogram' | 'rate' | 'percentage' | 'currency'
export type AggregationType = 'sum' | 'avg' | 'min' | 'max' | 'count' | 'median' | 'percentile'
export type ChartType = 'line' | 'bar' | 'pie' | 'donut' | 'area' | 'scatter' | 'heatmap' | 'table'

// Core metric definition
export interface Metric {
  id: MetricId
  name: string
  description: string
  type: MetricType
  unit: string
  category: string
  tags: string[]
  
  // Data source configuration
  source: MetricSource
  calculation: MetricCalculation
  
  // Display configuration
  display: MetricDisplay
  
  // Metadata
  createdAt: Date
  updatedAt: Date
  createdBy: string
  version: number
}

export interface MetricSource {
  type: 'api' | 'database' | 'file' | 'webhook' | 'computed'
  endpoint?: string
  query?: string
  refreshInterval: number
  timeout: number
  authentication?: {
    type: 'none' | 'bearer' | 'basic' | 'apikey'
    credentials?: Record<string, string>
  }
}

export interface MetricCalculation {
  aggregation: AggregationType
  formula?: string
  filters?: MetricFilter[]
  groupBy?: string[]
  having?: MetricFilter[]
}

export interface MetricDisplay {
  name: string
  description?: string
  unit: string
  format: 'number' | 'percentage' | 'currency' | 'bytes' | 'duration'
  decimals: number
  prefix?: string
  suffix?: string
}

export interface MetricFilter {
  field: string
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'nin' | 'contains' | 'regex'
  value: any
  logical?: 'and' | 'or'
}