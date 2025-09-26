# Variable: testUtils

> `const` **testUtils**: `object`

Defined in: [frontend/src/test/utils/test-utils.tsx:197](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/test/utils/test-utils.tsx#L197)

## Type Declaration

### generateTestId()

> **generateTestId**: (`prefix`) => `string`

#### Parameters

##### prefix

`string`

#### Returns

`string`

### measureRenderTime()

> **measureRenderTime**: (`renderFn`) => `Promise`\<`number`\>

#### Parameters

##### renderFn

() => `void`

#### Returns

`Promise`\<`number`\>

### mockApiError()

> **mockApiError**: (`code`, `message`, `status`) => `object`

#### Parameters

##### code

`string`

##### message

`string`

##### status

`number` = `400`

#### Returns

`object`

##### error

> **error**: `object`

###### error.code

> **code**: `string`

###### error.message

> **message**: `string`

##### success

> **success**: `boolean` = `false`

##### timestamp

> **timestamp**: `string`

### mockApiSuccess()

> **mockApiSuccess**: (`data`) => `object`

#### Parameters

##### data

`any`

#### Returns

`object`

##### data

> **data**: `any`

##### success

> **success**: `boolean` = `true`

##### timestamp

> **timestamp**: `string`

### checkA11y()

> **checkA11y**(): `Promise`\<`void`\>

#### Returns

`Promise`\<`void`\>

### closeModal()

> **closeModal**(`user`): `Promise`\<`void`\>

#### Parameters

##### user

`UserEvent`

#### Returns

`Promise`\<`void`\>

### fillForm()

> **fillForm**(`user`, `formData`): `Promise`\<`void`\>

#### Parameters

##### user

`UserEvent`

##### formData

`Record`\<`string`, `string`\>

#### Returns

`Promise`\<`void`\>

### navigateTo()

> **navigateTo**(`user`, `linkText`): `Promise`\<`void`\>

#### Parameters

##### user

`UserEvent`

##### linkText

`string`

#### Returns

`Promise`\<`void`\>

### openModal()

> **openModal**(`user`, `triggerText`): `Promise`\<`void`\>

#### Parameters

##### user

`UserEvent`

##### triggerText

`string`

#### Returns

`Promise`\<`void`\>

### submitForm()

> **submitForm**(`user`, `buttonText`): `Promise`\<`void`\>

#### Parameters

##### user

`UserEvent`

##### buttonText

`string` = `'submit'`

#### Returns

`Promise`\<`void`\>

### waitForError()

> **waitForError**(`errorText?`): `Promise`\<`void`\>

#### Parameters

##### errorText?

`string`

#### Returns

`Promise`\<`void`\>

### waitForLoadingToComplete()

> **waitForLoadingToComplete**(): `Promise`\<`void`\>

#### Returns

`Promise`\<`void`\>
