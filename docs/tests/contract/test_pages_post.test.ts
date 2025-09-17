import request from 'supertest';
import { CreatePageRequest, DocumentationPage } from '../../src/types/DocumentationPage';

describe('POST /api/docs/pages Contract Test', () => {
  const mockApp = {
    // This will be replaced with actual app when implemented
    post: jest.fn(),
    use: jest.fn(),
    listen: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should create new documentation page from valid request', async () => {
    const createRequest: CreatePageRequest = {
      title: 'Test Page',
      templateName: 'feature',
      section: 'backend',
      initialContent: '# Test Page\n\nThis is a test page.',
      frontMatter: {
        author: 'Test Author',
        audience: 'developer'
      }
    };

    // This test MUST fail until the actual API is implemented
    const response = await request(mockApp as any)
      .post('/api/docs/pages')
      .send(createRequest)
      .expect('Content-Type', /json/)
      .expect(201);

    // Validate response structure according to OpenAPI contract
    const page: DocumentationPage = response.body;
    expect(page).toHaveProperty('id');
    expect(page).toHaveProperty('title');
    expect(page).toHaveProperty('content');
    expect(page).toHaveProperty('frontMatter');
    expect(page).toHaveProperty('filePath');
    expect(page).toHaveProperty('status');

    expect(typeof page.id).toBe('string');
    expect(page.title).toBe(createRequest.title);
    expect(page.content).toContain(createRequest.initialContent);
    expect(page.frontMatter.author).toBe(createRequest.frontMatter?.author);
    expect(page.status).toBe('draft'); // Default status for new pages
  });

  it('should return 400 for missing required fields', async () => {
    const invalidRequest = {
      // Missing title
      templateName: 'feature',
      section: 'backend'
    };

    await request(mockApp as any)
      .post('/api/docs/pages')
      .send(invalidRequest)
      .expect(400);
  });

  it('should return 400 for invalid template name', async () => {
    const invalidRequest: CreatePageRequest = {
      title: 'Test Page',
      templateName: 'invalid-template',
      section: 'backend'
    };

    await request(mockApp as any)
      .post('/api/docs/pages')
      .send(invalidRequest)
      .expect(400);
  });

  it('should return 400 for invalid section', async () => {
    const invalidRequest: CreatePageRequest = {
      title: 'Test Page',
      templateName: 'feature',
      section: 'invalid-section'
    };

    await request(mockApp as any)
      .post('/api/docs/pages')
      .send(invalidRequest)
      .expect(400);
  });

  it('should return 409 for duplicate page title in same section', async () => {
    const createRequest: CreatePageRequest = {
      title: 'Existing Page',
      templateName: 'feature',
      section: 'backend'
    };

    await request(mockApp as any)
      .post('/api/docs/pages')
      .send(createRequest)
      .expect(409);
  });
});