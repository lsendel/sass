[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / types/auth

# types/auth

## Interfaces

### OAuth2ApiErrorResponse

Defined in: [src/types/auth.ts:189](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L189)

#### Properties

##### error

> **error**: `object`

Defined in: [src/types/auth.ts:190](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L190)

###### code

> **code**: `string`

###### details?

> `optional` **details**: `Record`\<`string`, `unknown`\>

###### message

> **message**: `string`

###### timestamp

> **timestamp**: `string`

***

### OAuth2AuthorizeRequest

Defined in: [src/types/auth.ts:52](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L52)

#### Properties

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:53](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L53)

##### redirectUri?

> `optional` **redirectUri**: `string`

Defined in: [src/types/auth.ts:54](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L54)

##### state?

> `optional` **state**: `string`

Defined in: [src/types/auth.ts:55](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L55)

***

### OAuth2AuthorizeResponse

Defined in: [src/types/auth.ts:59](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L59)

#### Properties

##### authorizationUrl

> **authorizationUrl**: `string`

Defined in: [src/types/auth.ts:60](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L60)

##### codeChallenge

> **codeChallenge**: `string`

Defined in: [src/types/auth.ts:62](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L62)

##### codeChallengeMethod

> **codeChallengeMethod**: `"S256"`

Defined in: [src/types/auth.ts:63](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L63)

##### state

> **state**: `string`

Defined in: [src/types/auth.ts:61](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L61)

***

### OAuth2AuthStatus

Defined in: [src/types/auth.ts:114](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L114)

#### Properties

##### error

> **error**: `null` \| [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:118](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L118)

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/types/auth.ts:116](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L116)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/types/auth.ts:115](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L115)

##### lastChecked

> **lastChecked**: `null` \| `string`

Defined in: [src/types/auth.ts:119](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L119)

##### session

