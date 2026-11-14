"""One instructed pre-result C001 UPDATE checkpoint; no assignment or Apply."""
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
    candidate=BASE/'threadline-confirmation-checkpoint.json';manifest=BASE/'threadline-confirmation-publication.json'
    if candidate.exists() or manifest.exists():raise RuntimeError('Existing candidate evidence: do not multiply IDs or deliveries')
    assert not (BASE/'c001-results.json').exists(),'This checkpoint describes the pre-result stage only'
    plan=json.loads((BASE/'c001-plan-v2.json').read_text());protocol=json.loads((BASE/'raw/c001-direct128-64/protocol.json').read_text())
    assert plan['games']==512 and plan['confirmation_family_size']==1 and protocol['network_sha256']==plan['network_sha256']
    assert protocol['openings_sha256']==plan['openings_sha256'] and protocol['max_plies']==800
    bundle=dict(protocol='threadline-bundle',version=1,id=str(uuid.uuid4()),operation='UPDATE',state=dict(
        position='Active research; production WDL25/CP64 and build101 remain unchanged. All15 fixed development endpoints are complete:960 audited games, zero caps. Added depth/pawn features and longer training improve diagnostics without established strength gains. Direct128 checkpoint64 has the highest exploratory estimate:25W/23D/16L,+49 Elo, paired95% -11..+112; standard256 checkpoint128 is+38,-22..+106. Ranking remains uncertain. Data audits establish substantial additional input coverage, not source-game independence. Matched duration controls and float/quantized diagnostics are complete; predictive ranking survives quantization. C001 has selected and launched exactly one independent512-game confirmation of direct128 checkpoint64 against WDL25, using256 reserved pairs and frozen runtime v4. Final results are pending; no evaluator is accepted or promoted. Research completion criteria remain unsatisfied.',
        next='Codex: continue from STATE.md and active c001-plan-v2.json. Check controller/process ownership before any resume. Await the full fixed endpoint and independent legal/artifact/statistical audit; no interim efficacy decisions, alternate candidate or unlucky-result retest. Apply the frozen complete/uncensored, paired95% lower-bounds-above-even and point>=20-Elo gates. Only if all pass, separately qualify bounded longer-clock/two-worker robustness before final synthesis. Production integration/promotion requires a separate owner decision.'),
        captures=[dict(kind='Decision',body='C001 selection is exploratory, not a statistical ranking: compact direct128 checkpoint64, SHA256 defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c.512 games,200ms,W1,Hash32,CLV_C1,-Xbatch,two20k-node warmup passes,800-ply censor ceiling,14400-second match budget,one primary attempt. No development-score pooling. Roots are legal/unique/previously unused in Phase2 and disjoint from development,cost and parity roots; related opening-family dependence can remain. Current research launcher is explicitlyW1-only; actualW2 needs isolated enablement and targeted validation.'),
                  dict(kind='Reference',body='Repository: C:/projects/seed/java/seedv6-unified/docs/research/nnue-phase2-cglhw/. CONTRACT.md and STATE.md govern/restart the research; REPORT.md is an interim synthesis. C001.md,c001-plan-v2.json and screening-summary.json identify the frozen confirmation and all prior evidence. Unknown current Threadline unresolved items/grouping are preserved by omission. Publication is not staging, application, acceptance or deployment.')])
    write(candidate,bundle);schema=PRODUCT/'docs/contracts/threadline-bundle-v1.schema.json'
    assert sha(schema)=='375582db417afefcae28e2b0cd568ecd60445f6f6aae0da61066b28e0374bbe6'
    script="const fs=require('fs');const Ajv=require(process.argv[1]);const v=new Ajv({allErrors:true,strict:false}).compile(JSON.parse(fs.readFileSync(process.argv[2],'utf8')));if(!v(JSON.parse(fs.readFileSync(process.argv[3],'utf8'))))throw Error(JSON.stringify(v.errors));"
    subprocess.run(['node','-e',script,str(PRODUCT/'node_modules/ajv'),str(schema),str(candidate)],check=True)
    data=candidate.read_bytes();data.decode('utf-8',errors='strict')
    assert len(data)<=256*1024 and 'unresolved' not in bundle['state'] and 'project' not in bundle['state']
    assert os.name=='nt' and INBOX.is_dir()
    name='seed-phase2-confirmation-'+str(uuid.uuid4());temporary=INBOX/(name+'.json.tmp');final=INBOX/(name+'.json')
    with temporary.open('xb') as stream:stream.write(data);stream.flush();os.fsync(stream.fileno())
    os.rename(temporary,final)
    assert final.is_file() and not final.is_symlink() and final.read_bytes()==data
    report=dict(operation='UPDATE',id=bundle['id'],path=str(final),bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),
        schema_sha256=sha(schema),schema_validation='PASS',semantic_size_validation='PASS',staging='NOT OBSERVED',apply='NOT PERFORMED',
        human_action='Explicitly assign intended Phase2 thread; review queue sequence/current state, approve V1 legacy/freshness warning and manually Apply. Non-blocking for research.')
    write(manifest,report);print(json.dumps(report,indent=2))


if __name__=='__main__':main()
