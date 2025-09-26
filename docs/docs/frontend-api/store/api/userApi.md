[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / store/api/userApi

# store/api/userApi

## Type Aliases

### PagedUserResponse

> **PagedUserResponse** = `object`

Defined in: [src/store/api/userApi.ts:23](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L23)

#### Properties

##### first

> **first**: `boolean`

Defined in: [src/store/api/userApi.ts:29](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L29)

##### last

> **last**: `boolean`

Defined in: [src/store/api/userApi.ts:30](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L30)

##### page

> **page**: `number`

Defined in: [src/store/api/userApi.ts:25](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L25)

##### size

> **size**: `number`

Defined in: [src/store/api/userApi.ts:26](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L26)

##### totalElements

> **totalElements**: `number`

Defined in: [src/store/api/userApi.ts:27](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L27)

##### totalPages

> **totalPages**: `number`

Defined in: [src/store/api/userApi.ts:28](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L28)

##### users

> **users**: [`User`](../../types/api.md#user)[]

Defined in: [src/store/api/userApi.ts:24](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L24)

***

### UpdatePreferencesRequest

> **UpdatePreferencesRequest** = `object`

Defined in: [src/store/api/userApi.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L13)

#### Properties

##### preferences

> **preferences**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/userApi.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L14)

***

### UpdateProfileRequest

> **UpdateProfileRequest** = `object`

Defined in: [src/store/api/userApi.ts:8](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L8)

#### Properties

##### name

> **name**: `string`

Defined in: [src/store/api/userApi.ts:9](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L9)

##### preferences

> **preferences**: `Record`\<`string`, `unknown`\>

Defined in: [src/store/api/userApi.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L10)

***

### UserStatistics

> **UserStatistics** = `object`

Defined in: [src/store/api/userApi.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L17)

#### Properties

##### newUsersThisWeek

> **newUsersThisWeek**: `number`

Defined in: [src/store/api/userApi.ts:19](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L19)

##### totalUsers

> **totalUsers**: `number`

Defined in: [src/store/api/userApi.ts:18](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L18)

##### usersByProvider

> **usersByProvider**: `Record`\<`string`, `number`\>

Defined in: [src/store/api/userApi.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L20)

## Variables

### useCountActiveUsersQuery

> **useCountActiveUsersQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `number`, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:149](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L149)

***

### useDeleteCurrentUserMutation

> **useDeleteCurrentUserMutation**: `UseMutation`\<`MutationDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `void`, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:144](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L144)

***

### useGetAllUsersQuery

> **useGetAllUsersQuery**: `UseQuery`\<`QueryDefinition`\<\{ `page?`: `number`; `size?`: `number`; `sortBy?`: `string`; `sortDirection?`: `"asc"` \| `"desc"`; \}, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, [`PagedUserResponse`](#pageduserresponse), `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:146](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L146)

***

### useGetCurrentUserQuery

> **useGetCurrentUserQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:140](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L140)

***

### useGetRecentUsersQuery

> **useGetRecentUsersQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:147](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L147)

***

### useGetUserByIdQuery

> **useGetUserByIdQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:141](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L141)

***

### useGetUsersByProviderQuery

> **useGetUsersByProviderQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:150](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L150)

***

### useGetUserStatisticsQuery

> **useGetUserStatisticsQuery**: `UseQuery`\<`QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, [`UserStatistics`](#userstatistics), `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:148](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L148)

***

### useLazyGetAllUsersQuery

> **useLazyGetAllUsersQuery**: `UseLazyQuery`\<`QueryDefinition`\<\{ `page?`: `number`; `size?`: `number`; `sortBy?`: `string`; `sortDirection?`: `"asc"` \| `"desc"`; \}, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, [`PagedUserResponse`](#pageduserresponse), `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:154](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L154)

***

### useLazySearchUsersQuery

> **useLazySearchUsersQuery**: `UseLazyQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:153](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L153)

***

### userApi

> `const` **userApi**: `Api`\<`BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, \{ `countActiveUsers`: `QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `number`, `"userApi"`, `unknown`\>; `deleteCurrentUser`: `MutationDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `void`, `"userApi"`, `unknown`\>; `getAllUsers`: `QueryDefinition`\<\{ `page?`: `number`; `size?`: `number`; `sortBy?`: `string`; `sortDirection?`: `"asc"` \| `"desc"`; \}, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, [`PagedUserResponse`](#pageduserresponse), `"userApi"`, `unknown`\>; `getCurrentUser`: `QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>; `getRecentUsers`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>; `getUserById`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>; `getUsersByProvider`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>; `getUserStatistics`: `QueryDefinition`\<`void`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, [`UserStatistics`](#userstatistics), `"userApi"`, `unknown`\>; `restoreUser`: `MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>; `searchUsers`: `QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>; `updatePreferences`: `MutationDefinition`\<[`UpdatePreferencesRequest`](#updatepreferencesrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>; `updateProfile`: `MutationDefinition`\<[`UpdateProfileRequest`](#updateprofilerequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>; \}, `"userApi"`, `"User"` \| `"UserProfile"`, *typeof* `coreModuleName` \| *typeof* `reactHooksModuleName`\>

Defined in: [src/store/api/userApi.ts:33](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L33)

***

### useRestoreUserMutation

> **useRestoreUserMutation**: `UseMutation`\<`MutationDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:151](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L151)

***

### useSearchUsersQuery

> **useSearchUsersQuery**: `UseQuery`\<`QueryDefinition`\<`string`, `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, `object`[], `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:145](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L145)

***

### useUpdatePreferencesMutation

> **useUpdatePreferencesMutation**: `UseMutation`\<`MutationDefinition`\<[`UpdatePreferencesRequest`](#updatepreferencesrequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:143](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L143)

***

### useUpdateProfileMutation

> **useUpdateProfileMutation**: `UseMutation`\<`MutationDefinition`\<[`UpdateProfileRequest`](#updateprofilerequest), `BaseQueryFn`\<`string` \| `FetchArgs`, `unknown`, `FetchBaseQueryError`, \{ \}, `FetchBaseQueryMeta`\>, `"User"` \| `"UserProfile"`, \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}, `"userApi"`, `unknown`\>\>

Defined in: [src/store/api/userApi.ts:142](https://github.com/lsendel/sass/blob/main/frontend/src/store/api/userApi.ts#L142)
