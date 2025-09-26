[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / lib/api/errorHandling

# lib/api/errorHandling

## Classes

### ApiError

Defined in: [src/lib/api/errorHandling.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L17)

#### Extends

- `Error`

#### Constructors

##### Constructor

> **new ApiError**(`message`, `info`, `originalError?`): [`ApiError`](#apierror)

Defined in: [src/lib/api/errorHandling.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L18)

###### Parameters

###### message

`string`

###### info

[`ErrorInfo`](#errorinfo)

###### originalError?

`Error`

###### Returns

[`ApiError`](#apierror)

###### Overrides

`Error.constructor`

#### Properties

##### info

> `readonly` **info**: [`ErrorInfo`](#errorinfo)

Defined in: [src/lib/api/errorHandling.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L20)

##### originalError?

> `readonly` `optional` **originalError**: `Error`

Defined in: [src/lib/api/errorHandling.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L21)

## Interfaces

### ErrorInfo

Defined in: [src/lib/api/errorHandling.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L9)

Comprehensive error handling for API responses with user-friendly messages

#### Properties

##### code

> **code**: `string`

Defined in: [src/lib/api/errorHandling.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L10)

##### isRetryable

> **isRetryable**: `boolean`

Defined in: [src/lib/api/errorHandling.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L13)

##### message

> **message**: `string`

Defined in: [src/lib/api/errorHandling.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L11)

##### severity

> **severity**: `"high"` \| `"low"` \| `"medium"` \| `"critical"`

Defined in: [src/lib/api/errorHandling.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L14)

##### userMessage

> **userMessage**: `string`

Defined in: [src/lib/api/errorHandling.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L12)

## Functions

### createApiError()

> **createApiError**(`error`): [`ApiError`](#apierror)

Defined in: [src/lib/api/errorHandling.ts:320](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L320)

Creates a user-facing ApiError from any error

#### Parameters

##### error

`unknown`

#### Returns

[`ApiError`](#apierror)

***

### getRetryDelay()

> **getRetryDelay**(`attemptCount`, `baseDelay`): `number`

Defined in: [src/lib/api/errorHandling.ts:351](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L351)

Calculates retry delay with exponential backoff

#### Parameters

##### attemptCount

`number`

##### baseDelay

`number` = `1000`

#### Returns

`number`

***

### processApiError()

> **processApiError**(`error`): [`ErrorInfo`](#errorinfo)

Defined in: [src/lib/api/errorHandling.ts:232](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L232)

Processes any error and returns structured error information

#### Parameters

##### error

`unknown`

#### Returns

[`ErrorInfo`](#errorinfo)

***

### shouldRetry()

> **shouldRetry**(`error`, `attemptCount`, `maxRetries`): `boolean`

Defined in: [src/lib/api/errorHandling.ts:330](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L330)

Determines if an error should trigger a retry

#### Parameters

##### error

[`ErrorInfo`](#errorinfo)

##### attemptCount

`number`

##### maxRetries

`number` = `3`

#### Returns

`boolean`

***

### useApiErrorHandler()

> **useApiErrorHandler**(): `object`

Defined in: [src/lib/api/errorHandling.ts:358](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/errorHandling.ts#L358)

React hook for error handling with retry logic

#### Returns

##### createRetryHandler()

> **createRetryHandler**: (`operation`, `maxRetries`, `baseDelay`) => (`attemptCount`) => `Promise`\<`any`\>

###### Parameters

###### operation

() => `Promise`\<`any`\>

###### maxRetries

`number` = `3`

###### baseDelay

`number` = `1000`

###### Returns

> (`attemptCount`): `Promise`\<`any`\>

###### Parameters

###### attemptCount

`number` = `0`

###### Returns

`Promise`\<`any`\>

##### getRetryDelay()

> **getRetryDelay**: (`attemptCount`, `baseDelay`) => `number`

Calculates retry delay with exponential backoff

###### Parameters

###### attemptCount

`number`

###### baseDelay

`number` = `1000`

###### Returns

`number`

##### handleError()

> **handleError**: (`error`, `context?`) => [`ApiError`](#apierror)

###### Parameters

###### error

`unknown`

###### context?

`string`

###### Returns

[`ApiError`](#apierror)

##### shouldRetry()

> **shouldRetry**: (`error`, `attemptCount`, `maxRetries`) => `boolean`

Determines if an error should trigger a retry

###### Parameters

###### error

[`ErrorInfo`](#errorinfo)

###### attemptCount

`number`

###### maxRetries

`number` = `3`

###### Returns

`boolean`
