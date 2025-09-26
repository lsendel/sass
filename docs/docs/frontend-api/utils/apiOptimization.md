[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / utils/apiOptimization

# utils/apiOptimization

## Classes

### ConnectionMonitor

Defined in: [src/utils/apiOptimization.ts:210](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L210)

#### Constructors

##### Constructor

> **new ConnectionMonitor**(): [`ConnectionMonitor`](#connectionmonitor)

Defined in: [src/utils/apiOptimization.ts:223](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L223)

###### Returns

[`ConnectionMonitor`](#connectionmonitor)

#### Methods

##### getConnectionQuality()

> **getConnectionQuality**(): `"fast"` \| `"slow"` \| `"offline"`

Defined in: [src/utils/apiOptimization.ts:241](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L241)

###### Returns

`"fast"` \| `"slow"` \| `"offline"`

##### onConnectionChange()

> **onConnectionChange**(`callback`): () => `void`

Defined in: [src/utils/apiOptimization.ts:245](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L245)

###### Parameters

###### callback

(`quality`) => `void`

###### Returns

> (): `void`

###### Returns

`void`

##### getInstance()

> `static` **getInstance**(): [`ConnectionMonitor`](#connectionmonitor)

Defined in: [src/utils/apiOptimization.ts:216](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L216)

###### Returns

[`ConnectionMonitor`](#connectionmonitor)

***

### DataPrefetcher

Defined in: [src/utils/apiOptimization.ts:64](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L64)

#### Constructors

##### Constructor

> **new DataPrefetcher**(): [`DataPrefetcher`](#dataprefetcher)

###### Returns

[`DataPrefetcher`](#dataprefetcher)

#### Methods

##### addToPrefetchQueue()

> **addToPrefetchQueue**(`endpoint`): `void`

Defined in: [src/utils/apiOptimization.ts:76](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L76)

###### Parameters

###### endpoint

`string`

###### Returns

`void`

##### getInstance()

> `static` **getInstance**(): [`DataPrefetcher`](#dataprefetcher)

Defined in: [src/utils/apiOptimization.ts:69](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L69)

###### Returns

[`DataPrefetcher`](#dataprefetcher)

***

### PerformanceMonitor

Defined in: [src/utils/apiOptimization.ts:154](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L154)

#### Constructors

##### Constructor

> **new PerformanceMonitor**(): [`PerformanceMonitor`](#performancemonitor)

###### Returns

[`PerformanceMonitor`](#performancemonitor)

#### Methods

##### getMetrics()

> **getMetrics**(): `Record`\<`string`, \{ `avgTime`: `number`; `count`: `number`; `errorRate`: `number`; \}\>

Defined in: [src/utils/apiOptimization.ts:188](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L188)

###### Returns

`Record`\<`string`, \{ `avgTime`: `number`; `count`: `number`; `errorRate`: `number`; \}\>

##### recordError()

> **recordError**(`operation`): `void`

Defined in: [src/utils/apiOptimization.ts:175](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L175)

###### Parameters

###### operation

`string`

###### Returns

`void`

##### reset()

> **reset**(): `void`

Defined in: [src/utils/apiOptimization.ts:202](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L202)

###### Returns

`void`

##### startTimer()

> **startTimer**(`operation`): () => `void`

Defined in: [src/utils/apiOptimization.ts:165](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L165)

###### Parameters

###### operation

`string`

###### Returns

> (): `void`

###### Returns

`void`

##### getInstance()

> `static` **getInstance**(): [`PerformanceMonitor`](#performancemonitor)

Defined in: [src/utils/apiOptimization.ts:158](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L158)

###### Returns

[`PerformanceMonitor`](#performancemonitor)

## Variables

### CACHE\_DURATIONS

> `const` **CACHE\_DURATIONS**: `object`

Defined in: [src/utils/apiOptimization.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L9)

#### Type Declaration

##### LONG

> `readonly` **LONG**: `number`

##### MEDIUM

> `readonly` **MEDIUM**: `number`

##### SHORT

> `readonly` **SHORT**: `number`

##### STATIC

> `readonly` **STATIC**: `number`

***

### CACHE\_TAGS

> `const` **CACHE\_TAGS**: `object`

Defined in: [src/utils/apiOptimization.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L17)

#### Type Declaration

##### Invoice

> `readonly` **Invoice**: `"Invoice"` = `'Invoice'`

##### Organization

> `readonly` **Organization**: `"Organization"` = `'Organization'`

##### Payment

> `readonly` **Payment**: `"Payment"` = `'Payment'`

##### PaymentMethod

> `readonly` **PaymentMethod**: `"PaymentMethod"` = `'PaymentMethod'`

##### PaymentStatistics

> `readonly` **PaymentStatistics**: `"PaymentStatistics"` = `'PaymentStatistics'`

##### SetupIntent

> `readonly` **SetupIntent**: `"SetupIntent"` = `'SetupIntent'`

##### Subscription

> `readonly` **Subscription**: `"Subscription"` = `'Subscription'`

##### SubscriptionPlan

> `readonly` **SubscriptionPlan**: `"SubscriptionPlan"` = `'SubscriptionPlan'`

##### User

> `readonly` **User**: `"User"` = `'User'`

***

### connectionMonitor

> `const` **connectionMonitor**: [`ConnectionMonitor`](#connectionmonitor)

Defined in: [src/utils/apiOptimization.ts:292](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L292)

***

### dataPrefetcher

> `const` **dataPrefetcher**: [`DataPrefetcher`](#dataprefetcher)

Defined in: [src/utils/apiOptimization.ts:325](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L325)

***

### performanceMonitor

> `const` **performanceMonitor**: [`PerformanceMonitor`](#performancemonitor)

Defined in: [src/utils/apiOptimization.ts:207](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L207)

***

### requestDeduplicator

> `const` **requestDeduplicator**: `RequestDeduplicator`

Defined in: [src/utils/apiOptimization.ts:151](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L151)

***

### selectCurrentUserWithOrganizations

> `const` **selectCurrentUserWithOrganizations**: (`state`, ...`params`) => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \} & `object` & `object` & `object`

Defined in: [src/utils/apiOptimization.ts:53](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L53)

***

### selectPrimaryOrganization

> `const` **selectPrimaryOrganization**: (`state`, ...`params`) => `null` & `object` & `object` & `object`

Defined in: [src/utils/apiOptimization.ts:58](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L58)

## Functions

### createMemoizedSelector()

> **createMemoizedSelector**\<`T`\>(`inputSelectors`, `resultFunc`): (`state`, ...`params`) => `T` & `object` & `object` & `object`

Defined in: [src/utils/apiOptimization.ts:45](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L45)

#### Type Parameters

##### T

`T`

#### Parameters

##### inputSelectors

`any`[]

##### resultFunc

(...`args`) => `T`

#### Returns

***

### createRetryConfig()

> **createRetryConfig**(`maxRetries`): `object`

Defined in: [src/utils/apiOptimization.ts:108](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L108)

#### Parameters

##### maxRetries

`number` = `3`

#### Returns

`object`

##### maxRetries

> **maxRetries**: `number`

##### retryCondition()

> **retryCondition**: (`error`) => `boolean`

###### Parameters

###### error

`any`

###### Returns

`boolean`

##### retryDelay()

> **retryDelay**: (`retryAttempt`) => `number`

###### Parameters

###### retryAttempt

`number`

###### Returns

`number`

***

### generateCacheKey()

> **generateCacheKey**(`endpoint`, `params?`, `userContext?`): `string`

Defined in: [src/utils/apiOptimization.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L30)

#### Parameters

##### endpoint

`string`

##### params?

`Record`\<`string`, `any`\>

##### userContext?

###### organizationId?

`string`

###### userId?

`string`

#### Returns

`string`

***

### getAdaptiveLoadingStrategy()

> **getAdaptiveLoadingStrategy**(): `object`

Defined in: [src/utils/apiOptimization.ts:295](https://github.com/lsendel/sass/blob/main/frontend/src/utils/apiOptimization.ts#L295)

#### Returns

`object`

##### cachePolicy

> **cachePolicy**: `string` = `'cache-only'`

##### enableBackgroundRefresh

> **enableBackgroundRefresh**: `boolean` = `false`

##### enablePrefetch

> **enablePrefetch**: `boolean` = `false`

##### imageOptimization

> **imageOptimization**: `string` = `'aggressive'`
