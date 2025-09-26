# Function: useValidatedQuery()

> **useValidatedQuery**\<`T`\>(`queryKey`, `queryFn`, `responseSchema`, `options`): [`ValidatedQueryState`](../interfaces/ValidatedQueryState.md)\<`T`\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:42](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L42)

Hook for validated queries with automatic caching and error handling

## Type Parameters

### T

`T`

## Parameters

### queryKey

`string`

### queryFn

() => `Promise`\<`unknown`\>

### responseSchema

`ZodType`\<`T`\>

### options

[`ValidatedApiOptions`](../interfaces/ValidatedApiOptions.md) = `{}`

## Returns

[`ValidatedQueryState`](../interfaces/ValidatedQueryState.md)\<`T`\>
