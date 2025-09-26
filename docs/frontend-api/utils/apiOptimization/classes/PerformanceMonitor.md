# Class: PerformanceMonitor

Defined in: [frontend/src/utils/apiOptimization.ts:155](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L155)

## Constructors

### Constructor

> **new PerformanceMonitor**(): `PerformanceMonitor`

#### Returns

`PerformanceMonitor`

## Methods

### getMetrics()

> **getMetrics**(): `Record`\<`string`, \{ `avgTime`: `number`; `count`: `number`; `errorRate`: `number`; \}\>

Defined in: [frontend/src/utils/apiOptimization.ts:189](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L189)

#### Returns

`Record`\<`string`, \{ `avgTime`: `number`; `count`: `number`; `errorRate`: `number`; \}\>

***

### recordError()

> **recordError**(`operation`): `void`

Defined in: [frontend/src/utils/apiOptimization.ts:176](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L176)

#### Parameters

##### operation

`string`

#### Returns

`void`

***

### reset()

> **reset**(): `void`

Defined in: [frontend/src/utils/apiOptimization.ts:203](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L203)

#### Returns

`void`

***

### startTimer()

> **startTimer**(`operation`): () => `void`

Defined in: [frontend/src/utils/apiOptimization.ts:166](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L166)

#### Parameters

##### operation

`string`

#### Returns

> (): `void`

##### Returns

`void`

***

### getInstance()

> `static` **getInstance**(): `PerformanceMonitor`

Defined in: [frontend/src/utils/apiOptimization.ts:159](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L159)

#### Returns

`PerformanceMonitor`
