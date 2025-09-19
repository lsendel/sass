import request from 'supertest';
import { SearchResult } from '../../src/types/DocumentationPage';

describe('GET /api/docs/search Contract Test', () => {
  const mockApp = {
    // This will be replaced with actual app when implemented
    get: jest.fn(),
    use: jest.fn(),
    listen: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should return search results for valid query', async () => {
    // This test MUST fail until the actual API is implemented
    const response = await request(mockApp as any)
      .get('/api/docs/search?q=authentication')
      .expect('Content-Type', /json/)
      .expect(200);

    // Validate response structure according to OpenAPI contract
    expect(response.body).toHaveProperty('results');
    expect(response.body).toHaveProperty('totalCount');
    expect(response.body).toHaveProperty('searchTime');
    expect(response.body).toHaveProperty('query');

    expect(Array.isArray(response.body.results)).toBe(true);
    expect(typeof response.body.totalCount).toBe('number');
    expect(typeof response.body.searchTime).toBe('number');
    expect(response.body.query).toBe('authentication');

    // Validate search result structure
    if (response.body.results.length > 0) {
      const result: SearchResult = response.body.results[0];
      expect(result).toHaveProperty('id');
      expect(result).toHaveProperty('title');
      expect(result).toHaveProperty('section');
      expect(result).toHaveProperty('excerpt');
      expect(result).toHaveProperty('score');
      expect(result).toHaveProperty('url');

      expect(typeof result.id).toBe('string');
      expect(typeof result.title).toBe('string');
      expect(typeof result.section).toBe('string');
      expect(typeof result.excerpt).toBe('string');
      expect(typeof result.score).toBe('number');
      expect(typeof result.url).toBe('string');
      expect(result.score).toBeGreaterThan(0);
      expect(result.score).toBeLessThanOrEqual(1);
    }
  });

  it('should filter search results by section', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/search?q=authentication&section=backend')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('results');
    // All returned results should be from backend section
    response.body.results.forEach((result: SearchResult) => {
      expect(result.section).toBe('backend');
    });
  });

  it('should filter search results by audience', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/search?q=authentication&audience=developer')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body).toHaveProperty('results');
    // Validate audience filtering
    response.body.results.forEach((result: SearchResult) => {
      expect(['developer', 'all']).toContain(result.audience);
    });
  });

  it('should return limited results with limit parameter', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/search?q=authentication&limit=5')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body.results.length).toBeLessThanOrEqual(5);
  });

  it('should return 400 for missing query parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/search')
      .expect(400);
  });

  it('should return 400 for empty query parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/search?q=')
      .expect(400);
  });

  it('should return 400 for invalid section parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/search?q=test&section=invalid')
      .expect(400);
  });

  it('should return 400 for invalid audience parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/search?q=test&audience=invalid')
      .expect(400);
  });

  it('should return 400 for invalid limit parameter', async () => {
    await request(mockApp as any)
      .get('/api/docs/search?q=test&limit=invalid')
      .expect(400);
  });

  it('should return empty results for no matches', async () => {
    const response = await request(mockApp as any)
      .get('/api/docs/search?q=nonexistentterm12345')
      .expect('Content-Type', /json/)
      .expect(200);

    expect(response.body.results).toEqual([]);
    expect(response.body.totalCount).toBe(0);
  });
});