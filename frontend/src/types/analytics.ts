/**
 * Advanced Analytics Type Definitions
 *
 * Comprehensive type system for enterprise analytics platform supporting:
 * - Real-time metrics and KPI tracking
 * - Machine learning-powered insights and predictions
 * - Custom dashboard creation and management
 * - Cross-tenant analytics and reporting
 * - Advanced data visualization and export capabilities
 * - Business intelligence and performance monitoring
 */

import { z } from 'zod'

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
export type MetricType =
  | 'counter'
  | 'gauge'
  | 'histogram'
  | 'rate'
  | 'percentage'
  | 'currency'
export type AggregationType =
  | 'sum'
  | 'avg'
  | 'min'
  | 'max'
  | 'count'
  | 'median'
  | 'percentile'
export type ChartType =
  | 'line'
  | 'bar'
  | 'pie'
  | 'donut'
  | 'area'
  | 'scatter'
  | 'heatmap'
  | 'table'

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

  // Alerting configuration
  alerting?: MetricAlerting

  // Access control
  accessLevel: 'public' | 'internal' | 'private'
  requiredPermissions: string[]

  // Metadata
  createdAt: string
  updatedAt: string
  createdBy: string
  version: number
}

interface MetricSource {
  type: 'database' | 'api' | 'stream' | 'calculation'
  query?: string
  endpoint?: string
  streamId?: string
  dependencies?: MetricId[]
}

interface MetricCalculation {
  aggregation: AggregationType
  timeWindow: string // ISO 8601 duration
  filters?: Record<string, string | number | boolean | string[]>
  groupBy?: string[]
}

interface MetricDisplay {
  format: 'number' | 'percentage' | 'currency' | 'duration' | 'bytes'
  precision: number
  prefix?: string
  suffix?: string
  colorScheme: 'default' | 'success' | 'warning' | 'error' | 'info'
}

interface MetricAlerting {
  enabled: boolean
  conditions: AlertCondition[]
  channels: AlertChannel[]
  cooldownPeriod: string // ISO 8601 duration
}

interface AlertCondition {
  type: 'threshold' | 'anomaly' | 'trend' | 'comparison'
  operator: 'gt' | 'gte' | 'lt' | 'lte' | 'eq' | 'neq'
  value: number | string
  severity: 'low' | 'medium' | 'high' | 'critical'
}

interface AlertChannel {
  type: 'email' | 'slack' | 'webhook' | 'sms'
  target: string
  enabled: boolean
}

// Data point structure
export interface DataPoint {
  timestamp: string
  value: number | string
  metadata?: Record<string, string | number | boolean>
}

export interface MetricData {
  metricId: MetricId
  period: TimePeriod
  granularity: TimeGranularity
  data: DataPoint[]
  aggregations?: {
    sum?: number
    avg?: number
    min?: number
    max?: number
    count?: number
    median?: number
    percentiles?: Record<string, number>
  }
  metadata: {
    totalPoints: number
    startTime: string
    endTime: string
    sampling?: string
    quality: 'high' | 'medium' | 'low'
  }
}

// Dashboard configuration
export interface Dashboard {
  id: DashboardId
  name: string
  description: string
  category: string
  tags: string[]

  // Layout configuration
  layout: DashboardLayout
  widgets: DashboardWidget[]

  // Access control
  visibility: 'public' | 'private' | 'shared'
  sharedWith: string[]
  permissions: DashboardPermissions

  // Personalization
  filters: DashboardFilter[]
  variables: DashboardVariable[]

  // Refresh configuration
  autoRefresh: boolean
  refreshInterval?: number // seconds

  // Metadata
  createdAt: string
  updatedAt: string
  createdBy: string
  lastViewedAt?: string
  viewCount: number
  isTemplate: boolean
}

interface DashboardLayout {
  type: 'grid' | 'flow' | 'custom'
  columns: number
  rows?: number
  gap: number
  responsive: boolean
}

interface WidgetConfig {
  // Chart-specific config
  chartType?: ChartType
  xAxis?: AxisConfig
  yAxis?: AxisConfig
  legend?: LegendConfig
  colors?: string[]

