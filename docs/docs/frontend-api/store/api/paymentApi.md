[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/api/paymentApi

# store/api/paymentApi

## Type Aliases

### AttachPaymentMethodRequest

> **AttachPaymentMethodRequest** = `z.infer`\<*typeof* `AttachPaymentMethodRequestSchema`\>

Defined in: [src/store/api/paymentApi.ts:70](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L70)

***

### ConfirmPaymentIntentRequest

> **ConfirmPaymentIntentRequest** = `z.infer`\<*typeof* `ConfirmPaymentIntentRequestSchema`\>

Defined in: [src/store/api/paymentApi.ts:69](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L69)

***

### CreatePaymentIntentRequest

> **CreatePaymentIntentRequest** = `z.infer`\<*typeof* `CreatePaymentIntentRequestSchema`\>

Defined in: [src/store/api/paymentApi.ts:67](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L67)

***

### CreateSetupIntentRequest

> **CreateSetupIntentRequest** = `z.infer`\<*typeof* `CreateSetupIntentRequestSchema`\>

Defined in: [src/store/api/paymentApi.ts:68](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L68)

***

### PaymentIntent

> **PaymentIntent** = `z.infer`\<*typeof* `PaymentIntentSchema`\>

Defined in: [src/store/api/paymentApi.ts:64](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L64)

***

### PaymentStatistics

> **PaymentStatistics** = `z.infer`\<*typeof* `PaymentStatisticsSchema`\>

Defined in: [src/store/api/paymentApi.ts:66](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L66)

***

### SetupIntent

> **SetupIntent** = `z.infer`\<*typeof* `SetupIntentSchema`\>

Defined in: [src/store/api/paymentApi.ts:65](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L65)

## Variables

### paymentApi

> `const` **paymentApi**: `Api`\<`BaseQueryFn`, \{ `attachPaymentMethod`: `MutationDefinition`\<\{ `organizationId`: `string`; `stripePaymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `brand?`: `string`; `createdAt`: `string`; `expiryMonth?`: `number`; `expiryYear?`: `number`; `id`: `string`; `isDefault`: `boolean`; `last4?`: `string`; `type`: `"CARD"` \| `"BANK_TRANSFER"` \| `"DIGITAL_WALLET"`; \}, `"paymentApi"`, `unknown`\>; `confirmPaymentIntent`: `MutationDefinition`\<`object` & `object`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `amount`: `number`; `clientSecret`: `string`; `currency`: `string`; `description`: `null` \| `string`; `id`: `string`; `metadata`: `null` \| `Record`\<`string`, `unknown`\>; `status`: `string`; \}, `"paymentApi"`, `unknown`\>; `createPaymentIntent`: `MutationDefinition`\<\{ `amount`: `number`; `currency`: `string`; `description?`: `string`; `metadata?`: `Record`\<`string`, `unknown`\>; `organizationId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `amount`: `number`; `clientSecret`: `string`; `currency`: `string`; `description`: `null` \| `string`; `id`: `string`; `metadata`: `null` \| `Record`\<`string`, `unknown`\>; `status`: `string`; \}, `"paymentApi"`, `unknown`\>; `createSetupIntent`: `MutationDefinition`\<\{ `organizationId`: `string`; `usage?`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `client_secret`: `string`; `id`: `string`; `status`: `string`; `usage?`: `string`; \}, `"paymentApi"`, `unknown`\>; `detachPaymentMethod`: `MutationDefinition`\<\{ `organizationId`: `string`; `paymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `void`, `"paymentApi"`, `unknown`\>; `getOrganizationPaymentMethods`: `QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>; `getOrganizationPayments`: `QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>; `getPaymentsByStatus`: `QueryDefinition`\<\{ `organizationId`: `string`; `status`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>; `getPaymentStatistics`: `QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `recentAmount`: `number`; `totalAmount`: `number`; `totalSuccessfulPayments`: `number`; \}, `"paymentApi"`, `unknown`\>; `setDefaultPaymentMethod`: `MutationDefinition`\<\{ `organizationId`: `string`; `paymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `brand?`: `string`; `createdAt`: `string`; `expiryMonth?`: `number`; `expiryYear?`: `number`; `id`: `string`; `isDefault`: `boolean`; `last4?`: `string`; `type`: `"CARD"` \| `"BANK_TRANSFER"` \| `"DIGITAL_WALLET"`; \}, `"paymentApi"`, `unknown`\>; \}, `"paymentApi"`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, *typeof* `coreModuleName` \| *typeof* `reactHooksModuleName`\>

Defined in: [src/store/api/paymentApi.ts:72](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L72)

***

### useAttachPaymentMethodMutation

> **useAttachPaymentMethodMutation**: `UseMutation`\<`MutationDefinition`\<\{ `organizationId`: `string`; `stripePaymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `brand?`: `string`; `createdAt`: `string`; `expiryMonth?`: `number`; `expiryYear?`: `number`; `id`: `string`; `isDefault`: `boolean`; `last4?`: `string`; `type`: `"CARD"` \| `"BANK_TRANSFER"` \| `"DIGITAL_WALLET"`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:267](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L267)

***

### useConfirmPaymentIntentMutation

> **useConfirmPaymentIntentMutation**: `UseMutation`\<`MutationDefinition`\<`object` & `object`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `amount`: `number`; `clientSecret`: `string`; `currency`: `string`; `description`: `null` \| `string`; `id`: `string`; `metadata`: `null` \| `Record`\<`string`, `unknown`\>; `status`: `string`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:263](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L263)

***

### useCreatePaymentIntentMutation

> **useCreatePaymentIntentMutation**: `UseMutation`\<`MutationDefinition`\<\{ `amount`: `number`; `currency`: `string`; `description?`: `string`; `metadata?`: `Record`\<`string`, `unknown`\>; `organizationId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `amount`: `number`; `clientSecret`: `string`; `currency`: `string`; `description`: `null` \| `string`; `id`: `string`; `metadata`: `null` \| `Record`\<`string`, `unknown`\>; `status`: `string`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:262](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L262)

***

### useCreateSetupIntentMutation

> **useCreateSetupIntentMutation**: `UseMutation`\<`MutationDefinition`\<\{ `organizationId`: `string`; `usage?`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `client_secret`: `string`; `id`: `string`; `status`: `string`; `usage?`: `string`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:261](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L261)

***

### useDetachPaymentMethodMutation

> **useDetachPaymentMethodMutation**: `UseMutation`\<`MutationDefinition`\<\{ `organizationId`: `string`; `paymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `void`, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:268](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L268)

***

### useGetOrganizationPaymentMethodsQuery

> **useGetOrganizationPaymentMethodsQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:270](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L270)

