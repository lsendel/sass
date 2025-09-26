# Variable: PaymentSchema

> `const` **PaymentSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `customerId`: `ZodString`; `description`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `metadata`: `ZodOptional`\<`ZodRecord`\<`ZodString`, `SomeType`\>\>; `organizationId`: `ZodString`; `paidAt`: `ZodOptional`\<`ZodString`\>; `paymentMethodId`: `ZodString`; `status`: `ZodEnum`\<\{ `COMPLETED`: `"COMPLETED"`; `FAILED`: `"FAILED"`; `PENDING`: `"PENDING"`; `REFUNDED`: `"REFUNDED"`; \}\>; `subscriptionId`: `ZodOptional`\<`ZodString`\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:110](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L110)
