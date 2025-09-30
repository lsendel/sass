"""Module Boundary Enforcer for maintaining clean module boundaries."""

class ModuleBoundaryEnforcer:
    """Enforcer responsible for maintaining clean module boundaries."""

    def get_defined_modules(self):
        """Get list of defined modules in the system.

        Returns:
            list: List of module names
        """
        return [
            "auth",
            "payment",
            "user",
            "subscription",
            "audit",
            "shared"
        ]

    def detect_cross_module_dependency(self, dependency_context):
        """Detect illegal cross-module dependencies.
        
        Args:
            dependency_context (dict): Dependency analysis context
            
        Returns:
            dict: Dependency violation results
        """
        source_file = dependency_context.get('source_file', '')
        import_statement = dependency_context.get('import_statement', '')
        source_module = dependency_context.get('source_module', '')
        target_module = dependency_context.get('target_module', '')

        direct_service_import = ('service' in import_statement.lower() and
                               source_module != target_module)

        return {
            "violation_detected": direct_service_import,
            "violation_type": "DIRECT_SERVICE_DEPENDENCY" if direct_service_import else None,
            "constitutional_violation": direct_service_import
        }

    def validate_event_communication(self, communication_context):
        """Validate proper event-driven communication between modules.

        Args:
            communication_context (dict): Communication validation context

        Returns:
            dict: Communication validation results
        """
        is_event = communication_context.get('communication_method') == 'event'
        event_type = communication_context.get('event_type', '')

        return {
            "communication_valid": is_event,
            "constitutional_compliant": is_event,
            "event_pattern_correct": is_event and event_type.endswith('Event')
        }

    def enforce_event_communication(self, communication_context):
        """Enforce event-driven communication between modules.

        Args:
            communication_context (dict): Communication context to enforce

        Returns:
            dict: Enforcement results
        """
        validation = self.validate_event_communication(communication_context)
        if not validation.get("communication_valid"):
            return {
                "enforced": True,
                "blocked": True,
                "reason": "Only event-driven communication is allowed between modules",
                "required_actions": ["Use event publishing", "Follow event-driven patterns"]
            }
        return {
            "enforced": True,
            "blocked": False,
            "compliant": True
        }

    def validate_no_cross_module_deps(self, dependency_context):
        """Validate that there are no illegal cross-module dependencies.

        Args:
            dependency_context (dict): Dependency analysis context

        Returns:
            dict: Validation results
        """
        violation_result = self.detect_cross_module_dependency(dependency_context)
        return {
            "valid": not violation_result.get("violation_detected"),
            "violations": [violation_result] if violation_result.get("violation_detected") else [],
            "constitutional_compliant": not violation_result.get("constitutional_violation")
        }

    def validate_module_communication(self, communication_context):
        """Validate module communication patterns.

        Args:
            communication_context (dict): Module communication context

        Returns:
            dict: Communication validation results
        """
        comm_type = communication_context.get('communication_type')
        event_type = communication_context.get('event_type')
        source_module = communication_context.get('source_module')
        target_module = communication_context.get('target_module')

        # Validate both source and target modules are defined
        defined_modules = self.get_defined_modules()
        violations = []
        
        if source_module not in defined_modules:
            violations.append({
                "type": "INVALID_MODULE",
                "severity": "HIGH",
                "description": f"Source module {source_module} is not defined"
            })
            
        if target_module not in defined_modules:
            violations.append({
                "type": "INVALID_MODULE",
                "severity": "HIGH",
                "description": f"Target module {target_module} is not defined"
            })
            
        if not event_type or not event_type.endswith('Event'):
            violations.append({
                "type": "INVALID_COMMUNICATION",
                "severity": "HIGH",
                "description": "Only event-driven communication is allowed between modules"
            })
            
        if comm_type == 'direct_service_call':
            violations.append({
                "type": "CONSTITUTIONAL_VIOLATION",
                "severity": "CRITICAL",
                "description": "Direct service calls between modules are forbidden by constitution"
            })

        # Event-driven communication is allowed between modules
        is_event_driven = comm_type == 'event' and event_type and event_type.endswith('Event')
        
        return {
            "allowed": is_event_driven and not violations,
            "mechanism": "EVENT_DRIVEN" if is_event_driven else None,
            "validation": {
                "source_valid": source_module in defined_modules,
                "target_valid": target_module in defined_modules,
                "event_pattern_valid": event_type.endswith('Event') if event_type else False
            },
            "violations": violations
        }