"""Freeze build76 and exact accepted native baseline after prerequisite comparisons.

This performs actual UCI preflights; never run alongside a game or calibration.
"""
import json
from pathlib import Path
import shutil
import subprocess
from types import SimpleNamespace
import zipfile
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module


def main():
    assert json.loads((HERE/'clock-development-half-v1-summary.json').read_text())['status'] in ('passed','resource-unqualified'), 'Finish/diagnose HALF first'
    assert json.loads((HERE/'spending-qualification-v1-summary.json').read_text())['status']=='passed'
    economics=json.loads((HERE/'spending-clock-cost-v1-a02-summary.json').read_text())
    assert economics['status']=='passed' and economics['timingQualified']
    assert json.loads((HERE/'build76-runtime-summary.json').read_text())['exitCode']==0
    parity=json.loads((HERE/'build76-runtime-parity-v1.json').read_text());assert parity['status']=='passed'
    assert not subprocess.check_output(['git','diff','--name-only','--','engine-core','app-uci','research','src/researchLauncher','VERSION_STATE.txt'],cwd=ROOT,text=True).strip()
    candidate=HERE/'raw/runtime-candidate-v2';baseline=HERE/'raw/runtime-production-v2'
    shutil.copytree(ROOT/'build/research/runtime',candidate)
    shutil.copytree(candidate,baseline,ignore=shutil.ignore_patterns('engine-core.jar','app-uci.jar'))
    native=ROOT/'releases/candidates/windows-x64/candidate-20261023T195552-c00e3315/SeedV6-UCI/app'
    native_hashes={'engine-core.jar':'facac0db65c7f97a120083174ea4de20ef210daf3252a79a3fd5eb2cac99e6f7',
                  'app-uci.jar':'1ba9d9bcf44fcf385c6495e909fc3c24bb2dea4d1a10ed4f0d48b75319f67a19'}
    audit={}
    for jar,sha in native_hashes.items():
        assert digest(native/jar)==sha
        assert digest(candidate/'lib'/jar)==parity['jars'][jar]['newSha256']
        shutil.copy2(native/jar,baseline/'lib'/jar)
        with zipfile.ZipFile(native/jar) as a,zipfile.ZipFile(candidate/'lib'/jar) as b:
            an={x for x in a.namelist() if not x.endswith('/')};bn={x for x in b.namelist() if not x.endswith('/')}
            changed=[x for x in sorted(an & bn) if a.read(x)!=b.read(x)]
            audit[jar]={'baselineSha256':sha,'candidateSha256':digest(candidate/'lib'/jar),
                'changedEntries':changed,'addedEntries':sorted(bn-an),'removedEntries':sorted(an-bn)}
            if jar=='engine-core.jar':
                assert an==bn
                assert set(changed)<={'com/ohinteractive/seedv6/application-version.properties',
                    'com/ohinteractive/seedv6/search/common/SearchControl.class',
                    'com/ohinteractive/seedv6/search/common/SingleDepthSearch.class',
                    'com/ohinteractive/seedv6/search/driver/SearchDriver.class'}
            else:
                assert not an-bn
                assert bn-an=={'com/ohinteractive/seedv6/uci/GoCommandParser$ClockBudget.class'}
                assert set(changed)=={'com/ohinteractive/seedv6/uci/GoCommandParser.class',
                    'com/ohinteractive/seedv6/uci/UciConfiguration.class','com/ohinteractive/seedv6/uci/UciEngine.class'}
    write_new(HERE/'spending-runtime-class-audit-v1.json',{'status':'passed','jars':audit})
    names=['spending-development-v1.json','prepare_spending_development_v1.py','run_spending_development_v1.py',
        'spending_match_v1.py','audit_spending_clock_v1.py','run_spending_economics_v1.py','run_trajectory_v1.py','verify_baseline_v1.py',
        'spending-economics-v1-binding.json','spending-clock-cost-v1-a02-summary.json','spending-qualification-v1-summary.json',
        'build76-runtime-parity-v1.json','spending-runtime-class-audit-v1.json','fastchess-clock-source-v1.json',
        'clock-development-v1-openings.pgn','clock-development-v1-openings.selection.json',
        'clock-development-half-v1-summary.json','clock-development-stable3-half-v1-summary.json']
    files=[HERE/n for n in names]
    files += [ROOT/'tools'/n for n in ('p3-selectivity-match.py','search-baseline.py','p3-tt-session.py',
        'tournament-process.py','sr024-process-cost.py','sr036-process-cost.py')]
    files += [ROOT/'build/research/tools/fastchess.exe',ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']
    for runtime in (baseline,candidate):files += sorted(p for p in runtime.rglob('*') if p.is_file())
    for source in json.loads((HERE/'fastchess-clock-source-v1.json').read_text())['files'].values():files.append(HERE/source['local'])
    bound=json.loads((HERE/'spending-economics-v1-binding.json').read_text())['files'].copy()
    for p in files:
        key=str(p.relative_to(ROOT));sha=digest(p)
        if key in bound:assert bound[key]==sha
        bound[key]=sha
    binding={'head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        'files':bound,'externalFiles':{'C:/java/jdk-21/bin/java.exe':digest(Path('C:/java/jdk-21/bin/java.exe'))},
        'baseline':'Exact accepted native build74 core AND UCI JARs, matched new launcher/warmup',
        'candidate':'Build76 source614511ce9a75, classes identical to qualified runtime except generated version metadata',
        'openings':'Explicit reuse of32 development pairs; permanently excluded from independent confirmation'}
    write_new(HERE/'spending-development-v1-binding.json',binding)
    m=module('match',HERE/'spending_match_v1.py');b=m.b
    args=SimpleNamespace(baseline=str(baseline),candidate=str(candidate),baseline_policy='baseline',candidate_policy='unstable2',
        baseline_workers=1,candidate_workers=1,java='C:/java/jdk-21/bin/java.exe',
        network=str(ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue'))
    receipt={'status':'running','bindingSha256':digest(HERE/'spending-development-v1-binding.json'),'profiles':{}}
    try:
        for profile in ('baseline','unstable2','boundary2'):
            side='baseline' if profile=='baseline' else 'candidate';args.candidate_policy=profile
            cmd=m.command(args,side)
            receipt['profiles'][profile]=m.preflight(cmd,HERE/'raw'/('spending-frozen-'+profile+'-preflight.txt'),1,profile,64)
        for path,sha in binding['files'].items():assert digest(ROOT/path)==sha,path
        receipt['status']='passed'
    except BaseException as error:receipt.update(status='failed',failure=repr(error));raise
    finally:write_new(HERE/'spending-frozen-preflight-v1-summary.json',receipt)
    print('Frozen build76, exact accepted native baseline, development openings and three passing preflights.')


if __name__=='__main__':main()
