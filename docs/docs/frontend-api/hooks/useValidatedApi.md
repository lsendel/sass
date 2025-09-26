[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useValidatedApi

# hooks/useValidatedApi

## Interfaces

### ValidatedApiOptions

Defined in: [src/hooks/useValidatedApi.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L28)

#### Properties

##### enabled?

> `optional` **enabled**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:29](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L29)

##### maxRetries?

> `optional` **maxRetries**: `number`

Defined in: [src/hooks/useValidatedApi.ts:31](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L31)

##### onError()?

> `optional` **onError**: (`error`) => `void`

Defined in: [src/hooks/useValidatedApi.ts:34](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L34)

###### Parameters

###### error

`Error`

###### Returns

`void`

##### onSuccess()?

> `optional` **onSuccess**: (`data`) => `void`

Defined in: [src/hooks/useValidatedApi.ts:33](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L33)

###### Parameters

###### data

`any`

###### Returns

`void`

##### retry?

> `optional` **retry**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L30)

##### retryDelay?

> `optional` **retryDelay**: `number`

Defined in: [src/hooks/useValidatedApi.ts:32](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L32)

##### staleTime?

> `optional` **staleTime**: `number`

Defined in: [src/hooks/useValidatedApi.ts:35](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L35)

***

### ValidatedMutationState

Defined in: [src/hooks/useValidatedApi.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L19)

#### Type Parameters

##### T

`T`

#### Properties

##### data

> **data**: `null` \| `T`

Defined in: [src/hooks/useValidatedApi.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L20)

##### error

> **error**: `null` \| `Error`

Defined in: [src/hooks/useValidatedApi.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L23)

##### isError

> **isError**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L22)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L21)

##### mutate()

> **mutate**: (`variables?`) => `Promise`\<`T`\>

Defined in: [src/hooks/useValidatedApi.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L24)

###### Parameters

###### variables?

`any`

###### Returns

`Promise`\<`T`\>

##### reset()

> **reset**: () => `void`

Defined in: [src/hooks/useValidatedApi.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L25)

###### Returns

`void`

***

### ValidatedQueryState

Defined in: [src/hooks/useValidatedApi.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L10)

React hooks for validated API calls with error handling and retry logic

#### Type Parameters

##### T

`T`

#### Properties

##### data

> **data**: `null` \| `T`

Defined in: [src/hooks/useValidatedApi.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L11)

##### error

> **error**: `null` \| `Error`

Defined in: [src/hooks/useValidatedApi.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L14)

##### isError

> **isError**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L13)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L12)

##### isValidating

> **isValidating**: `boolean`

Defined in: [src/hooks/useValidatedApi.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L15)

##### refetch()

> **refetch**: () => `Promise`\<`void`\>

Defined in: [src/hooks/useValidatedApi.ts:16](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L16)

###### Returns

`Promise`\<`void`\>

## Functions

### useValidatedInfiniteQuery()

> **useValidatedInfiniteQuery**\<`T`\>(`queryKey`, `queryFn`, `responseSchema`, `options`): `object`

Defined in: [src/hooks/useValidatedApi.ts:262](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L262)

Hook for infinite queries with validation

#### Type Parameters

##### T

`T`

#### Parameters

##### queryKey

`string`

##### queryFn

(`pageParam`) => `Promise`\<`unknown`\>

##### responseSchema

`ZodType`\<`T`\>

##### options

[`ValidatedApiOptions`](#validatedapioptions) & `object` = `{}`

#### Returns

`object`

##### data

> **data**: `T`[] = `pages`

##### error

> **error**: `null` \| `Error`

##### fetchNextPage()

> **fetchNextPage**: () => `void`

###### Returns

`void`

##### fetchPreviousPage()

> **fetchPreviousPage**: () => `void`

###### Returns

`void`

##### hasNextPage

> **hasNextPage**: `boolean`

##### hasPreviousPage

> **hasPreviousPage**: `boolean`

##### isError

> **isError**: `boolean`

##### isFetchingNextPage

> **isFetchingNextPage**: `boolean`

##### isFetchingPreviousPage

> **isFetchingPreviousPage**: `boolean`

##### isLoading

> **isLoading**: `boolean`

***

### useValidatedMutation()

> **useValidatedMutation**\<`TRequest`, `TResponse`\>(`mutationFn`, `requestSchema`, `responseSchema`, `options`): [`ValidatedMutationState`](#validatedmutationstate)\<`TResponse`\>

Defined in: [src/hooks/useValidatedApi.ts:147](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L147)

Hook for validated mutations with optimistic updates

#### Type Parameters

##### TRequest

`TRequest`

##### TResponse

`TResponse`

#### Parameters

##### mutationFn

(`variables`) => `Promise`\<`unknown`\>

##### requestSchema

`ZodType`\<`TRequest`\>

##### responseSchema

`ZodType`\<`TResponse`\>

##### options

[`ValidatedApiOptions`](#validatedapioptions) & `object` = `{}`

#### Returns

[`ValidatedMutationState`](#validatedmutationstate)\<`TResponse`\>

***

### useValidatedQuery()

> **useValidatedQuery**\<`T`\>(`queryKey`, `queryFn`, `responseSchema`, `options`): [`ValidatedQueryState`](#validatedquerystate)\<`T`\>

Defined in: [src/hooks/useValidatedApi.ts:41](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L41)

Hook for validated queries with automatic caching and error handling

#### Type Parameters

##### T

`T`

#### Parameters

##### queryKey

`string`

##### queryFn

() => `Promise`\<`unknown`\>

##### responseSchema

`ZodType`\<`T`\>

##### options

[`ValidatedApiOptions`](#validatedapioptions) = `{}`

#### Returns

[`ValidatedQueryState`](#validatedquerystate)\<`T`\>

***

### useValidatedSubscription()

> **useValidatedSubscription**\<`T`\>(`subscriptionKey`, `subscribe`, `responseSchema`, `options`): `object`

Defined in: [src/hooks/useValidatedApi.ts:379](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useValidatedApi.ts#L379)

Hook for real-time data with validation

#### Type Parameters

##### T

`T`

#### Parameters

##### subscriptionKey

`string`

##### subscribe

(`callback`) => () => `void`

##### responseSchema

`ZodType`\<`T`\>

##### options

[`ValidatedApiOptions`](#validatedapioptions) = `{}`

#### Returns

`object`

##### data

> **data**: `null` \| `T`

##### error

> **error**: `null` \| `Error`

##### isConnected

> **isConnected**: `boolean`
