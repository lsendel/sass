import type { RootState } from '../index'

export const withAuthHeader = (
  headers: Headers,
  getState: () => unknown,
  headerName = 'authorization'
) => {
  const token = (getState() as RootState).auth.token
  if (token) {
    headers.set(headerName, `Bearer ${token}`)
  }
  return headers
}
