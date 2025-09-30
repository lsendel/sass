"""Event System for managing event-driven communication."""

class EventSystem:
    """System for managing event-driven communication between modules."""

    def __init__(self):
        self._subscribers = {}

    def publish(self, event_type, event_data):
        """Publish an event to all subscribers.
        
        Args:
            event_type (str): Type of event being published
            event_data (dict): Event payload
            
        Returns:
            bool: Success status
        """
        if event_type in self._subscribers:
            for subscriber in self._subscribers[event_type]:
                subscriber(event_data)
        return True

    def subscribe(self, event_type, handler):
        """Subscribe to an event type.
        
        Args:
            event_type (str): Type of event to subscribe to
            handler (callable): Function to handle the event
            
        Returns:
            bool: Success status
        """
        if event_type not in self._subscribers:
            self._subscribers[event_type] = []
        self._subscribers[event_type].append(handler)
        return True

    def validate_event_pattern(self, event_type):
        """Validate event pattern against constitutional requirements.
        
        Args:
            event_type (str): Event type to validate
            
        Returns:
            bool: Validation result
        """
        return event_type.endswith('Event')