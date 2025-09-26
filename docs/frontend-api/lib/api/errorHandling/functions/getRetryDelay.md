# Function: getRetryDelay()

> **getRetryDelay**(`attemptCount`, `baseDelay`): `number`

Defined in: [frontend/src/lib/api/errorHandling.ts:353](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L353)

Calculates retry delay with exponential backoff

## Parameters

### attemptCount

`number`

### baseDelay

`number` = `1000`

## Returns

`number`
