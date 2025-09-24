/**
 * Schema validation for DocumentationTemplate entities and template processing
 * Uses Zod for runtime type validation
 */

import { z } from 'zod';
import { FrontMatterSchema, SectionSchema, AudienceSchema } from './page';

/**
 * Template name schema for valid template types
 */
export const TemplateNameSchema = z.enum(['feature', 'api', 'adr'], {
  errorMap: () => ({ message: 'Template name must be one of: feature, api, adr' })
});

/**
 * Template variable schema for dynamic content
 */
export const TemplateVariableSchema = z.object({
  name: z.string().min(1, 'Variable name cannot be empty').max(50, 'Variable name too long'),
  type: z.enum(['string', 'text', 'select', 'multiselect', 'boolean', 'date']),
  label: z.string().min(1, 'Variable label cannot be empty').max(100, 'Variable label too long'),
  description: z.string().max(500, 'Variable description too long').optional(),
  required: z.boolean().default(false),
  defaultValue: z.union([z.string(), z.boolean(), z.array(z.string())]).optional(),
  options: z.array(z.string()).optional(), // For select/multiselect types
  validation: z.object({
    minLength: z.number().int().min(0).optional(),
    maxLength: z.number().int().min(0).optional(),
    pattern: z.string().optional(),
    customValidator: z.string().optional(),
  }).optional(),
});

/**
 * Template section schema for organizing content
 */
export const TemplateSectionSchema = z.object({
  id: z.string().min(1, 'Section ID cannot be empty'),
  title: z.string().min(1, 'Section title cannot be empty').max(100, 'Section title too long'),
  description: z.string().max(500, 'Section description too long').optional(),
  order: z.number().int().min(0, 'Section order must be non-negative'),
  required: z.boolean().default(true),
  variables: z.array(TemplateVariableSchema).default([]),
  content: z.string().min(1, 'Section content cannot be empty'),
  conditional: z.object({
    variable: z.string(),
    condition: z.enum(['equals', 'not_equals', 'contains', 'not_contains']),
    value: z.union([z.string(), z.boolean(), z.array(z.string())]),
  }).optional(),
});

/**
 * Full documentation template schema
 */
export const DocumentationTemplateSchema = z.object({
  id: z.string().uuid('Template ID must be a valid UUID'),
  name: TemplateNameSchema,
  displayName: z.string().min(1, 'Display name cannot be empty').max(100, 'Display name too long'),
  description: z.string().min(1, 'Description cannot be empty').max(1000, 'Description too long'),
  version: z.string().regex(/^\d+\.\d+\.\d+$/, 'Version must be semantic (x.y.z)'),
  sections: z.array(TemplateSectionSchema).min(1, 'Template must have at least one section'),
  frontMatterSchema: z.record(z.any()).default({}),
  requiredFields: z.array(z.string()).default([]),
  outputFormat: z.enum(['markdown', 'mdx']).default('markdown'),
  targetSection: SectionSchema,
  targetAudience: AudienceSchema.optional(),
  tags: z.array(z.string()).default([]),
  createdAt: z.string().datetime('Created date must be ISO 8601 format'),
  updatedAt: z.string().datetime('Updated date must be ISO 8601 format'),
  createdBy: z.string().min(1, 'Created by cannot be empty'),
  isActive: z.boolean().default(true),
  usageCount: z.number().int().min(0, 'Usage count must be non-negative').default(0),
});

/**
 * Template processing request schema
 */
export const TemplateProcessingRequestSchema = z.object({
  templateId: z.string().uuid('Template ID must be a valid UUID'),
  variables: z.record(z.union([z.string(), z.boolean(), z.array(z.string())])),
  outputPath: z.string().regex(/^docs\/.*\.md$/, 'Output path must be in docs/ directory and end with .md').optional(),
  frontMatter: FrontMatterSchema.partial().optional(),
  validateOnly: z.boolean().default(false),
});

/**
 * Template processing result schema
 */
export const TemplateProcessingResultSchema = z.object({
  success: z.boolean(),
  content: z.string().optional(),
  frontMatter: z.record(z.any()).optional(),
  filePath: z.string().optional(),
  errors: z.array(z.object({
    field: z.string(),
    message: z.string(),
    code: z.string(),
  })).default([]),
  warnings: z.array(z.object({
    field: z.string(),
    message: z.string(),
    code: z.string(),
  })).default([]),
  processedSections: z.array(z.string()).default([]),
  skippedSections: z.array(z.string()).default([]),
  generatedAt: z.string().datetime('Generated date must be ISO 8601 format'),
});

