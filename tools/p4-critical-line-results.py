"""Analyze complete CLV development samples only; never admits confirmation."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('clv_results', ROOT / 'tools/p4-iteration-confidence-results.py')
analysis = importlib.util.module_from_spec(spec); spec.loader.exec_module(analysis)
analysis.OUT = ROOT / 'docs/research/search/p4-critical-line'


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--receipt',required=True)
    p.add_argument('--output',required=True);p.add_argument('--operational-only',action='store_true')
    args=p.parse_args();r=json.loads(Path(args.receipt).read_text(encoding='utf8'))
    protocol=json.loads((ROOT/r['protocol']).read_text(encoding='utf8'))
    if protocol.get('campaign')!='CLV-R001' or protocol['category']!='exploration':
        raise ValueError('CLV development only')
    result=analysis.analyze(args.receipt,args.operational_only)
    result['clvAdapterSha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    with Path(args.output).open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps(result))
