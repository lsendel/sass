const isDev = (): boolean => {
  try {
    return import.meta.env.DEV === true
  } catch {
    return false
  }
}

export const logger = {
  log: (...args: any[]) => {
    if (isDev()) console.log(...args)
  },
  error: (...args: any[]) => {
    console.error(...args)
  },
  warn: (...args: any[]) => {
    if (isDev()) console.warn(...args)
  },
  info: (...args: any[]) => {
    if (isDev()) console.info(...args)
  },
}

export { isDev }
