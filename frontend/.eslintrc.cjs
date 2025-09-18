/* eslint-env node */
module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
    'plugin:react/jsx-runtime',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    ecmaFeatures: { jsx: true },
  },
  plugins: ['react-refresh', '@typescript-eslint', 'react'],
  settings: {
    react: { version: 'detect' },
  },
  rules: {
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    '@typescript-eslint/no-unused-vars': ['error'],
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/explicit-module-boundary-types': 'off',
    '@typescript-eslint/no-explicit-any': 'warn',
    'react/prop-types': 'off',
    'react/react-in-jsx-scope': 'off',
    'prefer-const': 'error',
    'no-var': 'error',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    eqeqeq: ['error', 'always'],
    curly: 'error',

    // Separation of concerns (coarse-grained):
    // - UI components/hooks/utils/lib should not import from pages
    // - Store and API should not import from pages or components
    'no-restricted-imports': [
      'error',
      {
        patterns: [
          {
            group: ['../pages/*', '../../pages/*', '@/pages/*'],
            message: 'Components/hooks/utils/lib should not import from pages',
            targetImports: ['src/components/**', 'src/hooks/**', 'src/utils/**', 'src/lib/**'],
          },
          {
            group: [
              '../components/*',
              '../../components/*',
              '@/components/*',
              '../pages/*',
              '../../pages/*',
              '@/pages/*',
            ],
            message: 'Store and API should not import from components or pages',
            targetImports: ['src/store/**', 'src/store/api/**'],
          },
        ],
      },
    ],
  },
}

