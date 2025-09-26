# Function: useKeyboardNavigation()

> **useKeyboardNavigation**\<`T`\>(`options`): `object`

Defined in: [frontend/src/hooks/useAccessibility.ts:49](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useAccessibility.ts#L49)

Hook for managing keyboard navigation in lists/grids

## Type Parameters

### T

`T` *extends* `HTMLElement` = `HTMLElement`

## Parameters

### options

#### direction?

`"grid"` \| `"horizontal"` \| `"vertical"`

#### disabled?

`boolean`

#### loop?

`boolean`

#### onSelect?

(`index`) => `void`

## Returns

`object`

### containerRef

> **containerRef**: `RefObject`\<`null` \| `T`\>

### focusedIndex

> **focusedIndex**: `number`

### setFocusedIndex

> **setFocusedIndex**: `Dispatch`\<`SetStateAction`\<`number`\>\>

### updateFocusableItems()

> **updateFocusableItems**: () => `void`

#### Returns

`void`
