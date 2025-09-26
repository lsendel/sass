# Variable: InvoiceSchema

> `const` **InvoiceSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `downloadUrl`: `ZodOptional`\<`ZodString`\>; `dueDate`: `ZodString`; `id`: `ZodString`; `invoiceNumber`: `ZodString`; `organizationId`: `ZodString`; `paidAt`: `ZodOptional`\<`ZodString`\>; `status`: `ZodEnum`\<\{ `DRAFT`: `"DRAFT"`; `OPEN`: `"OPEN"`; `PAID`: `"PAID"`; `UNCOLLECTIBLE`: `"UNCOLLECTIBLE"`; `VOID`: `"VOID"`; \}\>; `subscriptionId`: `ZodString`; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:167](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L167)
