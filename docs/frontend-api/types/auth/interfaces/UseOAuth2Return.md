# Interface: UseOAuth2Return

Defined in: [frontend/src/types/auth.ts:123](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L123)

## Extended by

- [`OAuth2ContextValue`](OAuth2ContextValue.md)

## Properties

### clearError()

> **clearError**: () => `void`

Defined in: [frontend/src/types/auth.ts:139](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L139)

#### Returns

`void`

***

### currentProvider

> **currentProvider**: `null` \| [`OAuth2ProviderName`](../type-aliases/OAuth2ProviderName.md)

Defined in: [frontend/src/types/auth.ts:143](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L143)

***

### error

> **error**: `null` \| [`OAuth2Error`](OAuth2Error.md)

Defined in: [frontend/src/types/auth.ts:128](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L128)

***

### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [frontend/src/types/auth.ts:126](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L126)

***

### isInProgress

> **isInProgress**: `boolean`

Defined in: [frontend/src/types/auth.ts:142](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L142)

***

### isLoading

> **isLoading**: `boolean`

Defined in: [frontend/src/types/auth.ts:125](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L125)

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

***

### logout()

> **logout**: (`terminateProviderSession?`) => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:137](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L137)

#### Parameters

##### terminateProviderSession?

`boolean`

#### Returns

`Promise`\<`void`\>

***

### providers

> **providers**: `null` \| [`OAuth2Provider`](OAuth2Provider.md)[]

Defined in: [frontend/src/types/auth.ts:131](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L131)

***

### providersError

> **providersError**: `null` \| [`OAuth2Error`](OAuth2Error.md)

Defined in: [frontend/src/types/auth.ts:133](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L133)

***

### providersLoading

> **providersLoading**: `boolean`

Defined in: [frontend/src/types/auth.ts:132](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L132)

***

### refreshSession()

> **refreshSession**: () => `Promise`\<`void`\>

Defined in: [frontend/src/types/auth.ts:138](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L138)

#### Returns

`Promise`\<`void`\>

***

### session

> **session**: `null` \| [`OAuth2Session`](OAuth2Session.md)

Defined in: [frontend/src/types/auth.ts:127](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/types/auth.ts#L127)
