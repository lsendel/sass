[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / components/ui/ErrorStates

# components/ui/ErrorStates

## Interfaces

### ErrorStateProps

Defined in: [src/components/ui/ErrorStates.tsx:13](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L13)

#### Properties

##### action?

> `optional` **action**: `object`

Defined in: [src/components/ui/ErrorStates.tsx:16](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L16)

###### label

> **label**: `string`

###### onClick()

> **onClick**: () => `void`

###### Returns

`void`

##### message?

> `optional` **message**: `string`

Defined in: [src/components/ui/ErrorStates.tsx:15](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L15)

##### onRetry()?

> `optional` **onRetry**: () => `void`

Defined in: [src/components/ui/ErrorStates.tsx:21](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L21)

###### Returns

`void`

##### showRetry?

> `optional` **showRetry**: `boolean`

Defined in: [src/components/ui/ErrorStates.tsx:20](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L20)

##### title?

> `optional` **title**: `string`

Defined in: [src/components/ui/ErrorStates.tsx:14](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L14)

##### variant?

> `optional` **variant**: `"error"` \| `"network"` \| `"server"` \| `"not-found"` \| `"empty"`

Defined in: [src/components/ui/ErrorStates.tsx:22](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L22)

## Variables

### ApiErrorDisplay

> `const` **ApiErrorDisplay**: `React.FC`\<`ApiErrorDisplayProps`\>

Defined in: [src/components/ui/ErrorStates.tsx:144](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L144)

***

### default

> `const` **default**: `React.FC`\<[`ErrorStateProps`](#errorstateprops)\>

Defined in: [src/components/ui/ErrorStates.tsx:25](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L25)

***

### EmptyState

> `const` **EmptyState**: `React.FC`\<`Omit`\<[`ErrorStateProps`](#errorstateprops), `"variant"`\>\>

Defined in: [src/components/ui/ErrorStates.tsx:133](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L133)

***

### NetworkError

> `const` **NetworkError**: `React.FC`\<`Omit`\<[`ErrorStateProps`](#errorstateprops), `"variant"`\>\>

Defined in: [src/components/ui/ErrorStates.tsx:121](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L121)

***

### NotFoundError

> `const` **NotFoundError**: `React.FC`\<`Omit`\<[`ErrorStateProps`](#errorstateprops), `"variant"`\>\>

Defined in: [src/components/ui/ErrorStates.tsx:129](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L129)

***

### ServerError

> `const` **ServerError**: `React.FC`\<`Omit`\<[`ErrorStateProps`](#errorstateprops), `"variant"`\>\>

Defined in: [src/components/ui/ErrorStates.tsx:125](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorStates.tsx#L125)
