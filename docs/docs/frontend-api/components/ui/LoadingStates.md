[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / components/ui/LoadingStates

# components/ui/LoadingStates

## Variables

### CardSkeleton

> `const` **CardSkeleton**: `React.FC`\<\{ `className?`: `string`; \}\>

Defined in: [src/components/ui/LoadingStates.tsx:211](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L211)

***

### InlineLoading

> `const` **InlineLoading**: `React.FC`\<`InlineLoadingProps`\>

Defined in: [src/components/ui/LoadingStates.tsx:238](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L238)

***

### ListSkeleton

> `const` **ListSkeleton**: `React.FC`\<\{ `items?`: `number`; \}\>

Defined in: [src/components/ui/LoadingStates.tsx:193](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L193)

***

### LoadingButton

> `const` **LoadingButton**: `React.FC`\<`LoadingButtonProps`\>

Defined in: [src/components/ui/LoadingStates.tsx:19](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L19)

***

### LoadingCard

> `const` **LoadingCard**: `React.FC`\<`LoadingCardProps`\>

Defined in: [src/components/ui/LoadingStates.tsx:105](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L105)

***

### LoadingOverlay

> `const` **LoadingOverlay**: `React.FC`\<`LoadingOverlayProps`\>

Defined in: [src/components/ui/LoadingStates.tsx:78](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L78)

***

### Skeleton

> `const` **Skeleton**: `React.FC`\<`SkeletonProps`\>

Defined in: [src/components/ui/LoadingStates.tsx:133](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L133)

***

### TableSkeleton

> `const` **TableSkeleton**: `React.FC`\<\{ `columns?`: `number`; `rows?`: `number`; \}\>

Defined in: [src/components/ui/LoadingStates.tsx:166](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L166)

## Functions

### useLoadingState()

> **useLoadingState**(`initialState`): `object`

Defined in: [src/components/ui/LoadingStates.tsx:263](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/LoadingStates.tsx#L263)

#### Parameters

##### initialState

`boolean` = `false`

#### Returns

`object`

##### isLoading

> **isLoading**: `boolean`

##### setIsLoading

> **setIsLoading**: `Dispatch`\<`SetStateAction`\<`boolean`\>\>

##### startLoading()

> **startLoading**: () => `void`

###### Returns

`void`

##### stopLoading()

> **stopLoading**: () => `void`

###### Returns

`void`

##### toggleLoading()

> **toggleLoading**: () => `void`

###### Returns

`void`
