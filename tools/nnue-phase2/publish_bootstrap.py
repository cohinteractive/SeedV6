"""One instructed immutable CREATE checkpoint, validated against product Bundle V1.

No Threadline state/database writes, target assignment, Apply or general watcher.
"""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import uuid
from bootstrap import BASE, write, sha

PRODUCT=Path('C:/projects/threadline/react/threadline-CEC')
INBOX=Path('C:/dev/tl-local')


def main():
    manifest=BASE/'threadline-bootstrap-publication.json'
    candidate=BASE/'threadline-bootstrap.json'
    if manifest.exists() or candidate.exists():
        raise RuntimeError('Existing checkpoint evidence: inspect and reuse exact bytes; never multiply candidates')
    result=json.loads((BASE/'a001-replica-assessment.json').read_text())['results']['replica']
    assert result['parity']['status']=='PASS'
    identifier=str(uuid.uuid4())
    bundle=dict(protocol='threadline-bundle',version=1,id=identifier,operation='CREATE',state=dict(
        title='SeedV6 NNUE Strength Research — Phase 2',
        purpose='Develop substantially stronger, computationally efficient NNUE evaluators through controlled training and architecture research under unchanged SeedV6 search.',
        position='Active research on branch research/nnue-phase2 from build101/08877838. Contract and resumable programme established in docs/research/nnue-phase2-cglhw. Protected WDL25/CP64, broad training data, diagnostic holdout and47 frozen runtime files rehashed successfully. Independent-seed WDL25 reproduction reached checkpoint64;259 cross-language parity probes, trained incremental checks and UCI pass. Teacher/outcome MSE0.017226/0.069416 and zero measured hot allocation; no Phase2 strength result yet. Training-factor controls are executing; compact16-unit downstream residual and stable pawn-pair pilots are specified. Production unchanged; completion criteria unsatisfied.',
        unresolved=[
            'WDL25 recipe strength reproduction and causal contributions of supervision, data coverage and decay remain unestablished.',
            'Existing validation is filtered for position/symmetry neighbors, but full source-game independence remains unproven.',
            'Compact deeper and relational architectures still need implementation validation, training, measured Java/search cost and equal-time games.',
            'Candidate selection requires fresh paired screens and prospectively frozen independent confirmation with uncertainty/selection accounting.',
            'No production promotion, deployment or BotLi change is authorized; any future promotion needs a separate owner decision.'
        ],
        next='Codex: continue the governed programme from repository STATE.md, finish matched training controls, validate architecture pilots and run justified fixed paired comparisons against WDL25. Respect existing compute controls and preserve all raw evidence.'),
        captures=[dict(kind='Reference',body='Repository: C:/projects/seed/java/seedv6-unified/docs/research/nnue-phase2-cglhw/CONTRACT.md and STATE.md. Baseline hashes: baseline.json. Reproduction evidence: a001-replica-assessment.json and referenced local raw/. Phase1 evidence remains at docs/research/conventional-nnue-cglhw/strength-20261114/REPORT.md. Publication is not application or acceptance.')])
    write(candidate,bundle)
    schema=PRODUCT/'docs/contracts/threadline-bundle-v1.schema.json'
    script="const fs=require('fs');const Ajv=require(process.argv[1]);const schema=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));const value=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));const validate=new Ajv({allErrors:true,strict:false}).compile(schema);if(!validate(value))throw Error(JSON.stringify(validate.errors));"
    subprocess.run(['node','-e',script,str(PRODUCT/'node_modules/ajv'),str(schema),str(candidate)],check=True)
    data=candidate.read_bytes();assert len(data)<=256*1024
    assert bundle['state']['unresolved'] and 'project' not in bundle['state']
    assert INBOX.is_dir()
    name='seed-phase2-'+str(uuid.uuid4());temporary=INBOX/(name+'.json.tmp');final=INBOX/(name+'.json')
    with temporary.open('xb') as f:f.write(data);f.flush();os.fsync(f.fileno())
    # Windows os.rename is atomic/no-overwrite within the same directory.
    if os.name!='nt':raise RuntimeError('This publication is specified for Windows no-overwrite rename')
    os.rename(temporary,final)
    assert final.is_file() and final.read_bytes()==data
    report=dict(operation='CREATE',id=identifier,path=str(final),bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),
                schema_sha256=sha(schema),schema_validation='PASS',semantic_size_validation='PASS',
                staging='NOT OBSERVED',apply='NOT PERFORMED',human_action='Review and manually Apply in Threadline; not blocking independent repository research')
    write(manifest,report);print(json.dumps(report,indent=2))


if __name__=='__main__':main()
