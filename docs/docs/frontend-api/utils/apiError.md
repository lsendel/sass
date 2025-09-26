[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / utils/apiError

# utils/apiError

## Type Aliases

### ParsedApiError

> **ParsedApiError** = `object`

Defined in: [src/utils/apiError.ts:4](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiError.ts#L4)

#### Properties

##### message

> **message**: `string`

Defined in: [src/utils/apiError.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiError.ts#L6)

##### status?

> `optional` **status**: `number`

Defined in: [src/utils/apiError.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiError.ts#L5)

## Functions

### parseApiError()

> **parseApiError**(`err`): [`ParsedApiError`](#parsedapierror)

Defined in: [src/utils/apiError.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiError.ts#L9)

#### Parameters

##### err

`unknown`

#### Returns

[`ParsedApiError`](#parsedapierror)
