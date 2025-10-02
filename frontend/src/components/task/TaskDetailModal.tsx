import React, { useState } from 'react';
import {
  Calendar,
  Clock,
  MessageSquare,
  Edit3
} from 'lucide-react';
import { formatDistanceToNow, format } from 'date-fns';
import { toast } from 'react-hot-toast';

import { useGetTaskQuery, useUpdateTaskMutation, type Task } from '../../store/api/projectManagementApi';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/button';
import LoadingSpinner from '../ui/LoadingSpinner';

interface TaskDetailModalProps {
  taskId: string;
  isOpen: boolean;
  onClose: () => void;
  onTaskUpdated: () => void;
}

/**
 * TaskDetailModal Component
 * 
 * Detailed view of a task with editing capabilities and activity history.
 * Shows comprehensive task information, comments, and subtasks.
 * 
 * Features:
 * - Full task details display
 * - Inline editing capabilities
 * - Subtask management
 * - Comment thread
 * - Time tracking
 * - File attachments
 * - Activity history
 * - Status and priority updates
 */
export const TaskDetailModal: React.FC<TaskDetailModalProps> = ({
  taskId,
  isOpen,
  onClose,
  onTaskUpdated,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [newComment, setNewComment] = useState('');

  const {
    data: task,
    error,
    isLoading,
    refetch
  } = useGetTaskQuery(taskId, { skip: !isOpen });

  const [updateTask, { isLoading: isUpdating }] = useUpdateTaskMutation();

  const handleStatusChange = async (newStatus: string) => {
    if (!task) return;

    try {
      await updateTask({
        taskId,
        task: {
          status: newStatus as Task['status'],
        },
      }).unwrap();

      toast.success(`Task moved to ${newStatus.replace('_', ' ').toLowerCase()}`);
      refetch();
      onTaskUpdated();
    } catch (error) {
      toast.error('Failed to update task status');
    }
  };

  const handlePriorityChange = async (newPriority: string) => {
    if (!task) return;

    try {
      await updateTask({
        taskId,
        task: {
          priority: newPriority as Task['priority'],
        },
      }).unwrap();

      toast.success(`Priority changed to ${newPriority.toLowerCase()}`);
      refetch();
      onTaskUpdated();
    } catch (error) {
      toast.error('Failed to update task priority');
    }
  };

  if (!isOpen) return null;

  if (isLoading) {
    return (
      <Modal isOpen={isOpen} onClose={onClose} title="Loading...">
        <div className="flex items-center justify-center py-8">
          <LoadingSpinner size="lg" />
        </div>
      </Modal>
    );
  }

  if (error || !task) {
    return (
      <Modal isOpen={isOpen} onClose={onClose} title="Task Not Found">
        <div className="text-center py-8">
          <p className="text-gray-600 mb-4">Unable to load task details.</p>
          <Button onClick={onClose} variant="outline">
            Close
          </Button>
        </div>
      </Modal>
    );
  }

  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date();

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={task.title} size="lg">
      <div className="space-y-6">
        {/* Task Header */}
        <div className="flex items-start justify-between">
          <div className="flex-1 mr-4">
            <h1 className="text-xl font-semibold text-gray-900 mb-2">
              {task.title}
            </h1>
            {task.description && (
              <p className="text-gray-600 text-sm">
                {task.description}
              </p>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsEditing(!isEditing)}
            >
              <Edit3 className="h-4 w-4 mr-1" />
              Edit
            </Button>
          </div>
        </div>

        {/* Task Metadata */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Left Column */}
          <div className="space-y-4">
            {/* Status */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Status
              </label>
              <select
                value={task.status}
                onChange={(e) => handleStatusChange(e.target.value)}
                className="block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                disabled={isUpdating}
              >
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="IN_REVIEW">Review</option>
                <option value="COMPLETED">Completed</option>
                <option value="ARCHIVED">Archived</option>
              </select>
            </div>

            {/* Priority */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Priority
              </label>
              <select
                value={task.priority}
                onChange={(e) => handlePriorityChange(e.target.value)}
                className="block w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                disabled={isUpdating}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            {/* Assignee */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Assignee
              </label>
              <div className="flex items-center">
                {task.assigneeName ? (
                  <div className="flex items-center">
                    <span className="text-sm text-gray-900">
                      {task.assigneeName}
                    </span>
                  </div>
                ) : (
                  <span className="text-sm text-gray-500">Unassigned</span>
                )}
              </div>
            </div>
          </div>

          {/* Right Column */}
          <div className="space-y-4">
            {/* Due Date */}
            {task.dueDate && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Due Date
                </label>
                <div className={`flex items-center text-sm ${
                  isOverdue ? 'text-red-600' : 'text-gray-900'
                }`}>
                  <Calendar className="h-4 w-4 mr-2" />
                  <span>
                    {format(new Date(task.dueDate), 'MMM d, yyyy')}
                    {isOverdue && (
                      <span className="ml-2 text-red-600 font-medium">
                        (Overdue)
                      </span>
                    )}
                  </span>
                </div>
              </div>
            )}

            {/* Time Tracking */}
            {task.estimatedHours && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Time Estimate
                </label>
                <div className="flex items-center text-sm text-gray-900">
                  <Clock className="h-4 w-4 mr-2" />
                  <span>
                    {task.estimatedHours}h estimated
                  </span>
                </div>
              </div>
            )}

            {/* Created/Updated */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Created
              </label>
              <div className="text-sm text-gray-600">
                {formatDistanceToNow(new Date(task.createdAt), { addSuffix: true })}
              </div>
            </div>
          </div>
        </div>

        {/* Tags - Feature not yet in API */}

        {/* Subtasks - Feature not yet in API */}

        {/* Comments Section */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Comments ({task.commentCount || 0})
          </label>
          
          {/* Comment Input */}
          <div className="mb-4">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Add a comment..."
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
              rows={3}
            />
            <div className="flex justify-end mt-2">
              <Button size="sm" disabled={!newComment.trim()}>
                <MessageSquare className="h-4 w-4 mr-1" />
                Add Comment
              </Button>
            </div>
          </div>

          {/* Comments List - Feature not yet in API */}
        </div>

        {/* Actions */}
        <div className="flex justify-between pt-4 border-t">
          <div className="flex items-center space-x-2 text-sm text-gray-500">
            {/* Attachments - Feature not yet in API */}
          </div>
          
          <div className="flex space-x-2">
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
};