# Function: useNotifications()

> **useNotifications**(): `object`

Defined in: [frontend/src/components/ui/FeedbackSystem.tsx:92](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/components/ui/FeedbackSystem.tsx#L92)

## Returns

`object`

### addNotification()

> **addNotification**: (`notification`) => `string`

#### Parameters

##### notification

`Omit`\<[`NotificationData`](../interfaces/NotificationData.md), `"id"`\>

#### Returns

`string`

### notifications

> **notifications**: [`NotificationData`](../interfaces/NotificationData.md)[]

### removeNotification()

> **removeNotification**: (`id`) => `void`

#### Parameters

##### id

`string`

#### Returns

`void`

### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

#### Parameters

##### id

`string`

##### updates

`Partial`\<[`NotificationData`](../interfaces/NotificationData.md)\>

#### Returns

`void`
