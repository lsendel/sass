# Function: useOptimisticNotifications()

> **useOptimisticNotifications**\<`T`\>(): `object`

Defined in: [frontend/src/hooks/useNotificationIntegration.ts:11](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useNotificationIntegration.ts#L11)

Enhanced hook that integrates optimistic updates with the notification system
for better user feedback and error handling

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

[`OptimisticUpdateOptions`](../../useOptimisticUpdates/interfaces/OptimisticUpdateOptions.md)\<`T`\> = `{}`

#### Returns

`Promise`\<`null` \| `R`\>

### addOptimisticUpdateWithNotifications()

> **addOptimisticUpdateWithNotifications**: \<`R`\>(`data`, `mutationFn`, `options`) => `Promise`\<`null` \| `R`\>

#### Type Parameters

##### R

`R`

#### Parameters

##### data

`T`

##### mutationFn

(`data`) => `Promise`\<`R`\>

##### options

[`OptimisticUpdateOptions`](../../useOptimisticUpdates/interfaces/OptimisticUpdateOptions.md)\<`T`\> & `object` = `{}`

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

> **getFailedUpdates**: () => [`OptimisticUpdate`](../../useOptimisticUpdates/interfaces/OptimisticUpdate.md)\<`T`\>[]

#### Returns

[`OptimisticUpdate`](../../useOptimisticUpdates/interfaces/OptimisticUpdate.md)\<`T`\>[]

### getPendingUpdates()

> **getPendingUpdates**: () => [`OptimisticUpdate`](../../useOptimisticUpdates/interfaces/OptimisticUpdate.md)\<`T`\>[]

#### Returns

[`OptimisticUpdate`](../../useOptimisticUpdates/interfaces/OptimisticUpdate.md)\<`T`\>[]

### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

### notifications

> **notifications**: `object`

#### notifications.addNotification()

> **addNotification**: (`notification`) => `string`

##### Parameters

###### notification

`Omit`\<[`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md), `"id"`\>

##### Returns

`string`

#### notifications.notifications

> **notifications**: [`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md)[]

#### notifications.removeNotification()

> **removeNotification**: (`id`) => `void`

##### Parameters

###### id

`string`

##### Returns

`void`

#### notifications.updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

##### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md)\>

##### Returns

`void`

### optimisticData

> **optimisticData**: `T`[]

### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](../../useOptimisticUpdates/interfaces/OptimisticUpdate.md)\<`T`\>[]

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

### showActionable()

> **showActionable**: (`title`, `message`, `actions`) => `string`

#### Parameters

##### title

`string`

##### message

`string`

##### actions

`object`[]

#### Returns

`string`

### showError()

> **showError**: (`title`, `message?`) => `string`

#### Parameters

##### title

`string`

##### message?

`string`

#### Returns

`string`

### showInfo()

> **showInfo**: (`title`, `message?`) => `string`

#### Parameters

##### title

`string`

##### message?

`string`

#### Returns

`string`

### showLoading()

> **showLoading**: (`title`, `message?`) => `string`

#### Parameters

##### title

`string`

##### message?

`string`

#### Returns

`string`

### showSuccess()

> **showSuccess**: (`title`, `message?`) => `string`

#### Parameters

##### title

`string`

##### message?

`string`

#### Returns

`string`

### showWarning()

> **showWarning**: (`title`, `message?`) => `string`

#### Parameters

##### title

`string`

##### message?

`string`

#### Returns

`string`

### updateLoadingNotification()

> **updateLoadingNotification**: (`notificationId`, `progress`, `message?`) => `void`

#### Parameters

##### notificationId

`string`

##### progress

`number`

##### message?

`string`

#### Returns

`void`

### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

#### Parameters

##### id

`string`

##### updates

`Partial`\<[`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md)\>

#### Returns

`void`
