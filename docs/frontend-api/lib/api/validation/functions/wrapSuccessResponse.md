# Function: wrapSuccessResponse()

> **wrapSuccessResponse**\<`T`\>(`dataSchema`): `ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/lib/api/validation.ts:141](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L141)

Wraps a success response schema with the standard API wrapper

## Type Parameters

### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

## Parameters

### dataSchema

`T`

## Returns

`ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>
