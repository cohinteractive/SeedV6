"""Audit existing immutable extraction identities and the limits of game independence."""
from collections import Counter
import json
from bootstrap import ROOT, BASE, PRIOR, sha, write


def main():
    paths=[ROOT/'docs/research/conventional-nnue-cglhw/e006-data-manifest.json',PRIOR/'additional-data-manifest.json']
    manifests=[json.loads(p.read_text()) for p in paths]
    assert manifests[0]['prepared_identity']==manifests[1]['prepared_identity']
    selected=[];counts=[]
    for m in manifests:
        ids={r['index'] for r in m['units']};assert len(ids)==len(m['units']);selected.append(ids)
        c=Counter()
        for r in m['units']:
            c[(r['split'],r['unit']['shard'])]+=r['accepted']
        counts.append({f'{split}_shard{shard}':n for (split,shard),n in sorted(c.items())})
    assert not (selected[0]&selected[1]),'Additional extraction reused E006 units'
    training={r['index']:r for m in manifests for r in m['units'] if r['split']=='train'}
    validation={r['index']:r for r in manifests[0]['units'] if r['split']=='validation'}
    assert not (training.keys()&validation.keys())
    adjacent=[]
    for v in validation.values():
        for t in training.values():
            if v['unit']['shard']==t['unit']['shard'] and abs(v['unit']['chunk']-t['unit']['chunk'])==1:
                adjacent.append(dict(validation_index=v['index'],training_index=t['index'],shard=v['unit']['shard']))
    report=dict(schema='seed-phase2-data-independence-audit-v1',
        inputs={str(p.relative_to(ROOT)):sha(p) for p in paths},prepared_identity=manifests[0]['prepared_identity'],
        records_by_source_split=counts,training_units=len(training),diagnostic_validation_units=len(validation),
        cross_extraction_unit_overlap=0,training_validation_unit_overlap=0,
        adjacent_train_validation_chunks=adjacent,
        diagnostic_sha256=sha(PRIOR/'raw/mixed-holdout/validation.txt'),diagnostic_positions=6753,
        verified_baseline='baseline.json rehashes actual data/provenance/holdout and original47-file runtime',
        decoder=dict(path='engine-training/src/main/java/com/ohinteractive/seedv6/training/data/BinpackDecoder.java',
                     sha256=sha(ROOT/'engine-training/src/main/java/com/ohinteractive/seedv6/training/data/BinpackDecoder.java'),
                     observed='Each BINP chunk is decoded independently. Each base carries its own board, ply, result and continuation count; Record exposes no source game ID. Prepared-unit/ordinal provenance identifies consumed positions, not globally unique games.'),
        established=['Consumed training/validation prepared units are disjoint, including the additional extraction.',
                     'Both training months are represented in the combined training set; existing validation is not a source-month holdout.',
                     'Supervised DirectSequentialDataLoader uses global batch offset modulo record count. Broad64 endpoint is four dataset passes; narrow64 is eight.'],
        limits=['No game ID or guarantee that a source game occurs in only one base/chunk: disjoint chunks do not establish game separation.',
                'Source descriptor uses sampled source fingerprints; consumed prepared chunks have full SHA256 checks. This is not a full hash of every original compressed source byte.',
                'Outcome targets are traceable source labels; independent audit of original game outcomes/adjudication quality is unavailable.',
                'Jan/Feb teacher labels use FEN-only context. Repetition/history quality is not repaired by broader coverage.',
                'A new source-month holdout would require withholding a month from new training; it cannot retroactively clean current WDL25.',
                'Filtered diagnostic is useful for matched comparisons but must not be represented as independent strength evidence.'],
        disposition='Continue controlled experiments with explicit limits; use separate opening pools for strength. Do not discard existing data or claim unavailable source-game proof. Revisit a source-separated corpus if architecture ranking remains plausibly data-limited.')
    write(BASE/'d001-data-audit.json',report)
    print(json.dumps({k:report[k] for k in ['records_by_source_split','training_units','diagnostic_validation_units','adjacent_train_validation_chunks']},indent=2))


if __name__=='__main__':main()
