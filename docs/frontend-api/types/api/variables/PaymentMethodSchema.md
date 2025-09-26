# Variable: PaymentMethodSchema

> `const` **PaymentMethodSchema**: `ZodObject`\<\{ `brand`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `expiryMonth`: `ZodOptional`\<`ZodNumber`\>; `expiryYear`: `ZodOptional`\<`ZodNumber`\>; `id`: `ZodString`; `isDefault`: `ZodBoolean`; `last4`: `ZodOptional`\<`ZodString`\>; `type`: `ZodEnum`\<\{ `BANK_TRANSFER`: `"BANK_TRANSFER"`; `CARD`: `"CARD"`; `DIGITAL_WALLET`: `"DIGITAL_WALLET"`; \}\>; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:99](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L99)
