/**
 * Schema validation for DocumentationSection entities
 * Uses Zod for runtime type validation
 */

import { z } from 'zod';
import { SectionSchema, DocumentationPageSummarySchema } from './page';

/**
 * Navigation item schema for section children
 */
export const NavigationItemSchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty').max(200, 'Title too long'),
  path: z.string().regex(/^\/docs\/.*/, 'Path must start with /docs/'),
  order: z.number().int().min(0, 'Order must be non-negative'),
  section: SectionSchema,
  audience: z.enum(['developer', 'operator', 'stakeholder', 'all']),
  status: z.enum(['draft', 'review', 'published', 'deprecated', 'archived']),
  readingTime: z.number().int().min(0, 'Reading time must be non-negative'),
  lastModified: z.string().datetime('Last modified date must be ISO 8601 format'),
  hasChildren: z.boolean().optional(),
  level: z.number().int().min(1).max(6, 'Level must be between 1 and 6'),
});

/**
 * Full documentation section schema
 */
export const DocumentationSectionSchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  name: z.string().min(1, 'Section name cannot be empty').max(100, 'Section name too long'),
  path: z.string().regex(/^\/docs\/[a-z-]+$/, 'Path must be valid section path'),
  order: z.number().int().min(0, 'Order must be non-negative'),
  description: z.string().max(500, 'Description too long').optional(),
  icon: z.string().optional(),
  children: z.array(NavigationItemSchema).optional(),
  pageCount: z.number().int().min(0, 'Page count must be non-negative').optional(),
  lastUpdated: z.string().datetime('Last updated date must be ISO 8601 format').optional(),
  isVisible: z.boolean().default(true),
});

/**
 * Section summary schema (for navigation menus)
 */
export const SectionSummarySchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  name: z.string().min(1, 'Section name cannot be empty'),
  path: z.string().regex(/^\/docs\/[a-z-]+$/, 'Path must be valid section path'),
  order: z.number().int().min(0, 'Order must be non-negative'),
  icon: z.string().optional(),
  pageCount: z.number().int().min(0, 'Page count must be non-negative'),
  isVisible: z.boolean(),
});

/**
 * Section navigation tree schema
 */
export const NavigationTreeSchema = z.object({
  sections: z.array(DocumentationSectionSchema),
  totalSections: z.number().int().min(0, 'Total sections must be non-negative'),
  totalPages: z.number().int().min(0, 'Total pages must be non-negative'),
  lastUpdated: z.string().datetime('Last updated date must be ISO 8601 format'),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
});

/**
 * Section statistics schema
 */
export const SectionStatsSchema = z.object({
  sectionId: z.string().uuid('Section ID must be a valid UUID'),
  totalPages: z.number().int().min(0, 'Total pages must be non-negative'),
  publishedPages: z.number().int().min(0, 'Published pages must be non-negative'),
  draftPages: z.number().int().min(0, 'Draft pages must be non-negative'),
  averageReadingTime: z.number().min(0, 'Average reading time must be non-negative'),
  lastUpdated: z.string().datetime('Last updated date must be ISO 8601 format'),
  contributors: z.array(z.string()).optional(),
  completionRate: z.number().min(0, 'Completion rate must be non-negative').max(1, 'Completion rate cannot exceed 1'),
});

/**
 * Section creation request schema
 */
export const CreateSectionRequestSchema = z.object({
  name: z.string().min(1, 'Section name cannot be empty').max(100, 'Section name too long'),
  path: z.string().regex(/^[a-z-]+$/, 'Path must contain only lowercase letters and hyphens'),
  description: z.string().max(500, 'Description too long').optional(),
  icon: z.string().optional(),
  order: z.number().int().min(0, 'Order must be non-negative').optional(),
  isVisible: z.boolean().default(true),
});

/**
 * Section update request schema
 */
export const UpdateSectionRequestSchema = z.object({
  name: z.string().min(1, 'Section name cannot be empty').max(100, 'Section name too long').optional(),
  description: z.string().max(500, 'Description too long').optional(),
  icon: z.string().optional(),
  order: z.number().int().min(0, 'Order must be non-negative').optional(),
  isVisible: z.boolean().optional(),
});

/**
 * Breadcrumb item schema for navigation
 */
export const BreadcrumbItemSchema = z.object({
  title: z.string().min(1, 'Title cannot be empty'),
  path: z.string().regex(/^\/docs\/.*/, 'Path must start with /docs/'),
  isSection: z.boolean(),
});

/**
 * Breadcrumb trail schema
 */
export const BreadcrumbTrailSchema = z.object({
  items: z.array(BreadcrumbItemSchema),
  currentPage: z.string().min(1, 'Current page cannot be empty'),
});

/**
 * Validation functions for runtime type checking
 */

export const validateDocumentationSection = (data: unknown) => {
  return DocumentationSectionSchema.safeParse(data);
};

export const validateNavigationItem = (data: unknown) => {
  return NavigationItemSchema.safeParse(data);
};

export const validateSectionSummary = (data: unknown) => {
  return SectionSummarySchema.safeParse(data);
};

export const validateNavigationTree = (data: unknown) => {
  return NavigationTreeSchema.safeParse(data);
};

export const validateSectionStats = (data: unknown) => {
  return SectionStatsSchema.safeParse(data);
};

export const validateCreateSectionRequest = (data: unknown) => {
  return CreateSectionRequestSchema.safeParse(data);
};

export const validateUpdateSectionRequest = (data: unknown) => {
  return UpdateSectionRequestSchema.safeParse(data);
};

export const validateBreadcrumbTrail = (data: unknown) => {
  return BreadcrumbTrailSchema.safeParse(data);
};

/**
 * Helper functions for section validation
 */

export const validateSectionPath = (path: string): boolean => {
  return /^[a-z-]+$/.test(path) && path.length > 0 && path.length <= 50;
};

export const validateSectionOrder = (sections: any[]): boolean => {
  const orders = sections.map(s => s.order).sort((a, b) => a - b);
  return orders.every((order, index) => index === 0 || order >= orders[index - 1]);
};

export const generateSectionPath = (name: string): string => {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '') // Remove special characters
    .replace(/\s+/g, '-') // Replace spaces with hyphens
    .replace(/-+/g, '-') // Replace multiple hyphens with single
    .replace(/^-|-$/g, ''); // Remove leading/trailing hyphens
};

/**
 * Type exports for use in other modules
 */
export type DocumentationSection = z.infer<typeof DocumentationSectionSchema>;
export type NavigationItem = z.infer<typeof NavigationItemSchema>;
export type SectionSummary = z.infer<typeof SectionSummarySchema>;
export type NavigationTree = z.infer<typeof NavigationTreeSchema>;
export type SectionStats = z.infer<typeof SectionStatsSchema>;
export type CreateSectionRequest = z.infer<typeof CreateSectionRequestSchema>;
export type UpdateSectionRequest = z.infer<typeof UpdateSectionRequestSchema>;
export type BreadcrumbItem = z.infer<typeof BreadcrumbItemSchema>;
export type BreadcrumbTrail = z.infer<typeof BreadcrumbTrailSchema>;