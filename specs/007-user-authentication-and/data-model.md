# Data Model: OAuth2 Authentication System

**Feature**: User Authentication and Login System with OAuth2 Providers
**Branch**: `007-user-authentication-and`
**Date**: 2025-01-15

## Overview
This document defines the data entities for OAuth2/PKCE authentication system that extends the existing Spring Boot Modulith payment platform. The model supports multiple OAuth2 providers (Google, GitHub, Microsoft) with secure session management and comprehensive audit logging.

## Entity Definitions

### OAuth2Provider
Represents external OAuth2 authentication providers and their configuration.

**Purpose**: Configuration and metadata for OAuth2 providers (Google, GitHub, Microsoft)

**Attributes**:
- `name` (String, required): Provider identifier (google, github, microsoft)
- `displayName` (String, required): Human-readable provider name
- `clientId` (String, required): OAuth2 client identifier
- `clientSecret` (String, required): OAuth2 client secret (encrypted at rest)
- `issuerUri` (String, required): OAuth2 provider issuer URI
- `authorizationUri` (String, required): OAuth2 authorization endpoint
- `tokenUri` (String, required): OAuth2 token endpoint
- `userInfoUri` (String, required): OAuth2 user information endpoint
- `jwkSetUri` (String, optional): JWT key set URI (if applicable)
- `scopes` (Set<String>, required): Default OAuth2 scopes to request
- `enabled` (Boolean, required): Whether provider is active
- `createdAt` (LocalDateTime, required): Provider configuration creation time
- `updatedAt` (LocalDateTime, required): Last configuration update time

**Validation Rules**:
- `name` must be unique across all providers
- `clientId` and `clientSecret` must be non-empty when `enabled` is true
- All URI fields must be valid HTTP/HTTPS URLs
- `scopes` must contain at least "openid" for OIDC compliance

**Relationships**:
- One-to-many with `OAuth2UserInfo` (provider → user accounts)
- One-to-many with `OAuth2Session` (provider → active sessions)
- One-to-many with `OAuth2AuditEvent` (provider → audit events)

### OAuth2UserInfo
Represents user identity information from OAuth2 providers linked to platform users.

**Purpose**: OAuth2 provider-specific user identity and linking to platform User entity

**Attributes**:
- `id` (UUID, required): Primary key
- `userId` (UUID, required): Reference to platform User entity
- `provider` (String, required): OAuth2 provider name (references OAuth2Provider.name)
- `providerUserId` (String, required): Provider-specific user identifier (sub claim)
- `email` (String, required): User email from OAuth2 provider
- `emailVerified` (Boolean, required): Email verification status from provider
- `name` (String, optional): Full name from OAuth2 provider
- `givenName` (String, optional): First name from OAuth2 provider
- `familyName` (String, optional): Last name from OAuth2 provider
- `picture` (String, optional): Profile picture URL from OAuth2 provider
- `locale` (String, optional): User locale from OAuth2 provider
- `lastSync` (LocalDateTime, required): Last synchronization with provider
- `createdAt` (LocalDateTime, required): Account linking creation time
- `updatedAt` (LocalDateTime, required): Last profile update time

**Validation Rules**:
- Composite unique constraint on (`provider`, `providerUserId`)
- Composite unique constraint on (`userId`, `provider`) - one account per provider per user
- `email` must be valid email format
- `picture` must be valid HTTP/HTTPS URL if provided

**Relationships**:
- Many-to-one with `User` (OAuth2 accounts → platform user)
- Many-to-one with `OAuth2Provider` (user account → provider configuration)
- One-to-many with `OAuth2Session` (user account → active sessions)

### OAuth2Session
Represents active OAuth2 authentication sessions with secure token storage.

**Purpose**: Secure session management for OAuth2 authenticated users

**Attributes**:
- `id` (UUID, required): Primary key
- `sessionId` (String, required): Platform session identifier (opaque token)
- `userId` (UUID, required): Reference to platform User entity
- `provider` (String, required): OAuth2 provider used for authentication
- `accessTokenHash` (String, required): SHA-256 hash of OAuth2 access token
- `refreshTokenHash` (String, optional): SHA-256 hash of OAuth2 refresh token
- `tokenSalt` (String, required): Salt for token hashing
- `expiresAt` (LocalDateTime, required): Session expiration time
- `accessTokenExpiresAt` (LocalDateTime, required): OAuth2 access token expiration
- `refreshTokenExpiresAt` (LocalDateTime, optional): OAuth2 refresh token expiration
- `scopes` (Set<String>, required): OAuth2 scopes granted in this session
- `ipAddress` (String, required): Client IP address for session
- `userAgent` (String, required): Client user agent for session
- `lastActivity` (LocalDateTime, required): Last session activity timestamp
- `createdAt` (LocalDateTime, required): Session creation time

**Security Properties**:
- Actual OAuth2 tokens never stored in database (only hashes)
- Token hashes use SHA-256 with unique salt per session
- Session timeout enforced at 24 hours maximum
- IP address and user agent tracked for security monitoring

**Validation Rules**:
- `sessionId` must be unique across all sessions
- `expiresAt` must be within 24 hours of creation
- `accessTokenExpiresAt` must be before `expiresAt`
- `ipAddress` must be valid IPv4/IPv6 format

**Relationships**:
- Many-to-one with `User` (sessions → platform user)
- Many-to-one with `OAuth2Provider` (session → provider used)
- Many-to-one with `OAuth2UserInfo` (session → OAuth2 account)

