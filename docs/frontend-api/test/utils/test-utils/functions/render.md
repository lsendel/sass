# Function: render()

## Call Signature

> **render**\<`Q`, `Container`, `BaseElement`\>(`ui`, `options`): [`RenderResult`](../type-aliases/RenderResult.md)\<`Q`, `Container`, `BaseElement`\>

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:171

Render into a container which is appended to document.body. It should be used with cleanup.

### Type Parameters

#### Q

`Q` *extends* [`Queries`](../interfaces/Queries.md) = [`queries`](../namespaces/queries/index.md)

#### Container

`Container` *extends* `Container` = `HTMLElement`

#### BaseElement

`BaseElement` *extends* `Container` = `Container`

### Parameters

#### ui

`ReactNode`

#### options

[`RenderOptions`](../interfaces/RenderOptions.md)\<`Q`, `Container`, `BaseElement`\>

### Returns

[`RenderResult`](../type-aliases/RenderResult.md)\<`Q`, `Container`, `BaseElement`\>

## Call Signature

> **render**(`ui`, `options?`): [`RenderResult`](../type-aliases/RenderResult.md)

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:179

Render into a container which is appended to document.body. It should be used with cleanup.

### Parameters

#### ui

`ReactNode`

#### options?

`Omit`\<[`RenderOptions`](../interfaces/RenderOptions.md)\<[`queries`](../namespaces/queries/index.md), `HTMLElement`, `HTMLElement`\>, `"queries"`\>

### Returns

[`RenderResult`](../type-aliases/RenderResult.md)
