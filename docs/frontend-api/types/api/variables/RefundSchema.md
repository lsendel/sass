# Variable: RefundSchema

> `const` **RefundSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `id`: `ZodString`; `paymentId`: `ZodString`; `processedAt`: `ZodOptional`\<`ZodString`\>; `reason`: `ZodEnum`\<\{ `DUPLICATE`: `"DUPLICATE"`; `FRAUDULENT`: `"FRAUDULENT"`; `REQUESTED_BY_CUSTOMER`: `"REQUESTED_BY_CUSTOMER"`; `SUBSCRIPTION_CANCELED`: `"SUBSCRIPTION_CANCELED"`; \}\>; `status`: `ZodEnum`\<\{ `CANCELED`: `"CANCELED"`; `FAILED`: `"FAILED"`; `PENDING`: `"PENDING"`; `SUCCEEDED`: `"SUCCEEDED"`; \}\>; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:126](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L126)
