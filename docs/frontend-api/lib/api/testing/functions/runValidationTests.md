# Function: runValidationTests()

> **runValidationTests**\<`T`\>(`schema`, `scenarios`, `verbose`): `object`

Defined in: [frontend/src/lib/api/testing.ts:241](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/testing.ts#L241)

Runs validation test scenarios

## Type Parameters

### T

`T`

## Parameters

### schema

`ZodType`\<`T`\>

### scenarios

[`TestScenario`](../interfaces/TestScenario.md)\<`T`\>[]

### verbose

`boolean` = `false`

## Returns

`object`

### failed

> **failed**: `number`

### passed

> **passed**: `number`

### results

> **results**: `any`[]
