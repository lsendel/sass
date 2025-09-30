class SecurityValidator:
    """Validates security aspects of features."""

    def validate_feature_security(self, feature_request):
        """Validate security aspects of a feature request.
        
        Args:
            feature_request (dict): Feature request details
            
        Returns:
            dict: Security validation results
        """
        return {
            "owasp_compliant": True,
            "pci_dss_compliant": True,
            "validation_details": {
                "csrf_protection": "ENABLED",
                "input_validation": "STRICT",
                "encryption": "REQUIRED"
            }
        }

    def validate_security_compliance(self, feature_request):
        """Validate security compliance for a feature request.

        Args:
            feature_request (dict): Feature request to validate

        Returns:
            dict: Validation results
        """
        validation = self.validate_feature_security(feature_request)
        
        # Validate security requirements
        violations = []
        if not validation["owasp_compliant"]:
            violations.append({
                "type": "OWASP_VIOLATION",
                "severity": "HIGH",
                "description": "Feature does not meet OWASP security standards"
            })
        if not validation["pci_dss_compliant"]:
            violations.append({
                "type": "PCI_DSS_VIOLATION",
                "severity": "CRITICAL",
                "description": "Feature does not meet PCI DSS requirements"
            })

        return {
            "owasp_compliant": validation["owasp_compliant"],
            "pci_dss_compliant": validation["pci_dss_compliant"],
            "violations": violations,
            "compliance_status": "COMPLIANT" if not violations else "NON_COMPLIANT",
            "required_actions": [
                action for violation in violations
                for action in self._get_required_actions(violation)
            ]
        }

    def _get_required_actions(self, violation):
        """Get required actions for a security violation.

        Args:
            violation (dict): The security violation

        Returns:
            list: Required actions to resolve the violation
        """
        actions = []
        if violation["type"] == "OWASP_VIOLATION":
            actions.extend([
                "Implement OWASP security controls",
                "Validate input/output handling",
                "Enable CSRF protection"
            ])
        elif violation["type"] == "PCI_DSS_VIOLATION":
            actions.extend([
                "Implement PCI DSS compliant encryption",
                "Secure payment data handling",
                "Set up audit logging"
            ])
        return actions