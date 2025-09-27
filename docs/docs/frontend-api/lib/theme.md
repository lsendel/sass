[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / lib/theme

# lib/theme

## Variables

### ACCENT\_COLOR

> `const` **ACCENT\_COLOR**: `"#06b6d4"`

Defined in: [src/lib/theme.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L18)

***

### COLOR

> `const` **COLOR**: `"#2563eb"`

Defined in: [src/lib/theme.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L15)

***

### STYLE

> `const` **STYLE**: `"modern"`

Defined in: [src/lib/theme.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L12)

Global Theme Configuration

This file defines the core visual identity for the application.
All components should use these tokens for consistent styling.

Usage:
```ts
import { STYLE, COLOR, ACCENT_COLOR } from '@/lib/theme'
```

***

### styleVariants

> `const` **styleVariants**: `object`

Defined in: [src/lib/theme.ts:137](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L137)

#### Type Declaration

##### button

> `readonly` **button**: `object`

###### button.accent

> `readonly` **accent**: `"bg-[#06b6d4] hover:bg-[#06b6d4]/90 text-white"`

###### button.outline

> `readonly` **outline**: `"border border-[#2563eb] text-[#2563eb] hover:bg-[#2563eb] hover:text-white"`

###### button.primary

> `readonly` **primary**: `"bg-[#2563eb] hover:bg-[#2563eb]/90 text-white"`

###### button.secondary

> `readonly` **secondary**: `"bg-gray-100 hover:bg-gray-200 text-gray-900"` = `'bg-gray-100 hover:bg-gray-200 text-gray-900'`

##### card

> `readonly` **card**: `object`

###### card.elevated

> `readonly` **elevated**: `"bg-white border border-gray-200 shadow-lg"` = `'bg-white border border-gray-200 shadow-lg'`

###### card.primary

> `readonly` **primary**: `"bg-white border border-gray-200 shadow-sm"` = `'bg-white border border-gray-200 shadow-sm'`

###### card.subtle

> `readonly` **subtle**: `"bg-gray-50 border border-gray-100"` = `'bg-gray-50 border border-gray-100'`

##### text

> `readonly` **text**: `object`

###### text.accent

> `readonly` **accent**: `"text-[#06b6d4]"`

###### text.brand

> `readonly` **brand**: `"text-[#2563eb]"`

###### text.muted

> `readonly` **muted**: `"text-gray-500"` = `'text-gray-500'`

###### text.primary

> `readonly` **primary**: `"text-gray-900"` = `'text-gray-900'`

###### text.secondary

> `readonly` **secondary**: `"text-gray-600"` = `'text-gray-600'`

***

### tailwindTheme

> `const` **tailwindTheme**: `object`

Defined in: [src/lib/theme.ts:186](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L186)

#### Type Declaration

##### colors

> `readonly` **colors**: `object`

###### colors.accent

> `readonly` **accent**: `object`

###### colors.accent.100

> `readonly` **100**: `"#cffafe"` = `'#cffafe'`

###### colors.accent.200

> `readonly` **200**: `"#a5f3fc"` = `'#a5f3fc'`

###### colors.accent.300

> `readonly` **300**: `"#67e8f9"` = `'#67e8f9'`

###### colors.accent.400

> `readonly` **400**: `"#22d3ee"` = `'#22d3ee'`

###### colors.accent.50

> `readonly` **50**: `"#ecfeff"` = `'#ecfeff'`

###### colors.accent.500

> `readonly` **500**: `"#06b6d4"` = `ACCENT_COLOR`

###### colors.accent.600

> `readonly` **600**: `"#0891b2"` = `'#0891b2'`

###### colors.accent.700

> `readonly` **700**: `"#0e7490"` = `'#0e7490'`

###### colors.accent.800

> `readonly` **800**: `"#155e75"` = `'#155e75'`

###### colors.accent.900

> `readonly` **900**: `"#164e63"` = `'#164e63'`

###### colors.accent.DEFAULT

> `readonly` **DEFAULT**: `"#06b6d4"` = `ACCENT_COLOR`

###### colors.primary

> `readonly` **primary**: `object`

###### colors.primary.100

> `readonly` **100**: `"#dbeafe"` = `'#dbeafe'`

###### colors.primary.200

> `readonly` **200**: `"#bfdbfe"` = `'#bfdbfe'`

###### colors.primary.300

> `readonly` **300**: `"#93c5fd"` = `'#93c5fd'`

###### colors.primary.400

> `readonly` **400**: `"#60a5fa"` = `'#60a5fa'`

###### colors.primary.50

> `readonly` **50**: `"#eff6ff"` = `'#eff6ff'`

###### colors.primary.500

> `readonly` **500**: `"#3b82f6"` = `'#3b82f6'`

###### colors.primary.600

> `readonly` **600**: `"#2563eb"` = `COLOR`

###### colors.primary.700

> `readonly` **700**: `"#1d4ed8"` = `'#1d4ed8'`

###### colors.primary.800

> `readonly` **800**: `"#1e40af"` = `'#1e40af'`

###### colors.primary.900

> `readonly` **900**: `"#1e3a8a"` = `'#1e3a8a'`

###### colors.primary.DEFAULT

> `readonly` **DEFAULT**: `"#2563eb"` = `COLOR`

***

### theme

> `const` **theme**: `object`

Defined in: [src/lib/theme.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L21)

#### Type Declaration

##### animation

> `readonly` **animation**: `object`

###### animation.duration

> `readonly` **duration**: `object`

###### animation.duration.base

> `readonly` **base**: `"300ms"` = `'300ms'`

###### animation.duration.fast

> `readonly` **fast**: `"150ms"` = `'150ms'`

###### animation.duration.slow

> `readonly` **slow**: `"500ms"` = `'500ms'`

###### animation.easing

> `readonly` **easing**: `object`

###### animation.easing.ease

> `readonly` **ease**: `"ease"` = `'ease'`

###### animation.easing.easeIn

> `readonly` **easeIn**: `"ease-in"` = `'ease-in'`

###### animation.easing.easeInOut

> `readonly` **easeInOut**: `"ease-in-out"` = `'ease-in-out'`

###### animation.easing.easeOut

> `readonly` **easeOut**: `"ease-out"` = `'ease-out'`

##### borderRadius

> `readonly` **borderRadius**: `object`

###### borderRadius.2xl

> `readonly` **2xl**: `"1.5rem"` = `'1.5rem'`

###### borderRadius.base

> `readonly` **base**: `"0.5rem"` = `'0.5rem'`

###### borderRadius.lg

> `readonly` **lg**: `"0.75rem"` = `'0.75rem'`

###### borderRadius.sm

> `readonly` **sm**: `"0.375rem"` = `'0.375rem'`

###### borderRadius.xl

> `readonly` **xl**: `"1rem"` = `'1rem'`

##### colors

> `readonly` **colors**: `object`

###### colors.accent

> `readonly` **accent**: `"#06b6d4"` = `ACCENT_COLOR`

###### colors.background

> `readonly` **background**: `object`

###### colors.background.primary

> `readonly` **primary**: `"#ffffff"` = `'#ffffff'`

###### colors.background.secondary

> `readonly` **secondary**: `"#f9fafb"` = `'#f9fafb'`

###### colors.background.subtle

> `readonly` **subtle**: `"#f3f4f6"` = `'#f3f4f6'`

###### colors.error

> `readonly` **error**: `"#dc2626"` = `'#dc2626'`

###### colors.gray

> `readonly` **gray**: `object`

###### colors.gray.100

> `readonly` **100**: `"#f3f4f6"` = `'#f3f4f6'`

###### colors.gray.200

> `readonly` **200**: `"#e5e7eb"` = `'#e5e7eb'`

###### colors.gray.300

> `readonly` **300**: `"#d1d5db"` = `'#d1d5db'`

###### colors.gray.400

> `readonly` **400**: `"#9ca3af"` = `'#9ca3af'`

###### colors.gray.50

> `readonly` **50**: `"#f9fafb"` = `'#f9fafb'`

###### colors.gray.500

> `readonly` **500**: `"#6b7280"` = `'#6b7280'`

###### colors.gray.600

> `readonly` **600**: `"#4b5563"` = `'#4b5563'`

###### colors.gray.700

> `readonly` **700**: `"#374151"` = `'#374151'`

###### colors.gray.800

> `readonly` **800**: `"#1f2937"` = `'#1f2937'`

###### colors.gray.900

> `readonly` **900**: `"#111827"` = `'#111827'`

###### colors.primary

> `readonly` **primary**: `"#2563eb"` = `COLOR`

###### colors.success

> `readonly` **success**: `"#16a34a"` = `'#16a34a'`

###### colors.warning

> `readonly` **warning**: `"#d97706"` = `'#d97706'`

##### iconSize

> `readonly` **iconSize**: `object`

###### iconSize.base

> `readonly` **base**: `"w-5 h-5"` = `'w-5 h-5'`

###### iconSize.lg

> `readonly` **lg**: `"w-6 h-6"` = `'w-6 h-6'`

###### iconSize.sm

> `readonly` **sm**: `"w-4 h-4"` = `'w-4 h-4'`

###### iconSize.xl

> `readonly` **xl**: `"w-8 h-8"` = `'w-8 h-8'`

###### iconSize.xs

> `readonly` **xs**: `"w-3 h-3"` = `'w-3 h-3'`

##### shadow

> `readonly` **shadow**: `object`

###### shadow.base

> `readonly` **base**: `"0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)"` = `'0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)'`

###### shadow.lg

> `readonly` **lg**: `"0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)"` = `'0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)'`

###### shadow.sm

> `readonly` **sm**: `"0 1px 2px 0 rgb(0 0 0 / 0.05)"` = `'0 1px 2px 0 rgb(0 0 0 / 0.05)'`

###### shadow.xl

> `readonly` **xl**: `"0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)"` = `'0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)'`

##### spacing

> `readonly` **spacing**: `object`

###### spacing.2xl

> `readonly` **2xl**: `"3rem"` = `'3rem'`

###### spacing.3xl

> `readonly` **3xl**: `"4rem"` = `'4rem'`

###### spacing.base

> `readonly` **base**: `"1rem"` = `'1rem'`

###### spacing.lg

> `readonly` **lg**: `"1.5rem"` = `'1.5rem'`

###### spacing.sm

> `readonly` **sm**: `"0.75rem"` = `'0.75rem'`

###### spacing.xl

> `readonly` **xl**: `"2rem"` = `'2rem'`

###### spacing.xs

> `readonly` **xs**: `"0.5rem"` = `'0.5rem'`

##### typography

> `readonly` **typography**: `object`

###### typography.fontSize

> `readonly` **fontSize**: `object`

###### typography.fontSize.2xl

> `readonly` **2xl**: `"1.5rem"` = `'1.5rem'`

###### typography.fontSize.3xl

> `readonly` **3xl**: `"1.875rem"` = `'1.875rem'`

###### typography.fontSize.4xl

> `readonly` **4xl**: `"2.25rem"` = `'2.25rem'`

###### typography.fontSize.base

> `readonly` **base**: `"1rem"` = `'1rem'`

###### typography.fontSize.lg

> `readonly` **lg**: `"1.125rem"` = `'1.125rem'`

###### typography.fontSize.sm

> `readonly` **sm**: `"0.875rem"` = `'0.875rem'`

###### typography.fontSize.xl

> `readonly` **xl**: `"1.25rem"` = `'1.25rem'`

###### typography.fontSize.xs

> `readonly` **xs**: `"0.75rem"` = `'0.75rem'`

###### typography.fontWeight

> `readonly` **fontWeight**: `object`

###### typography.fontWeight.bold

> `readonly` **bold**: `"700"` = `'700'`

###### typography.fontWeight.medium

> `readonly` **medium**: `"500"` = `'500'`

###### typography.fontWeight.normal

> `readonly` **normal**: `"400"` = `'400'`

###### typography.fontWeight.semibold

> `readonly` **semibold**: `"600"` = `'600'`

###### typography.lineHeight

> `readonly` **lineHeight**: `object`

###### typography.lineHeight.normal

> `readonly` **normal**: `"1.5"` = `'1.5'`

###### typography.lineHeight.relaxed

> `readonly` **relaxed**: `"1.75"` = `'1.75'`

###### typography.lineHeight.tight

> `readonly` **tight**: `"1.25"` = `'1.25'`

## Functions

### getButtonClasses()

> **getButtonClasses**(`variant`, `size`): `string`

Defined in: [src/lib/theme.ts:164](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L164)

#### Parameters

##### variant

`"accent"` | `"outline"` | `"secondary"` | `"primary"`

##### size

`"sm"` | `"lg"` | `"base"`

#### Returns

`string`

***

### getCardClasses()

> **getCardClasses**(`variant`): `string`

Defined in: [src/lib/theme.ts:176](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L176)

#### Parameters

##### variant

`"primary"` | `"elevated"` | `"subtle"`

#### Returns

`string`

***

### getIconClasses()

> **getIconClasses**(`size`): `"w-3 h-3"` \| `"w-4 h-4"` \| `"w-5 h-5"` \| `"w-6 h-6"` \| `"w-8 h-8"`

Defined in: [src/lib/theme.ts:181](https://github.com/lsendel/sass/blob/main/frontend/src/lib/theme.ts#L181)

#### Parameters

##### size

`"sm"` | `"lg"` | `"base"` | `"xs"` | `"xl"`

#### Returns

`"w-3 h-3"` \| `"w-4 h-4"` \| `"w-5 h-5"` \| `"w-6 h-6"` \| `"w-8 h-8"`
