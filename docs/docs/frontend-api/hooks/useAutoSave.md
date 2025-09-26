[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useAutoSave

# hooks/useAutoSave

## Interfaces

### AutoSaveOptions

Defined in: [src/hooks/useAutoSave.ts:3](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L3)

#### Properties

##### delay?

> `optional` **delay**: `number`

Defined in: [src/hooks/useAutoSave.ts:4](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L4)

##### onError()?

> `optional` **onError**: (`error`) => `void`

Defined in: [src/hooks/useAutoSave.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L6)

###### Parameters

###### error

`Error`

###### Returns

`void`

##### onSave()

> **onSave**: (`data`) => `Promise`\<`void`\>

Defined in: [src/hooks/useAutoSave.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L5)

###### Parameters

###### data

`any`

###### Returns

`Promise`\<`void`\>

## Type Aliases

### AutoSaveStatus

> **AutoSaveStatus** = `"idle"` \| `"saving"` \| `"saved"` \| `"error"`

Defined in: [src/hooks/useAutoSave.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L9)

## Functions

### useAutoSave()

> **useAutoSave**(`data`, `options`): `object`

Defined in: [src/hooks/useAutoSave.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAutoSave.ts#L11)

#### Parameters

##### data

`any`

##### options

[`AutoSaveOptions`](#autosaveoptions)

#### Returns

`object`

##### error

> **error**: `null` \| `Error`

##### hasUnsavedChanges

> **hasUnsavedChanges**: `boolean`

##### lastSaved

> **lastSaved**: `null` \| `Date`

##### retry()

> **retry**: () => `void`

###### Returns

`void`

##### save()

> **save**: () => `Promise`\<`void`\>

###### Returns

`Promise`\<`void`\>

##### status

> **status**: [`AutoSaveStatus`](#autosavestatus)
