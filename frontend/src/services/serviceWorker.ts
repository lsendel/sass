/// <reference lib="webworker" />
declare const self: ServiceWorkerGlobalScope

const CACHE_NAME = 'sass-platform-v1'
const RUNTIME_CACHE = 'runtime-cache-v1'
const API_CACHE = 'api-cache-v1'

// Assets to cache on install
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json',
  '/favicon.ico',
  '/auth/login',
  '/dashboard',
  '/organizations',
  '/subscription',
  '/settings'
]

// Cache strategies
interface CacheStrategy {
  name: string
  maxAge?: number // in seconds
  maxEntries?: number
}



// Install event - cache static assets
self.addEventListener('install', (event) => {
  console.log('[ServiceWorker] Installing...')

  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('[ServiceWorker] Caching static assets')
        return cache.addAll(STATIC_ASSETS)
      })
      .then(() => self.skipWaiting())
  )
})

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
  console.log('[ServiceWorker] Activating...')

  event.waitUntil(
    caches.keys()
      .then(cacheNames => {
        return Promise.all(
          cacheNames
            .filter(cacheName => {
              return cacheName !== CACHE_NAME &&
                     cacheName !== RUNTIME_CACHE &&
                     cacheName !== API_CACHE
            })
            .map(cacheName => {
              console.log('[ServiceWorker] Deleting old cache:', cacheName)
              return caches.delete(cacheName)
            })
        )
      })
      .then(() => self.clients.claim())
  )
})

// Fetch event - implement cache strategies
self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  // Skip non-GET requests
  if (request.method !== 'GET') {
    return
  }

  // Handle API requests with network-first strategy
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(handleApiRequest(request))
    return
  }

  // Handle static assets with cache-first strategy
  if (isStaticAsset(url.pathname)) {
    event.respondWith(handleStaticRequest(request))
    return
  }

  // Handle navigation requests with network-first, fallback to cache
  if (request.mode === 'navigate') {
    event.respondWith(handleNavigationRequest(request))
    return
  }

  // Default: network-first with cache fallback
  event.respondWith(handleDefaultRequest(request))
})

// Handle API requests with network-first strategy and offline queue
async function handleApiRequest(request: Request): Promise<Response> {
  try {
    const response = await fetch(request)

    // Clone the response before caching
    if (response.ok) {
      const cache = await caches.open(API_CACHE)
      cache.put(request, response.clone())
    }

    return response
  } catch (error) {
    console.log('[ServiceWorker] API request failed, checking cache:', request.url)

    // Try to get from cache
    const cachedResponse = await caches.match(request)
    if (cachedResponse) {
      // Add header to indicate cached response
      const headers = new Headers(cachedResponse.headers)
      headers.set('X-From-Cache', 'true')
      headers.set('X-Cache-Time', new Date().toISOString())

      return new Response(cachedResponse.body, {
        status: cachedResponse.status,
        statusText: cachedResponse.statusText,
        headers
      })
    }

    // Return offline response for API requests
    return createOfflineResponse(request)
  }
}

// Handle static assets with cache-first strategy
async function handleStaticRequest(request: Request): Promise<Response> {
  const cachedResponse = await caches.match(request)

  if (cachedResponse) {
    // Update cache in background
    fetchAndCache(request, CACHE_NAME)
    return cachedResponse
  }

  try {
    const response = await fetch(request)
    const cache = await caches.open(CACHE_NAME)
    cache.put(request, response.clone())
    return response
  } catch (error) {
    console.error('[ServiceWorker] Failed to fetch static asset:', request.url)
    return createOfflineResponse(request)
  }
}

// Handle navigation requests
async function handleNavigationRequest(request: Request): Promise<Response> {
  try {
    const response = await fetch(request)

    if (response.ok) {
      const cache = await caches.open(RUNTIME_CACHE)
      cache.put(request, response.clone())
    }

    return response
  } catch (error) {
    console.log('[ServiceWorker] Navigation failed, trying cache:', request.url)

    const cachedResponse = await caches.match(request)
    if (cachedResponse) {
      return cachedResponse
    }

    // Fallback to offline page
    const offlinePage = await caches.match('/offline.html')
    if (offlinePage) {
      return offlinePage
    }

    return createOfflineResponse(request)
  }
}

