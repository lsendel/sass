# Spring Boot Modulith Payment Platform Documentation

This documentation site is built using [Docusaurus](https://docusaurus.io/), a modern static website generator.

## Installation

```bash
npm install
# or
yarn
```

## Local Development

```bash
npm start
# or
yarn start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

## Build

```bash
npm run build
# or
yarn build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

## API Documentation

The platform includes comprehensive API documentation for both backend and frontend:

### Backend API (Java)
- **Location**: `docs/backend-api-javadoc/`
- **Generation**: Run `./gradlew javadoc` in the backend directory
- **Content**: Complete Javadoc for all Java modules

### Frontend API (TypeScript)
- **Location**: `docs/frontend-api/`
- **Generation**: Run `npm run docs:build` in the frontend directory
- **Content**: TypeDoc-generated documentation for React components and TypeScript modules

### Automated Documentation Generation

Use the provided script to generate both backend and frontend documentation:

```bash
# From project root
./generate-docs.sh
```

## Documentation Structure

```
docs/
├── docs/                    # Main documentation content
│   ├── architecture/        # Architecture documentation
│   ├── development-guide/   # Developer guides
│   ├── security/           # Security documentation
│   ├── testing/            # Testing guides
│   ├── backend-api-javadoc/ # Generated Java API docs
│   ├── frontend-api/       # Generated TypeScript API docs
│   └── api-reference.md    # API documentation index
├── src/                    # Docusaurus source files
├── static/                 # Static assets
└── docusaurus.config.ts    # Docusaurus configuration
```

## Deployment

Using SSH:

```bash
USE_SSH=true yarn deploy
```

Not using SSH:

```bash
GIT_USER=<Your GitHub username> yarn deploy
```

If you are using GitHub pages for hosting, this command is a convenient way to build the website and push to the `gh-pages` branch.
