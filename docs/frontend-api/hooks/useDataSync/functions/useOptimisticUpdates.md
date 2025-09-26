# Function: useOptimisticUpdates()

> **useOptimisticUpdates**\<`T`\>(): `object`

Defined in: [frontend/src/hooks/useDataSync.ts:153](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useDataSync.ts#L153)

Hook for optimistic UI updates
Updates UI immediately while syncing with server in background

## Type Parameters

### T

`T`

## Returns

`object`

### addOptimisticUpdate()

> **addOptimisticUpdate**: (`key`, `data`) => `void`

#### Parameters

##### key

`string`

##### data

`T`

#### Returns

`void`

### clearOptimisticUpdates()

> **clearOptimisticUpdates**: () => `void`

#### Returns

`void`

### getOptimisticState()

> **getOptimisticState**: (`key`) => `undefined` \| `T`

#### Parameters

##### key

`string`

#### Returns

`undefined` \| `T`

### removeOptimisticUpdate()

> **removeOptimisticUpdate**: (`key`) => `void`

#### Parameters

##### key

`string`

#### Returns

`void`
