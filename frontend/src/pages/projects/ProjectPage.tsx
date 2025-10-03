import React from 'react'
import { useParams, Navigate } from 'react-router-dom'
import {
  Settings,
  Users,
  Calendar,
  MoreHorizontal,
  ArrowLeft,
  Star,
  Share2,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { format } from 'date-fns'

import {
  useGetProjectQuery,
  type Project,
} from '../../store/api/projectManagementApi'
import { KanbanBoard } from '../../components/task/KanbanBoard'
import LoadingSpinner from '../../components/ui/LoadingSpinner'
import { Button } from '../../components/ui/button'
import { Badge } from '../../components/ui/Badge'

/**
 * ProjectPage Component
 *
 * Main project view showing project details and task board.
 * Displays project metadata, team members, and integrated Kanban board.
 *
 * Features:
 * - Project header with metadata
 * - Member list and management
 * - Integrated Kanban board
 * - Project settings access
 * - Real-time updates
 * - Mobile responsive layout
 */
export const ProjectPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>()

  if (!projectId) {
    return <Navigate to="/projects" replace />
  }

  const { data: project, error, isLoading } = useGetProjectQuery(projectId)

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error || !project) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">
            Project Not Found
          </h2>
          <p className="text-gray-600 mb-6">
            The project you're looking for doesn't exist or you don't have
            access to it.
          </p>
          <Link to="/projects">
            <Button>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Projects
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  const statusColors: Record<Project['status'], string> = {
    PLANNING: 'bg-purple-100 text-purple-800',
    ACTIVE: 'bg-green-100 text-green-800',
    ON_HOLD: 'bg-yellow-100 text-yellow-800',
    COMPLETED: 'bg-blue-100 text-blue-800',
    ARCHIVED: 'bg-gray-100 text-gray-800',
  }

  const completionPercentage =
    project.taskCount > 0
      ? Math.round((project.completedTaskCount / project.taskCount) * 100)
      : 0

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Project Header */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between py-4">
            {/* Left side - Project info */}
            <div className="flex items-center min-w-0 flex-1">
              {/* Back button */}
              <Link
                to="/projects"
                className="mr-4 p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <ArrowLeft className="h-5 w-5 text-gray-600" />
              </Link>

              {/* Project color indicator */}
              <div
                className="w-4 h-4 rounded-full mr-3 flex-shrink-0"
                style={{ backgroundColor: '#6B7280' }}
              />

              {/* Project name and details */}
              <div className="min-w-0 flex-1">
                <h1 className="text-xl font-bold text-gray-900 truncate">
                  {project.name}
                </h1>
                <div className="flex items-center space-x-4 text-sm text-gray-600">
                  <div className="flex items-center">
                    <Users className="h-4 w-4 mr-1" />
                    <span>{project.memberCount} members</span>
                  </div>
                  <div className="flex items-center">
                    <Calendar className="h-4 w-4 mr-1" />
                    <span>
                      Created{' '}
                      {format(new Date(project.createdAt), 'MMM d, yyyy')}
                    </span>
                  </div>
                  <Badge className={statusColors[project.status]}>
                    {project.status.replace('_', ' ').toLowerCase()}
                  </Badge>
                </div>
              </div>
            </div>

            {/* Right side - Actions */}
            <div className="flex items-center space-x-2 ml-4">
              {/* Progress indicator */}
              <div className="hidden md:flex items-center text-sm text-gray-600 mr-4">
                <span className="mr-2">Progress:</span>
                <div className="w-20 bg-gray-200 rounded-full h-2 mr-2">
                  <div
                    className="bg-blue-600 h-2 rounded-full transition-all"
                    style={{ width: `${completionPercentage}%` }}
                  />
                </div>
                <span className="font-medium">{completionPercentage}%</span>
              </div>

              {/* Action buttons */}
              <Button variant="ghost" size="sm">
                <Star className="h-4 w-4" />
              </Button>

              <Button variant="ghost" size="sm">
                <Share2 className="h-4 w-4" />
              </Button>

              <Button variant="outline" size="sm">
                <Users className="h-4 w-4 mr-2" />
                <span className="hidden sm:inline">Members</span>
              </Button>

              <Button variant="outline" size="sm">
                <Settings className="h-4 w-4 mr-2" />
                <span className="hidden sm:inline">Settings</span>
              </Button>

              <Button variant="ghost" size="sm">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {/* Project description */}
          {project.description && (
            <div className="pb-4">
              <p className="text-gray-700 text-sm max-w-3xl">
                {project.description}
              </p>
            </div>
          )}

          {/* Mobile progress indicator */}
          <div className="md:hidden pb-4">
            <div className="flex items-center justify-between text-sm text-gray-600 mb-2">
              <span>Project Progress</span>
              <span className="font-medium">{completionPercentage}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all"
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
          </div>

          {/* Project dates */}
          {project.dueDate && (
            <div className="flex items-center space-x-6 text-sm text-gray-600 pb-4">
              <div>
                <span className="font-medium">Due:</span>{' '}
                {format(new Date(project.dueDate), 'MMM d, yyyy')}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Main Content - Kanban Board */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <KanbanBoard projectId={projectId} />
      </div>
    </div>
  )
}
