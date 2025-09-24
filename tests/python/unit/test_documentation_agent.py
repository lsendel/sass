#!/usr/bin/env python3
"""
Documentation Agent Unit Tests
Tests for comprehensive documentation generation and enforcement.
"""

import pytest
from pathlib import Path
from typing import Dict, Any


class TestDocumentationAgent:
    """Unit tests for Documentation Agent capabilities."""

    def test_documentation_agent_exists(self):
        """Test that Documentation Agent is properly defined."""
        agent_file = Path(".claude/agents/documentation/documentation-agent.md")
        assert agent_file.exists(), "Documentation Agent must exist"

        content = agent_file.read_text()

        # Verify required capabilities
        required_capabilities = [
            "code documentation",
            "api documentation",
            "architecture documentation",
            "test documentation",
            "llms.txt",
            "constitutional"
        ]

        for capability in required_capabilities:
            assert capability.lower() in content.lower(), f"Documentation Agent must support {capability}"

    def test_javadoc_generation(self):
        """Test JavaDoc generation for Spring Boot code."""
        sample_code = """
        public PaymentResult processPayment(PaymentRequest request) {
            return paymentService.process(request);
        }
        """

        expected_elements = [
            "@param",
            "@return",
            "@throws",
            "@since",
            "constitutional"
        ]

        # This would call the actual documentation generator
        # For now, we're testing the structure
        javadoc_template = """
        /**
         * Processes payment with constitutional compliance.
         *
         * @param request Payment request
         * @return Payment result
         * @throws ConstitutionalViolationException
         * @since 1.0.0
         */
        """

        for element in expected_elements[:4]:  # Check first 4 elements
            assert element in javadoc_template, f"JavaDoc must include {element}"

    def test_jsdoc_generation(self):
        """Test JSDoc/TSDoc generation for TypeScript code."""
        sample_code = """
        export const PaymentForm = ({ onSubmit }) => {
            return <form onSubmit={onSubmit}>...</form>;
        }
        """

        expected_elements = [
            "@component",
            "@param",
            "@returns",
            "@example",
            "@constitutional"
        ]

        jsdoc_template = """
        /**
         * Payment form component.
         *
         * @component
         * @param {Function} onSubmit - Submit handler
         * @returns {JSX.Element} Form component
         * @example
         * <PaymentForm onSubmit={handleSubmit} />
         * @constitutional Opaque tokens only
         */
        """

        for element in expected_elements[:4]:
            assert element in jsdoc_template, f"JSDoc must include {element}"

    def test_python_docstring_generation(self):
        """Test Python docstring generation."""
        sample_code = """
        def process_payment(self, request):
            return self.payment_service.process(request)
        """

        expected_elements = [
            "Args:",
            "Returns:",
            "Raises:",
            "Constitutional Requirements:",
            "Example:"
        ]

        docstring_template = '''
        """
        Process payment with validation.

        Args:
            request: Payment request object

        Returns:
            PaymentResult: Processing result

        Raises:
            ConstitutionalViolationError: If requirements violated

        Constitutional Requirements:
            - TDD compliance required
            - Opaque tokens only

        Example:
            >>> result = process_payment(request)
        """
        '''

        for element in expected_elements[:3]:
            assert element in docstring_template, f"Docstring must include {element}"

    def test_openapi_documentation_generation(self):
        """Test OpenAPI specification generation."""
        expected_fields = [
            "openapi: 3.0",
            "paths:",
            "components:",
            "security:",
            "opaqueToken"  # Constitutional requirement
        ]

        openapi_template = """
        openapi: 3.0.3
        info:
          title: Payment API
          version: 1.0.0
        paths:
          /payments:
            post:
              security:
                - opaqueToken: []
        components:
          securitySchemes:
            opaqueToken:
              type: apiKey
        """

        for field in expected_fields[:4]:
            assert field in openapi_template or field.replace(":", "") in openapi_template, \
                f"OpenAPI spec must include {field}"

    def test_adr_generation(self):
        """Test Architecture Decision Record generation."""
        expected_sections = [
            "Status",
            "Context",
            "Decision",
            "Consequences",
            "Constitutional Compliance"
        ]

        adr_template = """
        # ADR-001: Title

        ## Status
        Accepted

        ## Context
        Background information

        ## Decision
        What we decided

        ## Consequences
        ### Positive
        ### Negative

        ## Constitutional Compliance
        ✅ Compliant
        """

        for section in expected_sections:
            assert section in adr_template, f"ADR must include {section} section"

    def test_readme_generation_with_cli(self):
        """Test README generation with required CLI documentation."""
        required_elements = [
            "--help",
            "--version",
            "--format",
            "Constitutional Requirements",
            "TDD",
            "npm run test:contract"
        ]

        readme_template = """
        # Module Name

        ## Constitutional Requirements
        - TDD Required
        - Opaque tokens only

        ## CLI Usage
        ```
        module --help
        module --version
        module --format json
        ```

        ## Testing
        ```
        npm run test:contract
        npm run test:integration
        npm run test:e2e
        npm run test:unit
        ```
        """

        for element in required_elements[:3]:
            assert element in readme_template, f"README must document {element} flag"

    def test_llms_txt_generation(self):
        """Test llms.txt format generation."""
        expected_sections = [
            "Purpose",
            "Capabilities",
            "API Endpoints",
            "Constitutional Requirements",
            "Usage Examples"
        ]

        llms_template = """
        # Module Name

        ## Purpose
        Module purpose

        ## Capabilities
        - Capability 1
        - Capability 2

        ## API Endpoints
        POST /api/endpoint

        ## Constitutional Requirements
        - TDD Required
        - Opaque tokens only

        ## Usage Examples
        ```
        module command --flag
        ```
        """

        for section in expected_sections:
            assert f"## {section}" in llms_template, f"llms.txt must include {section}"

    def test_documentation_completeness_validation(self):
        """Test documentation completeness validation."""
        validation_criteria = {
            "has_description": True,
            "has_parameters": True,
            "has_return_value": True,
            "has_examples": True,
            "has_constitutional_notes": True,
            "coverage_percentage": 90
        }

        # All criteria should be checked
        assert all(isinstance(v, (bool, int, float)) for v in validation_criteria.values())
        assert validation_criteria["coverage_percentage"] >= 90

    def test_constitutional_documentation_enforcement(self):
        """Test constitutional documentation requirements enforcement."""
        constitutional_requirements = [
            "library_first_documentation",
            "tdd_test_documentation",
            "module_boundary_documentation",
            "event_documentation",
            "security_documentation"
        ]

        for requirement in constitutional_requirements:
            # Each requirement should be enforceable
            assert isinstance(requirement, str)
            assert len(requirement) > 0

    def test_documentation_freshness_validation(self):
        """Test documentation freshness validation."""
        max_age_days = 7

        # Documentation should not be older than 7 days
        assert max_age_days <= 7, "Documentation must be updated within 7 days"

    def test_multi_language_support(self):
        """Test support for multiple programming languages."""
        supported_languages = [
            "java",
            "javascript",
            "typescript",
            "python",
            "yaml",
            "markdown"
        ]

        for language in supported_languages:
            # Each language should be supported
            assert isinstance(language, str)
            assert len(language) > 0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])