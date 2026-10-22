"""Bounded, read-only extraction from the owner's CC0 Lichess eval archive.

Score-only supervision: result=0.5 is a format placeholder and MUST have wdl=0.
No full-source conversion, fabricated game outcomes, or claim of game-independent
validation. Requires pinned local venv: chess==1.11.2, zstandard==0.25.0.
"""
import argparse
from collections import Counter
import hashlib
import io
import itertools
import json
from pathlib import Path
import random
import time
import chess
import zstandard
import compute_gate


class HashedReader(io.RawIOBase):
    def __init__(self, path):
        self.stream = path.open('rb')
        self.digest = hashlib.sha256()
        self.count = 0
    def readable(self): return True
    def read(self, size=-1):
        data = self.stream.read(size)
        self.digest.update(data); self.count += len(data)
        return data
    def readinto(self, buffer):
        data = self.read(len(buffer)); buffer[:len(data)] = data; return len(data)
    def close(self):
        self.stream.close(); super().close()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('source', type=Path)
    p.add_argument('output', type=Path)
    p.add_argument('--positions', type=int, default=131072)
    p.add_argument('--max-lines', type=int, default=1000000)
    p.add_argument('--seed', type=int, default=20261022)
    args = p.parse_args()
    compute_gate.require()
    if not 1024 <= args.positions <= 4194304 or not 1 <= args.max_lines <= 10000000:
        raise ValueError('Outside bounded extraction limits')
    args.output.mkdir(parents=True, exist_ok=False)
    before = args.source.stat()
    counts = Counter(); seen = set(); train = []; validation = []; held = False
    reader = HashedReader(args.source)
    start = time.monotonic()
    try:
        with zstandard.ZstdDecompressor().stream_reader(reader, read_across_frames=True, closefd=False) as decoded:
            lines = io.TextIOWrapper(decoded, encoding='utf-8')
            for ordinal, line in enumerate(itertools.islice(lines,args.max_lines), 1):
                if ordinal%1000==1 and not compute_gate.permitted():
                    held = True
                    break
                counts['read'] += 1
                entry = json.loads(line)
                evaluations = [e for e in entry['evals'] if e.get('pvs') and 'cp' in e['pvs'][0]]
                if not evaluations:
                    counts['no_cp'] += 1
                    continue
                best = max(evaluations, key=lambda e:(e.get('depth',0),e.get('knodes',0)))
                cp = best['pvs'][0]['cp']
                fen = entry['fen']
                if len(fen.split()) == 4: fen += ' 0 1'
                board = chess.Board(fen)
                reason = None
                if best.get('depth',0) < 12: reason='shallow'
                elif abs(cp)>3000: reason='extreme_score'
                elif not board.is_valid(): reason='invalid_position'
                elif board.is_check() or board.is_game_over(): reason='check_or_terminal'
                else:
                    pv = best['pvs'][0].get('line','').split()
                    if not pv: reason='missing_pv'
                    else:
                        try: move=board.parse_uci(pv[0])
                        except ValueError: reason='invalid_pv'
                        else:
                            if board.is_capture(move) or move.promotion: reason='tactical_pv'
                if reason:
                    counts[reason]+=1
                else:
                    # Identical evaluator inputs cannot enter both splits (rights/clock
                    # are deliberately excluded). Related games remain unknowable.
                    key=board.board_fen()+' '+('w' if board.turn else 'b')
                    digest=hashlib.sha256(key.encode()).digest()
                    if digest in seen: counts['duplicate']+=1
                    else:
                        seen.add(digest)
                        record=(fen,cp,ordinal,best.get('depth'),best.get('knodes'))
                        (validation if int.from_bytes(digest[:8],'little')%10==0 else train).append(record)
                        counts['accepted']+=1
                if ordinal%10000==0:
                    print(f"read={ordinal} accepted={counts['accepted']} elapsed={time.monotonic()-start:.1f}s",flush=True)
                if counts['accepted']>=args.positions or ordinal>=args.max_lines: break
    finally:
        reader.close()
    after=args.source.stat()
    if (before.st_size,before.st_mtime_ns)!=(after.st_size,after.st_mtime_ns): raise RuntimeError('Source changed during extraction')
    random.Random(args.seed).shuffle(train)
    for name,records in [('train',train),('validation',validation)]:
        with (args.output/(name+'.txt')).open('w',encoding='utf-8',newline='\n') as out:
            for fen,cp,*_ in records: out.write(f'{fen} | {cp} | 0.5\n')
        with (args.output/(name+'-provenance.jsonl')).open('w',encoding='utf-8',newline='\n') as out:
            for fen,cp,ordinal,depth,nodes in records:
                out.write(json.dumps(dict(fen=fen,white_cp=cp,source_line=ordinal,depth=depth,knodes=nodes))+'\n')
    probes=[r[0] for r in validation[:128]]
    (args.output/'probes.fens').write_text('\n'.join(probes)+'\n')
    hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in args.output.iterdir() if p.is_file()}
    manifest=dict(source=str(args.source.resolve()),source_size=before.st_size,source_mtime_ns=before.st_mtime_ns,
        source_url='https://database.lichess.org/#evals',license='CC0',
        consumed_compressed_prefix_bytes=reader.count,consumed_compressed_prefix_sha256=reader.digest.hexdigest(),
        counts=dict(counts),train=len(train),validation=len(validation),seed=args.seed,hashes=hashes,
        targets='white-relative CP; sigmoid(cp/400); wdl MUST equal 0; result field is unused placeholder',
        filters='valid, nonterminal, not in check; highest-depth CP PV; depth>=12; abs(cp)<=3000; noncapture/nonpromotion PV',
        split='90/10 SHA256 placement+STM; exact evaluator-input dedup; no game IDs available',
        limitations=['bounded source prefix, not a random sample of the complete archive',
            'mixed Stockfish versions and CP calibration; no outcome labels',
            'related-game and symmetry leakage cannot be ruled out; validation is diagnostic, not strength evidence'],
        elapsed_seconds=time.monotonic()-start, stopped_for_compute_hold=held,
        requested_positions=args.positions, completed_requested_sample=counts['accepted']>=args.positions,
        continuation='Complete dataset is reusable; held partial extraction is evidence only. Restart extraction to a new path if needed.')
    (args.output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps(manifest,indent=2),flush=True)
    if held: raise SystemExit(75)


if __name__=='__main__':main()
