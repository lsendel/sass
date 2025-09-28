let devOverride: boolean | null = null

export const setLoggerEnvironment = (value: boolean | null) => {
  devOverride = value
}

const resolveDevFlag = (): boolean => {
  if (typeof devOverride === 'boolean') {
    return devOverride
  }

  // Check environment variable for development mode
  try {
    return import.meta.env?.DEV === true
  } catch {
    return false
  }

  try {
    return typeof import.meta !== 'undefined' && import.meta.env?.DEV === true
  } catch {
    return false
  }
}

let cachedIsDev: boolean | null = null

const isDev = (): boolean => {
  if (cachedIsDev === null) {
    cachedIsDev = resolveDevFlag()
  }
  return cachedIsDev
}

// For testing purposes
export const resetDevFlag = (): void => {
  cachedIsDev = null
}

export const logger = {
  log: (...args: any[]) => {
    if (isDev()) console.log(...args)
  },
  error: (...args: any[]) => {
    if (isDev()) console.error(...args)
  },
  warn: (...args: any[]) => {
    if (isDev()) console.warn(...args)
  },
  info: (...args: any[]) => {
    if (isDev()) console.info(...args)
  },
  debug: (...args: any[]) => {
    if (isDev()) console.debug?.(...args)
  },
}

export { isDev }
