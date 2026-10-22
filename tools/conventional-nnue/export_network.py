"""Wrap Bullet's merged quantised.bin in the strictly versioned Seed S6CNU001 envelope.

No transformation of quantised weights. Independent integer reference inference
and feature fixtures can be generated from the exported payload and FEN input.
Python uses arbitrary precision, independently checking Java's integer arithmetic.
"""
import argparse
import array
import hashlib
import json
import math
from pathlib import Path
import struct
import sys
import chess

HIDDEN=256
FEATURES=7680
COUNT=FEATURES*HIDDEN+HIDDEN*3+1
BUCKETS=(0,1,2,3,4,4,5,5,6,6,6,6,7,7,7,7,8,8,8,8,8,8,8,8,9,9,9,9,9,9,9,9)


def features(board,white):
    flip=0 if white else 56
    king=board.king(white)^flip
    mirror=7 if king%8>=4 else 0
    bucket=BUCKETS[(king//8)*4+((king^mirror)%8)]
    result=[]
    for square,piece in board.piece_map().items():
        channel=piece.piece_type-1+(0 if piece.color==white else 6)
        result.append(bucket*768+channel*64+(square^flip^mirror))
    return sorted(result)


def trunc(n,d):return (1 if n>=0 else -1)*(abs(n)//d)


def checkpoint_weights(path):
    data=path.read_bytes();offset=0;result={}
    while offset<len(data):
        end=data.index(b'\n',offset);name=data[offset:end].decode('ascii')
        count,=struct.unpack_from('<Q',data,end+1);offset=end+9
        if name in result or count>FEATURES*HIDDEN:raise ValueError('Invalid optimizer weight record')
        values=array.array('f');values.frombytes(data[offset:offset+count*4]);offset+=count*4
        if len(values)!=count:raise ValueError('Truncated optimizer weights')
        if sys.byteorder!='little':values.byteswap()
        result[name]=values
    expected={'l0w':FEATURES*HIDDEN,'l0f':768*HIDDEN,'l0b':HIDDEN,'l1w':2*HIDDEN,'l1b':1}
    if {k:len(v) for k,v in result.items()}!=expected:raise ValueError('Unexpected optimizer architecture')
    return result


def merged_weights(values):
    factor=values['l0f']
    # Round the sum to binary32, exactly as Bullet's export transform does.
    merged=array.array('f',(x+factor[i%len(factor)] for i,x in enumerate(values['l0w'])))
    return merged+values['l0b']+values['l1w']+values['l1b']


def verify_quantization(payload,checkpoint):
    floats=merged_weights(checkpoint_weights(checkpoint))
    quantised=array.array('h')
    split=(FEATURES+1)*HIDDEN
    for i,x in enumerate(floats):
        scale=255 if i<split else 64 if i<COUNT-1 else 255*64
        value=x*scale
        if not math.isfinite(value):raise ValueError('Nonfinite weight')
        quantised.append(math.trunc(value+math.copysign(.5,value)))
    if sys.byteorder!='little':quantised.byteswap()
    if quantised.tobytes()!=payload:raise ValueError('Independent factorizer/quantization mismatch')
    return floats


def evaluate(weights,board):
    biases=weights[FEATURES*HIDDEN:FEATURES*HIDDEN+HIDDEN]
    head=weights[FEATURES*HIDDEN+HIDDEN:]
    sums=[]; fs=[]
    total=0
    for side,white in enumerate((board.turn,not board.turn)):
        indices=features(board,white);fs.append(indices)
        acc=[biases[i]+sum(weights[f*HIDDEN+i] for f in indices) for i in range(HIDDEN)]
        sums.append(acc)
        total+=sum(max(0,min(255,a))**2*head[side*HIDDEN+i] for i,a in enumerate(acc))
    score=max(-32511,min(32511,trunc((trunc(total,255)+head[-1])*400,255*64)))
    return dict(score=score,features=fs,accumulators=sums)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('quantised',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--fens',type=Path);p.add_argument('--reference',type=Path)
    args=p.parse_args()
    raw=args.quantised.read_bytes();payload=raw[:COUNT*2]
    expected=(COUNT*2+63)//64*64
    padding=(b'bullet'*11)[:expected-COUNT*2]
    if len(raw)!=expected or raw[COUNT*2:]!=padding:raise ValueError('Unexpected Bullet length/padding')
    verify_quantization(payload,args.quantised.parent/'optimiser_state/weights.bin')
    digest=hashlib.sha256(payload).digest()
    header=b'S6CNU001'+struct.pack('<8I',1,FEATURES,HIDDEN,10,255,64,400,len(payload))+digest
    with args.output.open('xb') as out:out.write(header+payload)
    metadata=dict(format='S6CNU001',bullet_quantised_sha256=hashlib.sha256(raw).hexdigest(),
        payload_sha256=digest.hex(),file_sha256=hashlib.sha256(header+payload).hexdigest(),
        bytes=len(header+payload),parameters=COUNT,source=str(args.quantised.resolve()),
        independent_quantization='all parameters match merged optimizer weights with half-away rounding',
        layout='LE i16: feature-major weights[7680][256], bias[256], STM/NTM head[512], output bias',
        arithmetic='clamp 0..255 then square; long dot; trunc(dot/255); add bias; multiply400; trunc(/16320); clamp +-32511')
    args.output.with_suffix(args.output.suffix+'.json').write_text(json.dumps(metadata,indent=2)+'\n')
    if args.fens:
        if not args.reference:raise ValueError('--reference required with --fens')
        weights=array.array('h');weights.frombytes(payload)
        if sys.byteorder!='little':weights.byteswap()
        with args.reference.open('x',encoding='utf-8') as out:
            for fen in args.fens.read_text().splitlines():
                out.write(json.dumps(dict(fen=fen,**evaluate(weights,chess.Board(fen))))+'\n')
    print(json.dumps(metadata,indent=2))


if __name__=='__main__':main()
