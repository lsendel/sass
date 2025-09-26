[**payment-platform-frontend v1.0.0**](../../README.md)

***

[payment-platform-frontend](../../README.md) / test/utils/mockStore

# test/utils/mockStore

## Type Aliases

### MockStore

> **MockStore** = `ReturnType`\<*typeof* [`createMockStore`](#createmockstore)\>

Defined in: src/test/utils/mockStore.ts:48

***

### PartialTestState

> **PartialTestState** = `object`

Defined in: src/test/utils/mockStore.ts:5

#### Properties

##### auth?

> `optional` **auth**: `Partial`\<[`AuthState`](../../store/slices/authSlice.md#authstate)\>

Defined in: src/test/utils/mockStore.ts:6

##### ui?

> `optional` **ui**: `Partial`\<[`UiState`](../../store/slices/uiSlice.md#uistate)\>

Defined in: src/test/utils/mockStore.ts:7

## Functions

### createMockAuthState()

> **createMockAuthState**(`overrides`): [`AuthState`](../../store/slices/authSlice.md#authstate)

Defined in: src/test/utils/mockStore.ts:10

#### Parameters

##### overrides

`Partial`\<[`AuthState`](../../store/slices/authSlice.md#authstate)\> = `{}`

#### Returns

[`AuthState`](../../store/slices/authSlice.md#authstate)

***

### createMockStore()

> **createMockStore**(`initialState`): `EnhancedStore`\<\{ `auth`: [`AuthState`](../../store/slices/authSlice.md#authstate); `ui`: [`UiState`](../../store/slices/uiSlice.md#uistate); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

Defined in: src/test/utils/mockStore.ts:36

#### Parameters

##### initialState

[`PartialTestState`](#partialteststate) = `{}`

#### Returns

`EnhancedStore`\<\{ `auth`: [`AuthState`](../../store/slices/authSlice.md#authstate); `ui`: [`UiState`](../../store/slices/uiSlice.md#uistate); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ \}\>, `StoreEnhancer`\]\>\>

***

### createMockUiState()

> **createMockUiState**(`overrides`): [`UiState`](../../store/slices/uiSlice.md#uistate)

Defined in: src/test/utils/mockStore.ts:21

#### Parameters

##### overrides

`Partial`\<[`UiState`](../../store/slices/uiSlice.md#uistate)\> = `{}`

#### Returns

[`UiState`](../../store/slices/uiSlice.md#uistate)