### OAuth2AuditEvent
Represents audit log entries for OAuth2 authentication events and security monitoring.

**Purpose**: Comprehensive audit logging for OAuth2 authentication flows and security events

**Attributes**:
- `id` (UUID, required): Primary key
- `eventType` (String, required): OAuth2 event type (LOGIN_START, LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, TOKEN_REFRESH, PROVIDER_ERROR)
- `userId` (UUID, optional): Platform user ID (null for failed login attempts)
- `provider` (String, required): OAuth2 provider involved in event
- `sessionId` (String, optional): Session identifier (for successful events)
- `ipAddress` (String, required): Client IP address
- `userAgent` (String, required): Client user agent
- `errorCode` (String, optional): OAuth2 error code (for failures)
- `errorDescription` (String, optional): Human-readable error description
- `providerResponse` (Map<String, Object>, optional): Sanitized provider response data
- `requestId` (String, required): Unique request identifier for correlation
- `duration` (Long, optional): Event processing duration in milliseconds
- `createdAt` (LocalDateTime, required): Event timestamp

**Event Types**:
- `LOGIN_START`: OAuth2 authorization flow initiated
- `LOGIN_SUCCESS`: Successful OAuth2 authentication and session creation
- `LOGIN_FAILURE`: Failed OAuth2 authentication (various reasons)
- `LOGOUT`: User-initiated session termination
- `TOKEN_REFRESH`: OAuth2 access token refresh operation
- `PROVIDER_ERROR`: OAuth2 provider error or unavailability
- `SESSION_TIMEOUT`: Automatic session expiration
- `ACCOUNT_LINKING`: OAuth2 account linked to existing user

**Validation Rules**:
- `eventType` must be from predefined enum values
- `errorCode` and `errorDescription` required for failure events
- `userId` required for successful authentication events
- `duration` must be positive number if provided

**Relationships**:
- Many-to-one with `User` (audit events → platform user, optional)
- Many-to-one with `OAuth2Provider` (audit events → provider)

## Entity Relationships Summary

```
User (existing)
├── 1:N OAuth2UserInfo (OAuth2 accounts per user)
│   ├── N:1 OAuth2Provider (account → provider configuration)
│   └── 1:N OAuth2Session (account → active sessions)
└── 1:N OAuth2AuditEvent (user → audit events)

OAuth2Provider
├── 1:N OAuth2UserInfo (provider → user accounts)
├── 1:N OAuth2Session (provider → sessions)
└── 1:N OAuth2AuditEvent (provider → audit events)

OAuth2Session
├── N:1 User (session → platform user)
├── N:1 OAuth2Provider (session → provider)
└── N:1 OAuth2UserInfo (session → OAuth2 account)

OAuth2AuditEvent
├── N:1 User (audit → platform user, optional)
└── N:1 OAuth2Provider (audit → provider)
```

## Database Schema Considerations

### Indexes
- `OAuth2UserInfo`: Index on (`provider`, `providerUserId`), (`userId`, `provider`)
- `OAuth2Session`: Index on `sessionId`, `userId`, `expiresAt`, (`provider`, `lastActivity`)
- `OAuth2AuditEvent`: Index on `userId`, `provider`, `eventType`, `createdAt`

### Security
- `OAuth2Provider.clientSecret`: Encrypted at rest using platform encryption
- `OAuth2Session.accessTokenHash`: SHA-256 with salt, never store plain tokens
- `OAuth2AuditEvent.providerResponse`: PII redacted, only safe metadata stored

### Performance
- OAuth2Session cleanup job: Remove expired sessions daily
- OAuth2AuditEvent retention: 90 days for security events, configurable
- Connection pooling optimized for OAuth2 token validation queries

### GDPR Compliance
- OAuth2UserInfo: User profile data subject to GDPR deletion requests
- OAuth2AuditEvent: Anonymize user data after retention period
- OAuth2Session: Immediate cleanup on user account deletion

## State Transitions

### OAuth2Session Lifecycle
1. **Created**: New session after successful OAuth2 authentication
2. **Active**: Session within expiration time, regular activity
3. **Expired**: Session past expiration time, cleanup pending
4. **Terminated**: User-initiated logout or security revocation

### OAuth2UserInfo Synchronization
1. **Initial**: First-time OAuth2 account linking
2. **Synced**: Regular profile data synchronization from provider
3. **Stale**: Profile data older than sync threshold (24 hours)
4. **Error**: Provider synchronization failed, retry pending

## Migration Strategy

### Phase 1: Core Entities
1. Create `OAuth2Provider` configuration table
2. Extend existing `User` entity relationships
3. Create `OAuth2UserInfo` linking table

### Phase 2: Session Management
1. Create `OAuth2Session` secure session storage
2. Integrate with existing Redis session store
3. Implement token hash storage patterns

### Phase 3: Audit & Monitoring
1. Create `OAuth2AuditEvent` comprehensive logging
2. Integrate with existing audit module
3. Implement GDPR compliance features

## Testing Data Model

### Test Scenarios
- Multiple OAuth2 providers per user account
- Session expiration and cleanup workflows
- Provider configuration changes and migration
- Audit event generation and retention policies
- GDPR deletion cascading through OAuth2 entities

### Performance Testing
- Concurrent OAuth2 authentications (1000+ users)
- Session validation query performance (<50ms)
- Audit event insertion performance (10k+ events/minute)
- Database cleanup job performance (expired data removal)