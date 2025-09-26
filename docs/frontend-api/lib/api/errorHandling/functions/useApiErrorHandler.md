# Function: useApiErrorHandler()

> **useApiErrorHandler**(): `object`

Defined in: [frontend/src/lib/api/errorHandling.ts:360](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L360)

React hook for error handling with retry logic

## Returns

### createRetryHandler()

> **createRetryHandler**: (`operation`, `maxRetries`, `baseDelay`) => (`attemptCount`) => `Promise`\<`any`\>

#### Parameters

##### operation

() => `Promise`\<`any`\>

##### maxRetries

`number` = `3`

##### baseDelay

`number` = `1000`

#### Returns

> (`attemptCount`): `Promise`\<`any`\>

##### Parameters

###### attemptCount

`number` = `0`

##### Returns

`Promise`\<`any`\>

### getRetryDelay()

> **getRetryDelay**: (`attemptCount`, `baseDelay`) => `number`

Calculates retry delay with exponential backoff

#### Parameters

##### attemptCount

`number`

##### baseDelay

`number` = `1000`

#### Returns

`number`

### handleError()

> **handleError**: (`error`, `context?`) => [`ApiError`](../classes/ApiError.md)

#### Parameters

##### error

`unknown`

##### context?

`string`

#### Returns

[`ApiError`](../classes/ApiError.md)

### shouldRetry()

> **shouldRetry**: (`error`, `attemptCount`, `maxRetries`) => `boolean`

Determines if an error should trigger a retry

#### Parameters

##### error

[`ErrorInfo`](../interfaces/ErrorInfo.md)

##### attemptCount

`number`

##### maxRetries

`number` = `3`

#### Returns

`boolean`
