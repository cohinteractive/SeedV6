"""One instructed S001/D002 UPDATE candidate; no target selection or Apply."""
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
    candidate=BASE/'threadline-s001-checkpoint.json';manifest=BASE/'threadline-s001-publication.json'
    if candidate.exists() or manifest.exists():raise RuntimeError('Existing candidate: inspect/reuse exact bytes; do not multiply IDs')
    screens=json.loads((BASE/'s001-summary.json').read_text());audit=json.loads((BASE/'d002-input-overlap.json').read_text())
    assert screens['total_games']==512 and screens['total_censored']==0
    assert audit['result']['sha256']=='3a4d8d009e730e2700cf5552b48a165c7cc044d84b3bdb6518a3108d440a5729'
    qualified=[]
    for name in ('base','deep16','pawnpairs'):
        report=BASE/f'compact128-{name}-64-assessment.json'
        if report.exists():
            result=json.loads(report.read_text());assert result['parity']['status']=='PASS' and result['uci']['status']=='PASS'
            qualified.append(name)
    assert 'base' in qualified
    bundle=dict(protocol='threadline-bundle',version=1,id=str(uuid.uuid4()),operation='UPDATE',state=dict(
        position='Active research on research/nnue-phase2; production WDL25/CP64 and build101 remain unchanged. All eight initial64-game paired screens are complete, legally replayed and artifact-audited, with zero caps. Exploratory Elo versus WDL25: reproduction+33 (paired95% -33..+100), teacher-only-38, narrow-data-72,50% outcome-83, late decay-16, deep256+16-83, pawn-pair256-66, exact-weight fast kernel-33. No established improvement or promotion candidate. D002 found32.48M additional-only input fingerprints;2.83% of added records match original-source inputs. Source-game independence remains unproved. Eight targeted Java tests and width-codec regression pass; compact128 runtime v4 is frozen. Qualified compact64 endpoints so far: '+', '.join(qualified)+'. The matched compact trio and controlled cost comparison are the current follow-up; independent confirmation/robustness openings remain unused. Completion criteria are not yet satisfied.',
        next='Codex: resume from repository STATE.md, inspect controller/process ownership before launching anything, finish the active compact128 driver and cost gates, then freeze justified development comparisons. Select independent confirmation only from credible candidates with prospective statistical and resource limits. Preserve all completed/failed raw evidence and production assets.'),
        captures=[dict(kind='Codex result',body='Evidence: docs/research/nnue-phase2-cglhw/s001-summary.json, screen-*-results.json, d002-input-overlap.json, comparative-cost.json, B003.md and compact128-*-assessment.json. Shared-root training contrasts are descriptive/post-hoc, not multiplicity-adjusted acceptance or direct head-to-head Elo. Faster inference and lower prediction loss did not establish strength. The queued PowerShell exit-code handoff failed after successful games; direct child-exit verification recovered without repeating games.'),
                  dict(kind='Reference',body='Contract and restart state: C:/projects/seed/java/seedv6-unified/docs/research/nnue-phase2-cglhw/CONTRACT.md and STATE.md. Unknown current Threadline unresolved items are preserved by omitting unresolved, and project grouping is omitted. Publication is not staging, application, acceptance or production promotion.')])
    write(candidate,bundle);schema=PRODUCT/'docs/contracts/threadline-bundle-v1.schema.json'
    script="const fs=require('fs');const Ajv=require(process.argv[1]);const v=new Ajv({allErrors:true,strict:false}).compile(JSON.parse(fs.readFileSync(process.argv[2],'utf8')));if(!v(JSON.parse(fs.readFileSync(process.argv[3],'utf8'))))throw Error(JSON.stringify(v.errors));"
    subprocess.run(['node','-e',script,str(PRODUCT/'node_modules/ajv'),str(schema),str(candidate)],check=True)
    data=candidate.read_bytes();data.decode('utf-8',errors='strict')
    assert len(data)<=256*1024 and 'unresolved' not in bundle['state'] and 'project' not in bundle['state']
    assert INBOX.is_dir() and os.name=='nt'
    name='seed-phase2-checkpoint-'+str(uuid.uuid4());temporary=INBOX/(name+'.json.tmp');final=INBOX/(name+'.json')
    with temporary.open('xb') as stream:stream.write(data);stream.flush();os.fsync(stream.fileno())
    os.rename(temporary,final)
    assert final.is_file() and not final.is_symlink() and final.read_bytes()==data
    report=dict(operation='UPDATE',id=bundle['id'],path=str(final),bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),
        schema_sha256=sha(schema),schema_validation='PASS',semantic_size_validation='PASS',staging='NOT OBSERVED',apply='NOT PERFORMED',
        human_action='Assign intended Phase2 thread, review sequence/current state, explicitly approve V1 legacy/freshness warning and manually Apply; non-blocking for independent repository research')
    write(manifest,report);print(json.dumps(report,indent=2))


if __name__=='__main__':main()
