module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    '@docusaurus/eslint-plugin/recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    ecmaFeatures: {
      jsx: true,
    },
  },
  plugins: ['@typescript-eslint'],
  rules: {
    // Custom rules for documentation project
    'no-console': 'warn',
    'no-unused-vars': 'error',
    '@typescript-eslint/no-unused-vars': 'error',
  },
  overrides: [
    {
      files: ['*.md', '*.mdx'],
      parser: 'eslint-plugin-markdownlint/parser',
      extends: ['plugin:markdownlint/recommended'],
    },
  ],
};