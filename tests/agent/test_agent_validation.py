#!/usr/bin/env python3
"""
Agent Validation Test - T006
Tests agent configuration validation and basic functionality.
MUST FAIL until agent implementation is complete.
"""

import pytest
import os
import yaml
from pathlib import Path


class TestAgentValidation:
    """Test suite for validating agent configurations and functionality."""

    def setup_method(self):
        """Setup test environment."""
        self.claude_dir = Path(".claude")
        self.agents_dir = self.claude_dir / "agents"

    def test_claude_directory_exists(self):
        """Verify .claude directory structure exists."""
        assert self.claude_dir.exists(), ".claude directory must exist"
        assert self.agents_dir.exists(), ".claude/agents directory must exist"

        # Verify agent category directories
        categories = ["architecture", "development", "testing", "ui-ux", "security", "devops"]
        for category in categories:
            category_dir = self.agents_dir / category
            assert category_dir.exists(), f".claude/agents/{category} directory must exist"

    def test_agent_configuration_files_exist(self):
        """Verify critical agent configuration files exist."""
        # Critical path agents that must exist
        critical_agents = [
            "testing/tdd-compliance-agent.md",
            "architecture/spring-boot-modulith-architect.md",
            "development/task-coordination-agent.md",
            "development/constitutional-enforcement-agent.md"
        ]

        for agent_path in critical_agents:
            agent_file = self.agents_dir / agent_path
            assert agent_file.exists(), f"Critical agent {agent_path} must exist"

    def test_agent_yaml_frontmatter_valid(self):
        """Verify agent configurations have valid YAML frontmatter."""
        agent_files = list(self.agents_dir.rglob("*.md"))
        assert len(agent_files) > 0, "At least one agent configuration must exist"

        for agent_file in agent_files:
            content = agent_file.read_text()

            # Check for YAML frontmatter
            assert content.startswith("---"), f"{agent_file.name} must have YAML frontmatter"

            # Extract and validate YAML
            parts = content.split("---", 2)
            assert len(parts) >= 3, f"{agent_file.name} must have properly closed YAML frontmatter"

            try:
                yaml_config = yaml.safe_load(parts[1])
                assert yaml_config is not None, f"{agent_file.name} YAML frontmatter must be valid"

                # Verify required fields
                required_fields = ["name", "model", "description"]
                for field in required_fields:
                    assert field in yaml_config, f"{agent_file.name} must have '{field}' field"

            except yaml.YAMLError as e:
                pytest.fail(f"{agent_file.name} has invalid YAML frontmatter: {e}")

    def test_constitutional_compliance_enforcement(self):
        """Verify agents enforce constitutional principles."""
        # This test MUST FAIL until constitutional enforcement is implemented
        constitution_file = Path(".claude/context/project-constitution.md")

        # Will fail until context files are created
        assert constitution_file.exists(), "Project constitution context must exist"

        # Will fail until agents are implemented with constitutional enforcement
        tdd_agent = self.agents_dir / "testing/tdd-compliance-agent.md"
        if tdd_agent.exists():
            content = tdd_agent.read_text()
            assert "TDD Required (NON-NEGOTIABLE)" in content, "TDD agent must enforce constitutional TDD requirement"
            assert "RED-GREEN-Refactor" in content, "TDD agent must enforce proper TDD cycle"

    def test_task_tool_integration(self):
        """Verify agents are configured for Task tool integration."""
        # This test MUST FAIL until Task tool integration is implemented
        task_coord_agent = self.agents_dir / "development/task-coordination-agent.md"

        # Will fail until agent is created
        assert task_coord_agent.exists(), "Task Coordination Agent must exist for multi-agent workflows"

        if task_coord_agent.exists():
            content = task_coord_agent.read_text()
            # Will fail until proper Task tool integration is implemented
            assert "Task" in content, "Task Coordination Agent must reference Task tool"
            assert "multi-agent" in content.lower(), "Agent must support multi-agent coordination"

    def test_context_file_integration(self):
        """Verify agents can load context files."""
        # This test MUST FAIL until context management is implemented
        context_dir = Path(".claude/context")

        # Will fail until context files are created
        assert context_dir.exists(), "Context directory must exist"

        context_files = list(context_dir.glob("*.md"))
        assert len(context_files) > 0, "At least one context file must exist"

        # Will fail until agents reference context files
        agent_files = list(self.agents_dir.rglob("*.md"))
        for agent_file in agent_files:
            if agent_file.exists():
                content = agent_file.read_text()
                # Check if agent references context files
                has_context_ref = any(
                    "context" in content.lower() or
                    ".claude/context/" in content
                    for line in content.split('\n')
                )
                # This assertion will fail until context integration is implemented
                assert has_context_ref, f"{agent_file.name} must reference context files"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])