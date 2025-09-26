# Interface: OAuth2ContextValue

Defined in: [frontend/src/types/auth.ts:147](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L147)

## Extends

- [`UseOAuth2Return`](UseOAuth2Return.md)

## Properties

### clearError()

> **clearError**: () => `void`

Defined in: [frontend/src/types/auth.ts:139](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L139)

#### Returns

`void`

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`clearError`](UseOAuth2Return.md#clearerror)

***

### currentProvider

> **currentProvider**: `null` \| [`OAuth2ProviderName`](../type-aliases/OAuth2ProviderName.md)

Defined in: [frontend/src/types/auth.ts:143](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L143)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`currentProvider`](UseOAuth2Return.md#currentprovider)

***

### error

> **error**: `null` \| [`OAuth2Error`](OAuth2Error.md)

Defined in: [frontend/src/types/auth.ts:128](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L128)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`error`](UseOAuth2Return.md#error)

***

### handleCallback()

> **handleCallback**: (`params`) => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:150](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L150)

#### Parameters

##### params

[`OAuth2CallbackParams`](OAuth2CallbackParams.md)

#### Returns

`Promise`\<`void`\>

***

### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [frontend/src/types/auth.ts:126](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L126)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`isAuthenticated`](UseOAuth2Return.md#isauthenticated)

***

### isInProgress

> **isInProgress**: `boolean`

Defined in: [frontend/src/types/auth.ts:142](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L142)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`isInProgress`](UseOAuth2Return.md#isinprogress)

***

### isLoading

> **isLoading**: `boolean`

Defined in: [frontend/src/types/auth.ts:125](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L125)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`isLoading`](UseOAuth2Return.md#isloading)

***

### login()

> **login**: (`provider`, `redirectUri?`) => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:136](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L136)

#### Parameters

##### provider

[`OAuth2ProviderName`](../type-aliases/OAuth2ProviderName.md)

##### redirectUri?

`string`

#### Returns

`Promise`\<`void`\>

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`login`](UseOAuth2Return.md#login)

***

### logout()

> **logout**: (`terminateProviderSession?`) => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:137](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L137)

#### Parameters

##### terminateProviderSession?

`boolean`

#### Returns

`Promise`\<`void`\>

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`logout`](UseOAuth2Return.md#logout)

***

### providers

> **providers**: `null` \| [`OAuth2Provider`](OAuth2Provider.md)[]

Defined in: [frontend/src/types/auth.ts:131](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L131)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`providers`](UseOAuth2Return.md#providers)

***

### providersError

> **providersError**: `null` \| [`OAuth2Error`](OAuth2Error.md)

Defined in: [frontend/src/types/auth.ts:133](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L133)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`providersError`](UseOAuth2Return.md#providerserror)

***

### providersLoading

> **providersLoading**: `boolean`

Defined in: [frontend/src/types/auth.ts:132](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L132)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`providersLoading`](UseOAuth2Return.md#providersloading)

***

### refreshSession()

> **refreshSession**: () => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:138](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L138)

#### Returns

`Promise`\<`void`\>

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`refreshSession`](UseOAuth2Return.md#refreshsession)

***

### session

> **session**: `null` \| [`OAuth2Session`](OAuth2Session.md)

Defined in: [frontend/src/types/auth.ts:127](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L127)

#### Inherited from

[`UseOAuth2Return`](UseOAuth2Return.md).[`session`](UseOAuth2Return.md#session)

***

### setAuthenticationState()

> **setAuthenticationState**: (`state`) => `void`

Defined in: [frontend/src/types/auth.ts:149](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L149)

#### Parameters

##### state

`Partial`\<[`OAuth2AuthStatus`](OAuth2AuthStatus.md)\>

#### Returns

`void`
