# Function: useDataSync()

> **useDataSync**(`options`): `object`

Defined in: [frontend/src/hooks/useDataSync.ts:36](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useDataSync.ts#L36)

Custom hook for synchronizing data across components
Helps maintain consistency when data changes in one component affect others

## Parameters

### options

`DataSyncOptions`

## Returns

`object`

### lastSync

> **lastSync**: `Record`\<`string`, `any`\> = `lastSyncRef.current`

### triggerSync()

> **triggerSync**: (`data`) => `void`

#### Parameters

##### data

`Record`\<`string`, `any`\>

#### Returns

`void`
