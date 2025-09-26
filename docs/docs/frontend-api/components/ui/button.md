[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / components/ui/button

# components/ui/button

## Interfaces

### ButtonProps

Defined in: [src/components/ui/button.tsx:48](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L48)

#### Extends

- `ButtonHTMLAttributes`\<`HTMLButtonElement`\>.`VariantProps`\<*typeof* [`buttonVariants`](#buttonvariants)\>

#### Properties

##### asChild?

> `optional` **asChild**: `boolean`

Defined in: [src/components/ui/button.tsx:51](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L51)

##### size?

> `optional` **size**: `null` \| `"default"` \| `"sm"` \| `"lg"` \| `"icon"`

Defined in: [src/components/ui/button.tsx:34](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L34)

###### Inherited from

`VariantProps.size`

##### variant?

> `optional` **variant**: `null` \| `"default"` \| `"accent"` \| `"destructive"` \| `"outline"` \| `"secondary"` \| `"ghost"` \| `"link"`

Defined in: [src/components/ui/button.tsx:12](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L12)

###### Inherited from

`VariantProps.variant`

## Variables

### Button

> `const` **Button**: `ForwardRefExoticComponent`\<[`ButtonProps`](#buttonprops) & `RefAttributes`\<`HTMLButtonElement`\>\>

Defined in: [src/components/ui/button.tsx:54](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L54)

***

### buttonVariants()

> `const` **buttonVariants**: (`props?`) => `string`

Defined in: [src/components/ui/button.tsx:8](https://github.com/lsendel/sass/blob/main/frontend/src/components/ui/button.tsx#L8)

#### Parameters

##### props?

ConfigVariants\<\{ variant: \{ default: string; accent: string; destructive: string; outline: string; secondary: string; ghost: string; link: string; \}; size: \{ sm: string; default: string; lg: string; icon: string; \}; \}\> & ClassProp

#### Returns

`string`
