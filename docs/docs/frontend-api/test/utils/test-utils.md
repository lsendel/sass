[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / test/utils/test-utils

# test/utils/test-utils

## Variables

### adminUserState

> `const` **adminUserState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:105](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L105)

***

### authenticatedState

> `const` **authenticatedState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:84](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L84)

***

### customMatchers

> `const` **customMatchers**: `object`

Defined in: [src/test/utils/test-utils.tsx:292](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L292)

#### Type Declaration

##### toBeVisible()

> **toBeVisible**: (`element`) => `object`

###### Parameters

###### element

`HTMLElement`

###### Returns

`object`

###### message()

> **message**: () => `string`

###### Returns

`string`

###### pass

> **pass**: `boolean`

##### toHaveLoadingState()

> **toHaveLoadingState**: (`element`) => `object`

###### Parameters

###### element

`HTMLElement`

###### Returns

`object`

###### message()

> **message**: () => `string`

###### Returns

`string`

###### pass

> **pass**: `boolean` = `!!hasLoadingIndicator`

##### toHaveValidationError()

> **toHaveValidationError**: (`form`, `fieldName`, `errorMessage?`) => `object`

###### Parameters

###### form

`HTMLElement`

###### fieldName

`string`

###### errorMessage?

`string`

###### Returns

`object`

###### message()

> **message**: () => `string`

###### Returns

`string`

###### pass

> **pass**: `boolean` = `hasError`

***

### defaultMockState

> `const` **defaultMockState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:55](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L55)

***

### errorState

> `const` **errorState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:126](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L126)

***

### loadingState

> `const` **loadingState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:117](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L117)

***

### mockHooks

> `const` **mockHooks**: `object`

Defined in: [src/test/utils/test-utils.tsx:366](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L366)

#### Type Declaration

##### useLocation

> **useLocation**: `Mock`\<() => `object`\>

##### useNavigate

> **useNavigate**: `Mock`\<`Procedure`\>

##### useParams

> **useParams**: `Mock`\<() => `object`\>

##### useSearchParams

> **useSearchParams**: `Mock`\<() => (`Mock`\<`Procedure`\> \| `URLSearchParams`)[]\>

***

### testUtils

> `const` **testUtils**: `object`

Defined in: [src/test/utils/test-utils.tsx:196](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L196)

#### Type Declaration

##### generateTestId()

> **generateTestId**: (`prefix`) => `string`

###### Parameters

###### prefix

`string`

###### Returns

`string`

##### measureRenderTime()

> **measureRenderTime**: (`renderFn`) => `Promise`\<`number`\>

###### Parameters

###### renderFn

() => `void`

###### Returns

`Promise`\<`number`\>

##### mockApiError()

> **mockApiError**: (`code`, `message`, `status`) => `object`

###### Parameters

###### code

`string`

###### message

`string`

###### status

`number` = `400`

###### Returns

`object`

###### error

> **error**: `object`

###### error.code

> **code**: `string`

###### error.message

> **message**: `string`

###### success

> **success**: `boolean` = `false`

###### timestamp

> **timestamp**: `string`

##### mockApiSuccess()

> **mockApiSuccess**: (`data`) => `object`

###### Parameters

###### data

`any`

###### Returns

`object`

###### data

> **data**: `any`

###### success

> **success**: `boolean` = `true`

###### timestamp

> **timestamp**: `string`

##### checkA11y()

