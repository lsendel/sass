import React, { useState, useEffect } from 'react'
import {
  DragDropContext,
  Droppable,
  Draggable,
  DropResult,
} from '@hello-pangea/dnd'
import { Plus, MoreHorizontal, Filter } from 'lucide-react'
import { toast } from 'react-hot-toast'

import {
  useGetTasksQuery,
  useUpdateTaskMutation,
  type Task,
} from '../../store/api/projectManagementApi'
import { Button } from '../ui/button'
import LoadingSpinner from '../ui/LoadingSpinner'

import { TaskCard } from './TaskCard'
import { CreateTaskModal } from './CreateTaskModal'
import { TaskDetailModal } from './TaskDetailModal'

interface KanbanBoardProps {
  projectId: string
}

interface Column {
  id: Task['status']
  title: string
  color: string
  tasks: Task[]
}

const DEFAULT_COLUMNS: Array<Omit<Column, 'tasks'>> = [
  { id: 'TODO', title: 'To Do', color: 'bg-gray-100' },
  { id: 'IN_PROGRESS', title: 'In Progress', color: 'bg-blue-100' },
  { id: 'IN_REVIEW', title: 'Review', color: 'bg-yellow-100' },
  { id: 'COMPLETED', title: 'Done', color: 'bg-green-100' },
]

/**
 * KanbanBoard Component
 *
 * Interactive Kanban board with drag-and-drop functionality for task management.
 * Supports real-time updates, task creation, and status changes.
 *
 * Features:
 * - Drag-and-drop task reordering and status changes
 * - Real-time task updates via WebSocket
 * - Task creation and editing modals
 * - Column-based task organization
 * - Optimistic UI updates
 * - Loading and error states
 * - Responsive design for mobile
 */
