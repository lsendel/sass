# Interface: ValidatedMutationState\<T\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:20](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L20)

## Type Parameters

### T

`T`

## Properties

### data

> **data**: `null` \| `T`

Defined in: [frontend/src/hooks/useValidatedApi.ts:21](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L21)

***

### error

> **error**: `null` \| `Error`

Defined in: [frontend/src/hooks/useValidatedApi.ts:24](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L24)

***

### isError

> **isError**: `boolean`

Defined in: [frontend/src/hooks/useValidatedApi.ts:23](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L23)

***

### isLoading

> **isLoading**: `boolean`

Defined in: [frontend/src/hooks/useValidatedApi.ts:22](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L22)

***

### mutate()

> **mutate**: (`variables?`) => `Promise`\<`T`\>

Defined in: [frontend/src/hooks/useValidatedApi.ts:25](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L25)

#### Parameters

##### variables?

`any`

#### Returns

`Promise`\<`T`\>

***

### reset()

> **reset**: () => `void`

Defined in: [frontend/src/hooks/useValidatedApi.ts:26](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useValidatedApi.ts#L26)

#### Returns

`void`
