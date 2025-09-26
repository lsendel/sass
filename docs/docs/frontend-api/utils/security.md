[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / utils/security

# utils/security

## Functions

### generateSecureToken()

> **generateSecureToken**(): `string`

Defined in: [src/utils/security.ts:38](https://github.com/lsendel/sass/blob/main/frontend/src/utils/security.ts#L38)

Generate a secure random string for CSRF tokens

#### Returns

`string`

***

### isSafeUrl()

> **isSafeUrl**(`url`): `boolean`

Defined in: [src/utils/security.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/utils/security.ts#L22)

Validate that a URL is safe for redirection

#### Parameters

##### url

`string`

#### Returns

`boolean`

***

### isValidEmail()

> **isValidEmail**(`email`): `boolean`

Defined in: [src/utils/security.ts:47](https://github.com/lsendel/sass/blob/main/frontend/src/utils/security.ts#L47)

Validate email format

#### Parameters

##### email

`string`

#### Returns

`boolean`

***

### sanitizeInput()

> **sanitizeInput**(`input`): `string`

Defined in: [src/utils/security.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/utils/security.ts#L8)

Sanitize user input to prevent XSS attacks

#### Parameters

##### input

`string`

#### Returns

`string`

***

### validatePassword()

> **validatePassword**(`password`): `object`

Defined in: [src/utils/security.ts:55](https://github.com/lsendel/sass/blob/main/frontend/src/utils/security.ts#L55)

Check if password meets security requirements

#### Parameters

##### password

`string`

#### Returns

`object`

##### errors

> **errors**: `string`[]

##### valid

> **valid**: `boolean`
