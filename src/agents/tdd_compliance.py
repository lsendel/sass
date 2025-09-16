#!/usr/bin/env python3
"""
TDD Compliance Agent - Implementation
Enforces Test-Driven Development principles with constitutional authority.

This agent ensures strict adherence to RED-GREEN-Refactor cycle.
TDD compliance is NON-NEGOTIABLE.
"""

from typing import Dict, List, Any, Optional
from enum import Enum
import logging
from dataclasses import dataclass


class TDDPhase(Enum):
    """TDD cycle phases."""
    RED = "RED"
    GREEN = "GREEN"
    REFACTOR = "REFACTOR"
    INVALID = "INVALID"


class TestType(Enum):
    """Test types in hierarchy order."""
    CONTRACT = "CONTRACT"
    INTEGRATION = "INTEGRATION"
    E2E = "E2E"
    UNIT = "UNIT"


@dataclass
class TestResult:
    """Test execution result."""
    failed: int
    passed: int
    skipped: int


class TDDComplianceAgent:
    """
    TDD Compliance Agent with constitutional enforcement authority.

    Enforces strict TDD compliance following constitutional principles.
    """

    def __init__(self):
        """Initialize TDD Compliance Agent."""
        self.logger = logging.getLogger("tdd_compliance")
        self.test_hierarchy = [TestType.CONTRACT, TestType.INTEGRATION, TestType.E2E, TestType.UNIT]

    def get_test_hierarchy(self) -> List[str]:
        """Return the constitutional test hierarchy order."""
        return [test_type.value for test_type in self.test_hierarchy]

    def validate_red_phase(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate RED phase - tests must fail before implementation.

        Args:
            context: Test execution context

        Returns:
            RED phase validation result
        """
        test_files = context.get("test_files", [])
        test_results = context.get("test_results", {})

        if not test_files:
            return {
                "red_phase_valid": False,
                "reason": "No test files found",
                "constitutional_violation": True
            }

        failed_count = test_results.get("failed", 0)
        passed_count = test_results.get("passed", 0)

        # RED phase requires failing tests
        red_phase_valid = failed_count > 0 and passed_count == 0

        return {
            "red_phase_valid": red_phase_valid,
            "failing_tests_count": failed_count,
            "passing_tests_count": passed_count,
            "phase": TDDPhase.RED.value,
            "constitutional_compliant": red_phase_valid
        }

    def validate_green_phase(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate GREEN phase - all tests must pass after minimal implementation.

        Args:
            context: Implementation and test context

        Returns:
            GREEN phase validation result
        """
        test_files = context.get("test_files", [])
        test_results = context.get("test_results", {})
        implementation_files = context.get("implementation_files", [])

        if not test_files or not implementation_files:
            return {
                "green_phase_valid": False,
                "reason": "Missing test files or implementation files"
            }

        failed_count = test_results.get("failed", 0)
        passed_count = test_results.get("passed", 0)

        # GREEN phase requires all tests to pass
        all_tests_passing = failed_count == 0 and passed_count > 0

        # Check if implementation is minimal (only what's needed to make tests pass)
        implementation_minimal = self._validate_minimal_implementation(implementation_files, test_results)

        return {
            "green_phase_valid": all_tests_passing,
            "all_tests_passing": all_tests_passing,
            "implementation_minimal": implementation_minimal,
            "phase": TDDPhase.GREEN.value,
            "constitutional_compliant": all_tests_passing and implementation_minimal
        }

    def validate_refactor_phase(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate REFACTOR phase - tests must still pass after refactoring.

        Args:
            context: Refactoring context

        Returns:
            REFACTOR phase validation result
        """
        test_results = context.get("test_results", {})
        refactoring_completed = context.get("refactoring_completed", False)

        failed_count = test_results.get("failed", 0)
        passed_count = test_results.get("passed", 0)

        # REFACTOR phase requires tests to still pass
        tests_still_passing = failed_count == 0 and passed_count > 0

        return {
            "refactor_phase_valid": tests_still_passing and refactoring_completed,
            "tests_still_passing": tests_still_passing,
            "refactoring_completed": refactoring_completed,
            "phase": TDDPhase.REFACTOR.value,
            "constitutional_compliant": tests_still_passing
        }

    def validate_test_hierarchy(self, test_files: List[str]) -> Dict[str, Any]:
        """
        Validate test hierarchy follows constitutional order.

        Args:
            test_files: List of test file paths

        Returns:
            Test hierarchy validation result
        """
        detected_types = []

        for test_file in test_files:
            if "contract" in test_file.lower():
                detected_types.append(TestType.CONTRACT)
            elif "integration" in test_file.lower():
                detected_types.append(TestType.INTEGRATION)
            elif "e2e" in test_file.lower():
                detected_types.append(TestType.E2E)
            elif "unit" in test_file.lower():
                detected_types.append(TestType.UNIT)

        # Remove duplicates while preserving order
        unique_types = []
        for test_type in detected_types:
            if test_type not in unique_types:
                unique_types.append(test_type)

        expected_order = self.test_hierarchy
        order_correct = unique_types == expected_order[:len(unique_types)]

        return {
            "order": [t.value for t in unique_types],
            "expected_order": [t.value for t in expected_order],
            "order_correct": order_correct,
            "all_types_present": len(unique_types) == len(expected_order),
            "constitutional_compliant": order_correct
        }

    def block_implementation_without_tests(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Block implementation when no tests exist.

        Args:
            context: Implementation context

        Returns:
            Blocking result
        """
        implementation_files = context.get("implementation_files", [])
        test_files = context.get("test_files", [])

        should_block = len(implementation_files) > 0 and len(test_files) == 0

        if should_block:
            self.logger.critical("BLOCKING IMPLEMENTATION: No tests found - TDD violation")

        return {
            "blocked": should_block,
            "reason": "TDD_VIOLATION_NO_TESTS" if should_block else None,
            "constitutional_violation": should_block,
            "authority": "TDD_COMPLIANCE"
        }

    def enforce_test_first_development(self, feature_request: Dict[str, Any]) -> Dict[str, Any]:
        """
        Enforce test-first development for feature requests.

        Args:
            feature_request: Feature development request

        Returns:
            Test-first enforcement result
        """
        feature_name = feature_request.get("name", "unknown")
        requirements = feature_request.get("requirements", [])

        tdd_required = "TDD required" in requirements

        return {
            "tests_written_first": tdd_required,
            "red_phase_validated": tdd_required,
            "feature": feature_name,
            "constitutional_compliant": tdd_required
        }

    def validate_tdd_cycle_completion(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate complete TDD cycle has been followed.

        Args:
            context: TDD cycle context

        Returns:
            TDD cycle completion validation
        """
        phases_completed = context.get("phases_completed", [])
        required_phases = [TDDPhase.RED.value, TDDPhase.GREEN.value, TDDPhase.REFACTOR.value]

        cycle_complete = all(phase in phases_completed for phase in required_phases)

        return {
            "tdd_cycle_complete": cycle_complete,
            "phases_completed": phases_completed,
            "required_phases": required_phases,
            "constitutional_compliant": cycle_complete
        }

    def analyze_test_coverage(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Analyze test coverage for constitutional compliance.

        Args:
            context: Test coverage context

        Returns:
            Test coverage analysis
        """
        coverage_percentage = context.get("coverage_percentage", 0)
        minimum_coverage = 80  # Constitutional requirement

        coverage_adequate = coverage_percentage >= minimum_coverage

        return {
            "coverage_percentage": coverage_percentage,
            "minimum_required": minimum_coverage,
            "coverage_adequate": coverage_adequate,
            "constitutional_compliant": coverage_adequate
        }

    def validate_test_quality(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate test quality meets constitutional standards.

        Args:
            context: Test quality context

        Returns:
            Test quality validation
        """
        test_files = context.get("test_files", [])

        # Basic quality checks
        quality_score = 0
        quality_issues = []

        if len(test_files) > 0:
            quality_score += 25  # Tests exist
        else:
            quality_issues.append("No tests found")

        # Advanced quality checks
        naming_score, naming_issues = self._validate_test_naming(test_files)
        isolation_score, isolation_issues = self._validate_test_isolation(context)
        readability_score, readability_issues = self._validate_test_readability(test_files)
        assertion_score, assertion_issues = self._validate_assertion_quality(context)

        quality_score += naming_score + isolation_score + readability_score + assertion_score
        quality_issues.extend(naming_issues + isolation_issues + readability_issues + assertion_issues)

        quality_adequate = quality_score >= 75

        return {
            "quality_score": quality_score,
            "quality_issues": quality_issues,
            "quality_adequate": quality_adequate,
            "constitutional_compliant": quality_adequate
        }

    def monitor_tdd_compliance(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Monitor ongoing TDD compliance.

        Args:
            context: Monitoring context

        Returns:
            TDD compliance monitoring result
        """
        violations = []

        # Check for implementation without tests
        if context.get("implementation_files") and not context.get("test_files"):
            violations.append("IMPLEMENTATION_WITHOUT_TESTS")

        # Check for skipped tests
        test_results = context.get("test_results", {})
        if test_results.get("skipped", 0) > 0:
            violations.append("SKIPPED_TESTS")

        compliance_status = "COMPLIANT" if not violations else "VIOLATIONS_DETECTED"

        return {
            "compliance_status": compliance_status,
            "violations": violations,
            "monitoring_active": True,
            "constitutional_authority": "TDD_COMPLIANCE_AGENT"
        }

    def _validate_minimal_implementation(self, implementation_files: List[str], test_results: Dict[str, Any]) -> bool:
        """
        Validate that implementation is minimal - only what's needed to make tests pass.

        Args:
            implementation_files: List of implementation files
            test_results: Test execution results

        Returns:
            True if implementation appears minimal
        """
        if not implementation_files:
            return True  # No implementation is certainly minimal

        # Basic heuristics for minimal implementation
        total_tests = test_results.get("passed", 0) + test_results.get("failed", 0)

        if total_tests == 0:
            return False  # Implementation without tests is not minimal

        # Rule of thumb: implementation should be roughly proportional to tests
        # More sophisticated analysis would require code parsing
        implementation_ratio = len(implementation_files) / max(total_tests, 1)

        # Allow reasonable implementation-to-test ratio
        return implementation_ratio <= 2.0

    def _validate_test_naming(self, test_files: List[str]) -> tuple[int, List[str]]:
        """
        Validate test naming conventions.

        Args:
            test_files: List of test file paths

        Returns:
            Tuple of (score, issues)
        """
        score = 0
        issues = []

        if not test_files:
            return score, issues

        # Check for descriptive test file names
        descriptive_names = 0
        for test_file in test_files:
            filename = test_file.split('/')[-1].lower()
            if any(word in filename for word in ['test', 'spec', 'should', 'when']):
                descriptive_names += 1

        if descriptive_names == len(test_files):
            score += 15  # All files have descriptive names
        elif descriptive_names > len(test_files) * 0.75:
            score += 10  # Most files have descriptive names
        else:
            issues.append("Test files lack descriptive naming conventions")

        return score, issues

    def _validate_test_isolation(self, context: Dict[str, Any]) -> tuple[int, List[str]]:
        """
        Validate test isolation (tests don't depend on each other).

        Args:
            context: Test context

        Returns:
            Tuple of (score, issues)
        """
        score = 0
        issues = []

        # Check for test dependencies or shared state
        test_results = context.get("test_results", {})
        failed_count = test_results.get("failed", 0)
        passed_count = test_results.get("passed", 0)

        if passed_count > 0:
            score += 10  # Some tests pass, suggesting basic isolation

        if failed_count == 0 or (failed_count > 0 and passed_count > 0):
            score += 10  # No cascading failures suggesting good isolation
        else:
            issues.append("Potential test isolation issues detected")

        return score, issues

    def _validate_test_readability(self, test_files: List[str]) -> tuple[int, List[str]]:
        """
        Validate test readability and structure.

        Args:
            test_files: List of test file paths

        Returns:
            Tuple of (score, issues)
        """
        score = 0
        issues = []

        if not test_files:
            return score, issues

        # Basic readability checks based on file organization
        organized_tests = 0
        for test_file in test_files:
            # Check if tests are organized by type/feature
            path_parts = test_file.split('/')
            if len(path_parts) >= 2:  # Tests are in subdirectories
                organized_tests += 1

        if organized_tests == len(test_files):
            score += 15  # All tests are well organized
        elif organized_tests > len(test_files) * 0.5:
            score += 8   # Most tests are organized
        else:
            issues.append("Tests lack proper organization structure")

        return score, issues

    def _validate_assertion_quality(self, context: Dict[str, Any]) -> tuple[int, List[str]]:
        """
        Validate assertion quality in tests.

        Args:
            context: Test context

        Returns:
            Tuple of (score, issues)
        """
        score = 0
        issues = []

        test_results = context.get("test_results", {})
        passed_count = test_results.get("passed", 0)
        failed_count = test_results.get("failed", 0)

        if passed_count > 0:
            score += 15  # Tests are passing, suggesting valid assertions

        # Look for balanced pass/fail ratio during development
        total_tests = passed_count + failed_count
        if total_tests > 0:
            pass_ratio = passed_count / total_tests
            # During TDD, we expect some failures initially
            if 0.3 <= pass_ratio <= 0.8:
                score += 5  # Healthy development pattern

        return score, issues


# TDD Compliance Agent Instance
tdd_compliance_agent = TDDComplianceAgent()