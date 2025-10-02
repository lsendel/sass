import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Calendar, Users, ArrowRight } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

import { useGetProjectsQuery } from '../../store/api/projectManagementApi';
import { Button } from '../ui/button';
import { Card } from '../ui/card';
import LoadingSpinner from '../ui/LoadingSpinner';
import { EmptyState } from '../ui/EmptyState';

import { CreateProjectModal } from './CreateProjectModal';

interface ProjectListProps {
  workspaceId: string;
}

/**
 * ProjectList Component
 * 
 * Displays a list of projects within a workspace with create functionality.
 * Supports empty states, loading states, and quick project creation.
 * 
 * Features:
 * - Grid layout with project cards
 * - Create project modal integration
 * - Loading and error states
 * - Responsive design
 * - Project metadata display (members, created date, status)
 */
export const ProjectList: React.FC<ProjectListProps> = ({ workspaceId }) => {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const {
    data: projectsResponse,
    error,
    isLoading,
    refetch
  } = useGetProjectsQuery({ workspaceId });

  const handleProjectCreated = () => {
    setIsCreateModalOpen(false);
    refetch();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (error) {
    return (
      <Card className="p-8 text-center">
        <div className="text-red-600 mb-4">
          <h3 className="text-lg font-semibold">Unable to load projects</h3>
          <p className="text-sm text-gray-600 mt-2">
            There was an error loading your projects. Please try again.
          </p>
        </div>
        <Button 
          onClick={() => refetch()}
          variant="outline"
        >
          Try Again
        </Button>
      </Card>
    );
  }

  const projects = projectsResponse?.content || [];

  if (!projects || projects.length === 0) {
    return (
      <>
        <EmptyState
          title="No projects yet"
          description="Create your first project to start organizing tasks and collaborating with your team."
          icon={<Plus className="h-12 w-12 text-gray-400" />}
          action={
            <Button
              onClick={() => setIsCreateModalOpen(true)}
              className="mt-4"
            >
              <Plus className="h-4 w-4 mr-2" />
              Create First Project
            </Button>
          }
        />
        <CreateProjectModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
          onProjectCreated={handleProjectCreated}
          workspaceId={workspaceId}
        />
      </>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Projects</h1>
          <p className="text-gray-600 mt-1">
            {projects.length} project{projects.length !== 1 ? 's' : ''} in your workspace
          </p>
        </div>
        <Button
          onClick={() => setIsCreateModalOpen(true)}
          className="bg-blue-600 hover:bg-blue-700"
        >
          <Plus className="h-4 w-4 mr-2" />
          New Project
        </Button>
      </div>

      {/* Project Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {projects.map((project) => (
          <Card key={project.id} className="hover:shadow-lg transition-shadow duration-200">
            <div className="p-6">
              {/* Project Header */}
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h3 className="text-lg font-semibold text-gray-900 mb-1">
                    {project.name}
                  </h3>
                  {project.description && (
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {project.description}
                    </p>
                  )}
                </div>
                <div
                  className="w-4 h-4 rounded-full ml-3 flex-shrink-0"
                  style={{ backgroundColor: '#6B7280' }}
                  title="Project color"
                />
              </div>

              {/* Project Stats */}
              <div className="space-y-2 mb-4">
                <div className="flex items-center text-sm text-gray-500">
                  <Users className="h-4 w-4 mr-2" />
                  <span>{project.memberCount || 0} member{(project.memberCount || 0) !== 1 ? 's' : ''}</span>
                </div>
                <div className="flex items-center text-sm text-gray-500">
                  <Calendar className="h-4 w-4 mr-2" />
                  <span>
                    Created {formatDistanceToNow(new Date(project.createdAt), { addSuffix: true })}
                  </span>
                </div>
              </div>

              {/* Status Badge */}
              <div className="flex items-center justify-between">
                <span
                  className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    project.status === 'ACTIVE'
                      ? 'bg-green-100 text-green-800'
                      : project.status === 'COMPLETED'
                      ? 'bg-blue-100 text-blue-800'
                      : project.status === 'ON_HOLD'
                      ? 'bg-yellow-100 text-yellow-800'
                      : 'bg-gray-100 text-gray-800'
                  }`}
                >
                  {project.status.replace('_', ' ').toLowerCase()}
                </span>

                {/* Action Button */}
                <Link
                  to={`/projects/${project.id}`}
                  className="inline-flex items-center text-sm text-blue-600 hover:text-blue-800 font-medium"
                >
                  Open
                  <ArrowRight className="h-4 w-4 ml-1" />
                </Link>
              </div>
            </div>
          </Card>
        ))}
      </div>

      {/* Create Project Modal */}
      <CreateProjectModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onProjectCreated={handleProjectCreated}
        workspaceId={workspaceId}
      />
    </div>
  );
};