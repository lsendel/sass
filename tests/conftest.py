"""Pytest configuration to ensure project packages import correctly.

Adds the repository root to `sys.path` so imports like `import src...` work
consistently across environments.
"""

import sys
from pathlib import Path

# Insert repo root at the beginning of sys.path
ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

