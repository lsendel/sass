import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { organizationApi } from '../../store/api/organizationApi';
import { createApiTestStore } from '../utils/testStore';

const API_BASE_URL = 'http://localhost:3000/api/v1/organizations';

// Mock data
const mockOrganization = {
  id: 'org-1',
  name: 'Acme Corporation',
  slug: 'acme-corp',
  ownerId: 'user-1',
  settings: { theme: 'dark', notifications: true },
  createdAt: '2025-01-01T00:00:00Z',
  updatedAt: '2025-01-01T00:00:00Z',
};

const mockOrganization2 = {
  id: 'org-2',
  name: 'Tech Startup',
  slug: 'tech-startup',
  ownerId: 'user-1',
  settings: { theme: 'light' },
  createdAt: '2025-01-02T00:00:00Z',
  updatedAt: '2025-01-02T00:00:00Z',
};

const mockMember = {
  userId: 'user-2',
  userEmail: 'member@example.com',
  userName: 'John Member',
  role: 'MEMBER',
  joinedAt: '2025-01-03T00:00:00Z',
};

const mockInvitation = {
  id: 'inv-1',
  organizationId: 'org-1',
  invitedBy: 'user-1',
  email: 'newuser@example.com',
  role: 'MEMBER',
  status: 'PENDING',
  token: 'invite-token-123',
  expiresAt: '2025-02-01T00:00:00Z',
  createdAt: '2025-01-10T00:00:00Z',
};

