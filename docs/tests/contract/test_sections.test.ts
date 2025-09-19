import request from 'supertest';
import { DocumentationSection } from '../../src/types/DocumentationPage';

describe('GET /api/docs/sections Contract Test', () => {
  const mockApp = {
    // This will be replaced with actual app when implemented
    get: jest.fn(),
    use: jest.fn(),
    listen: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should return navigation tree with all sections', async () => {
    // This test MUST fail until the actual API is implemented
    const response = await request(mockApp as any)
      .get('/api/docs/sections')
      .expect('Content-Type', /json/)
      .expect(200);

    // Validate response structure according to OpenAPI contract
    expect(response.body).toHaveProperty('sections');
    expect(Array.isArray(response.body.sections)).toBe(true);

    // Validate section structure
    if (response.body.sections.length > 0) {
      const section: DocumentationSection = response.body.sections[0];
      expect(section).toHaveProperty('id');
      expect(section).toHaveProperty('name');
      expect(section).toHaveProperty('path');
      expect(section).toHaveProperty('order');
      expect(section).toHaveProperty('children');

      expect(typeof section.id).toBe('string');
      expect(typeof section.name).toBe('string');
      expect(typeof section.path).toBe('string');
      expect(typeof section.order).toBe('number');
      expect(Array.isArray(section.children)).toBe(true);

      // Validate children structure if present
      if (section.children && section.children.length > 0) {
        const child = section.children[0];
        expect(child).toHaveProperty('id');
        expect(child).toHaveProperty('title');
        expect(child).toHaveProperty('path');
        expect(child).toHaveProperty('order');
        expect(typeof child.id).toBe('string');
        expect(typeof child.title).toBe('string');
        expect(typeof child.path).toBe('string');
        expect(typeof child.order).toBe('number');
      }
    }
  });

  it('should filter sections by audience', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/sections?audience=developer')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('sections');
    // Validate audience filtering
    response.body.sections.forEach((section: DocumentationSection) => {
      if (section.children) {
        section.children.forEach((child: any) => {
          expect(['developer', 'all']).toContain(child.audience);
        });
      }
    });
  });

  it('should filter sections by version', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/sections?version=1.0.0')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('sections');
    // Validate version filtering
    response.body.sections.forEach((section: DocumentationSection) => {
      if (section.children) {
        section.children.forEach((child: any) => {
          expect(child.version).toBe('1.0.0');
        });
      }
    });
  });

  it('should return sections ordered by order field', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/sections')
      .expect('Content-Type', /json/)
      .expect(200);

    const sections = response.body.sections;
    for (let i = 1; i < sections.length; i++) {
      expect(sections[i].order).toBeGreaterThanOrEqual(sections[i - 1].order);
    }

    // Check children ordering within each section
    sections.forEach((section: DocumentationSection) => {
      if (section.children && section.children.length > 1) {
        for (let i = 1; i < section.children.length; i++) {
          expect(section.children[i].order).toBeGreaterThanOrEqual(
            section.children[i - 1].order
          );
        }
      }
    });
  });

  it('should return 400 for invalid audience parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/sections?audience=invalid')
      .expect(400);
  });

  it('should return 400 for invalid version format', async () => {
    await request(mockApp as any)
      .get('/api/docs/sections?version=invalid')
      .expect(400);
  });

  it('should include expected core sections', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/sections')
      .expect('Content-Type', /json/)
      .expect(200);

    const sectionNames = response.body.sections.map((s: DocumentationSection) => s.name);

    // These are the core sections we expect to exist
    expect(sectionNames).toContain('Getting Started');
    expect(sectionNames).toContain('Architecture');
    expect(sectionNames).toContain('Backend Development');
    expect(sectionNames).toContain('Frontend Development');
  });
});