"""Validate inference and the outcome-blind clock audit before independent play."""
import ast
import json
import shutil
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module


def main():
    names = ['prepare_confirmation_v1.py','run_confirmation_block_v1.py','audit_confirmation_clock_v1.py',
             'analyze_confirmation_v1.py','validate_confirmation_v1.py']
    record = {'status':'running','checks':[]}
    try:
        for name in names: ast.parse((HERE/name).read_text())
        record['checks'].append('Five script syntax checks')
        plan = json.loads((HERE/'confirmation-v1.json').read_text()); d = plan['domain']; a = plan['analysis']
        assert d['pairs'] == d['blocks']*d['pairsPerBlock'] == 128
        assert d['blocks'] == 4 and d['pairsPerBlock'] == 32 and d['workers'] == d['concurrentGames'] == 1 and d['clock'] == '10+0.1'
        assert a['alpha'] == .05 and plan['selectedProfile'] in ('unstable2','boundary2')
        record['checks'].append('Single-finalist domain, fixed block/sample and alpha consistency')
        module('analysis',HERE/'analyze_confirmation_v1.py').self_test()
        record['checks'] += ['Closed-form variance and standard error','95% normal quantile','Permutation invariance','Zero-variance refusal','Elo transform']
        # Re-audit a closed development fixture with statistics removed. It cannot
        # become confirmation data. Preserve originals and the derived fixture.
        source = HERE/'raw/spending-development-unstable2-v1'
        fixture_name = 'confirmation-clock-audit-fixture-v1'; fixture = HERE/'raw'/fixture_name
        fixture.mkdir(exist_ok=False)
        for name in ('games.pgn','process-cost.json'): shutil.copy2(source/name,fixture/name)
        result = json.loads((source/'run.json').read_text()); assert result['status'] == 'passed'
        result['results']['statistics'] = None
        result['derivedFixture'] = {'source':str(source.relative_to(HERE)),'sourceRunSha256':digest(source/'run.json'),
            'purpose':'Audit parity only; never independent confirmation evidence'}
        write_new(fixture/'run.json', result)
        audit = module('clock_audit', HERE/'audit_confirmation_clock_v1.py'); captured = []
        audit.write_new = lambda path,value: captured.append((path,value))
        observed = audit.audit(fixture_name)
        reference_path = HERE/'spending-development-unstable2-v1-clock-analysis-v1.json'
        reference = json.loads(reference_path.read_text())
        assert len(captured) == 1 and 'strength' not in observed
        for key in ('actors','integrity','clockReliabilityPassed','maximumRequestOverHardMillis','actorNativeCpuSeconds','peakWorkingSetBytes'):
            assert observed[key] == reference[key], key
        record['checks'].append('All8292 closed request/clock records reproduce the original audit exactly; confirmation output omits strength')
        record['fixture'] = {'files':{str(p.relative_to(HERE)):digest(p) for p in fixture.iterdir()},
            'referencePath':reference_path.name,'referenceSha256':digest(reference_path)}
        record['status'] = 'passed'
    except BaseException as error:
        record.update(status='failed',failure=repr(error)); raise
    finally:
        record['files'] = {name:digest(HERE/name) for name in names+['confirmation-v1.json']}
        write_new(HERE/'confirmation-statistics-validation-v1.json', record)
    print(json.dumps(record,indent=2))


if __name__ == '__main__': main()
