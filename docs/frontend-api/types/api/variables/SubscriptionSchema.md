# Variable: SubscriptionSchema

> `const` **SubscriptionSchema**: `ZodObject`\<\{ `cancelAtPeriodEnd`: `ZodBoolean`; `canceledAt`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `currentPeriodEnd`: `ZodString`; `currentPeriodStart`: `ZodString`; `id`: `ZodString`; `organizationId`: `ZodString`; `planId`: `ZodString`; `status`: `ZodEnum`\<\{ `ACTIVE`: `"ACTIVE"`; `CANCELED`: `"CANCELED"`; `PAST_DUE`: `"PAST_DUE"`; `PAUSED`: `"PAUSED"`; \}\>; `trialEnd`: `ZodOptional`\<`ZodString`\>; `trialStart`: `ZodOptional`\<`ZodString`\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [frontend/src/types/api.ts:152](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/api.ts#L152)
