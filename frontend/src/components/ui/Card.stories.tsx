import type { Meta, StoryObj } from '@storybook/react';
import { Card, CardHeader, CardFooter, CardTitle, CardDescription, CardContent } from './card';
import { Button } from './button';

const meta = {
  title: 'UI/Card',
  component: Card,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: { type: 'select' },
      options: ['default', 'glass', 'glass-subtle'],
    },
  },
} satisfies Meta<typeof Card>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    className: 'w-96',
    children: (
      <>
        <CardHeader>
          <CardTitle>Card Title</CardTitle>
          <CardDescription>This is a description of the card content.</CardDescription>
        </CardHeader>
        <CardContent>
          <p>Card content goes here. This could be any type of content.</p>
        </CardContent>
        <CardFooter>
          <Button>Action</Button>
        </CardFooter>
      </>
    ),
  },
};

export const Glass: Story = {
  args: {
    variant: 'glass',
    className: 'w-96',
    children: (
      <>
        <CardHeader>
          <CardTitle>Glass Card</CardTitle>
          <CardDescription>A card with glass morphism effect.</CardDescription>
        </CardHeader>
        <CardContent>
          <p>This card has a frosted glass appearance with backdrop blur.</p>
        </CardContent>
        <CardFooter>
          <Button variant="accent">Try it</Button>
        </CardFooter>
      </>
    ),
  },
  parameters: {
    backgrounds: {
      default: 'gradient',
      values: [
        { name: 'gradient', value: 'linear-gradient(45deg, #667eea 0%, #764ba2 100%)' },
      ],
    },
  },
};

export const GlassSubtle: Story = {
  args: {
    variant: 'glass-subtle',
    className: 'w-96',
    children: (
      <>
        <CardHeader>
          <CardTitle>Subtle Glass Card</CardTitle>
          <CardDescription>A card with subtle glass morphism effect.</CardDescription>
        </CardHeader>
        <CardContent>
          <p>This card has a more subtle frosted glass appearance.</p>
        </CardContent>
        <CardFooter>
          <Button variant="outline">Learn more</Button>
        </CardFooter>
      </>
    ),
  },
  parameters: {
    backgrounds: {
      default: 'gradient',
      values: [
        { name: 'gradient', value: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
      ],
    },
  },
};

export const SimpleContent: Story = {
  render: () => (
    <Card className="w-80">
      <CardHeader>
        <CardTitle>Simple Card</CardTitle>
      </CardHeader>
      <CardContent>
        <p>Just some simple content without a footer.</p>
      </CardContent>
    </Card>
  ),
};

export const WithMultipleActions: Story = {
  render: () => (
    <Card className="w-96">
      <CardHeader>
        <CardTitle>Payment Method</CardTitle>
        <CardDescription>Manage your payment information</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          <p className="text-sm text-gray-600">**** **** **** 1234</p>
          <p className="text-sm text-gray-600">Expires 12/25</p>
        </div>
      </CardContent>
      <CardFooter className="gap-2">
        <Button variant="outline" size="sm">Edit</Button>
        <Button variant="destructive" size="sm">Remove</Button>
      </CardFooter>
    </Card>
  ),
};

export const AllVariants: Story = {
  render: () => (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 p-8"
         style={{
           background: 'linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%)'
         }}>
      <Card variant="default" className="w-72">
        <CardHeader>
          <CardTitle>Default Card</CardTitle>
          <CardDescription>Standard card appearance</CardDescription>
        </CardHeader>
        <CardContent>
          <p>This is the default card style with a solid background.</p>
        </CardContent>
      </Card>

      <Card variant="glass" className="w-72">
        <CardHeader>
          <CardTitle>Glass Card</CardTitle>
          <CardDescription>Frosted glass effect</CardDescription>
        </CardHeader>
        <CardContent>
          <p>This card features a strong glass morphism effect.</p>
        </CardContent>
      </Card>

      <Card variant="glass-subtle" className="w-72">
        <CardHeader>
          <CardTitle>Subtle Glass</CardTitle>
          <CardDescription>Light glass effect</CardDescription>
        </CardHeader>
        <CardContent>
          <p>This card has a more subtle glass appearance.</p>
        </CardContent>
      </Card>
    </div>
  ),
  parameters: {
    layout: 'fullscreen',
  },
};