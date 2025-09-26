# Function: useValidatedMutation()

> **useValidatedMutation**\<`TRequest`, `TResponse`\>(`mutationFn`, `requestSchema`, `responseSchema`, `options`): [`ValidatedMutationState`](../interfaces/ValidatedMutationState.md)\<`TResponse`\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:148](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L148)

Hook for validated mutations with optimistic updates

## Type Parameters

### TRequest

`TRequest`

### TResponse

`TResponse`

## Parameters

### mutationFn

(`variables`) => `Promise`\<`unknown`\>

### requestSchema

`ZodType`\<`TRequest`\>

### responseSchema

`ZodType`\<`TResponse`\>

### options

[`ValidatedApiOptions`](../interfaces/ValidatedApiOptions.md) & `object` = `{}`

## Returns

[`ValidatedMutationState`](../interfaces/ValidatedMutationState.md)\<`TResponse`\>
