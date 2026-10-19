"""Assess only a completed, fully audited fixed clock screen; never read interim outcomes."""
import hashlib,json,pathlib
base=pathlib.Path(__file__).resolve().parent;raw=base/'raw/clock-screen-w4-v3'
run=json.loads((raw/'run.json').read_text());cpu=json.loads((raw/'process-cost.json').read_text())
assert run['status']=='passed' and run['inputsStable'] and cpu['status']=='passed'
assert run['results']['terminations']=={'normal':64} and run['runnerExitCode']==0
result={'classification':'fixed 32-pair development screen; not independent strength confirmation',
        'protocol':'clock-screen-v3.json','run':'raw/clock-screen-w4-v3/run.json',
        'runSha256':hashlib.sha256((raw/'run.json').read_bytes()).hexdigest(),
        'pgnSha256':hashlib.sha256((raw/'games.pgn').read_bytes()).hexdigest(),
        'candidate':'TWO_PASS','baseline':'PVS','workers':4,'tc':'10+0.1','concurrentGames':1,
        'wdl':run['results']['wdl'],'statistics':run['results']['statistics'],
        'terminations':run['results']['terminations'],'wholeRequestResources':run['results']['wholeRequestResources'],
        'matchWallSeconds':run['matchWallSeconds'],'runnerWallSeconds':run['wallSeconds'],
        'lifetimeCpuSeconds':cpu['cpuSeconds'],'candidateToBaselineCpu':cpu['candidateToBaselineCpu'],
        'inputsStable':True,'exclusions':[],
        'priorFailures':'V1 startup deadline expired with zero completed games; V2 adapter failed before runner launch. Neither contributes data. Both retained.',
        'limits':'Four-worker domain only; same machine and JVM. Normal interval is approximate and development selection/multiplicity are not confirmation-controlled. Conservative bounded interval also reported. Equal clock is not equal CPU. Desktop load uncontrolled; no peak RSS measurement. No production adoption.'}
(base/'clock-screen-v3-summary.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
