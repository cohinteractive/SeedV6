"""Run only the prospectively frozen stages permitted by the SR-024 assessor.

No live score looks, automatic retuning, post-stop extension or resumption into
an existing output directory. Each completed stage receives a fresh full audit.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
from types import SimpleNamespace


def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result


def run(protocol_path,output):
    protocol_path=Path(protocol_path).resolve();p=json.loads(protocol_path.read_text(encoding='utf8'))
    def digest(path):
        with Path(path).open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
    assert p['executorSha256']==digest(__file__)
    assert p['assessorSha256']==digest(p['assessorModule'])
    assert all(digest(path)==expected for path,expected in p['frozenFiles'].items())
    assert all(str(Path(p[key]).resolve()) in p['frozenFiles'] for key in ('runnerModule','assessorModule'))
    runner=module('sr024_match',p['runnerModule']);assessor=module('sr024_confirmation',p['assessorModule']);b=runner.b
    out=Path(output).resolve();out.mkdir(parents=True,exist_ok=False)
    record={'status':'running','protocol':str(protocol_path),'protocolSha256':b.digest(protocol_path),'runs':[],'assessments':[]}
    b.save(out/'execution.json',record)
    try:
        for index,stage in enumerate(p['stages'],1):
            assert b.digest(protocol_path)==record['protocolSha256']
            arguments=dict(p['expectedArguments'],out=str(out/f'stage{index}'),openings=stage['openings'],pairs=stage['pairs'])
            runner.run(SimpleNamespace(**arguments));record['runs'].append(arguments['out'])
            assert b.digest(protocol_path)==record['protocolSha256']
            assessment=assessor.assess(protocol_path,record['runs'])
            assert b.digest(protocol_path)==record['protocolSha256']
            path=out/f'assessment{index}.json';b.save(path,assessment);record['assessments'].append(str(path))
            record['decision']=assessment['finalDecision'];b.save(out/'execution.json',record)
            print(json.dumps({'completedLook':index,'assessment':assessment['looks'][-1]}),flush=True)
            if record['decision']!='continue':break
        assert record['decision']!='continue'
        record['status']='closed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error));raise
    finally:
        b.save(out/'execution.json',record)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--protocol',required=True);parser.add_argument('--out',required=True)
    a=parser.parse_args();run(a.protocol,a.out)
