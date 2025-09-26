[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/slices/authSlice

# store/slices/authSlice

## Type Aliases

### AuthState

> **AuthState** = `object`

Defined in: [src/store/slices/authSlice.ts:4](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L4)

#### Properties

##### error

> **error**: `string` \| `null`

Defined in: [src/store/slices/authSlice.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L9)

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/store/slices/authSlice.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L7)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/store/slices/authSlice.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L8)

##### token

> **token**: `string` \| `null`

Defined in: [src/store/slices/authSlice.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L6)

##### user

> **user**: [`User`](../../types/api.md#user) \| `null`

Defined in: [src/store/slices/authSlice.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L5)

## Variables

### clearError

> **clearError**: `ActionCreatorWithoutPayload`\<`"auth/clearError"`\>

Defined in: [src/store/slices/authSlice.ts:63](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L63)

***

### logout

> **logout**: `ActionCreatorWithoutPayload`\<`"auth/logout"`\>

Defined in: [src/store/slices/authSlice.ts:62](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L62)

***

### setCredentials

> **setCredentials**: `ActionCreatorWithPayload`\<\{ `user`: \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}; \}, `"auth/setCredentials"`\>

Defined in: [src/store/slices/authSlice.ts:58](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L58)

***

### setError

> **setError**: `ActionCreatorWithPayload`\<`null` \| `string`, `"auth/setError"`\>

Defined in: [src/store/slices/authSlice.ts:61](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L61)

***

### setLoading

> **setLoading**: `ActionCreatorWithPayload`\<`boolean`, `"auth/setLoading"`\>

Defined in: [src/store/slices/authSlice.ts:60](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L60)

***

### updateUser

> **updateUser**: `ActionCreatorWithPayload`\<`Partial`\<\{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}\>, `"auth/updateUser"`\>

Defined in: [src/store/slices/authSlice.ts:59](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L59)

## Functions

### selectAuthError()

> **selectAuthError**(`state`): `null` \| `string`

Defined in: [src/store/slices/authSlice.ts:75](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L75)

#### Parameters

##### state

###### auth

[`AuthState`](#authstate)

#### Returns

`null` \| `string`

***

### selectAuthLoading()

> **selectAuthLoading**(`state`): `boolean`

Defined in: [src/store/slices/authSlice.ts:73](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L73)

#### Parameters

##### state

###### auth

[`AuthState`](#authstate)

#### Returns

`boolean`

***

### selectCurrentUser()

> **selectCurrentUser**(`state`): `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

Defined in: [src/store/slices/authSlice.ts:69](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L69)

#### Parameters

##### state

###### auth

[`AuthState`](#authstate)

#### Returns

`null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

***

### selectIsAuthenticated()

> **selectIsAuthenticated**(`state`): `boolean`

Defined in: [src/store/slices/authSlice.ts:71](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L71)

#### Parameters

##### state

###### auth

[`AuthState`](#authstate)

#### Returns

`boolean`

***

### selectToken()

> **selectToken**(`state`): `null` \| `string`

Defined in: [src/store/slices/authSlice.ts:70](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/authSlice.ts#L70)

#### Parameters

##### state

###### auth

[`AuthState`](#authstate)

#### Returns

`null` \| `string`
