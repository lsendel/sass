# Function: useAccessibilityContext()

> **useAccessibilityContext**(): `object`

Defined in: [frontend/src/hooks/useAccessibility.ts:377](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/hooks/useAccessibility.ts#L377)

Comprehensive accessibility provider for the entire app

## Returns

`object`

### announce

> **announce**: `any`

### components

> **components**: `object`

#### components.AnnouncementRegion

> **AnnouncementRegion**: `any`

#### components.SkipLink

> **SkipLink**: `any`

#### components.StatusRegion

> **StatusRegion**: `any`

### getContrastClass()

> **getContrastClass**: (`normalClass`, `highContrastClass`) => `string`

#### Parameters

##### normalClass

`string`

##### highContrastClass

`string`

#### Returns

`string`

### isHighContrast

> **isHighContrast**: `boolean`

### prefersReducedMotion

> **prefersReducedMotion**: `boolean`

### updateLiveRegion

> **updateLiveRegion**: `any`
