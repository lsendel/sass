# Function: useAutoSave()

> **useAutoSave**(`data`, `options`): `object`

Defined in: [frontend/src/hooks/useAutoSave.ts:11](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useAutoSave.ts#L11)

## Parameters

### data

`any`

### options

[`AutoSaveOptions`](../interfaces/AutoSaveOptions.md)

## Returns

`object`

### error

> **error**: `null` \| `Error`

### hasUnsavedChanges

> **hasUnsavedChanges**: `boolean`

### lastSaved

> **lastSaved**: `null` \| `Date`

### retry()

> **retry**: () => `void`

#### Returns

`void`

### save()

> **save**: () => `Promise`\<`void`\>

#### Returns

`Promise`\<`void`\>

### status

> **status**: [`AutoSaveStatus`](../type-aliases/AutoSaveStatus.md)
