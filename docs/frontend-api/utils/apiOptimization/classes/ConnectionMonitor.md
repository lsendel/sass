# Class: ConnectionMonitor

Defined in: [frontend/src/utils/apiOptimization.ts:211](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L211)

## Constructors

### Constructor

> **new ConnectionMonitor**(): `ConnectionMonitor`

Defined in: [frontend/src/utils/apiOptimization.ts:224](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L224)

#### Returns

`ConnectionMonitor`

## Methods

### getConnectionQuality()

> **getConnectionQuality**(): `"fast"` \| `"slow"` \| `"offline"`

Defined in: [frontend/src/utils/apiOptimization.ts:242](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L242)

#### Returns

`"fast"` \| `"slow"` \| `"offline"`

***

### onConnectionChange()

> **onConnectionChange**(`callback`): () => `void`

Defined in: [frontend/src/utils/apiOptimization.ts:246](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L246)

#### Parameters

##### callback

(`quality`) => `void`

#### Returns

> (): `void`

##### Returns

`void`

***

### getInstance()

> `static` **getInstance**(): `ConnectionMonitor`

Defined in: [frontend/src/utils/apiOptimization.ts:217](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L217)

#### Returns

`ConnectionMonitor`
