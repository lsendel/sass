# Variable: UserSchema

> `const` **UserSchema**: `ZodObject`\<\{ `createdAt`: `ZodString`; `email`: `ZodString`; `emailVerified`: `ZodBoolean`; `firstName`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `lastLoginAt`: `ZodOptional`\<`ZodString`\>; `lastName`: `ZodOptional`\<`ZodString`\>; `organizationId`: `ZodOptional`\<`ZodString`\>; `role`: `ZodEnum`\<\{ `ADMIN`: `"ADMIN"`; `ORGANIZATION_ADMIN`: `"ORGANIZATION_ADMIN"`; `USER`: `"USER"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:15](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L15)
