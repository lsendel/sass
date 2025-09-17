# CLI Interface Contracts

## docs-build Command

### Command Interface
```bash
docs-build [options]
```

### Options
- `--help, -h`: Show help information
- `--version, -V`: Show version number
- `--format, -f`: Output format (json|text|yaml) [default: text]
- `--config, -c`: Configuration file path [default: ./docusaurus.config.js]
- `--out-dir, -o`: Output directory [default: ./build]
- `--validate`: Enable content validation [default: true]
- `--watch, -w`: Watch for changes and rebuild
- `--verbose, -v`: Verbose output
- `--clean`: Clean output directory before build

### Input/Output Contract

**Input**:
- Documentation source files (markdown)
- Configuration file
- Static assets

**Output (text format)**:
```
✓ Building documentation...
✓ Processing 45 pages
✓ Generating search index
✓ Optimizing assets
✓ Build completed in 3.2s
📁 Output: ./build
```

**Output (json format)**:
```json
{
  "status": "success",
  "buildTime": 3200,
  "stats": {
    "pagesProcessed": 45,
    "assetsOptimized": 23,
    "searchIndexSize": "2.1MB"
  },
  "outputDir": "./build",
  "warnings": [],
  "errors": []
}
```

**Error Scenarios**:
- Missing configuration file
- Invalid markdown syntax
- Broken internal links
- Missing required front matter

## docs-serve Command

### Command Interface
```bash
docs-serve [options]
```

### Options
- `--help, -h`: Show help information
- `--version, -V`: Show version number
- `--format, -f`: Output format (json|text) [default: text]
- `--port, -p`: Port number [default: 3000]
- `--host`: Host address [default: localhost]
- `--open, -o`: Open browser automatically
- `--hot-reload`: Enable hot reloading [default: true]
- `--build-dir`: Build directory to serve [default: ./build]

### Input/Output Contract

**Output (text format)**:
```
🚀 Documentation server started
📍 Local: http://localhost:3000
📍 Network: http://192.168.1.100:3000
👀 Watching for changes...
```

**Output (json format)**:
```json
{
  "status": "started",
  "urls": {
    "local": "http://localhost:3000",
    "network": "http://192.168.1.100:3000"
  },
  "config": {
    "port": 3000,
    "hotReload": true,
    "buildDir": "./build"
  }
}
```

## docs-validate Command

### Command Interface
```bash
docs-validate [path] [options]
```

### Options
- `--help, -h`: Show help information
- `--version, -V`: Show version number
- `--format, -f`: Output format (json|text|junit) [default: text]
- `--fix`: Auto-fix issues where possible
- `--config, -c`: Configuration file path
- `--check-links`: Validate internal and external links
- `--check-images`: Validate image references
- `--template`: Validate against templates
- `--schema`: Validate front matter schema

### Input/Output Contract

**Input**: Documentation files or directory path

**Output (text format)**:
```
🔍 Validating documentation...

✓ ./docs/architecture/overview.md
✗ ./docs/api/authentication.md
  - Line 15: Missing required front matter field 'lastUpdated'
  - Line 23: Broken link to './nonexistent.md'
⚠ ./docs/guides/setup.md
  - Line 8: External link not responding (timeout)

Summary:
  ✓ 42 files passed
  ✗ 1 file with errors
  ⚠ 1 file with warnings
```

**Output (json format)**:
```json
{
  "summary": {
    "totalFiles": 44,
    "passed": 42,
    "errors": 1,
    "warnings": 1
  },
  "results": [
    {
      "file": "./docs/api/authentication.md",
      "status": "error",
      "issues": [
        {
          "type": "schema",
          "severity": "error",
          "line": 15,
          "message": "Missing required front matter field 'lastUpdated'"
        },
        {
          "type": "link",
          "severity": "error",
          "line": 23,
          "message": "Broken link to './nonexistent.md'"
        }
      ]
    }
  ]
}
```

## docs-create Command

### Command Interface
```bash
docs-create <template> <title> [options]
```

### Options
- `--help, -h`: Show help information
- `--version, -V`: Show version number
- `--format, -f`: Output format (json|text) [default: text]
- `--path, -p`: Output file path [auto-generated if not specified]
- `--section, -s`: Documentation section
- `--author, -a`: Author name
- `--interactive, -i`: Interactive mode for additional fields

### Input/Output Contract

**Input**:
- Template name (adr|api|feature|guide|troubleshooting)
- Document title
- Optional metadata

**Output (text format)**:
```
📄 Creating new documentation page...
✓ Template: Architecture Decision Record
✓ Title: Database Migration Strategy
✓ File: ./docs/architecture/database-migration-strategy.md
✓ Created successfully!

Next steps:
1. Edit the content sections
2. Add relevant diagrams to ./static/img/
3. Run 'docs-validate' to check completeness
```

**Output (json format)**:
```json
{
  "status": "created",
  "file": {
    "path": "./docs/architecture/database-migration-strategy.md",
    "template": "adr",
    "title": "Database Migration Strategy",
    "id": "database-migration-strategy"
  },
  "nextSteps": [
    "Edit the content sections",
    "Add relevant diagrams to ./static/img/",
    "Run 'docs-validate' to check completeness"
  ]
}
```

## Error Handling Standards

### Exit Codes
- `0`: Success
- `1`: General error
- `2`: Invalid arguments
- `3`: Configuration error
- `4`: Validation failure
- `5`: Build failure
- `6`: Network error

### Error Output Format
All commands output errors to stderr in consistent format:

**Text format**:
```
❌ Error: Configuration file not found
   Path: ./docusaurus.config.js
   Suggestion: Run 'docs-init' to create initial configuration
```

**JSON format**:
```json
{
  "error": {
    "code": "CONFIG_NOT_FOUND",
    "message": "Configuration file not found",
    "details": {
      "path": "./docusaurus.config.js",
      "suggestion": "Run 'docs-init' to create initial configuration"
    }
  }
}
```

## Integration Contracts

### Git Integration
- Commands respect `.gitignore` for file operations
- Automatic commit hooks for documentation changes
- Branch-based validation for pull requests

### CI/CD Integration
- JSON output format for automated parsing
- JUnit XML format for test reporting integration
- Exit codes for pipeline decision making

### IDE Integration
- Language server protocol support for validation
- Real-time link checking
- Auto-completion for cross-references