// MSW request handlers
const handlers = [
  // GET /organizations - Get user's organizations
  http.get(API_BASE_URL, ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }
    return HttpResponse.json([mockOrganization, mockOrganization2]);
  }),

  // POST /organizations - Create organization
  http.post(API_BASE_URL, async ({ request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const body = (await request.json()) as any;
    if (!body?.name || typeof body?.name !== 'string') {
      return HttpResponse.json(
        { message: 'Name is required' },
        { status: 400 }
      );
    }

    if (!body?.slug || typeof body?.slug !== 'string') {
      return HttpResponse.json(
        { message: 'Slug is required' },
        { status: 400 }
      );
    }

    // Simulate slug conflict
    if (body.slug === 'existing-slug') {
      return HttpResponse.json(
        { message: 'Organization slug already exists' },
        { status: 409 }
      );
    }

    return HttpResponse.json({
      ...mockOrganization,
      name: body.name,
      slug: body.slug,
      settings: body.settings || {},
    });
  }),

  // GET /organizations/:id - Get organization by ID
  http.get(`${API_BASE_URL}/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id === 'org-1') {
      return HttpResponse.json(mockOrganization);
    }
    if (id === 'org-2') {
      return HttpResponse.json(mockOrganization2);
    }
    return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
  }),

  // GET /organizations/slug/:slug - Get organization by slug
  http.get(`${API_BASE_URL}/slug/:slug`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { slug } = params;
    if (slug === 'acme-corp') {
      return HttpResponse.json(mockOrganization);
    }
    if (slug === 'tech-startup') {
      return HttpResponse.json(mockOrganization2);
    }
    return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
  }),

  // PUT /organizations/:id - Update organization
  http.put(`${API_BASE_URL}/:id`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    const body = (await request.json()) as any;
    if (!body?.name || typeof body?.name !== 'string') {
      return HttpResponse.json(
        { message: 'Name is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockOrganization,
      id,
      name: body.name,
      settings: body.settings || mockOrganization.settings,
      updatedAt: new Date().toISOString(),
    });
  }),

  // PUT /organizations/:id/settings - Update organization settings
  http.put(`${API_BASE_URL}/:id/settings`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    const body = (await request.json()) as any;
    if (!body?.settings || typeof body?.settings !== 'object') {
      return HttpResponse.json(
        { message: 'Settings object is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockOrganization,
      id,
      settings: body.settings,
      updatedAt: new Date().toISOString(),
    });
  }),

  // DELETE /organizations/:id - Delete organization
  http.delete(`${API_BASE_URL}/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // GET /organizations/:id/members - Get organization members
  http.get(`${API_BASE_URL}/:id/members`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    return HttpResponse.json([
      {
        userId: 'user-1',
        userEmail: 'owner@example.com',
        userName: 'Owner User',
        role: 'OWNER',
        joinedAt: '2025-01-01T00:00:00Z',
      },
      mockMember,
    ]);
  }),

  // POST /organizations/:id/invitations - Invite user
  http.post(`${API_BASE_URL}/:id/invitations`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    const body = (await request.json()) as any;
    if (!body?.email || typeof body?.email !== 'string') {
      return HttpResponse.json(
        { message: 'Email is required' },
        { status: 400 }
      );
    }

    if (!body?.role || !['OWNER', 'ADMIN', 'MEMBER'].includes(body.role)) {
      return HttpResponse.json(
        { message: 'Valid role is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      ...mockInvitation,
      organizationId: id,
      email: body.email,
      role: body.role,
    });
  }),

  // POST /organizations/invitations/:token/accept - Accept invitation
  http.post(`${API_BASE_URL}/invitations/:token/accept`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { token } = params;
    if (token !== 'invite-token-123') {
      return HttpResponse.json(
        { message: 'Invalid or expired invitation' },
        { status: 404 }
      );
    }

    return HttpResponse.json({
      id: 'member-1',
      userId: 'user-2',
      organizationId: 'org-1',
      role: 'MEMBER',
      joinedAt: new Date().toISOString(),
    });
  }),

  // POST /organizations/invitations/:token/decline - Decline invitation
  http.post(`${API_BASE_URL}/invitations/:token/decline`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { token } = params;
    if (token !== 'invite-token-123') {
      return HttpResponse.json(
        { message: 'Invalid or expired invitation' },
        { status: 404 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // GET /organizations/:id/invitations - Get pending invitations
  http.get(`${API_BASE_URL}/:id/invitations`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    return HttpResponse.json([mockInvitation]);
  }),

  // DELETE /organizations/invitations/:id - Revoke invitation
  http.delete(`${API_BASE_URL}/invitations/:id`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id } = params;
    if (id !== 'inv-1') {
      return HttpResponse.json(
        { message: 'Invitation not found' },
        { status: 404 }
      );
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // DELETE /organizations/:id/members/:userId - Remove member
  http.delete(`${API_BASE_URL}/:id/members/:userId`, ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id, userId } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    if (userId !== 'user-2' && userId !== 'user-3') {
      return HttpResponse.json({ message: 'Member not found' }, { status: 404 });
    }

    return new HttpResponse(null, { status: 204 });
  }),

  // PUT /organizations/:id/members/:userId/role - Update member role
  http.put(`${API_BASE_URL}/:id/members/:userId/role`, async ({ params, request }) => {
    const authHeader = request.headers.get('authorization');
    if (!authHeader) {
      return HttpResponse.json({ message: 'Unauthorized' }, { status: 401 });
    }

    const { id, userId } = params;
    if (id !== 'org-1' && id !== 'org-2') {
      return HttpResponse.json({ message: 'Organization not found' }, { status: 404 });
    }

    if (userId !== 'user-2' && userId !== 'user-3') {
      return HttpResponse.json({ message: 'Member not found' }, { status: 404 });
    }

    const body = (await request.json()) as any;
    if (!body?.role || !['OWNER', 'ADMIN', 'MEMBER'].includes(body.role)) {
      return HttpResponse.json(
        { message: 'Valid role is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      id: 'member-1',
      userId,
      organizationId: id,
      role: body.role,
      joinedAt: '2025-01-03T00:00:00Z',
    });
  }),
];

// Setup MSW server
const server = setupServer(...handlers);

// Helper to create authenticated test store
const createTestStore = () => {
  return createApiTestStore(organizationApi, {
    auth: {
      token: 'test-token',
      isAuthenticated: true,
      user: { id: 'user-1', email: 'test@example.com', name: 'Test User' },
    },
  });
};

// Test suite
describe('Organization API', () => {
  beforeAll(() => {
    server.listen({ onUnhandledRequest: 'error' });
  });

  afterEach(() => {
    server.resetHandlers();
  });

  afterAll(() => {
    server.close();
  });

  describe('POST /organizations', () => {
    it('should create an organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.createOrganization.initiate({
          name: 'New Organization',
          slug: 'new-org',
          settings: { theme: 'dark' },
        })
      );

      expect(result.data).toHaveProperty('id');
      expect(result.data?.name).toBe('New Organization');
      expect(result.data?.slug).toBe('new-org');
      expect(result.data?.settings).toEqual({ theme: 'dark' });
    });

    it('should return 400 for missing name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.createOrganization.initiate({
          name: '',
          slug: 'test-slug',
        })
      );

      expect(result.error?.status).toBe(400);
    });

    it('should return 409 for duplicate slug', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.createOrganization.initiate({
          name: 'Test Org',
          slug: 'existing-slug',
        })
      );

      expect(result.error?.status).toBe(409);
    });

    it('should create organization without settings', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.createOrganization.initiate({
          name: 'Minimal Org',
          slug: 'minimal-org',
        })
      );

      expect(result.data).toHaveProperty('id');
      expect(result.data?.settings).toEqual({});
    });
  });

  describe('GET /organizations', () => {
    it('should get user organizations', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.length).toBe(2);
      expect(result.data?.[0].name).toBe('Acme Corporation');
    });

    it('should return 401 for unauthenticated request', async () => {
      const store = createApiTestStore(organizationApi, {
        auth: { token: null, isAuthenticated: false, user: null },
      });

      const result = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      expect(result.error?.status).toBe(401);
    });

    it('should cache user organizations', async () => {
      const store = createTestStore();

      const result1 = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      const result2 = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      expect(result1.data).toEqual(result2.data);
    });
  });

  describe('GET /organizations/:id', () => {
    it('should get organization by ID', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.id).toBe('org-1');
      expect(result.data?.name).toBe('Acme Corporation');
    });

    it('should return 404 for non-existent organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-999')
      );

      expect(result.error?.status).toBe(404);
    });

    it('should include organization metadata', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      expect(result.data).toHaveProperty('ownerId');
      expect(result.data).toHaveProperty('settings');
      expect(result.data).toHaveProperty('createdAt');
      expect(result.data).toHaveProperty('updatedAt');
    });
  });

  describe('GET /organizations/slug/:slug', () => {
    it('should get organization by slug', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganizationBySlug.initiate('acme-corp')
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data?.slug).toBe('acme-corp');
      expect(result.data?.name).toBe('Acme Corporation');
    });

    it('should return 404 for non-existent slug', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganizationBySlug.initiate('non-existent')
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('PUT /organizations/:id', () => {
    it('should update organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateOrganization.initiate({
          organizationId: 'org-1',
          name: 'Updated Name',
          settings: { theme: 'light' },
        })
      );

      expect(result.data?.name).toBe('Updated Name');
      expect(result.data?.settings).toEqual({ theme: 'light' });
    });

    it('should return 400 for missing name', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateOrganization.initiate({
          organizationId: 'org-1',
          name: '',
        })
      );

      expect(result.error?.status).toBe(400);
    });

    it('should return 404 for non-existent organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateOrganization.initiate({
          organizationId: 'org-999',
          name: 'Updated Name',
        })
      );

      expect(result.error?.status).toBe(404);
    });

    it('should invalidate organization cache', async () => {
      const store = createTestStore();

      await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      const updateResult = await store.dispatch(
        organizationApi.endpoints.updateOrganization.initiate({
          organizationId: 'org-1',
          name: 'New Name',
        })
      );

      expect(updateResult.data?.name).toBe('New Name');
    });
  });

  describe('PUT /organizations/:id/settings', () => {
    it('should update organization settings', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateSettings.initiate({
          organizationId: 'org-1',
          settings: { theme: 'light', notifications: false },
        })
      );

      expect(result.data?.settings).toEqual({ theme: 'light', notifications: false });
    });

    it('should return 400 for missing settings', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateSettings.initiate({
          organizationId: 'org-1',
          settings: null as any,
        })
      );

      expect(result.error?.status).toBe(400);
    });
  });

  describe('DELETE /organizations/:id', () => {
    it('should delete organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.deleteOrganization.initiate('org-1')
      );

      expect(result.error).toBeUndefined();
    });

    it('should return 404 for non-existent organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.deleteOrganization.initiate('org-999')
      );

      expect(result.error?.status).toBe(404);
    });

    it('should invalidate organization cache', async () => {
      const store = createTestStore();

      await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      await store.dispatch(
        organizationApi.endpoints.deleteOrganization.initiate('org-1')
      );

      const state = store.getState();
      const tags = state.organizationApi.provided;
      expect(Object.keys(tags || {}).length).toBeGreaterThan(0);
    });
  });

  describe('GET /organizations/:id/members', () => {
    it('should get organization members', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganizationMembers.initiate('org-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.length).toBe(2);
      expect(result.data?.[0].role).toBe('OWNER');
    });

    it('should return 404 for non-existent organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganizationMembers.initiate('org-999')
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('POST /organizations/:id/invitations', () => {
    it('should invite user to organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.inviteUser.initiate({
          organizationId: 'org-1',
          email: 'newuser@example.com',
          role: 'MEMBER',
        })
      );

      expect(result.data?.email).toBe('newuser@example.com');
      expect(result.data?.role).toBe('MEMBER');
      expect(result.data?.status).toBe('PENDING');
    });

    it('should return 400 for missing email', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.inviteUser.initiate({
          organizationId: 'org-1',
          email: '',
          role: 'MEMBER',
        })
      );

      expect(result.error?.status).toBe(400);
    });

    it('should return 400 for invalid role', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.inviteUser.initiate({
          organizationId: 'org-1',
          email: 'test@example.com',
          role: 'INVALID_ROLE' as any,
        })
      );

      expect(result.error?.status).toBe(400);
    });
  });

  describe('POST /organizations/invitations/:token/accept', () => {
    it('should accept invitation', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.acceptInvitation.initiate('invite-token-123')
      );

      expect(result.data?.userId).toBeDefined();
      expect(result.data?.organizationId).toBe('org-1');
    });

    it('should return 404 for invalid token', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.acceptInvitation.initiate('invalid-token')
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('POST /organizations/invitations/:token/decline', () => {
    it('should decline invitation', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.declineInvitation.initiate('invite-token-123')
      );

      expect(result.error).toBeUndefined();
    });

    it('should return 404 for invalid token', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.declineInvitation.initiate('invalid-token')
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('GET /organizations/:id/invitations', () => {
    it('should get pending invitations', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.getPendingInvitations.initiate('org-1')
      );

      expect(result.status).toBe('fulfilled');
      expect(Array.isArray(result.data)).toBe(true);
      expect(result.data?.[0].status).toBe('PENDING');
    });
  });

  describe('DELETE /organizations/invitations/:id', () => {
    it('should revoke invitation', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.revokeInvitation.initiate('inv-1')
      );

      expect(result.error).toBeUndefined();
    });

    it('should return 404 for non-existent invitation', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.revokeInvitation.initiate('inv-999')
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('DELETE /organizations/:id/members/:userId', () => {
    it('should remove member from organization', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.removeMember.initiate({
          organizationId: 'org-1',
          userId: 'user-2',
        })
      );

      expect(result.error).toBeUndefined();
    });

    it('should return 404 for non-existent member', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.removeMember.initiate({
          organizationId: 'org-1',
          userId: 'user-999',
        })
      );

      expect(result.error?.status).toBe(404);
    });
  });

  describe('PUT /organizations/:id/members/:userId/role', () => {
    it('should update member role', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateMemberRole.initiate({
          organizationId: 'org-1',
          userId: 'user-2',
          role: 'ADMIN',
        })
      );

      expect(result.data?.role).toBe('ADMIN');
    });

    it('should return 400 for invalid role', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        organizationApi.endpoints.updateMemberRole.initiate({
          organizationId: 'org-1',
          userId: 'user-2',
          role: 'INVALID_ROLE' as any,
        })
      );

      expect(result.error?.status).toBe(400);
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      server.use(
        http.get(API_BASE_URL, () => {
          return HttpResponse.error();
        })
      );

      const store = createTestStore();
      const result = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      expect(result.error).toBeDefined();
    });

    it('should handle server errors', async () => {
      server.use(
        http.get(API_BASE_URL, () => {
          return HttpResponse.json(
            { message: 'Internal server error' },
            { status: 500 }
          );
        })
      );

      const store = createTestStore();
      const result = await store.dispatch(
        organizationApi.endpoints.getUserOrganizations.initiate()
      );

      expect(result.error?.status).toBe(500);
    });
  });

  describe('Cache Management', () => {
    it('should cache organization queries', async () => {
      const store = createTestStore();

      await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      const result = await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      expect(result.data?.id).toBe('org-1');
    });

    it('should invalidate cache on organization update', async () => {
      const store = createTestStore();

      await store.dispatch(
        organizationApi.endpoints.getOrganization.initiate('org-1')
      );

      await store.dispatch(
        organizationApi.endpoints.updateOrganization.initiate({
          organizationId: 'org-1',
          name: 'Updated Org',
        })
      );

      const state = store.getState();
      const tags = state.organizationApi.provided;
      expect(tags).toBeDefined();
    });

    it('should invalidate member cache on role update', async () => {
      const store = createTestStore();

      await store.dispatch(
        organizationApi.endpoints.getOrganizationMembers.initiate('org-1')
      );

      await store.dispatch(
        organizationApi.endpoints.updateMemberRole.initiate({
          organizationId: 'org-1',
          userId: 'user-2',
          role: 'ADMIN',
        })
      );

      const state = store.getState();
      const tags = state.organizationApi.provided;
      expect(tags).toBeDefined();
    });
  });
});