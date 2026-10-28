"""Extend a completed SQLite sample without redoing or mutating its parent selection."""
import argparse
import json
from pathlib import Path
import shutil
import sqlite3
import time
from baseline import ROOT,admit,sha
from compute_control import atomic
from prepare_objective import alias,unpack
from prepare_scale import database,number,commit_candidates,CHUNK,MULTIPLIER,SEED

TARGET=4194304


def clone_parent(parent,target,binding,expected_count):
    if target.exists():return database(target,binding)
    pending=target.with_name(target.name+'.partial-'+str(time.time_ns()))
    source=sqlite3.connect(parent.resolve().as_uri()+'?mode=ro',uri=True)
    child=None
    try:
        if source.execute('PRAGMA quick_check').fetchone()!=('ok',):raise ValueError('Parent SQLite integrity')
        if number(source,'count')!=expected_count or source.execute('SELECT count(*) FROM rows WHERE ordinal IS NOT NULL').fetchone()[0]!=expected_count:raise ValueError('Parent count')
        child=sqlite3.connect(pending);source.backup(child)
        with child:
            child.execute("INSERT INTO meta VALUES ('parent_binding',(SELECT value FROM meta WHERE key='binding'))")
            child.execute("UPDATE meta SET value=? WHERE key='binding'",(json.dumps(binding,sort_keys=True),))
        child.close();child=None;pending.replace(target)
    finally:
        source.close()
        if child is not None:child.close()
    return database(target,binding)


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('output',type=Path);parser.add_argument('--seconds',type=int,default=240);args=parser.parse_args()
    if not 1<=args.seconds<=240:raise ValueError('Bounded foreground tranche')
    started=time.monotonic();samples=[admit(initial=True)];args.output.mkdir(parents=True,exist_ok=True)
    parent=ROOT/'docs/research/brn-relational-cglhw/raw/r002-scale-data';manifest=json.loads((parent/'manifest.json').read_text())
    for name,digest in manifest['files'].items():
        if sha(parent/name)!=digest:raise ValueError('Parent file identity: '+name)
    source=Path(manifest['source'])
    if sha(source)!=manifest['binding']['source_sha256']:raise ValueError('Source identity')
    binding=dict(target=TARGET,chunk=CHUNK,seed=SEED,multiplier=MULTIPLIER,parent_manifest_sha256=sha(parent/'manifest.json'),
                 parent_database_sha256=sha(parent/'selection.sqlite'),script_sha256=sha(Path(__file__)),
                 helpers={name:sha(Path(__file__).with_name(name)) for name in ('prepare_scale.py','prepare_objective.py','baseline.py')},source_sha256=sha(source))
    done=args.output/'manifest.json'
    if done.exists():
        result=json.loads(done.read_text())
        if result['binding']!=binding or any(sha(args.output/name)!=digest for name,digest in result['files'].items()):raise ValueError('Completed coverage identity')
        print('Verified complete R010 selection');return
    db=clone_parent(parent/'selection.sqlite',args.output/'selection.sqlite',binding,manifest['train']);status='CHECKPOINTED';error=None
    try:
        size=source.stat().st_size//32
        with source.open('rb') as stream:
            while number(db,'count')<TARGET and time.monotonic()-started<args.seconds:
                samples.append(admit());cursor=number(db,'cursor');last=min(cursor+CHUNK,size)
                if last==cursor:raise ValueError('Source exhausted')
                records=[]
                for i in range(cursor,last):
                    source_id=(MULTIPLIER*i+SEED)%size;stream.seek(source_id*32);board,target=unpack(stream.read(32))
                    records.append((source_id,alias(board),f'{board.fen()}\t{target:.17g}'))
                commit_candidates(db,records,last,TARGET)
        if number(db,'count')==TARGET:
            samples.append(admit())
            temporary=args.output/'train.tsv.part'
            with temporary.open('w',newline='') as output:
                for row in db.execute('SELECT line FROM rows WHERE ordinal IS NOT NULL ORDER BY ordinal'):output.write(row[0]+'\n')
            temporary.replace(args.output/'train.tsv')
            temporary=args.output/'selected-indices.json.part'
            with temporary.open('w',newline='') as output:
                output.write('[')
                for i,row in enumerate(db.execute('SELECT source_id FROM rows WHERE ordinal IS NOT NULL ORDER BY ordinal')):output.write((',' if i else '')+str(row[0]))
                output.write(']\n')
            temporary.replace(args.output/'selected-indices.json');shutil.copyfile(parent/'validation.tsv',args.output/'validation.tsv')
            if db.execute('PRAGMA quick_check').fetchone()!=('ok',):raise ValueError('Child integrity')
            atomic(done,dict(schema='brn-coverage-extension-v1',binding=binding,train=TARGET,validation=4096,source=str(source),
                original_train_prefix=manifest['train'],selection_cursor=number(db,'cursor'),
                files={name:sha(args.output/name) for name in ('train.tsv','validation.tsv','selected-indices.json')},
                limitations='Consumed development validation; unknown source games; inherited final parent-chunk tail skipped; deterministic affine source ordering, not iid.'))
            status='COMPLETE'
    except Exception as exc:error=repr(exc);raise
    finally:
        receipt=dict(status=status if error is None else 'STOPPED',error=error,train=number(db,'count'),cursor=number(db,'cursor'),seconds=time.monotonic()-started,resources=samples)
        db.close();atomic(args.output/('execution-'+str(time.time_ns())+'.json'),receipt);print(json.dumps({k:v for k,v in receipt.items() if k!='resources'}))


if __name__=='__main__':main()
