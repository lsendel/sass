import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Plus, X } from 'lucide-react';
import { toast } from 'react-hot-toast';

import { useCreateTaskMutation } from '../../store/api/projectManagementApi';
import { TaskStatus } from '../../types/project';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/input';
import { TextArea } from '../ui/TextArea';
import { Select } from '../ui/Select';
import { Button } from '../ui/button';
import { Badge } from '../ui/Badge';

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTaskCreated: () => void;
  projectId: string;
  initialStatus?: TaskStatus;
}

const createTaskSchema = z.object({
  title: z
    .string()
    .min(1, 'Task title is required')
    .max(500, 'Title must be less than 500 characters'),
  description: z
    .string()
    .max(5000, 'Description must be less than 5000 characters')
    .optional(),
  status: z.enum(['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).default('MEDIUM'),
  assigneeId: z.string().optional(),
  dueDate: z.string().optional(),
  estimatedHours: z.number().min(0).optional(),
  tags: z.array(z.string()).default([]),
});

type CreateTaskFormData = z.infer<typeof createTaskSchema>;

/**
 * CreateTaskModal Component
 * 
 * Modal for creating new tasks with comprehensive form validation.
 * Supports task assignment, priority setting, and tag management.
 * 
 * Features:
 * - Form validation with Zod schema
 * - Tag input with dynamic addition/removal
 * - Priority and status selection
 * - Due date picker
 * - Time estimation input
 * - Loading states and error handling
 */
export const CreateTaskModal: React.FC<CreateTaskModalProps> = ({
  isOpen,
  onClose,
  onTaskCreated,
  projectId,
  initialStatus = 'TODO',
}) => {
  const [createTask, { isLoading }] = useCreateTaskMutation();
  const [tagInput, setTagInput] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
    reset,
    watch,
    setValue,
  } = useForm<CreateTaskFormData>({
    resolver: zodResolver(createTaskSchema),
    mode: 'onChange',
    defaultValues: {
      status: initialStatus,
      priority: 'MEDIUM',
      tags: [],
    },
  });

  const watchedTags = watch('tags');

  const onSubmit = async (data: CreateTaskFormData) => {
    try {
      await createTask({
        projectId,
        ...data,
        estimatedHours: data.estimatedHours || undefined,
        dueDate: data.dueDate || undefined,
      }).unwrap();

      toast.success('Task created successfully!');
      reset();
      onTaskCreated();
    } catch (error: any) {
      const errorMessage = error?.data?.message || 'Failed to create task';
      toast.error(errorMessage);
    }
  };

  const handleAddTag = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      const tag = tagInput.trim();
      if (tag && !watchedTags.includes(tag)) {
        setValue('tags', [...watchedTags, tag]);
        setTagInput('');
      }
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setValue('tags', watchedTags.filter(tag => tag !== tagToRemove));
  };

  const handleClose = () => {
    reset();
    setTagInput('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Create New Task">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Task Title */}
        <div>
          <Input
            {...register('title')}
            placeholder="Enter task title..."
            error={errors.title?.message}
            disabled={isLoading}
            autoFocus
          />
        </div>

        {/* Task Description */}
        <div>
          <TextArea
            {...register('description')}
            placeholder="Describe the task (optional)..."
            rows={3}
            error={errors.description?.message}
            disabled={isLoading}
          />
        </div>

        {/* Status and Priority Row */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status
            </label>
            <Select
              {...register('status')}
              disabled={isLoading}
            >
              <option value="TODO">To Do</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="REVIEW">Review</option>
              <option value="DONE">Done</option>
            </Select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Priority
            </label>
            <Select
              {...register('priority')}
              disabled={isLoading}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </Select>
          </div>
        </div>

        {/* Due Date and Estimated Hours */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Due Date (Optional)
            </label>
            <Input
              type="date"
              {...register('dueDate')}
              disabled={isLoading}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Estimated Hours (Optional)
            </label>
            <Input
              type="number"
              min="0"
              step="0.5"
              {...register('estimatedHours', { valueAsNumber: true })}
              placeholder="0"
              disabled={isLoading}
            />
          </div>
        </div>

        {/* Tags */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Tags
          </label>
          
          {/* Existing Tags */}
          {watchedTags.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-2">
              {watchedTags.map((tag) => (
                <Badge
                  key={tag}
                  variant="secondary"
                  className="flex items-center gap-1"
                >
                  {tag}
                  <button
                    type="button"
                    onClick={() => handleRemoveTag(tag)}
                    className="hover:bg-gray-300 rounded-full p-0.5"
                    disabled={isLoading}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
            </div>
          )}

          {/* Tag Input */}
          <Input
            value={tagInput}
            onChange={(e) => setTagInput(e.target.value)}
            onKeyDown={handleAddTag}
            placeholder="Type a tag and press Enter..."
            disabled={isLoading}
          />
          <p className="text-xs text-gray-500 mt-1">
            Press Enter or comma to add tags
          </p>
        </div>

        {/* Form Actions */}
        <div className="flex justify-end space-x-3 pt-4 border-t">
          <Button
            type="button"
            variant="outline"
            onClick={handleClose}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={!isValid || isLoading}
            className="bg-blue-600 hover:bg-blue-700"
          >
            {isLoading ? (
              <>
                <Loader2 className="animate-spin h-4 w-4 mr-2" />
                Creating...
              </>
            ) : (
              <>
                <Plus className="h-4 w-4 mr-2" />
                Create Task
              </>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
};