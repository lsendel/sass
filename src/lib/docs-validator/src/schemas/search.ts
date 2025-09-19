/**
 * Schema validation for SearchIndex entities and search functionality
 * Uses Zod for runtime type validation
 */

import { z } from 'zod';
import { SectionSchema, AudienceSchema } from './page';

/**
 * Search index entry schema
 */
export const SearchIndexEntrySchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty').max(200, 'Title too long'),
  content: z.string().min(1, 'Content cannot be empty'),
  excerpt: z.string().max(500, 'Excerpt too long'),
  url: z.string().url('URL must be valid'),
  section: SectionSchema,
  audience: AudienceSchema,
  tags: z.array(z.string()).default([]),
  keywords: z.array(z.string()).default([]),
  headings: z.array(z.string()).default([]),
  wordCount: z.number().int().min(0, 'Word count must be non-negative'),
  lastModified: z.string().datetime('Last modified date must be ISO 8601 format'),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
  boost: z.number().min(0, 'Boost must be non-negative').max(10, 'Boost cannot exceed 10').default(1),
  language: z.string().length(2, 'Language must be ISO 639-1 code').default('en'),
});

/**
 * Search query schema
 */
export const SearchQuerySchema = z.object({
  q: z.string().min(1, 'Query cannot be empty').max(500, 'Query too long'),
  section: SectionSchema.optional(),
  audience: AudienceSchema.optional(),
  tags: z.array(z.string()).optional(),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)').optional(),
  limit: z.number().int().min(1, 'Limit must be at least 1').max(100, 'Limit cannot exceed 100').default(20),
  offset: z.number().int().min(0, 'Offset must be non-negative').default(0),
  fuzzy: z.boolean().default(false),
  highlight: z.boolean().default(true),
  sort: z.enum(['relevance', 'date', 'title', 'section']).default('relevance'),
  order: z.enum(['asc', 'desc']).default('desc'),
});

/**
 * Search filter schema
 */
export const SearchFilterSchema = z.object({
  sections: z.array(SectionSchema).optional(),
  audiences: z.array(AudienceSchema).optional(),
  tags: z.array(z.string()).optional(),
  versions: z.array(z.string().regex(/^\d+\.\d+\.\d+$/)).optional(),
  dateRange: z.object({
    from: z.string().datetime().optional(),
    to: z.string().datetime().optional(),
  }).optional(),
  wordCountRange: z.object({
    min: z.number().int().min(0).optional(),
    max: z.number().int().min(0).optional(),
  }).optional(),
  readingTimeRange: z.object({
    min: z.number().int().min(0).optional(),
    max: z.number().int().min(0).optional(),
  }).optional(),
});

/**
 * Search result with highlighting schema
 */
export const SearchResultWithHighlightSchema = z.object({
  id: z.string().uuid('ID must be a valid UUID'),
  title: z.string().min(1, 'Title cannot be empty'),
  section: SectionSchema,
  excerpt: z.string().max(500, 'Excerpt too long'),
  score: z.number().min(0, 'Score must be non-negative').max(1, 'Score cannot exceed 1'),
  url: z.string().url('URL must be valid'),
  audience: AudienceSchema,
  highlights: z.object({
    title: z.array(z.string()).optional(),
    content: z.array(z.string()).optional(),
    headings: z.array(z.string()).optional(),
  }).optional(),
  matchedTerms: z.array(z.string()).default([]),
  matchCount: z.number().int().min(0, 'Match count must be non-negative'),
});

/**
 * Advanced search response schema
 */
export const AdvancedSearchResponseSchema = z.object({
  results: z.array(SearchResultWithHighlightSchema),
  totalCount: z.number().int().min(0, 'Total count must be non-negative'),
  searchTime: z.number().min(0, 'Search time must be non-negative'),
  query: z.string().min(1, 'Query cannot be empty'),
  filters: SearchFilterSchema.optional(),
  pagination: z.object({
    currentPage: z.number().int().min(1, 'Current page must be at least 1'),
    totalPages: z.number().int().min(0, 'Total pages must be non-negative'),
    limit: z.number().int().min(1, 'Limit must be at least 1'),
    offset: z.number().int().min(0, 'Offset must be non-negative'),
  }),
  facets: z.object({
    sections: z.array(z.object({
      section: SectionSchema,
      count: z.number().int().min(0),
    })).optional(),
    audiences: z.array(z.object({
      audience: AudienceSchema,
      count: z.number().int().min(0),
    })).optional(),
    tags: z.array(z.object({
      tag: z.string(),
      count: z.number().int().min(0),
    })).optional(),
  }).optional(),
  suggestions: z.array(z.string()).optional(),
});

