[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / utils/performance

# utils/performance

## Type Aliases

### LazyComponent

> **LazyComponent**\<`T`\> = `ComponentType`\<`T`\>

Defined in: [src/utils/performance.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L6)

#### Type Parameters

##### T

`T` = `any`

## Variables

### memoizedFetch()

> `const` **memoizedFetch**: (`url`, `options?`) => `Promise`\<`any`\>

Defined in: [src/utils/performance.ts:39](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L39)

#### Parameters

##### url

`string`

##### options?

`RequestInit`

#### Returns

`Promise`\<`any`\>

## Functions

### lazyLoad()

> **lazyLoad**\<`T`\>(`importFunc`): `ComponentType`\<`T`\>

Defined in: [src/utils/performance.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L9)

#### Type Parameters

##### T

`T` *extends* `object`

#### Parameters

##### importFunc

() => `Promise`\<\{ `default`: `ComponentType`\<`T`\>; \}\>

#### Returns

`ComponentType`\<`T`\>

***

### measurePerformance()

> **measurePerformance**(`name`, `fn`): `void`

Defined in: [src/utils/performance.ts:54](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L54)

#### Parameters

##### name

`string`

##### fn

() => `void`

#### Returns

`void`

***

### useDebounce()

> **useDebounce**(`value`, `delay`): `string`

Defined in: [src/utils/performance.ts:27](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L27)

#### Parameters

##### value

`string`

##### delay

`number`

#### Returns

`string`

***

### withPerformanceMonitoring()

> **withPerformanceMonitoring**\<`T`\>(`Component`, `name`): `ComponentType`\<`T`\>

Defined in: [src/utils/performance.ts:62](https://github.com/lsendel/sass/blob/main/frontend/src/utils/performance.ts#L62)

#### Type Parameters

##### T

`T` *extends* `object`

#### Parameters

##### Component

`ComponentType`\<`T`\>

##### name

`string`

#### Returns

`ComponentType`\<`T`\>
