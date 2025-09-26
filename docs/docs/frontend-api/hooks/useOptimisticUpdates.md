[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useOptimisticUpdates

# hooks/useOptimisticUpdates

## Interfaces

### OptimisticUpdate

Defined in: [src/hooks/useOptimisticUpdates.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L13)

#### Type Parameters

##### T

`T`

#### Properties

##### data

> **data**: `T`

Defined in: [src/hooks/useOptimisticUpdates.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L15)

##### id

> **id**: `string`

Defined in: [src/hooks/useOptimisticUpdates.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L14)

##### status

> **status**: `"pending"` \| `"confirmed"` \| `"failed"` \| `"rolledBack"`

Defined in: [src/hooks/useOptimisticUpdates.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L17)

##### timestamp

> **timestamp**: `number`

Defined in: [src/hooks/useOptimisticUpdates.ts:16](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L16)

***

### OptimisticUpdateOptions

Defined in: [src/hooks/useOptimisticUpdates.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L5)

#### Type Parameters

##### T

`T`

#### Properties

##### errorMessage?

> `optional` **errorMessage**: `string`

Defined in: [src/hooks/useOptimisticUpdates.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L9)

##### onError()?

> `optional` **onError**: (`error`, `rollbackData`) => `void`

Defined in: [src/hooks/useOptimisticUpdates.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L7)

###### Parameters

###### error

`Error`

###### rollbackData

`T`

###### Returns

`void`

##### onSuccess()?

> `optional` **onSuccess**: (`data`) => `void`

Defined in: [src/hooks/useOptimisticUpdates.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L6)

###### Parameters

###### data

`T`

###### Returns

`void`

##### rollbackDelay?

> `optional` **rollbackDelay**: `number`

Defined in: [src/hooks/useOptimisticUpdates.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L10)

##### successMessage?

> `optional` **successMessage**: `string`

Defined in: [src/hooks/useOptimisticUpdates.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L8)

## Functions

### useOptimisticList()

> **useOptimisticList**\<`T`\>(`initialData`): `object`

Defined in: [src/hooks/useOptimisticUpdates.ts:181](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L181)

#### Type Parameters

##### T

`T` *extends* `object`

#### Parameters

##### initialData

`T`[] = `[]`

#### Returns

`object`

##### addItem()

> **addItem**: (`item`, `mutationFn`, `index?`) => `Promise`\<`null` \| `T`\>

###### Parameters

###### item

`T`

###### mutationFn

(`item`) => `Promise`\<`T`\>

###### index?

`number`

###### Returns

`Promise`\<`null` \| `T`\>

##### baseData

> **baseData**: `T`[]

##### data

> **data**: `T`[] = `optimisticList`

##### deleteItem()

> **deleteItem**: (`item`, `mutationFn`) => `Promise`\<`null` \| `void`\>

###### Parameters

###### item

`T`

###### mutationFn

(`id`) => `Promise`\<`void`\>

###### Returns

`Promise`\<`null` \| `void`\>

##### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

##### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](#optimisticupdate)\<\{ `index?`: `number`; `item`: `T`; `type`: `"delete"` \| `"add"` \| `"update"`; \}\>[]

##### setData()

> **setData**: (`data`) => `void`

###### Parameters

###### data

`T`[]

###### Returns

`void`

##### updateItem()

> **updateItem**: (`item`, `mutationFn`) => `Promise`\<`null` \| `T`\>

###### Parameters

###### item

`T`

###### mutationFn

(`item`) => `Promise`\<`T`\>

###### Returns

`Promise`\<`null` \| `T`\>

***

### useOptimisticUpdates()

> **useOptimisticUpdates**\<`T`\>(): `object`

Defined in: [src/hooks/useOptimisticUpdates.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useOptimisticUpdates.ts#L20)

#### Type Parameters

##### T

`T`

#### Returns

`object`

##### addOptimisticUpdate()

> **addOptimisticUpdate**: \<`R`\>(`data`, `mutationFn`, `options`) => `Promise`\<`null` \| `R`\>

###### Type Parameters

###### R

`R`

###### Parameters

###### data

`T`

###### mutationFn

(`data`) => `Promise`\<`R`\>

###### options

[`OptimisticUpdateOptions`](#optimisticupdateoptions)\<`T`\> = `{}`

###### Returns

`Promise`\<`null` \| `R`\>

##### cancelOptimisticUpdate()

> **cancelOptimisticUpdate**: (`updateId`) => `void`

###### Parameters

###### updateId

`string`

###### Returns

`void`

##### getFailedUpdates()

> **getFailedUpdates**: () => [`OptimisticUpdate`](#optimisticupdate)\<`T`\>[]

###### Returns

[`OptimisticUpdate`](#optimisticupdate)\<`T`\>[]

##### getPendingUpdates()

> **getPendingUpdates**: () => [`OptimisticUpdate`](#optimisticupdate)\<`T`\>[]

###### Returns

[`OptimisticUpdate`](#optimisticupdate)\<`T`\>[]

##### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

##### optimisticData

> **optimisticData**: `T`[]

##### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](#optimisticupdate)\<`T`\>[]

##### rollbackUpdate()

> **rollbackUpdate**: (`updateId`, `data`, `onError?`) => `void`

###### Parameters

###### updateId

`string`

###### data

`T`

###### onError?

(`error`, `rollbackData`) => `void`

###### Returns

`void`
