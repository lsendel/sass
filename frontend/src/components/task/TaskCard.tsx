import React from 'react';
import { Clock, MessageSquare, Paperclip, Calendar } from 'lucide-react';
import { formatDistanceToNow, format } from 'date-fns';

import { Task } from '../../types/project';
import { Avatar } from '../ui/Avatar';
import { Badge } from '../ui/Badge';

interface TaskCardProps {
  task: Task;
  isDragging?: boolean;
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
export const TaskCard: React.FC<TaskCardProps> = ({ task, isDragging = false }) => {
  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date();
  const isDueSoon = task.dueDate && 
    new Date(task.dueDate) <= new Date(Date.now() + 24 * 60 * 60 * 1000) && 
    !isOverdue;

  const priorityColors = {
    LOW: 'bg-gray-100 text-gray-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    URGENT: 'bg-red-100 text-red-800',
  };

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

      {/* Task Tags */}
      {task.tags && task.tags.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-3">
          {task.tags.slice(0, 3).map((tag, index) => (
            <span
              key={index}
              className="inline-flex items-center px-2 py-1 rounded text-xs bg-blue-100 text-blue-800"
            >
              {tag}
            </span>
          ))}
          {task.tags.length > 3 && (
            <span className="text-xs text-gray-500">
              +{task.tags.length - 3} more
            </span>
          )}
        </div>
      )}

      {/* Due Date */}
      {task.dueDate && (
        <div className={`flex items-center mb-3 text-xs ${
          isOverdue 
            ? 'text-red-600' 
            : isDueSoon 
            ? 'text-yellow-600' 
            : 'text-gray-600'
        }`}>
          <Calendar className="h-3 w-3 mr-1 flex-shrink-0" />
          <span>
            {isOverdue 
              ? `Overdue by ${formatDistanceToNow(new Date(task.dueDate))}`
              : `Due ${formatDistanceToNow(new Date(task.dueDate), { addSuffix: true })}`
            }
          </span>
        </div>
      )}

      {/* Time Tracking */}
      {(task.estimatedHours || task.actualHours) && (
        <div className="flex items-center mb-3 text-xs text-gray-600">
          <Clock className="h-3 w-3 mr-1 flex-shrink-0" />
          <span>
            {task.actualHours || 0}h / {task.estimatedHours || 0}h
          </span>
        </div>
      )}

      {/* Task Footer */}
      <div className="flex items-center justify-between pt-3 border-t border-gray-100">
        {/* Assignee Avatar */}
        <div className="flex items-center">
          {task.assignee ? (
            <Avatar
              src={task.assignee.avatar}
              alt={`${task.assignee.firstName} ${task.assignee.lastName}`}
              size="sm"
            />
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
          {(task.attachmentCount || 0) > 0 && (
            <div className="flex items-center">
              <Paperclip className="h-3 w-3 mr-1" />
              <span>{task.attachmentCount}</span>
            </div>
          )}
        </div>
      </div>

      {/* Subtask Progress */}
      {task.subtaskCount && task.subtaskCount > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-100">
          <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
            <span>Subtasks</span>
            <span>{task.completedSubtaskCount || 0}/{task.subtaskCount}</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-1.5">
            <div
              className="bg-blue-600 h-1.5 rounded-full transition-all"
              style={{
                width: `${((task.completedSubtaskCount || 0) / task.subtaskCount) * 100}%`
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
};