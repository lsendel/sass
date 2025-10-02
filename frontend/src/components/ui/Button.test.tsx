import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { Button } from './button';

/**
 * Button Component Tests
 *
 * Best Practices Applied:
 * 1. Test user behavior, not implementation details
 * 2. Use accessible queries (getByRole, getByLabelText)
 * 3. Test visual states and interactions
 * 4. Use userEvent for realistic user interactions
 * 5. Test accessibility features
 */

describe('Button', () => {
  describe('Rendering', () => {
    it('should render button with text content', () => {
      render(<Button>Click me</Button>);

      const button = screen.getByRole('button', { name: /click me/i });
      expect(button).toBeInTheDocument();
    });

    it('should render button with custom className', () => {
      render(<Button className="custom-class">Custom Button</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('custom-class');
    });

    it('should forward ref to button element', () => {
      const ref = vi.fn();
      render(<Button ref={ref}>Ref Button</Button>);

      expect(ref).toHaveBeenCalled();
    });
  });

  describe('Variants', () => {
    it('should render default variant', () => {
      render(<Button variant="default">Default</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should render destructive variant', () => {
      render(<Button variant="destructive">Delete</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should render outline variant', () => {
      render(<Button variant="outline">Outline</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should render ghost variant', () => {
      render(<Button variant="ghost">Ghost</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('should render link variant', () => {
      render(<Button variant="link">Link</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });
  });

  describe('Sizes', () => {
    it('should render small size button', () => {
      render(<Button size="sm">Small</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('h-8');
    });

    it('should render default size button', () => {
      render(<Button size="default">Default</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('h-10');
    });

    it('should render large size button', () => {
      render(<Button size="lg">Large</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('h-12');
    });

    it('should render icon size button', () => {
      render(<Button size="icon" aria-label="Icon button">🔍</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('h-10', 'w-10');
    });
  });

  describe('User Interactions', () => {
    it('should call onClick handler when clicked', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button onClick={handleClick}>Click me</Button>);

      const button = screen.getByRole('button');
      await user.click(button);

      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('should be focusable with keyboard', async () => {
      const user = userEvent.setup();

      render(<Button>Focusable</Button>);

      const button = screen.getByRole('button');
      await user.tab();

      expect(button).toHaveFocus();
    });

    it('should trigger onClick when pressing Enter', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button onClick={handleClick}>Press Enter</Button>);

      const button = screen.getByRole('button');
      button.focus();
      await user.keyboard('{Enter}');

      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('should trigger onClick when pressing Space', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button onClick={handleClick}>Press Space</Button>);

      const button = screen.getByRole('button');
      button.focus();
      await user.keyboard(' ');

      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('Disabled State', () => {
    it('should render disabled button', () => {
      render(<Button disabled>Disabled</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
    });

    it('should not call onClick when disabled', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button disabled onClick={handleClick}>Disabled</Button>);

      const button = screen.getByRole('button');
      await user.click(button);

      expect(handleClick).not.toHaveBeenCalled();
    });

    it('should have disabled styling', () => {
      render(<Button disabled>Disabled</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('disabled:opacity-60');
    });
  });

  describe('Loading State', () => {
    it('should render loading spinner when isLoading is true', () => {
      render(<Button isLoading>Loading</Button>);

      const button = screen.getByRole('button');
      const spinner = button.querySelector('svg');

      expect(spinner).toBeInTheDocument();
      expect(spinner).toHaveClass('animate-spin');
    });

    it('should be disabled when loading', () => {
      render(<Button isLoading>Loading</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
    });

    it('should not call onClick when loading', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button isLoading onClick={handleClick}>Loading</Button>);

      const button = screen.getByRole('button');
      await user.click(button);

      expect(handleClick).not.toHaveBeenCalled();
    });

    it('should display button text along with spinner', () => {
      render(<Button isLoading>Loading...</Button>);

      expect(screen.getByText('Loading...')).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      render(<Button aria-label="Submit form">Submit</Button>);

      const button = screen.getByRole('button', { name: /submit form/i });
      expect(button).toHaveAttribute('aria-label', 'Submit form');
    });

    it('should support aria-disabled', () => {
      render(<Button aria-disabled="true">Disabled</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('aria-disabled', 'true');
    });

    it('should have focus ring for keyboard navigation', () => {
      render(<Button>Focus me</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveClass('focus:ring-2');
    });

    it('should support custom aria-describedby', () => {
      render(
        <>
          <Button aria-describedby="help-text">Action</Button>
          <div id="help-text">This performs an action</div>
        </>
      );

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('aria-describedby', 'help-text');
    });
  });

  describe('asChild Prop', () => {
    it('should render as child element when asChild is true', () => {
      render(
        <Button asChild>
          <a href="/link">Link as Button</a>
        </Button>
      );

      const link = screen.getByRole('link');
      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute('href', '/link');
    });
  });

  describe('Type Attribute', () => {
    it('should default to button type', () => {
      render(<Button>Button</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('type', 'button');
    });

    it('should support submit type', () => {
      render(<Button type="submit">Submit</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('type', 'submit');
    });

    it('should support reset type', () => {
      render(<Button type="reset">Reset</Button>);

      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('type', 'reset');
    });
  });

  describe('Edge Cases', () => {
    it('should handle both disabled and loading states', () => {
      render(<Button disabled isLoading>Disabled Loading</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
      expect(button.querySelector('svg')).toBeInTheDocument();
    });

    it('should handle empty children', () => {
      render(<Button aria-label="Empty button" />);

      const button = screen.getByRole('button', { name: /empty button/i });
      expect(button).toBeInTheDocument();
    });

    it('should handle complex children elements', () => {
      render(
        <Button>
          <span>Icon</span>
          <span>Text</span>
        </Button>
      );

      const button = screen.getByRole('button');
      expect(button).toHaveTextContent('IconText');
    });
  });

  describe('Event Handlers', () => {
    it('should handle onMouseEnter event', async () => {
      const user = userEvent.setup();
      const handleMouseEnter = vi.fn();

      render(<Button onMouseEnter={handleMouseEnter}>Hover me</Button>);

      const button = screen.getByRole('button');
      await user.hover(button);

      expect(handleMouseEnter).toHaveBeenCalledTimes(1);
    });

    it('should handle onMouseLeave event', async () => {
      const user = userEvent.setup();
      const handleMouseLeave = vi.fn();

      render(<Button onMouseLeave={handleMouseLeave}>Leave me</Button>);

      const button = screen.getByRole('button');
      await user.hover(button);
      await user.unhover(button);

      expect(handleMouseLeave).toHaveBeenCalledTimes(1);
    });

    it('should handle onFocus event', async () => {
      const user = userEvent.setup();
      const handleFocus = vi.fn();

      render(<Button onFocus={handleFocus}>Focus me</Button>);

       screen.getByRole('button');
      await user.tab();

      expect(handleFocus).toHaveBeenCalledTimes(1);
    });

    it('should handle onBlur event', async () => {
      const user = userEvent.setup();
      const handleBlur = vi.fn();

      render(
        <>
          <Button onBlur={handleBlur}>Blur me</Button>
          <Button>Other button</Button>
        </>
      );

       screen.getAllByRole('button')[0];
      await user.tab(); // Focus first button
      await user.tab(); // Focus second button (blur first)

      expect(handleBlur).toHaveBeenCalledTimes(1);
    });
  });
});