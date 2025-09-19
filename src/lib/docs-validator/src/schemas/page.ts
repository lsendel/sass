/**
 * Schema validation for DocumentationPage entities
 * Uses Zod for runtime type validation
 */

import { z } from 'zod';

// Base enums matching TypeScript types
export const PageStatusSchema = z.enum(['draft', 'review', 'published', 'deprecated', 'archived']);
export const AudienceSchema = z.enum(['developer', 'operator', 'stakeholder', 'all']);
export const SectionSchema = z.enum(['guides', 'architecture', 'backend', 'frontend']);
export const TemplateNameSchema = z.enum(['feature', 'api', 'adr']);

// Front matter schema
export const FrontMatterSchema = z.object({
  author: z.string().optional(),
  audience: AudienceSchema.optional(),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)').optional(),
  tags: z.array(z.string()).optional(),
  lastModified: z.string().datetime().optional(),
  reviewers: z.array(z.string()).optional(),
}).passthrough(); // Allow additional properties

// Full documentation page schema
export const DocumentationPageSchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty').max(200, 'Title too long'),
  content: z.string().min(1, 'Content cannot be empty'),
  frontMatter: FrontMatterSchema,
  filePath: z.string().regex(/^docs\/.*\.md$/, 'File path must be in docs/ directory and end with .md'),
  status: PageStatusSchema,
  section: SectionSchema,
  audience: AudienceSchema,
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
  createdAt: z.string().datetime('Created date must be ISO 8601 format'),
  updatedAt: z.string().datetime('Updated date must be ISO 8601 format'),
  wordCount: z.number().int().min(0, 'Word count must be non-negative'),
  readingTime: z.number().int().min(0, 'Reading time must be non-negative'),
});

// Page summary schema (for listings)
export const DocumentationPageSummarySchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty').max(200, 'Title too long'),
  status: PageStatusSchema,
  section: SectionSchema,
  audience: AudienceSchema,
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
  lastModified: z.string().datetime('Last modified date must be ISO 8601 format'),
  readingTime: z.number().int().min(0, 'Reading time must be non-negative'),
});

// Create page request schema
export const CreatePageRequestSchema = z.object({
  title: z.string().min(1, 'Title cannot be empty').max(200, 'Title too long'),
  templateName: TemplateNameSchema,
  section: SectionSchema,
  initialContent: z.string().optional(),
  frontMatter: FrontMatterSchema.partial().optional(),
});

// Search result schema
export const SearchResultSchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty'),
  section: SectionSchema,
  excerpt: z.string().max(500, 'Excerpt too long'),
  score: z.number().min(0, 'Score must be non-negative').max(1, 'Score cannot exceed 1'),
  url: z.string().url('URL must be valid'),
  audience: AudienceSchema,
  highlights: z.array(z.string()).optional(),
});

// Search response schema
export const SearchResponseSchema = z.object({
  results: z.array(SearchResultSchema),
  totalCount: z.number().int().min(0, 'Total count must be non-negative'),
  searchTime: z.number().min(0, 'Search time must be non-negative'),
  query: z.string().min(1, 'Query cannot be empty'),
});

// Paginated response schema
export const PagedResponseSchema = <T extends z.ZodTypeAny>(itemSchema: T) =>
  z.object({
    pages: z.array(itemSchema),
    totalCount: z.number().int().min(0, 'Total count must be non-negative'),
    currentPage: z.number().int().min(1, 'Current page must be at least 1'),
    totalPages: z.number().int().min(0, 'Total pages must be non-negative'),
  });

// Validation result schemas
export const ValidationErrorSchema = z.object({
  type: z.enum(['syntax', 'schema', 'link', 'template', 'completeness']),
  severity: z.literal('error'),
  message: z.string().min(1, 'Error message cannot be empty'),
  line: z.number().int().min(1).optional(),
  column: z.number().int().min(1).optional(),
  field: z.string().optional(),
});

export const ValidationWarningSchema = z.object({
  type: z.enum(['syntax', 'schema', 'link', 'template', 'completeness']),
  severity: z.literal('warning'),
  message: z.string().min(1, 'Warning message cannot be empty'),
  line: z.number().int().min(1).optional(),
  column: z.number().int().min(1).optional(),
  field: z.string().optional(),
});

export const ValidationResultSchema = z.object({
  isValid: z.boolean(),
  errors: z.array(ValidationErrorSchema),
  warnings: z.array(ValidationWarningSchema),
});

/**
 * Validation functions for runtime type checking
 */

export const validateDocumentationPage = (data: unknown) => {
  return DocumentationPageSchema.safeParse(data);
};

export const validatePageSummary = (data: unknown) => {
  return DocumentationPageSummarySchema.safeParse(data);
};

export const validateCreatePageRequest = (data: unknown) => {
  return CreatePageRequestSchema.safeParse(data);
};

export const validateSearchResult = (data: unknown) => {
  return SearchResultSchema.safeParse(data);
};

export const validateSearchResponse = (data: unknown) => {
  return SearchResponseSchema.safeParse(data);
};

export const validatePagedResponse = <T extends z.ZodTypeAny>(itemSchema: T) => (data: unknown) => {
  return PagedResponseSchema(itemSchema).safeParse(data);
};

/**
 * Helper function to extract validation errors in a readable format
 */
export const formatValidationErrors = (result: z.SafeParseError<any>) => {
  return result.error.issues.map(issue => ({
    field: issue.path.join('.'),
    message: issue.message,
    code: issue.code,
  }));
};

/**
 * Type exports for use in other modules
 */
export type DocumentationPage = z.infer<typeof DocumentationPageSchema>;
export type DocumentationPageSummary = z.infer<typeof DocumentationPageSummarySchema>;
export type CreatePageRequest = z.infer<typeof CreatePageRequestSchema>;
export type SearchResult = z.infer<typeof SearchResultSchema>;
export type SearchResponse = z.infer<typeof SearchResponseSchema>;
export type ValidationError = z.infer<typeof ValidationErrorSchema>;
export type ValidationWarning = z.infer<typeof ValidationWarningSchema>;
export type ValidationResult = z.infer<typeof ValidationResultSchema>;