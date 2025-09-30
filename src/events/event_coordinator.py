"""Event Coordinator for managing event-driven communication."""

class EventCoordinator:
    """Coordinator for event-driven communication between modules."""

    def __init__(self):
        self._event_registry = {}
        self._event_handlers = {}

    def register_event(self, event_type, source_module):
        """Register a new event type.
        
        Args:
            event_type (str): Type of event to register
            source_module (str): Module that owns the event
            
        Returns:
            bool: Registration success
        """
        if not event_type.endswith('Event'):
            return False
            
        self._event_registry[event_type] = {
            'source_module': source_module,
            'handlers': []
        }
        return True

    def register_handler(self, event_type, handler_module, handler_fn):
        """Register an event handler.
        
        Args:
            event_type (str): Type of event to handle
            handler_module (str): Module containing the handler
            handler_fn (callable): Handler function
            
        Returns:
            bool: Registration success
        """
        if event_type not in self._event_registry:
            return False
            
        self._event_registry[event_type]['handlers'].append({
            'module': handler_module,
            'handler': handler_fn
        })
        return True

    def validate_event_flow(self, source_module, target_module, event_type):
        """Validate event flow between modules.
        
        Args:
            source_module (str): Source module
            target_module (str): Target module
            event_type (str): Type of event
            
        Returns:
            dict: Validation results
        """
        if event_type not in self._event_registry:
            return {
                'valid': False,
                'reason': 'EVENT_NOT_REGISTERED'
            }
            
        if self._event_registry[event_type]['source_module'] != source_module:
            return {
                'valid': False,
                'reason': 'INVALID_SOURCE_MODULE'
            }
            
        return {
            'valid': True,
            'event_type': event_type,
            'flow_validated': True
        }

    def get_registered_events(self):
        """Get list of registered events.

        Returns:
            list: Registered event types
        """
        return list(self._event_registry.keys())

    def publish_event(self, event_type, event_data):
        """Publish an event to all registered handlers.

        Args:
            event_type (str): Type of event to publish
            event_data (dict): Event payload

        Returns:
            dict: Publishing results
        """
        if event_type not in self._event_registry:
            return {
                "published": False,
                "reason": "Event type not registered"
            }

        handlers = self._event_registry[event_type].get('handlers', [])
        return {
            "published": True,
            "event_type": event_type,
            "handlers_notified": len(handlers)
        }

    def subscribe_to_event(self, event_type, handler):
        """Subscribe to an event.

        Args:
            event_type (str): Type of event to subscribe to
            handler (callable): Handler function

        Returns:
            dict: Subscription results
        """
        if event_type not in self._event_registry:
            self._event_registry[event_type] = {
                'source_module': 'unknown',
                'handlers': []
            }

        self._event_registry[event_type]['handlers'].append(handler)
        return {
            "subscribed": True,
            "event_type": event_type
        }

    def block_direct_service_calls(self, call_context):
        """Block direct service calls between modules.

        Args:
            call_context (dict): Service call context

        Returns:
            dict: Blocking results
        """
        is_direct_call = call_context.get('call_type') == 'direct'
        if is_direct_call:
            return {
                "blocked": True,
                "reason": "Direct service calls violate event-driven architecture",
                "required_actions": ["Use event publishing", "Implement event-driven pattern"]
            }
        return {
            "blocked": False,
            "compliant": True
        }