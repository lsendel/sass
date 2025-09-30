"""Module for checking deployment readiness based on constitutional principles."""

from typing import Dict, List
from src.agents.constitutional_enforcement import ConstitutionalEnforcementAgent


class ReadinessChecker:
    """Class for checking deployment readiness."""

    def __init__(self):
        """Initialize readiness checker."""
        self.const_agent = ConstitutionalEnforcementAgent()

    def check_readiness(self, deployment_data: Dict) -> Dict:
        """Check deployment readiness against constitutional requirements.
        
        Args:
            deployment_data (Dict): Data about the deployment to validate
            
        Returns:
            Dict: Readiness check results
        """
        # Check constitutional compliance
        const_result = self.const_agent.validate_constitutional_compliance(deployment_data)
        
        # Check security compliance
        security_result = self.const_agent.coordinate_security_validation(deployment_data)
        
        # Final validation
        final_result = self.const_agent.final_production_validation(deployment_data)
        
        return {
            "ready": all([
                const_result["compliant"],
                security_result["constitutional_compliant"],
                final_result["constitutional_compliant"]
            ]),
            "constitutional_validation": const_result,
            "security_validation": security_result,
            "final_validation": final_result,
            "required_actions": self._get_required_actions(const_result, security_result, final_result)
        }

    def _get_required_actions(self, const_result: Dict, security_result: Dict, final_result: Dict) -> List[str]:
        """Get list of required actions based on validation results.
        
        Args:
            const_result (Dict): Constitutional validation results
            security_result (Dict): Security validation results
            final_result (Dict): Final validation results
            
        Returns:
            List[str]: List of required actions
        """
        actions = []
        
        # Add actions from constitutional violations
        if not const_result["compliant"]:
            actions.extend(const_result.get("required_actions", []))
            
        # Add security actions
        if not security_result["constitutional_compliant"]:
            actions.append("Fix security constitutional violations")
            
        # Add final validation actions
        if not final_result["constitutional_compliant"]:
            actions.append("Complete constitutional validation")
            
        return actions