import { test, expect } from '@playwright/test';

/**
 * End-to-end tests for Project Management platform.
 *
 * Tests complete user journeys including authentication, workspace navigation,
 * project creation, task management, and real-time collaboration features.
 *
 * Constitutional Compliance:
 * - Performance: Tests <2s page load times and <1s real-time updates
 * - Accessibility: Tests keyboard navigation and screen reader compatibility
 * - Cross-browser: Runs on Chromium, Firefox, and WebKit
 */

// Test data constants
const TEST_USER = {
  email: 'test@example.com',
  password: 'password123',
  firstName: 'Test',
  lastName: 'User'
};

const TEST_WORKSPACE = {
  name: 'Test Workspace',
  slug: 'test-workspace',
  description: 'A workspace for end-to-end testing'
};

const TEST_PROJECT = {
  name: 'E2E Test Project',
  slug: 'e2e-test-project',
  description: 'Project created during end-to-end testing'
};

const TEST_TASK = {
  title: 'Test Task',
  description: 'Task created during end-to-end testing'
};

test.describe('Project Management Platform', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto('/');

    // Wait for initial page load
    await expect(page.locator('body')).toBeVisible();
  });

  // ========================================
  // Authentication Flow Tests
  // ========================================

  test('user can log in successfully', async ({ page }) => {
    // Navigate to login page
    await page.getByRole('link', { name: /login/i }).click();

    // Fill login form
    await page.getByLabel(/email/i).fill(TEST_USER.email);
    await page.getByLabel(/password/i).fill(TEST_USER.password);

    // Submit login form
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should redirect to dashboard
    await expect(page).toHaveURL(/\/dashboard/);

    // Should show user information
    await expect(page.getByText(`${TEST_USER.firstName} ${TEST_USER.lastName}`)).toBeVisible();
  });

  test('user cannot log in with invalid credentials', async ({ page }) => {
    await page.getByRole('link', { name: /login/i }).click();

    // Fill with invalid credentials
    await page.getByLabel(/email/i).fill('invalid@example.com');
    await page.getByLabel(/password/i).fill('wrongpassword');

    await page.getByRole('button', { name: /sign in/i }).click();

    // Should show error message
    await expect(page.getByText(/invalid credentials/i)).toBeVisible();

    // Should remain on login page
    await expect(page).toHaveURL(/\/auth\/login/);
  });

  test('user can register a new account', async ({ page }) => {
    await page.getByRole('link', { name: /register/i }).click();

    // Fill registration form
    await page.getByLabel(/email/i).fill('newuser@example.com');
    await page.getByLabel(/first name/i).fill('New');
    await page.getByLabel(/last name/i).fill('User');
    await page.getByLabel(/password/i).fill('securepassword123');

    await page.getByRole('button', { name: /create account/i }).click();

    // Should show success message
    await expect(page.getByText(/registration successful/i)).toBeVisible();
  });

  // ========================================
  // Workspace Management Tests
  // ========================================

  test('authenticated user can view workspaces', async ({ page }) => {
    // Login first
    await loginUser(page, TEST_USER);

    // Should be on dashboard with workspaces visible
    await expect(page.getByText('Acme Corp Projects')).toBeVisible();
    await expect(page.getByText(/workspace/i)).toBeVisible();

    // Should show workspace statistics
    await expect(page.getByText(/projects/i)).toBeVisible();
    await expect(page.getByText(/members/i)).toBeVisible();
    await expect(page.getByText(/storage/i)).toBeVisible();
  });

  test('user can create a new workspace', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Click create workspace button
    await page.getByRole('button', { name: /new workspace/i }).click();

    // Fill workspace creation form
    await page.getByLabel(/workspace name/i).fill(TEST_WORKSPACE.name);
    await page.getByLabel(/slug/i).fill(TEST_WORKSPACE.slug);
    await page.getByLabel(/description/i).fill(TEST_WORKSPACE.description);

    // Submit form
    await page.getByRole('button', { name: /create workspace/i }).click();

    // Should show success message and new workspace
    await expect(page.getByText(/workspace created/i)).toBeVisible();
    await expect(page.getByText(TEST_WORKSPACE.name)).toBeVisible();
  });

  test('workspace dashboard shows correct metrics', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Click on a workspace to view dashboard
    await page.getByText('Acme Corp Projects').click();

    // Should show workspace dashboard
    await expect(page.getByRole('heading', { name: 'Acme Corp Projects' })).toBeVisible();

    // Should show metrics
    await expect(page.getByText(/\d+ Projects/)).toBeVisible();
    await expect(page.getByText(/\d+ Members/)).toBeVisible();
    await expect(page.getByText(/Storage Used/)).toBeVisible();

    // Should show storage progress bar
    await expect(page.locator('[aria-label*="Storage usage"]')).toBeVisible();
  });

  // ========================================
  // Project Management Tests
  // ========================================

  test('user can create and view projects', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToWorkspace(page, 'Acme Corp Projects');

    // Create new project
    await page.getByRole('button', { name: /new project/i }).click();

    await page.getByLabel(/project name/i).fill(TEST_PROJECT.name);
    await page.getByLabel(/slug/i).fill(TEST_PROJECT.slug);
    await page.getByLabel(/description/i).fill(TEST_PROJECT.description);
    await page.getByLabel(/priority/i).selectOption('HIGH');

    await page.getByRole('button', { name: /create project/i }).click();

    // Should show new project in list
    await expect(page.getByText(TEST_PROJECT.name)).toBeVisible();
    await expect(page.getByText(TEST_PROJECT.description)).toBeVisible();
    await expect(page.getByText(/high priority/i)).toBeVisible();
  });

  test('project dashboard displays correctly', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToWorkspace(page, 'Acme Corp Projects');

    // Click on a project
    await page.getByText('E-commerce Platform').click();

    // Should show project dashboard
    await expect(page.getByRole('heading', { name: 'E-commerce Platform' })).toBeVisible();
    await expect(page.getByText(/modern e-commerce solution/i)).toBeVisible();

    // Should show project stats
    await expect(page.getByText(/progress/i)).toBeVisible();
    await expect(page.getByText(/team members/i)).toBeVisible();
    await expect(page.getByText(/\d+%/)).toBeVisible(); // Progress percentage

    // Should show tabs
    await expect(page.getByRole('tab', { name: /tasks/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /overview/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /activity/i })).toBeVisible();
  });

  // ========================================
  // Task Management Tests
  // ========================================

  test('user can create and manage tasks', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Should be on tasks tab by default
    await expect(page.getByRole('tab', { name: /tasks/i, selected: true })).toBeVisible();

    // Create new task
    await page.getByRole('button', { name: /add task/i }).click();

    await page.getByLabel(/task title/i).fill(TEST_TASK.title);
    await page.getByLabel(/description/i).fill(TEST_TASK.description);
    await page.getByLabel(/priority/i).selectOption('HIGH');
    await page.getByLabel(/due date/i).fill('2024-12-31');

    await page.getByRole('button', { name: /create task/i }).click();

    // Should show new task in list
    await expect(page.getByText(TEST_TASK.title)).toBeVisible();
    await expect(page.getByText(TEST_TASK.description)).toBeVisible();
    await expect(page.getByText(/high/i)).toBeVisible();
  });

  test('user can update task status', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Find a task with TODO status
    const todoTask = page.locator('[data-testid="task-item"]').filter({ hasText: /to do/i }).first();
    await expect(todoTask).toBeVisible();

    // Click the status checkbox to mark as in progress
    await todoTask.getByRole('button', { name: /mark task as complete/i }).click();

    // Should update to in progress
    await expect(todoTask.getByText(/in progress/i)).toBeVisible();

    // Click again to mark as completed
    await todoTask.getByRole('button', { name: /mark task as incomplete/i }).click();

    // Should show completed status
    await expect(todoTask.getByRole('button', { checked: true })).toBeVisible();
  });

  test('task filtering and sorting works correctly', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Test status filter
    await page.getByLabel(/all status/i).selectOption('COMPLETED');

    // Should only show completed tasks
    const taskItems = page.locator('[data-testid="task-item"]');
    await expect(taskItems).toHaveCount(1);
    await expect(page.getByText(/write api documentation/i)).toBeVisible();

    // Test priority filter
    await page.getByLabel(/all status/i).selectOption('ALL');
    await page.getByLabel(/all priority/i).selectOption('HIGH');

    // Should only show high priority tasks
    await expect(page.getByText(/implement user authentication/i)).toBeVisible();

    // Test sorting
    await page.getByLabel(/newest first/i).selectOption('priority');

    // Tasks should be sorted by priority (critical, high, medium, low)
    const firstTask = taskItems.first();
    await expect(firstTask.getByText(/high|critical/i)).toBeVisible();
  });

  // ========================================
  // Search Functionality Tests
  // ========================================

  test('global search returns relevant results', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Use global search
    const searchInput = page.getByPlaceholder(/search/i);
    await searchInput.fill('authentication');
    await searchInput.press('Enter');

    // Should show search results page
    await expect(page).toHaveURL(/\/search/);
    await expect(page.getByText(/search results/i)).toBeVisible();

    // Should show relevant results
    await expect(page.getByText(/implement user authentication/i)).toBeVisible();
    await expect(page.getByText(/task/i)).toBeVisible(); // Result type

    // Should show result metadata
    await expect(page.getByText(/e-commerce platform/i)).toBeVisible(); // Context
  });

  test('search suggestions appear and work correctly', async ({ page }) => {
    await loginUser(page, TEST_USER);

    const searchInput = page.getByPlaceholder(/search/i);
    await searchInput.fill('auth');

    // Should show search suggestions
    await expect(page.getByText('authentication')).toBeVisible();
    await expect(page.getByText(/tasks related to authentication/i)).toBeVisible();

    // Click on suggestion
    await page.getByText('authentication').click();

    // Should perform search with selected suggestion
    await expect(page).toHaveURL(/\/search/);
    await expect(page.getByDisplayValue('authentication')).toBeVisible();
  });

  // ========================================
  // Real-time Collaboration Tests
  // ========================================

  test('real-time task updates work correctly', async ({ page, context }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Open second tab to simulate another user
    const secondPage = await context.newPage();
    await loginUser(secondPage, TEST_USER);
    await navigateToProject(secondPage, 'E-commerce Platform');

    // Update task status in first tab
    const task = page.getByText('Implement user authentication').locator('..');
    await task.getByRole('button', { name: /mark task as complete/i }).click();

    // Second tab should see the update within 2 seconds
    await expect(secondPage.getByText('Implement user authentication').locator('..')).toContainText(/in progress/i, { timeout: 2000 });
  });

  test('user presence indicators work correctly', async ({ page, context }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Open second tab
    const secondPage = await context.newPage();
    await loginUser(secondPage, TEST_USER);
    await navigateToProject(secondPage, 'E-commerce Platform');

    // Should show presence indicators
    await expect(page.getByText(/\d+ users online/i)).toBeVisible({ timeout: 3000 });
    await expect(secondPage.getByText(/\d+ users online/i)).toBeVisible({ timeout: 3000 });
  });

  // ========================================
  // Performance Tests
  // ========================================

  test('dashboard loads within performance requirements', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Measure page load time
    const startTime = Date.now();
    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - startTime;

    // Should load within 2 seconds (2000ms)
    expect(loadTime).toBeLessThan(2000);

    // Core elements should be visible
    await expect(page.getByText(/dashboard/i)).toBeVisible();
    await expect(page.getByText(/workspaces/i)).toBeVisible();
  });

  test('project dashboard loads quickly', async ({ page }) => {
    await loginUser(page, TEST_USER);

    const startTime = Date.now();
    await navigateToProject(page, 'E-commerce Platform');
    const loadTime = Date.now() - startTime;

    // Project dashboard should load within 2 seconds
    expect(loadTime).toBeLessThan(2000);

    // Key elements should be visible
    await expect(page.getByRole('heading', { name: 'E-commerce Platform' })).toBeVisible();
    await expect(page.getByText(/progress/i)).toBeVisible();
  });

  // ========================================
  // Accessibility Tests
  // ========================================

  test('application is keyboard navigable', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Test keyboard navigation through main elements
    await page.keyboard.press('Tab');
    await expect(page.locator(':focus')).toBeVisible();

    // Should be able to navigate to workspace
    await page.keyboard.press('Enter');

    // Should navigate successfully
    await expect(page.getByText('Acme Corp Projects')).toBeVisible();
  });

  test('screen reader support works correctly', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToProject(page, 'E-commerce Platform');

    // Check for proper ARIA labels
    await expect(page.getByRole('button', { name: /new task/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /tasks/i })).toBeVisible();

    // Check progress bars have proper labels
    await expect(page.getByLabelText(/progress/i)).toBeVisible();
  });

  // ========================================
  // Error Handling Tests
  // ========================================

  test('application handles network errors gracefully', async ({ page }) => {
    await loginUser(page, TEST_USER);

    // Simulate network failure
    await page.route('**/api/**', route => route.abort());

    // Try to perform an action that requires network
    await page.getByRole('button', { name: /new project/i }).click();

    // Should show error message
    await expect(page.getByText(/network error|failed to load/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /retry/i })).toBeVisible();
  });

  test('form validation works correctly', async ({ page }) => {
    await loginUser(page, TEST_USER);
    await navigateToWorkspace(page, 'Acme Corp Projects');

    // Try to create project with invalid data
    await page.getByRole('button', { name: /new project/i }).click();

    // Submit with empty name
    await page.getByRole('button', { name: /create project/i }).click();

    // Should show validation errors
    await expect(page.getByText(/name is required/i)).toBeVisible();

    // Fill name but with invalid slug
    await page.getByLabel(/project name/i).fill('Test Project');
    await page.getByLabel(/slug/i).fill('ab'); // Too short

    await page.getByRole('button', { name: /create project/i }).click();

    // Should show slug validation error
    await expect(page.getByText(/slug must be at least 3 characters/i)).toBeVisible();
  });
});

// ========================================
// Helper Functions
// ========================================

async function loginUser(page, user) {
  await page.goto('/auth/login');
  await page.getByLabel(/email/i).fill(user.email);
  await page.getByLabel(/password/i).fill(user.password);
  await page.getByRole('button', { name: /sign in/i }).click();
  await expect(page).toHaveURL(/\/dashboard/);
}

async function navigateToWorkspace(page, workspaceName: string) {
  await page.getByText(workspaceName).click();
  await expect(page.getByRole('heading', { name: workspaceName })).toBeVisible();
}

async function navigateToProject(page, projectName: string) {
  // Assumes we're already in a workspace
  await page.getByText(projectName).click();
  await expect(page.getByRole('heading', { name: projectName })).toBeVisible();
}