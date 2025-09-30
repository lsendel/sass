"""Test fixtures for API tests."""

import pytest
import threading
import time
import requests
from src.api.constitutional_api import app

@pytest.fixture(scope="session")
def api_server():
    """Start a Flask server in a separate thread for API tests."""
    def run_server():
        app.run(host='localhost', port=8080, debug=False)
    
    server_thread = threading.Thread(target=run_server)
    server_thread.daemon = True
    server_thread.start()
    
    # Wait for server to start
    retries = 5
    while retries > 0:
        try:
            requests.get('http://localhost:8080/api/constitutional/validate')
            break
        except requests.exceptions.ConnectionError:
            time.sleep(1)
            retries -= 1
    
    yield
    # Server thread will be terminated when the test session ends