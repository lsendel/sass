import type { User } from '@/types/api'

export type UserFixture = User & {
  name?: string
  provider?: string
  preferences?: Record<string, unknown>
  lastActiveAt?: string | null
}

const DEFAULT_TIMESTAMP = '2024-01-01T00:00:00Z'

const BASE_USER: UserFixture = {
  id: '00000000-0000-0000-0000-00000000ffff',
  email: 'test@example.com',
  firstName: 'Test',
  lastName: 'User',
  role: 'USER',
  emailVerified: true,
  organizationId: '00000000-0000-0000-0000-00000000aaaa',
  createdAt: DEFAULT_TIMESTAMP,
  updatedAt: DEFAULT_TIMESTAMP,
  lastLoginAt: DEFAULT_TIMESTAMP,
  name: 'Test User',
  provider: 'google',
  preferences: {},
  lastActiveAt: null,
}

export const createMockUser = (
  overrides: Partial<UserFixture> = {}
): UserFixture => ({
  ...BASE_USER,
  ...overrides,
})
