[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / test/fixtures/users

# test/fixtures/users

## Type Aliases

### UserFixture

> **UserFixture** = [`User`](../../types/api.md#user) & `object`

Defined in: src/test/fixtures/users.ts:3

#### Type Declaration

##### lastActiveAt?

> `optional` **lastActiveAt**: `string` \| `null`

##### name?

> `optional` **name**: `string`

##### preferences?

> `optional` **preferences**: `Record`\<`string`, `unknown`\>

##### provider?

> `optional` **provider**: `string`

## Functions

### createMockUser()

> **createMockUser**(`overrides`): [`UserFixture`](#userfixture)

Defined in: src/test/fixtures/users.ts:29

#### Parameters

##### overrides

`Partial`\<[`UserFixture`](#userfixture)\> = `{}`

#### Returns

[`UserFixture`](#userfixture)