***

### useGetOrganizationPaymentsQuery

> **useGetOrganizationPaymentsQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:264](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L264)

***

### useGetPaymentsByStatusQuery

> **useGetPaymentsByStatusQuery**: `UseQuery`\<`QueryDefinition`\<\{ `organizationId`: `string`; `status`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:265](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L265)

***

### useGetPaymentStatisticsQuery

> **useGetPaymentStatisticsQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `recentAmount`: `number`; `totalAmount`: `number`; `totalSuccessfulPayments`: `number`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:266](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L266)

***

### useLazyGetOrganizationPaymentsQuery

> **useLazyGetOrganizationPaymentsQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, `object`[], `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:272](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L272)

***

### useLazyGetPaymentStatisticsQuery

> **useLazyGetPaymentStatisticsQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `recentAmount`: `number`; `totalAmount`: `number`; `totalSuccessfulPayments`: `number`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:273](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L273)

***

### useSetDefaultPaymentMethodMutation

> **useSetDefaultPaymentMethodMutation**: `UseMutation`\<`MutationDefinition`\<\{ `organizationId`: `string`; `paymentMethodId`: `string`; \}, `BaseQueryFn`, `"Payment"` \| `"PaymentMethod"` \| `"PaymentStatistics"` \| `"SetupIntent"`, \{ `brand?`: `string`; `createdAt`: `string`; `expiryMonth?`: `number`; `expiryYear?`: `number`; `id`: `string`; `isDefault`: `boolean`; `last4?`: `string`; `type`: `"CARD"` \| `"BANK_TRANSFER"` \| `"DIGITAL_WALLET"`; \}, `"paymentApi"`, `unknown`\>\>

Defined in: [src/store/api/paymentApi.ts:269](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/paymentApi.ts#L269)