  // Table-specific config
  columns?: ColumnConfig[]
  pagination?: PaginationConfig
  sorting?: SortConfig

  // Metric-specific config
  format?: MetricDisplay
  comparison?: ComparisonConfig

  // Text-specific config
  content?: string
  markdown?: boolean

  // Common config
  showTitle?: boolean
  showDescription?: boolean
  interactive?: boolean
}

// Export DataSource and DashboardWidget interfaces
export interface DataSource {
  id: string
  type: 'metric' | 'query' | 'api'
  metricId?: MetricId
  query?: string
  endpoint?: string
  parameters?: Record<string, string | number | boolean>
  transform?: DataTransformation[]
}

export interface DashboardWidget {
  id: string
  type: 'metric' | 'chart' | 'table' | 'text' | 'image' | 'iframe'
  title: string
  description?: string
  config: WidgetConfig | Record<string, unknown>
  dataSources: DataSource[]
  position: { x: number; y: number; w: number; h: number }
  styling?: WidgetStyling
}

interface DataTransformation {
  type: 'filter' | 'aggregate' | 'calculate' | 'sort' | 'limit'
  config: Record<string, string | number | boolean>
}

interface AxisConfig {
  label?: string
  min?: number
  max?: number
  scale: 'linear' | 'logarithmic' | 'time'
  format?: string
}

interface LegendConfig {
  show: boolean
  position: 'top' | 'bottom' | 'left' | 'right'
  align: 'start' | 'center' | 'end'
}

interface ColumnConfig {
  key: string
  label: string
  type: 'text' | 'number' | 'date' | 'boolean' | 'link'
  width?: number | string
  sortable?: boolean
  filterable?: boolean
  format?: string
}

interface PaginationConfig {
  enabled: boolean
  pageSize: number
  showSizeSelector: boolean
}

interface SortConfig {
  enabled: boolean
  defaultSort?: {
    column: string
    direction: 'asc' | 'desc'
  }
}

interface ComparisonConfig {
  enabled: boolean
  period: 'previous_period' | 'same_period_last_year' | 'custom'
  customPeriod?: string
}

interface WidgetStyling {
  backgroundColor?: string
  borderColor?: string
  borderWidth?: number
  borderRadius?: number
  padding?: number
  margin?: number
  fontSize?: number
  fontWeight?: 'normal' | 'bold'
  textAlign?: 'left' | 'center' | 'right'
}

interface DashboardPermissions {
  canView: string[]
  canEdit: string[]
  canShare: string[]
  canDelete: string[]
}

interface DashboardFilter {
  id: string
  name: string
  type: 'select' | 'multiselect' | 'date' | 'daterange' | 'text' | 'number'
  options?: FilterOption[]
  defaultValue?: string | number | boolean | string[]
  required: boolean
}

interface FilterOption {
  label: string
  value: string | number | boolean
  group?: string
}

interface DashboardVariable {
  id: string
  name: string
  type: 'constant' | 'query' | 'custom' | 'datasource'
  value?: string | number | boolean
  query?: string
  options?: VariableOption[]
}

interface VariableOption {
  label: string
  value: string | number | boolean
}

// Report configuration
export interface Report {
  id: ReportId
  name: string
  description: string
  category: string
  type: 'standard' | 'custom' | 'scheduled'

  // Data configuration
  dataSources: ReportDataSource[]
  filters: ReportFilter[]
  grouping: ReportGrouping[]
  sorting: ReportSorting[]

  // Output configuration
  format: 'pdf' | 'excel' | 'csv' | 'json'
  template?: string
  styling?: ReportStyling

  // Scheduling (if applicable)
  schedule?: ReportSchedule

  // Distribution
  recipients: ReportRecipient[]
  deliveryMethod: 'email' | 'slack' | 'webhook' | 'storage'

  // Access control
  permissions: ReportPermissions

  // Metadata
  createdAt: string
  updatedAt: string
  createdBy: string
  lastRunAt?: string
  runCount: number
  status: 'active' | 'paused' | 'error'
}

