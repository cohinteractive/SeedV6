"""Fresh BRN development panel, with immutable exclusion DB and bounded checkpoints."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import random
import sqlite3
import time
import chess
from baseline import ROOT,admit,sha
from compute_control import atomic
from prepare_objective import alias


def record(index,line):
    fen,cp,_=line.strip().split(' | ');b=chess.Board(fen);value=float(cp)*(1 if b.turn else -1)/400
    if not b.is_valid() or not math.isfinite(value):raise ValueError('Invalid source position/target')
    target=1/(1+math.exp(-value)) if value>=0 else math.exp(value)/(1+math.exp(value))
    return dict(index=index,alias=alias(b),line=f'{b.fen()}\t{target:.17g}')


def select(records,order,contains,state,target,save,gate,chunk=512):
    seen={row['alias'] for row in state['rows']}
    for cursor in range(state['cursor'],len(order)):
        if len(state['rows'])>=target:break
        if cursor%chunk==0:gate()
        index=order[cursor];row=record(index,records[index]);key=row['alias']
        if key not in seen and not contains(key):state['rows'].append(row);seen.add(key)
        state['cursor']=cursor+1
        if (cursor+1)%chunk==0:save(state)
    save(state)
    if len(state['rows'])!=target:raise ValueError('Insufficient eligible candidate pool')


def checksum(value):return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('config',type=Path);args=parser.parse_args();config=json.loads(args.config.read_text())
    start=time.monotonic();resources=[admit(initial=True)];out=ROOT/config['output'];out.mkdir(parents=True,exist_ok=True)
    def gate():
        resources.append(admit())
        if time.monotonic()-start>config['max_seconds']:raise RuntimeError('Bounded selection allocation; saved cursor retained')
    source=ROOT/config['source'];excluded=ROOT/config['exclusion_database']
    if sha(source)!=config['source_sha256'] or sha(excluded)!=config['exclusion_sha256']:raise ValueError('Input identity')
    if Path(str(excluded)+'-wal').exists() and Path(str(excluded)+'-wal').stat().st_size:raise ValueError('Exclusion database not frozen')
    binding=dict(config_sha256=sha(args.config),runner_sha256=sha(Path(__file__)),alias_helper_sha256=sha(Path(__file__).with_name('prepare_objective.py')))
    done=out/'manifest.json'
    if done.exists():
        prior=json.loads(done.read_text())
        if prior['binding']!=binding or any(sha(out/k)!=v for k,v in prior['files'].items()):raise ValueError('Completed panel identity')
        print('Verified completed fresh panel');return
    checkpoint=out/'selection.json'
    state=dict(binding=binding,cursor=0,rows=[])
    if checkpoint.exists():
        envelope=json.loads(checkpoint.read_text());state=envelope['state']
        if envelope['sha256']!=checksum(state) or state['binding']!=binding:raise ValueError('Selection checkpoint identity')
    order=random.Random(config['seed']).sample(range(config['source_rows']),config['candidates']);wanted=set(order);records={}
    with source.open() as f:
        for i,line in enumerate(f):
            if i%65536==0:gate()
            if i in wanted:records[i]=line
    if len(records)!=len(order):raise ValueError('Source row count')
    def save(s):atomic(checkpoint,dict(sha256=checksum(s),state=s))
    db=sqlite3.connect(excluded.resolve().as_uri()+'?mode=ro&immutable=1',uri=True)
    try:
        db.execute('PRAGMA cache_size=-4096')
        if int(db.execute("SELECT value FROM meta WHERE key='count'").fetchone()[0])!=config['excluded_training_rows']:raise ValueError('Exclusion metadata')
        select(records,order,lambda key:db.execute('SELECT 1 FROM rows WHERE alias=?',(key,)).fetchone() is not None,state,config['rows'],save,gate)
    finally:db.close()
    if sha(excluded)!=config['exclusion_sha256']:raise ValueError('Exclusion database changed')
    text='\n'.join(row['line'] for row in state['rows'])+'\n';data=out/'validation.tsv'
    if data.exists():
        if data.read_text()!=text:raise ValueError('Partial publication conflict')
    else:
        temporary=out/('validation-'+str(time.time_ns())+'.part')
        with temporary.open('x',newline='',encoding='utf8') as f:f.write(text)
        temporary.rename(data)
    atomic(done,dict(binding=binding,rows=len(state['rows']),cursor=state['cursor'],files={p.name:sha(p) for p in (data,checkpoint)},seconds=time.monotonic()-start,resources=resources,
        scope='Fresh BRN development only;CP64 previously selected on source pool;unknown source games. Later training expansion must exclude these canonical aliases. No R010 mutation.'))
    print(json.dumps(dict(rows=len(state['rows']),cursor=state['cursor'],seconds=time.monotonic()-start,manifest_sha256=sha(done))))


if __name__=='__main__':main()
