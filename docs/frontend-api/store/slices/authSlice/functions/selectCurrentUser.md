# Function: selectCurrentUser()

> **selectCurrentUser**(`state`): `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

Defined in: [frontend/src/store/slices/authSlice.ts:70](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/store/slices/authSlice.ts#L70)

## Parameters

### state

#### auth

[`AuthState`](../type-aliases/AuthState.md)

## Returns

`null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}
