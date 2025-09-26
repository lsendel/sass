[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / components/ui/FeedbackSystem

# components/ui/FeedbackSystem

## Interfaces

### NotificationData

Defined in: [src/components/ui/FeedbackSystem.tsx:15](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L15)

#### Properties

##### actions?

> `optional` **actions**: `object`[]

Defined in: [src/components/ui/FeedbackSystem.tsx:22](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L22)

###### label

> **label**: `string`

###### onClick()

> **onClick**: () => `void`

###### Returns

`void`

###### variant?

> `optional` **variant**: `"secondary"` \| `"primary"`

##### duration?

> `optional` **duration**: `number`

Defined in: [src/components/ui/FeedbackSystem.tsx:20](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L20)

##### id

> **id**: `string`

Defined in: [src/components/ui/FeedbackSystem.tsx:16](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L16)

##### message?

> `optional` **message**: `string`

Defined in: [src/components/ui/FeedbackSystem.tsx:19](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L19)

##### persistent?

> `optional` **persistent**: `boolean`

Defined in: [src/components/ui/FeedbackSystem.tsx:21](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L21)

##### title

> **title**: `string`

Defined in: [src/components/ui/FeedbackSystem.tsx:18](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L18)

##### variant

> **variant**: [`NotificationVariant`](#notificationvariant)

Defined in: [src/components/ui/FeedbackSystem.tsx:17](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L17)

## Type Aliases

### NotificationVariant

> **NotificationVariant** = `"success"` \| `"error"` \| `"warning"` \| `"info"` \| `"loading"`

Defined in: [src/components/ui/FeedbackSystem.tsx:13](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L13)

## Variables

### NotificationProvider

> `const` **NotificationProvider**: `React.FC`\<\{ `children`: `React.ReactNode`; \}\>

Defined in: [src/components/ui/FeedbackSystem.tsx:43](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L43)

***

### ProgressIndicator

> `const` **ProgressIndicator**: `React.FC`\<`ProgressIndicatorProps`\>

Defined in: [src/components/ui/FeedbackSystem.tsx:293](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L293)

## Functions

### createNotificationHelpers()

> **createNotificationHelpers**(): `object`

Defined in: [src/components/ui/FeedbackSystem.tsx:242](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L242)

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

##### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](#notificationdata)\>

###### Returns

`void`

***

### useNotifications()

> **useNotifications**(): `object`

Defined in: [src/components/ui/FeedbackSystem.tsx:92](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/FeedbackSystem.tsx#L92)

#### Returns

`object`

##### addNotification()

> **addNotification**: (`notification`) => `string`

###### Parameters

###### notification

`Omit`\<[`NotificationData`](#notificationdata), `"id"`\>

###### Returns

`string`

##### notifications

> **notifications**: [`NotificationData`](#notificationdata)[]

##### removeNotification()

> **removeNotification**: (`id`) => `void`

###### Parameters

###### id

`string`

###### Returns

`void`

##### updateNotification()

> **updateNotification**: (`id`, `updates`) => `void`

###### Parameters

###### id

`string`

###### updates

`Partial`\<[`NotificationData`](#notificationdata)\>

###### Returns

`void`

## References

### default

Renames and re-exports [NotificationProvider](#notificationprovider)
