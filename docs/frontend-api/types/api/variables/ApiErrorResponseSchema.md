# Variable: ApiErrorResponseSchema

> `const` **ApiErrorResponseSchema**: `ZodObject`\<\{ `correlationId`: `ZodOptional`\<`ZodString`\>; `error`: `ZodObject`\<\{ `code`: `ZodString`; `details`: `ZodOptional`\<`ZodRecord`\<`ZodAny`, `SomeType`\>\>; `message`: `ZodString`; \}, `$strip`\>; `success`: `ZodLiteral`\<`false`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:191](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L191)
