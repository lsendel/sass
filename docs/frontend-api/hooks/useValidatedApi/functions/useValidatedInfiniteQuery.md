# Function: useValidatedInfiniteQuery()

> **useValidatedInfiniteQuery**\<`T`\>(`queryKey`, `queryFn`, `responseSchema`, `options`): `object`

Defined in: [frontend/src/hooks/useValidatedApi.ts:263](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L263)

Hook for infinite queries with validation

## Type Parameters

### T

`T`

## Parameters

### queryKey

`string`

### queryFn

(`pageParam`) => `Promise`\<`unknown`\>

### responseSchema

`ZodType`\<`T`\>

### options

[`ValidatedApiOptions`](../interfaces/ValidatedApiOptions.md) & `object` = `{}`

## Returns

`object`

### data

> **data**: `T`[] = `pages`

### error

> **error**: `null` \| `Error`

### fetchNextPage()

> **fetchNextPage**: () => `void`

#### Returns

`void`

### fetchPreviousPage()

> **fetchPreviousPage**: () => `void`

#### Returns

`void`

### hasNextPage

> **hasNextPage**: `boolean`

### hasPreviousPage

> **hasPreviousPage**: `boolean`

### isError

> **isError**: `boolean`

### isFetchingNextPage

> **isFetchingNextPage**: `boolean`

### isFetchingPreviousPage

> **isFetchingPreviousPage**: `boolean`

### isLoading

> **isLoading**: `boolean`
