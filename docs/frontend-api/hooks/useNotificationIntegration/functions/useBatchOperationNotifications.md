# Function: useBatchOperationNotifications()

> **useBatchOperationNotifications**(): `object`

Defined in: [frontend/src/hooks/useNotificationIntegration.ts:215](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useNotificationIntegration.ts#L215)

Hook for batch operations with progress tracking

## Returns

`object`

### executeBatchOperation()

> **executeBatchOperation**: \<`T`, `R`\>(`items`, `operationFn`, `options`) => `Promise`\<(`null` \| `R`)[]\>

#### Type Parameters

##### T

`T`

##### R

`R`

#### Parameters

##### items

`T`[]

##### operationFn

(`item`, `index`) => `Promise`\<`R`\>

##### options

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

#### Returns

`Promise`\<(`null` \| `R`)[]\>

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

### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

#### Parameters

##### id

`string`

##### updates

`Partial`\<[`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md)\>

#### Returns

`void`
