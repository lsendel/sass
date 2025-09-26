# Variable: customMatchers

> `const` **customMatchers**: `object`

Defined in: [frontend/src/test/utils/test-utils.tsx:293](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/test/utils/test-utils.tsx#L293)

## Type Declaration

### toBeVisible()

> **toBeVisible**: (`element`) => `object`

#### Parameters

##### element

`HTMLElement`

#### Returns

`object`

##### message()

> **message**: () => `string`

###### Returns

`string`

##### pass

> **pass**: `boolean`

### toHaveLoadingState()

> **toHaveLoadingState**: (`element`) => `object`

#### Parameters

##### element

`HTMLElement`

#### Returns

`object`

##### message()

> **message**: () => `string`

###### Returns

`string`

##### pass

> **pass**: `boolean` = `!!hasLoadingIndicator`

### toHaveValidationError()

> **toHaveValidationError**: (`form`, `fieldName`, `errorMessage?`) => `object`

#### Parameters

##### form

`HTMLElement`

##### fieldName

`string`

##### errorMessage?

`string`

#### Returns

`object`

##### message()

> **message**: () => `string`

###### Returns

`string`

##### pass

> **pass**: `boolean` = `hasError`
