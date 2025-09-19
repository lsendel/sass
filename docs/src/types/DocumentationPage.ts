/**
 * Type definitions for documentation pages and related entities
 */

// Core documentation page status
export type PageStatus = 'draft' | 'review' | 'published' | 'deprecated' | 'archived';

// Target audience for documentation
export type Audience = 'developer' | 'operator' | 'stakeholder' | 'all';

// Documentation sections
export type Section = 'guides' | 'architecture' | 'backend' | 'frontend';

// Template types
export type TemplateName = 'feature' | 'api' | 'adr';

/**
 * Front matter metadata for documentation pages
 */
export interface FrontMatter {
  author?: string;
  audience?: Audience;
  version?: string;
  tags?: string[];
  lastModified?: string;
  reviewers?: string[];
  [key: string]: any; // Allow additional custom fields
}

/**
 * Full documentation page entity
 */
export interface DocumentationPage {
  id: string;
  title: string;
  content: string;
  frontMatter: FrontMatter;
  filePath: string;
  status: PageStatus;
  section: Section;
  audience: Audience;
  version: string;
  createdAt: string;
  updatedAt: string;
  wordCount: number;
  readingTime: number; // in minutes
}

/**
 * Summary version of documentation page for listings
 */
export interface DocumentationPageSummary {
  id: string;
  title: string;
  status: PageStatus;
  section: Section;
  audience: Audience;
  version: string;
  lastModified: string;
  readingTime: number;
}

/**
 * Request to create a new documentation page
 */
export interface CreatePageRequest {
  title: string;
  templateName: TemplateName;
  section: Section;
  initialContent?: string;
  frontMatter?: Partial<FrontMatter>;
}

/**
 * Documentation section structure for navigation
 */
export interface DocumentationSection {
  id: string;
  name: string;
  path: string;
  order: number;
  children?: DocumentationPageSummary[];
}

/**
 * Search result for documentation content
 */
export interface SearchResult {
  id: string;
  title: string;
  section: Section;
  excerpt: string;
  score: number; // 0-1 relevance score
  url: string;
  audience: Audience;
  highlights?: string[]; // Highlighted text snippets
}

/**
 * Search response structure
 */
export interface SearchResponse {
  results: SearchResult[];
  totalCount: number;
  searchTime: number; // in milliseconds
  query: string;
}

/**
 * Paginated response for documentation pages
 */
export interface PagedResponse<T> {
  pages: T[];
  totalCount: number;
  currentPage: number;
  totalPages: number;
}

/**
 * Documentation template structure
 */
export interface DocumentationTemplate {
  name: TemplateName;
  displayName: string;
  description: string;
  content: string;
  frontMatterSchema: Record<string, any>;
  requiredFields: string[];
  version: string;
}

/**
 * Validation result for documentation content
 */
export interface ValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

/**
 * Validation error details
 */
export interface ValidationError {
  type: 'syntax' | 'schema' | 'link' | 'template' | 'completeness';
  severity: 'error';
  message: string;
  line?: number;
  column?: number;
  field?: string;
}

/**
 * Validation warning details
 */
export interface ValidationWarning {
  type: 'syntax' | 'schema' | 'link' | 'template' | 'completeness';
  severity: 'warning';
  message: string;
  line?: number;
  column?: number;
  field?: string;
}