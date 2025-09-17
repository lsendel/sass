// OAuth2 Authentication Types
// Based on OpenAPI specification from contracts/oauth2-api.yaml

// Branded type for OAuth2 provider names
export type OAuth2ProviderName = 'google' | 'github' | 'microsoft'

// OAuth2 Provider Configuration
export interface OAuth2Provider {
  name: OAuth2ProviderName
  displayName: string
  authorizationUrl: string
  scopes: string[]
}

// OAuth2 Providers Response
export interface OAuth2ProvidersResponse {
  providers: OAuth2Provider[]
}

// OAuth2 User Information from Provider
export interface OAuth2UserInfo {
  sub: string
  email: string
  emailVerified?: boolean
  name: string
  givenName?: string
  familyName?: string
  picture?: string
  locale?: string
  provider: OAuth2ProviderName
}

// OAuth2 Session Information
export interface OAuth2Session {
  sessionId: string
  userId: string
  userInfo: OAuth2UserInfo
  provider: OAuth2ProviderName
  isAuthenticated: boolean
  expiresAt: string // ISO 8601 timestamp
  createdAt: string // ISO 8601 timestamp
  lastAccessedAt: string // ISO 8601 timestamp
}

// OAuth2 Session Response
export interface OAuth2SessionResponse {
  session: OAuth2Session | null
  isAuthenticated: boolean
}

// OAuth2 Authorization Request Parameters
export interface OAuth2AuthorizeRequest {
  provider: OAuth2ProviderName
  redirectUri?: string
  state?: string
}

// OAuth2 Authorization Response
export interface OAuth2AuthorizeResponse {
  authorizationUrl: string
  state: string
  codeChallenge: string
  codeChallengeMethod: 'S256'
}

// OAuth2 Callback Parameters
export interface OAuth2CallbackParams {
  provider: OAuth2ProviderName
  code: string
  state: string
  error?: string
  errorDescription?: string
}

// OAuth2 Callback Response
export interface OAuth2CallbackResponse {
  success: boolean
  session?: OAuth2Session
  redirectTo?: string
  error?: OAuth2Error
}

// OAuth2 Logout Request
export interface OAuth2LogoutRequest {
  terminateProviderSession?: boolean
  redirectUri?: string
}

// OAuth2 Logout Response
export interface OAuth2LogoutResponse {
  success: boolean
  redirectTo?: string
  message?: string
}

// OAuth2 Error Types
export interface OAuth2Error {
  code: string
  message: string
  details?: Record<string, unknown>
  timestamp: string // ISO 8601 timestamp
}

// OAuth2 Flow State Management
export interface OAuth2FlowState {
  provider: OAuth2ProviderName
  state: string
  codeVerifier: string
  redirectUri: string
  startedAt: string // ISO 8601 timestamp
}

// OAuth2 Authentication Status
export interface OAuth2AuthStatus {
  isLoading: boolean
  isAuthenticated: boolean
  session: OAuth2Session | null
  error: OAuth2Error | null
  lastChecked: string | null // ISO 8601 timestamp
}

// OAuth2 Hook Return Type
export interface UseOAuth2Return {
  // Authentication state
  isLoading: boolean
  isAuthenticated: boolean
  session: OAuth2Session | null
  error: OAuth2Error | null

  // Provider management
  providers: OAuth2Provider[] | null
  providersLoading: boolean
  providersError: OAuth2Error | null

  // Authentication actions
  login: (provider: OAuth2ProviderName, redirectUri?: string) => Promise<void>
  logout: (terminateProviderSession?: boolean) => Promise<void>
  refreshSession: () => Promise<void>
  clearError: () => void

  // Flow state
  isInProgress: boolean
  currentProvider: OAuth2ProviderName | null
}

// OAuth2 Context Type
export interface OAuth2ContextValue extends UseOAuth2Return {
  // Additional context-specific methods
  setAuthenticationState: (state: Partial<OAuth2AuthStatus>) => void
  handleCallback: (params: OAuth2CallbackParams) => Promise<void>
}

// OAuth2 Provider Button Props
export interface OAuth2LoginButtonProps {
  provider: OAuth2ProviderName
  redirectUri?: string
  disabled?: boolean
  loading?: boolean
  className?: string
  children?: React.ReactNode
  onLoginStart?: () => void
  onLoginSuccess?: (session: OAuth2Session) => void
  onLoginError?: (error: OAuth2Error) => void
}

// OAuth2 Callback Handler Props
export interface OAuth2CallbackHandlerProps {
  onSuccess?: (session: OAuth2Session) => void
  onError?: (error: OAuth2Error) => void
  fallbackRedirect?: string
}

// OAuth2 Configuration
export interface OAuth2Config {
  baseUrl: string
  endpoints: {
    providers: string
    authorize: string
    callback: string
    session: string
    logout: string
  }
  defaultRedirectUri: string
  sessionCheckInterval: number // milliseconds
  flowTimeout: number // milliseconds
}

// OAuth2 API Error Response
export interface OAuth2ApiErrorResponse {
  error: {
    code: string
    message: string
    details?: Record<string, unknown>
    timestamp: string
  }
}

// Type guards
export const isOAuth2Error = (value: unknown): value is OAuth2Error => {
  return (
    typeof value === 'object' &&
    value !== null &&
    'code' in value &&
    'message' in value &&
    'timestamp' in value
  )
}

export const isOAuth2Session = (value: unknown): value is OAuth2Session => {
  return (
    typeof value === 'object' &&
    value !== null &&
    'sessionId' in value &&
    'userId' in value &&
    'userInfo' in value &&
    'provider' in value &&
    'isAuthenticated' in value
  )
}

export const isValidOAuth2Provider = (
  provider: string
): provider is OAuth2ProviderName => {
  return ['google', 'github', 'microsoft'].includes(provider)
}

// OAuth2 Constants
export const OAUTH2_PROVIDERS: OAuth2ProviderName[] = [
  'google',
  'github',
  'microsoft',
]

export const OAUTH2_PROVIDER_DISPLAY_NAMES: Record<OAuth2ProviderName, string> =
  {
    google: 'Google',
    github: 'GitHub',
    microsoft: 'Microsoft',
  }

export const OAUTH2_SCOPES: Record<OAuth2ProviderName, string[]> = {
  google: ['openid', 'profile', 'email'],
  github: ['read:user', 'user:email'],
  microsoft: ['openid', 'profile', 'email'],
}

// Storage keys for OAuth2 flow state
export const OAUTH2_STORAGE_KEYS = {
  FLOW_STATE: 'oauth2_flow_state',
  CODE_VERIFIER: 'oauth2_code_verifier',
  REDIRECT_URI: 'oauth2_redirect_uri',
} as const
