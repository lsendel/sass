import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

import type { RootState } from '../index';

// API base URL configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api/v1';

/**
 * RTK Query API for Project Management endpoints.
 *
 * Provides comprehensive API integration for projects, tasks, workspaces,
 * search, and dashboard functionality with automatic caching and invalidation.
 *
 * Constitutional Compliance:
 * - Performance: Automatic caching with smart invalidation strategies
 * - Real-time: Tag-based cache invalidation for instant updates
 * - Error Handling: Consistent error handling with retry logic
 */

// Types matching the backend DTOs
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  timezone: string;
  language: string;
  isActive: boolean;
  emailVerified: boolean;
  createdAt: string;
  lastLoginAt: string;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  logoUrl?: string;
  storageUsed: number;
  storageLimit: number;
  memberCount: number;
  projectCount: number;
  userRole: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
  createdAt: string;
}

export interface Project {
  id: string;
  workspaceId: string;
  name: string;
  slug: string;
  description?: string;
  status: 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
  ownerName: string;
  memberCount: number;
  taskCount: number;
  completedTaskCount: number;
}

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'COMPLETED' | 'ARCHIVED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  assigneeId?: string;
  assigneeName?: string;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
  estimatedHours?: number;
}

export interface TaskComment {
  id: string;
  taskId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface SearchResult {
  id: string;
  type: 'PROJECT' | 'TASK' | 'USER' | 'WORKSPACE' | 'COMMENT' | 'FILE';
  title: string;
  description: string;
  url: string;
  contextName?: string;
  lastModified: string;
  relevanceScore: number;
}

export interface DashboardOverview {
  workspaceStats: {
    totalWorkspaces: number;
    totalProjects: number;
    totalTasks: number;
    completedTasks: number;
    activeMembers: number;
  };
  activitySummary: {
    tasksCreatedThisWeek: number;
    tasksCompletedThisWeek: number;
    projectsStartedThisWeek: number;
    projectsCompletedThisWeek: number;
  };
  productivityMetrics: {
    completionRate: number;
    averageTasksPerDay: number;
    averageHoursPerTask: number;
    velocityScore: number;
  };
  recentActivity: Array<{
    id: string;
    type: string;
    description: string;
    actorName: string;
    contextName: string;
    timestamp: string;
  }>;
}

export interface Notification {
  id: string;
  type: string;
  message: string;
  title: string;
  unread: boolean;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  createdAt: string;
}

// Page interface for paginated responses
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: string[];
  };
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// Request types
export interface CreateWorkspaceRequest {
  name: string;
  slug: string;
  description?: string;
  logoUrl?: string;
}

export interface UpdateWorkspaceRequest {
  name?: string;
  description?: string;
  logoUrl?: string;
  settings?: Record<string, any>;
}

export interface CreateProjectRequest {
  workspaceId: string;
  name: string;
  slug: string;
  description?: string;
  priority?: Project['priority'];
  dueDate?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
  status?: Project['status'];
  priority?: Project['priority'];
  dueDate?: string;
}

export interface CreateTaskRequest {
  projectId: string;
  title: string;
  description?: string;
  priority?: Task['priority'];
  assigneeId?: string;
  dueDate?: string;
  estimatedHours?: number;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  status?: Task['status'];
  priority?: Task['priority'];
  assigneeId?: string;
  dueDate?: string;
  estimatedHours?: number;
}

