# Class: ApiError

Defined in: [frontend/src/lib/api/errorHandling.ts:19](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L19)

## Extends

- `Error`

## Constructors

### Constructor

> **new ApiError**(`message`, `info`, `originalError?`): `ApiError`

Defined in: [frontend/src/lib/api/errorHandling.ts:20](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L20)

#### Parameters

##### message

`string`

##### info

[`ErrorInfo`](../interfaces/ErrorInfo.md)

##### originalError?

`Error`

#### Returns

`ApiError`

#### Overrides

`Error.constructor`

## Properties

### info

> `readonly` **info**: [`ErrorInfo`](../interfaces/ErrorInfo.md)

Defined in: [frontend/src/lib/api/errorHandling.ts:22](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L22)

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

### originalError?

> `readonly` `optional` **originalError**: `Error`

Defined in: [frontend/src/lib/api/errorHandling.ts:23](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/errorHandling.ts#L23)

***

### stack?

> `optional` **stack**: `string`

Defined in: docs/node\_modules/typescript/lib/lib.es5.d.ts:1078

#### Inherited from

`Error.stack`
