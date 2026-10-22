"""Cooperative boundary checks for operations launched by compute_control.py."""
import json
import os
from pathlib import Path
import time


def permitted():
    try:
        state = json.loads(Path(os.environ['SEED_NNUE_CONTROL']).read_text())
        active = state['active']
        return (state['schema'] == 1 and state['mode'] == 'NORMAL' and active is not None
            and active['id'] == os.environ['SEED_NNUE_OPERATION']
            and active['stop_requested_at'] is None
            and (state['expires_at'] is None or time.time() < state['expires_at']))
    except (KeyError, OSError, ValueError, TypeError):
        return False


def require():
    if not permitted():
        raise RuntimeError('No active NNUE compute permission. Launch through nnue-control.cmd run/train.')
