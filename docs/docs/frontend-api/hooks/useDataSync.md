[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useDataSync

# hooks/useDataSync

## Functions

### useCrossComponentSync()

> **useCrossComponentSync**(): `object`

Defined in: [src/hooks/useDataSync.ts:106](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useDataSync.ts#L106)

Hook for cross-component state synchronization
Useful when multiple components need to stay in sync with shared data

#### Returns

`object`

##### syncAllData()

> **syncAllData**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### syncOrganizationData()

> **syncOrganizationData**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### syncPaymentData()

> **syncPaymentData**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### syncSubscriptionData()

> **syncSubscriptionData**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### syncUserData()

> **syncUserData**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

***

### useDataSync()

> **useDataSync**(`options`): `object`

Defined in: [src/hooks/useDataSync.ts:35](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useDataSync.ts#L35)

Custom hook for synchronizing data across components
Helps maintain consistency when data changes in one component affect others

#### Parameters

##### options

`DataSyncOptions`

#### Returns

`object`

##### lastSync

> **lastSync**: `Record`\<`string`, `any`\> = `lastSyncRef.current`

##### triggerSync()

> **triggerSync**: (`data`) => `void`

###### Parameters

###### data

`Record`\<`string`, `any`\>

###### Returns

`void`

***

### useOptimisticUpdates()

> **useOptimisticUpdates**\<`T`\>(): `object`

Defined in: [src/hooks/useDataSync.ts:152](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useDataSync.ts#L152)

Hook for optimistic UI updates
Updates UI immediately while syncing with server in background

#### Type Parameters

##### T

`T`

#### Returns

`object`

##### addOptimisticUpdate()

> **addOptimisticUpdate**: (`key`, `data`) => `void`

###### Parameters

###### key

`string`

###### data

`T`

###### Returns

`void`

##### clearOptimisticUpdates()

> **clearOptimisticUpdates**: () => `void`

###### Returns

`void`

##### getOptimisticState()

> **getOptimisticState**: (`key`) => `undefined` \| `T`

###### Parameters

###### key

`string`

###### Returns

`undefined` \| `T`

##### removeOptimisticUpdate()

> **removeOptimisticUpdate**: (`key`) => `void`

###### Parameters

###### key

`string`

###### Returns

`void`