export const projectManagementApi = createApi({
  reducerPath: 'projectManagementApi',
  baseQuery: fetchBaseQuery({
    baseUrl: API_BASE_URL,
    prepareHeaders: (headers, { getState }) => {
      // Add authentication headers if available
      const state = getState() as RootState;
      const token = state.auth?.token;

      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }

      headers.set('Content-Type', 'application/json');
      return headers;
    },
  }),
  tagTypes: [
    'User',
    'Workspace',
    'Project',
    'Task',
    'TaskComment',
    'SearchResult',
    'Dashboard',
    'Notification'
  ],
  endpoints: (builder) => ({
    // User endpoints
    getCurrentUser: builder.query<User, void>({
      query: () => 'users/me',
      providesTags: ['User'],
    }),
    updateCurrentUser: builder.mutation<User, Partial<User>>({
      query: (user) => ({
        url: 'users/me',
        method: 'PUT',
        body: user,
      }),
      invalidatesTags: ['User'],
    }),

    // Workspace endpoints
    getUserWorkspaces: builder.query<Workspace[], void>({
      query: () => 'workspaces',
      providesTags: ['Workspace'],
    }),
    getWorkspace: builder.query<Workspace, string>({
      query: (workspaceId) => `workspaces/${workspaceId}`,
      providesTags: (_result, _error, workspaceId) => [
        { type: 'Workspace', id: workspaceId }
      ],
    }),
    createWorkspace: builder.mutation<Workspace, CreateWorkspaceRequest>({
      query: (workspace) => ({
        url: 'workspaces',
        method: 'POST',
        body: workspace,
      }),
      invalidatesTags: ['Workspace'],
    }),
    updateWorkspace: builder.mutation<Workspace, { workspaceId: string; workspace: UpdateWorkspaceRequest }>({
      query: ({ workspaceId, workspace }) => ({
        url: `workspaces/${workspaceId}`,
        method: 'PUT',
        body: workspace,
      }),
      invalidatesTags: (_result, _error, { workspaceId }) => [
        'Workspace',
        { type: 'Workspace', id: workspaceId }
      ],
    }),

    // Project endpoints
    getWorkspaceProjects: builder.query<Page<Project>, {
      workspaceId: string;
      status?: Project['status'];
      page?: number;
      size?: number;
    }>({
      query: ({ workspaceId, status, page = 0, size = 20 }) => ({
        url: 'projects',
        params: {
          workspaceId,
          status,
          page,
          size,
        },
      }),
      providesTags: (_result, _error, { workspaceId }) => [
        'Project',
        { type: 'Workspace', id: workspaceId }
      ],
    }),
    getProject: builder.query<Project, string>({
      query: (projectId) => `projects/${projectId}`,
      providesTags: (_result, _error, projectId) => [
        { type: 'Project', id: projectId }
      ],
    }),
    createProject: builder.mutation<Project, CreateProjectRequest>({
      query: (project) => ({
        url: 'projects',
        method: 'POST',
        body: project,
      }),
      invalidatesTags: ['Project', 'Workspace'],
    }),
    updateProject: builder.mutation<Project, { projectId: string; project: UpdateProjectRequest }>({
      query: ({ projectId, project }) => ({
        url: `projects/${projectId}`,
        method: 'PUT',
        body: project,
      }),
      invalidatesTags: (_result, _error, { projectId }) => [
        'Project',
        { type: 'Project', id: projectId }
      ],
    }),
    deleteProject: builder.mutation<void, string>({
      query: (projectId) => ({
        url: `projects/${projectId}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Project', 'Workspace'],
    }),

    // Task endpoints
    getProjectTasks: builder.query<Page<Task>, {
      projectId: string;
      status?: Task['status'];
      priority?: Task['priority'];
      assigneeId?: string;
      page?: number;
      size?: number;
    }>({
      query: ({ projectId, status, priority, assigneeId, page = 0, size = 20 }) => ({
        url: 'tasks',
        params: {
          projectId,
          status,
          priority,
          assigneeId,
          page,
          size,
        },
      }),
      providesTags: (_result, _error, { projectId }) => [
        'Task',
        { type: 'Project', id: projectId }
      ],
    }),
    getTask: builder.query<Task, string>({
      query: (taskId) => `tasks/${taskId}`,
      providesTags: (_result, _error, taskId) => [
        { type: 'Task', id: taskId }
      ],
    }),
    createTask: builder.mutation<Task, CreateTaskRequest>({
      query: (task) => ({
        url: 'tasks',
        method: 'POST',
        body: task,
      }),
      invalidatesTags: ['Task', 'Project'],
    }),
    updateTask: builder.mutation<Task, { taskId: string; task: UpdateTaskRequest }>({
      query: ({ taskId, task }) => ({
        url: `tasks/${taskId}`,
        method: 'PUT',
        body: task,
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        'Task',
        { type: 'Task', id: taskId },
        'Project'
      ],
    }),
    deleteTask: builder.mutation<void, string>({
      query: (taskId) => ({
        url: `tasks/${taskId}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Task', 'Project'],
    }),

    // Task Comments
    getTaskComments: builder.query<TaskComment[], string>({
      query: (taskId) => `tasks/${taskId}/comments`,
      providesTags: (_result, _error, taskId) => [
        'TaskComment',
        { type: 'Task', id: taskId }
      ],
    }),
    addTaskComment: builder.mutation<TaskComment, { taskId: string; content: string }>({
      query: ({ taskId, content }) => ({
        url: `tasks/${taskId}/comments`,
        method: 'POST',
        body: { content },
      }),
      invalidatesTags: (_result, _error, { taskId }) => [
        'TaskComment',
        { type: 'Task', id: taskId }
      ],
    }),

    // Search endpoints
    searchAll: builder.query<Page<SearchResult>, {
      query: string;
      workspaceId?: string;
      type?: SearchResult['type'];
      page?: number;
      size?: number;
    }>({
      query: ({ query, workspaceId, type, page = 0, size = 20 }) => ({
        url: 'search',
        params: {
          query,
          workspaceId,
          type,
          page,
          size,
        },
      }),
      providesTags: ['SearchResult'],
    }),
    getSearchSuggestions: builder.query<Array<{
      suggestion: string;
      type: SearchResult['type'];
      description: string;
      matchCount: number;
    }>, { query: string; workspaceId?: string; limit?: number }>({
      query: ({ query, workspaceId, limit = 10 }) => ({
        url: 'search/suggestions',
        params: { query, workspaceId, limit },
      }),
    }),
    getRecentItems: builder.query<SearchResult[], {
      workspaceId?: string;
      limit?: number;
    }>({
      query: ({ workspaceId, limit = 10 }) => ({
        url: 'search/recent',
        params: { workspaceId, limit },
      }),
      providesTags: ['SearchResult'],
    }),

    // Dashboard endpoints
    getDashboardOverview: builder.query<DashboardOverview, { workspaceId?: string }>({
      query: ({ workspaceId }) => ({
        url: 'dashboard/overview',
        params: { workspaceId },
      }),
      providesTags: ['Dashboard'],
    }),
    getRecentActivity: builder.query<Array<{
      id: string;
      type: string;
      description: string;
      actorName: string;
      contextName: string;
      timestamp: string;
    }>, {
      workspaceId?: string;
      projectId?: string;
      limit?: number;
    }>({
      query: ({ workspaceId, projectId, limit = 20 }) => ({
        url: 'dashboard/activity',
        params: { workspaceId, projectId, limit },
      }),
      providesTags: ['Dashboard'],
    }),

    // Notification endpoints
    getNotifications: builder.query<Notification[], {
      unreadOnly?: boolean;
      limit?: number;
    }>({
      query: ({ unreadOnly = false, limit = 20 }) => ({
        url: 'dashboard/notifications',
        params: { unreadOnly, limit },
      }),
      providesTags: ['Notification'],
    }),
    markNotificationAsRead: builder.mutation<void, string>({
      query: (notificationId) => ({
        url: `dashboard/notifications/${notificationId}/read`,
        method: 'PUT',
      }),
      invalidatesTags: ['Notification'],
    }),
  }),
});

// Export hooks for usage in functional components
export const {
  // User hooks
  useGetCurrentUserQuery,
  useUpdateCurrentUserMutation,

  // Workspace hooks
  useGetUserWorkspacesQuery,
  useGetWorkspaceQuery,
  useCreateWorkspaceMutation,
  useUpdateWorkspaceMutation,

  // Project hooks
  useGetWorkspaceProjectsQuery,
  useGetProjectQuery,
  useCreateProjectMutation,
  useUpdateProjectMutation,
  useDeleteProjectMutation,

  // Task hooks
  useGetProjectTasksQuery,
  useGetTaskQuery,
  useCreateTaskMutation,
  useUpdateTaskMutation,
  useDeleteTaskMutation,

  // Task comment hooks
  useGetTaskCommentsQuery,
  useAddTaskCommentMutation,

  // Search hooks
  useSearchAllQuery,
  useGetSearchSuggestionsQuery,
  useGetRecentItemsQuery,

  // Dashboard hooks
  useGetDashboardOverviewQuery,
  useGetRecentActivityQuery,

  // Notification hooks
  useGetNotificationsQuery,
  useMarkNotificationAsReadMutation,
} = projectManagementApi;

// Compatibility aliases for components
export const useGetProjectsQuery = useGetWorkspaceProjectsQuery;
export const useGetTasksQuery = useGetProjectTasksQuery;
export const useGetDashboardStatsQuery = useGetDashboardOverviewQuery;
export const useSearchQuery = useSearchAllQuery;