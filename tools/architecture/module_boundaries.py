#!/usr/bin/env python3
"""
Module Boundary Enforcer - Implementation
Enforces constitutional module boundaries and event-driven communication.

This enforcer ensures strict adherence to module isolation and prevents
direct cross-module dependencies as required by constitutional principles.
"""

from typing import Dict, List, Any, Optional
from enum import Enum
import logging
from dataclasses import dataclass


class CommunicationType(Enum):
    """Types of module communication."""
    EVENT = "event"
    DIRECT_SERVICE_CALL = "direct_service_call"
    SHARED_DATA = "shared_data"
    API_CALL = "api_call"


class ModuleType(Enum):
    """Defined module types."""
    AUTH = "auth"
    PAYMENT = "payment"
    USER = "user"
    SUBSCRIPTION = "subscription"
    AUDIT = "audit"
    SHARED = "shared"


@dataclass
class ModuleBoundaryViolation:
    """Represents a module boundary violation."""
    source_module: str
    target_module: str
    violation_type: str
    description: str
    constitutional_violation: bool


class ModuleBoundaryEnforcer:
    """
    Module Boundary Enforcer with constitutional compliance.

    Enforces strict module boundaries and event-driven communication.
    """

    def __init__(self):
        """Initialize Module Boundary Enforcer."""
        self.logger = logging.getLogger("module_boundary_enforcer")
        self.defined_modules = [module.value for module in ModuleType]
        self.allowed_communications = self._initialize_allowed_communications()

    def _initialize_allowed_communications(self) -> Dict[str, List[str]]:
        """Initialize allowed communication patterns."""
        return {
            "event": ["auth", "payment", "user", "subscription", "audit"],
            "shared": ["auth", "payment", "user", "subscription", "audit"]
        }

    def get_defined_modules(self) -> List[str]:
        """Return all defined modules."""
        return self.defined_modules

    def validate_no_cross_module_deps(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate no direct cross-module dependencies exist.

        Args:
            context: Validation context

        Returns:
            Validation result
        """
        source_file = context.get("source_file", "")
        imports = context.get("imports", [])

        violations = []

        for import_statement in imports:
            violation = self._check_import_violation(source_file, import_statement)
            if violation:
                violations.append(violation)

        return {
            "validation_passed": len(violations) == 0,
            "violations": [v.__dict__ for v in violations],
            "constitutional_compliant": len(violations) == 0
        }

    def _check_import_violation(self, source_file: str, import_statement: str) -> Optional[ModuleBoundaryViolation]:
        """Check if import statement violates module boundaries."""
        source_module = self._extract_module_from_file(source_file)
        target_module = self._extract_module_from_import(import_statement)

        if source_module != target_module and target_module != "shared":
            # Direct cross-module import detected
            return ModuleBoundaryViolation(
                source_module=source_module,
                target_module=target_module,
                violation_type="DIRECT_CROSS_MODULE_IMPORT",
                description=f"Direct import from {source_module} to {target_module}",
                constitutional_violation=True
            )

        return None

    def _extract_module_from_file(self, file_path: str) -> str:
        """Extract module name from file path."""
        for module in self.defined_modules:
            if f"/{module}/" in file_path or f"\\{module}\\" in file_path:
                return module
        return "unknown"

    def _extract_module_from_import(self, import_statement: str) -> str:
        """Extract target module from import statement."""
        for module in self.defined_modules:
            if f".{module}." in import_statement or f"/{module}/" in import_statement:
                return module
        return "unknown"

    def enforce_event_communication(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Enforce event-driven communication between modules.

        Args:
            context: Communication context

        Returns:
            Enforcement result
        """
        source_module = context.get("source_module")
        target_module = context.get("target_module")
        communication_type = context.get("communication_type")

        if communication_type == "event":
            return {
                "enforcement_successful": True,
                "communication_allowed": True,
                "constitutional_compliant": True
            }
        else:
            return {
                "enforcement_successful": False,
                "communication_allowed": False,
                "constitutional_violation": True,
                "required_communication": "event"
            }

    def detect_cross_module_dependency(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Detect cross-module dependency violations.

        Args:
            context: Detection context

        Returns:
            Detection result
        """
        source_file = context.get("source_file", "")
        import_statement = context.get("import_statement", "")
        source_module = context.get("source_module", "")
        target_module = context.get("target_module", "")

        # Check for direct service dependency
        is_service_import = "service" in import_statement.lower() or "Service" in import_statement

        if source_module != target_module and is_service_import:
            return {
                "violation_detected": True,
                "violation_type": "DIRECT_SERVICE_DEPENDENCY",
                "constitutional_violation": True,
                "source_module": source_module,
                "target_module": target_module
            }

        return {
            "violation_detected": False,
            "constitutional_compliant": True
        }

    def validate_event_communication(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate event-driven communication patterns.

        Args:
            context: Validation context

        Returns:
            Validation result
        """
        source_module = context.get("source_module")
        target_module = context.get("target_module")
        communication_method = context.get("communication_method")
        event_type = context.get("event_type", "")

        if communication_method == "event":
            # Validate event naming convention
            expected_suffix = "Event"
            event_pattern_correct = event_type.endswith(expected_suffix)

            return {
                "communication_valid": True,
                "constitutional_compliant": True,
                "event_pattern_correct": event_pattern_correct,
                "source_module": source_module,
                "target_module": target_module
            }

        return {
            "communication_valid": False,
            "constitutional_compliant": False,
            "required_method": "event"
        }

    def validate_module_communication(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Validate module communication against constitutional requirements.

        Args:
            context: Communication context

        Returns:
            Validation result
        """
        source_module = context.get("source_module")
        target_module = context.get("target_module")
        communication_type = context.get("communication_type")

        if communication_type == "event":
            return {
                "allowed": True,
                "mechanism": "EVENT_DRIVEN",
                "constitutional_compliant": True
            }
        elif communication_type == "direct_service_call":
            return {
                "allowed": False,
                "violations": ["CONSTITUTIONAL_VIOLATION"],
                "required_mechanism": "EVENT_DRIVEN",
                "constitutional_violation": True
            }

        return {
            "allowed": False,
            "reason": "UNKNOWN_COMMUNICATION_TYPE"
        }

    def analyze_module_dependencies(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Analyze module dependencies for constitutional compliance.

        Args:
            context: Analysis context

        Returns:
            Dependency analysis result
        """
        modules = context.get("modules", [])
        dependencies = context.get("dependencies", {})

        violations = []
        compliant_communications = []

        for source_module, targets in dependencies.items():
            for target_module, communication_info in targets.items():
                comm_type = communication_info.get("type", "unknown")

                if comm_type == "direct_import" and source_module != target_module:
                    violations.append({
                        "source": source_module,
                        "target": target_module,
                        "violation": "DIRECT_CROSS_MODULE_DEPENDENCY"
                    })
                elif comm_type == "event":
                    compliant_communications.append({
                        "source": source_module,
                        "target": target_module,
                        "type": "event"
                    })

        return {
            "analysis_complete": True,
            "violations": violations,
            "compliant_communications": compliant_communications,
            "constitutional_compliant": len(violations) == 0
        }

    def create_module_boundary_test(self, context: Dict[str, Any]) -> Dict[str, Any]:
        """
        Create ArchUnit-style module boundary test.

        Args:
            context: Test creation context

        Returns:
            Test creation result
        """
        modules = self.get_defined_modules()

        test_template = {
            "test_name": "module_boundary_enforcement",
            "test_type": "architectural",
            "framework": "archunit_python_equivalent",
            "rules": [
                {
                    "rule": "no_cross_module_imports",
                    "description": "Modules should not import directly from other modules",
                    "modules": modules
                },
                {
                    "rule": "event_driven_communication",
                    "description": "Modules should communicate via events only",
                    "modules": modules
                }
            ]
        }

        return {
            "test_created": True,
            "test_template": test_template,
            "constitutional_basis": "MODULE_COMMUNICATION_VIA_EVENTS"
        }


# Module Boundary Enforcer Instance
module_boundary_enforcer = ModuleBoundaryEnforcer()