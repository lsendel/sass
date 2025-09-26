# Interface: OAuth2LoginButtonProps

Defined in: [frontend/src/types/auth.ts:154](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L154)

## Properties

### children?

> `optional` **children**: `ReactNode`

Defined in: [frontend/src/types/auth.ts:160](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L160)

***

### className?

> `optional` **className**: `string`

Defined in: [frontend/src/types/auth.ts:159](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L159)

***

### disabled?

> `optional` **disabled**: `boolean`

Defined in: [frontend/src/types/auth.ts:157](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L157)

***

### loading?

> `optional` **loading**: `boolean`

Defined in: [frontend/src/types/auth.ts:158](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L158)

***

### onLoginError()?

> `optional` **onLoginError**: (`error`) => `void`

Defined in: [frontend/src/types/auth.ts:163](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L163)

#### Parameters

##### error

[`OAuth2Error`](OAuth2Error.md)

#### Returns

`void`

***

### onLoginStart()?

> `optional` **onLoginStart**: () => `void`

Defined in: [frontend/src/types/auth.ts:161](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L161)

#### Returns

`void`

***

### onLoginSuccess()?

> `optional` **onLoginSuccess**: (`session`) => `void`

Defined in: [frontend/src/types/auth.ts:162](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L162)

#### Parameters

##### session

[`OAuth2Session`](OAuth2Session.md)

#### Returns

`void`

***

### provider

> **provider**: [`OAuth2ProviderName`](../type-aliases/OAuth2ProviderName.md)

Defined in: [frontend/src/types/auth.ts:155](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L155)

***

### redirectUri?

> `optional` **redirectUri**: `string`

Defined in: [frontend/src/types/auth.ts:156](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L156)
