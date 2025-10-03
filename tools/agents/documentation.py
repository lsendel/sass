#!/usr/bin/env python3
"""
Documentation Agent - Implementation
Comprehensive documentation generation, validation, and enforcement.

This agent ensures all code is properly documented according to
constitutional requirements and best practices.
"""

import re
import json
from typing import Dict, List, Any, Optional
from enum import Enum
from pathlib import Path
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta


class DocumentationType(Enum):
    """Types of documentation."""
    JAVADOC = "javadoc"
    JSDOC = "jsdoc"
    PYTHON_DOCSTRING = "python_docstring"
    OPENAPI = "openapi"
    MARKDOWN = "markdown"
    ADR = "adr"
    README = "readme"
    LLMS_TXT = "llms_txt"


class DocumentationLevel(Enum):
    """Documentation completeness levels."""
    NONE = 0
    MINIMAL = 25
    BASIC = 50
    GOOD = 75
    EXCELLENT = 90
    COMPLETE = 100


@dataclass
class DocumentationResult:
    """Result of documentation generation or validation."""
    success: bool
    documentation: str
    coverage: float
    violations: List[str]
    suggestions: List[str]
    constitutional_compliant: bool


class DocumentationAgent:
    """
    Comprehensive Documentation Agent with constitutional enforcement.

    Generates, validates, and maintains all documentation types while
    ensuring constitutional compliance.
    """

    def __init__(self):
        """Initialize Documentation Agent."""
        self.logger = logging.getLogger("documentation_agent")
        self.constitutional_requirements = self._load_constitutional_requirements()
        self.documentation_templates = self._load_templates()

    def _load_constitutional_requirements(self) -> Dict[str, Any]:
        """Load constitutional documentation requirements."""
        return {
            "library_cli_required": ["--help", "--version", "--format"],
            "tdd_documentation_required": True,
            "api_documentation_required": True,
            "test_hierarchy_documentation": ["contract", "integration", "e2e", "unit"],
            "security_documentation": ["opaque_tokens", "no_jwt"],
            "module_boundary_documentation": True
        }

    def _load_templates(self) -> Dict[str, str]:
        """Load documentation templates."""
        return {
            DocumentationType.JAVADOC: self._javadoc_template(),
            DocumentationType.JSDOC: self._jsdoc_template(),
            DocumentationType.PYTHON_DOCSTRING: self._python_docstring_template(),
            DocumentationType.OPENAPI: self._openapi_template(),
            DocumentationType.README: self._readme_template()
        }

    def generate_documentation(self, context: Dict[str, Any]) -> DocumentationResult:
        """
        Generate appropriate documentation based on context.

        Args:
            context: Code context including language, type, and content

        Returns:
            DocumentationResult with generated documentation
        """
        language = context.get("language", "").lower()
        code_type = context.get("type", "")
        code_content = context.get("content", "")

        # Determine documentation type
        doc_type = self._determine_documentation_type(language, code_type)

        # Generate documentation
        documentation = self._generate_for_type(doc_type, context)

        # Validate constitutional compliance
        violations = self._validate_constitutional_compliance(documentation, context)

        # Calculate coverage
        coverage = self._calculate_documentation_coverage(documentation, code_content)

        # Generate suggestions
        suggestions = self._generate_improvement_suggestions(documentation, context)

        return DocumentationResult(
            success=len(violations) == 0,
            documentation=documentation,
            coverage=coverage,
            violations=violations,
            suggestions=suggestions,
            constitutional_compliant=len(violations) == 0
        )

    def _determine_documentation_type(self, language: str, code_type: str) -> DocumentationType:
        """Determine appropriate documentation type."""
        if language == "java":
            return DocumentationType.JAVADOC
        elif language in ["javascript", "typescript"]:
            return DocumentationType.JSDOC
        elif language == "python":
            return DocumentationType.PYTHON_DOCSTRING
        elif code_type == "api":
            return DocumentationType.OPENAPI
        elif code_type == "architecture":
            return DocumentationType.ADR
        else:
            return DocumentationType.MARKDOWN

    def _generate_for_type(self, doc_type: DocumentationType, context: Dict[str, Any]) -> str:
        """Generate documentation for specific type."""
        if doc_type == DocumentationType.JAVADOC:
            return self._generate_javadoc(context)
        elif doc_type == DocumentationType.JSDOC:
            return self._generate_jsdoc(context)
        elif doc_type == DocumentationType.PYTHON_DOCSTRING:
            return self._generate_python_docstring(context)
        elif doc_type == DocumentationType.OPENAPI:
            return self._generate_openapi(context)
        elif doc_type == DocumentationType.ADR:
            return self._generate_adr(context)
        elif doc_type == DocumentationType.README:
            return self._generate_readme(context)
        else:
            return self._generate_markdown(context)

    def _generate_javadoc(self, context: Dict[str, Any]) -> str:
        """Generate JavaDoc documentation."""
        method_name = context.get("method_name", "method")
        params = context.get("parameters", [])
        returns = context.get("returns", "void")
        throws = context.get("throws", [])

        javadoc = f"""/**
 * {self._generate_description(context)}
 *
 * This method enforces constitutional requirements including:
 * - TDD compliance (tests written first)
 * - Module boundary constraints (event-driven only)
 * - Security requirements (opaque tokens only)
 *"""

        for param in params:
            javadoc += f"\n * @param {param['name']} {param.get('description', 'Parameter description')}"

        if returns != "void":
            javadoc += f"\n * @return {returns} with constitutional compliance status"

        for exception in throws:
            javadoc += f"\n * @throws {exception} if constitutional requirements violated"

        javadoc += """
 * @see ConstitutionalEnforcementAgent
 * @since 1.0.0
 * @constitutional Required - enforces non-negotiable principles
 */"""

        return javadoc

    def _generate_jsdoc(self, context: Dict[str, Any]) -> str:
        """Generate JSDoc/TSDoc documentation."""
        component_name = context.get("component_name", "Component")
        props = context.get("props", [])

        jsdoc = f"""/**
 * {self._generate_description(context)}
 *
 * Implements constitutional requirements:
 * - Opaque token authentication (no JWT)
 * - Event-driven state management
 * - TDD with failing tests first
 *
 * @component
 * @example
 * ```tsx
 * <{component_name}"""

        for prop in props[:2]:  # Show first 2 props in example
            jsdoc += f"\n *   {prop['name']}={{{prop.get('example', 'value')}}}"

        jsdoc += f"""
 * />
 * ```
 *"""

        for prop in props:
            jsdoc += f"\n * @param {{{prop.get('type', 'any')}}} props.{prop['name']} - {prop.get('description', '')}"

        jsdoc += """
 * @returns {JSX.Element} Component with constitutional compliance
 * @constitutional Enforces opaque tokens and event-driven patterns
 */"""

        return jsdoc

    def _generate_python_docstring(self, context: Dict[str, Any]) -> str:
        """Generate Python docstring."""
        function_name = context.get("function_name", "function")
        params = context.get("parameters", [])
        returns = context.get("returns", "None")
        raises = context.get("raises", [])

        docstring = f"""\"\"\"
    {self._generate_description(context)}

    Enforces constitutional requirements including TDD compliance,
    module boundaries, and security standards.

    Args:"""

        for param in params:
            docstring += f"\n        {param['name']}: {param.get('description', 'Parameter description')}"

        docstring += f"""

    Returns:
        {returns}: Result with constitutional compliance status"""

        if raises:
            docstring += "\n\n    Raises:"
            for exception in raises:
                docstring += f"\n        {exception}: If constitutional requirements violated"

        docstring += """

    Constitutional Requirements:
        - TDD: Tests must be written first
        - Security: Opaque tokens only (no JWT)
        - Modules: Event-driven communication only

    Example:
        >>> result = """ + function_name + """(param)
        >>> assert result.constitutional_compliant

    Note:
        This function has constitutional enforcement authority.
    \"\"\""""

        return docstring

    def _generate_openapi(self, context: Dict[str, Any]) -> str:
        """Generate OpenAPI specification."""
        endpoint = context.get("endpoint", "/api/resource")
        method = context.get("method", "POST")

        openapi = f"""openapi: 3.0.3
info:
  title: {context.get('title', 'API')}
  description: |
    Constitutional-compliant API with enforced security standards.

    ## Constitutional Requirements
    - Authentication: Opaque tokens only (no JWT)
    - Module Communication: Event-driven patterns
    - Testing: Contract tests required
  version: 1.0.0

paths:
  {endpoint}:
    {method.lower()}:
      summary: {context.get('summary', 'Operation summary')}
      description: |
        {self._generate_description(context)}
        Enforces constitutional compliance.
      operationId: {context.get('operation_id', 'operation')}
      security:
        - opaqueToken: []  # Constitutional requirement
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Request'
      responses:
        '200':
          description: Success with constitutional compliance
        '403':
          description: Constitutional violation detected

components:
  securitySchemes:
    opaqueToken:
      type: apiKey
      in: header
      name: X-Session-Token
      description: Opaque token (constitutional requirement - no JWT)"""

        return openapi

    def _generate_adr(self, context: Dict[str, Any]) -> str:
        """Generate Architecture Decision Record."""
        title = context.get("title", "Architecture Decision")
        decision = context.get("decision", "Decision description")

        adr = f"""# ADR-{context.get('number', '001')}: {title}

## Status
Accepted

## Context
{context.get('context', 'Background and problem statement')}

Constitutional requirements mandate:
- TDD with test-first development
- Event-driven module communication
- Opaque token authentication

## Decision
{decision}

## Consequences

### Positive
- Constitutional compliance maintained
- {context.get('positive', 'Positive outcomes')}

### Negative
- {context.get('negative', 'Trade-offs')}

## Constitutional Compliance
✅ This decision complies with all constitutional requirements:
- TDD: Test coverage maintained
- Security: Opaque tokens enforced
- Architecture: Event-driven patterns used

## References
- Constitutional Requirements Document
- Spring Boot Modulith Documentation
- OWASP Security Guidelines"""

        return adr

    def _generate_readme(self, context: Dict[str, Any]) -> str:
        """Generate README with constitutional requirements."""
        module_name = context.get("module_name", "Module")

        readme = f"""# {module_name}

[![Constitutional Compliance](https://img.shields.io/badge/Constitutional-Compliant-green)]()
[![TDD Coverage](https://img.shields.io/badge/TDD-100%25-green)]()

## Overview
{context.get('description', 'Module description')}

## Constitutional Requirements ⚠️

This module enforces NON-NEGOTIABLE constitutional principles:

1. **Library-First Architecture**: Standalone library with CLI
2. **TDD Required**: Tests MUST be written first
3. **Module Communication**: Event-driven only
4. **Security**: Opaque tokens only (JWT prohibited)

## Installation
```bash
npm install {module_name.lower()}
```

## CLI Usage (Constitutional Requirement)
```bash
# Required flags per constitution
{module_name.lower()} --help
{module_name.lower()} --version
{module_name.lower()} --format json

# Operations
{module_name.lower()} process --token opaque_token_123
```

## Testing (TDD Required)
```bash
# Run in constitutional order
npm run test:contract
npm run test:integration
npm run test:e2e
npm run test:unit
```

## API Documentation
See [OpenAPI Specification](./docs/api/openapi.yaml)

## Constitutional Compliance
All code changes must pass constitutional validation."""

        return readme

    def _generate_markdown(self, context: Dict[str, Any]) -> str:
        """Generate generic Markdown documentation."""
        return f"""# {context.get('title', 'Documentation')}

## Overview
{self._generate_description(context)}

## Constitutional Requirements
- TDD compliance enforced
- Module boundaries maintained
- Security standards applied

## Details
{context.get('content', 'Content description')}

## References
- [Constitutional Requirements](./constitution.md)
- [Testing Standards](./testing.md)
- [Security Guidelines](./security.md)"""

    def generate_llms_txt(self, context: Dict[str, Any]) -> str:
        """Generate llms.txt format documentation."""
        module_name = context.get("module_name", "Module")
        capabilities = context.get("capabilities", [])
        endpoints = context.get("endpoints", [])

        llms_txt = f"""# {module_name}

## Purpose
{context.get('purpose', 'Module purpose')}

## Capabilities"""

        for capability in capabilities:
            llms_txt += f"\n- {capability}"

        llms_txt += "\n\n## API Endpoints"
        for endpoint in endpoints:
            llms_txt += f"\n{endpoint['method']} {endpoint['path']} - {endpoint['description']}"

        llms_txt += """

## Constitutional Requirements
- TDD: Required (tests first)
- Tokens: Opaque only (no JWT)
- Communication: Events only
- Dependencies: Real only (no mocks)

## Usage Examples
```
""" + module_name.lower() + """ --help
""" + module_name.lower() + """ process --format json
```

## Integration Points
- Event Bus
- PostgreSQL
- Redis Session Store

## Security
- PCI DSS compliant
- OWASP validated
- Opaque tokens only"""

        return llms_txt

    def validate_documentation(self, documentation: str, context: Dict[str, Any]) -> DocumentationResult:
        """Validate documentation completeness and quality."""
        violations = []
        suggestions = []

        # Check for required elements
        if context.get("language") == "java" and "@param" not in documentation:
            violations.append("Missing @param tags in JavaDoc")

        if context.get("requires_constitutional") and "constitutional" not in documentation.lower():
            violations.append("Missing constitutional requirements documentation")

        # Check documentation coverage
        coverage = self._calculate_documentation_coverage(documentation, context.get("code", ""))

        if coverage < 80:
            suggestions.append(f"Increase documentation coverage from {coverage}% to at least 80%")

        # Validate freshness
        if self._is_documentation_stale(context):
            suggestions.append("Documentation is older than 7 days - update required")

        return DocumentationResult(
            success=len(violations) == 0,
            documentation=documentation,
            coverage=coverage,
            violations=violations,
            suggestions=suggestions,
            constitutional_compliant=len(violations) == 0
        )

    def _calculate_documentation_coverage(self, documentation: str, code: str) -> float:
        """Calculate documentation coverage percentage."""
        if not code:
            return 100.0

        # Simple heuristic: ratio of documentation to code
        doc_lines = len(documentation.split('\n'))
        code_lines = len(code.split('\n'))

        if code_lines == 0:
            return 100.0

        coverage = min((doc_lines / code_lines) * 100, 100.0)
        return round(coverage, 2)

    def _validate_constitutional_compliance(self, documentation: str, context: Dict[str, Any]) -> List[str]:
        """Validate documentation meets constitutional requirements."""
        violations = []

        # Check for CLI documentation in READMEs
        if context.get("type") == "readme":
            for flag in self.constitutional_requirements["library_cli_required"]:
                if flag not in documentation:
                    violations.append(f"Missing required CLI flag documentation: {flag}")

        # Check for TDD documentation
        if self.constitutional_requirements["tdd_documentation_required"]:
            if "tdd" not in documentation.lower() and "test" not in documentation.lower():
                violations.append("Missing TDD requirements documentation")

        # Check for security documentation
        if "jwt" in documentation.lower() and "opaque" not in documentation.lower():
            violations.append("JWT mentioned without clarifying opaque token requirement")

        return violations

    def _generate_improvement_suggestions(self, documentation: str, context: Dict[str, Any]) -> List[str]:
        """Generate suggestions for documentation improvement."""
        suggestions = []

        # Check for examples
        if "@example" not in documentation and "Example:" not in documentation:
            suggestions.append("Add usage examples")

        # Check for links/references
        if "@see" not in documentation and "[" not in documentation:
            suggestions.append("Add references to related documentation")

        # Check readability
        if self._calculate_readability_score(documentation) < 60:
            suggestions.append("Simplify language for better readability")

        # Check for version information
        if "@since" not in documentation and "version" not in documentation.lower():
            suggestions.append("Add version information")

        return suggestions

    def _calculate_readability_score(self, text: str) -> float:
        """Calculate readability score (simplified)."""
        # Simplified readability calculation
        words = text.split()
        sentences = text.count('.') + text.count('!') + text.count('?')

        if sentences == 0:
            return 0

        avg_words_per_sentence = len(words) / sentences

        # Lower average = better readability
        if avg_words_per_sentence < 15:
            return 90
        elif avg_words_per_sentence < 20:
            return 70
        elif avg_words_per_sentence < 25:
            return 50
        else:
            return 30

    def _is_documentation_stale(self, context: Dict[str, Any]) -> bool:
        """Check if documentation is stale."""
        last_updated = context.get("last_updated")

        if not last_updated:
            return False

        if isinstance(last_updated, str):
            last_updated = datetime.fromisoformat(last_updated)

        age = datetime.now() - last_updated
        return age > timedelta(days=7)

    def _generate_description(self, context: Dict[str, Any]) -> str:
        """Generate appropriate description based on context."""
        return context.get("description", "Processes request with constitutional compliance validation")

    def _javadoc_template(self) -> str:
        """JavaDoc template."""
        return """/**
 * {description}
 *
 * @param {param_name} {param_description}
 * @return {return_description}
 * @throws {exception} {exception_description}
 * @constitutional Required
 */"""

    def _jsdoc_template(self) -> str:
        """JSDoc template."""
        return """/**
 * {description}
 *
 * @param {{type}} {param_name} - {param_description}
 * @returns {{type}} {return_description}
 * @constitutional Opaque tokens only
 */"""

    def _python_docstring_template(self) -> str:
        """Python docstring template."""
        return """
    {description}

    Args:
        {param_name}: {param_description}

    Returns:
        {return_description}

    Constitutional Requirements:
        - {requirements}
    """

    def _openapi_template(self) -> str:
        """OpenAPI template."""
        return """openapi: 3.0.3
info:
  title: {title}
  version: {version}
paths:
  {endpoint}:
    {method}:
      security:
        - opaqueToken: []"""

    def _readme_template(self) -> str:
        """README template."""
        return """# {module_name}

## Constitutional Requirements
- TDD Required
- Opaque Tokens Only

## CLI Usage
```
{module} --help
{module} --version
{module} --format json
```"""


# Documentation Agent Instance
documentation_agent = DocumentationAgent()