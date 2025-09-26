# Variable: act

> `const` **act**: `0` *extends* `1` & *typeof* `reactAct` ? *typeof* `reactDeprecatedAct` : *typeof* `reactAct`

Defined in: frontend/node\_modules/@testing-library/react/types/index.d.ts:285

Simply calls React.act(cb)
If that's not available (older version of react) then it
simply calls the deprecated version which is ReactTestUtils.act(cb)
