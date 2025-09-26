# Class: MockApiClient

Defined in: [frontend/src/lib/api/testing.ts:317](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L317)

Mock API client for testing

## Constructors

### Constructor

> **new MockApiClient**(): `MockApiClient`

#### Returns

`MockApiClient`

## Methods

### request()

> **request**\<`T`\>(`endpoint`, `schema`): `Promise`\<`T`\>

Defined in: [frontend/src/lib/api/testing.ts:332](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L332)

#### Type Parameters

##### T

`T`

#### Parameters

##### endpoint

`string`

##### schema

`ZodType`\<`T`\>

#### Returns

`Promise`\<`T`\>

***

### reset()

> **reset**(): `void`

Defined in: [frontend/src/lib/api/testing.ts:354](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L354)

#### Returns

`void`

***

### setResponse()

> **setResponse**(`endpoint`, `response`, `delay?`, `errorRate?`): `void`

Defined in: [frontend/src/lib/api/testing.ts:322](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L322)

#### Parameters

##### endpoint

`string`

##### response

`any`

##### delay?

`number`

##### errorRate?

`number`

#### Returns

`void`
