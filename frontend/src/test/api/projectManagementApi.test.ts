import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { projectManagementApi } from '../../store/api/projectManagementApi';
import { createApiTestStore } from '../utils/testStore';

const API_BASE_URL = 'http://localhost:3000/api/v1';

// Type definitions for request payloads
interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  timezone?: string;
  language?: string;
}

interface WorkspaceCreateRequest {
  name?: string;
  slug?: string;
  description?: string;
}

interface WorkspaceUpdateRequest {
  name?: string;
  description?: string;
}

interface ProjectCreateRequest {
  workspaceId?: string;
  name?: string;
  slug?: string;
  description?: string;
  priority?: string;
  status?: string;
}

interface ProjectUpdateRequest {
  name?: string;
  description?: string;
  status?: string;
  priority?: string;
}

interface TaskCreateRequest {
  projectId?: string;
  title?: string;
  description?: string;
  priority?: string;
  status?: string;
  assigneeId?: string;
}

interface TaskUpdateRequest {
  title?: string;
  description?: string;
  status?: string;
  priority?: string;
  assigneeId?: string;
}

interface CommentCreateRequest {
  content?: string;
}

// Mock data
const mockUser = {
  id: 'user-1',
  email: 'test@example.com',
  firstName: 'John',
  lastName: 'Doe',
  timezone: 'America/New_York',
  language: 'en',
  isActive: true,
  emailVerified: true,
  createdAt: '2025-01-01T00:00:00Z',
  lastLoginAt: '2025-01-15T00:00:00Z',
};

const mockWorkspace = {
  id: 'workspace-1',
  name: 'Test Workspace',
  slug: 'test-workspace',
  description: 'A test workspace',
  storageUsed: 1024,
  storageLimit: 10240,
  memberCount: 5,
  projectCount: 3,
  userRole: 'OWNER',
  createdAt: '2025-01-01T00:00:00Z',
};

const mockProject = {
  id: 'project-1',
  workspaceId: 'workspace-1',
  name: 'Test Project',
  slug: 'test-project',
  description: 'A test project',
  status: 'ACTIVE',
  priority: 'HIGH',
  dueDate: '2025-03-01T00:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-15T00:00:00Z',
  ownerName: 'John Doe',
  memberCount: 3,
  taskCount: 10,
  completedTaskCount: 5,
};

const mockTask = {
  id: 'task-1',
  projectId: 'project-1',
  title: 'Test Task',
  description: 'A test task',
  status: 'IN_PROGRESS',
  priority: 'MEDIUM',
  assigneeId: 'user-1',
  assigneeName: 'John Doe',
  dueDate: '2025-02-01T00:00:00Z',
  createdAt: '2025-01-10T00:00:00Z',
  updatedAt: '2025-01-15T00:00:00Z',
  commentCount: 2,
  estimatedHours: 8,
};

const mockComment = {
  id: 'comment-1',
  taskId: 'task-1',
  authorId: 'user-1',
  authorName: 'John Doe',
  content: 'This is a test comment',
  createdAt: '2025-01-15T00:00:00Z',
  updatedAt: '2025-01-15T00:00:00Z',
};

