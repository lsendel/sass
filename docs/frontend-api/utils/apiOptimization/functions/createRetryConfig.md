# Function: createRetryConfig()

> **createRetryConfig**(`maxRetries`): `object`

Defined in: [frontend/src/utils/apiOptimization.ts:109](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L109)

## Parameters

### maxRetries

`number` = `3`

## Returns

`object`

### maxRetries

> **maxRetries**: `number`

### retryCondition()

> **retryCondition**: (`error`) => `boolean`

#### Parameters

##### error

`any`

#### Returns

`boolean`

### retryDelay()

> **retryDelay**: (`retryAttempt`) => `number`

#### Parameters

##### retryAttempt

`number`

#### Returns

`number`
