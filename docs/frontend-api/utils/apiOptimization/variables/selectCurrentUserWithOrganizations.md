# Variable: selectCurrentUserWithOrganizations

> `const` **selectCurrentUserWithOrganizations**: (`state`, ...`params`) => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \} & `object` & `object` & `object`

Defined in: [frontend/src/utils/apiOptimization.ts:54](https://github.com/lsendel/sass/blob/ca8b2b87627589617e0de57047e1f50d53e78078/frontend/src/utils/apiOptimization.ts#L54)

## Type Declaration

### clearCache()

> **clearCache**: () => `void`

#### Returns

`void`

### resetResultsCount()

> **resetResultsCount**: () => `void`

#### Returns

`void`

### resultsCount()

> **resultsCount**: () => `number`

#### Returns

`number`

## Type Declaration

### dependencies

> **dependencies**: \[(`state`) => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}\]

The array of the input selectors used by `createSelector` to compose the
combiner (`OutputSelectorFields.memoizedResultFunc memoizedResultFunc`).

### dependencyRecomputations()

> **dependencyRecomputations**: () => `number`

Counts the number of times the input selectors (`OutputSelectorFields.dependencies dependencies`)
have been recalculated. This is distinct from `OutputSelectorFields.recomputations recomputations`,
which tracks the recalculations of the result function.

#### Returns

`number`

#### Since

5.0.0

### lastResult()

> **lastResult**: () => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

#### Returns

`null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

#### Returns

The last result calculated by `OutputSelectorFields.memoizedResultFunc memoizedResultFunc`.

### memoizedResultFunc

> **memoizedResultFunc**: (...`resultFuncArgs`) => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \} & `object`

The memoized version of `OutputSelectorFields.resultFunc resultFunc`.

#### Type Declaration

##### clearCache()

> **clearCache**: () => `void`

###### Returns

`void`

##### resetResultsCount()

> **resetResultsCount**: () => `void`

###### Returns

`void`

##### resultsCount()

> **resultsCount**: () => `number`

###### Returns

`number`

### recomputations()

> **recomputations**: () => `number`

Counts the number of times `OutputSelectorFields.memoizedResultFunc memoizedResultFunc` has been recalculated.

#### Returns

`number`

### resetDependencyRecomputations()

> **resetDependencyRecomputations**: () => `void`

Resets the count `OutputSelectorFields.dependencyRecomputations dependencyRecomputations`
for the input selectors (`OutputSelectorFields.dependencies dependencies`)
of a memoized selector.

#### Returns

`void`

#### Since

5.0.0

### resetRecomputations()

> **resetRecomputations**: () => `void`

Resets the count of `OutputSelectorFields.recomputations recomputations` count to 0.

#### Returns

`void`

### resultFunc()

> **resultFunc**: (...`resultFuncArgs`) => `null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

The final function passed to `createSelector`. Otherwise known as the `combiner`.

A function that takes input selectors' return values as arguments and returns a result. Otherwise known as `resultFunc`.

#### Parameters

##### resultFuncArgs

...\[`null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}\]

Return values of input selectors.

#### Returns

`null` \| \{ `createdAt`: `string`; `email`: `string`; `emailVerified`: `boolean`; `firstName?`: `string`; `id`: `string`; `lastLoginAt?`: `string`; `lastName?`: `string`; `organizationId?`: `string`; `role`: `"USER"` \| `"ORGANIZATION_ADMIN"` \| `"ADMIN"`; `updatedAt`: `string`; \}

The return value of `OutputSelectorFields.resultFunc resultFunc`.

## Type Declaration

### argsMemoize()

> **argsMemoize**: \<`Func`\>(`func`, `options?`) => `Func` & `object`

The optional memoize function that is used to memoize the arguments
passed into the output selector generated by `createSelector`
(e.g., `lruMemoize` or `weakMapMemoize`).

When passed directly into `createSelector`, it overrides the
`argsMemoize` function initially passed into `createSelectorCreator`.
If none was initially provided, `weakMapMemoize` will be used.

**`Experimental`**

Creates a tree of `WeakMap`-based cache nodes based on the identity of the
arguments it's been called with (in this case, the extracted values from your input selectors).
This allows `weakMapMemoize` to have an effectively infinite cache size.
Cache results will be kept in memory as long as references to the arguments still exist,
and then cleared out as the arguments are garbage-collected.

__Design Tradeoffs for `weakMapMemoize`:__
- Pros:
  - It has an effectively infinite cache size, but you have no control over
  how long values are kept in cache as it's based on garbage collection and `WeakMap`s.
- Cons:
  - There's currently no way to alter the argument comparisons.
  They're based on strict reference equality.
  - It's roughly the same speed as `lruMemoize`, although likely a fraction slower.

__Use Cases for `weakMapMemoize`:__
- This memoizer is likely best used for cases where you need to call the
same selector instance with many different arguments, such as a single
selector instance that is used in a list item component and called with
item IDs like:
  ```ts
  useSelector(state => selectSomeData(state, props.category))
  ```

#### Type Parameters

##### Func

`Func` *extends* `AnyFunction`

The type of the function that is memoized.

#### Parameters

##### func

`Func`

The function to be memoized.

##### options?

`WeakMapMemoizeOptions`\<`ReturnType`\<`Func`\>\>

#### Returns

`Func` & `object`

A memoized function with a `.clearCache()` method attached.

#### Examples

<caption>Using `createSelector`</caption>
```ts
import { createSelector, weakMapMemoize } from 'reselect'

interface RootState {
  items: { id: number; category: string; name: string }[]
}

const selectItemsByCategory = createSelector(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category),
  {
    memoize: weakMapMemoize,
    argsMemoize: weakMapMemoize
  }
)
```

<caption>Using `createSelectorCreator`</caption>
```ts
import { createSelectorCreator, weakMapMemoize } from 'reselect'

