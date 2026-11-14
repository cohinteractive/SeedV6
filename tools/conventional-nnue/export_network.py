"""Wrap Bullet's merged quantised.bin in the strictly versioned Seed S6CNU001/2 envelope.

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
        if name in result or count>FEATURES*512:raise ValueError('Invalid optimizer weight record')
        values=array.array('f');values.frombytes(data[offset:offset+count*4]);offset+=count*4
        if len(values)!=count:raise ValueError('Truncated optimizer weights')
        if sys.byteorder!='little':values.byteswap()
        result[name]=values
    heads=len(result.get('l1b',[]))
    if heads not in (1,8):raise ValueError('Unsupported output geometry')
    hidden=len(result.get('l0b',[]))
    if (hidden,heads) not in ((256,1),(256,8),(512,8)):raise ValueError('Unsupported hidden geometry')
    expected={'l0w':FEATURES*hidden,'l0f':768*hidden,'l0b':hidden,'l1w':2*hidden*heads,'l1b':heads}
    if {k:len(v) for k,v in result.items()}!=expected:raise ValueError('Unexpected optimizer architecture')
    return result


def merged_weights(values):
    hidden=len(values['l0b'])
    factor=values['l0f']
    # Round the sum to binary32, exactly as Bullet's export transform does.
    merged=array.array('f',(x+factor[i%len(factor)] for i,x in enumerate(values['l0w'])))
    heads=len(values['l1b'])
    # Bullet affine tensors are column-major: input then output. Its saved
    # transpose makes complete selected heads contiguous for CPU inference.
    head=array.array('f',(values['l1w'][i*heads+b] for b in range(heads) for i in range(2*hidden)))
    return merged+values['l0b']+head+values['l1b']


def verify_quantization(payload,checkpoint):
    values=checkpoint_weights(checkpoint);heads=len(values['l1b']);hidden=len(values['l0b']);floats=merged_weights(values)
    quantised=array.array('h')
    split=(FEATURES+1)*hidden
    for i,x in enumerate(floats):
        scale=255 if i<split else 64 if i<len(floats)-heads else 255*64
        value=x*scale
        if not math.isfinite(value):raise ValueError('Nonfinite weight')
        quantised.append(math.trunc(value+math.copysign(.5,value)))
    if sys.byteorder!='little':quantised.byteswap()
    if quantised.tobytes()!=payload:raise ValueError('Independent factorizer/quantization mismatch')
    return floats


def evaluate(weights,board,hidden=HIDDEN):
    biases=weights[FEATURES*hidden:FEATURES*hidden+hidden]
    head=weights[FEATURES*hidden+hidden:]
    heads=len(head)//(2*hidden+1)
    if heads not in (1,8):raise ValueError('Unsupported output geometry')
    bucket=0 if heads==1 else (len(board.piece_map())-2)//4
    if not 0<=bucket<heads:raise ValueError('Invalid material count')
    sums=[]; fs=[]
    total=0
    for side,white in enumerate((board.turn,not board.turn)):
        indices=features(board,white);fs.append(indices)
        acc=[biases[i]+sum(weights[f*hidden+i] for f in indices) for i in range(hidden)]
        sums.append(acc)
        total+=sum(max(0,min(255,a))**2*head[bucket*2*hidden+side*hidden+i] for i,a in enumerate(acc))
    score=max(-32511,min(32511,trunc((trunc(total,255)+head[heads*2*hidden+bucket])*400,255*64)))
    return dict(score=score,features=fs,accumulators=sums)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('quantised',type=Path);p.add_argument('output',type=Path)
    p.add_argument('--fens',type=Path);p.add_argument('--reference',type=Path)
    args=p.parse_args()
    values=checkpoint_weights(args.quantised.parent/'optimiser_state/weights.bin');heads=len(values['l1b']);hidden=len(values['l0b'])
    config=json.loads((args.quantised.parent.parent/'config.json').read_text())
    if config.get('output_buckets',1)!=heads or config.get('hidden',256)!=hidden:raise ValueError('Config/model geometry mismatch')
    count=(FEATURES+1)*hidden+(2*hidden+1)*heads
    raw=args.quantised.read_bytes();payload=raw[:count*2]
    expected=(count*2+63)//64*64
    padding=(b'bullet'*11)[:expected-count*2]
    if len(raw)!=expected or raw[count*2:]!=padding:raise ValueError('Unexpected Bullet length/padding')
    verify_quantization(payload,args.quantised.parent/'optimiser_state/weights.bin')
    digest=hashlib.sha256(payload).digest()
    version=3 if hidden==512 else 1 if heads==1 else 2;magic=f'S6CNU00{version}'.encode('ascii')
    header=magic+struct.pack('<8I',version,FEATURES,hidden,10,255,64,400,len(payload))+digest
    with args.output.open('xb') as out:out.write(header+payload)
    metadata=dict(format=magic.decode(),output_buckets=heads,hidden=hidden,bullet_quantised_sha256=hashlib.sha256(raw).hexdigest(),
        payload_sha256=digest.hex(),file_sha256=hashlib.sha256(header+payload).hexdigest(),
        bytes=len(header+payload),parameters=count,source=str(args.quantised.resolve()),
        independent_quantization='all parameters match merged optimizer weights with half-away rounding',
        layout=f'LE i16: feature-major weights[7680][{hidden}], bias[{hidden}], heads[{heads}][{hidden*2}], output biases[{heads}]',
        arithmetic='clamp 0..255 then square; long dot; trunc(dot/255); add bias; multiply400; trunc(/16320); clamp +-32511')
    args.output.with_suffix(args.output.suffix+'.json').write_text(json.dumps(metadata,indent=2)+'\n')
    if args.fens:
        if not args.reference:raise ValueError('--reference required with --fens')
        weights=array.array('h');weights.frombytes(payload)
        if sys.byteorder!='little':weights.byteswap()
        with args.reference.open('x',encoding='utf-8') as out:
            for fen in args.fens.read_text().splitlines():
                out.write(json.dumps(dict(fen=fen,**evaluate(weights,chess.Board(fen),hidden)))+'\n')
    print(json.dumps(metadata,indent=2))


if __name__=='__main__':main()
