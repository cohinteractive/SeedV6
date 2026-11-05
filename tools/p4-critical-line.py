"""CLV-R001 admission adapter; reuse tested Windows Job/resource supervision.

No closed campaign state is mutated. Independent confirmation is not admitted by
this development runner. Its protocol/analysis must be frozen separately.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('clv_execution', ROOT / 'tools/p4-iteration-confidence.py')
execution = importlib.util.module_from_spec(spec)
spec.loader.exec_module(execution)
execution.OUT = ROOT / 'docs/research/search/p4-critical-line'
original_validate = execution.validate


def validate(protocol, ledger):
    if ledger['campaign'] != 'CLV-R001':
        raise ValueError('Wrong campaign ledger')
    if protocol.get('campaign') != 'CLV-R001':
        raise ValueError('Wrong campaign protocol')
    if protocol['kind'] == 'timed':
        # The inherited runner charges all timed work as development here. Reserve
        # all 640 independent games and enforce the distinct development ceiling.
        if ledger['consumed']['confirmationAttempts'] or ledger['consumed']['confirmationGames']:
            raise ValueError('Development after independent admission is not authorized')
        if ledger['consumed']['initiatedTimedGames'] + protocol['games'] > ledger['ceilings']['developmentGames']:
            raise ValueError('Development game ceiling')
        if ledger['planningReserve']['confirmationGames'] != 640:
            raise ValueError('Independent game reserve must remain 640')
    # The inherited supervisor plans/guards at four workers. A W1 diagnostic is
    # admitted under that conservative resource ceiling; its actual domain stays W1.
    checked = dict(protocol)
    if protocol['kind'] == 'fixed-work' and protocol['workers'] == 1:
        checked['workers'] = 4
    original_validate(checked, ledger)


execution.validate = validate


def run(path):
    try:
        execution.run(path)
    finally:
        ledger_path = execution.OUT / 'budget.json'
        ledger = json.loads(ledger_path.read_text(encoding='utf8'))
        if not ledger['pending']:
            consumed = ledger['consumed']
            consumed['developmentGames'] = consumed['initiatedTimedGames'] - consumed['confirmationGames']
            execution.save(ledger_path, ledger)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol', required=True)
    run(parser.parse_args().protocol)
