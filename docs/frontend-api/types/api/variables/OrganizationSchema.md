# Variable: OrganizationSchema

> `const` **OrganizationSchema**: `ZodObject`\<\{ `billingEmail`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `currentUsers`: `ZodNumber`; `id`: `ZodString`; `maxUsers`: `ZodNumber`; `name`: `ZodString`; `plan`: `ZodEnum`\<\{ `ENTERPRISE`: `"ENTERPRISE"`; `FREE`: `"FREE"`; `PRO`: `"PRO"`; \}\>; `status`: `ZodEnum`\<\{ `ACTIVE`: `"ACTIVE"`; `DELETED`: `"DELETED"`; `SUSPENDED`: `"SUSPENDED"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:28](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L28)
