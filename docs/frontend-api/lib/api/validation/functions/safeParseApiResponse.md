# Function: safeParseApiResponse()

> **safeParseApiResponse**\<`T`\>(`schema`, `data`, `fallback?`): \{ `data`: `T`; `success`: `true`; \} \| \{ `error`: `ZodError`; `fallback?`: `T`; `success`: `false`; \}

Defined in: [frontend/src/lib/api/validation.ts:224](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/lib/api/validation.ts#L224)

Utility to safely parse API responses with fallback

## Type Parameters

### T

`T`

## Parameters

### schema

`ZodType`\<`T`\>

### data

`unknown`

### fallback?

`T`

## Returns

\{ `data`: `T`; `success`: `true`; \} \| \{ `error`: `ZodError`; `fallback?`: `T`; `success`: `false`; \}