/**
 * Template summary schema for listings
 */
export const TemplateSummarySchema = z.object({
  id: z.string().uuid('Template ID must be a valid UUID'),
  name: TemplateNameSchema,
  displayName: z.string().min(1, 'Display name cannot be empty'),
  description: z.string().min(1, 'Description cannot be empty'),
  version: z.string().regex(/^\d+\.\d+\.\d+$/),
  targetSection: SectionSchema,
  targetAudience: AudienceSchema.optional(),
  usageCount: z.number().int().min(0),
  isActive: z.boolean(),
  lastUsed: z.string().datetime().optional(),
  updatedAt: z.string().datetime(),
});

/**
 * Template creation request schema
 */
export const CreateTemplateRequestSchema = z.object({
  name: TemplateNameSchema,
  displayName: z.string().min(1, 'Display name cannot be empty').max(100, 'Display name too long'),
  description: z.string().min(1, 'Description cannot be empty').max(1000, 'Description too long'),
  targetSection: SectionSchema,
  targetAudience: AudienceSchema.optional(),
  sections: z.array(TemplateSectionSchema).min(1, 'Template must have at least one section'),
  frontMatterSchema: z.record(z.any()).default({}),
  requiredFields: z.array(z.string()).default([]),
  tags: z.array(z.string()).default([]),
});

/**
 * Template update request schema
 */
export const UpdateTemplateRequestSchema = z.object({
  displayName: z.string().min(1, 'Display name cannot be empty').max(100, 'Display name too long').optional(),
  description: z.string().min(1, 'Description cannot be empty').max(1000, 'Description too long').optional(),
  sections: z.array(TemplateSectionSchema).min(1, 'Template must have at least one section').optional(),
  frontMatterSchema: z.record(z.any()).optional(),
  requiredFields: z.array(z.string()).optional(),
  tags: z.array(z.string()).optional(),
  isActive: z.boolean().optional(),
});

/**
 * Template validation schema
 */
export const TemplateValidationSchema = z.object({
  templateId: z.string().uuid('Template ID must be a valid UUID'),
  variables: z.record(z.union([z.string(), z.boolean(), z.array(z.string())])),
  strict: z.boolean().default(true), // Whether to enforce all required fields
});

/**
 * Template validation result schema
 */
export const TemplateValidationResultSchema = z.object({
  isValid: z.boolean(),
  missingVariables: z.array(z.string()).default([]),
  invalidVariables: z.array(z.object({
    name: z.string(),
    value: z.union([z.string(), z.boolean(), z.array(z.string())]),
    error: z.string(),
  })).default([]),
  warnings: z.array(z.object({
    name: z.string(),
    message: z.string(),
  })).default([]),
  conditionalSections: z.array(z.object({
    sectionId: z.string(),
    included: z.boolean(),
    reason: z.string(),
  })).default([]),
});

/**
 * Validation functions for runtime type checking
 */

export const validateDocumentationTemplate = (data: unknown) => {
  return DocumentationTemplateSchema.safeParse(data);
};

export const validateTemplateVariable = (data: unknown) => {
  return TemplateVariableSchema.safeParse(data);
};

export const validateTemplateSection = (data: unknown) => {
  return TemplateSectionSchema.safeParse(data);
};

export const validateTemplateProcessingRequest = (data: unknown) => {
  return TemplateProcessingRequestSchema.safeParse(data);
};

export const validateTemplateProcessingResult = (data: unknown) => {
  return TemplateProcessingResultSchema.safeParse(data);
};

export const validateTemplateSummary = (data: unknown) => {
  return TemplateSummarySchema.safeParse(data);
};

export const validateCreateTemplateRequest = (data: unknown) => {
  return CreateTemplateRequestSchema.safeParse(data);
};

export const validateUpdateTemplateRequest = (data: unknown) => {
  return UpdateTemplateRequestSchema.safeParse(data);
};

export const validateTemplateValidation = (data: unknown) => {
  return TemplateValidationSchema.safeParse(data);
};

