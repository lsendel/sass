# Interface: OptimisticUpdateOptions\<T\>

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:6](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L6)

## Type Parameters

### T

`T`

## Properties

### errorMessage?

> `optional` **errorMessage**: `string`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:10](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L10)

***

### onError()?

> `optional` **onError**: (`error`, `rollbackData`) => `void`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:8](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L8)

#### Parameters

##### error

`Error`

##### rollbackData

`T`

#### Returns

`void`

***

### onSuccess()?

> `optional` **onSuccess**: (`data`) => `void`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:7](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L7)

#### Parameters

##### data

`T`

#### Returns

`void`

***

### rollbackDelay?

> `optional` **rollbackDelay**: `number`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:11](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L11)

***

### successMessage?

> `optional` **successMessage**: `string`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:9](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L9)
