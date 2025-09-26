# Function: useOptimisticUpdates()

> **useOptimisticUpdates**\<`T`\>(): `object`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:21](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L21)

## Type Parameters

### T

`T`

## Returns

`object`

### addOptimisticUpdate()

> **addOptimisticUpdate**: \<`R`\>(`data`, `mutationFn`, `options`) => `Promise`\<`null` \| `R`\>

#### Type Parameters

##### R

`R`

#### Parameters

##### data

`T`

##### mutationFn

(`data`) => `Promise`\<`R`\>

##### options

[`OptimisticUpdateOptions`](../interfaces/OptimisticUpdateOptions.md)\<`T`\> = `{}`

#### Returns

`Promise`\<`null` \| `R`\>

### cancelOptimisticUpdate()

> **cancelOptimisticUpdate**: (`updateId`) => `void`

#### Parameters

##### updateId

`string`

#### Returns

`void`

### getFailedUpdates()

> **getFailedUpdates**: () => [`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<`T`\>[]

#### Returns

[`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<`T`\>[]

### getPendingUpdates()

> **getPendingUpdates**: () => [`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<`T`\>[]

#### Returns

[`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<`T`\>[]

### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

### optimisticData

> **optimisticData**: `T`[]

### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<`T`\>[]

### rollbackUpdate()

> **rollbackUpdate**: (`updateId`, `data`, `onError?`) => `void`

#### Parameters

##### updateId

`string`

##### data

`T`

##### onError?

(`error`, `rollbackData`) => `void`

#### Returns

`void`
