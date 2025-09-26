# Function: useFormSubmissionNotifications()

> **useFormSubmissionNotifications**(): `object`

Defined in: [frontend/src/hooks/useNotificationIntegration.ts:137](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useNotificationIntegration.ts#L137)

Hook for enhanced form submission with integrated notifications

## Returns

`object`

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

### submitFormWithNotifications()

> **submitFormWithNotifications**: \<`T`, `R`\>(`data`, `submitFn`, `options`) => `Promise`\<`null` \| `R`\>

#### Type Parameters

##### T

`T`

##### R

`R`

#### Parameters

##### data

`T`

##### submitFn

(`data`) => `Promise`\<`R`\>

##### options

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

#### Returns

`Promise`\<`null` \| `R`\>

### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

#### Parameters

##### id

`string`

##### updates

`Partial`\<[`NotificationData`](../../../components/ui/FeedbackSystem/interfaces/NotificationData.md)\>

#### Returns

`void`
