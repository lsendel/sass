import type { Meta, StoryObj } from '@storybook/react';
import LoadingSpinner from './LoadingSpinner';

const meta = {
  title: 'UI/LoadingSpinner',
  component: LoadingSpinner,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: { type: 'select' },
      options: ['sm', 'md', 'lg'],
    },
    color: {
      control: { type: 'select' },
      options: ['primary', 'white', 'gray'],
    },
  },
} satisfies Meta<typeof LoadingSpinner>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};

export const Small: Story = {
  args: {
    size: 'sm',
  },
};

export const Medium: Story = {
  args: {
    size: 'md',
  },
};

export const Large: Story = {
  args: {
    size: 'lg',
  },
};

export const Primary: Story = {
  args: {
    color: 'primary',
    size: 'lg',
  },
};

export const White: Story = {
  args: {
    color: 'white',
    size: 'lg',
  },
  parameters: {
    backgrounds: {
      default: 'dark',
      values: [
        { name: 'dark', value: '#1f2937' },
      ],
    },
  },
};

export const Gray: Story = {
  args: {
    color: 'gray',
    size: 'lg',
  },
};

export const AllSizes: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <div className="text-center">
        <LoadingSpinner size="sm" />
        <p className="mt-2 text-sm text-gray-600">Small</p>
      </div>
      <div className="text-center">
        <LoadingSpinner size="md" />
        <p className="mt-2 text-sm text-gray-600">Medium</p>
      </div>
      <div className="text-center">
        <LoadingSpinner size="lg" />
        <p className="mt-2 text-sm text-gray-600">Large</p>
      </div>
    </div>
  ),
};

export const AllColors: Story = {
  render: () => (
    <div className="flex items-center gap-4">
      <div className="text-center">
        <LoadingSpinner color="primary" size="lg" />
        <p className="mt-2 text-sm text-gray-600">Primary</p>
      </div>
      <div className="text-center p-4 bg-gray-800 rounded">
        <LoadingSpinner color="white" size="lg" />
        <p className="mt-2 text-sm text-white">White</p>
      </div>
      <div className="text-center">
        <LoadingSpinner color="gray" size="lg" />
        <p className="mt-2 text-sm text-gray-600">Gray</p>
      </div>
    </div>
  ),
};

export const InButton: Story = {
  render: () => (
    <div className="flex gap-4">
      <button className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed" disabled>
        <LoadingSpinner size="sm" color="white" className="mr-2" />
        Loading...
      </button>
      <button className="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
        <LoadingSpinner size="sm" color="white" className="mr-2" />
        Processing
      </button>
    </div>
  ),
};