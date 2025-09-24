#!/usr/bin/env python3
"""
Implementation Gate - Constitutional Compliance Gate
Blocks implementation that violates constitutional principles.

This gate ensures no code can be implemented without proper constitutional compliance.
"""

from typing import Dict, List, Any
import logging


class ImplementationGate:
    """
    Implementation Gate with constitutional authority.

    Blocks non-compliant implementation attempts.
    """

    def __init__(self):
        """Initialize Implementation Gate."""
        self.logger = logging.getLogger("implementation_gate")

    def validate_implementation_attempt(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate implementation attempt against constitutional requirements.

        Args:
            context: Implementation context

        Returns:
            Validation result
        """
        implementation_files = context.get("implementation_files", [])
        test_files = context.get("test_files", [])
        module = context.get("module", "unknown")

        # Constitutional requirement: No implementation without tests
        has_implementation = len(implementation_files) > 0
        has_tests = len(test_files) > 0

        if has_implementation and not has_tests:
            self.logger.critical(f"BLOCKING IMPLEMENTATION: No tests found for module {module}")
            return {
                "implementation_allowed": False,
                "blocking_reason": "NO_TESTS_FOUND",
                "constitutional_enforcement": True,
                "module": module
            }

        return {
            "implementation_allowed": True,
            "constitutional_compliant": True,
            "module": module
        }

    def validate_implementation_readiness(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate implementation readiness with comprehensive checks.

        Args:
            context: Implementation readiness context

        Returns:
            Readiness validation result
        """
        implementation_files = context.get("implementation_files", [])
        test_files = context.get("test_files", [])
        module = context.get("module", "unknown")
        feature = context.get("feature", "unknown")

        blocking_reasons = []

        # Check for tests
        if implementation_files and not test_files:
            blocking_reasons.append("TDD_VIOLATION")

        # Check for proper test hierarchy
        test_results = context.get("test_results", {})
        if test_files and test_results.get("failed", 0) == 0:
            # Implementation should only happen when tests are failing (RED phase)
            # Unless we're in GREEN phase where tests should pass
            pass  # More sophisticated logic needed here

        implementation_allowed = len(blocking_reasons) == 0

        return {
            "allowed": implementation_allowed,
            "blocking_reasons": blocking_reasons,
            "constitutional_enforcement": not implementation_allowed,
            "module": module,
            "feature": feature
        }

    def block_implementation_without_tests(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """Block implementation when no tests exist."""
        return self.validate_implementation_attempt(context)

    def allow_implementation_with_failing_tests(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """Allow implementation when failing tests exist (RED phase)."""
        implementation_files = context.get("implementation_files", [])
        test_files = context.get("test_files", [])
        test_results = context.get("test_results", {})
        module = context.get("module", "unknown")

        has_implementation = len(implementation_files) > 0
        has_tests = len(test_files) > 0
        has_failing_tests = test_results.get("failed", 0) > 0
        has_passing_tests = test_results.get("passed", 0) > 0

        if has_tests and has_failing_tests and not has_passing_tests:
            # Perfect RED phase - failing tests, ready for implementation
            return {
                "implementation_allowed": True,
                "tdd_phase": "RED_TO_GREEN",
                "constitutional_compliant": True,
                "module": module
            }

        return {
            "implementation_allowed": False,
            "reason": "NOT_IN_PROPER_TDD_PHASE",
            "tdd_phase": "INVALID",
            "constitutional_compliant": False,
            "module": module
        }


# Implementation Gate Instance
implementation_gate = ImplementationGate()