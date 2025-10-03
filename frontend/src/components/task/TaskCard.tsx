import React from 'react'
import { Clock, MessageSquare, Paperclip, Calendar } from 'lucide-react'
import { formatDistanceToNow } from 'date-fns'

import type { Task } from '../../store/api/projectManagementApi'
import { Badge } from '../ui/Badge'

interface TaskCardProps {
  task: Task
  isDragging?: boolean
}

/**
 * TaskCard Component
 *
 * Displays a task as a card in the Kanban board with key task information.
 * Shows assignee, priority, due date, and interaction counts.
 *
 * Features:
 * - Priority color coding
 * - Due date highlighting for overdue tasks
 * - Assignee avatar display
 * - Comment and attachment counts
 * - Responsive layout
 * - Hover and dragging states
 */
export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  isDragging = false,
}) => {
  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date()
  const isDueSoon =
    task.dueDate &&
    new Date(task.dueDate) <= new Date(Date.now() + 24 * 60 * 60 * 1000) &&
    !isOverdue

  const priorityColors: Record<Task['priority'], string> = {
    LOW: 'bg-gray-100 text-gray-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    CRITICAL: 'bg-red-100 text-red-800',
  }

  return (
    <div
      className={`
        bg-white rounded-lg border p-4 cursor-pointer transition-all duration-200
        hover:shadow-md
        ${isDragging ? 'shadow-lg rotate-2' : 'shadow-sm'}
        ${isOverdue ? 'border-red-300 bg-red-50' : 'border-gray-200'}
      `}
    >
      {/* Task Header */}
      <div className="flex items-start justify-between mb-3">
        <h4 className="text-sm font-medium text-gray-900 line-clamp-2 pr-2">
          {task.title}
        </h4>
        {task.priority !== 'LOW' && (
          <Badge
            className={`text-xs ${priorityColors[task.priority]} flex-shrink-0`}
          >
            {task.priority.toLowerCase()}
          </Badge>
        )}
      </div>

      {/* Task Description */}
      {task.description && (
        <p className="text-xs text-gray-600 mb-3 line-clamp-2">
          {task.description}
        </p>
      )}

      {/* Task Tags - Feature not yet in API */}

      {/* Due Date */}
      {task.dueDate && (
        <div
          className={`flex items-center mb-3 text-xs ${
            isOverdue
              ? 'text-red-600'
              : isDueSoon
                ? 'text-yellow-600'
                : 'text-gray-600'
          }`}
        >
          <Calendar className="h-3 w-3 mr-1 flex-shrink-0" />
          <span>
            {isOverdue
              ? `Overdue by ${formatDistanceToNow(new Date(task.dueDate))}`
              : `Due ${formatDistanceToNow(new Date(task.dueDate), { addSuffix: true })}`}
          </span>
        </div>
      )}

      {/* Time Tracking */}
      {task.estimatedHours && (
        <div className="flex items-center mb-3 text-xs text-gray-600">
          <Clock className="h-3 w-3 mr-1 flex-shrink-0" />
          <span>{task.estimatedHours}h estimated</span>
        </div>
      )}

      {/* Task Footer */}
      <div className="flex items-center justify-between pt-3 border-t border-gray-100">
        {/* Assignee Name */}
        <div className="flex items-center">
          {task.assigneeName ? (
            <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center">
              <span className="text-xs text-blue-600 font-medium">
                {task.assigneeName.charAt(0).toUpperCase()}
              </span>
            </div>
          ) : (
            <div className="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center">
              <span className="text-xs text-gray-500">?</span>
            </div>
          )}
        </div>

        {/* Interaction Counts */}
        <div className="flex items-center space-x-3 text-xs text-gray-500">
          {(task.commentCount || 0) > 0 && (
            <div className="flex items-center">
              <MessageSquare className="h-3 w-3 mr-1" />
              <span>{task.commentCount}</span>
            </div>
          )}
          {'attachmentCount' in task &&
            ((task as any).attachmentCount || 0) > 0 && (
              <div className="flex items-center">
                <Paperclip className="h-3 w-3 mr-1" />
                <span>{(task as any).attachmentCount}</span>
              </div>
            )}
        </div>
      </div>

      {/* Subtask Progress - Feature not yet in API */}
    </div>
  )
}
