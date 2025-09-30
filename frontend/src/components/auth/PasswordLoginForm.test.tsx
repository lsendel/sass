import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import PasswordLoginForm from './PasswordLoginForm';
import { authApi } from '../../store/api/authApi';
import authReducer from '../../store/slices/authSlice';

/**
 * PasswordLoginForm Component Tests
 *
 * Best Practices Applied:
 * 1. Mock API calls with MSW or Redux store mocks
 * 2. Test form validation with realistic user input
 * 3. Test async operations with waitFor
 * 4. Test error handling and user feedback
 * 5. Test accessibility and keyboard navigation
 * 6. Use user-event for realistic interactions
 */

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
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render login form with all fields', () => {
      renderWithProvider(<PasswordLoginForm />);

      expect(screen.getByRole('heading', { name: /sign in to your account/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    it('should render email input field', () => {
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
      expect(emailInput).toBeInTheDocument();
      expect(emailInput).toHaveAttribute('type', 'email');
    });

    it('should render password input field', () => {
      renderWithProvider(<PasswordLoginForm />);

      const passwordInput = screen.getByLabelText(/password/i);
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

      const emailInput = screen.getByLabelText(/email/i);
      await user.type(emailInput, 'invalid-email');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
      });
    });

    it('should show error for short password', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, 'short');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
      });
    });

    it('should accept valid email and password', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);

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

      const passwordInput = screen.getByLabelText(/password/i);
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

      // Mock successful API response
      const mockStore = createMockStore();
      vi.spyOn(authApi.endpoints.passwordLogin, 'useMutation').mockReturnValue([
        vi.fn().mockResolvedValue({
          unwrap: () => Promise.resolve({
            user: { id: '1', email: 'user@example.com', name: 'Test User' },
          }),
        }),
        { isLoading: false },
      ] as any);

      renderWithProvider(<PasswordLoginForm onSuccess={onSuccess} />, {
        store: mockStore,
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalledTimes(1);
      });
    });

    it('should display loading state during submission', async () => {
      const user = userEvent.setup();

      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');

      // Start submission
      await user.click(submitButton);

      // Check for loading indicator (button should be disabled)
      await waitFor(() => {
        expect(submitButton).toBeDisabled();
      });
    });

    it('should disable form inputs during submission', async () => {
      const user = userEvent.setup();

      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(emailInput).toBeDisabled();
        expect(passwordInput).toBeDisabled();
      });
    });
  });

  describe('Error Handling', () => {
    it('should display error message for invalid credentials', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock failed API response
      vi.spyOn(authApi.endpoints.passwordLogin, 'useMutation').mockReturnValue([
        vi.fn().mockRejectedValue({
          unwrap: () => Promise.reject({
            status: 401,
            data: { message: 'Invalid email or password' },
          }),
        }),
        { isLoading: false },
      ] as any);

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
      });
    });

    it('should display rate limit error message', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock rate limit error
      vi.spyOn(authApi.endpoints.passwordLogin, 'useMutation').mockReturnValue([
        vi.fn().mockRejectedValue({
          unwrap: () => Promise.reject({
            status: 429,
            data: { message: 'Too many requests' },
          }),
        }),
        { isLoading: false },
      ] as any);

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalledWith(
          expect.stringContaining('Too many login attempts')
        );
      });
    });

    it('should handle network errors gracefully', async () => {
      const user = userEvent.setup();
      const onError = vi.fn();

      // Mock network error
      vi.spyOn(authApi.endpoints.passwordLogin, 'useMutation').mockReturnValue([
        vi.fn().mockRejectedValue({
          unwrap: () => Promise.reject({
            name: 'NetworkError',
            message: 'Network request failed',
          }),
        }),
        { isLoading: false },
      ] as any);

      renderWithProvider(<PasswordLoginForm onError={onError} />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(onError).toHaveBeenCalled();
      });
    });
  });

  describe('Accessibility', () => {
    it('should have proper labels for all inputs', () => {
      renderWithProvider(<PasswordLoginForm />);

      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    });

    it('should link error messages to inputs with aria-describedby', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
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
      expect(screen.getByLabelText(/email/i)).toHaveFocus();

      await user.tab(); // Password input
      expect(screen.getByLabelText(/password/i)).toHaveFocus();

      await user.tab(); // Toggle button
      expect(screen.getByRole('button', { name: /show password/i })).toHaveFocus();

      await user.tab(); // Submit button
      expect(screen.getByRole('button', { name: /sign in/i })).toHaveFocus();
    });

    it('should announce errors to screen readers', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);
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

      renderWithProvider(<PasswordLoginForm onSuccess={onSuccess} />);

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'user@example.com');
      await user.type(passwordInput, 'password123');

      // Rapid clicks
      await user.click(submitButton);
      await user.click(submitButton);
      await user.click(submitButton);

      // Should only call once due to loading state
      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalledTimes(1);
      });
    });

    it('should clear error when user starts typing', async () => {
      const user = userEvent.setup();
      renderWithProvider(<PasswordLoginForm />);

      const emailInput = screen.getByLabelText(/email/i);

      // Trigger error
      await user.type(emailInput, 'invalid');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/valid email/i)).toBeInTheDocument();
      });

      // Start typing again
      await user.clear(emailInput);
      await user.type(emailInput, 'user@example.com');

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