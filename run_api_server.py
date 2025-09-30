#!/usr/bin/env python
"""Launch script for constitutional enforcement API server."""

import sys
import os
from src.api.constitutional_api import app

def run_server():
    """Start the API server with proper configuration."""
    port = int(os.environ.get('PORT', 8080))
    host = os.environ.get('HOST', 'localhost')
    
    app.run(host=host, port=port, debug=False)

if __name__ == '__main__':
    run_server()