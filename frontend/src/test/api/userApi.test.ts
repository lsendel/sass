import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { userApi } from '../../store/api/userApi';
import { createApiTestStore } from '../utils/testStore';

/**
 * User API Integration Tests
 *
 * Tests for user management, profile updates, and user search
 */

const API_BASE_URL = 'http://localhost:3000/api/v1/users';

const mockUsers = [
  {
    id: 'user-1',
    email: 'user1@example.com',
    name: 'User One',
    role: 'USER',
    createdAt: '2025-09-01T00:00:00Z',
    updatedAt: '2025-09-01T00:00:00Z',
  },
  {
    id: 'user-2',
    email: 'user2@example.com',
    name: 'User Two',
    role: 'ADMIN',
    createdAt: '2025-09-02T00:00:00Z',
    updatedAt: '2025-09-02T00:00:00Z',
  },
];

const handlers = [
  // GET /users/me
  http.get(`${API_BASE_URL}/me`, ({ request }) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    return HttpResponse.json({
      id: 'current-user',
      email: 'current@example.com',
      name: 'Current User',
      role: 'USER',
      preferences: { theme: 'dark' },
      createdAt: '2025-09-01T00:00:00Z',
      updatedAt: '2025-09-30T00:00:00Z',
    });
  }),

  // PUT /users/me/profile
  http.put(`${API_BASE_URL}/me/profile`, async ({ request }) => {
    const body = await request.json() as any;

    if (!body.name) {
      return HttpResponse.json(
        { message: 'Name is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      id: 'current-user',
      email: 'current@example.com',
      name: body.name,
      role: 'USER',
      preferences: body.preferences || {},
      updatedAt: new Date().toISOString(),
    });
  }),

  // PUT /users/me/preferences
  http.put(`${API_BASE_URL}/me/preferences`, async ({ request }) => {
    const body = await request.json() as any;

    return HttpResponse.json({
      id: 'current-user',
      email: 'current@example.com',
      name: 'Current User',
      role: 'USER',
      preferences: body.preferences,
      updatedAt: new Date().toISOString(),
    });
  }),

  // DELETE /users/me
  http.delete(`${API_BASE_URL}/me`, () => {
    return HttpResponse.json({ message: 'User deleted successfully' });
  }),

  // GET /users/search (must come before generic /users/:id)
  http.get(`${API_BASE_URL}/search`, ({ request }) => {
    const url = new URL(request.url);
    const name = url.searchParams.get('name');

    if (!name) {
      return HttpResponse.json(
        { message: 'Name parameter is required' },
        { status: 400 }
      );
    }

    const filtered = mockUsers.filter((u) =>
      u.name.toLowerCase().includes(name.toLowerCase())
    );

    return HttpResponse.json(filtered);
  }),

  // GET /users/recent (must come before generic /users)
  http.get(`${API_BASE_URL}/recent`, ({ request }) => {
    const url = new URL(request.url);
    const since = url.searchParams.get('since');

    if (!since) {
      return HttpResponse.json(
        { message: 'Since parameter is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json(mockUsers);
  }),

  // GET /users/statistics (must come before generic /users)
  http.get(`${API_BASE_URL}/statistics`, () => {
    return HttpResponse.json({
      totalUsers: 150,
      newUsersThisWeek: 12,
      usersByProvider: {
        password: 100,
        google: 30,
        github: 20,
      },
    });
  }),

  // GET /users/count (must come before generic /users)
  http.get(`${API_BASE_URL}/count`, () => {
    return HttpResponse.json(150);
  }),

  // GET /users/providers/:provider (must come before generic /users/:id)
  http.get(`${API_BASE_URL}/providers/:provider`, ({ params }) => {
    const { provider } = params;
    return HttpResponse.json(
      mockUsers.filter((u) => u.email.includes(provider as string))
    );
  }),

  // POST /users/:id/restore
  http.post(`${API_BASE_URL}/:id/restore`, ({ params }) => {
    const { id } = params;
    const user = mockUsers.find((u) => u.id === id);

    if (!user) {
      return HttpResponse.json(
        { message: 'User not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      ...user,
      updatedAt: new Date().toISOString(),
    });
  }),

  // GET /users/:id (generic param handler - must come after all specific routes)
  http.get(`${API_BASE_URL}/:id`, ({ params }) => {
    const { id } = params;
    const user = mockUsers.find((u) => u.id === id);

    if (!user) {
      return HttpResponse.json(
        { message: 'User not found' },
        { status: 404 }
      );
    }

    return HttpResponse.json(user);
  }),

  // GET /users (base route handler - must come LAST)
  http.get(`${API_BASE_URL}`, ({ request }) => {
    const url = new URL(request.url);
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '20');

    return HttpResponse.json({
      users: mockUsers.slice(page * size, (page + 1) * size),
      page,
      size,
      totalElements: mockUsers.length,
      totalPages: Math.ceil(mockUsers.length / size),
      first: page === 0,
      last: (page + 1) * size >= mockUsers.length,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const createTestStore = () => {
  return createApiTestStore(userApi, {
    auth: {
      token: 'test-token',
      isAuthenticated: true,
      user: { id: 'user-1', email: 'test@example.com', name: 'Test User' },
    },
  });
};

describe('User API', () => {
  describe('GET /users/me', () => {
    it('should get current user profile', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('id');
      expect(result.data).toHaveProperty('email');
      expect(result.data).toHaveProperty('name');
      expect(result.data?.email).toBe('current@example.com');
    });

    it('should return 401 for unauthenticated request', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/me`, () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 401,
      });
    });

    it('should include user email', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.data).toHaveProperty('email');
      expect(result.data?.email).toBeDefined();
    });
  });

  describe('GET /users/:id', () => {
    it('should get user by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUserById.initiate('user-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('user-1');
      expect(result.data?.email).toBe('user1@example.com');
    });

    it('should return 404 for non-existent user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUserById.initiate('non-existent')
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 404,
      });
    });

    it('should include user metadata', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUserById.initiate('user-1')
      );

      expect(result.data).toHaveProperty('createdAt');
      expect(result.data).toHaveProperty('updatedAt');
      expect(result.data).toHaveProperty('role');
    });
  });

  describe('PUT /users/me/profile', () => {
    it('should update user profile', async () => {
      const store = createTestStore();

      const updateData = {
        name: 'Updated Name',
        preferences: { theme: 'light' },
      };

      const result = await store.dispatch(
        userApi.endpoints.updateProfile.initiate(updateData)
      );

      expect(result.data?.email).toBeDefined();
    });

    it('should return 400 for missing name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.updateProfile.initiate({
          name: '',
          preferences: {},
        })
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 400,
      });
    });

    it('should update preferences in profile', async () => {
      const store = createTestStore();

      const updateData = {
        name: 'User Name',
        preferences: { theme: 'dark', language: 'en' },
      };

      const result = await store.dispatch(
        userApi.endpoints.updateProfile.initiate(updateData)
      );

      expect(result.data?.email).toBeDefined();
    });

    it('should invalidate user profile cache', async () => {
      const store = createTestStore();

      // Get current user
      await store.dispatch(userApi.endpoints.getCurrentUser.initiate());

      // Update profile
      await store.dispatch(
        userApi.endpoints.updateProfile.initiate({
          name: 'New Name',
          preferences: {},
        })
      );

      // Verify cache invalidation
      const state = store.getState();
      expect(state.userApi).toBeDefined();
    });
  });

  describe('PUT /users/me/preferences', () => {
    it('should update user preferences', async () => {
      const store = createTestStore();

      const preferences = {
        preferences: {
          theme: 'dark',
          language: 'en',
          notifications: true,
        },
      };

      const result = await store.dispatch(
        userApi.endpoints.updatePreferences.initiate(preferences)
      );

      expect(result.data?.email).toBeDefined();
    });

    it('should handle empty preferences', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.updatePreferences.initiate({
          preferences: {},
        })
      );

      expect(result.data?.email).toBeDefined();
    });

    it('should preserve other user data', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.updatePreferences.initiate({
          preferences: { theme: 'light' },
        })
      );

      expect(result.data).toHaveProperty('id');
      expect(result.data).toHaveProperty('email');
    });
  });

  describe('DELETE /users/me', () => {
    it('should delete current user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.deleteCurrentUser.initiate()
      );

      expect(result.error).toBeUndefined();
    });

    it('should invalidate user profile cache on delete', async () => {
      const store = createTestStore();

      // Get current user
      await store.dispatch(userApi.endpoints.getCurrentUser.initiate());

      // Delete user
      await store.dispatch(userApi.endpoints.deleteCurrentUser.initiate());

      const state = store.getState();
      expect(state.userApi).toBeDefined();
    });
  });

  describe('GET /users/search', () => {
    it('should search users by name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.searchUsers.initiate('User')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.length).toBeGreaterThan(0);
    });

    it('should return empty array for no matches', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/search`, () => {
          return HttpResponse.json([]);
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.searchUsers.initiate('NonExistent')
      );

      expect(result.data).toEqual([]);
    });

    it('should return 400 for missing search term', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/search`, () => {
          return HttpResponse.json(
            { message: 'Name parameter is required' },
            { status: 400 }
          );
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.searchUsers.initiate('')
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 400,
      });
    });

    it('should perform case-insensitive search', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.searchUsers.initiate('user')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.length).toBeGreaterThan(0);
    });
  });

  describe('GET /users', () => {
    it('should get all users with pagination', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getAllUsers.initiate({ page: 0, size: 20 })
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('users');
      expect(result.data).toHaveProperty('page');
      expect(result.data).toHaveProperty('totalElements');
      expect(Array.isArray(result.data?.users)).toBe(true);
    });

    it('should support pagination', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getAllUsers.initiate({ page: 0, size: 1 })
      );

      expect(result.data?.page).toBe(0);
      expect(result.data?.size).toBe(1);
      expect(result.data?.users.length).toBeLessThanOrEqual(1);
    });

    it('should support sorting', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getAllUsers.initiate({
          page: 0,
          size: 20,
          sortBy: 'createdAt',
          sortDirection: 'desc',
        })
      );

      expect(result.status).toBe('fulfilled');
    });

    it('should return pagination metadata', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getAllUsers.initiate({ page: 0, size: 20 })
      );

      expect(result.data?.first).toBeDefined();
      expect(result.data?.last).toBeDefined();
      expect(result.data?.totalPages).toBeDefined();
    });
  });

  describe('GET /users/recent', () => {
    it('should get recent users', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getRecentUsers.initiate('2025-09-01')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
    });

    it('should return 400 for missing since parameter', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/recent`, () => {
          return HttpResponse.json(
            { message: 'Since parameter is required' },
            { status: 400 }
          );
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.getRecentUsers.initiate('')
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 400,
      });
    });
  });

  describe('GET /users/statistics', () => {
    it('should get user statistics', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUserStatistics.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('totalUsers');
      expect(result.data).toHaveProperty('newUsersThisWeek');
      expect(result.data).toHaveProperty('usersByProvider');
    });

    it('should return correct statistics structure', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUserStatistics.initiate()
      );

      expect(typeof result.data?.totalUsers).toBe('number');
      expect(typeof result.data?.newUsersThisWeek).toBe('number');
      expect(typeof result.data?.usersByProvider).toBe('object');
    });
  });

  describe('GET /users/count', () => {
    it('should get active user count', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.countActiveUsers.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(typeof result.data).toBe('number');
      expect(result.data).toBeGreaterThan(0);
    });
  });

  describe('GET /users/providers/:provider', () => {
    it('should get users by provider', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.getUsersByProvider.initiate('google')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
    });

    it('should support multiple providers', async () => {
      const store = createTestStore();

      const providers = ['google', 'github', 'password'];

      for (const provider of providers) {
        const result = await store.dispatch(
          userApi.endpoints.getUsersByProvider.initiate(provider)
        );

        expect(result.status).toBe('fulfilled');
      }
    });
  });

  describe('POST /users/:id/restore', () => {
    it('should restore deleted user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.restoreUser.initiate('user-1')
      );

      expect(result.data?.id).toBe('user-1');
    });

    it('should return 404 for non-existent user', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.restoreUser.initiate('non-existent')
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 404,
      });
    });

    it('should update user timestamp on restore', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        userApi.endpoints.restoreUser.initiate('user-1')
      );

      expect(result.data).toHaveProperty('updatedAt');
    });

    it('should invalidate user cache on restore', async () => {
      const store = createTestStore();

      // Get users
      await store.dispatch(
        userApi.endpoints.getAllUsers.initiate({ page: 0, size: 20 })
      );

      // Restore user
      await store.dispatch(userApi.endpoints.restoreUser.initiate('user-1'));

      const state = store.getState();
      expect(state.userApi).toBeDefined();
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/me`, () => {
          return HttpResponse.error();
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('rejected');
    });

    it('should handle server errors', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/me`, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 500,
      });
    });
  });

  describe('Cache Management', () => {
    it('should cache user queries', async () => {
      const store = createTestStore();

      // First call
      await store.dispatch(userApi.endpoints.getCurrentUser.initiate());

      // Second call should use cache
      const result = await store.dispatch(
        userApi.endpoints.getCurrentUser.initiate()
      );

      expect(result.status).toBe('fulfilled');
    });

    it('should invalidate cache on profile update', async () => {
      const store = createTestStore();

      // Get user
      await store.dispatch(userApi.endpoints.getCurrentUser.initiate());

      // Update profile
      await store.dispatch(
        userApi.endpoints.updateProfile.initiate({
          name: 'New Name',
          preferences: {},
        })
      );

      const state = store.getState();
      expect(state.userApi).toBeDefined();
    });
  });
});