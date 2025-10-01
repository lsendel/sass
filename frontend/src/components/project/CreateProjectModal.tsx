import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Loader2, Save } from 'lucide-react';
import { toast } from 'react-hot-toast';

import { useCreateProjectMutation } from '../../store/api/projectManagementApi';
import { Button } from '../ui/button';
import { Modal } from '../ui/Modal';
import { Input } from '../ui/input';
import { TextArea } from '../ui/TextArea';
import { Select } from '../ui/Select';

interface CreateProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onProjectCreated: () => void;
  workspaceId: string;
}

// Validation schema based on the data model
const createProjectSchema = z.object({
  name: z
    .string()
    .min(1, 'Project name is required')
    .max(200, 'Project name must be less than 200 characters')
    .regex(/^[a-zA-Z0-9\s-_.]+$/, 'Project name contains invalid characters'),
  description: z
    .string()
    .max(1000, 'Description must be less than 1000 characters')
    .optional(),
  color: z
    .string()
    .regex(/^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$/, 'Invalid color format'),
  template: z
    .string(),
  privacy: z
    .enum(['PRIVATE', 'WORKSPACE', 'PUBLIC']),
  startDate: z
    .string()
    .optional(),
  endDate: z
    .string()
    .optional(),
});

type CreateProjectFormData = z.infer<typeof createProjectSchema>;

// Project templates with predefined settings
const PROJECT_TEMPLATES = [
  { id: 'default', name: 'Default Project', description: 'Basic project setup' },
  { id: 'web-development', name: 'Web Development', description: 'Frontend & backend development' },
  { id: 'design', name: 'Design Project', description: 'UI/UX design workflows' },
  { id: 'marketing', name: 'Marketing Campaign', description: 'Marketing and promotional activities' },
  { id: 'research', name: 'Research Project', description: 'Research and analysis workflows' },
];

// Predefined color options
const COLOR_OPTIONS = [
  '#2563eb', '#dc2626', '#059669', '#d97706', '#7c3aed',
  '#db2777', '#0891b2', '#65a30d', '#ea580c', '#9333ea'
];

/**
 * CreateProjectModal Component
 * 
 * Modal dialog for creating new projects with comprehensive form validation.
 * Features auto-save drafts, template selection, and color customization.
 * 
 * Features:
 * - Form validation with Zod schema
 * - Auto-save to localStorage
 * - Template selection
 * - Color picker
 * - Real-time validation feedback
 * - Loading states
 */
