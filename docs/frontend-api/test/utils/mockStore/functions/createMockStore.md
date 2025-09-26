# Function: createMockStore()

> **createMockStore**(`initialState`): `EnhancedStore`\<\{ `auth`: [`AuthState`](../../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../../store/slices/uiSlice/type-aliases/UiState.md); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ `dispatch`: `ThunkDispatch`\<\{ `auth`: [`AuthState`](../../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../../store/slices/uiSlice/type-aliases/UiState.md); \}\>; \}\>, `StoreEnhancer`\]\>\>

Defined in: [frontend/src/test/utils/mockStore.ts:37](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/test/utils/mockStore.ts#L37)

## Parameters

### initialState

[`PartialTestState`](../type-aliases/PartialTestState.md) = `{}`

## Returns

`EnhancedStore`\<\{ `auth`: [`AuthState`](../../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../../store/slices/uiSlice/type-aliases/UiState.md); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ `dispatch`: `ThunkDispatch`\<\{ `auth`: [`AuthState`](../../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../../store/slices/uiSlice/type-aliases/UiState.md); \}\>; \}\>, `StoreEnhancer`\]\>\>
