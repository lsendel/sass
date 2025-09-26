# Variable: PlanSchema

> `const` **PlanSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `description`: `ZodOptional`\<`ZodString`\>; `features`: `ZodArray`\<`ZodString`\>; `id`: `ZodString`; `interval`: `ZodEnum`\<\{ `MONTHLY`: `"MONTHLY"`; `YEARLY`: `"YEARLY"`; \}\>; `isActive`: `ZodBoolean`; `maxUsers`: `ZodNumber`; `name`: `ZodString`; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:138](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L138)
