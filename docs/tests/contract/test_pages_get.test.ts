import request from 'supertest';
import { DocumentationPageSummary } from '../../src/types/DocumentationPage';

describe('GET /api/docs/pages Contract Test', () => {
  const mockApp = {
    // This will be replaced with actual app when implemented
    get: jest.fn(),
    use: jest.fn(),
    listen: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should return list of documentation pages with default parameters', async () => {
    // This test MUST fail until the actual API is implemented
    const response = await request(mockApp as any)
      .get('/api/docs/pages')
      .expect('Content-Type', /json/)
      .expect(200);

    // Validate response structure according to OpenAPI contract
    expect(response.body).toHaveProperty('pages');
    expect(response.body).toHaveProperty('totalCount');
    expect(response.body).toHaveProperty('currentPage');
    expect(response.body).toHaveProperty('totalPages');

    expect(Array.isArray(response.body.pages)).toBe(true);
    expect(typeof response.body.totalCount).toBe('number');
    expect(typeof response.body.currentPage).toBe('number');
    expect(typeof response.body.totalPages).toBe('number');

    // Validate page summary structure
    if (response.body.pages.length > 0) {
      const page: DocumentationPageSummary = response.body.pages[0];
      expect(page).toHaveProperty('id');
      expect(page).toHaveProperty('title');
      expect(page).toHaveProperty('status');
      expect(typeof page.id).toBe('string');
      expect(typeof page.title).toBe('string');
      expect(['draft', 'review', 'published', 'deprecated', 'archived']).toContain(page.status);
    }
  });

  it('should filter pages by section parameter', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/pages?section=architecture')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('pages');
    // All returned pages should be from architecture section
    response.body.pages.forEach((page: DocumentationPageSummary) => {
      expect(page.section).toBe('architecture');
    });
  });

  it('should filter pages by audience parameter', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/pages?audience=developer')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('pages');
    // Validate audience filtering
    response.body.pages.forEach((page: DocumentationPageSummary) => {
      expect(['developer', 'all']).toContain(page.audience);
    });
  });

  it('should filter pages by status parameter', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/pages?status=published')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('pages');
    // All returned pages should have published status
    response.body.pages.forEach((page: DocumentationPageSummary) => {
      expect(page.status).toBe('published');
    });
  });

  it('should filter pages by version parameter', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/pages?version=1.0.0')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('pages');
    // Validate version filtering
    response.body.pages.forEach((page: DocumentationPageSummary) => {
      expect(page.version).toBe('1.0.0');
    });
  });

  it('should return 400 for invalid audience parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/pages?audience=invalid')
      .expect(400);
  });

  it('should return 400 for invalid status parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/pages?status=invalid')
      .expect(400);
  });

  it('should return 400 for invalid version format', async () => {
    await request(mockApp as any)
      .get('/api/docs/pages?version=invalid')
      .expect(400);
  });
});