interface ReportDataSource {
  id: string
  name: string
  type: 'metrics' | 'database' | 'api'
  config: Record<string, string | number | boolean>
  joins?: ReportJoin[]
}

interface ReportJoin {
  type: 'inner' | 'left' | 'right' | 'full'
  targetSource: string
  condition: string
}

interface ReportFilter {
  field: string
  operator:
    | 'eq'
    | 'neq'
    | 'gt'
    | 'gte'
    | 'lt'
    | 'lte'
    | 'in'
    | 'not_in'
    | 'contains'
    | 'starts_with'
  value: string | number | boolean | string[]
  dynamic?: boolean
}

interface ReportGrouping {
  field: string
  aggregations: Array<{
    function: AggregationType
    field: string
    alias?: string
  }>
}

interface ReportSorting {
  field: string
  direction: 'asc' | 'desc'
  nulls?: 'first' | 'last'
}

interface ReportStyling {
  theme: 'default' | 'dark' | 'light' | 'custom'
  colors?: string[]
  fonts?: {
    header: string
    body: string
    monospace: string
  }
  logo?: string
  watermark?: string
}

interface ReportSchedule {
  frequency: 'hourly' | 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly'
  interval: number
  startDate: string
  endDate?: string
  timezone: string
  cron?: string // For complex scheduling
}

interface ReportRecipient {
  type: 'user' | 'group' | 'email'
  identifier: string
  format?: 'pdf' | 'excel' | 'csv'
}

interface ReportPermissions {
  canRun: string[]
  canSchedule: string[]
  canEdit: string[]
  canDelete: string[]
}

// Analytics insights and ML predictions
export interface Insight {
  id: string
  type: 'trend' | 'anomaly' | 'correlation' | 'prediction' | 'recommendation'
  severity: 'low' | 'medium' | 'high' | 'critical'
  confidence: number // 0-1

  title: string
  description: string
  summary: string

  // Related data
  metricIds: MetricId[]
  timeRange: {
    start: string
    end: string
  }

  // ML model information
  model?: {
    name: string
    version: string
    accuracy?: number
    lastTrained?: string
  }

  // Recommendations
  recommendations?: Recommendation[]

  // Metadata
  generatedAt: string
  expiresAt?: string
  viewedAt?: string
  acknowledged?: boolean
}

interface Recommendation {
  action: string
  impact: 'low' | 'medium' | 'high'
  effort: 'low' | 'medium' | 'high'
  description: string
  resources?: string[]
}

// Real-time analytics configuration
export interface AnalyticsStream {
  id: string
  name: string
  source: 'websocket' | 'sse' | 'webhook' | 'polling'
  endpoint?: string
  metrics: StreamMetric[]
  filters?: StreamFilter[]
  bufferSize: number
  updateInterval: number // milliseconds
  retention: string // ISO 8601 duration
  active: boolean
}

interface StreamMetric {
  metricId: MetricId
  aggregation: AggregationType
  windowSize: string // ISO 8601 duration
}

interface StreamFilter {
  field: string
  operator: string
  value: string | number | boolean | string[]
}

// Analytics configuration and settings
export interface AnalyticsConfiguration {
  // Data retention
  dataRetention: {
    raw: string // ISO 8601 duration
    aggregated: {
      hourly: string
      daily: string
      weekly: string
      monthly: string
    }
  }

  // Performance settings
  performance: {
    queryTimeout: number
    maxDataPoints: number
    cacheDuration: number
    enableCompression: boolean
  }

  // ML settings
  machineLearning: {
    enabled: boolean
    models: MLModelConfig[]
    training: {
      frequency: string
      minDataPoints: number
      validationSplit: number
    }
  }

  // Export settings
  export: {
    maxFileSize: number
    allowedFormats: string[]
    retention: string
  }

  // Security settings
  security: {
    dataAnonymization: boolean
    auditLevel: 'basic' | 'detailed' | 'comprehensive'
    encryptionAtRest: boolean
    accessLogging: boolean
  }
}

