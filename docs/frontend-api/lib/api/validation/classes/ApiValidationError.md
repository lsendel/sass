# Class: ApiValidationError

Defined in: [frontend/src/lib/api/validation.ts:12](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L12)

Runtime API validation utilities using Zod
Provides type-safe API responses with runtime validation

## Extends

- `Error`

## Constructors

### Constructor

> **new ApiValidationError**(`message`, `originalError`, `response`): `ApiValidationError`

Defined in: [frontend/src/lib/api/validation.ts:13](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L13)

#### Parameters

##### message

`string`

##### originalError

`ZodError`

##### response

`unknown`

#### Returns

`ApiValidationError`

#### Overrides

`Error.constructor`

## Properties

### message

> **message**: `string`

Defined in: docs/node\_modules/typescript/lib/lib.es5.d.ts:1077

#### Inherited from

`Error.message`

***

### name

> **name**: `string`

Defined in: docs/node\_modules/typescript/lib/lib.es5.d.ts:1076

#### Inherited from

`Error.name`

***

### originalError

> `readonly` **originalError**: `ZodError`

Defined in: [frontend/src/lib/api/validation.ts:15](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L15)

***

### response

> `readonly` **response**: `unknown`

Defined in: [frontend/src/lib/api/validation.ts:16](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L16)

***

### stack?

> `optional` **stack**: `string`

Defined in: docs/node\_modules/typescript/lib/lib.es5.d.ts:1078

#### Inherited from

`Error.stack`
