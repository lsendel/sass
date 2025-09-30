"""Module Boundary Validator for validating module boundaries."""
from typing import Dict, Any

class ModuleBoundaryValidator:
    """Validates module boundaries and dependencies."""

    def validate_module_boundaries(self, feature_request: Dict[str, Any]) -> Dict[str, Any]:
        """Validate module boundaries for a feature request.

        Args:
            feature_request (dict): Feature request to validate

        Returns:
            dict: Validation results
        """
        # Check module dependencies
        dependencies = feature_request.get('module_dependencies', [])
        violations = []

        for dep in dependencies:
            source = dep.get('source')
            target = dep.get('target')
            comm_type = dep.get('type')

            if comm_type == 'direct_call':
                violations.append({
                    "type": "BOUNDARY_VIOLATION",
                    "severity": "HIGH",
                    "description": f"Direct call from {source} to {target} - use events instead"
                })

        return {
            "boundaries_respected": len(violations) == 0,
            "violations": violations,
            "required_actions": [
                "Replace direct calls with events",
                "Use event-driven communication",
                "Review module boundaries"
            ] if violations else []
        }