// MSW handlers
const handlers = [
  // User endpoints
  http.get(`${API_BASE_URL}/users/me`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json(mockUser);
  }),

  http.patch<never, UserUpdateRequest>(`${API_BASE_URL}/users/me`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    return HttpResponse.json({ ...mockUser, ...body });
  }),

  http.put<never, UserUpdateRequest>(`${API_BASE_URL}/users/me`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    return HttpResponse.json({ ...mockUser, ...body });
  }),

  // Workspace endpoints
  http.get(`${API_BASE_URL}/workspaces`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json([mockWorkspace]);
  }),

  http.get(`${API_BASE_URL}/workspaces/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id === 'workspace-1') {
      return HttpResponse.json(mockWorkspace);
    }
    return HttpResponse.json({ message: 'Workspace not found' }, { status: 404 });
  }),

  http.post<never, WorkspaceCreateRequest>(`${API_BASE_URL}/workspaces`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    if (!body?.name) {
      return HttpResponse.json({ message: 'Name is required' }, { status: 400 });
    }
    return HttpResponse.json({ ...mockWorkspace, ...body });
  }),

  http.put<{ id: string }, WorkspaceUpdateRequest>(`${API_BASE_URL}/workspaces/:id`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id !== 'workspace-1') {
      return HttpResponse.json({ message: 'Workspace not found' }, { status: 404 });
    }
    const body = await request.json();
    return HttpResponse.json({ ...mockWorkspace, ...body });
  }),

  // Project endpoints
  http.get(`${API_BASE_URL}/projects`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json({
      content: [mockProject],
      pageable: {
        pageNumber: 0,
        pageSize: 20,
        sort: []
      },
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
      empty: false
    });
  }),

  http.get(`${API_BASE_URL}/projects/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id === 'project-1') {
      return HttpResponse.json(mockProject);
    }
    return HttpResponse.json({ message: 'Project not found' }, { status: 404 });
  }),

  http.post<never, ProjectCreateRequest>(`${API_BASE_URL}/projects`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    if (!body?.name) {
      return HttpResponse.json({ message: 'Name is required' }, { status: 400 });
    }
    return HttpResponse.json({ ...mockProject, ...body });
  }),

  http.put<{ id: string }, ProjectUpdateRequest>(`${API_BASE_URL}/projects/:id`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id !== 'project-1') {
      return HttpResponse.json({ message: 'Project not found' }, { status: 404 });
    }
    const body = await request.json();
    return HttpResponse.json({ ...mockProject, ...body });
  }),

  http.delete(`${API_BASE_URL}/projects/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id !== 'project-1') {
      return HttpResponse.json({ message: 'Project not found' }, { status: 404 });
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // Task endpoints
  http.get(`${API_BASE_URL}/tasks`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json({
      content: [mockTask],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  }),

  http.get(`${API_BASE_URL}/tasks/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id === 'task-1') {
      return HttpResponse.json(mockTask);
    }
    return HttpResponse.json({ message: 'Task not found' }, { status: 404 });
  }),

  http.post<never, TaskCreateRequest>(`${API_BASE_URL}/tasks`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    if (!body?.title) {
      return HttpResponse.json({ message: 'Title is required' }, { status: 400 });
    }
    return HttpResponse.json({ ...mockTask, ...body });
  }),

  http.put<{ id: string }, TaskUpdateRequest>(`${API_BASE_URL}/tasks/:id`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id !== 'task-1') {
      return HttpResponse.json({ message: 'Task not found' }, { status: 404 });
    }
    const body = await request.json();
    return HttpResponse.json({ ...mockTask, ...body });
  }),

  http.delete(`${API_BASE_URL}/tasks/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const { id } = params;
    if (id !== 'task-1') {
      return HttpResponse.json({ message: 'Task not found' }, { status: 404 });
    }
    return new HttpResponse(null, { status: 204 });
  }),

  // Comment endpoints
  http.get(`${API_BASE_URL}/tasks/:taskId/comments`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json([mockComment]);
  }),

  http.post<{ taskId: string }, CommentCreateRequest>(`${API_BASE_URL}/tasks/:taskId/comments`, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    const body = await request.json();
    if (!body?.content) {
      return HttpResponse.json({ message: 'Content is required' }, { status: 400 });
    }
    return HttpResponse.json({ ...mockComment, ...body });
  }),

  // Search endpoints
  http.get(`${API_BASE_URL}/search`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json({
      content: [
        {
          id: 'result-1',
          type: 'PROJECT',
          title: 'Test Project',
          description: 'A test project',
          url: '/projects/project-1',
          lastModified: '2025-01-15T00:00:00Z',
          relevanceScore: 0.95,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  }),

  // Dashboard endpoints
  http.get(`${API_BASE_URL}/dashboard/overview`, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json({
      workspaceStats: {
        totalProjects: 10,
        activeProjects: 7,
        totalTasks: 50,
        completedTasks: 30,
        storageUsed: 1024,
        storageLimit: 10240,
      },
      recentProjects: [mockProject],
      upcomingDeadlines: [mockTask],
      teamActivity: 25,
    });
  }),
];

// Setup MSW server
const server = setupServer(...handlers);

// Helper to create authenticated test store
const createTestStore = () => {
  return createApiTestStore(projectManagementApi, {
    auth: {
      token: 'test-token',
      isAuthenticated: true,
      user: {
        id: 'user-1',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'USER' as const,
        emailVerified: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      isLoading: false,
      error: null,
    },
  });
};

// Test suite
describe('Project Management API', () => {
  beforeAll(() => {
    server.listen({ onUnhandledRequest: 'error' });
  });

  afterEach(() => {
    server.resetHandlers();
  });

  afterAll(() => {
    server.close();
  });

  describe('User Endpoints', () => {
    it('should get current user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.email).toBe('test@example.com');
      expect(result.data?.firstName).toBe('John');
    });

    it('should update current user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.updateCurrentUser.initiate({
          firstName: 'Jane',
        })
      );

      expect(result.data?.firstName).toBe('Jane');
    });

    it('should return 401 for unauthenticated request', async () => {
      const store = createApiTestStore(projectManagementApi, {
        auth: { token: null, isAuthenticated: false, user: null, isLoading: false, error: null },
      });

      const result = await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(401);
      } else {
        throw new Error('Expected error response');
      }
    });
  });

  describe('Workspace Endpoints', () => {
    it('should get user workspaces', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getUserWorkspaces.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.[0].name).toBe('Test Workspace');
    });

    it('should get workspace by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getWorkspace.initiate('workspace-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('workspace-1');
      expect(result.data?.userRole).toBe('OWNER');
    });

    it('should return 404 for non-existent workspace', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getWorkspace.initiate('workspace-999')
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(404);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should create workspace', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createWorkspace.initiate({
          name: 'New Workspace',
          slug: 'new-workspace',
        })
      );

      expect(result.data?.name).toBe('New Workspace');
    });

    it('should return 400 for missing name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createWorkspace.initiate({
          name: '',
          slug: 'test',
        })
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(400);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should update workspace', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.updateWorkspace.initiate({
          workspaceId: 'workspace-1',
          workspace: { name: 'Updated Workspace' },
        })
      );

      expect(result.data?.name).toBe('Updated Workspace');
    });
  });

  describe('Project Endpoints', () => {
    it('should get workspace projects', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getWorkspaceProjects.initiate({
          workspaceId: 'workspace-1',
          page: 0,
          size: 20,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('content');
      expect(Array.isArray(result.data?.content)).toBe(true);
      expect(result.data?.content[0].name).toBe('Test Project');
    });

    it('should get project by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getProject.initiate('project-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('project-1');
      expect(result.data?.status).toBe('ACTIVE');
    });

    it('should return 404 for non-existent project', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getProject.initiate('project-999')
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(404);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should create project', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createProject.initiate({
          workspaceId: 'workspace-1',
          name: 'New Project',
          slug: 'new-project',
          priority: 'MEDIUM',
        })
      );

      expect(result.data?.name).toBe('New Project');
    });

    it('should return 400 for missing name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createProject.initiate({
          workspaceId: 'workspace-1',
          name: '',
          slug: 'test',
          priority: 'MEDIUM',
        })
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(400);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should update project', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.updateProject.initiate({
          projectId: 'project-1',
          project: { name: 'Updated Project', status: 'COMPLETED' },
        })
      );

      expect(result.data?.name).toBe('Updated Project');
      expect(result.data?.status).toBe('COMPLETED');
    });

    it('should delete project', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.deleteProject.initiate('project-1')
      );

      expect(result.error).toBeUndefined();
    });
  });

  describe('Task Endpoints', () => {
    it('should get project tasks', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getProjectTasks.initiate({
          projectId: 'project-1',
          page: 0,
          size: 20,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('content');
      expect(Array.isArray(result.data?.content)).toBe(true);
      expect(result.data?.content[0].title).toBe('Test Task');
    });

    it('should get task by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getTask.initiate('task-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('task-1');
      expect(result.data?.status).toBe('IN_PROGRESS');
    });

    it('should return 404 for non-existent task', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getTask.initiate('task-999')
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(404);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should create task', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createTask.initiate({
          projectId: 'project-1',
          title: 'New Task',
          priority: 'MEDIUM',
        })
      );

      expect(result.data?.title).toBe('New Task');
    });

    it('should return 400 for missing title', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.createTask.initiate({
          projectId: 'project-1',
          title: '',
          priority: 'MEDIUM',
        })
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(400);
      } else {
        throw new Error('Expected error response');
      }
    });

    it('should update task', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.updateTask.initiate({
          taskId: 'task-1',
          task: { title: 'Updated Task', status: 'COMPLETED' },
        })
      );

      expect(result.data?.title).toBe('Updated Task');
      expect(result.data?.status).toBe('COMPLETED');
    });

    it('should delete task', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.deleteTask.initiate('task-1')
      );

      expect(result.error).toBeUndefined();
    });
  });

  describe('Comment Endpoints', () => {
    it('should get task comments', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getTaskComments.initiate('task-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.[0].content).toBe('This is a test comment');
    });

    it('should add task comment', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.addTaskComment.initiate({
          taskId: 'task-1',
          content: 'New comment',
        })
      );

      expect(result.data?.content).toBe('New comment');
    });

    it('should return 400 for missing content', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.addTaskComment.initiate({
          taskId: 'task-1',
          content: '',
        })
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(400);
      } else {
        throw new Error('Expected error response');
      }
    });
  });

  describe('Search Endpoints', () => {
    it('should search all content', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.searchAll.initiate({
          query: 'test',
          page: 0,
          size: 20,
        })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('content');
      expect(Array.isArray(result.data?.content)).toBe(true);
    });
  });

  describe('Dashboard Endpoints', () => {
    it('should get dashboard overview', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        projectManagementApi.endpoints.getDashboardOverview.initiate({})
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('workspaceStats');
      expect(result.data?.workspaceStats.totalProjects).toBe(10);
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      server.use(
        http.get(`${API_BASE_URL}/users/me`, () => {
          return HttpResponse.error();
        })
      );

      const store = createTestStore();
      const result = await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.error).toBeDefined();
    });

    it('should handle server errors', async () => {
      server.use(
        http.get(`${API_BASE_URL}/users/me`, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const store = createTestStore();
      const result = await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      if ('error' in result && result.error && typeof result.error === 'object' && 'status' in result.error) {
        expect(result.error.status).toBe(500);
      } else {
        throw new Error('Expected error response');
      }
    });
  });

  describe('Cache Management', () => {
    it('should cache user queries', async () => {
      const store = createTestStore();

      await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      const result = await store.dispatch(
        projectManagementApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.data?.email).toBe('test@example.com');
    });

    it('should cache workspace queries', async () => {
      const store = createTestStore();

      await store.dispatch(
        projectManagementApi.endpoints.getUserWorkspaces.initiate()
      );

      const result = await store.dispatch(
        projectManagementApi.endpoints.getUserWorkspaces.initiate()
      );

      expect(Array.isArray(result.data)).toBe(true);
    });
  });
});
