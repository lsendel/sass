[**payment-platform-frontend v1.0.0**](../README.md)

***

[payment-platform-frontend](../README.md) / hooks/useRealTimeUpdates

# hooks/useRealTimeUpdates

## Interfaces

### RealTimeOptions

Defined in: [src/hooks/useRealTimeUpdates.ts:3](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L3)

#### Properties

##### enabled?

> `optional` **enabled**: `boolean`

Defined in: [src/hooks/useRealTimeUpdates.ts:4](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L4)

##### interval?

> `optional` **interval**: `number`

Defined in: [src/hooks/useRealTimeUpdates.ts:5](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L5)

##### pauseAfterInactivity?

> `optional` **pauseAfterInactivity**: `number`

Defined in: [src/hooks/useRealTimeUpdates.ts:7](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L7)

##### pauseWhenInactive?

> `optional` **pauseWhenInactive**: `boolean`

Defined in: [src/hooks/useRealTimeUpdates.ts:6](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L6)

***

### RealTimeUpdatesReturn

Defined in: [src/hooks/useRealTimeUpdates.ts:10](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L10)

#### Properties

##### forceUpdate()

> **forceUpdate**: () => `Promise`\<`any`\>

Defined in: [src/hooks/useRealTimeUpdates.ts:17](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L17)

###### Returns

`Promise`\<`any`\>

##### isActive

> **isActive**: `boolean`

Defined in: [src/hooks/useRealTimeUpdates.ts:11](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L11)

##### isPaused

> **isPaused**: `boolean`

Defined in: [src/hooks/useRealTimeUpdates.ts:13](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L13)

##### lastUpdate

> **lastUpdate**: `null` \| `Date`

Defined in: [src/hooks/useRealTimeUpdates.ts:12](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L12)

##### pauseUpdates()

> **pauseUpdates**: () => `void`

Defined in: [src/hooks/useRealTimeUpdates.ts:16](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L16)

###### Returns

`void`

##### resumeUpdates()

> **resumeUpdates**: () => `void`

Defined in: [src/hooks/useRealTimeUpdates.ts:15](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L15)

###### Returns

`void`

##### updateCount

> **updateCount**: `number`

Defined in: [src/hooks/useRealTimeUpdates.ts:14](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L14)

## Functions

### useRealTimeUpdates()

> **useRealTimeUpdates**(`updateFunction`, `options`): [`RealTimeUpdatesReturn`](#realtimeupdatesreturn)

Defined in: [src/hooks/useRealTimeUpdates.ts:20](https://github.com/lsendel/sass/blob/main/frontend/src/hooks/useRealTimeUpdates.ts#L20)

#### Parameters

##### updateFunction

() => `Promise`\<`any`\>

##### options

[`RealTimeOptions`](#realtimeoptions) = `{}`

#### Returns

[`RealTimeUpdatesReturn`](#realtimeupdatesreturn)
