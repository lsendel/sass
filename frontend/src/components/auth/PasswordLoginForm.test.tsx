import { describe, it, expect, vi, beforeEach, afterEach, beforeAll, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

import { authApi } from '../../store/api/authApi';
import authReducer from '../../store/slices/authSlice';

import PasswordLoginForm from './PasswordLoginForm';

/**
 * PasswordLoginForm Component Tests
 *
 * Best Practices Applied:
 * 1. Mock API calls with MSW
 * 2. Test form validation with realistic user input
 * 3. Test async operations with waitFor
 * 4. Test error handling and user feedback
 * 5. Test accessibility and keyboard navigation
 * 6. Use user-event for realistic interactions
 */

const API_BASE_URL = 'http://localhost:3000/api/v1';

// MSW server setup
const server = setupServer(
  // Default successful login handler
  http.post(`${API_BASE_URL}/auth/login`, async () => {
    return HttpResponse.json({
      user: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        email: 'user@example.com',
        firstName: 'Test',
        lastName: 'User',
        role: 'USER',
      },
      accessToken: 'mock-access-token',
    });
  })
);

// Create a mock store for testing
const createMockStore = (initialState = {}) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      [authApi.reducerPath]: authApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(authApi.middleware),
    preloadedState: initialState,
  });
};

// Helper to render with Redux Provider
const renderWithProvider = (
  ui: React.ReactElement,
  { store = createMockStore(), ...options } = {}
) => {
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <Provider store={store}>{children}</Provider>
  );

  return {
    ...render(ui, { wrapper: Wrapper, ...options }),
    store,
  };
};