interface MLModelConfig {
  name: string
  type: 'forecasting' | 'anomaly_detection' | 'classification' | 'clustering'
  algorithm: string
  parameters: Record<string, string | number | boolean>
  metrics: string[]
  enabled: boolean
}

// Query and search interfaces
export interface AnalyticsQuery {
  metrics: MetricId[]
  timeRange: {
    start: string
    end: string
    granularity: TimeGranularity
  }
  filters?: QueryFilter[]
  groupBy?: string[]
  orderBy?: QuerySort[]
  limit?: number
  offset?: number
}

interface QueryFilter {
  field: string
  operator: string
  value: string | number | boolean | string[]
  type?: 'and' | 'or'
}

interface QuerySort {
  field: string
  direction: 'asc' | 'desc'
}

export interface AnalyticsQueryResult {
  query: AnalyticsQuery
  data: MetricData[]
  insights: Insight[]
  executionTime: number
  cached: boolean
  nextPageToken?: string
}

// Validation schemas
export const MetricSchema = z.object({
  id: z.string(),
  name: z.string().min(1).max(100),
  description: z.string().max(500),
  type: z.enum([
    'counter',
    'gauge',
    'histogram',
    'rate',
    'percentage',
    'currency',
  ]),
  unit: z.string(),
  category: z.string(),
  tags: z.array(z.string()),
  accessLevel: z.enum(['public', 'internal', 'private']),
  requiredPermissions: z.array(z.string()),
})

export const DashboardSchema = z.object({
  id: z.string(),
  name: z.string().min(1).max(100),
  description: z.string().max(500),
  category: z.string(),
  tags: z.array(z.string()),
  visibility: z.enum(['public', 'private', 'shared']),
  autoRefresh: z.boolean(),
  refreshInterval: z.number().min(5).optional(),
})

export const ReportSchema = z.object({
  id: z.string(),
  name: z.string().min(1).max(100),
  description: z.string().max(500),
  category: z.string(),
  type: z.enum(['standard', 'custom', 'scheduled']),
  format: z.enum(['pdf', 'excel', 'csv', 'json']),
  deliveryMethod: z.enum(['email', 'slack', 'webhook', 'storage']),
  status: z.enum(['active', 'paused', 'error']),
})

export const AnalyticsQuerySchema = z.object({
  metrics: z.array(z.string()),
  timeRange: z.object({
    start: z.string().datetime(),
    end: z.string().datetime(),
    granularity: z.enum(['minute', 'hour', 'day', 'week', 'month']),
  }),
  filters: z
    .array(
      z.object({
        field: z.string(),
        operator: z.string(),
        value: z.union([
          z.string(),
          z.number(),
          z.boolean(),
          z.array(z.string()),
        ]),
        type: z.enum(['and', 'or']).optional(),
      })
    )
    .optional(),
  groupBy: z.array(z.string()).optional(),
  limit: z.number().min(1).max(10000).optional(),
  offset: z.number().min(0).optional(),
})

// Request/Response types
export type CreateMetricRequest = z.infer<typeof MetricSchema>
export type UpdateMetricRequest = Partial<CreateMetricRequest>
export type CreateDashboardRequest = z.infer<typeof DashboardSchema>
export type UpdateDashboardRequest = Partial<CreateDashboardRequest>
export type CreateReportRequest = z.infer<typeof ReportSchema>
export type UpdateReportRequest = Partial<CreateReportRequest>
export type AnalyticsQueryRequest = z.infer<typeof AnalyticsQuerySchema>

// Response interfaces
export interface MetricListResponse {
  metrics: Metric[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
}

export interface DashboardListResponse {
  dashboards: Dashboard[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
}

export interface ReportListResponse {
  reports: Report[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
}

// Utility types
export interface AnalyticsContext {
  tenantId?: string
  userId?: string
  organizationId?: string
  permissions: string[]
  preferences: AnalyticsPreferences
}

interface AnalyticsPreferences {
  defaultTimeRange: TimePeriod
  defaultGranularity: TimeGranularity
  preferredChartType: ChartType
  darkMode: boolean
  autoRefresh: boolean
  notifications: {
    insights: boolean
    alerts: boolean
    reports: boolean
  }
}
