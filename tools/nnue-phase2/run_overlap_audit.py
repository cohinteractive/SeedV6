"""Bounded D002 fixture checks followed by the frozen mixture audit, under owner control."""
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import sys
from bootstrap import ROOT, BASE, PRIOR, write, sha
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
import compute_gate


def record(mirror=False,variant=False,score=0):
    pieces=[(4,5),(9 if variant else 8,0),(55,8),(60,13)]
    if mirror:pieces=[(sq^7,pc) for sq,pc in pieces]
    pieces.sort();data=bytearray(32);struct.pack_into('<Q',data,0,sum(1<<s for s,p in pieces))
    for i,(square,piece) in enumerate(pieces):data[8+i//2]|=piece<<(4*(i%2))
    struct.pack_into('<h',data,24,score);data[26]=1;data[27]=3 if mirror else 4;data[28]=data[27]
    return bytes(data)


def main():
    compute_gate.require();out=BASE/'raw/d002-input-overlap';out.mkdir(exist_ok=False);classes=out/'classes'
    runtime=BASE/'raw/frozen-runtime-v3/lib/*';source=ROOT/'tools/nnue-phase2/InputOverlapAudit.java';commands=[]
    def run(command,name,timeout=330):
        compute_gate.require();log=out/(name+'.log');commands.append(dict(command=command,log=str(log)))
        with log.open('xb') as stream:subprocess.run(command,stdout=stream,stderr=subprocess.STDOUT,check=True,timeout=timeout)
    run(['C:/java/jdk-21/bin/javac.exe','-cp',str(runtime),'-d',str(classes),str(source)],'compile',60)
    java=['C:/java/jdk-21/bin/java.exe','-XX:ActiveProcessorCount=2','-Xmx2g','-cp',str(classes)+os.pathsep+str(runtime),'InputOverlapAudit']
    fixtures=[]
    for name,other,expected in [('mirrored',record(True,score=777)*4096,(1,1,4096)),
                                ('distinct',record(variant=True,score=-999)*4096,(2,0,0)),
                                ('partial',record(score=555)*2048+record(variant=True)*2048,(2,1,2048))]:
        data=out/(name+'.data');data.write_bytes(record(score=-123)*4096+other);report=out/(name+'.json')
        run(java+[str(data),sha(data),str(report)],name,60);result=json.loads(report.read_text())
        assert (result['unique_fingerprints'],result['shared_fingerprints'],result['additional_records_matching_original'])==expected
        fixtures.append(dict(name=name,result=result,input_sha256=sha(data)))
    config=json.loads((BASE/'a001-replica-pilot.json').read_text());report=out/'mixture.json'
    run(java+[str(ROOT/config['data']),config['data_sha256'],str(report)],'mixture')
    result=json.loads(report.read_text());assert result['records']==67108864
    assert result['unique_fingerprints']+result['duplicate_records']==result['records']
    assert result['original_unique_fingerprints']+result['additional_unique_fingerprints']-result['shared_fingerprints']==result['unique_fingerprints']
    assert result['additional_only_fingerprints']==result['additional_unique_fingerprints']-result['shared_fingerprints']
    write(BASE/'d002-input-overlap.json',dict(result=result,fixtures=fixtures,commands=commands,source_sha256=sha(source),
        class_sha256=sha(classes/'InputOverlapAudit.class'),raw_report_sha256=sha(report),
        interpretation='Source/record accounting audit only. No data mutation and no proof of source-game separation.'))
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
