# Function: shouldRetry()

> **shouldRetry**(`error`, `attemptCount`, `maxRetries`): `boolean`

Defined in: [frontend/src/lib/api/errorHandling.ts:332](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L332)

Determines if an error should trigger a retry

## Parameters

### error

[`ErrorInfo`](../interfaces/ErrorInfo.md)

### attemptCount

`number`

### maxRetries

`number` = `3`

## Returns

`boolean`
