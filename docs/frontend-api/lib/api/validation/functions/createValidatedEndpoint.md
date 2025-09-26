# Function: createValidatedEndpoint()

> **createValidatedEndpoint**\<`TResponse`, `TRequest`\>(`responseSchema`, `endpoint`): `object`

Defined in: [frontend/src/lib/api/validation.ts:99](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L99)

Creates a validated endpoint that automatically validates responses

## Type Parameters

### TResponse

`TResponse`

### TRequest

`TRequest` = `void`

## Parameters

### responseSchema

`ZodType`\<`TResponse`\>

### endpoint

#### method?

`string`

#### query

(`arg`) => `any`

## Returns

`object`

### method?

> `optional` **method**: `string`

### query()

> **query**: (`arg`) => `any`

#### Parameters

##### arg

`TRequest`

#### Returns

`any`

### transformResponse()

> **transformResponse**: (`response`, `meta`, `arg`) => `any`

#### Parameters

##### response

`unknown`

##### meta

`any`

##### arg

`TRequest`

#### Returns

`any`
