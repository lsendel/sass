# Interface: OAuth2CallbackHandlerProps

Defined in: [frontend/src/types/auth.ts:167](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L167)

## Properties

### fallbackRedirect?

> `optional` **fallbackRedirect**: `string`

Defined in: [frontend/src/types/auth.ts:170](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L170)

***

### onError()?

> `optional` **onError**: (`error`) => `void`

Defined in: [frontend/src/types/auth.ts:169](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L169)

#### Parameters

##### error

[`OAuth2Error`](OAuth2Error.md)

#### Returns

`void`

***

### onSuccess()?

> `optional` **onSuccess**: (`session`) => `void`

Defined in: [frontend/src/types/auth.ts:168](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L168)

#### Parameters

##### session

[`OAuth2Session`](OAuth2Session.md)

#### Returns

`void`
