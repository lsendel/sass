[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/slices/uiSlice

# store/slices/uiSlice

## Type Aliases

### Notification

> **Notification** = `object`

Defined in: [src/store/slices/uiSlice.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L5)

#### Properties

##### actions?

> `optional` **actions**: `object`[]

Defined in: [src/store/slices/uiSlice.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L11)

###### action()

> **action**: () => `void`

###### Returns

`void`

###### label

> **label**: `string`

##### duration?

> `optional` **duration**: `number`

Defined in: [src/store/slices/uiSlice.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L10)

##### id

> **id**: `string`

Defined in: [src/store/slices/uiSlice.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L6)

##### message

> **message**: `string`

Defined in: [src/store/slices/uiSlice.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L9)

##### title

> **title**: `string`

Defined in: [src/store/slices/uiSlice.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L8)

##### type

> **type**: `"success"` \| `"error"` \| `"warning"` \| `"info"`

Defined in: [src/store/slices/uiSlice.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L7)

***

### Theme

> **Theme** = `"light"` \| `"dark"`

Defined in: [src/store/slices/uiSlice.ts:3](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L3)

***

### UiState

> **UiState** = `object`

Defined in: [src/store/slices/uiSlice.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L17)

#### Properties

##### loading

> **loading**: `object`

Defined in: [src/store/slices/uiSlice.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L20)

###### components

> **components**: `Record`\<`string`, `boolean`\>

###### global

> **global**: `boolean`

##### modals

> **modals**: `object`

Defined in: [src/store/slices/uiSlice.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L25)

###### isInviteUserModalOpen

> **isInviteUserModalOpen**: `boolean`

###### isPaymentMethodModalOpen

> **isPaymentMethodModalOpen**: `boolean`

###### isSubscriptionModalOpen

> **isSubscriptionModalOpen**: `boolean`

##### notifications

> **notifications**: [`Notification`](#notification)[]

Defined in: [src/store/slices/uiSlice.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L24)

##### sidebarOpen

> **sidebarOpen**: `boolean`

Defined in: [src/store/slices/uiSlice.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L19)

##### theme

> **theme**: [`Theme`](#theme)

Defined in: [src/store/slices/uiSlice.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L18)

## Variables

### addNotification

> **addNotification**: `ActionCreatorWithPayload`\<`Omit`\<[`Notification`](#notification), `"id"`\>, `"ui/addNotification"`\>

Defined in: [src/store/slices/uiSlice.ts:110](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L110)

***

### clearNotifications

> **clearNotifications**: `ActionCreatorWithoutPayload`\<`"ui/clearNotifications"`\>

Defined in: [src/store/slices/uiSlice.ts:112](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L112)

***

### closeAllModals

> **closeAllModals**: `ActionCreatorWithoutPayload`\<`"ui/closeAllModals"`\>

Defined in: [src/store/slices/uiSlice.ts:115](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L115)

***

### closeModal

> **closeModal**: `ActionCreatorWithPayload`\<`"isPaymentMethodModalOpen"` \| `"isSubscriptionModalOpen"` \| `"isInviteUserModalOpen"`, `"ui/closeModal"`\>

Defined in: [src/store/slices/uiSlice.ts:114](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L114)

***

### openModal

> **openModal**: `ActionCreatorWithPayload`\<`"isPaymentMethodModalOpen"` \| `"isSubscriptionModalOpen"` \| `"isInviteUserModalOpen"`, `"ui/openModal"`\>

Defined in: [src/store/slices/uiSlice.ts:113](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L113)

***

### removeNotification

> **removeNotification**: `ActionCreatorWithPayload`\<`string`, `"ui/removeNotification"`\>

Defined in: [src/store/slices/uiSlice.ts:111](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L111)

***

### setComponentLoading

> **setComponentLoading**: `ActionCreatorWithPayload`\<\{ `component`: `string`; `loading`: `boolean`; \}, `"ui/setComponentLoading"`\>

Defined in: [src/store/slices/uiSlice.ts:109](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L109)

***

### setGlobalLoading

> **setGlobalLoading**: `ActionCreatorWithPayload`\<`boolean`, `"ui/setGlobalLoading"`\>

Defined in: [src/store/slices/uiSlice.ts:108](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L108)

***

### setSidebarOpen

> **setSidebarOpen**: `ActionCreatorWithPayload`\<`boolean`, `"ui/setSidebarOpen"`\>

Defined in: [src/store/slices/uiSlice.ts:107](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L107)

***

### setTheme

> **setTheme**: `ActionCreatorWithPayload`\<[`Theme`](#theme), `"ui/setTheme"`\>

Defined in: [src/store/slices/uiSlice.ts:105](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L105)

***

### toggleSidebar

> **toggleSidebar**: `ActionCreatorWithoutPayload`\<`"ui/toggleSidebar"`\>

Defined in: [src/store/slices/uiSlice.ts:106](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L106)

## Functions

### selectComponentLoading()

> **selectComponentLoading**(`component`): (`state`) => `boolean`

Defined in: [src/store/slices/uiSlice.ts:127](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L127)

#### Parameters

##### component

`string`

#### Returns

> (`state`): `boolean`

##### Parameters

###### state

###### ui

[`UiState`](#uistate)

##### Returns

`boolean`

***

### selectGlobalLoading()

> **selectGlobalLoading**(`state`): `boolean`

Defined in: [src/store/slices/uiSlice.ts:124](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L124)

#### Parameters

##### state

###### ui

[`UiState`](#uistate)

#### Returns

`boolean`

***

### selectModals()

> **selectModals**(`state`): `object`

Defined in: [src/store/slices/uiSlice.ts:131](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L131)

#### Parameters

##### state

###### ui

[`UiState`](#uistate)

#### Returns

`object`

##### isInviteUserModalOpen

> **isInviteUserModalOpen**: `boolean`

##### isPaymentMethodModalOpen

> **isPaymentMethodModalOpen**: `boolean`

##### isSubscriptionModalOpen

> **isSubscriptionModalOpen**: `boolean`

***

### selectNotifications()

> **selectNotifications**(`state`): [`Notification`](#notification)[]

Defined in: [src/store/slices/uiSlice.ts:129](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L129)

#### Parameters

##### state

###### ui

[`UiState`](#uistate)

#### Returns

[`Notification`](#notification)[]

***

### selectSidebarOpen()

> **selectSidebarOpen**(`state`): `boolean`

Defined in: [src/store/slices/uiSlice.ts:122](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L122)

#### Parameters

##### state

###### ui

[`UiState`](#uistate)

#### Returns

`boolean`

***

### selectTheme()

> **selectTheme**(`state`): [`Theme`](#theme)

Defined in: [src/store/slices/uiSlice.ts:121](https://github.com/lsendel/sass/blob/main/frontend/src/store/slices/uiSlice.ts#L121)

#### Parameters

##### state

###### ui

[`UiState`](#uistate)

#### Returns

[`Theme`](#theme)