> **session**: `null` \| [`OAuth2Session`](#oauth2session)

Defined in: [src/types/auth.ts:117](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L117)

***

### OAuth2CallbackHandlerProps

Defined in: [src/types/auth.ts:167](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L167)

#### Properties

##### fallbackRedirect?

> `optional` **fallbackRedirect**: `string`

Defined in: [src/types/auth.ts:170](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L170)

##### onError()?

> `optional` **onError**: (`error`) => `void`

Defined in: [src/types/auth.ts:169](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L169)

###### Parameters

###### error

[`OAuth2Error`](#oauth2error)

###### Returns

`void`

##### onSuccess()?

> `optional` **onSuccess**: (`session`) => `void`

Defined in: [src/types/auth.ts:168](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L168)

###### Parameters

###### session

[`OAuth2Session`](#oauth2session)

###### Returns

`void`

***

### OAuth2CallbackParams

Defined in: [src/types/auth.ts:67](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L67)

#### Properties

##### code

> **code**: `string`

Defined in: [src/types/auth.ts:69](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L69)

##### error?

> `optional` **error**: `string`

Defined in: [src/types/auth.ts:71](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L71)

##### errorDescription?

> `optional` **errorDescription**: `string`

Defined in: [src/types/auth.ts:72](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L72)

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:68](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L68)

##### state

> **state**: `string`

Defined in: [src/types/auth.ts:70](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L70)

***

### OAuth2CallbackResponse

Defined in: [src/types/auth.ts:76](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L76)

#### Properties

##### error?

> `optional` **error**: [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:80](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L80)

##### redirectTo?

> `optional` **redirectTo**: `string`

Defined in: [src/types/auth.ts:79](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L79)

##### session?

> `optional` **session**: [`OAuth2Session`](#oauth2session)

Defined in: [src/types/auth.ts:78](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L78)

##### success

> **success**: `boolean`

Defined in: [src/types/auth.ts:77](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L77)

***

### OAuth2Config

Defined in: [src/types/auth.ts:174](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L174)

#### Properties

##### baseUrl

> **baseUrl**: `string`

Defined in: [src/types/auth.ts:175](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L175)

##### defaultRedirectUri

> **defaultRedirectUri**: `string`

Defined in: [src/types/auth.ts:183](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L183)

##### endpoints

> **endpoints**: `object`

Defined in: [src/types/auth.ts:176](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L176)

###### authorize

> **authorize**: `string`

###### callback

> **callback**: `string`

###### logout

> **logout**: `string`

###### providers

> **providers**: `string`

###### session

> **session**: `string`

##### flowTimeout

> **flowTimeout**: `number`

Defined in: [src/types/auth.ts:185](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L185)

##### sessionCheckInterval

> **sessionCheckInterval**: `number`

Defined in: [src/types/auth.ts:184](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L184)

***

### OAuth2ContextValue

Defined in: [src/types/auth.ts:147](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L147)

#### Extends

- [`UseOAuth2Return`](#useoauth2return)

#### Properties

##### clearError()

> **clearError**: () => `void`

Defined in: [src/types/auth.ts:139](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L139)

###### Returns

`void`

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`clearError`](#clearerror-1)

##### currentProvider

> **currentProvider**: `null` \| [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:143](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L143)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`currentProvider`](#currentprovider-1)

##### error

> **error**: `null` \| [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:128](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L128)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`error`](#error-5)

##### handleCallback()

> **handleCallback**: (`params`) => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:150](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L150)

###### Parameters

###### params

[`OAuth2CallbackParams`](#oauth2callbackparams)

###### Returns

`Promise`\<`void`\>

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/types/auth.ts:126](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L126)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`isAuthenticated`](#isauthenticated-4)

##### isInProgress

> **isInProgress**: `boolean`

Defined in: [src/types/auth.ts:142](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L142)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`isInProgress`](#isinprogress-1)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/types/auth.ts:125](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L125)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`isLoading`](#isloading-2)

##### login()

> **login**: (`provider`, `redirectUri?`) => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:136](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L136)

###### Parameters

###### provider

[`OAuth2ProviderName`](#oauth2providername-1)

###### redirectUri?

`string`

###### Returns

`Promise`\<`void`\>

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`login`](#login-1)

##### logout()

> **logout**: (`terminateProviderSession?`) => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:137](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L137)

###### Parameters

###### terminateProviderSession?

`boolean`

###### Returns

`Promise`\<`void`\>

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`logout`](#logout-1)

##### providers

> **providers**: `null` \| [`OAuth2Provider`](#oauth2provider)[]

Defined in: [src/types/auth.ts:131](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L131)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`providers`](#providers-2)

##### providersError

> **providersError**: `null` \| [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:133](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L133)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`providersError`](#providerserror-1)

##### providersLoading

> **providersLoading**: `boolean`

Defined in: [src/types/auth.ts:132](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L132)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`providersLoading`](#providersloading-1)

##### refreshSession()

> **refreshSession**: () => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:138](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L138)

###### Returns

`Promise`\<`void`\>

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`refreshSession`](#refreshsession-1)

##### session

> **session**: `null` \| [`OAuth2Session`](#oauth2session)

Defined in: [src/types/auth.ts:127](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L127)

###### Inherited from

[`UseOAuth2Return`](#useoauth2return).[`session`](#session-4)

##### setAuthenticationState()

> **setAuthenticationState**: (`state`) => `void`

Defined in: [src/types/auth.ts:149](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L149)

###### Parameters

###### state

`Partial`\<[`OAuth2AuthStatus`](#oauth2authstatus)\>

###### Returns

`void`

***

### OAuth2Error

Defined in: [src/types/auth.ts:97](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L97)

#### Properties

##### code

> **code**: `string`

Defined in: [src/types/auth.ts:98](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L98)

##### details?

> `optional` **details**: `Record`\<`string`, `unknown`\>

Defined in: [src/types/auth.ts:100](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L100)

##### message

> **message**: `string`

Defined in: [src/types/auth.ts:99](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L99)

##### timestamp

> **timestamp**: `string`

Defined in: [src/types/auth.ts:101](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L101)

***

### OAuth2FlowState

Defined in: [src/types/auth.ts:105](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L105)

#### Properties

##### codeVerifier

> **codeVerifier**: `string`

Defined in: [src/types/auth.ts:108](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L108)

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:106](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L106)

##### redirectUri

> **redirectUri**: `string`

Defined in: [src/types/auth.ts:109](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L109)

##### startedAt

> **startedAt**: `string`

Defined in: [src/types/auth.ts:110](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L110)

##### state

> **state**: `string`

Defined in: [src/types/auth.ts:107](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L107)

***

### OAuth2LoginButtonProps

Defined in: [src/types/auth.ts:154](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L154)

#### Properties

##### children?

> `optional` **children**: `ReactNode`

Defined in: [src/types/auth.ts:160](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L160)

##### className?

> `optional` **className**: `string`

Defined in: [src/types/auth.ts:159](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L159)

##### disabled?

> `optional` **disabled**: `boolean`

Defined in: [src/types/auth.ts:157](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L157)

##### loading?

> `optional` **loading**: `boolean`

Defined in: [src/types/auth.ts:158](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L158)

##### onLoginError()?

> `optional` **onLoginError**: (`error`) => `void`

Defined in: [src/types/auth.ts:163](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L163)

###### Parameters

###### error

[`OAuth2Error`](#oauth2error)

###### Returns

`void`

##### onLoginStart()?

> `optional` **onLoginStart**: () => `void`

Defined in: [src/types/auth.ts:161](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L161)

###### Returns

`void`

##### onLoginSuccess()?

> `optional` **onLoginSuccess**: (`session`) => `void`

Defined in: [src/types/auth.ts:162](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L162)

###### Parameters

###### session

[`OAuth2Session`](#oauth2session)

###### Returns

`void`

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:155](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L155)

##### redirectUri?

> `optional` **redirectUri**: `string`

Defined in: [src/types/auth.ts:156](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L156)

***

### OAuth2LogoutRequest

Defined in: [src/types/auth.ts:84](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L84)

#### Properties

##### redirectUri?

> `optional` **redirectUri**: `string`

Defined in: [src/types/auth.ts:86](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L86)

##### terminateProviderSession?

> `optional` **terminateProviderSession**: `boolean`

Defined in: [src/types/auth.ts:85](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L85)

***

### OAuth2LogoutResponse

Defined in: [src/types/auth.ts:90](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L90)

#### Properties

##### message?

> `optional` **message**: `string`

Defined in: [src/types/auth.ts:93](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L93)

##### redirectTo?

> `optional` **redirectTo**: `string`

Defined in: [src/types/auth.ts:92](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L92)

##### success

> **success**: `boolean`

Defined in: [src/types/auth.ts:91](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L91)

***

### OAuth2Provider

Defined in: [src/types/auth.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L8)

#### Properties

##### authorizationUrl

> **authorizationUrl**: `string`

Defined in: [src/types/auth.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L11)

##### displayName

> **displayName**: `string`

Defined in: [src/types/auth.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L10)

##### name

> **name**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L9)

##### scopes

> **scopes**: `string`[]

Defined in: [src/types/auth.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L12)

***

### OAuth2ProvidersResponse

Defined in: [src/types/auth.ts:16](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L16)

#### Properties

##### providers

> **providers**: [`OAuth2Provider`](#oauth2provider)[]

Defined in: [src/types/auth.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L17)

***

### OAuth2Session

Defined in: [src/types/auth.ts:34](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L34)

#### Properties

##### createdAt

> **createdAt**: `string`

Defined in: [src/types/auth.ts:41](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L41)

##### expiresAt

> **expiresAt**: `string`

Defined in: [src/types/auth.ts:40](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L40)

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/types/auth.ts:39](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L39)

##### lastAccessedAt

> **lastAccessedAt**: `string`

Defined in: [src/types/auth.ts:42](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L42)

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:38](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L38)

##### sessionId

> **sessionId**: `string`

Defined in: [src/types/auth.ts:35](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L35)

##### userId

> **userId**: `string`

Defined in: [src/types/auth.ts:36](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L36)

##### userInfo

> **userInfo**: [`OAuth2UserInfo`](#oauth2userinfo)

Defined in: [src/types/auth.ts:37](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L37)

***

### OAuth2SessionResponse

Defined in: [src/types/auth.ts:46](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L46)

#### Properties

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/types/auth.ts:48](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L48)

##### session

> **session**: `null` \| [`OAuth2Session`](#oauth2session)

Defined in: [src/types/auth.ts:47](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L47)

***

### OAuth2UserInfo

Defined in: [src/types/auth.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L21)

#### Properties

##### email

> **email**: `string`

Defined in: [src/types/auth.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L23)

##### emailVerified?

> `optional` **emailVerified**: `boolean`

Defined in: [src/types/auth.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L24)

##### familyName?

> `optional` **familyName**: `string`

Defined in: [src/types/auth.ts:27](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L27)

##### givenName?

> `optional` **givenName**: `string`

Defined in: [src/types/auth.ts:26](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L26)

##### locale?

> `optional` **locale**: `string`

Defined in: [src/types/auth.ts:29](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L29)

##### name

> **name**: `string`

Defined in: [src/types/auth.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L25)

##### picture?

> `optional` **picture**: `string`

Defined in: [src/types/auth.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L28)

##### provider

> **provider**: [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L30)

##### sub

> **sub**: `string`

Defined in: [src/types/auth.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L22)

***

### UseOAuth2Return

Defined in: [src/types/auth.ts:123](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L123)

#### Extended by

- [`OAuth2ContextValue`](#oauth2contextvalue)

#### Properties

##### clearError()

> **clearError**: () => `void`

Defined in: [src/types/auth.ts:139](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L139)

###### Returns

`void`

##### currentProvider

> **currentProvider**: `null` \| [`OAuth2ProviderName`](#oauth2providername-1)

Defined in: [src/types/auth.ts:143](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L143)

##### error

> **error**: `null` \| [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:128](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L128)

##### isAuthenticated

> **isAuthenticated**: `boolean`

Defined in: [src/types/auth.ts:126](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L126)

##### isInProgress

> **isInProgress**: `boolean`

Defined in: [src/types/auth.ts:142](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L142)

##### isLoading

> **isLoading**: `boolean`

Defined in: [src/types/auth.ts:125](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L125)

##### login()

> **login**: (`provider`, `redirectUri?`) => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:136](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L136)

###### Parameters

###### provider

[`OAuth2ProviderName`](#oauth2providername-1)

###### redirectUri?

`string`

###### Returns

`Promise`\<`void`\>

##### logout()

> **logout**: (`terminateProviderSession?`) => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:137](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L137)

###### Parameters

###### terminateProviderSession?

`boolean`

###### Returns

`Promise`\<`void`\>

##### providers

> **providers**: `null` \| [`OAuth2Provider`](#oauth2provider)[]

Defined in: [src/types/auth.ts:131](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L131)

##### providersError

> **providersError**: `null` \| [`OAuth2Error`](#oauth2error)

Defined in: [src/types/auth.ts:133](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L133)

##### providersLoading

> **providersLoading**: `boolean`

Defined in: [src/types/auth.ts:132](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L132)

##### refreshSession()

> **refreshSession**: () => `Promise`\<`void`\>

Defined in: [src/types/auth.ts:138](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L138)

###### Returns

`Promise`\<`void`\>

##### session

> **session**: `null` \| [`OAuth2Session`](#oauth2session)

Defined in: [src/types/auth.ts:127](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L127)

## Type Aliases

### OAuth2ProviderName

> **OAuth2ProviderName** = `"google"` \| `"github"` \| `"microsoft"`

Defined in: [src/types/auth.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L5)

## Variables

### OAUTH2\_PROVIDER\_DISPLAY\_NAMES

> `const` **OAUTH2\_PROVIDER\_DISPLAY\_NAMES**: `Record`\<[`OAuth2ProviderName`](#oauth2providername-1), `string`\>

Defined in: [src/types/auth.ts:234](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L234)

***

### OAUTH2\_PROVIDERS

> `const` **OAUTH2\_PROVIDERS**: [`OAuth2ProviderName`](#oauth2providername-1)[]

Defined in: [src/types/auth.ts:228](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L228)

***

### OAUTH2\_SCOPES

> `const` **OAUTH2\_SCOPES**: `Record`\<[`OAuth2ProviderName`](#oauth2providername-1), `string`[]\>

Defined in: [src/types/auth.ts:241](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L241)

***

### OAUTH2\_STORAGE\_KEYS

> `const` **OAUTH2\_STORAGE\_KEYS**: `object`

Defined in: [src/types/auth.ts:248](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L248)

#### Type Declaration

##### CODE\_VERIFIER

> `readonly` **CODE\_VERIFIER**: `"oauth2_code_verifier"` = `'oauth2_code_verifier'`

##### FLOW\_STATE

> `readonly` **FLOW\_STATE**: `"oauth2_flow_state"` = `'oauth2_flow_state'`

##### REDIRECT\_URI

> `readonly` **REDIRECT\_URI**: `"oauth2_redirect_uri"` = `'oauth2_redirect_uri'`

## Functions

### isOAuth2Error()

> **isOAuth2Error**(`value`): `value is OAuth2Error`

Defined in: [src/types/auth.ts:199](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L199)

#### Parameters

##### value

`unknown`

#### Returns

`value is OAuth2Error`

***

### isOAuth2Session()

> **isOAuth2Session**(`value`): `value is OAuth2Session`

Defined in: [src/types/auth.ts:209](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L209)

#### Parameters

##### value

`unknown`

#### Returns

`value is OAuth2Session`

***

### isValidOAuth2Provider()

> **isValidOAuth2Provider**(`provider`): `provider is OAuth2ProviderName`

Defined in: [src/types/auth.ts:221](https://github.com/lsendel/sass/blob/main/frontend/src/types/auth.ts#L221)

#### Parameters

##### provider

`string`

#### Returns

`provider is OAuth2ProviderName`
