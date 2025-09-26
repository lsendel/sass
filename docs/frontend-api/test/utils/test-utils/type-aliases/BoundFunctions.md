# Type Alias: BoundFunctions\<Q\>

> **BoundFunctions**\<`Q`\> = `Q` *extends* *typeof* [`queries`](../namespaces/queries/index.md) ? `object` & `{ [P in keyof Q]: BoundFunction<Q[P]> }` : `{ [P in keyof Q]: BoundFunction<Q[P]> }`

Defined in: frontend/node\_modules/@testing-library/dom/types/get-queries-for-element.d.ts:10

## Type Parameters

### Q

`Q`
