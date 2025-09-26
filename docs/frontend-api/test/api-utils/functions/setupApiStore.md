# Function: setupApiStore()

> **setupApiStore**\<`T`\>(`api`, `extraReducers?`, `preloadedState?`): `EnhancedStore`\<\{ `auth`: [`AuthState`](../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../store/slices/uiSlice/type-aliases/UiState.md); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ `dispatch`: `ThunkDispatch`\<\{ `auth`: [`AuthState`](../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../store/slices/uiSlice/type-aliases/UiState.md); \}\>; \}\>, `StoreEnhancer`\]\>\>

Defined in: [frontend/src/test/api-utils.ts:8](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/test/api-utils.ts#L8)

## Type Parameters

### T

`T` *extends* `Api`\<`BaseQueryFn`, `any`, `any`, `any`\>

## Parameters

### api

`T`

### extraReducers?

`Record`\<`string`, `any`\>

### preloadedState?

`any`

## Returns

`EnhancedStore`\<\{ `auth`: [`AuthState`](../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../store/slices/uiSlice/type-aliases/UiState.md); \}, `UnknownAction`, `Tuple`\<\[`StoreEnhancer`\<\{ `dispatch`: `ThunkDispatch`\<\{ `auth`: [`AuthState`](../../../store/slices/authSlice/type-aliases/AuthState.md); `ui`: [`UiState`](../../../store/slices/uiSlice/type-aliases/UiState.md); \}\>; \}\>, `StoreEnhancer`\]\>\>
