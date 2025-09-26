[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/api/organizationApi

# store/api/organizationApi

## Type Aliases

### CreateOrganizationRequest

> **CreateOrganizationRequest** = `object`

Defined in: [src/store/api/organizationApi.ts:47](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L47)

#### Properties

##### name

> **name**: `string`

Defined in: [src/store/api/organizationApi.ts:48](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L48)

##### settings?

> `optional` **settings**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/organizationApi.ts:50](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L50)

##### slug

> **slug**: `string`

Defined in: [src/store/api/organizationApi.ts:49](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L49)

***

### Invitation

> **Invitation** = `object`

Defined in: [src/store/api/organizationApi.ts:35](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L35)

#### Properties

##### createdAt

> **createdAt**: `string`

Defined in: [src/store/api/organizationApi.ts:44](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L44)

##### email

> **email**: `string`

Defined in: [src/store/api/organizationApi.ts:39](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L39)

##### expiresAt

> **expiresAt**: `string`

Defined in: [src/store/api/organizationApi.ts:43](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L43)

##### id

> **id**: `string`

Defined in: [src/store/api/organizationApi.ts:36](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L36)

##### invitedBy

> **invitedBy**: `string`

Defined in: [src/store/api/organizationApi.ts:38](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L38)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/organizationApi.ts:37](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L37)

##### role

> **role**: [`OrganizationMemberRole`](#organizationmemberrole-1)

Defined in: [src/store/api/organizationApi.ts:40](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L40)

##### status

> **status**: `"PENDING"` \| `"ACCEPTED"` \| `"DECLINED"` \| `"EXPIRED"` \| `"REVOKED"`

Defined in: [src/store/api/organizationApi.ts:41](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L41)

##### token

> **token**: `string`

Defined in: [src/store/api/organizationApi.ts:42](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L42)

***

### InviteUserRequest

> **InviteUserRequest** = `object`

Defined in: [src/store/api/organizationApi.ts:62](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L62)

#### Properties

##### email

> **email**: `string`

Defined in: [src/store/api/organizationApi.ts:63](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L63)

##### role

> **role**: [`OrganizationMemberRole`](#organizationmemberrole-1)

Defined in: [src/store/api/organizationApi.ts:64](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L64)

***

### Organization

> **Organization** = `object`

Defined in: [src/store/api/organizationApi.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L7)

#### Properties

##### createdAt

> **createdAt**: `string`

Defined in: [src/store/api/organizationApi.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L13)

##### id

> **id**: `string`

Defined in: [src/store/api/organizationApi.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L8)

##### name

> **name**: `string`

Defined in: [src/store/api/organizationApi.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L9)

##### ownerId

> **ownerId**: `string`

Defined in: [src/store/api/organizationApi.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L11)

##### settings

> **settings**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/organizationApi.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L12)

##### slug

> **slug**: `string`

Defined in: [src/store/api/organizationApi.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L10)

##### updatedAt

> **updatedAt**: `string`

Defined in: [src/store/api/organizationApi.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L14)

***

### OrganizationMember

> **OrganizationMember** = `object`

Defined in: [src/store/api/organizationApi.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L19)

#### Properties

##### id

> **id**: `string`

Defined in: [src/store/api/organizationApi.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L20)

##### joinedAt

> **joinedAt**: `string`

Defined in: [src/store/api/organizationApi.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L24)

##### organizationId

> **organizationId**: `string`

Defined in: [src/store/api/organizationApi.ts:22](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L22)

##### role

> **role**: [`OrganizationMemberRole`](#organizationmemberrole-1)

Defined in: [src/store/api/organizationApi.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L23)

##### userId

> **userId**: `string`

Defined in: [src/store/api/organizationApi.ts:21](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L21)

***

### OrganizationMemberInfo

> **OrganizationMemberInfo** = `object`

Defined in: [src/store/api/organizationApi.ts:27](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L27)

#### Properties

##### joinedAt

> **joinedAt**: `string`

Defined in: [src/store/api/organizationApi.ts:32](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L32)

##### role

> **role**: [`OrganizationMemberRole`](#organizationmemberrole-1)

Defined in: [src/store/api/organizationApi.ts:31](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L31)

##### userEmail

> **userEmail**: `string`

Defined in: [src/store/api/organizationApi.ts:29](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L29)

##### userId

> **userId**: `string`

Defined in: [src/store/api/organizationApi.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L28)

##### userName

> **userName**: `string`

Defined in: [src/store/api/organizationApi.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L30)

***

### OrganizationMemberRole

> **OrganizationMemberRole** = `"OWNER"` \| `"ADMIN"` \| `"MEMBER"`

Defined in: [src/store/api/organizationApi.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L17)

***

### UpdateMemberRoleRequest

> **UpdateMemberRoleRequest** = `object`

Defined in: [src/store/api/organizationApi.ts:67](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L67)

#### Properties

##### role

> **role**: [`OrganizationMemberRole`](#organizationmemberrole-1)

Defined in: [src/store/api/organizationApi.ts:68](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L68)

***

### UpdateOrganizationRequest

> **UpdateOrganizationRequest** = `object`

Defined in: [src/store/api/organizationApi.ts:53](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L53)

#### Properties

##### name

> **name**: `string`

Defined in: [src/store/api/organizationApi.ts:54](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L54)

##### settings?

> `optional` **settings**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/organizationApi.ts:55](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L55)

***

### UpdateSettingsRequest

> **UpdateSettingsRequest** = `object`

Defined in: [src/store/api/organizationApi.ts:58](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L58)

#### Properties

##### settings

> **settings**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/organizationApi.ts:59](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L59)

## Variables

### organizationApi

> `const` **organizationApi**: `Api`\<`BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, \{ `acceptInvitation`: `MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMember`](#organizationmember), `"organizationApi"`, `unknown`\>; `createOrganization`: `MutationDefinition`\<[`CreateOrganizationRequest`](#createorganizationrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>; `declineInvitation`: `MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>; `deleteOrganization`: `MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>; `getOrganization`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>; `getOrganizationBySlug`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>; `getOrganizationMembers`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMemberInfo`](#organizationmemberinfo)[], `"organizationApi"`, `unknown`\>; `getPendingInvitations`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Invitation`](#invitation)[], `"organizationApi"`, `unknown`\>; `getUserOrganizations`: `QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization)[], `"organizationApi"`, `unknown`\>; `inviteUser`: `MutationDefinition`\<`object` & [`InviteUserRequest`](#inviteuserrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Invitation`](#invitation), `"organizationApi"`, `unknown`\>; `removeMember`: `MutationDefinition`\<\{ `organizationId`: `string`; `userId`: `string`; \}, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>; `revokeInvitation`: `MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>; `updateMemberRole`: `MutationDefinition`\<`object` & [`UpdateMemberRoleRequest`](#updatememberrolerequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMember`](#organizationmember), `"organizationApi"`, `unknown`\>; `updateOrganization`: `MutationDefinition`\<`object` & [`UpdateOrganizationRequest`](#updateorganizationrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>; `updateSettings`: `MutationDefinition`\<`object` & [`UpdateSettingsRequest`](#updatesettingsrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>; \}, `"organizationApi"`, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, *typeof* `coreModuleName` \| *typeof* `reactHooksModuleName`\>

Defined in: [src/store/api/organizationApi.ts:71](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L71)

***

### useAcceptInvitationMutation

> **useAcceptInvitationMutation**: `UseMutation`\<`MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMember`](#organizationmember), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:248](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L248)

***

### useCreateOrganizationMutation

> **useCreateOrganizationMutation**: `UseMutation`\<`MutationDefinition`\<[`CreateOrganizationRequest`](#createorganizationrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:239](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L239)

***

### useDeclineInvitationMutation

> **useDeclineInvitationMutation**: `UseMutation`\<`MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:249](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L249)

***

### useDeleteOrganizationMutation

> **useDeleteOrganizationMutation**: `UseMutation`\<`MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:245](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L245)

***

### useGetOrganizationBySlugQuery

> **useGetOrganizationBySlugQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:241](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L241)

***

### useGetOrganizationMembersQuery

> **useGetOrganizationMembersQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMemberInfo`](#organizationmemberinfo)[], `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:246](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L246)

