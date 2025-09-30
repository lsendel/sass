"""Implementation Gate for controlling implementation flow."""

class ImplementationGate:
    """Gate controlling implementation according to constitutional requirements."""

    def validate_implementation_attempt(self, context):
        """Validate implementation attempt against constitutional requirements.

        Args:
            context (dict): Implementation context

        Returns:
            dict: Validation results
        """
        implementation_files = context.get('implementation_files', [])
        test_files = context.get('test_files', [])
        has_tests = len(test_files) > 0
        blocking_reason = None if has_tests else "NO_TESTS_FOUND"

        return {
            "allowed": has_tests,
            "implementation_allowed": has_tests,
            "blocking_reasons": ["TDD_VIOLATION"] if not has_tests else [],
            "blocking_reason": blocking_reason,
            "constitutional_enforcement": True
        }

    def allow_implementation_with_failing_tests(self, context):
        """Allow implementation during RED phase.
        
        Args:
            context (dict): Implementation context
            
        Returns:
            dict: Implementation permission results
        """
        test_results = context.get('test_results', {})
        failing_tests = test_results.get('failed', 0)
        passing_tests = test_results.get('passed', 0)

        return {
            "implementation_allowed": failing_tests > 0 and passing_tests == 0,
            "tdd_phase": "RED_TO_GREEN",
            "constitutional_compliant": True
        }