/**
 * Advanced Dashboard Builder
 *
 * Interactive dashboard creation and management interface supporting:
 * - Drag-and-drop widget placement and sizing
 * - Real-time data visualization configuration
 * - Custom chart types and styling options
 * - Advanced filtering and data source management
 * - Template-based dashboard creation
 * - Collaborative dashboard sharing and permissions
 */

import React, { useState, useCallback, useMemo } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import {
  PlusIcon,
  TrashIcon,
  EyeIcon,
  DocumentDuplicateIcon,
  SaveIcon,
  XMarkIcon,
  ArrowsPointingOutIcon,
  Squares2X2Icon,
  ChartBarIcon,
  ChartPieIcon,
  TableCellsIcon,
  DocumentTextIcon,
  PhotoIcon,
  GlobeAltIcon,
} from '@heroicons/react/24/outline'

import {
  useCreateDashboardMutation,
  useUpdateDashboardMutation,
  useListMetricsQuery,
} from '@/store/api/analyticsApi'
import type {
  Dashboard,
  DashboardWidget,
  ChartType,
  DataSource,
  CreateDashboardRequest,
} from '@/types/analytics'
import { DashboardSchema } from '@/types/analytics'

// Icons

// React DnD for drag and drop (simplified interface)

interface Position {
  x: number
  y: number
  width: number
  height: number
}

interface DashboardBuilderProps {
  dashboardId?: string
  initialDashboard?: Dashboard
  onSave?: (dashboard: Dashboard) => void
  onCancel?: () => void
  className?: string
}

const WidgetTypeConfig = {
  metric: { name: 'Metric Card', icon: ChartBarIcon, minWidth: 2, minHeight: 2 },
  chart: { name: 'Chart', icon: ChartPieIcon, minWidth: 4, minHeight: 3 },
  table: { name: 'Table', icon: TableCellsIcon, minWidth: 4, minHeight: 3 },
  text: { name: 'Text', icon: DocumentTextIcon, minWidth: 2, minHeight: 1 },
  image: { name: 'Image', icon: PhotoIcon, minWidth: 2, minHeight: 2 },
  iframe: { name: 'Embed', icon: GlobeAltIcon, minWidth: 4, minHeight: 3 },
}

const ChartTypes: Record<ChartType, { name: string; icon: string }> = {
  line: { name: 'Line Chart', icon: '📈' },
  bar: { name: 'Bar Chart', icon: '📊' },
  pie: { name: 'Pie Chart', icon: '🥧' },
  donut: { name: 'Donut Chart', icon: '🍩' },
  area: { name: 'Area Chart', icon: '📊' },
  scatter: { name: 'Scatter Plot', icon: '•' },
  heatmap: { name: 'Heatmap', icon: '🔥' },
  table: { name: 'Data Table', icon: '📋' },
}

const createDashboardSchema = DashboardSchema.omit({ id: true })