// Default handler
async function handleDefaultRequest(request: Request): Promise<Response> {
  try {
    const response = await fetch(request)

    if (response.ok) {
      const cache = await caches.open(RUNTIME_CACHE)
      cache.put(request, response.clone())
    }

    return response
  } catch (error) {
    const cachedResponse = await caches.match(request)
    if (cachedResponse) {
      return cachedResponse
    }

    return createOfflineResponse(request)
  }
}

// Helper function to check if a path is a static asset
function isStaticAsset(pathname: string): boolean {
  const staticExtensions = [
    '.js', '.css', '.png', '.jpg', '.jpeg', '.gif', '.svg',
    '.woff', '.woff2', '.ttf', '.eot', '.ico'
  ]

  return staticExtensions.some(ext => pathname.endsWith(ext))
}

// Helper function to fetch and cache in background
async function fetchAndCache(request: Request, cacheName: string): Promise<void> {
  try {
    const response = await fetch(request)
    if (response.ok) {
      const cache = await caches.open(cacheName)
      cache.put(request, response)
    }
  } catch (error) {
    console.log('[ServiceWorker] Background fetch failed:', request.url)
  }
}

// Create offline response
function createOfflineResponse(request: Request): Response {
  const url = new URL(request.url)

  // Return JSON for API requests
  if (url.pathname.startsWith('/api/')) {
    return new Response(
      JSON.stringify({
        error: 'Offline',
        message: 'You are currently offline. This data may be outdated.',
        offline: true,
        timestamp: new Date().toISOString()
      }),
      {
        status: 503,
        headers: {
          'Content-Type': 'application/json',
          'X-Offline-Response': 'true'
        }
      }
    )
  }

  // Return HTML for navigation requests
  return new Response(
    `<!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Offline - SASS Platform</title>
      <style>
        body {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 100vh;
          margin: 0;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .offline-container {
          background: white;
          padding: 3rem;
          border-radius: 1rem;
          box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
          text-align: center;
          max-width: 400px;
        }
        h1 {
          color: #1a202c;
          margin-bottom: 1rem;
        }
        p {
          color: #4a5568;
          line-height: 1.6;
        }
        .retry-button {
          margin-top: 2rem;
          padding: 0.75rem 1.5rem;
          background: #667eea;
          color: white;
          border: none;
          border-radius: 0.5rem;
          font-size: 1rem;
          cursor: pointer;
          transition: background 0.3s;
        }
        .retry-button:hover {
          background: #5a67d8;
        }
      </style>
    </head>
    <body>
      <div class="offline-container">
        <h1>📡 You're Offline</h1>
        <p>It looks like you've lost your internet connection. Don't worry, we've saved your work locally.</p>
        <p>Some features may be limited while offline.</p>
        <button class="retry-button" onclick="window.location.reload()">
          Try Again
        </button>
      </div>
    </body>
    </html>`,
    {
      status: 503,
      headers: {
        'Content-Type': 'text/html',
        'X-Offline-Response': 'true'
      }
    }
  )
}

// Message event - handle messages from the main thread
self.addEventListener('message', (event) => {
  const { type, payload } = event.data

  switch (type) {
    case 'SKIP_WAITING':
      self.skipWaiting()
      break

    case 'CLEAR_CACHE':
      caches.keys().then(cacheNames => {
        Promise.all(cacheNames.map(cacheName => caches.delete(cacheName)))
      })
      break

    case 'CACHE_URLS':
      if (payload?.urls) {
        caches.open(RUNTIME_CACHE).then(cache => {
          cache.addAll(payload.urls)
        })
      }
      break

    default:
      console.log('[ServiceWorker] Unknown message type:', type)
  }
})

// Background sync for failed requests
self.addEventListener('sync', (event: any) => {
  if (event.tag === 'sync-api-requests') {
    event.waitUntil(syncApiRequests())
  }
})

async function syncApiRequests(): Promise<void> {
  // Get queued requests from IndexedDB (implementation would go here)
  console.log('[ServiceWorker] Syncing API requests...')
  // Process queue and retry failed requests
}

export {}