[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / test/api-utils

# test/api-utils

## Type Aliases

### ApiStore

> **ApiStore**\<`T`\> = `ReturnType`\<*typeof* [`setupApiStore`](#setupapistore)\>

Defined in: [src/test/api-utils.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/test/api-utils.ts#L28)

#### Type Parameters

##### T

`T` *extends* `Api`\<`BaseQueryFn`, `any`, `any`, `any`\>

## Functions

### createMockAuthMethods()

> **createMockAuthMethods**(`overrides`): `object`

Defined in: [src/test/api-utils.ts:52](https://github.com/lsendel/sass/blob/main/frontend/src/test/api-utils.ts#L52)

#### Parameters

##### overrides

#### Returns

`object`

##### methods

> **methods**: `string`[]

##### oauth2Providers

> **oauth2Providers**: `string`[]

##### passwordAuthEnabled

> **passwordAuthEnabled**: `boolean` = `true`

***

### createMockSessionInfo()

> **createMockSessionInfo**(`overrides`): `object`

Defined in: [src/test/api-utils.ts:42](https://github.com/lsendel/sass/blob/main/frontend/src/test/api-utils.ts#L42)

#### Parameters

##### overrides

#### Returns

`object`

##### session

> **session**: `object`

###### session.activeTokens

> **activeTokens**: `number` = `1`

###### session.createdAt

> **createdAt**: `string` = `'2024-01-01T09:00:00Z'`

###### session.lastActiveAt

> **lastActiveAt**: `string` = `'2024-01-15T10:30:00Z'`

##### user

> **user**: `object`

###### user.createdAt

> **createdAt**: `string` = `'2024-01-01T00:00:00Z'`

###### user.email

> **email**: `string` = `'test@example.com'`

###### user.id

> **id**: `string` = `'123'`

###### user.lastActiveAt

> **lastActiveAt**: `null` = `null`

###### user.name

> **name**: `string` = `'Test User'`

###### user.preferences

> **preferences**: `object` = `{}`

###### user.provider

> **provider**: `string` = `'google'`

***

### createMockUser()

> **createMockUser**(`overrides`): `object`

Defined in: [src/test/api-utils.ts:31](https://github.com/lsendel/sass/blob/main/frontend/src/test/api-utils.ts#L31)

#### Parameters

##### overrides

#### Returns

`object`

##### createdAt

> **createdAt**: `string` = `'2024-01-01T00:00:00Z'`

##### email

> **email**: `string` = `'test@example.com'`

##### id

> **id**: `string` = `'123'`

##### lastActiveAt

> **lastActiveAt**: `null` = `null`

##### name

> **name**: `string` = `'Test User'`

##### preferences

> **preferences**: `object` = `{}`

##### provider

> **provider**: `string` = `'google'`

***

### setupApiStore()

> **setupApiStore**\<`T`\>(`api`, `extraReducers?`, `preloadedState?`): `EnhancedStore`\<\{ `auth`: [`AuthState`](../store/slices/authSlice.md#authstate); `ui`: [`UiState`](../store/slices/uiSlice.md#uistate); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

Defined in: [src/test/api-utils.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/test/api-utils.ts#L7)

#### Type Parameters

##### T

`T` *extends* `Api`\<`BaseQueryFn`, `any`, `any`, `any`\>

#### Parameters

##### api

`T`

##### extraReducers?

`Record`\<`string`, `any`\>

##### preloadedState?

`any`

#### Returns

`EnhancedStore`\<\{ `auth`: [`AuthState`](../store/slices/authSlice.md#authstate); `ui`: [`UiState`](../store/slices/uiSlice.md#uistate); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>