export const KanbanBoard: React.FC<KanbanBoardProps> = ({ projectId }) => {
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null)
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [createInColumn, setCreateInColumn] = useState<Task['status']>('TODO')

  const {
    data: tasksResponse,
    error,
    isLoading,
    refetch,
  } = useGetTasksQuery({ projectId })

  const [updateTask] = useUpdateTaskMutation()

  // Extract tasks from paginated response
  const tasks = tasksResponse?.content || []

  // Organize tasks into columns
  const [columns, setColumns] = useState<Column[]>(() =>
    DEFAULT_COLUMNS.map(col => ({ ...col, tasks: [] }))
  )

  useEffect(() => {
    if (tasks.length > 0) {
      const updatedColumns = DEFAULT_COLUMNS.map(col => ({
        ...col,
        tasks: tasks.filter((task: Task) => task.status === col.id),
      }))
      setColumns(updatedColumns)
    }
  }, [tasks])

  const handleDragEnd = async (result: DropResult) => {
    const { destination, source, draggableId } = result

    if (!destination) return

    // If dropped in the same position, do nothing
    if (
      destination.droppableId === source.droppableId &&
      destination.index === source.index
    ) {
      return
    }

    const task = tasks.find((t: Task) => t.id === draggableId)
    if (!task) return

    const sourceColumn = columns.find(
      col => col.id === (source.droppableId as Task['status'])
    )
    const destColumn = columns.find(
      col => col.id === (destination.droppableId as Task['status'])
    )

    if (!sourceColumn || !destColumn) return

    // Optimistic update
    const newColumns = [...columns]
    const sourceColIndex = newColumns.findIndex(
      col => col.id === sourceColumn.id
    )
    const destColIndex = newColumns.findIndex(col => col.id === destColumn.id)

    // Remove task from source
    const [movedTask] = newColumns[sourceColIndex].tasks.splice(source.index, 1)

    // Add task to destination
    movedTask.status = destColumn.id
    newColumns[destColIndex].tasks.splice(destination.index, 0, movedTask)

    // Note: Position tracking removed as API Task doesn't have position property

    setColumns(newColumns)

    try {
      // Update task status on server
      await updateTask({
        taskId: task.id,
        task: {
          status: destColumn.id,
        },
      }).unwrap()

      toast.success(`Task moved to ${destColumn.title}`)
    } catch (error) {
      // Revert optimistic update on error
      refetch()
      toast.error('Failed to move task. Please try again.')
    }
  }

  const handleCreateTask = (columnId: Task['status']) => {
    setCreateInColumn(columnId)
    setIsCreateModalOpen(true)
  }

  const handleTaskCreated = () => {
    setIsCreateModalOpen(false)
    refetch()
  }

  const handleTaskClick = (taskId: string) => {
    setSelectedTaskId(taskId)
  }

  const handleTaskUpdated = () => {
    refetch()
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-96">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <div className="text-red-800">
          <h3 className="text-lg font-semibold mb-2">Unable to load board</h3>
          <p className="text-sm mb-4">
            There was an error loading the task board.
          </p>
          <Button onClick={() => refetch()} variant="outline" size="sm">
            Try Again
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col">
      {/* Board Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Task Board</h2>
          <p className="text-sm text-gray-600 mt-1">
            {tasks.length} tasks across {columns.length} columns
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" size="sm" className="hidden sm:inline-flex">
            <Filter className="h-4 w-4 mr-2" />
            Filter
          </Button>
          <Button variant="outline" size="sm">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Kanban Columns */}
      <DragDropContext onDragEnd={handleDragEnd}>
        <div className="flex-1 flex overflow-x-auto gap-6 pb-6">
          {columns.map(column => (
            <div
              key={column.id}
              className="flex-shrink-0 w-72 bg-gray-50 rounded-lg p-4"
            >
              {/* Column Header */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center">
                  <div
                    className={`w-3 h-3 rounded-full ${column.color} mr-2`}
                  />
                  <h3 className="font-medium text-gray-900">{column.title}</h3>
                  <span className="ml-2 px-2 py-1 bg-gray-200 text-gray-700 text-xs rounded-full">
                    {column.tasks.length}
                  </span>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleCreateTask(column.id)}
                  className="p-1 h-auto"
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>

              {/* Droppable Column */}
              <Droppable droppableId={column.id}>
                {(provided, snapshot) => (
                  <div
                    {...provided.droppableProps}
                    ref={provided.innerRef}
                    className={`space-y-3 min-h-32 transition-colors ${
                      snapshot.isDraggingOver ? 'bg-blue-50' : ''
                    }`}
                  >
                    {column.tasks.map((task, index) => (
                      <Draggable
                        key={task.id}
                        draggableId={task.id}
                        index={index}
                      >
                        {(provided, snapshot) => (
                          <div
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            className={`transition-shadow ${
                              snapshot.isDragging ? 'shadow-lg' : ''
                            }`}
                            onClick={() => handleTaskClick(task.id)}
                          >
                            <TaskCard
                              task={task}
                              isDragging={snapshot.isDragging}
                            />
                          </div>
                        )}
                      </Draggable>
                    ))}
                    {provided.placeholder}

                    {/* Empty Column State */}
                    {column.tasks.length === 0 && (
                      <div className="text-center py-8 text-gray-500">
                        <p className="text-sm mb-2">No tasks yet</p>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleCreateTask(column.id)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <Plus className="h-4 w-4 mr-1" />
                          Add task
                        </Button>
                      </div>
                    )}
                  </div>
                )}
              </Droppable>
            </div>
          ))}
        </div>
      </DragDropContext>

      {/* Modals */}
      <CreateTaskModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onTaskCreated={handleTaskCreated}
        projectId={projectId}
        initialStatus={createInColumn}
      />

      {selectedTaskId && (
        <TaskDetailModal
          taskId={selectedTaskId}
          isOpen={!!selectedTaskId}
          onClose={() => setSelectedTaskId(null)}
          onTaskUpdated={handleTaskUpdated}
        />
      )}
    </div>
  )
}