export const CreateProjectModal: React.FC<CreateProjectModalProps> = ({
  isOpen,
  onClose,
  onProjectCreated,
  workspaceId,
}) => {
  const [createProject, { isLoading }] = useCreateProjectMutation();
  const [autoSaveKey] = useState(`create-project-draft-${workspaceId}`);

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
    reset,
    watch,
    setValue,
    getValues,
  } = useForm<CreateProjectFormData>({
    resolver: zodResolver(createProjectSchema),
    mode: 'onChange',
    defaultValues: {
      color: '#2563eb',
      privacy: 'WORKSPACE',
      template: 'default',
    },
  });

  const watchedValues = watch();

  // Auto-save draft to localStorage
  useEffect(() => {
    if (isOpen) {
      const savedDraft = localStorage.getItem(autoSaveKey);
      if (savedDraft) {
        try {
          const draft = JSON.parse(savedDraft);
          Object.keys(draft).forEach((key) => {
            setValue(key as keyof CreateProjectFormData, draft[key]);
          });
        } catch (error) {
          console.error('Failed to load project draft:', error);
        }
      }
    }
  }, [isOpen, autoSaveKey, setValue]);

  // Save draft on form changes
  useEffect(() => {
    if (isOpen) {
      const timeoutId = setTimeout(() => {
        localStorage.setItem(autoSaveKey, JSON.stringify(watchedValues));
      }, 1000);
      return () => clearTimeout(timeoutId);
    }
  }, [watchedValues, isOpen, autoSaveKey]);

  const onSubmit = async (data: CreateProjectFormData) => {
    try {
      await createProject({
        workspaceId,
        name: data.name,
        slug: generateSlug(data.name),
        description: data.description,
      }).unwrap();

      toast.success('Project created successfully!');
      
      // Clear draft
      localStorage.removeItem(autoSaveKey);
      
      // Reset form and close modal
      reset();
      onProjectCreated();
    } catch (error: any) {
      const errorMessage = error?.data?.message ?? 'Failed to create project';
      toast.error(errorMessage);
    }
  };

  const handleClose = () => {
    // Save current state as draft before closing
    localStorage.setItem(autoSaveKey, JSON.stringify(getValues()));
    onClose();
  };

  const handleTemplateSelect = (templateId: string) => {
    setValue('template', templateId);
    
    // Apply template-specific defaults
    const template = PROJECT_TEMPLATES.find(t => t.id === templateId);
    if (template) {
      toast.success(`Applied ${template.name} template`);
    }
  };

  const generateSlug = (name: string): string => {
    return name
      .toLowerCase()
      .trim()
      .replace(/[^\w\s-]/g, '')
      .replace(/[\s_-]+/g, '-')
      .replace(/^-+|-+$/g, '');
  };

  if (!isOpen) return null;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Create New Project">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Project Name */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
            Project Name *
          </label>
          <Input
            id="name"
            {...register('name')}
            placeholder="Enter project name..."
            error={errors.name?.message}
            disabled={isLoading}
          />
          {errors.name && (
            <p className="text-red-600 text-sm mt-1">{errors.name.message}</p>
          )}
        </div>

        {/* Description */}
        <div>
          <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <TextArea
            id="description"
            {...register('description')}
            placeholder="Describe your project..."
            rows={3}
            error={errors.description?.message}
            disabled={isLoading}
          />
          {errors.description && (
            <p className="text-red-600 text-sm mt-1">{errors.description.message}</p>
          )}
        </div>

        {/* Template Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Project Template
          </label>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {PROJECT_TEMPLATES.map((template) => (
              <div
                key={template.id}
                className={`p-3 border rounded-lg cursor-pointer transition-all ${
                  watchedValues.template === template.id
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
                onClick={() => handleTemplateSelect(template.id)}
              >
                <h4 className="font-medium text-sm">{template.name}</h4>
                <p className="text-xs text-gray-600 mt-1">{template.description}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Color and Privacy Row */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Color Picker */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Project Color
            </label>
            <div className="flex items-center space-x-2">
              <div
                className="w-8 h-8 rounded-lg border-2 border-gray-200"
                style={{ backgroundColor: watchedValues.color }}
              />
              <div className="flex flex-wrap gap-1">
                {COLOR_OPTIONS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    className={`w-6 h-6 rounded-full border-2 ${
                      watchedValues.color === color 
                        ? 'border-gray-900' 
                        : 'border-gray-200'
                    }`}
                    style={{ backgroundColor: color }}
                    onClick={() => setValue('color', color)}
                    disabled={isLoading}
                  />
                ))}
              </div>
            </div>
          </div>

          {/* Privacy Level */}
          <div>
            <label htmlFor="privacy" className="block text-sm font-medium text-gray-700 mb-1">
              Privacy Level
            </label>
            <Select
              id="privacy"
              {...register('privacy')}
              disabled={isLoading}
            >
              <option value="PRIVATE">Private (Only me)</option>
              <option value="WORKSPACE">Workspace (All members)</option>
              <option value="PUBLIC">Public (Everyone)</option>
            </Select>
          </div>
        </div>

        {/* Date Range */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
              Start Date (Optional)
            </label>
            <Input
              id="startDate"
              type="date"
              {...register('startDate')}
              disabled={isLoading}
            />
          </div>
          <div>
            <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
              End Date (Optional)
            </label>
            <Input
              id="endDate"
              type="date"
              {...register('endDate')}
              disabled={isLoading}
            />
          </div>
        </div>

        {/* Form Actions */}
        <div className="flex items-center justify-between pt-4 border-t">
          <div className="text-sm text-gray-500">
            <Save className="inline h-4 w-4 mr-1" />
            Draft auto-saved
          </div>
          <div className="flex space-x-3">
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
                'Create Project'
              )}
            </Button>
          </div>
        </div>
      </form>
    </Modal>
  );
};