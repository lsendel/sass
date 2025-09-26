# Function: benchmarkValidation()

> **benchmarkValidation**\<`T`\>(`schema`, `data`, `iterations`): `Promise`\<\{ `averageTime`: `number`; `iterations`: `number`; `maxTime`: `number`; `minTime`: `number`; `totalTime`: `number`; \}\>

Defined in: [frontend/src/lib/api/testing.ts:364](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L364)

Performance testing utility

## Type Parameters

### T

`T`

## Parameters

### schema

`ZodType`\<`T`\>

### data

`unknown`

### iterations

`number` = `1000`

## Returns

`Promise`\<\{ `averageTime`: `number`; `iterations`: `number`; `maxTime`: `number`; `minTime`: `number`; `totalTime`: `number`; \}\>
