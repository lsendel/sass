# Function: PaginatedResponseSchema()

> **PaginatedResponseSchema**\<`T`\>(`itemSchema`): `ZodObject`\<\{ `data`: `ZodObject`\<\{ `items`: `ZodArray`\<`T`\>; `pagination`: `ZodObject`\<\{ `hasNext`: `ZodBoolean`; `hasPrevious`: `ZodBoolean`; `page`: `ZodNumber`; `size`: `ZodNumber`; `totalElements`: `ZodNumber`; `totalPages`: `ZodNumber`; \}, `$strip`\>; \}, `$strip`\>; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:202](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L202)

## Type Parameters

### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

## Parameters

### itemSchema

`T`

## Returns

`ZodObject`\<\{ `data`: `ZodObject`\<\{ `items`: `ZodArray`\<`T`\>; `pagination`: `ZodObject`\<\{ `hasNext`: `ZodBoolean`; `hasPrevious`: `ZodBoolean`; `page`: `ZodNumber`; `size`: `ZodNumber`; `totalElements`: `ZodNumber`; `totalPages`: `ZodNumber`; \}, `$strip`\>; \}, `$strip`\>; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>
