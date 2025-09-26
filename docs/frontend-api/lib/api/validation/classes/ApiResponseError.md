# Class: ApiResponseError

Defined in: [frontend/src/lib/api/validation.ts:23](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L23)

## Extends

- `Error`

## Constructors

### Constructor

> **new ApiResponseError**(`message`, `status`, `data`): `ApiResponseError`

Defined in: [frontend/src/lib/api/validation.ts:24](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L24)

#### Parameters

##### message

`string`

##### status

`number`

##### data

`unknown`

#### Returns

`ApiResponseError`

#### Overrides

`Error.constructor`

## Properties

### data

> `readonly` **data**: `unknown`

Defined in: [frontend/src/lib/api/validation.ts:27](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L27)

***

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

### stack?

> `optional` **stack**: `string`

Defined in: docs/node\_modules/typescript/lib/lib.es5.d.ts:1078

#### Inherited from

`Error.stack`

***

### status

> `readonly` **status**: `number`

Defined in: [frontend/src/lib/api/validation.ts:26](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L26)
