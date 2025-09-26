[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/api/subscriptionApi

# store/api/subscriptionApi

## Type Aliases

### CancelSubscriptionRequest

> **CancelSubscriptionRequest** = `object`

Defined in: [src/store/api/subscriptionApi.ts:87](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L87)

#### Properties

##### cancelAt?

> `optional` **cancelAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:90](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L90)

##### immediate?

> `optional` **immediate**: `boolean`

Defined in: [src/store/api/subscriptionApi.ts:89](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L89)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:88](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L88)

***

### ChangePlanRequest

> **ChangePlanRequest** = `object`

Defined in: [src/store/api/subscriptionApi.ts:81](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L81)

#### Properties

##### newPlanId

> **newPlanId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:83](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L83)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:82](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L82)

##### prorationBehavior?

> `optional` **prorationBehavior**: `boolean`

Defined in: [src/store/api/subscriptionApi.ts:84](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L84)

***

### CreateSubscriptionRequest

> **CreateSubscriptionRequest** = `object`

Defined in: [src/store/api/subscriptionApi.ts:74](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L74)

#### Properties

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:75](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L75)

##### paymentMethodId?

> `optional` **paymentMethodId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:77](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L77)

##### planId

> **planId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:76](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L76)

##### trialEligible?

> `optional` **trialEligible**: `boolean`

Defined in: [src/store/api/subscriptionApi.ts:78](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L78)

***

### Invoice

> **Invoice** = `object`

Defined in: [src/store/api/subscriptionApi.ts:51](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L51)

#### Properties

##### createdAt

> **createdAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:64](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L64)

##### currency

> **currency**: `string`

Defined in: [src/store/api/subscriptionApi.ts:61](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L61)

##### dueDate

> **dueDate**: `string` \| `null`

Defined in: [src/store/api/subscriptionApi.ts:62](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L62)

##### id

> **id**: `string`

Defined in: [src/store/api/subscriptionApi.ts:52](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L52)

##### invoiceNumber

> **invoiceNumber**: `string`

Defined in: [src/store/api/subscriptionApi.ts:56](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L56)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:53](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L53)

##### paidAt

> **paidAt**: `string` \| `null`

Defined in: [src/store/api/subscriptionApi.ts:63](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L63)

##### status

> **status**: [`InvoiceStatus`](#invoicestatus-1)

Defined in: [src/store/api/subscriptionApi.ts:57](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L57)

##### stripeInvoiceId

> **stripeInvoiceId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:55](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L55)

##### subscriptionId

> **subscriptionId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:54](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L54)

##### subtotalAmount

> **subtotalAmount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:58](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L58)

##### taxAmount

> **taxAmount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:59](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L59)

##### totalAmount

> **totalAmount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:60](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L60)

***

### InvoiceStatus

> **InvoiceStatus** = `"DRAFT"` \| `"OPEN"` \| `"PAID"` \| `"VOID"` \| `"UNCOLLECTIBLE"`

Defined in: [src/store/api/subscriptionApi.ts:49](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L49)

***

### Plan

> **Plan** = `object`

Defined in: [src/store/api/subscriptionApi.ts:32](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L32)

#### Properties

##### active

> **active**: `boolean`

Defined in: [src/store/api/subscriptionApi.ts:42](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L42)

##### amount

> **amount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:37](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L37)

##### createdAt

> **createdAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:45](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L45)

##### currency

> **currency**: `string`

Defined in: [src/store/api/subscriptionApi.ts:38](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L38)

##### description

> **description**: `string`

Defined in: [src/store/api/subscriptionApi.ts:36](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L36)

##### displayOrder

> **displayOrder**: `number`

Defined in: [src/store/api/subscriptionApi.ts:43](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L43)

##### features

> **features**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/subscriptionApi.ts:44](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L44)

##### id

> **id**: `string`

Defined in: [src/store/api/subscriptionApi.ts:33](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L33)

##### interval

> **interval**: [`PlanInterval`](#planinterval-1)

Defined in: [src/store/api/subscriptionApi.ts:39](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L39)

##### intervalCount

> **intervalCount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:40](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L40)

##### name

> **name**: `string`

Defined in: [src/store/api/subscriptionApi.ts:34](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L34)

##### slug

> **slug**: `string`

Defined in: [src/store/api/subscriptionApi.ts:35](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L35)

##### trialDays

> **trialDays**: `number` \| `null`

Defined in: [src/store/api/subscriptionApi.ts:41](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L41)

##### updatedAt

> **updatedAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:46](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L46)

***

### PlanInterval

> **PlanInterval** = `"DAY"` \| `"WEEK"` \| `"MONTH"` \| `"YEAR"`

Defined in: [src/store/api/subscriptionApi.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L30)

***

### ReactivateSubscriptionRequest

> **ReactivateSubscriptionRequest** = `object`

Defined in: [src/store/api/subscriptionApi.ts:93](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L93)

#### Properties

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:94](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L94)

***

### Subscription

> **Subscription** = `object`

Defined in: [src/store/api/subscriptionApi.ts:16](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L16)

#### Properties

##### cancelAt

> **cancelAt**: `string` \| `null`

Defined in: [src/store/api/subscriptionApi.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L25)

##### createdAt

> **createdAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:26](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L26)

##### currentPeriodEnd

> **currentPeriodEnd**: `string`

Defined in: [src/store/api/subscriptionApi.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L23)

##### currentPeriodStart

> **currentPeriodStart**: `string`

Defined in: [src/store/api/subscriptionApi.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L22)

