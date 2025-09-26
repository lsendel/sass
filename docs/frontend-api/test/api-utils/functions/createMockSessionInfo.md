# Function: createMockSessionInfo()

> **createMockSessionInfo**(`overrides`): `object`

Defined in: [frontend/src/test/api-utils.ts:43](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/test/api-utils.ts#L43)

## Parameters

### overrides

## Returns

`object`

### session

> **session**: `object`

#### session.activeTokens

> **activeTokens**: `number` = `1`

#### session.createdAt

> **createdAt**: `string` = `'2024-01-01T09:00:00Z'`

#### session.lastActiveAt

> **lastActiveAt**: `string` = `'2024-01-15T10:30:00Z'`

### user

> **user**: `object`

#### user.createdAt

> **createdAt**: `string` = `'2024-01-01T00:00:00Z'`

#### user.email

> **email**: `string` = `'test@example.com'`

#### user.id

> **id**: `string` = `'123'`

#### user.lastActiveAt

> **lastActiveAt**: `null` = `null`

#### user.name

> **name**: `string` = `'Test User'`

#### user.preferences

> **preferences**: `object` = `{}`

#### user.provider

> **provider**: `string` = `'google'`
