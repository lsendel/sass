[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / components/ui/ErrorBoundary

# components/ui/ErrorBoundary

## Classes

### default

Defined in: [src/components/ui/ErrorBoundary.tsx:13](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorBoundary.tsx#L13)

#### Extends

- `Component`\<`Props`, `State`\>

#### Constructors

##### Constructor

> **new default**(`props`): [`default`](#default)

Defined in: [src/components/ui/ErrorBoundary.tsx:14](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorBoundary.tsx#L14)

###### Parameters

###### props

`Props`

###### Returns

[`default`](#default)

###### Overrides

`Component<Props, State>.constructor`

#### Methods

##### componentDidCatch()

> **componentDidCatch**(`error`, `errorInfo`): `void`

Defined in: [src/components/ui/ErrorBoundary.tsx:23](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorBoundary.tsx#L23)

Catches exceptions generated in descendant components. Unhandled exceptions will cause
the entire component tree to unmount.

###### Parameters

###### error

`Error`

###### errorInfo

`ErrorInfo`

###### Returns

`void`

###### Overrides

`Component.componentDidCatch`

##### render()

> **render**(): `undefined` \| `null` \| `string` \| `number` \| `bigint` \| `boolean` \| `Element` \| `Iterable`\<`ReactNode`, `any`, `any`\> \| `Promise`\<`AwaitedReactNode`\>

Defined in: [src/components/ui/ErrorBoundary.tsx:27](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorBoundary.tsx#L27)

###### Returns

`undefined` \| `null` \| `string` \| `number` \| `bigint` \| `boolean` \| `Element` \| `Iterable`\<`ReactNode`, `any`, `any`\> \| `Promise`\<`AwaitedReactNode`\>

###### Overrides

`Component.render`

##### getDerivedStateFromError()

> `static` **getDerivedStateFromError**(`error`): `State`

Defined in: [src/components/ui/ErrorBoundary.tsx:19](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/ErrorBoundary.tsx#L19)

###### Parameters

###### error

`Error`

###### Returns

`State`
