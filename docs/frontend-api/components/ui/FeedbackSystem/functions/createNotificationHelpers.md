# Function: createNotificationHelpers()

> **createNotificationHelpers**(): `object`

Defined in: [frontend/src/components/ui/FeedbackSystem.tsx:242](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/components/ui/FeedbackSystem.tsx#L242)

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

### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

#### Parameters

##### id

`string`

##### updates

`Partial`\<[`NotificationData`](../interfaces/NotificationData.md)\>

#### Returns

`void`
