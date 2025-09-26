# Function: createValidatedQuery()

> **createValidatedQuery**\<`TResponse`, `TRequest`\>(`responseSchema`, `queryFn`): (`arg`) => `Promise`\<`TResponse`\>

Defined in: [frontend/src/lib/api/validation.ts:148](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L148)

Type-safe query hook factory with automatic validation

## Type Parameters

### TResponse

`TResponse`

### TRequest

`TRequest` = `void`

## Parameters

### responseSchema

`ZodType`\<`TResponse`\>

### queryFn

(`arg`) => `Promise`\<`unknown`\>

## Returns

> (`arg`): `Promise`\<`TResponse`\>

### Parameters

#### arg

`TRequest`

### Returns

`Promise`\<`TResponse`\>
