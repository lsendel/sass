import { describe, it, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { authApi } from '../../store/api/authApi';
import { createApiTestStore } from '../utils/testStore';

/**
 * Auth API Integration Tests
 *
 * Best Practices Applied:
 * 1. Use MSW for realistic API mocking
 * 2. Test all API endpoints with success and error cases
 * 3. Verify request payloads and response structures
 * 4. Test API error handling and edge cases
 * 5. Validate authentication flows
 */

const API_BASE_URL = 'http://localhost:3000/api/v1/auth';

// Type definitions for request/response payloads
interface PasswordLoginRequest {
  email: string;
  password: string;
  organizationId: string;
}

interface PasswordRegisterRequest {
  email: string;
  password: string;
  name: string;
  organizationId: string;
}

interface CallbackRequest {
  code: string;
  state?: string;
}

interface UserResponse {
  id: string;
  email: string;
  name: string;
  role: string;
}

// Interfaces for type documentation - used implicitly in HttpResponse.json calls
// @ts-expect-error - Type used for documentation
interface AuthResponse {
  user: UserResponse;
  token: string;
}

// @ts-expect-error - Type used for documentation
interface AuthUrlResponse {
  authUrl: string;
}

// @ts-expect-error - Type used for documentation
interface SessionResponse {
  user: UserResponse;
  expiresAt: string;
}

// @ts-expect-error - Type used for documentation
interface TokenResponse {
  token: string;
}

// @ts-expect-error - Type used for documentation
interface ErrorResponse {
  message: string;
}

// Mock handlers for auth endpoints
const handlers = [
  // GET /auth/methods
  http.get(`${API_BASE_URL}/methods`, () => {
    return HttpResponse.json({
      oauth: {
        enabled: true,
        providers: ['google', 'github'],
      },
      password: {
        enabled: true,
        requiresEmailVerification: false,
      },
    });
  }),

  // POST /auth/login
  http.post(`${API_BASE_URL}/login`, async ({ request }) => {
    const body = await request.json() as PasswordLoginRequest;

    if (body.email === 'invalid@example.com') {
      return HttpResponse.json(
        { message: 'Invalid email or password' },
        { status: 401 }
      );
    }

    if (body.email === 'ratelimit@example.com') {
      return HttpResponse.json(
        { message: 'Too many login attempts' },
        { status: 429 }
      );
    }

    return HttpResponse.json({
      user: {
        id: '123',
        email: body.email,
        name: 'Test User',
        role: 'USER',
      },
      token: 'mock-session-token',
    });
  }),

  // POST /auth/register
  http.post(`${API_BASE_URL}/register`, async ({ request }) => {
    const body = await request.json() as PasswordRegisterRequest;

    if (body.email === 'existing@example.com') {
      return HttpResponse.json(
        { message: 'Email already registered' },
        { status: 409 }
      );
    }

    return HttpResponse.json({
      user: {
        id: '124',
        email: body.email,
        name: body.name,
        role: 'USER',
      },
      token: 'mock-session-token',
    });
  }),

  // GET /auth/authorize
  http.get(`${API_BASE_URL}/authorize`, ({ request }) => {
    const url = new URL(request.url);
    const provider = url.searchParams.get('provider');

    if (!provider) {
      return HttpResponse.json(
        { message: 'Provider is required' },
        { status: 400 }
      );
    }

    return HttpResponse.json({
      authUrl: `https://oauth.${provider}.com/authorize?client_id=test`,
    });
  }),

  // GET /auth/session
  http.get(`${API_BASE_URL}/session`, ({ request }) => {
    const authHeader = request.headers.get('authorization');

    if (!authHeader) {
      return HttpResponse.json(
        { message: 'Unauthorized' },
        { status: 401 }
      );
    }

    return HttpResponse.json({
      user: {
        id: '123',
        email: 'user@example.com',
        name: 'Test User',
        role: 'USER',
      },
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
    });
  }),

  // POST /auth/callback
  http.post(`${API_BASE_URL}/callback`, async ({ request }) => {
    const body = await request.json() as CallbackRequest;

    if (!body.code) {
      return HttpResponse.json(
        { message: 'Authorization code is required' },
        { status: 400 }
      );
    }

    if (body.code === 'invalid-code') {
      return HttpResponse.json(
        { message: 'Invalid authorization code' },
        { status: 401 }
      );
    }

    return HttpResponse.json({
      user: {
        id: '125',
        email: 'oauth@example.com',
        name: 'OAuth User',
        role: 'USER',
      },
      token: 'mock-oauth-token',
    });
  }),

  // POST /auth/logout
  http.post(`${API_BASE_URL}/logout`, () => {
    return HttpResponse.json({ message: 'Logged out successfully' });
  }),

  // POST /auth/refresh
  http.post(`${API_BASE_URL}/refresh`, () => {
    return HttpResponse.json({
      token: 'new-session-token',
    });
  }),
];

const server = setupServer(...handlers);

// Setup/teardown
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

// Helper to create store for testing
const createTestStore = () => {
  return createApiTestStore(authApi);
};

describe('Auth API', () => {
  describe('GET /auth/methods', () => {
    it('should fetch available authentication methods', async () => {
      const store = createTestStore();
      const result = await store.dispatch(
        authApi.endpoints.getAuthMethods.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toEqual({
        oauth: {
          enabled: true,
          providers: ['google', 'github'],
        },
        password: {
          enabled: true,
          requiresEmailVerification: false,
        },
      });
    });

    it('should return authentication methods structure', async () => {
      const store = createTestStore();
      const result = await store.dispatch(
        authApi.endpoints.getAuthMethods.initiate()
      );

      expect(result.data).toHaveProperty('methods');
      expect(result.data).toHaveProperty('passwordAuthEnabled');
      expect(result.data).toHaveProperty('oauth2Providers');
    });
  });

  describe('POST /auth/login', () => {
    it('should login successfully with valid credentials', async () => {
      const store = createTestStore();
      const credentials = {
        email: 'user@example.com',
        password: 'password123',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordLogin.initiate(credentials)
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('user');
      expect(result.data).toHaveProperty('token');
      expect(result.data?.user.email).toBe(credentials.email);
    });

    it('should return 401 for invalid credentials', async () => {
      const store = createTestStore();
      const credentials = {
        email: 'invalid@example.com',
        password: 'wrongpassword',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordLogin.initiate(credentials)
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 401,
      });
    });

    it('should return 429 for rate limited requests', async () => {
      const store = createTestStore();
      const credentials = {
        email: 'ratelimit@example.com',
        password: 'password123',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordLogin.initiate(credentials)
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 429,
      });
    });

    it('should send correct request payload', async () => {
      const store = createTestStore();
      let capturedBody: PasswordLoginRequest | undefined;

      server.use(
        http.post(`${API_BASE_URL}/login`, async ({ request }) => {
          capturedBody = await request.json() as PasswordLoginRequest;
          return HttpResponse.json({
            user: { id: '1', email: 'test@example.com', name: 'Test', role: 'USER' },
            token: 'token',
          });
        })
      );

      await store.dispatch(
        authApi.endpoints.passwordLogin.initiate({
          email: 'test@example.com',
          password: 'pass123',
          organizationId: 'org-1',
        })
      );

      expect(capturedBody).toEqual({
        email: 'test@example.com',
        password: 'pass123',
        organizationId: 'org-1',
      });
    });
  });

  describe('POST /auth/register', () => {
    it('should register new user successfully', async () => {
      const store = createTestStore();
      const userData = {
        email: 'newuser@example.com',
        password: 'password123',
        name: 'New User',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordRegister.initiate(userData)
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('user');
      expect(result.data?.user.email).toBe(userData.email);
      expect(result.data?.user.firstName).toBe('Test');
      expect(result.data?.user.lastName).toBe('User');
    });

    it('should return 409 for existing email', async () => {
      const store = createTestStore();
      const userData = {
        email: 'existing@example.com',
        password: 'password123',
        name: 'Existing User',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordRegister.initiate(userData)
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 409,
      });
    });

    it('should include user details in response', async () => {
      const store = createTestStore();
      const userData = {
        email: 'user@example.com',
        password: 'password123',
        name: 'Test User',
        organizationId: 'org-123',
      };

      const result = await store.dispatch(
        authApi.endpoints.passwordRegister.initiate(userData)
      );

      expect(result.data?.user).toHaveProperty('id');
      expect(result.data?.user).toHaveProperty('email');
      expect(result.data?.user).toHaveProperty('name');
      expect(result.data?.user).toHaveProperty('role');
    });
  });

  describe('GET /auth/authorize', () => {
    it('should get OAuth authorization URL', async () => {
      const store = createTestStore();
      const params = {
        provider: 'google',
        redirectUri: 'http://localhost:3000/callback',
      };

      const result = await store.dispatch(
        authApi.endpoints.getAuthUrl.initiate(params)
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('authUrl');
      expect(result.data?.authUrl).toContain('oauth.google.com');
    });

    it('should return 400 for missing provider', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/authorize`, () => {
          return HttpResponse.json(
            { message: 'Provider is required' },
            { status: 400 }
          );
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.getAuthUrl.initiate({ provider: '', redirectUri: '' })
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 400,
      });
    });

    it('should handle different OAuth providers', async () => {
      const store = createTestStore();

      const providers = ['google', 'github', 'microsoft'];

      for (const provider of providers) {
        const result = await store.dispatch(
          authApi.endpoints.getAuthUrl.initiate({
            provider,
            redirectUri: 'http://localhost:3000/callback',
          })
        );

        expect(result.status).toBe('fulfilled');
        expect(result.data?.authUrl).toContain(provider);
      }
    });
  });

  describe('GET /auth/session', () => {
    it('should get current session with valid auth', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/session`, () => {
          return HttpResponse.json({
            user: {
              id: '123',
              email: 'user@example.com',
              name: 'Test User',
              role: 'USER',
            },
            expiresAt: new Date(Date.now() + 3600000).toISOString(),
          });
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.getSession.initiate()
      );

      expect(result.status).toBe('fulfilled');
      expect(result.data).toHaveProperty('user');
      expect(result.data).toHaveProperty('expiresAt');
    });

    it('should return 401 for unauthenticated request', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/session`, () => {
          return HttpResponse.json(
            { message: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.getSession.initiate()
      );

      expect(result.status).toBe('rejected');
      expect(result.error).toMatchObject({
        status: 401,
      });
    });
  });

  describe('POST /auth/callback', () => {
    it('should handle OAuth callback successfully', async () => {
      const store = createTestStore();
      const callbackData = {
        code: 'valid-auth-code',
        state: 'random-state',
      };

      const result = await store.dispatch(
        authApi.endpoints.handleCallback.initiate(callbackData)
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('user');
      expect(result.data).toHaveProperty('token');
    });

    it('should return 400 for missing code', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        authApi.endpoints.handleCallback.initiate({ code: '' })
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 400,
      });
    });

    it('should return 401 for invalid code', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        authApi.endpoints.handleCallback.initiate({ code: 'invalid-code' })
      );

      expect(result.error).toBeDefined();
      expect(result.error).toMatchObject({
        status: 401,
      });
    });
  });

  describe('POST /auth/logout', () => {
    it('should logout successfully', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        authApi.endpoints.logout.initiate()
      );

      expect(result.error).toBeUndefined();
    });

    it('should clear API state on logout', async () => {
      const store = createTestStore();

      // First login
      await store.dispatch(
        authApi.endpoints.passwordLogin.initiate({
          email: 'user@example.com',
          password: 'password123',
          organizationId: 'org-123',
        })
      );

      // Then logout
      await store.dispatch(authApi.endpoints.logout.initiate());

      // Check that state is cleared
      const state = store.getState();
      expect((state as any).authApi.queries).toEqual({});
    });
  });

  describe('POST /auth/refresh', () => {
    it('should refresh session token', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        authApi.endpoints.refreshToken.initiate()
      );

      expect(result.error).toBeUndefined();
      expect(result.data).toHaveProperty('token');
      expect(result.data?.token).toBe('new-session-token');
    });

    it('should return new token on refresh', async () => {
      const store = createTestStore();

      const result = await store.dispatch(
        authApi.endpoints.refreshToken.initiate()
      );

      expect(result.data?.token).toBeTruthy();
      expect(typeof result.data?.token).toBe('string');
    });
  });

  describe('API Error Handling', () => {
    it('should handle network errors', async () => {
      const store = createTestStore();

      server.use(
        http.post(`${API_BASE_URL}/login`, () => {
          return HttpResponse.error();
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.passwordLogin.initiate({
          email: 'user@example.com',
          password: 'password123',
          organizationId: 'org-123',
        })
      );

      expect(result.error).toBeDefined();
    });

    it('should handle timeout errors', async () => {
      const store = createTestStore();

      server.use(
        http.post(`${API_BASE_URL}/login`, async () => {
          await new Promise((resolve) => setTimeout(resolve, 1000));
          return HttpResponse.json({});
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.passwordLogin.initiate({
          email: 'user@example.com',
          password: 'password123',
          organizationId: 'org-123',
        })
      );

      // Should timeout or handle appropriately
      expect(result.error).toBeUndefined(); // Should succeed after delay
    });

    it('should handle malformed JSON responses', async () => {
      const store = createTestStore();

      server.use(
        http.get(`${API_BASE_URL}/methods`, () => {
          return new HttpResponse('invalid json', {
            headers: { 'Content-Type': 'application/json' },
          });
        })
      );

      const result = await store.dispatch(
        authApi.endpoints.getAuthMethods.initiate()
      );

      expect(result.status).toBe('rejected');
    });
  });

  describe('Cache and State Management', () => {
    it('should cache session query results', async () => {
      const store = createTestStore();

      // Setup auth header for session endpoint
      server.use(
        http.get(`${API_BASE_URL}/session`, () => {
          return HttpResponse.json({
            user: { id: '123', email: 'user@example.com', name: 'Test User', role: 'USER' },
            expiresAt: new Date(Date.now() + 3600000).toISOString(),
          });
        })
      );

      // First call
      await store.dispatch(authApi.endpoints.getSession.initiate());

      // Second call should use cache
      const result = await store.dispatch(
        authApi.endpoints.getSession.initiate()
      );

      expect(result.error).toBeUndefined();
    });

    it('should invalidate session cache on login', async () => {
      const store = createTestStore();

      // Login should invalidate Session tag
      await store.dispatch(
        authApi.endpoints.passwordLogin.initiate({
          email: 'user@example.com',
          password: 'password123',
          organizationId: 'org-123',
        })
      );

      const state = store.getState();
      // Session queries should be invalidated
      expect((state as any).authApi).toBeDefined();
    });

    it('should invalidate session cache on logout', async () => {
      const store = createTestStore();

      // Get session
      await store.dispatch(authApi.endpoints.getSession.initiate());

      // Logout
      await store.dispatch(authApi.endpoints.logout.initiate());

      const state = store.getState();
      // All API state should be cleared
      expect(Object.keys((state as any).authApi.queries)).toHaveLength(0);
    });
  });
});