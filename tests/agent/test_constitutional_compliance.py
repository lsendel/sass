#!/usr/bin/env python3
"""
Constitutional Compliance Enforcement Test - T009
Tests constitutional compliance enforcement across all agents.
MUST FAIL until constitutional compliance is properly implemented.
"""

import pytest
import yaml
from pathlib import Path
from typing import Dict, List


class TestConstitutionalCompliance:
    """Test suite for validating constitutional compliance enforcement across agents."""

    def setup_method(self):
        """Setup test environment."""
        self.claude_dir = Path(".claude")
        self.agents_dir = self.claude_dir / "agents"
        self.context_dir = self.claude_dir / "context"

        # Constitutional principles from project constitution
        self.constitutional_principles = {
            "library_first_architecture": {
                "description": "Every feature must be a standalone library",
                "keywords": ["library", "standalone", "self-contained", "independent"],
                "enforcing_agents": ["architecture/spring-boot-modulith-architect.md"]
            },
            "tdd_required_non_negotiable": {
                "description": "TDD is mandatory with RED-GREEN-Refactor cycle",
                "keywords": ["TDD", "test-first", "RED-GREEN-Refactor", "test before implementation"],
                "enforcing_agents": ["testing/tdd-compliance-agent.md"],
                "forbidden_patterns": ["implementation before test", "skipping RED phase"]
            },
            "test_order_enforced": {
                "description": "Test order: Contract → Integration → E2E → Unit strictly enforced",
                "keywords": ["Contract", "Integration", "E2E", "Unit", "test hierarchy"],
                "enforcing_agents": [
                    "testing/contract-testing-agent.md",
                    "testing/integration-testing-agent.md",
                    "testing/playwright-e2e-agent.md"
                ]
            },
            "real_dependencies_in_integration": {
                "description": "Integration tests must use real dependencies",
                "keywords": ["TestContainers", "real dependencies", "no mocks in integration"],
                "enforcing_agents": ["testing/integration-testing-agent.md"]
            },
            "observability_required": {
                "description": "Structured logging and observability required",
                "keywords": ["structured logging", "observability", "monitoring", "metrics"],
                "enforcing_agents": ["devops/observability-agent.md"]
            },
            "module_communication_via_events": {
                "description": "Module communication must use events, not direct calls",
                "keywords": ["event-driven", "ApplicationEventPublisher", "module boundaries"],
                "enforcing_agents": ["architecture/spring-boot-modulith-architect.md"]
            },
            "opaque_tokens_only": {
                "description": "Only opaque tokens allowed, no custom JWT",
                "keywords": ["opaque tokens", "SHA-256", "no JWT", "token security"],
                "enforcing_agents": ["security/oauth-security-agent.md", "development/auth-module-agent.md"]
            },
            "gdpr_compliance": {
                "description": "GDPR compliance with PII redaction and retention policies",
                "keywords": ["GDPR", "PII redaction", "retention policies", "compliance"],
                "enforcing_agents": ["security/gdpr-compliance-agent.md"]
            }
        }

    def test_project_constitution_context_exists(self):
        """Verify project constitution context file exists."""
        constitution_file = self.context_dir / "project-constitution.md"

        # Will fail until constitution context is created
        assert constitution_file.exists(), "Project constitution context must exist"

        if constitution_file.exists():
            content = constitution_file.read_text()

            # Verify constitutional principles are documented
            for principle_key, principle_data in self.constitutional_principles.items():
                description = principle_data["description"]
                principle_name = principle_key.replace("_", " ").title()

                # Will fail until constitution is properly documented
                assert any(
                    keyword.lower() in content.lower()
                    for keyword in principle_data["keywords"][:2]  # Check first 2 keywords
                ), f"Constitution must document principle: {principle_name}"

    def test_constitutional_enforcement_agent_exists(self):
        """Verify Constitutional Enforcement Agent exists and is properly configured."""
        const_agent = self.agents_dir / "development/constitutional-enforcement-agent.md"

        # Will fail until Constitutional Enforcement Agent is created
        assert const_agent.exists(), "Constitutional Enforcement Agent must exist"

        if const_agent.exists():
            content = const_agent.read_text()

            # Verify YAML frontmatter
            assert content.startswith("---"), "Constitutional Enforcement Agent must have YAML frontmatter"

            # Verify constitutional principles are referenced
            for principle_key, principle_data in self.constitutional_principles.items():
                # Check for at least one keyword from each principle
                has_principle_reference = any(
                    keyword.lower() in content.lower()
                    for keyword in principle_data["keywords"][:1]  # Check primary keyword
                )
                assert has_principle_reference, f"Constitutional agent must reference: {principle_key}"

    def test_tdd_compliance_enforcement(self):
        """Verify TDD compliance is enforced across relevant agents."""
        tdd_agent = self.agents_dir / "testing/tdd-compliance-agent.md"

        # Will fail until TDD Compliance Agent is created
        assert tdd_agent.exists(), "TDD Compliance Agent must exist"

        if tdd_agent.exists():
            content = tdd_agent.read_text()

            # Verify TDD enforcement keywords
            tdd_keywords = ["TDD Required (NON-NEGOTIABLE)", "RED-GREEN-Refactor", "test-first"]
            for keyword in tdd_keywords:
                assert keyword in content, f"TDD agent must enforce: {keyword}"

            # Verify forbidden patterns are rejected
            forbidden_patterns = ["implementation before test", "skipping RED phase"]
            for pattern in forbidden_patterns:
                # Should reference these as forbidden
                assert "forbidden" in content.lower() or "reject" in content.lower(), \
                    "TDD agent must explicitly reject anti-patterns"

    def test_module_boundary_enforcement(self):
        """Verify module boundaries are enforced by Spring Boot Modulith Architect."""
        architect_agent = self.agents_dir / "architecture/spring-boot-modulith-architect.md"

        # Will fail until Spring Boot Modulith Architect is created
        assert architect_agent.exists(), "Spring Boot Modulith Architect must exist"

        if architect_agent.exists():
            content = content_lower = content.read_text().lower()

            # Verify module boundary enforcement
            boundary_keywords = [
                "module boundaries", "ArchUnit", "event-driven", "ApplicationEventPublisher"
            ]
            for keyword in boundary_keywords:
                assert keyword.lower() in content_lower, f"Architect agent must enforce: {keyword}"

            # Verify Spring Boot Modulith specific enforcement
            modulith_keywords = ["Spring Modulith", "modular monolith", "module isolation"]
            for keyword in modulith_keywords:
                assert keyword.lower() in content_lower, f"Architect agent must understand: {keyword}"

    def test_security_compliance_enforcement(self):
        """Verify security compliance is enforced across security agents."""
        security_principles = {
            "security/oauth-security-agent.md": ["OAuth2", "PKCE", "opaque tokens", "no JWT"],
            "security/payment-security-agent.md": ["PCI DSS", "payment security", "Stripe", "webhook"],
            "security/gdpr-compliance-agent.md": ["GDPR", "PII redaction", "retention", "compliance"]
        }

        for agent_path, required_keywords in security_principles.items():
            agent_file = self.agents_dir / agent_path
            if agent_file.exists():
                content = agent_file.read_text().lower()

                for keyword in required_keywords:
                    # Will fail until security agents properly enforce principles
                    assert keyword.lower() in content, f"{agent_path} must enforce: {keyword}"

    def test_testing_hierarchy_enforcement(self):
        """Verify testing hierarchy is enforced across testing agents."""
        testing_hierarchy = {
            "testing/contract-testing-agent.md": ["Contract", "API", "OpenAPI", "Pact"],
            "testing/integration-testing-agent.md": ["Integration", "TestContainers", "real dependencies"],
            "testing/playwright-e2e-agent.md": ["E2E", "end-to-end", "user journey", "Playwright"],
            "testing/tdd-compliance-agent.md": ["Unit", "JUnit", "Mockito", "test hierarchy"]
        }

        for agent_path, required_keywords in testing_hierarchy.items():
            agent_file = self.agents_dir / agent_path
            if agent_file.exists():
                content = agent_file.read_text().lower()

                for keyword in required_keywords[:2]:  # Check first 2 keywords
                    # Will fail until testing agents properly enforce hierarchy
                    assert keyword.lower() in content, f"{agent_path} must enforce: {keyword}"

    def test_observability_enforcement(self):
        """Verify observability requirements are enforced."""
        observability_agent = self.agents_dir / "devops/observability-agent.md"

        if observability_agent.exists():
            content = observability_agent.read_text().lower()

            # Verify observability enforcement
            observability_keywords = [
                "structured logging", "metrics", "monitoring", "alerting", "SLA", "SLO"
            ]
            for keyword in observability_keywords[:3]:  # Check first 3 keywords
                # Will fail until observability is properly enforced
                assert keyword.lower() in content, f"Observability agent must enforce: {keyword}"

    def test_constitutional_validation_in_workflows(self):
        """Verify workflows include constitutional compliance validation."""
        workflows_dir = self.claude_dir / "workflows"
        workflow_files = list(workflows_dir.glob("*.yml"))

        # Will fail until workflows are created
        assert len(workflow_files) > 0, "At least one workflow file must exist"

        for workflow_file in workflow_files:
            if workflow_file.exists():
                content = workflow_file.read_text()
                workflow_config = yaml.safe_load(content)

                # Check for constitutional compliance steps
                steps = workflow_config.get("steps", [])
                if steps:
                    # Look for constitutional compliance validation
                    has_compliance_check = any(
                        "constitutional" in str(step).lower() or
                        "compliance" in str(step).lower()
                        for step in steps
                    )
                    # Will fail until constitutional compliance is integrated into workflows
                    assert has_compliance_check, f"{workflow_file.name} must include constitutional compliance check"

    def test_agent_constitutional_self_enforcement(self):
        """Verify agents self-enforce constitutional principles in their domain."""
        domain_specific_enforcement = {
            "development/auth-module-agent.md": ["opaque tokens", "OAuth2", "security"],
            "development/payment-processing-agent.md": ["PCI compliance", "Stripe", "webhook security"],
            "ui-ux/accessibility-champion-agent.md": ["WCAG", "accessibility", "inclusive"],
            "devops/cicd-optimization-agent.md": ["pipeline", "security scanning", "automated"]
        }

        for agent_path, domain_keywords in domain_specific_enforcement.items():
            agent_file = self.agents_dir / agent_path
            if agent_file.exists():
                content = agent_file.read_text().lower()

                # Verify domain-specific constitutional enforcement
                for keyword in domain_keywords[:2]:  # Check first 2 keywords
                    # Will fail until agents properly self-enforce in their domain
                    assert keyword.lower() in content, f"{agent_path} must self-enforce: {keyword}"

    def test_constitutional_violation_detection(self):
        """Verify agents can detect constitutional violations."""
        const_agent = self.agents_dir / "development/constitutional-enforcement-agent.md"

        if const_agent.exists():
            content = content.read_text().lower()

            # Check for violation detection capabilities
            detection_keywords = [
                "violation", "detect", "validate", "enforce", "compliance check"
            ]
            for keyword in detection_keywords[:3]:  # Check first 3 keywords
                # Will fail until violation detection is implemented
                assert keyword.lower() in content, f"Constitutional agent must have {keyword} capability"

    def test_constitutional_compliance_gates(self):
        """Verify constitutional compliance gates are implemented."""
        # Check for compliance gates in context
        context_files = list(self.context_dir.glob("*.md"))

        has_compliance_context = any(
            "constitutional" in file.name.lower() or "compliance" in file.name.lower()
            for file in context_files
        )

        # Will fail until compliance context is created
        assert has_compliance_context, "Constitutional compliance context must exist"

        # Check for gates in workflows
        workflows_dir = self.claude_dir / "workflows"
        if workflows_dir.exists():
            workflow_files = list(workflows_dir.glob("*.yml"))
            for workflow_file in workflow_files:
                if workflow_file.exists():
                    content = workflow_file.read_text().lower()

                    # Look for gate keywords
                    gate_keywords = ["gate", "check", "validation", "compliance"]
                    has_gates = any(keyword in content for keyword in gate_keywords)

                    # Will fail until proper gates are implemented
                    assert has_gates, f"{workflow_file.name} must have constitutional compliance gates"

    def test_multi_agent_constitutional_coordination(self):
        """Verify multiple agents can coordinate constitutional enforcement."""
        # This test ensures agents work together to enforce constitutional principles

        # Check Task Coordination Agent exists
        task_coord_agent = self.agents_dir / "development/task-coordination-agent.md"
        const_agent = self.agents_dir / "development/constitutional-enforcement-agent.md"

        # Will fail until both coordination agents exist
        assert task_coord_agent.exists(), "Task Coordination Agent required for multi-agent enforcement"
        assert const_agent.exists(), "Constitutional Enforcement Agent required for coordination"

        if task_coord_agent.exists() and const_agent.exists():
            task_content = task_coord_agent.read_text().lower()
            const_content = const_agent.read_text().lower()

            # Verify coordination between agents
            coordination_keywords = ["coordinate", "collaborate", "enforce", "validate"]
            for keyword in coordination_keywords[:2]:  # Check first 2 keywords
                # At least one agent should reference coordination
                has_coordination = (
                    keyword in task_content or keyword in const_content
                )
                # Will fail until multi-agent coordination is implemented
                assert has_coordination, f"Agents must coordinate constitutional enforcement: {keyword}"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])