> **checkA11y**(): `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### closeModal()

> **closeModal**(`user`): `Promise`\<`void`\>

###### Parameters

###### user

`UserEvent`

###### Returns

`Promise`\<`void`\>

##### fillForm()

> **fillForm**(`user`, `formData`): `Promise`\<`void`\>

###### Parameters

###### user

`UserEvent`

###### formData

`Record`\<`string`, `string`\>

###### Returns

`Promise`\<`void`\>

##### navigateTo()

> **navigateTo**(`user`, `linkText`): `Promise`\<`void`\>

###### Parameters

###### user

`UserEvent`

###### linkText

`string`

###### Returns

`Promise`\<`void`\>

##### openModal()

> **openModal**(`user`, `triggerText`): `Promise`\<`void`\>

###### Parameters

###### user

`UserEvent`

###### triggerText

`string`

###### Returns

`Promise`\<`void`\>

##### submitForm()

> **submitForm**(`user`, `buttonText`): `Promise`\<`void`\>

###### Parameters

###### user

`UserEvent`

###### buttonText

`string` = `'submit'`

###### Returns

`Promise`\<`void`\>

##### waitForError()

> **waitForError**(`errorText?`): `Promise`\<`void`\>

###### Parameters

###### errorText?

`string`

###### Returns

`Promise`\<`void`\>

##### waitForLoadingToComplete()

> **waitForLoadingToComplete**(): `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

***

### unauthenticatedState

> `const` **unauthenticatedState**: `Partial`\<[`RootState`](../../store.md#rootstate)\>

Defined in: [src/test/utils/test-utils.tsx:93](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L93)

## Functions

### cleanupTests()

> **cleanupTests**(): `void`

Defined in: [src/test/utils/test-utils.tsx:374](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L374)

#### Returns

`void`

***

### createMockOrganization()

> **createMockOrganization**(`overrides`): `object`

Defined in: [src/test/utils/test-utils.tsx:353](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L353)

#### Parameters

##### overrides

#### Returns

`object`

##### createdAt

> **createdAt**: `string`

##### currentUsers

> **currentUsers**: `number` = `3`

##### id

> **id**: `string`

##### maxUsers

> **maxUsers**: `number` = `10`

##### name

> **name**: `string` = `'Test Organization'`

##### plan

> **plan**: `"PRO"`

##### status

> **status**: `"ACTIVE"`

##### updatedAt

> **updatedAt**: `string`

***

### createMockPayment()

> **createMockPayment**(`overrides`): `object`

Defined in: [src/test/utils/test-utils.tsx:339](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L339)

#### Parameters

##### overrides

#### Returns

`object`

##### amount

> **amount**: `number` = `1999`

##### createdAt

> **createdAt**: `string`

##### currency

> **currency**: `string` = `'USD'`

##### customerId

> **customerId**: `string` = `'cus-123'`

##### id

> **id**: `string`

##### organizationId

> **organizationId**: `string` = `'org-123'`

##### paidAt

> **paidAt**: `string`

##### paymentMethodId

> **paymentMethodId**: `string` = `'pm-123'`

##### status

> **status**: `"COMPLETED"`

##### updatedAt

> **updatedAt**: `string`

***

### createMockStore()

> **createMockStore**(`options`): `EnhancedStore`\<`any`, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

Defined in: [src/test/utils/test-utils.tsx:25](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L25)

#### Parameters

##### options

`MockStoreOptions` = `{}`

#### Returns

`EnhancedStore`\<`any`, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

***

### createMockUser()

> **createMockUser**(`overrides`): `object`

Defined in: [src/test/utils/test-utils.tsx:327](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L327)

#### Parameters

##### overrides

#### Returns

`object`

##### createdAt

> **createdAt**: `string`

##### email

> **email**: `string` = `'test@example.com'`

##### emailVerified

> **emailVerified**: `boolean` = `true`

##### firstName

> **firstName**: `string` = `'Test'`

##### id

> **id**: `string`

##### lastName

> **lastName**: `string` = `'User'`

##### role

> **role**: `"USER"`

##### updatedAt

> **updatedAt**: `string`

***

### customRender()

> **customRender**(`ui`, `options`): `object`

Defined in: [src/test/utils/test-utils.tsx:144](https://github.com/lsendel/sass/blob/main/frontend/src/test/utils/test-utils.tsx#L144)

#### Parameters

##### ui

`ReactElement`

##### options

`CustomRenderOptions` = `{}`

#### Returns

`object`

##### store

> **store**: `EnhancedStore`\<`any`, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

##### user

> **user**: `UserEvent`
