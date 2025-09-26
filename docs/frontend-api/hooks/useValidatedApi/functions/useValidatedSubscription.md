# Function: useValidatedSubscription()

> **useValidatedSubscription**\<`T`\>(`subscriptionKey`, `subscribe`, `responseSchema`, `options`): `object`

Defined in: [frontend/src/hooks/useValidatedApi.ts:380](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L380)

Hook for real-time data with validation

## Type Parameters

### T

`T`

## Parameters

### subscriptionKey

`string`

### subscribe

(`callback`) => () => `void`

### responseSchema

`ZodType`\<`T`\>

### options

[`ValidatedApiOptions`](../interfaces/ValidatedApiOptions.md) = `{}`

## Returns

`object`

### data

> **data**: `null` \| `T`

### error

> **error**: `null` \| `Error`

### isConnected

> **isConnected**: `boolean`
