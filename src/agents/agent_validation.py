"""Agent Validation for ensuring agent integrity."""

import yaml

def validate_agent_config(config):
    """Validate agent configuration against schema.
    
    Args:
        config (dict): Agent configuration to validate
        
    Returns:
        dict: Validation results
    """
    try:
        required_sections = ['version', 'agents', 'principles']
        for section in required_sections:
            if section not in config:
                return {
                    'valid': False,
                    'error': f'Missing required section: {section}'
                }

        # Validate agents
        required_agent_fields = ['enabled', 'authority_level']
        for agent, settings in config['agents'].items():
            for field in required_agent_fields:
                if field not in settings:
                    return {
                        'valid': False,
                        'error': f'Agent {agent} missing required field: {field}'
                    }

        # Validate principles
        required_principle_fields = ['enforced', 'severity']
        for principle, settings in config['principles'].items():
            for field in required_principle_fields:
                if field not in settings:
                    return {
                        'valid': False,
                        'error': f'Principle {principle} missing required field: {field}'
                    }

        return {
            'valid': True,
            'version': config['version']
        }
        
    except Exception as e:
        return {
            'valid': False,
            'error': str(e)
        }