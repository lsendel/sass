# Function: createMockApiResponse()

> **createMockApiResponse**\<`T`\>(`data`, `success`, `message?`): [`ApiSuccessResponse`](../../../../types/api/type-aliases/ApiSuccessResponse.md)\<`T`\> \| \{ `correlationId?`: `string`; `error`: \{ `code`: `string`; `details?`: `Record`\<`any`, `unknown`\>; `message`: `string`; \}; `success`: `false`; `timestamp`: `string`; \}

Defined in: [frontend/src/lib/api/testing.ts:30](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L30)

Creates a mock API response wrapper

## Type Parameters

### T

`T`

## Parameters

### data

`T`

### success

`boolean` = `true`

### message?

`string`

## Returns

[`ApiSuccessResponse`](../../../../types/api/type-aliases/ApiSuccessResponse.md)\<`T`\> \| \{ `correlationId?`: `string`; `error`: \{ `code`: `string`; `details?`: `Record`\<`any`, `unknown`\>; `message`: `string`; \}; `success`: `false`; `timestamp`: `string`; \}
