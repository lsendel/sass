// import { useAppSelector } from '@/store/hooks'

export interface Tenant {
  id: string
  name: string
  slug: string
}

export function useTenant() {
  // For now, return a default tenant since multi-tenancy is not fully implemented
  // This would typically come from the auth state or a separate tenant slice
  const currentTenant: Tenant = {
    id: 'default',
    name: 'Default Organization',
    slug: 'default'
  }

  return {
    currentTenant,
    // Add other tenant-related functionality as needed
    isLoading: false,
    error: null
  }
}