/**
 * Search analytics schema
 */
export const SearchAnalyticsSchema = z.object({
  query: z.string().min(1, 'Query cannot be empty'),
  timestamp: z.string().datetime('Timestamp must be ISO 8601 format'),
  userId: z.string().optional(),
  sessionId: z.string().optional(),
  resultCount: z.number().int().min(0, 'Result count must be non-negative'),
  searchTime: z.number().min(0, 'Search time must be non-negative'),
  clickedResults: z.array(z.object({
    resultId: z.string().uuid(),
    position: z.number().int().min(1),
    clickTime: z.string().datetime(),
  })).default([]),
  filters: SearchFilterSchema.optional(),
  userAgent: z.string().optional(),
  referer: z.string().url().optional(),
});

/**
 * Search index configuration schema
 */
export const SearchIndexConfigSchema = z.object({
  name: z.string().min(1, 'Index name cannot be empty'),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
  language: z.string().length(2, 'Language must be ISO 639-1 code').default('en'),
  fields: z.object({
    title: z.object({
      boost: z.number().min(0).max(10).default(3),
      analyzer: z.string().default('standard'),
    }),
    content: z.object({
      boost: z.number().min(0).max(10).default(1),
      analyzer: z.string().default('standard'),
    }),
    headings: z.object({
      boost: z.number().min(0).max(10).default(2),
      analyzer: z.string().default('standard'),
    }),
    tags: z.object({
      boost: z.number().min(0).max(10).default(1.5),
      analyzer: z.string().default('keyword'),
    }),
  }),
  settings: z.object({
    maxResults: z.number().int().min(1).max(1000).default(100),
    fuzzyThreshold: z.number().min(0).max(1).default(0.8),
    highlightLength: z.number().int().min(50).max(500).default(200),
    excerptLength: z.number().int().min(100).max(1000).default(300),
  }),
  lastUpdated: z.string().datetime('Last updated date must be ISO 8601 format'),
  documentCount: z.number().int().min(0, 'Document count must be non-negative'),
});

/**
 * Validation functions for runtime type checking
 */

export const validateSearchIndexEntry = (data: unknown) => {
  return SearchIndexEntrySchema.safeParse(data);
};

export const validateSearchQuery = (data: unknown) => {
  return SearchQuerySchema.safeParse(data);
};

export const validateSearchFilter = (data: unknown) => {
  return SearchFilterSchema.safeParse(data);
};

export const validateAdvancedSearchResponse = (data: unknown) => {
  return AdvancedSearchResponseSchema.safeParse(data);
};

export const validateSearchAnalytics = (data: unknown) => {
  return SearchAnalyticsSchema.safeParse(data);
};

export const validateSearchIndexConfig = (data: unknown) => {
  return SearchIndexConfigSchema.safeParse(data);
};

/**
 * Helper functions for search validation
 */

export const sanitizeSearchQuery = (query: string): string => {
  return query
    .trim()
    .replace(/[<>\"'&]/g, '') // Remove potentially dangerous characters
    .replace(/\s+/g, ' ') // Normalize whitespace
    .substring(0, 500); // Limit length
};

export const validateSearchTerm = (term: string): boolean => {
  return term.length >= 1 && term.length <= 100 && !/[<>\"'&]/.test(term);
};

export const calculateSearchScore = (
  titleMatches: number,
  contentMatches: number,
  headingMatches: number,
  boost: number = 1
): number => {
  const score = (titleMatches * 3 + headingMatches * 2 + contentMatches) * boost;
  return Math.min(1, score / 100); // Normalize to 0-1
};

export const extractSearchKeywords = (content: string): string[] => {
  return content
    .toLowerCase()
    .replace(/[^\w\s]/g, ' ')
    .split(/\s+/)
    .filter(word => word.length > 2 && word.length < 50)
    .slice(0, 100); // Limit to 100 keywords
};

/**
 * Type exports for use in other modules
 */
export type SearchIndexEntry = z.infer<typeof SearchIndexEntrySchema>;
export type SearchQuery = z.infer<typeof SearchQuerySchema>;
export type SearchFilter = z.infer<typeof SearchFilterSchema>;
export type SearchResultWithHighlight = z.infer<typeof SearchResultWithHighlightSchema>;
export type AdvancedSearchResponse = z.infer<typeof AdvancedSearchResponseSchema>;
export type SearchAnalytics = z.infer<typeof SearchAnalyticsSchema>;
export type SearchIndexConfig = z.infer<typeof SearchIndexConfigSchema>;