***

### useGetOrganizationQuery

> **useGetOrganizationQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:240](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L240)

***

### useGetPendingInvitationsQuery

> **useGetPendingInvitationsQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Invitation`](#invitation)[], `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:250](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L250)

***

### useGetUserOrganizationsQuery

> **useGetUserOrganizationsQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization)[], `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:242](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L242)

***

### useInviteUserMutation

> **useInviteUserMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`InviteUserRequest`](#inviteuserrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Invitation`](#invitation), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:247](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L247)

***

### useLazyGetOrganizationBySlugQuery

> **useLazyGetOrganizationBySlugQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:256](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L256)

***

### useLazyGetOrganizationQuery

> **useLazyGetOrganizationQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:255](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L255)

***

### useRemoveMemberMutation

> **useRemoveMemberMutation**: `UseMutation`\<`MutationDefinition`\<\{ `organizationId`: `string`; `userId`: `string`; \}, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:252](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L252)

***

### useRevokeInvitationMutation

> **useRevokeInvitationMutation**: `UseMutation`\<`MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, `void`, `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:251](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L251)

***

### useUpdateMemberRoleMutation

> **useUpdateMemberRoleMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`UpdateMemberRoleRequest`](#updatememberrolerequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`OrganizationMember`](#organizationmember), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:253](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L253)

***

### useUpdateOrganizationMutation

> **useUpdateOrganizationMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`UpdateOrganizationRequest`](#updateorganizationrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:243](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L243)

***

### useUpdateSettingsMutation

> **useUpdateSettingsMutation**: `UseMutation`\<`MutationDefinition`\<`object` & [`UpdateSettingsRequest`](#updatesettingsrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"Organization"` \| `"OrganizationMember"` \| `"Invitation"`, [`Organization`](#organization), `"organizationApi"`, `unknown`\>\>

Defined in: [src/store/api/organizationApi.ts:244](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/organizationApi.ts#L244)
