# Function: createValidatedMutation()

> **createValidatedMutation**\<`TResponse`, `TRequest`\>(`responseSchema`, `mutationFn`): (`arg`) => `Promise`\<`TResponse`\>

Defined in: [frontend/src/lib/api/validation.ts:168](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L168)

Type-safe mutation hook factory with automatic validation

## Type Parameters

### TResponse

`TResponse`

### TRequest

`TRequest` = `void`

## Parameters

### responseSchema

`ZodType`\<`TResponse`\>

### mutationFn

(`arg`) => `Promise`\<`unknown`\>

## Returns

> (`arg`): `Promise`\<`TResponse`\>

### Parameters

#### arg

`TRequest`

### Returns

`Promise`\<`TResponse`\>