const createSelectorWeakMap = createSelectorCreator({ memoize: weakMapMemoize, argsMemoize: weakMapMemoize })

const selectItemsByCategory = createSelectorWeakMap(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category)
)
```

#### See

[\`weakMapMemoize\`](https://reselect.js.org/api/weakMapMemoize)

#### Since

5.0.0

#### Example

```ts
import { createSelector, weakMapMemoize } from 'reselect'

const selectItemsByCategory = createSelector(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category),
  { argsMemoize: weakMapMemoize }
)
```

#### Default

```ts
weakMapMemoize
```

#### Since

5.0.0

### memoize()

> **memoize**: \<`Func`\>(`func`, `options?`) => `Func` & `object`

The memoize function that is used to memoize the `OutputSelectorFields.resultFunc resultFunc`
inside `createSelector` (e.g., `lruMemoize` or `weakMapMemoize`).

When passed directly into `createSelector`, it overrides the `memoize` function initially passed into `createSelectorCreator`.

**`Experimental`**

Creates a tree of `WeakMap`-based cache nodes based on the identity of the
arguments it's been called with (in this case, the extracted values from your input selectors).
This allows `weakMapMemoize` to have an effectively infinite cache size.
Cache results will be kept in memory as long as references to the arguments still exist,
and then cleared out as the arguments are garbage-collected.

__Design Tradeoffs for `weakMapMemoize`:__
- Pros:
  - It has an effectively infinite cache size, but you have no control over
  how long values are kept in cache as it's based on garbage collection and `WeakMap`s.
- Cons:
  - There's currently no way to alter the argument comparisons.
  They're based on strict reference equality.
  - It's roughly the same speed as `lruMemoize`, although likely a fraction slower.

__Use Cases for `weakMapMemoize`:__
- This memoizer is likely best used for cases where you need to call the
same selector instance with many different arguments, such as a single
selector instance that is used in a list item component and called with
item IDs like:
  ```ts
  useSelector(state => selectSomeData(state, props.category))
  ```

#### Type Parameters

##### Func

`Func` *extends* `AnyFunction`

The type of the function that is memoized.

#### Parameters

##### func

`Func`

The function to be memoized.

##### options?

`WeakMapMemoizeOptions`\<`ReturnType`\<`Func`\>\>

#### Returns

`Func` & `object`

A memoized function with a `.clearCache()` method attached.

#### Examples

<caption>Using `createSelector`</caption>
```ts
import { createSelector, weakMapMemoize } from 'reselect'

interface RootState {
  items: { id: number; category: string; name: string }[]
}

const selectItemsByCategory = createSelector(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category),
  {
    memoize: weakMapMemoize,
    argsMemoize: weakMapMemoize
  }
)
```

<caption>Using `createSelectorCreator`</caption>
```ts
import { createSelectorCreator, weakMapMemoize } from 'reselect'

const createSelectorWeakMap = createSelectorCreator({ memoize: weakMapMemoize, argsMemoize: weakMapMemoize })

const selectItemsByCategory = createSelectorWeakMap(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category)
)
```

#### See

[\`weakMapMemoize\`](https://reselect.js.org/api/weakMapMemoize)

#### Since

5.0.0

#### Example

```ts
import { createSelector, weakMapMemoize } from 'reselect'

const selectItemsByCategory = createSelector(
  [
    (state: RootState) => state.items,
    (state: RootState, category: string) => category
  ],
  (items, category) => items.filter(item => item.category === category),
  { memoize: weakMapMemoize }
)
```

#### Since

5.0.0
