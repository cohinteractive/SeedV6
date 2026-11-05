"""Reproduce isolated complementary-depth helpers from exact96 IndependentSearch."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT=Path(__file__).resolve().parents[1]
SOURCE='engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/IndependentSearch.java'
TARGET='research/src/main/java/com/ohinteractive/seedv6/search/exact/P4ComplementarySearch.java'
BASE='44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0'


def generate():
    original=subprocess.check_output(['git','show',BASE+':'+SOURCE],cwd=ROOT).decode()
    assert (ROOT/SOURCE).read_text()==original
    source=original.replace('IndependentSearch','P4ComplementarySearch')
    a=source.index('/** MW-R001');b=source.index('public final class',a)
    source=source[:a]+'''/** IC-C3 Complementary-depth helpers, a research-only copy of exact96 IndependentSearch.
 * OWNER retains adjacent ADAPTIVE16 iterations, selection and unchanged BOUNDARY2.
 * Helpers visit alternating depth parities: workers1/3 even, worker2 odd atW4.
 * Skipped depths are never claimed complete; the existing aspiration adjacency
 * guard gives helpers full windows across their gaps. Shared TT remains heuristic,
 * not independent confidence. Global hard stops, accounting and joins are unchanged. */
'''+source[b:]
    before='                for(int depth=1;depth<=request.depth();depth++) {'
    after='''                int firstDepth=Math.min(request.depth(),index==0?1:1+(index&1));
                int stride=index==0?1:2;
                for(int depth=firstDepth;depth<=request.depth();depth+=stride) {'''
    assert source.count(before)==1
    return original.encode(),source.replace(before,after).encode()


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--check',action='store_true');args=parser.parse_args()
    original,generated=generate();path=ROOT/TARGET
    if args.check:assert path.read_bytes()==generated
    else:
        with path.open('xb') as stream:stream.write(generated)
    print(json.dumps(dict(status='verified' if args.check else 'created',base=BASE,source=SOURCE,target=TARGET,
                          originalLfSha256=hashlib.sha256(original).hexdigest(),targetSha256=hashlib.sha256(generated).hexdigest())))
