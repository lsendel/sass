# Interface: ValidatedQueryState\<T\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:11](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L11)

React hooks for validated API calls with error handling and retry logic

## Type Parameters

### T

`T`

## Properties

### data

> **data**: `null` \| `T`

Defined in: [frontend/src/hooks/useValidatedApi.ts:12](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L12)

***

### error

> **error**: `null` \| `Error`

Defined in: [frontend/src/hooks/useValidatedApi.ts:15](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L15)

***

### isError

> **isError**: `boolean`

Defined in: [frontend/src/hooks/useValidatedApi.ts:14](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L14)

***

### isLoading

> **isLoading**: `boolean`

Defined in: [frontend/src/hooks/useValidatedApi.ts:13](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L13)

***

### isValidating

> **isValidating**: `boolean`

Defined in: [frontend/src/hooks/useValidatedApi.ts:16](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L16)

***

### refetch()

> **refetch**: () => `Promise`\<`void`\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:17](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L17)

#### Returns

`Promise`\<`void`\>
