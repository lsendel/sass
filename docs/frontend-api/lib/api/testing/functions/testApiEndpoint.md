# Function: testApiEndpoint()

> **testApiEndpoint**\<`TRequest`, `TResponse`\>(`endpoint`, `requestSchema`, `responseSchema`, `testCases`): `Promise`\<`void`\>

Defined in: [frontend/src/lib/api/testing.ts:405](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L405)

Integration test helper for API endpoints

## Type Parameters

### TRequest

`TRequest`

### TResponse

`TResponse`

## Parameters

### endpoint

(`request`) => `Promise`\<`TResponse`\>

### requestSchema

`ZodType`\<`TRequest`\>

### responseSchema

`ZodType`\<`TResponse`\>

### testCases

`object`[]

## Returns

`Promise`\<`void`\>