export const validateTemplateValidationResult = (data: unknown) => {
  return TemplateValidationResultSchema.safeParse(data);
};

/**
 * Helper functions for template processing
 */

export const validateTemplateName = (name: string): boolean => {
  return TemplateNameSchema.safeParse(name).success;
};

export const validateVariableValue = (variable: any, value: any): boolean => {
  const variableResult = TemplateVariableSchema.safeParse(variable);
  if (!variableResult.success) return false;

  const varDef = variableResult.data;

  switch (varDef.type) {
    case 'string':
      return typeof value === 'string' &&
             (!varDef.validation?.minLength || value.length >= varDef.validation.minLength) &&
             (!varDef.validation?.maxLength || value.length <= varDef.validation.maxLength) &&
             (!varDef.validation?.pattern || new RegExp(varDef.validation.pattern).test(value));
    case 'text':
      return typeof value === 'string';
    case 'boolean':
      return typeof value === 'boolean';
    case 'select':
      return typeof value === 'string' &&
             (!varDef.options || varDef.options.includes(value));
    case 'multiselect':
      return Array.isArray(value) &&
             value.every(v => typeof v === 'string') &&
             (!varDef.options || value.every(v => varDef.options!.includes(v)));
    case 'date':
      return typeof value === 'string' && !isNaN(Date.parse(value));
    default:
      return false;
  }
};

export const processTemplateContent = (content: string, variables: Record<string, any>): string => {
  let processed = content;

  // Replace template variables with format {{variableName}}
  Object.entries(variables).forEach(([key, value]) => {
    const placeholder = new RegExp(`{{\\s*${key}\\s*}}`, 'g');
    const stringValue = Array.isArray(value) ? value.join(', ') : String(value);
    processed = processed.replace(placeholder, stringValue);
  });

  return processed;
};

export const extractTemplateVariables = (content: string): string[] => {
  const variableRegex = /{{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}/g;
  const variables: string[] = [];
  let match;

  while ((match = variableRegex.exec(content)) !== null) {
    if (!variables.includes(match[1])) {
      variables.push(match[1]);
    }
  }

  return variables;
};

export const generateTemplateId = (name: string, version: string): string => {
  const timestamp = Date.now();
  const hash = Buffer.from(`${name}-${version}-${timestamp}`).toString('base64')
    .replace(/[^a-zA-Z0-9]/g, '')
    .substring(0, 8);
  return `template-${name}-${hash}`;
};

export const validateConditionalSection = (
  section: any,
  variables: Record<string, any>
): boolean => {
  const sectionResult = TemplateSectionSchema.safeParse(section);
  if (!sectionResult.success || !sectionResult.data.conditional) {
    return true; // Include section if no conditional or invalid
  }

  const { variable, condition, value } = sectionResult.data.conditional;
  const varValue = variables[variable];

  switch (condition) {
    case 'equals':
      return varValue === value;
    case 'not_equals':
      return varValue !== value;
    case 'contains':
      return Array.isArray(varValue) && Array.isArray(value)
        ? value.some(v => varValue.includes(v))
        : String(varValue).includes(String(value));
    case 'not_contains':
      return Array.isArray(varValue) && Array.isArray(value)
        ? !value.some(v => varValue.includes(v))
        : !String(varValue).includes(String(value));
    default:
      return true;
  }
};

/**
 * Type exports for use in other modules
 */
export type DocumentationTemplate = z.infer<typeof DocumentationTemplateSchema>;
export type TemplateVariable = z.infer<typeof TemplateVariableSchema>;
export type TemplateSection = z.infer<typeof TemplateSectionSchema>;
export type TemplateProcessingRequest = z.infer<typeof TemplateProcessingRequestSchema>;
export type TemplateProcessingResult = z.infer<typeof TemplateProcessingResultSchema>;
export type TemplateSummary = z.infer<typeof TemplateSummarySchema>;
export type CreateTemplateRequest = z.infer<typeof CreateTemplateRequestSchema>;
export type UpdateTemplateRequest = z.infer<typeof UpdateTemplateRequestSchema>;
export type TemplateValidation = z.infer<typeof TemplateValidationSchema>;
export type TemplateValidationResult = z.infer<typeof TemplateValidationResultSchema>;
export type TemplateName = z.infer<typeof TemplateNameSchema>;