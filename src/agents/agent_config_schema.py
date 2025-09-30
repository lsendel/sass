"""Constitutional Agent Configuration Schema."""

import yaml

SCHEMA = {
    "version": 1,
    "agents": {
        "constitutional_enforcement": {
            "enabled": True,
            "authority_level": "SUPREME",
            "override_capability": True
        },
        "tdd_compliance": {
            "enabled": True,
            "authority_level": "HIGH",
            "phases": ["RED", "GREEN", "REFACTOR"]
        },
        "module_boundaries": {
            "enabled": True,
            "authority_level": "HIGH",
            "communication": "EVENT_DRIVEN"
        },
        "security_compliance": {
            "enabled": True,
            "authority_level": "HIGH",
            "standards": ["OWASP", "PCI_DSS"]
        }
    },
    "principles": {
        "LIBRARY_FIRST_ARCHITECTURE": {
            "enforced": True,
            "severity": "HIGH"
        },
        "TDD_REQUIRED_NON_NEGOTIABLE": {
            "enforced": True,
            "severity": "CRITICAL"
        },
        "TEST_ORDER_HIERARCHY": {
            "enforced": True,
            "severity": "HIGH"
        },
        "MODULE_COMMUNICATION_VIA_EVENTS": {
            "enforced": True,
            "severity": "HIGH"
        },
        "OPAQUE_TOKENS_ONLY": {
            "enforced": True,
            "severity": "CRITICAL"
        }
    }
}