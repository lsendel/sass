# Function: useOptimisticList()

> **useOptimisticList**\<`T`\>(`initialData`): `object`

Defined in: [frontend/src/hooks/useOptimisticUpdates.ts:182](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useOptimisticUpdates.ts#L182)

## Type Parameters

### T

`T` *extends* `object`

## Parameters

### initialData

`T`[] = `[]`

## Returns

`object`

### addItem()

> **addItem**: (`item`, `mutationFn`, `index?`) => `Promise`\<`null` \| `T`\>

#### Parameters

##### item

`T`

##### mutationFn

(`item`) => `Promise`\<`T`\>

##### index?

`number`

#### Returns

`Promise`\<`null` \| `T`\>

### baseData

> **baseData**: `T`[]

### data

> **data**: `T`[] = `optimisticList`

### deleteItem()

> **deleteItem**: (`item`, `mutationFn`) => `Promise`\<`null` \| `void`\>

#### Parameters

##### item

`T`

##### mutationFn

(`id`) => `Promise`\<`void`\>

#### Returns

`Promise`\<`null` \| `void`\>

### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](../interfaces/OptimisticUpdate.md)\<\{ `index?`: `number`; `item`: `T`; `type`: `"delete"` \| `"add"` \| `"update"`; \}\>[]

### setData()

> **setData**: (`data`) => `void`

#### Parameters

##### data

`T`[]

#### Returns

`void`

### updateItem()

> **updateItem**: (`item`, `mutationFn`) => `Promise`\<`null` \| `T`\>

#### Parameters

##### item

`T`

##### mutationFn

(`item`) => `Promise`\<`T`\>

#### Returns

`Promise`\<`null` \| `T`\>