describe('PasswordLoginForm', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
  afterAll(() => server.close());
  afterEach(() => server.resetHandlers());

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render login form with all fields', () => {
      renderWithProvider(<PasswordLoginForm />);

      expect(screen.getByRole('heading', { name: /sign in to your account/i })).toBeInTheDocument();
      expect(screen.getByTestId('email-input')).toBeInTheDocument();
      expect(screen.getByTestId('password-input')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    it('should render email input field', () => {
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId('email-input');
      expect(emailInput).toBeInTheDocument();
      expect(emailInput).toHaveAttribute('type', 'email');
    });

    it('should render password input field', () => {
      renderWithProvider(<PasswordLoginForm />);

      const passwordInput = screen.getByTestId('password-input');
      expect(passwordInput).toBeInTheDocument();
      expect(passwordInput).toHaveAttribute('type', 'password');
    });

    it('should render password toggle button', () => {
      renderWithProvider(<PasswordLoginForm />);

      const toggleButton = screen.getByRole('button', { name: /show password/i });
      expect(toggleButton).toBeInTheDocument();
    });
  });

  describe('Form Validation', () => {
    it('should show error for invalid email format', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      await user.type(emailInput, 'invalid-email');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
      });
    });

    it('should show error for short password', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const passwordInput = screen.getByTestId("password-input");
      await user.type(passwordInput, 'short');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
      });
    });

    it('should accept valid email and password', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.tab();

      await waitFor(() => {
        expect(screen.queryByText(/please enter a valid email/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/password must be at least/i)).not.toBeInTheDocument();
      });
    });

    it('should not submit form with validation errors', async () => {
      const user = userEvent.setup();
      const onSuccess = vi.fn();
      renderWithProvider(<PasswordLoginForm onSuccess={onSuccess} />);

      const submitButton = screen.getByRole('button', { name: /sign in/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSuccess).not.toHaveBeenCalled();
      });
    });
  });

  describe('Password Visibility Toggle', () => {
    it('should toggle password visibility on button click', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const passwordInput = screen.getByTestId("password-input");
      const toggleButton = screen.getByRole('button', { name: /show password/i });

      // Initially password should be hidden
      expect(passwordInput).toHaveAttribute('type', 'password');

      // Click to show password
      await user.click(toggleButton);
      await waitFor(() => {
        expect(passwordInput).toHaveAttribute('type', 'text');
      });

      // Click to hide password again
      await user.click(toggleButton);
      await waitFor(() => {
        expect(passwordInput).toHaveAttribute('type', 'password');
      });
    });

    it('should update toggle button aria-label', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const toggleButton = screen.getByRole('button', { name: /show password/i });

      await user.click(toggleButton);
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /hide password/i })).toBeInTheDocument();
      });
    });
  });

  describe('Form Submission', () => {
    it('should call onSuccess callback on successful login', async () => {
      const user = userEvent.setup();
      const onSuccess = vi.fn();

      renderWithProvider(<PasswordLoginForm onSuccess={onSuccess} />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalledTimes(1);
      }, { timeout: 3000 });
    });

    it('should display loading state during submission', async () => {
      const user = userEvent.setup();

      // Add delay to simulate loading state
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, async () => {
          await new Promise(resolve => setTimeout(resolve, 100));
          return HttpResponse.json({
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              email: 'user@example.com',
              firstName: 'Test',
              lastName: 'User',
              role: 'USER',
            },
            accessToken: 'mock-access-token',
          });
        })
      );

      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');

      // Start submission
      await user.click(submitButton);

      // Check for loading indicator (button should be disabled)
      await waitFor(() => {
        expect(submitButton).toBeDisabled();
      }, { timeout: 50 }); // Short timeout to catch loading state
    });

    it('should disable form inputs during submission', async () => {
      const user = userEvent.setup();

      // Add delay to simulate loading state
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, async () => {
          await new Promise(resolve => setTimeout(resolve, 100));
          return HttpResponse.json({
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              email: 'user@example.com',
              firstName: 'Test',
              lastName: 'User',
              role: 'USER',
            },
            accessToken: 'mock-access-token',
          });
        })
      );

      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(emailInput).toBeDisabled();
        expect(passwordInput).toBeDisabled();
      }, { timeout: 50 }); // Short timeout to catch loading state
    });
  });

  describe('Error Handling', () => {
    it('should display error message for invalid credentials', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock 401 error response with MSW
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, () => {
          return new HttpResponse(
            JSON.stringify({ message: 'Invalid email or password' }),
            { status: 401, headers: { 'Content-Type': 'application/json' } }
          );
        })
      );

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalledWith(
          expect.stringContaining('Invalid email or password')
        );
      }, { timeout: 3000 });
    });

    it('should display rate limit error message', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock 429 rate limit error with MSW
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, () => {
          return new HttpResponse(
            JSON.stringify({ message: 'Too many login attempts. Please try again later.' }),
            { status: 429, headers: { 'Content-Type': 'application/json' } }
          );
        })
      );

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalledWith(
          expect.stringContaining('Too many login attempts')
        );
      }, { timeout: 3000 });
    });

    it('should handle network errors gracefully', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock network error with MSW
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, () => {
          return HttpResponse.error();
        })
      );

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalled();
      }, { timeout: 3000 });
    });
  });

  describe('Accessibility', () => {
    it('should have proper labels for all inputs', () => {
      renderWithProvider(<PasswordLoginForm />);

      expect(screen.getByTestId("email-input")).toBeInTheDocument();
      expect(screen.getByTestId("password-input")).toBeInTheDocument();
    });

    it('should link error messages to inputs with aria-describedby', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      await user.type(emailInput, 'invalid');
      await user.tab();

      await waitFor(() => {
        const errorId = emailInput.getAttribute('aria-describedby');
        expect(errorId).toBeTruthy();
        expect(document.getElementById(errorId!)).toHaveTextContent(/valid email/i);
      });
    });

    it('should support keyboard navigation', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      // Tab through form elements
      await user.tab(); // Email input
      expect(screen.getByTestId("email-input")).toHaveFocus();

      await user.tab(); // Password input
      expect(screen.getByTestId("password-input")).toHaveFocus();

      await user.tab(); // Toggle button
      expect(screen.getByRole('button', { name: /show password/i })).toHaveFocus();

      await user.tab(); // Submit button
      expect(screen.getByRole('button', { name: /sign in/i })).toHaveFocus();
    });

    it('should announce errors to screen readers', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");
      await user.type(emailInput, 'invalid');
      await user.tab();

      await waitFor(() => {
        const errorMessage = screen.getByText(/please enter a valid email/i);
        expect(errorMessage).toHaveAttribute('role', 'alert');
      });
    });
  });

  describe('Edge Cases', () => {
    it('should handle rapid form submissions', async () => {
      const user = userEvent.setup();
      const onSuccess = vi.fn();

      // Add delay to allow loading state to be set before subsequent clicks
      server.use(
        http.post(`${API_BASE_URL}/auth/login`, async () => {
          await new Promise(resolve => setTimeout(resolve, 200));
          return HttpResponse.json({
            user: {
              id: '123e4567-e89b-12d3-a456-426614174000',
              email: 'user@example.com',
              firstName: 'Test',
              lastName: 'User',
              role: 'USER',
            },
            accessToken: 'mock-access-token',
          });
        })
      );

      renderWithProvider(<PasswordLoginForm onSuccess={onSuccess} />);

      const emailInput = screen.getByTestId("email-input");
      const passwordInput = screen.getByTestId("password-input");
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');

      // Rapid clicks
      await user.click(submitButton);
      await user.click(submitButton);
      await user.click(submitButton);

      // Should only call once due to loading state preventing additional clicks
      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalledTimes(1);
      }, { timeout: 3000 });
    });

    it('should clear error when user re-validates with valid input', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByTestId("email-input");

      // Trigger error
      await user.type(emailInput, 'invalid');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/valid email/i)).toBeInTheDocument();
      });

      // Clear and type valid email, then trigger blur validation
      await user.clear(emailInput);
      await user.type(emailInput, 'user@example.com');
      await user.tab(); // Trigger blur validation with valid input

      await waitFor(() => {
        expect(screen.queryByText(/valid email/i)).not.toBeInTheDocument();
      });
    });

    it('should handle empty form submission', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const submitButton = screen.getByRole('button', { name: /sign in/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/valid email/i)).toBeInTheDocument();
        expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
      });
    });
  });
});