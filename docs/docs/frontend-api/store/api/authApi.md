[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/api/authApi

# store/api/authApi

## Variables

### authApi

> `const` **authApi**: `Api`\<`BaseQueryFn`, \{ `getAuthMethods`: `QueryDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `methods`: `string`[]; `oauth2Providers`: `string`[]; `passwordAuthEnabled`: `boolean`; \}, `"authApi"`, `unknown`\>; `getAuthUrl`: `QueryDefinition`\<\{ `provider`: `string`; `redirectUri`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `authUrl`: `string`; \}, `"authApi"`, `unknown`\>; `getSession`: `QueryDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `session`: \{ `activeTokens`: `number`; `createdAt`: `string`; `lastActiveAt`: `string`; \}; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>; `handleCallback`: `MutationDefinition`\<\{ `code`: `string`; `state?`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>; `logout`: `MutationDefinition`\<`void`, `BaseQueryFn`, `"Session"`, `void`, `"authApi"`, `unknown`\>; `passwordLogin`: `MutationDefinition`\<\{ `email`: `string`; `organizationId`: `string`; `password`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>; `passwordRegister`: `MutationDefinition`\<\{ `email`: `string`; `name`: `string`; `password`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>; `refreshToken`: `MutationDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; \}, `"authApi"`, `unknown`\>; \}, `"authApi"`, `"Session"`, *typeof* `coreModuleName` \| *typeof* `reactHooksModuleName`\>

Defined in: [src/store/api/authApi.ts:26](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L26)

***

### useGetAuthMethodsQuery

> **useGetAuthMethodsQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `methods`: `string`[]; `oauth2Providers`: `string`[]; `passwordAuthEnabled`: `boolean`; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:145](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L145)

***

### useGetAuthUrlQuery

> **useGetAuthUrlQuery**: `UseQuery`\<`QueryDefinition`\<\{ `provider`: `string`; `redirectUri`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `authUrl`: `string`; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:148](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L148)

***

### useGetSessionQuery

> **useGetSessionQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `session`: \{ `activeTokens`: `number`; `createdAt`: `string`; `lastActiveAt`: `string`; \}; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:149](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L149)

***

### useHandleCallbackMutation

> **useHandleCallbackMutation**: `UseMutation`\<`MutationDefinition`\<\{ `code`: `string`; `state?`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:150](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L150)

***

### useLazyGetAuthUrlQuery

> **useLazyGetAuthUrlQuery**: `UseLazyQuery`\<`QueryDefinition`\<\{ `provider`: `string`; `redirectUri`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `authUrl`: `string`; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:154](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L154)

***

### useLazyGetSessionQuery

> **useLazyGetSessionQuery**: `UseLazyQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `session`: \{ `activeTokens`: `number`; `createdAt`: `string`; `lastActiveAt`: `string`; \}; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:155](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L155)

***

### useLogoutMutation

> **useLogoutMutation**: `UseMutation`\<`MutationDefinition`\<`void`, `BaseQueryFn`, `"Session"`, `void`, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:151](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L151)

***

### usePasswordLoginMutation

> **usePasswordLoginMutation**: `UseMutation`\<`MutationDefinition`\<\{ `email`: `string`; `organizationId`: `string`; `password`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:146](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L146)

***

### usePasswordRegisterMutation

> **usePasswordRegisterMutation**: `UseMutation`\<`MutationDefinition`\<\{ `email`: `string`; `name`: `string`; `password`: `string`; \}, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:147](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L147)

***

### useRefreshTokenMutation

> **useRefreshTokenMutation**: `UseMutation`\<`MutationDefinition`\<`void`, `BaseQueryFn`, `"Session"`, \{ `token`: `string`; \}, `"authApi"`, `unknown`\>\>

Defined in: [src/store/api/authApi.ts:152](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/authApi.ts#L152)