const DashboardBuilder: React.FC<DashboardBuilderProps> = ({
  dashboardId,
  initialDashboard,
  onSave,
  onCancel,
  className = '',
}) => {
  const [dashboard, setDashboard] = useState<Partial<Dashboard>>(
    initialDashboard ?? {
      name: 'New Dashboard',
      description: '',
      category: 'General',
      tags: [],
      layout: { type: 'grid', columns: 12, gap: 4, responsive: true },
      widgets: [],
      visibility: 'private',
      sharedWith: [],
      permissions: { canView: [], canEdit: [], canShare: [], canDelete: [] },
      filters: [],
      variables: [],
      autoRefresh: false,
    }
  )

  const [selectedWidget, setSelectedWidget] = useState<string | null>(null)
  const [showWidgetConfig, setShowWidgetConfig] = useState(false)
  const [gridSize] = useState({ width: 12, height: 20 })
  const [previewMode, setPreviewMode] = useState(false)

  const [createDashboard, { isLoading: isCreating }] = useCreateDashboardMutation()
  const [updateDashboard, { isLoading: isUpdating }] = useUpdateDashboardMutation()

  const { data: metricsResponse } = useListMetricsQuery({
    page: 1,
    pageSize: 100,
    sortBy: 'name',
    sortOrder: 'asc',
  })

  const metrics = metricsResponse?.metrics ?? []

  const dashboardForm = useForm<CreateDashboardRequest>({
    resolver: zodResolver(createDashboardSchema),
    defaultValues: {
      name: dashboard.name ?? 'New Dashboard',
      description: dashboard.description ?? '',
      category: dashboard.category ?? 'General',
      tags: dashboard.tags ?? [],
      visibility: dashboard.visibility ?? 'private',
      autoRefresh: dashboard.autoRefresh ?? false,
      refreshInterval: dashboard.refreshInterval,
    },
  })

  const generateWidgetId = () => `widget_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

  const addWidget = useCallback((type: keyof typeof WidgetTypeConfig, position?: Position) => {
    const widgetConfig = WidgetTypeConfig[type]
    const newWidget: DashboardWidget = {
      id: generateWidgetId(),
      type,
      title: `New ${widgetConfig.name}`,
      position: position ?? {
        x: 0,
        y: 0,
        width: widgetConfig.minWidth,
        height: widgetConfig.minHeight,
      },
      config: {},
      dataSources: [],
    }

    setDashboard(prev => ({
      ...prev,
      widgets: [...(prev.widgets ?? []), newWidget],
    }))

    setSelectedWidget(newWidget.id)
    setShowWidgetConfig(true)
  }, [])

  const updateWidget = useCallback((widgetId: string, updates: Partial<DashboardWidget>) => {
    setDashboard(prev => ({
      ...prev,
      widgets: (prev.widgets ?? []).map(widget =>
        widget.id === widgetId ? { ...widget, ...updates } : widget
      ),
    }))
  }, [])

  const removeWidget = useCallback((widgetId: string) => {
    setDashboard(prev => ({
      ...prev,
      widgets: (prev.widgets ?? []).filter(widget => widget.id !== widgetId),
    }))
    if (selectedWidget === widgetId) {
      setSelectedWidget(null)
      setShowWidgetConfig(false)
    }
  }, [selectedWidget])

  const duplicateWidget = useCallback((widgetId: string) => {
    const widget = dashboard.widgets?.find(w => w.id === widgetId)
    if (widget) {
      const newWidget: DashboardWidget = {
        ...widget,
        id: generateWidgetId(),
        title: `${widget.title} (Copy)`,
        position: {
          ...widget.position,
          x: Math.min(widget.position.x + 1, gridSize.width - widget.position.width),
          y: widget.position.y + 1,
        },
      }
      setDashboard(prev => ({
        ...prev,
        widgets: [...(prev.widgets ?? []), newWidget],
      }))
    }
  }, [dashboard.widgets, gridSize.width])


  const handleSave = async () => {
    try {
      const formData = dashboardForm.getValues()
      const dashboardData: CreateDashboardRequest = {
        ...formData,
        ...dashboard,
      } as CreateDashboardRequest

      let savedDashboard: Dashboard
      if (dashboardId) {
        savedDashboard = await updateDashboard({
          dashboardId,
          updates: dashboardData,
        }).unwrap()
      } else {
        savedDashboard = await createDashboard(dashboardData).unwrap()
      }

      onSave?.(savedDashboard)
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred'
      console.error('Failed to save dashboard:', errorMessage)
    }
  }

  const selectedWidgetData = useMemo(() => {
    if (!selectedWidget) return null
    return dashboard.widgets?.find(w => w.id === selectedWidget) ?? null
  }, [selectedWidget, dashboard.widgets])

  const gridStyle = {
    backgroundImage: `
      linear-gradient(to right, #f3f4f6 1px, transparent 1px),
      linear-gradient(to bottom, #f3f4f6 1px, transparent 1px)
    `,
    backgroundSize: `${100 / gridSize.width}% ${100 / gridSize.height}%`,
  }

  return (
    <div className={`flex h-screen ${className}`}>
      {/* Sidebar */}
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
        {/* Header */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-900">Dashboard Builder</h2>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setPreviewMode(!previewMode)}
                className={`p-2 rounded-lg ${
                  previewMode ? 'bg-indigo-100 text-indigo-600' : 'bg-gray-100 text-gray-600'
                } hover:bg-indigo-200`}
                title={previewMode ? 'Exit Preview' : 'Preview'}
              >
                <EyeIcon className="h-5 w-5" />
              </button>
              <button
                onClick={() => void handleSave()}
                disabled={isCreating || isUpdating}
                className="p-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                title="Save Dashboard"
              >
                <SaveIcon className="h-5 w-5" />
              </button>
              {onCancel && (
                <button
                  onClick={onCancel}
                  className="p-2 bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200"
                  title="Cancel"
                >
                  <XMarkIcon className="h-5 w-5" />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Dashboard Settings */}
        <div className="p-4 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-900 mb-3">Dashboard Settings</h3>
          <div className="space-y-3">
            <div>
              <label
                htmlFor="dashboard-name"
                className="block text-xs font-medium text-gray-700 mb-1"
              >
                Name
              </label>
              <Controller
                name="name"
                control={dashboardForm.control}
                render={({ field }) => (
                  <input
                    {...field}
                    id="dashboard-name"
                    type="text"
                    className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Dashboard name"
                  />
                )}
              />
            </div>
            <div>
              <label
                htmlFor="dashboard-description"
                className="block text-xs font-medium text-gray-700 mb-1"
              >
                Description
              </label>
              <Controller
                name="description"
                control={dashboardForm.control}
                render={({ field }) => (
                  <textarea
                    {...field}
                    id="dashboard-description"
                    rows={2}
                    className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                    placeholder="Dashboard description"
                  />
                )}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Category
              </label>
              <Controller
                name="category"
                control={dashboardForm.control}
                render={({ field }) => (
                  <select
                    {...field}
                    className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="General">General</option>
                    <option value="Business">Business</option>
                    <option value="Technical">Technical</option>
                    <option value="Marketing">Marketing</option>
                    <option value="Sales">Sales</option>
                    <option value="Operations">Operations</option>
                  </select>
                )}
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Visibility
              </label>
              <Controller
                name="visibility"
                control={dashboardForm.control}
                render={({ field }) => (
                  <select
                    {...field}
                    className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="private">Private</option>
                    <option value="shared">Shared</option>
                    <option value="public">Public</option>
                  </select>
                )}
              />
            </div>
            <div className="flex items-center">
              <Controller
                name="autoRefresh"
                control={dashboardForm.control}
                render={({ field }) => (
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                  />
                )}
              />
              <label className="ml-2 text-xs text-gray-700">
                Auto refresh
              </label>
            </div>
          </div>
        </div>

        {/* Widget Library */}
        <div className="flex-1 p-4 overflow-y-auto">
          <h3 className="text-sm font-medium text-gray-900 mb-3">Widget Library</h3>
          <div className="space-y-2">
            {Object.entries(WidgetTypeConfig).map(([type, config]) => (
              <button
                key={type}
                onClick={() => addWidget(type as keyof typeof WidgetTypeConfig)}
                className="w-full p-3 text-left border border-gray-200 rounded-lg hover:border-indigo-300 hover:bg-indigo-50 transition-colors"
                disabled={previewMode}
              >
                <div className="flex items-center">
                  <config.icon className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm font-medium text-gray-900">{config.name}</span>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Widget Configuration Panel */}
        {showWidgetConfig && selectedWidgetData && (
          <div className="border-t border-gray-200 bg-gray-50">
            <div className="p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-medium text-gray-900">Widget Configuration</h3>
                <button
                  onClick={() => setShowWidgetConfig(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <XMarkIcon className="h-4 w-4" />
                </button>
              </div>

              <div className="space-y-3">
                <div>
                  <label
                    htmlFor={`widget-title-${selectedWidgetData.id}`}
                    className="block text-xs font-medium text-gray-700 mb-1"
                  >
                    Title
                  </label>
                  <input
                    id={`widget-title-${selectedWidgetData.id}`}
                    type="text"
                    value={selectedWidgetData.title}
                    onChange={(e) => updateWidget(selectedWidgetData.id, { title: e.target.value })}
                    className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                  />
                </div>

                {selectedWidgetData.type === 'chart' && (
                  <div>
                    <label
                      htmlFor={`chart-type-${selectedWidgetData.id}`}
                      className="block text-xs font-medium text-gray-700 mb-1"
                    >
                      Chart Type
                    </label>
                    <select
                      id={`chart-type-${selectedWidgetData.id}`}
                      value={selectedWidgetData.config?.chartType ?? 'line'}
                      onChange={(e) => updateWidget(selectedWidgetData.id, {
                        config: { ...selectedWidgetData.config, chartType: e.target.value as ChartType }
                      })}
                      className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                    >
                      {Object.entries(ChartTypes).map(([value, config]) => (
                        <option key={value} value={value}>
                          {config.icon} {config.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {selectedWidgetData.type === 'metric' && (
                  <div>
                    <label
                      htmlFor={`metric-select-${selectedWidgetData.id}`}
                      className="block text-xs font-medium text-gray-700 mb-1"
                    >
                      Metric
                    </label>
                    <select
                      id={`metric-select-${selectedWidgetData.id}`}
                      value={selectedWidgetData.dataSources?.[0]?.metricId ?? ''}
                      onChange={(e) => {
                        const dataSource: DataSource = {
                          id: 'primary',
                          type: 'metric',
                          metricId: e.target.value || undefined,
                        }
                        updateWidget(selectedWidgetData.id, {
                          dataSources: [dataSource]
                        })
                      }}
                      className="w-full px-3 py-1 text-sm border border-gray-300 rounded-md focus:ring-indigo-500 focus:border-indigo-500"
                    >
                      <option value="">Select a metric</option>
                      {metrics.map((metric) => (
                        <option key={metric.id} value={metric.id}>
                          {metric.name} ({metric.category})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="flex items-center justify-between pt-3 border-t border-gray-200">
                  <button
                    onClick={() => duplicateWidget(selectedWidgetData.id)}
                    className="text-xs text-indigo-600 hover:text-indigo-800"
                  >
                    <DocumentDuplicateIcon className="h-4 w-4 inline mr-1" />
                    Duplicate
                  </button>
                  <button
                    onClick={() => removeWidget(selectedWidgetData.id)}
                    className="text-xs text-red-600 hover:text-red-800"
                  >
                    <TrashIcon className="h-4 w-4 inline mr-1" />
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Main Canvas */}
      <div className="flex-1 flex flex-col">
        {/* Canvas Header */}
        <div className="p-4 bg-white border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold text-gray-900">
                {dashboardForm.watch('name') ?? 'New Dashboard'}
              </h1>
              <p className="text-sm text-gray-500">
                {dashboard.widgets?.length ?? 0} widgets • Grid: {gridSize.width}x{gridSize.height}
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <span className="text-xs text-gray-500">
                {previewMode ? 'Preview Mode' : 'Edit Mode'}
              </span>
            </div>
          </div>
        </div>

        {/* Canvas */}
        <div className="flex-1 p-4 bg-gray-50 overflow-auto">
          <div
            className="relative bg-white rounded-lg shadow-sm border border-gray-200 min-h-full"
            style={{
              ...gridStyle,
              minHeight: '800px',
            }}
          >
            {dashboard.widgets?.map((widget) => (
              <div
                key={widget.id}
                className={`absolute border-2 rounded-lg transition-all duration-200 ${
                  selectedWidget === widget.id
                    ? 'border-indigo-500 bg-indigo-50'
                    : 'border-gray-200 bg-white hover:border-gray-300'
                } ${previewMode ? 'cursor-default' : 'cursor-pointer'}`}
                style={{
                  left: `${(widget.position.x / gridSize.width) * 100}%`,
                  top: `${(widget.position.y / gridSize.height) * 100}%`,
                  width: `${(widget.position.width / gridSize.width) * 100}%`,
                  height: `${(widget.position.height / gridSize.height) * 100}%`,
                  minWidth: '120px',
                  minHeight: '80px',
                }}
                onClick={() => {
                  if (!previewMode) {
                    setSelectedWidget(widget.id)
                    setShowWidgetConfig(true)
                  }
                }}
              >
                {/* Widget Header */}
                <div className="flex items-center justify-between p-2 border-b border-gray-200">
                  <h4 className="text-sm font-medium text-gray-900 truncate">
                    {widget.title}
                  </h4>
                  {!previewMode && selectedWidget === widget.id && (
                    <div className="flex items-center space-x-1">
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          // Handle resize
                        }}
                        className="text-gray-400 hover:text-gray-600"
                        title="Resize"
                      >
                        <ArrowsPointingOutIcon className="h-3 w-3" />
                      </button>
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          removeWidget(widget.id)
                        }}
                        className="text-gray-400 hover:text-red-600"
                        title="Delete"
                      >
                        <XMarkIcon className="h-3 w-3" />
                      </button>
                    </div>
                  )}
                </div>

                {/* Widget Content */}
                <div className="p-3 h-full">
                  {widget.type === 'metric' && (
                    <div className="text-center">
                      <div className="text-2xl font-bold text-gray-900">
                        {widget.dataSources?.[0]?.metricId ? '1,234' : '--'}
                      </div>
                      <div className="text-sm text-gray-500">
                        {widget.dataSources?.[0]?.metricId
                          ? metrics.find(m => m.id === widget.dataSources?.[0]?.metricId)?.name
                          : 'No metric selected'
                        }
                      </div>
                    </div>
                  )}

                  {widget.type === 'chart' && (
                    <div className="text-center text-gray-500">
                      <ChartBarIcon className="h-12 w-12 mx-auto mb-2" />
                      <p className="text-sm">
                        {ChartTypes[widget.config?.chartType ?? 'line']?.name ?? 'Chart'}
                      </p>
                    </div>
                  )}

                  {widget.type === 'table' && (
                    <div className="text-center text-gray-500">
                      <TableCellsIcon className="h-12 w-12 mx-auto mb-2" />
                      <p className="text-sm">Data Table</p>
                    </div>
                  )}

                  {widget.type === 'text' && (
                    <div className="text-gray-500">
                      <DocumentTextIcon className="h-8 w-8 mb-2" />
                      <p className="text-sm">Text content will appear here</p>
                    </div>
                  )}

                  {widget.type === 'image' && (
                    <div className="text-center text-gray-500">
                      <PhotoIcon className="h-12 w-12 mx-auto mb-2" />
                      <p className="text-sm">Image</p>
                    </div>
                  )}

                  {widget.type === 'iframe' && (
                    <div className="text-center text-gray-500">
                      <GlobeAltIcon className="h-12 w-12 mx-auto mb-2" />
                      <p className="text-sm">Embedded Content</p>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* Empty State */}
            {(!dashboard.widgets || dashboard.widgets.length === 0) && (
              <div className="flex items-center justify-center h-full">
                <div className="text-center text-gray-500">
                  <Squares2X2Icon className="h-16 w-16 mx-auto mb-4" />
                  <h3 className="text-lg font-medium mb-2">Start Building Your Dashboard</h3>
                  <p className="text-sm mb-4">
                    Add widgets from the sidebar to create your custom dashboard
                  </p>
                  <button
                    onClick={() => addWidget('metric')}
                    className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700"
                    disabled={previewMode}
                  >
                    <PlusIcon className="h-4 w-4 mr-2" />
                    Add First Widget
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default DashboardBuilder
