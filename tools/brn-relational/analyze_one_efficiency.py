"""Summarize every frozen R057 lifecycle observation without selecting timing rounds."""
import hashlib
import argparse
import json
from pathlib import Path
import statistics


def main():
    base = Path('docs/research/brn-relational-cglhw')
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,default=base/'raw/r057/lifecycle-v3-vector/result.json')
    parser.add_argument('--output',type=Path,default=base/'r057-efficiency-results.json')
    args=parser.parse_args();source=args.source
    data = json.loads(source.read_text())
    modes = ('R056_ONE_FULL' if data['untrained'] else 'R057_ONE_FULL', 'R057_ONE_INCREMENTAL', 'CP64')
    metrics = {}
    for mode in modes:
        row = {}
        for group in ('components', 'lifecycle'):
            observations = [r for r in data[group] if r['mode'] == mode]
            for phase in sorted({r['phase'] for r in observations}):
                selected = [r for r in observations if r['phase'] == phase]
                key = 'ns_per_operation' if group == 'components' else 'ns_per_evaluation_including_all_state_work'
                values = [r[key] for r in selected]
                row[phase] = dict(median_ns=statistics.median(values), min_ns=min(values), max_ns=max(values),
                    samples=len(values), allocated_bytes=sorted({r['allocated_bytes'] for r in selected}))
        searches = [r for r in data['search'] if r['mode'] == mode]
        row['search'] = dict(requests=len(searches), actual_nodes=sum(r['nodes'] for r in searches),
            search_seconds=sum(r['search_ns'] for r in searches)/1e9,
            aggregate_nps=sum(r['nodes'] for r in searches)*1e9/sum(r['search_ns'] for r in searches))
        metrics[mode] = row
    for fen in data['roots']:
        pair = [r for r in data['search'] if r['fen'] == fen and r['mode'] != 'CP64']
        if len({(r['nodes'], r['best_move'], r['score'], r['depth']) for r in pair}) != 1:
            raise ValueError('Reference/incremental search divergence: '+fen)
    latency = metrics[modes[1]]['complete_search_trace']['median_ns']/metrics[modes[2]]['complete_search_trace']['median_ns']
    throughput = metrics[modes[1]]['search']['aggregate_nps']/metrics[modes[2]]['search']['aggregate_nps']
    result = dict(experiment='R057', status='EFFICIENCY_MEASURED', untrained=data['untrained'],
        source=str(source), source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
        source_model_sha256=data['model_sha256'], trace_sha256=data['trace_sha256'],
        trace_events=data['trace_events'], trace_counts=data['trace_counts_initialize_child_copy_evaluate'],
        cp64_backend=data['cp64_head_backend'], metrics=metrics,
        one_cp64_lifecycle_ratio=latency, one_cp64_nps_ratio=throughput,
        predeclared_criteria=dict(maximum_lifecycle_ratio=1.25,minimum_nps_ratio=.8),
        passes=latency<=1.25 and throughput>=.8, reference_incremental_search_results_identical=True,
        capacity=dict(width=data.get('width',8),rank=data.get('rank',4),active_parameters=data.get('parameters',19201),cp64_parameters=1970440),
        immutable_payload_bytes=data['immutable_payload_bytes'], accumulator_payload_bytes=data['accumulator_payload_bytes'],
        initialization={k:v for k,v in data.items() if k.startswith('immutable_') or k=='state_initialization'},
        limitations=[data['limitations'], 'Nine within-JVM timing rounds are correlated; min/max are descriptive, not confidence intervals.',
            'Fixed-node NPS uses different CP64/ONE trees; replay fixes the transition trace. No playing-strength inference.',
            'Training volume and quality are separate evidence; repeat timing for the final selected checkpoint.',
            'Earlier v1/v2 scalar-head results retained; their CP64 full cost used the slow independent oracle, not normal refresh.'])
    target=args.output
    if target.exists():
        raise ValueError('Preserve published analysis')
    target.write_text(json.dumps(result,indent=2)+'\n')
    print('lifecycle ratio',latency,'NPS ratio',throughput,'passes',result['passes'])


if __name__ == '__main__':
    main()
