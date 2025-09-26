# Function: ApiSuccessResponseSchema()

> **ApiSuccessResponseSchema**\<`T`\>(`dataSchema`): `ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:183](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L183)

## Type Parameters

### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

## Parameters

### dataSchema

`T`

## Returns

`ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>
