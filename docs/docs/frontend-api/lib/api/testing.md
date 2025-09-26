[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / lib/api/testing

# lib/api/testing

## Classes

### MockApiClient

Defined in: [src/lib/api/testing.ts:315](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L315)

Mock API client for testing

#### Constructors

##### Constructor

> **new MockApiClient**(): [`MockApiClient`](#mockapiclient)

###### Returns

[`MockApiClient`](#mockapiclient)

#### Methods

##### request()

> **request**\<`T`\>(`endpoint`, `schema`): `Promise`\<`T`\>

Defined in: [src/lib/api/testing.ts:330](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L330)

###### Type Parameters

###### T

`T`

###### Parameters

###### endpoint

`string`

###### schema

`ZodType`\<`T`\>

###### Returns

`Promise`\<`T`\>

##### reset()

> **reset**(): `void`

Defined in: [src/lib/api/testing.ts:352](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L352)

###### Returns

`void`

##### setResponse()

> **setResponse**(`endpoint`, `response`, `delay?`, `errorRate?`): `void`

Defined in: [src/lib/api/testing.ts:320](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L320)

###### Parameters

###### endpoint

`string`

###### response

`any`

###### delay?

`number`

###### errorRate?

`number`

###### Returns

`void`

## Interfaces

### MockApiOptions

Defined in: [src/lib/api/testing.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L10)

Testing utilities for API validation and error handling

#### Properties

##### errorRate?

> `optional` **errorRate**: `number`

Defined in: [src/lib/api/testing.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L14)

##### includeOptionalFields?

> `optional` **includeOptionalFields**: `boolean`

Defined in: [src/lib/api/testing.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L11)

##### responseDelay?

> `optional` **responseDelay**: `number`

Defined in: [src/lib/api/testing.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L13)

##### useInvalidData?

> `optional` **useInvalidData**: `boolean`

Defined in: [src/lib/api/testing.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L12)

***

### TestScenario

Defined in: [src/lib/api/testing.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L17)

#### Type Parameters

##### T

`T`

#### Properties

##### expectedData?

> `optional` **expectedData**: `T`

Defined in: [src/lib/api/testing.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L21)

##### expectedError?

> `optional` **expectedError**: `string`

Defined in: [src/lib/api/testing.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L22)

##### expectedValid

> **expectedValid**: `boolean`

Defined in: [src/lib/api/testing.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L20)

##### input

> **input**: `unknown`

Defined in: [src/lib/api/testing.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L19)

##### name

> **name**: `string`

Defined in: [src/lib/api/testing.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L18)

## Functions

### benchmarkValidation()

> **benchmarkValidation**\<`T`\>(`schema`, `data`, `iterations`): `Promise`\<\{ `averageTime`: `number`; `iterations`: `number`; `maxTime`: `number`; `minTime`: `number`; `totalTime`: `number`; \}\>

Defined in: [src/lib/api/testing.ts:362](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L362)

Performance testing utility

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### data

`unknown`

##### iterations

`number` = `1000`

#### Returns

`Promise`\<\{ `averageTime`: `number`; `iterations`: `number`; `maxTime`: `number`; `minTime`: `number`; `totalTime`: `number`; \}\>

***

### createMockApiResponse()

> **createMockApiResponse**\<`T`\>(`data`, `success`, `message?`): [`ApiSuccessResponse`](../../types/api.md#apisuccessresponse)\<`T`\> \| \{ `correlationId?`: `string`; `error`: \{ `code`: `string`; `details?`: `Record`\<`any`, `unknown`\>; `message`: `string`; \}; `success`: `false`; `timestamp`: `string`; \}

Defined in: [src/lib/api/testing.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L28)

Creates a mock API response wrapper

#### Type Parameters

##### T

`T`

#### Parameters

##### data

`T`

##### success

`boolean` = `true`

##### message?

`string`

#### Returns

[`ApiSuccessResponse`](../../types/api.md#apisuccessresponse)\<`T`\> \| \{ `correlationId?`: `string`; `error`: \{ `code`: `string`; `details?`: `Record`\<`any`, `unknown`\>; `message`: `string`; \}; `success`: `false`; `timestamp`: `string`; \}

***

### createValidationTestSuite()

> **createValidationTestSuite**\<`T`\>(`schema`, `suiteName`): [`TestScenario`](#testscenario)\<`T`\>[]

Defined in: [src/lib/api/testing.ts:187](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L187)

Creates a comprehensive test suite for a schema

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### suiteName

`string`

#### Returns

[`TestScenario`](#testscenario)\<`T`\>[]

***

### generateRealisticMockData()

> **generateRealisticMockData**\<`T`\>(`schema`): `T`

Defined in: [src/lib/api/testing.ts:59](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L59)

Generates mock data for a given schema with realistic values

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

#### Returns

`T`

***

### runValidationTests()

> **runValidationTests**\<`T`\>(`schema`, `scenarios`, `verbose`): `object`

Defined in: [src/lib/api/testing.ts:239](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L239)

Runs validation test scenarios

#### Type Parameters

##### T

`T`

#### Parameters

##### schema

`ZodType`\<`T`\>

##### scenarios

[`TestScenario`](#testscenario)\<`T`\>[]

##### verbose

`boolean` = `false`

#### Returns

`object`

##### failed

> **failed**: `number`

##### passed

> **passed**: `number`

##### results

> **results**: `any`[]

***

### testApiEndpoint()

> **testApiEndpoint**\<`TRequest`, `TResponse`\>(`endpoint`, `requestSchema`, `responseSchema`, `testCases`): `Promise`\<`void`\>

Defined in: [src/lib/api/testing.ts:403](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L403)

Integration test helper for API endpoints

#### Type Parameters

##### TRequest

`TRequest`

##### TResponse

`TResponse`

#### Parameters

##### endpoint

(`request`) => `Promise`\<`TResponse`\>

##### requestSchema

`ZodType`\<`TRequest`\>

##### responseSchema

`ZodType`\<`TResponse`\>

##### testCases

`object`[]

#### Returns

`Promise`\<`void`\>

***

### testErrorHandling()

> **testErrorHandling**(`errors`): `void`

Defined in: [src/lib/api/testing.ts:441](https://github.com/lsendel/sass/blob/main/frontend/src/lib/api/testing.ts#L441)

Error handling test utility

#### Parameters

##### errors

`unknown`[]

#### Returns

`void`
