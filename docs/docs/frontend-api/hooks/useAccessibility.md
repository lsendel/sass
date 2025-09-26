[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useAccessibility

# hooks/useAccessibility

## Functions

### useAccessibilityContext()

> **useAccessibilityContext**(): `object`

Defined in: [src/hooks/useAccessibility.ts:377](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L377)

Comprehensive accessibility provider for the entire app

#### Returns

`object`

##### announce

> **announce**: `any`

##### components

> **components**: `object`

###### components.AnnouncementRegion

> **AnnouncementRegion**: `any`

###### components.SkipLink

> **SkipLink**: `any`

###### components.StatusRegion

> **StatusRegion**: `any`

##### getContrastClass()

> **getContrastClass**: (`normalClass`, `highContrastClass`) => `string`

###### Parameters

###### normalClass

`string`

###### highContrastClass

`string`

###### Returns

`string`

##### isHighContrast

> **isHighContrast**: `boolean`

##### prefersReducedMotion

> **prefersReducedMotion**: `boolean`

##### updateLiveRegion

> **updateLiveRegion**: `any`

***

### useAriaAttributes()

> **useAriaAttributes**(): `object`

Defined in: [src/hooks/useAccessibility.ts:277](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L277)

Hook for managing ARIA attributes dynamically

#### Returns

`object`

##### setAriaDescribedBy()

> **setAriaDescribedBy**: (`element`, `id`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### id

`string`

###### Returns

`void`

##### setAriaDisabled()

> **setAriaDisabled**: (`element`, `disabled`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### disabled

`boolean`

###### Returns

`void`

##### setAriaExpanded()

> **setAriaExpanded**: (`element`, `expanded`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### expanded

`boolean`

###### Returns

`void`

##### setAriaLabel()

> **setAriaLabel**: (`element`, `label`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### label

`string`

###### Returns

`void`

##### setAriaSelected()

> **setAriaSelected**: (`element`, `selected`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### selected

`boolean`

###### Returns

`void`

##### setRole()

> **setRole**: (`element`, `role`) => `void`

###### Parameters

###### element

`null` | `HTMLElement`

###### role

`string`

###### Returns

`void`

***

### useFocusTrap()

> **useFocusTrap**(`isActive`): `RefObject`\<`null` \| `HTMLElement`\>

Defined in: [src/hooks/useAccessibility.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L6)

Hook for managing focus trap within a container (useful for modals, dropdowns)

#### Parameters

##### isActive

`boolean` = `false`

#### Returns

`RefObject`\<`null` \| `HTMLElement`\>

***

### useHighContrast()

> **useHighContrast**(): `object`

Defined in: [src/hooks/useAccessibility.ts:251](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L251)

Hook for color contrast checking and high contrast mode

#### Returns

`object`

##### getContrastClass()

> **getContrastClass**: (`normalClass`, `highContrastClass`) => `string`

###### Parameters

###### normalClass

`string`

###### highContrastClass

`string`

###### Returns

`string`

##### isHighContrast

> **isHighContrast**: `boolean`

***

### useKeyboardNavigation()

> **useKeyboardNavigation**\<`T`\>(`options`): `object`

Defined in: [src/hooks/useAccessibility.ts:49](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L49)

Hook for managing keyboard navigation in lists/grids

#### Type Parameters

##### T

`T` *extends* `HTMLElement` = `HTMLElement`

#### Parameters

##### options

###### direction?

`"grid"` \| `"horizontal"` \| `"vertical"`

###### disabled?

`boolean`

###### loop?

`boolean`

###### onSelect?

(`index`) => `void`

#### Returns

`object`

##### containerRef

> **containerRef**: `RefObject`\<`null` \| `T`\>

##### focusedIndex

> **focusedIndex**: `number`

##### setFocusedIndex

> **setFocusedIndex**: `Dispatch`\<`SetStateAction`\<`number`\>\>

##### updateFocusableItems()

> **updateFocusableItems**: () => `void`

###### Returns

`void`

***

### useLiveRegion()

> **useLiveRegion**(`type`): `void`

Defined in: [src/hooks/useAccessibility.ts:327](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L327)

Hook for managing live regions for dynamic content updates

#### Parameters

##### type

`"alert"` | `"status"`

#### Returns

`void`

***

### useReducedMotion()

> **useReducedMotion**(): `boolean`

Defined in: [src/hooks/useAccessibility.ts:200](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L200)

Hook for managing reduced motion preferences

#### Returns

`boolean`

***

### useScreenReader()

> **useScreenReader**(): `void`

Defined in: [src/hooks/useAccessibility.ts:164](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L164)

Hook for managing screen reader announcements

#### Returns

`void`

***

### useSkipLinks()

> **useSkipLinks**(): `void`

Defined in: [src/hooks/useAccessibility.ts:221](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useAccessibility.ts#L221)

Hook for managing skip links

#### Returns

`void`