##### id

> **id**: `string`

Defined in: [src/store/api/subscriptionApi.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L17)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L18)

##### planId

> **planId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L19)

##### status

> **status**: [`SubscriptionStatus`](#subscriptionstatus-1)

Defined in: [src/store/api/subscriptionApi.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L21)

##### stripeSubscriptionId

> **stripeSubscriptionId**: `string`

Defined in: [src/store/api/subscriptionApi.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L20)

##### trialEnd

> **trialEnd**: `string` \| `null`

Defined in: [src/store/api/subscriptionApi.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L24)

##### updatedAt

> **updatedAt**: `string`

Defined in: [src/store/api/subscriptionApi.ts:27](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L27)

***

### SubscriptionStatistics

> **SubscriptionStatistics** = `object`

Defined in: [src/store/api/subscriptionApi.ts:67](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L67)

#### Properties

##### recentAmount

> **recentAmount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:71](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L71)

##### status

> **status**: [`SubscriptionStatus`](#subscriptionstatus-1) \| `null`

Defined in: [src/store/api/subscriptionApi.ts:68](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L68)

##### totalAmount

> **totalAmount**: `number`

Defined in: [src/store/api/subscriptionApi.ts:70](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L70)

##### totalInvoices

> **totalInvoices**: `number`

Defined in: [src/store/api/subscriptionApi.ts:69](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L69)

***

### SubscriptionStatus

> **SubscriptionStatus** = `"ACTIVE"` \| `"TRIALING"` \| `"PAST_DUE"` \| `"CANCELED"` \| `"UNPAID"` \| `"INCOMPLETE"` \| `"INCOMPLETE_EXPIRED"`

Defined in: [src/store/api/subscriptionApi.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L7)

## Variables

### subscriptionApi

> `const` **subscriptionApi**: `Api`\<`BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, \{ `cancelSubscription`: `MutationDefinition`\<`object` & [`CancelSubscriptionRequest`](#cancelsubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>; `changeSubscriptionPlan`: `MutationDefinition`\<`object` & [`ChangePlanRequest`](#changeplanrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>; `createSubscription`: `MutationDefinition`\<[`CreateSubscriptionRequest`](#createsubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>; `getAvailablePlans`: `QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan)[], `"subscriptionApi"`, `unknown`\>; `getOrganizationInvoices`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Invoice`](#invoice)[], `"subscriptionApi"`, `unknown`\>; `getOrganizationSubscription`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>; `getPlanBySlug`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan), `"subscriptionApi"`, `unknown`\>; `getPlansByInterval`: `QueryDefinition`\<[`PlanInterval`](#planinterval-1), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan)[], `"subscriptionApi"`, `unknown`\>; `getSubscriptionStatistics`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`SubscriptionStatistics`](#subscriptionstatistics), `"subscriptionApi"`, `unknown`\>; `reactivateSubscription`: `MutationDefinition`\<`object` & [`ReactivateSubscriptionRequest`](#reactivatesubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>; \}, `"subscriptionApi"`, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, *typeof* `coreModuleName` \| *typeof* `reactHooksModuleName`\>

Defined in: [src/store/api/subscriptionApi.ts:97](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L97)

***

### useCancelSubscriptionMutation

> **useCancelSubscriptionMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`CancelSubscriptionRequest`](#cancelsubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:205](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L205)

***

### useChangeSubscriptionPlanMutation

> **useChangeSubscriptionPlanMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`ChangePlanRequest`](#changeplanrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:204](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L204)

***

### useCreateSubscriptionMutation

> **useCreateSubscriptionMutation**: `UseMutation`\<`MutationDefinition`\<[`CreateSubscriptionRequest`](#createsubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:202](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L202)

***

### useGetAvailablePlansQuery

> **useGetAvailablePlansQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan)[], `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:209](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L209)

***

### useGetOrganizationInvoicesQuery

> **useGetOrganizationInvoicesQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Invoice`](#invoice)[], `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:208](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L208)

***

### useGetOrganizationSubscriptionQuery

> **useGetOrganizationSubscriptionQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:203](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L203)

***

### useGetPlanBySlugQuery

> **useGetPlanBySlugQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:211](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L211)

***

### useGetPlansByIntervalQuery

> **useGetPlansByIntervalQuery**: `UseQuery`\<`QueryDefinition`\<[`PlanInterval`](#planinterval-1), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan)[], `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:210](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L210)

***

### useGetSubscriptionStatisticsQuery

> **useGetSubscriptionStatisticsQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`SubscriptionStatistics`](#subscriptionstatistics), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:207](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L207)

***

### useLazyGetOrganizationSubscriptionQuery

> **useLazyGetOrganizationSubscriptionQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:213](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L213)

***

### useLazyGetPlanBySlugQuery

> **useLazyGetPlanBySlugQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Plan`](#plan), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:215](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L215)

***

### useLazyGetSubscriptionStatisticsQuery

> **useLazyGetSubscriptionStatisticsQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`SubscriptionStatistics`](#subscriptionstatistics), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:214](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L214)

***

### useReactivateSubscriptionMutation

> **useReactivateSubscriptionMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`ReactivateSubscriptionRequest`](#reactivatesubscriptionrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Subscription"` \| `"Plan"` \| `"Invoice"` \| `"SubscriptionStatistics"`, [`Subscription`](#subscription), `"subscriptionApi"`, `unknown`\>\>

Defined in: [src/store/api/subscriptionApi.ts:206](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/subscriptionApi.ts#L206)
