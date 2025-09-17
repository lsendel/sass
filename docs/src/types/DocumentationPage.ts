// Documentation entity types based on data model

export interface DocumentationPageSummary {
  id: string;
  title: string;
  status: 'draft' | 'review' | 'published' | 'deprecated' | 'archived';
  lastUpdated?: string;
  author?: string;
  section?: string;
  audience?: 'developer' | 'operator' | 'stakeholder' | 'all';
  version?: string;
}

export interface DocumentationPage extends DocumentationPageSummary {
  content: string;
  frontMatter: {
    description?: string;
    keywords?: string[];
    lastUpdated?: string;
    author?: string;
    version?: string;
    audience?: 'developer' | 'operator' | 'stakeholder' | 'all';
    status?: 'draft' | 'review' | 'published' | 'deprecated' | 'archived';
  };
  filePath: string;
  parentSection?: string;
  childPages?: string[];
  crossReferences?: string[];
}

export interface CreatePageRequest {
  title: string;
  templateName: string;
  section: string;
  initialContent?: string;
  frontMatter?: Record<string, any>;
}

export interface UpdatePageRequest {
  title?: string;
  content?: string;
  frontMatter?: Record<string, any>;
  version?: string; // for optimistic locking
}

export interface ValidationRequest {
  content: string;
  templateName: string;
  frontMatter?: Record<string, any>;
}

export interface ValidationResponse {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

export interface ValidationError {
  type: 'syntax' | 'schema' | 'link' | 'template' | 'completeness';
  message: string;
  line?: number;
  column?: number;
}

export interface ValidationWarning {
  type: string;
  message: string;
  line?: number;
}