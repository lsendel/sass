# Interface: RenderHookResult\<Result, Props\>

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:184

## Type Parameters

### Result

`Result`

### Props

`Props`

## Properties

### rerender()

> **rerender**: (`props?`) => `void`

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:188

Triggers a re-render. The props will be passed to your renderHook callback.

#### Parameters

##### props?

`Props`

#### Returns

`void`

***

### result

> **result**: `object`

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:193

This is a stable reference to the latest value returned by your renderHook
callback

#### current

> **current**: `Result`

The value returned by your renderHook callback

***

### unmount()

> **unmount**: () => `void`

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:203

Unmounts the test component. This is useful for when you need to test
any cleanup your useEffects have.

#### Returns

`void`
