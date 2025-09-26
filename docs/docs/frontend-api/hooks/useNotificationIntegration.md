[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useNotificationIntegration

# hooks/useNotificationIntegration

## Functions

### useBatchOperationNotifications()

> **useBatchOperationNotifications**(): `object`

Defined in: [src/hooks/useNotificationIntegration.ts:213](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useNotificationIntegration.ts#L213)

Hook for batch operations with progress tracking

#### Returns

`object`

##### executeBatchOperation()

> **executeBatchOperation**: \<`T`, `R`\>(`items`, `operationFn`, `options`) => `Promise`\<(`null` \| `R`)[]\>

###### Type Parameters

###### T

`T`

###### R

`R`

###### Parameters

###### items

`T`[]

###### operationFn

(`item`, `index`) => `Promise`\<`R`\>

###### options

###### batchTitle?

`string`

###### errorTitle?

`string`

###### onComplete?

(`results`) => `void`

###### onItemComplete?

(`item`, `result`, `index`) => `void`

###### onItemError?

(`item`, `error`, `index`) => `void`

###### showProgress?

`boolean`

###### successTitle?

`string`

###### Returns

`Promise`\<(`null` \| `R`)[]\>

##### showActionable()

> **showActionable**: (`title`, `message`, `actions`) => `string`

###### Parameters

###### title

`string`

###### message

`string`

###### actions

`object`[]

###### Returns

`string`

##### showError()

> **showError**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showInfo()

> **showInfo**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showLoading()

> **showLoading**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showSuccess()

> **showSuccess**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showWarning()

> **showWarning**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata)\>

###### Returns

`void`

***

### useFormSubmissionNotifications()

> **useFormSubmissionNotifications**(): `object`

Defined in: [src/hooks/useNotificationIntegration.ts:135](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useNotificationIntegration.ts#L135)

Hook for enhanced form submission with integrated notifications

#### Returns

`object`

##### showActionable()

> **showActionable**: (`title`, `message`, `actions`) => `string`

###### Parameters

###### title

`string`

###### message

`string`

###### actions

`object`[]

###### Returns

`string`

##### showError()

> **showError**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showInfo()

> **showInfo**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showLoading()

> **showLoading**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showSuccess()

> **showSuccess**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showWarning()

> **showWarning**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### submitFormWithNotifications()

> **submitFormWithNotifications**: \<`T`, `R`\>(`data`, `submitFn`, `options`) => `Promise`\<`null` \| `R`\>

###### Type Parameters

###### T

`T`

###### R

`R`

###### Parameters

###### data

`T`

###### submitFn

(`data`) => `Promise`\<`R`\>

###### options

###### errorTitle?

`string`

###### loadingTitle?

`string`

###### onError?

(`error`) => `void`

###### onSuccess?

(`result`) => `void`

###### successMessage?

`string`

###### successTitle?

`string`

###### validateBeforeSubmit?

(`data`) => `null` \| `string`

###### Returns

`Promise`\<`null` \| `R`\>

##### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata)\>

###### Returns

`void`

***

### useOptimisticNotifications()

> **useOptimisticNotifications**\<`T`\>(): `object`

Defined in: [src/hooks/useNotificationIntegration.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useNotificationIntegration.ts#L9)

Enhanced hook that integrates optimistic updates with the notification system
for better user feedback and error handling

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

[`OptimisticUpdateOptions`](useOptimisticUpdates.md#optimisticupdateoptions)\<`T`\> = `{}`

###### Returns

`Promise`\<`null` \| `R`\>

##### addOptimisticUpdateWithNotifications()

> **addOptimisticUpdateWithNotifications**: \<`R`\>(`data`, `mutationFn`, `options`) => `Promise`\<`null` \| `R`\>

###### Type Parameters

###### R

`R`

###### Parameters

###### data

`T`

###### mutationFn

(`data`) => `Promise`\<`R`\>

###### options

[`OptimisticUpdateOptions`](useOptimisticUpdates.md#optimisticupdateoptions)\<`T`\> & `object` = `{}`

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

> **getFailedUpdates**: () => [`OptimisticUpdate`](useOptimisticUpdates.md#optimisticupdate)\<`T`\>[]

###### Returns

[`OptimisticUpdate`](useOptimisticUpdates.md#optimisticupdate)\<`T`\>[]

##### getPendingUpdates()

> **getPendingUpdates**: () => [`OptimisticUpdate`](useOptimisticUpdates.md#optimisticupdate)\<`T`\>[]

###### Returns

[`OptimisticUpdate`](useOptimisticUpdates.md#optimisticupdate)\<`T`\>[]

##### hasOptimisticUpdates

> **hasOptimisticUpdates**: `boolean`

##### notifications

> **notifications**: `object`

###### notifications.addNotification()

> **addNotification**: (`notification`) => `string`

###### Parameters

###### notification

`Omit`\<[`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata), `"id"`\>

###### Returns

`string`

###### notifications.notifications

> **notifications**: [`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata)[]

###### notifications.removeNotification()

> **removeNotification**: (`id`) => `void`

###### Parameters

###### id

`string`

###### Returns

`void`

###### notifications.updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata)\>

###### Returns

`void`

##### optimisticData

> **optimisticData**: `T`[]

##### optimisticUpdates

> **optimisticUpdates**: [`OptimisticUpdate`](useOptimisticUpdates.md#optimisticupdate)\<`T`\>[]

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

##### showActionable()

> **showActionable**: (`title`, `message`, `actions`) => `string`

###### Parameters

###### title

`string`

###### message

`string`

###### actions

`object`[]

###### Returns

`string`

##### showError()

> **showError**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showInfo()

> **showInfo**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showLoading()

> **showLoading**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showSuccess()

> **showSuccess**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### showWarning()

> **showWarning**: (`title`, `message?`) => `string`

###### Parameters

###### title

`string`

###### message?

`string`

###### Returns

`string`

##### updateLoadingNotification()

> **updateLoadingNotification**: (`notificationId`, `progress`, `message?`) => `void`

###### Parameters

###### notificationId

`string`

###### progress

`number`

###### message?

`string`

###### Returns

`void`

##### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](../components/ui/FeedbackSystem.md#notificationdata)\>

###### Returns

`void`
