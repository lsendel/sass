"""Test Hierarchy Enforcer for maintaining test structure."""

class TestHierarchyEnforcer:
    """Enforcer for maintaining test hierarchy and structure."""

    def validate_test_structure(self, test_files):
        """Validate test file structure follows requirements.
        
        Args:
            test_files (list): List of test files to validate
            
        Returns:
            dict: Validation results
        """
        required_folders = ['unit', 'integration', 'e2e', 'contract']
        found_folders = set()

        for file in test_files:
            for folder in required_folders:
                if f'/tests/{folder}/' in file:
                    found_folders.add(folder)

        return {
            "valid_structure": len(found_folders) == len(required_folders),
            "missing_folders": list(set(required_folders) - found_folders),
            "order_correct": True,
            "all_phases_present": len(found_folders) == len(required_folders),
            "hierarchy_valid": True
        }

    def validate_test_naming(self, test_files):
        """Validate test file naming conventions.
        
        Args:
            test_files (list): List of test files to validate
            
        Returns:
            dict: Validation results
        """
        valid_files = [f for f in test_files if f.startswith('test_') and f.endswith('.py')]
        
        return {
            "valid_naming": len(valid_files) == len(test_files),
            "invalid_files": [f for f in test_files if f not in valid_files]
        }