[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / types/api

# types/api

## Type Aliases

### ApiErrorResponse

> **ApiErrorResponse** = `z.infer`\<*typeof* [`ApiErrorResponseSchema`](#apierrorresponseschema)\>

Defined in: [src/types/api.ts:273](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L273)

***

### ApiSuccessResponse

> **ApiSuccessResponse**\<`T`\> = `object`

Defined in: [src/types/api.ts:266](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L266)

#### Type Parameters

##### T

`T`

#### Properties

##### data

> **data**: `T`

Defined in: [src/types/api.ts:268](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L268)

##### message?

> `optional` **message**: `string`

Defined in: [src/types/api.ts:269](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L269)

##### success

> **success**: `true`

Defined in: [src/types/api.ts:267](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L267)

##### timestamp

> **timestamp**: `string`

Defined in: [src/types/api.ts:270](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L270)

***

### AuditEvent

> **AuditEvent** = `z.infer`\<*typeof* [`AuditEventSchema`](#auditeventschema)\>

Defined in: [src/types/api.ts:264](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L264)

***

### AuthMethodsResponse

> **AuthMethodsResponse** = `z.infer`\<*typeof* [`AuthMethodsResponseSchema`](#authmethodsresponseschema)\>

Defined in: [src/types/api.ts:247](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L247)

***

### AuthUrlResponse

> **AuthUrlResponse** = `z.infer`\<*typeof* [`AuthUrlResponseSchema`](#authurlresponseschema)\>

Defined in: [src/types/api.ts:249](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L249)

***

### CallbackRequest

> **CallbackRequest** = `z.infer`\<*typeof* [`CallbackRequestSchema`](#callbackrequestschema)\>

Defined in: [src/types/api.ts:254](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L254)

***

### Invoice

> **Invoice** = `z.infer`\<*typeof* [`InvoiceSchema`](#invoiceschema)\>

Defined in: [src/types/api.ts:262](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L262)

***

### LoginRequest

> **LoginRequest** = `z.infer`\<*typeof* [`LoginRequestSchema`](#loginrequestschema)\>

Defined in: [src/types/api.ts:251](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L251)

***

### LoginResponse

> **LoginResponse** = `z.infer`\<*typeof* [`LoginResponseSchema`](#loginresponseschema)\>

Defined in: [src/types/api.ts:248](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L248)

***

### OAuth2Provider

> **OAuth2Provider** = `z.infer`\<*typeof* [`OAuth2ProviderSchema`](#oauth2providerschema)\>

Defined in: [src/types/api.ts:245](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L245)

***

### Organization

> **Organization** = `z.infer`\<*typeof* [`OrganizationSchema`](#organizationschema)\>

Defined in: [src/types/api.ts:244](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L244)

***

### PaginatedResponse

> **PaginatedResponse**\<`T`\> = [`ApiSuccessResponse`](#apisuccessresponse)\<\{ `items`: `T`[]; `pagination`: \{ `hasNext`: `boolean`; `hasPrevious`: `boolean`; `page`: `number`; `size`: `number`; `totalElements`: `number`; `totalPages`: `number`; \}; \}\>

Defined in: [src/types/api.ts:275](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L275)

#### Type Parameters

##### T

`T`

***

### PasswordLoginRequest

> **PasswordLoginRequest** = `z.infer`\<*typeof* [`PasswordLoginRequestSchema`](#passwordloginrequestschema)\>

Defined in: [src/types/api.ts:252](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L252)

***

### PasswordRegisterRequest

> **PasswordRegisterRequest** = `z.infer`\<*typeof* [`PasswordRegisterRequestSchema`](#passwordregisterrequestschema)\>

Defined in: [src/types/api.ts:253](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L253)

***

### Payment

> **Payment** = `z.infer`\<*typeof* [`PaymentSchema`](#paymentschema)\>

Defined in: [src/types/api.ts:257](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L257)

***

### PaymentMethod

> **PaymentMethod** = `z.infer`\<*typeof* [`PaymentMethodSchema`](#paymentmethodschema)\>

Defined in: [src/types/api.ts:256](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L256)

***

### Plan

> **Plan** = `z.infer`\<*typeof* [`PlanSchema`](#planschema)\>

Defined in: [src/types/api.ts:260](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L260)

***

### Refund

> **Refund** = `z.infer`\<*typeof* [`RefundSchema`](#refundschema)\>

Defined in: [src/types/api.ts:258](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L258)

***

### SessionInfo

> **SessionInfo** = `z.infer`\<*typeof* [`SessionInfoSchema`](#sessioninfoschema)\>

Defined in: [src/types/api.ts:246](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L246)

***

### Subscription

> **Subscription** = `z.infer`\<*typeof* [`SubscriptionSchema`](#subscriptionschema)\>

Defined in: [src/types/api.ts:261](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L261)

***

### User

> **User** = `z.infer`\<*typeof* [`UserSchema`](#userschema)\>

Defined in: [src/types/api.ts:243](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L243)

## Variables

### ApiErrorResponseSchema

> `const` **ApiErrorResponseSchema**: `ZodObject`\<\{ `correlationId`: `ZodOptional`\<`ZodString`\>; `error`: `ZodObject`\<\{ `code`: `ZodString`; `details`: `ZodOptional`\<`ZodRecord`\<`ZodAny`, `SomeType`\>\>; `message`: `ZodString`; \}, `$strip`\>; `success`: `ZodLiteral`\<`false`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:191](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L191)

***

### AuditEventSchema

> `const` **AuditEventSchema**: `ZodObject`\<\{ `action`: `ZodString`; `correlationId`: `ZodOptional`\<`ZodString`\>; `details`: `ZodRecord`\<`ZodAny`, `SomeType`\>; `entityId`: `ZodString`; `entityType`: `ZodString`; `eventType`: `ZodString`; `id`: `ZodString`; `ipAddress`: `ZodOptional`\<`ZodString`\>; `organizationId`: `ZodOptional`\<`ZodString`\>; `timestamp`: `ZodString`; `userAgent`: `ZodOptional`\<`ZodString`\>; `userId`: `ZodOptional`\<`ZodString`\>; \}, `$strip`\>

Defined in: [src/types/api.ts:218](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L218)

***

### AuthMethodsResponseSchema

> `const` **AuthMethodsResponseSchema**: `ZodObject`\<\{ `methods`: `ZodArray`\<`ZodString`\>; `oauth2Providers`: `ZodArray`\<`ZodString`\>; `passwordAuthEnabled`: `ZodBoolean`; \}, `$strip`\>

Defined in: [src/types/api.ts:57](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L57)

***

### AuthUrlResponseSchema

> `const` **AuthUrlResponseSchema**: `ZodObject`\<\{ `authUrl`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:68](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L68)

***

### CallbackRequestSchema

> `const` **CallbackRequestSchema**: `ZodObject`\<\{ `code`: `ZodString`; `state`: `ZodOptional`\<`ZodString`\>; \}, `$strip`\>

Defined in: [src/types/api.ts:93](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L93)

***

### InvoiceSchema

> `const` **InvoiceSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `downloadUrl`: `ZodOptional`\<`ZodString`\>; `dueDate`: `ZodString`; `id`: `ZodString`; `invoiceNumber`: `ZodString`; `organizationId`: `ZodString`; `paidAt`: `ZodOptional`\<`ZodString`\>; `status`: `ZodEnum`\<\{ `DRAFT`: `"DRAFT"`; `OPEN`: `"OPEN"`; `PAID`: `"PAID"`; `UNCOLLECTIBLE`: `"UNCOLLECTIBLE"`; `VOID`: `"VOID"`; \}\>; `subscriptionId`: `ZodString`; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:167](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L167)

***

### LoginRequestSchema

> `const` **LoginRequestSchema**: `ZodObject`\<\{ `provider`: `ZodString`; `redirectUri`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:73](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L73)

***

### LoginResponseSchema

> `const` **LoginResponseSchema**: `ZodObject`\<\{ `token`: `ZodString`; `user`: `ZodObject`\<\{ `createdAt`: `ZodString`; `email`: `ZodString`; `emailVerified`: `ZodBoolean`; `firstName`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `lastLoginAt`: `ZodOptional`\<`ZodString`\>; `lastName`: `ZodOptional`\<`ZodString`\>; `organizationId`: `ZodOptional`\<`ZodString`\>; `role`: `ZodEnum`\<\{ `ADMIN`: `"ADMIN"`; `ORGANIZATION_ADMIN`: `"ORGANIZATION_ADMIN"`; `USER`: `"USER"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>; \}, `$strip`\>

Defined in: [src/types/api.ts:63](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L63)

***

### OAuth2ProviderSchema

> `const` **OAuth2ProviderSchema**: `ZodObject`\<\{ `authUrl`: `ZodString`; `displayName`: `ZodString`; `iconUrl`: `ZodOptional`\<`ZodString`\>; `name`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:41](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L41)

***

### OrganizationPlanSchema

> `const` **OrganizationPlanSchema**: `ZodEnum`\<\{ `ENTERPRISE`: `"ENTERPRISE"`; `FREE`: `"FREE"`; `PRO`: `"PRO"`; \}\>

Defined in: [src/types/api.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L10)

***

### OrganizationSchema

> `const` **OrganizationSchema**: `ZodObject`\<\{ `billingEmail`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `currentUsers`: `ZodNumber`; `id`: `ZodString`; `maxUsers`: `ZodNumber`; `name`: `ZodString`; `plan`: `ZodEnum`\<\{ `ENTERPRISE`: `"ENTERPRISE"`; `FREE`: `"FREE"`; `PRO`: `"PRO"`; \}\>; `status`: `ZodEnum`\<\{ `ACTIVE`: `"ACTIVE"`; `DELETED`: `"DELETED"`; `SUSPENDED`: `"SUSPENDED"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L28)

***

### PasswordLoginRequestSchema

> `const` **PasswordLoginRequestSchema**: `ZodObject`\<\{ `email`: `ZodString`; `organizationId`: `ZodString`; `password`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:78](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L78)

***

### PasswordRegisterRequestSchema

> `const` **PasswordRegisterRequestSchema**: `ZodObject`\<\{ `email`: `ZodString`; `name`: `ZodString`; `password`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:84](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L84)

***

### PaymentMethodSchema

> `const` **PaymentMethodSchema**: `ZodObject`\<\{ `brand`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `expiryMonth`: `ZodOptional`\<`ZodNumber`\>; `expiryYear`: `ZodOptional`\<`ZodNumber`\>; `id`: `ZodString`; `isDefault`: `ZodBoolean`; `last4`: `ZodOptional`\<`ZodString`\>; `type`: `ZodEnum`\<\{ `BANK_TRANSFER`: `"BANK_TRANSFER"`; `CARD`: `"CARD"`; `DIGITAL_WALLET`: `"DIGITAL_WALLET"`; \}\>; \}, `$strip`\>

Defined in: [src/types/api.ts:99](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L99)

***

### PaymentSchema

> `const` **PaymentSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `customerId`: `ZodString`; `description`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `metadata`: `ZodOptional`\<`ZodRecord`\<`ZodString`, `SomeType`\>\>; `organizationId`: `ZodString`; `paidAt`: `ZodOptional`\<`ZodString`\>; `paymentMethodId`: `ZodString`; `status`: `ZodEnum`\<\{ `COMPLETED`: `"COMPLETED"`; `FAILED`: `"FAILED"`; `PENDING`: `"PENDING"`; `REFUNDED`: `"REFUNDED"`; \}\>; `subscriptionId`: `ZodOptional`\<`ZodString`\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:110](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L110)

***

### PaymentStatusSchema

> `const` **PaymentStatusSchema**: `ZodEnum`\<\{ `COMPLETED`: `"COMPLETED"`; `FAILED`: `"FAILED"`; `PENDING`: `"PENDING"`; `REFUNDED`: `"REFUNDED"`; \}\>

Defined in: [src/types/api.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L11)

***

### PlanSchema

> `const` **PlanSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `description`: `ZodOptional`\<`ZodString`\>; `features`: `ZodArray`\<`ZodString`\>; `id`: `ZodString`; `interval`: `ZodEnum`\<\{ `MONTHLY`: `"MONTHLY"`; `YEARLY`: `"YEARLY"`; \}\>; `isActive`: `ZodBoolean`; `maxUsers`: `ZodNumber`; `name`: `ZodString`; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:138](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L138)

***

### RefundSchema

> `const` **RefundSchema**: `ZodObject`\<\{ `amount`: `ZodNumber`; `createdAt`: `ZodString`; `currency`: `ZodString`; `id`: `ZodString`; `paymentId`: `ZodString`; `processedAt`: `ZodOptional`\<`ZodString`\>; `reason`: `ZodEnum`\<\{ `DUPLICATE`: `"DUPLICATE"`; `FRAUDULENT`: `"FRAUDULENT"`; `REQUESTED_BY_CUSTOMER`: `"REQUESTED_BY_CUSTOMER"`; `SUBSCRIPTION_CANCELED`: `"SUBSCRIPTION_CANCELED"`; \}\>; `status`: `ZodEnum`\<\{ `CANCELED`: `"CANCELED"`; `FAILED`: `"FAILED"`; `PENDING`: `"PENDING"`; `SUCCEEDED`: `"SUCCEEDED"`; \}\>; \}, `$strip`\>

Defined in: [src/types/api.ts:126](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L126)

***

### SessionInfoSchema

> `const` **SessionInfoSchema**: `ZodObject`\<\{ `session`: `ZodObject`\<\{ `activeTokens`: `ZodNumber`; `createdAt`: `ZodString`; `lastActiveAt`: `ZodString`; \}, `$strip`\>; `user`: `ZodObject`\<\{ `createdAt`: `ZodString`; `email`: `ZodString`; `emailVerified`: `ZodBoolean`; `firstName`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `lastLoginAt`: `ZodOptional`\<`ZodString`\>; `lastName`: `ZodOptional`\<`ZodString`\>; `organizationId`: `ZodOptional`\<`ZodString`\>; `role`: `ZodEnum`\<\{ `ADMIN`: `"ADMIN"`; `ORGANIZATION_ADMIN`: `"ORGANIZATION_ADMIN"`; `USER`: `"USER"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>; \}, `$strip`\>

Defined in: [src/types/api.ts:48](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L48)

***

### SubscriptionSchema

> `const` **SubscriptionSchema**: `ZodObject`\<\{ `cancelAtPeriodEnd`: `ZodBoolean`; `canceledAt`: `ZodOptional`\<`ZodString`\>; `createdAt`: `ZodString`; `currentPeriodEnd`: `ZodString`; `currentPeriodStart`: `ZodString`; `id`: `ZodString`; `organizationId`: `ZodString`; `planId`: `ZodString`; `status`: `ZodEnum`\<\{ `ACTIVE`: `"ACTIVE"`; `CANCELED`: `"CANCELED"`; `PAST_DUE`: `"PAST_DUE"`; `PAUSED`: `"PAUSED"`; \}\>; `trialEnd`: `ZodOptional`\<`ZodString`\>; `trialStart`: `ZodOptional`\<`ZodString`\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:152](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L152)

***

### SubscriptionStatusSchema

> `const` **SubscriptionStatusSchema**: `ZodEnum`\<\{ `ACTIVE`: `"ACTIVE"`; `CANCELED`: `"CANCELED"`; `PAST_DUE`: `"PAST_DUE"`; `PAUSED`: `"PAUSED"`; \}\>

Defined in: [src/types/api.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L12)

***

### UserRoleSchema

> `const` **UserRoleSchema**: `ZodEnum`\<\{ `ADMIN`: `"ADMIN"`; `ORGANIZATION_ADMIN`: `"ORGANIZATION_ADMIN"`; `USER`: `"USER"`; \}\>

Defined in: [src/types/api.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L9)

Comprehensive Zod schemas for runtime API type validation
These schemas ensure type safety and provide validation for all API responses

***

### UserSchema

> `const` **UserSchema**: `ZodObject`\<\{ `createdAt`: `ZodString`; `email`: `ZodString`; `emailVerified`: `ZodBoolean`; `firstName`: `ZodOptional`\<`ZodString`\>; `id`: `ZodString`; `lastLoginAt`: `ZodOptional`\<`ZodString`\>; `lastName`: `ZodOptional`\<`ZodString`\>; `organizationId`: `ZodOptional`\<`ZodString`\>; `role`: `ZodEnum`\<\{ `ADMIN`: `"ADMIN"`; `ORGANIZATION_ADMIN`: `"ORGANIZATION_ADMIN"`; `USER`: `"USER"`; \}\>; `updatedAt`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L15)

## Functions

### ApiSuccessResponseSchema()

> **ApiSuccessResponseSchema**\<`T`\>(`dataSchema`): `ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:183](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L183)

#### Type Parameters

##### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

#### Parameters

##### dataSchema

`T`

#### Returns

`ZodObject`\<\{ `data`: `T`; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

***

### PaginatedResponseSchema()

> **PaginatedResponseSchema**\<`T`\>(`itemSchema`): `ZodObject`\<\{ `data`: `ZodObject`\<\{ `items`: `ZodArray`\<`T`\>; `pagination`: `ZodObject`\<\{ `hasNext`: `ZodBoolean`; `hasPrevious`: `ZodBoolean`; `page`: `ZodNumber`; `size`: `ZodNumber`; `totalElements`: `ZodNumber`; `totalPages`: `ZodNumber`; \}, `$strip`\>; \}, `$strip`\>; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>

Defined in: [src/types/api.ts:202](https://github.com/lsendel/sass/blob/main/frontend/src/types/api.ts#L202)

#### Type Parameters

##### T

`T` *extends* `ZodType`\<`unknown`, `unknown`, `$ZodTypeInternals`\<`unknown`, `unknown`\>\>

#### Parameters

##### itemSchema

`T`

#### Returns

`ZodObject`\<\{ `data`: `ZodObject`\<\{ `items`: `ZodArray`\<`T`\>; `pagination`: `ZodObject`\<\{ `hasNext`: `ZodBoolean`; `hasPrevious`: `ZodBoolean`; `page`: `ZodNumber`; `size`: `ZodNumber`; `totalElements`: `ZodNumber`; `totalPages`: `ZodNumber`; \}, `$strip`\>; \}, `$strip`\>; `message`: `ZodOptional`\<`ZodString`\>; `success`: `ZodLiteral`\<`true`\>; `timestamp`: `ZodString`; \}, `$strip`\>
