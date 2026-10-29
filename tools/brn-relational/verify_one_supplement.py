"""Independently check every R057 supplement record, label and reserved alias."""
import collections
import json
import math
import mmap
from pathlib import Path
import struct
import subprocess
import time
import chess
from baseline import ROOT, sha
from prepare_objective import unpack
from prepare_one_exclusions import compact
from verify_one_corpus import contains
import compute_gate


def main():
    compute_gate.require()
    started = time.monotonic()
    base = ROOT / 'docs/research/brn-relational-cglhw'
    raw = base / 'raw/r057'
    source = raw / 'supplement'
    output = raw / 'supplement-check'
    output.mkdir(exist_ok=False)
    manifest = json.loads((source / 'manifest.json').read_text())
    original = json.loads((ROOT / 'docs/research/conventional-nnue-cglhw/e006-data-manifest.json').read_text())
    merge = json.loads((raw / 'corpus-merge/result.json').read_text())
    for name, digest in manifest['hashes'].items():
        assert sha(source / name) == digest, name
    used = {u['index'] for u in original['units']}
    units = {u['unit_index']: u for u in manifest['units']}
    assert len(units) == len(manifest['units']) and not used.intersection(units)
    links = {}
    def link(encoded):
        if encoded not in links:
            lo, hi = 0., 1.
            for _ in range(56):
                q = (lo + hi) / 2
                if 660.6*q/(1-.9751875*q**10) < abs(encoded): lo = q
                else: hi = q
            q = ((lo+hi)/2) * (1 if encoded > 0 else -1) if encoded else 0.
            p = max(.0001, min(.9999, (q+1)/2))
            links[encoded] = math.floor(400*math.log(p/(1-p))+.5)
        return links[encoded]
    files = {'fresh_validation': (source/'fresh-validation.data').read_bytes(),
             'training_replacement': (source/'topup.data').read_bytes()}
    cursor = collections.Counter(); phases = collections.Counter(); counts = collections.Counter()
    seen = set(); previous = {}; combined = bytearray()
    with (raw/'corpus-merge/unique.keys').open('rb') as uf, (raw/'exclusions/reserved.keys').open('rb') as rf:
        with mmap.mmap(uf.fileno(),0,access=mmap.ACCESS_READ) as unique, mmap.mmap(rf.fileno(),0,access=mmap.ACCESS_READ) as reserved:
            reservations = struct.unpack_from('>I',reserved,8)[0]
            with (source/'proof.txt').open() as proofs, (source/'labels.jsonl').open() as labels:
                for n,(text,label) in enumerate(zip(proofs,labels,strict=True),1):
                    if n%1024 == 0: compute_gate.require()
                    row = json.loads(label); split = row['split']; entry = units[row['unit']]
                    assert entry['split'] == split and previous.get(row['unit'],-1) < row['ordinal'] < entry['unit']['count']
                    previous[row['unit']] = row['ordinal']; counts[row['unit']] += 1
                    offset = cursor[split]*32; record = files[split][offset:offset+32]; cursor[split] += 1
                    board, target = unpack(record)
                    score,result = struct.unpack_from('<hB',record,24)
                    assert score == link(row['encoded']) == row['link'] and result == row['stm_result']+1
                    fen, white_score, white_result = text.strip().split(' | ')
                    original_board = chess.Board(fen); sign = 1 if original_board.turn else -1
                    normalized = original_board if original_board.turn else original_board.mirror()
                    assert original_board.is_valid() and normalized.board_fen() == board.board_fen()
                    assert int(white_score)*sign == score and float(white_result) == (sign*row['stm_result']+1)/2
                    assert row['ply'] >= 16 and not original_board.is_check()
                    key = compact(board)
                    assert key not in seen and not contains(unique,key,merge['exact_canonical_unique'])[0]
                    assert not contains(reserved,key,reservations,12)[0]
                    seen.add(key); combined.extend(record)
                    phases[split+'_pieces_'+str(len(board.piece_map())//4*4)] += 1
                    phases[split+'_ply_'+str(row['ply']//32*32)] += 1
    assert all(counts[i] == u['accepted'] for i,u in units.items())
    assert cursor == dict(fresh_validation=8192,training_replacement=39396)
    assert all(len(files[k]) == cursor[k]*32 for k in files)
    trainer = ROOT/'build/conventional-nnue/cargo-target/release/seed-conventional-nnue.exe'
    with (output/'conversion.log').open('xb') as log:
        subprocess.run([str(trainer),'convert',str(source/'proof.txt'),str(output/'upstream.data')],
                       stdout=log,stderr=subprocess.STDOUT,check=True)
    assert (output/'upstream.data').read_bytes() == combined, 'Upstream packing mismatch'
    report = dict(status='PASS',counts=dict(cursor),canonical_unique=len(seen),
                  all_labels_and_packing_verified=True,all_alias_exclusions_verified=True,
                  original_units_disjoint=True,unit_counts=dict(counts),distribution=dict(sorted(phases.items())),
                  manifest_sha256=sha(source/'manifest.json'),trainer_sha256=sha(trainer),
                  upstream_sha256=sha(output/'upstream.data'),seconds=time.monotonic()-started,
                  limitations='Reuses tested source decoder. No game IDs; source-game independence remains unknown. Fresh validation remains unconsumed by model selection.')
    for p in [output/'result.json',base/'r057-supplement-audit.json']:
        with p.open('x',encoding='utf-8') as f: f.write(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report,indent=2))


if __name__ == '__main__': main()
