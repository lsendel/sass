#!/usr/bin/env python3
"""
Agent Configuration Schema Test - T007
Tests agent configuration schema validation and YAML frontmatter compliance.
MUST FAIL until proper agent configurations are implemented.
"""

import pytest
import yaml
import json
from pathlib import Path
from typing import Dict, Any


class TestAgentConfigSchema:
    """Test suite for validating agent configuration schemas."""

    def setup_method(self):
        """Setup test environment."""
        self.claude_dir = Path(".claude")
        self.agents_dir = self.claude_dir / "agents"
        self.templates_dir = self.claude_dir / "templates"

    def test_agent_template_exists(self):
        """Verify agent configuration template exists."""
        template_file = self.templates_dir / "agent-template.md"
        # Will fail until template is created
        assert template_file.exists(), "Agent configuration template must exist"

    def test_required_yaml_frontmatter_fields(self):
        """Verify all agents have required YAML frontmatter fields."""
        required_fields = {
            "name": str,
            "model": str,
            "description": str,
            "triggers": list,
            "tools": list,
            "context_files": list
        }

        agent_files = list(self.agents_dir.rglob("*.md"))
        # Will fail until agents are created
        assert len(agent_files) >= 4, "At least 4 critical path agents must exist"

        for agent_file in agent_files:
            content = agent_file.read_text()

            # Extract YAML frontmatter
            if not content.startswith("---"):
                pytest.fail(f"{agent_file.name} must start with YAML frontmatter")

            parts = content.split("---", 2)
            if len(parts) < 3:
                pytest.fail(f"{agent_file.name} must have properly closed YAML frontmatter")

            try:
                config = yaml.safe_load(parts[1])
            except yaml.YAMLError as e:
                pytest.fail(f"{agent_file.name} has invalid YAML: {e}")

            # Validate required fields
            for field, field_type in required_fields.items():
                assert field in config, f"{agent_file.name} missing required field: {field}"
                assert isinstance(config[field], field_type), f"{agent_file.name} field '{field}' must be {field_type.__name__}"

    def test_agent_model_validation(self):
        """Verify agents use valid Claude models."""
        valid_models = [
            "claude-sonnet",
            "claude-opus",
            "claude-haiku"
        ]

        agent_files = list(self.agents_dir.rglob("*.md"))
        for agent_file in agent_files:
            if agent_file.exists():
                content = agent_file.read_text()
                parts = content.split("---", 2)
                if len(parts) >= 3:
                    config = yaml.safe_load(parts[1])
                    if config and "model" in config:
                        model = config["model"]
                        # Will fail until valid models are specified
                        assert model in valid_models, f"{agent_file.name} must use valid Claude model: {valid_models}"

    def test_trigger_patterns_valid(self):
        """Verify agent trigger patterns are properly defined."""
        agent_files = list(self.agents_dir.rglob("*.md"))

        for agent_file in agent_files:
            if agent_file.exists():
                content = agent_file.read_text()
                parts = content.split("---", 2)
                if len(parts) >= 3:
                    config = yaml.safe_load(parts[1])
                    if config and "triggers" in config:
                        triggers = config["triggers"]

                        # Verify triggers is a list of strings
                        assert isinstance(triggers, list), f"{agent_file.name} triggers must be a list"
                        assert len(triggers) > 0, f"{agent_file.name} must have at least one trigger"

                        for trigger in triggers:
                            assert isinstance(trigger, str), f"{agent_file.name} triggers must be strings"
                            assert len(trigger.strip()) > 0, f"{agent_file.name} triggers cannot be empty"

    def test_tools_configuration_valid(self):
        """Verify agent tools configuration is valid."""
        valid_tools = [
            "Read", "Write", "Edit", "MultiEdit", "Bash", "Glob", "Grep",
            "Task", "WebSearch", "WebFetch", "TodoWrite",
            "mcp__playwright__*", "mcp__ide__*"
        ]

        agent_files = list(self.agents_dir.rglob("*.md"))

        for agent_file in agent_files:
            if agent_file.exists():
                content = agent_file.read_text()
                parts = content.split("---", 2)
                if len(parts) >= 3:
                    config = yaml.safe_load(parts[1])
                    if config and "tools" in config:
                        tools = config["tools"]

                        assert isinstance(tools, list), f"{agent_file.name} tools must be a list"
                        assert len(tools) > 0, f"{agent_file.name} must specify at least one tool"

                        for tool in tools:
                            assert isinstance(tool, str), f"{agent_file.name} tools must be strings"

                            # Check if tool is valid (exact match or wildcard pattern)
                            is_valid = (
                                tool in valid_tools or
                                any(tool.startswith(vt.rstrip("*")) for vt in valid_tools if vt.endswith("*"))
                            )
                            # Will fail until proper tools are configured
                            assert is_valid, f"{agent_file.name} uses invalid tool: {tool}"

    def test_context_files_reference_valid(self):
        """Verify agent context file references are valid."""
        agent_files = list(self.agents_dir.rglob("*.md"))

        for agent_file in agent_files:
            if agent_file.exists():
                content = agent_file.read_text()
                parts = content.split("---", 2)
                if len(parts) >= 3:
                    config = yaml.safe_load(parts[1])
                    if config and "context_files" in config:
                        context_files = config["context_files"]

                        assert isinstance(context_files, list), f"{agent_file.name} context_files must be a list"

                        for context_file in context_files:
                            assert isinstance(context_file, str), f"{agent_file.name} context files must be strings"

                            # Context files should reference .claude/context/ or project files
                            is_valid_reference = (
                                context_file.startswith(".claude/context/") or
                                context_file.startswith("src/") or
                                context_file.startswith("specs/") or
                                "*" in context_file  # Glob patterns allowed
                            )
                            # Will fail until proper context file references are added
                            assert is_valid_reference, f"{agent_file.name} has invalid context file reference: {context_file}"

    def test_agent_specialization_compliance(self):
        """Verify agents are properly specialized for their domains."""
        specialization_requirements = {
            "testing/tdd-compliance-agent.md": ["TDD", "test", "constitutional"],
            "architecture/spring-boot-modulith-architect.md": ["Spring Boot", "Modulith", "architecture"],
            "development/task-coordination-agent.md": ["Task", "coordination", "multi-agent"],
            "security/payment-security-agent.md": ["security", "payment", "PCI", "compliance"]
        }

        for agent_path, required_keywords in specialization_requirements.items():
            agent_file = self.agents_dir / agent_path
            if agent_file.exists():
                content = agent_file.read_text().lower()

                for keyword in required_keywords:
                    # Will fail until agents have proper specialization content
                    assert keyword.lower() in content, f"{agent_path} must contain keyword: {keyword}"

    def test_constitutional_enforcement_in_config(self):
        """Verify agent configurations enforce constitutional principles."""
        constitutional_principles = [
            "TDD Required (NON-NEGOTIABLE)",
            "Library-First Architecture",
            "Real Dependencies in Integration",
            "Observability Required",
            "Module Communication via Events"
        ]

        # Check TDD Compliance Agent specifically
        tdd_agent = self.agents_dir / "testing/tdd-compliance-agent.md"
        if tdd_agent.exists():
            content = tdd_agent.read_text()

            # Will fail until constitutional enforcement is implemented
            for principle in constitutional_principles[:3]:  # Check critical principles
                assert principle in content, f"TDD agent must reference constitutional principle: {principle}"

    def test_playwright_mcp_integration_config(self):
        """Verify UI/UX agents have Playwright MCP integration configured."""
        ui_ux_agents = list((self.agents_dir / "ui-ux").glob("*.md"))

        for agent_file in ui_ux_agents:
            if agent_file.exists():
                content = agent_file.read_text()
                parts = content.split("---", 2)
                if len(parts) >= 3:
                    config = yaml.safe_load(parts[1])
                    if config and "tools" in config:
                        tools = config["tools"]

                        # UI/UX agents should have Playwright MCP tools
                        has_playwright = any("playwright" in tool.lower() for tool in tools)
                        # Will fail until Playwright MCP integration is configured
                        assert has_playwright, f"{agent_file.name} must have Playwright MCP tools configured"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])