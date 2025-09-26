[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / lib/api/validation

# lib/api/validation

## Classes

### ApiResponseError

Defined in: [src/lib/api/validation.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L22)

#### Extends

- `Error`

#### Constructors

##### Constructor

> **new ApiResponseError**(`message`, `status`, `data`): [`ApiResponseError`](#apiresponseerror)

Defined in: [src/lib/api/validation.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L23)

###### Parameters

###### message

`string`

###### status

`number`

###### data

`unknown`

###### Returns

[`ApiResponseError`](#apiresponseerror)

###### Overrides

`Error.constructor`

#### Properties

##### data

> `readonly` **data**: `unknown`

Defined in: [src/lib/api/validation.ts:26](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L26)

##### status

> `readonly` **status**: `number`

Defined in: [src/lib/api/validation.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L25)

***

### ApiValidationError

Defined in: [src/lib/api/validation.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L11)

Runtime API validation utilities using Zod
Provides type-safe API responses with runtime validation

#### Extends

- `Error`

#### Constructors

##### Constructor

> **new ApiValidationError**(`message`, `originalError`, `response`): [`ApiValidationError`](#apivalidationerror)

Defined in: [src/lib/api/validation.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L12)

###### Parameters

###### message

`string`

###### originalError

`ZodError`

###### response

`unknown`

###### Returns

[`ApiValidationError`](#apivalidationerror)

###### Overrides

`Error.constructor`

#### Properties

##### originalError

> `readonly` **originalError**: `ZodError`

Defined in: [src/lib/api/validation.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L14)

##### response

> `readonly` **response**: `unknown`

Defined in: [src/lib/api/validation.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L15)

## Functions

### createValidatedBaseQuery()

> **createValidatedBaseQuery**(`baseUrl`, `options?`): `BaseQueryFn`

Defined in: [src/lib/api/validation.ts:64](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L64)

Creates a validated RTK Query base query with automatic response validation

#### Parameters

##### baseUrl

`string`

##### options?

`FetchBaseQueryArgs`

#### Returns

`BaseQueryFn`

***

### createValidatedEndpoint()

> **createValidatedEndpoint**\<`TResponse`, `TRequest`\>(`responseSchema`, `endpoint`): `object`

Defined in: [src/lib/api/validation.ts:98](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L98)

Creates a validated endpoint that automatically validates responses

#### Type Parameters

##### TResponse

`TResponse`

##### TRequest

`TRequest` = `void`

#### Parameters

##### responseSchema

`ZodType`\<`TResponse`\>

##### endpoint

###### method?

`string`

###### query

(`arg`) => `any`

#### Returns

`object`

##### method?

> `optional` **method**: `string`

##### query()

> **query**: (`arg`) => `any`

###### Parameters

###### arg

`TRequest`

###### Returns

`any`

##### transformResponse()

> **transformResponse**: (`response`, `meta`, `arg`) => `any`

###### Parameters

###### response

`unknown`

###### meta

`any`

###### arg

`TRequest`

###### Returns

`any`

***

### createValidatedMutation()

> **createValidatedMutation**\<`TResponse`, `TRequest`\>(`responseSchema`, `mutationFn`): (`arg`) => `Promise`\<`TResponse`\>

Defined in: [src/lib/api/validation.ts:167](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L167)

Type-safe mutation hook factory with automatic validation

#### Type Parameters

##### TResponse

`TResponse`

##### TRequest

`TRequest` = `void`

#### Parameters

##### responseSchema

`ZodType`\<`TResponse`\>

##### mutationFn

(`arg`) => `Promise`\<`unknown`\>

#### Returns

> (`arg`): `Promise`\<`TResponse`\>

##### Parameters

###### arg

`TRequest`

##### Returns

`Promise`\<`TResponse`\>

***

### createValidatedQuery()

> **createValidatedQuery**\<`TResponse`, `TRequest`\>(`responseSchema`, `queryFn`): (`arg`) => `Promise`\<`TResponse`\>

Defined in: [src/lib/api/validation.ts:147](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L147)

Type-safe query hook factory with automatic validation

#### Type Parameters

##### TResponse

`TResponse`

##### TRequest

`TRequest` = `void`

#### Parameters

##### responseSchema

`ZodType`\<`TResponse`\>

##### queryFn

(`arg`) => `Promise`\<`unknown`\>

#### Returns

> (`arg`): `Promise`\<`TResponse`\>

##### Parameters

###### arg

`TRequest`

##### Returns

`Promise`\<`TResponse`\>

***

### generateMockData()

> **generateMockData**\<`T`\>(`schema`): `T`

Defined in: [src/lib/api/validation.ts:249](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L249)

Mock data generator for development and testing

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

#### Returns

`T`

***

### safeParseApiResponse()

> **safeParseApiResponse**\<`T`\>(`schema`, `data`, `fallback?`): \{ `data`: `T`; `success`: `true`; \} \| \{ `error`: `ZodError`; `fallback?`: `T`; `success`: `false`; \}

Defined in: [src/lib/api/validation.ts:223](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L223)

Utility to safely parse API responses with fallback

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### data

`unknown`

##### fallback?

`T`

#### Returns

\{ `data`: `T`; `success`: `true`; \} \| \{ `error`: `ZodError`; `fallback?`: `T`; `success`: `false`; \}

***

### validateApiResponse()

> **validateApiResponse**\<`T`\>(`schema`, `data`, `context?`): `T`

Defined in: [src/lib/api/validation.ts:36](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L36)

Validates API response data against a Zod schema

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### data

`unknown`

##### context?

`string`

#### Returns

`T`

***

### validateResponseInDev()

> **validateResponseInDev**\<`T`\>(`schema`, `data`, `endpoint`): `T`

Defined in: [src/lib/api/validation.ts:187](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L187)

Development mode response validator for debugging

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### data

`unknown`

##### endpoint

`string`

#### Returns

`T`

***

### wrapSuccessResponse()

> **wrapSuccessResponse**\<`T`\>(`dataSchema`): `ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [src/lib/api/validation.ts:140](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/validation.ts#L140)

Wraps a success response schema with the standard API wrapper

#### Type Parameters

##### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

#### Parameters

##### dataSchema

`T`

#### Returns

`ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>
