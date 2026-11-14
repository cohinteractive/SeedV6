"""Independent Phase2 float/quantized feature and arithmetic oracle; no production writes."""
import array
import hashlib
import json
import math
from pathlib import Path
import struct
import sys
import chess

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
from export_network import features as base_features, trunc
H=256
KINDS={'base':0,'deep16':1,'pawnpairs':2}


def sha(path):
    with Path(path).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()


def shapes(kind):
    result={'l0w':7680*H,'l0f':768*H,'l0b':H,'l1w':512*8,'l1b':8}
    if kind==1:result.update(l2w=512*16,l2b=16,l3w=16*8,l3b=8)
    if kind==2:result['ppw']=4560*H
    return result


def tensors(path,kind):
    data=Path(path).read_bytes();at=0;result={};expected=shapes(kind)
    while at<len(data):
        end=data.index(b'\n',at);key=data[at:end].decode('ascii');count,=struct.unpack_from('<Q',data,end+1);at=end+9
        if key in result or expected.get(key)!=count:raise ValueError('Unexpected tensor geometry')
        values=array.array('f');values.frombytes(data[at:at+4*count]);at+=4*count
        if sys.byteorder!='little':values.byteswap()
        if len(values)!=count or not all(map(math.isfinite,values)):raise ValueError('Invalid tensor payload')
        result[key]=values
    if set(result)!=set(expected):raise ValueError('Incomplete tensor set')
    return result


def verify_checkpoint(path):
    path=Path(path);receipt=json.loads((path/'receipt.json').read_text());config=json.loads((path.parent/'config.json').read_text())
    if receipt['schema']!='seed-phase2-checkpoint-v1' or receipt['config_sha256']!=sha(path.parent/'config.json'):
        raise ValueError('Receipt/config mismatch')
    if receipt['architecture']!=config['architecture'] or receipt['completed_superbatch']!=config['end_superbatch']:
        raise ValueError('Receipt endpoint mismatch')
    expected={'quantised.bin','raw.bin','optimiser_state/weights.bin','optimiser_state/momentum.bin','optimiser_state/velocity.bin','gpu-probes.jsonl'}
    if set(receipt['files'])!=expected:raise ValueError('Incomplete receipt')
    for name,digest in receipt['files'].items():
        if sha(path/name)!=digest:raise ValueError('Checkpoint checksum mismatch: '+name)
    kind=KINDS[config['architecture']]
    for name in ('weights','momentum','velocity'):tensors(path/f'optimiser_state/{name}.bin',kind)
    return config,receipt


def merged(values,kind):
    factor=values['l0f']
    first=array.array('f',(x+factor[i%len(factor)] for i,x in enumerate(values['l0w'])))
    if kind==2:first.extend(values['ppw'])
    result=[(first,255),(values['l0b'],255)]
    for layer,rows,cols in [('l1',8,512)]+([('l2',16,512),('l3',8,16)] if kind==1 else []):
        result.append((array.array('f',(values[layer+'w'][i*rows+j] for j in range(rows) for i in range(cols))),64))
        result.append((values[layer+'b'],255*64))
    return result


def quantize_value(x,scale):
    # Pinned Bullet promotes binary32 weights to f64 BEFORE multiplication.
    # A binary32 product can create a false half tie (e.g.128.49999994 ->128.5).
    value=x*scale
    return math.trunc(value+math.copysign(.5,value))


def wrap_conventional(source,destination):
    """Same quantized payload, experimental envelope only; isolate kernel economics."""
    source=Path(source);destination=Path(destination);data=source.read_bytes();payload=data[72:]
    expected=(2,7680,256,10,255,64,400,(7681*256+8*513)*2)
    if data[:8]!=b'S6CNU002' or struct.unpack_from('<8I',data,8)!=expected or len(payload)!=expected[-1]:
        raise ValueError('Not the eight-head conventional256 format')
    digest=hashlib.sha256(payload).digest()
    if digest!=data[40:72]:raise ValueError('Conventional checksum mismatch')
    envelope=b'S6P2N001'+struct.pack('<9I',1,0,7680,256,0,255,64,400,len(payload))+digest+payload
    with destination.open('xb') as out:out.write(envelope)
    return dict(source=str(source),source_sha256=sha(source),network=str(destination),network_sha256=sha(destination),
                payload_sha256=digest.hex(),transformation='Envelope only; exact quantized payload retained')


def export(checkpoint,path):
    checkpoint=Path(checkpoint);path=Path(path);config,receipt=verify_checkpoint(checkpoint);kind=KINDS[config['architecture']]
    values_by_name=tensors(checkpoint/'optimiser_state/weights.bin',kind)
    parts=merged(values_by_name,kind);quant=array.array('h')
    for values,scale in parts:
        for x in values:
            q=quantize_value(x,scale)
            if not -32768<=q<=32767:raise ValueError('Quantization overflow')
            if scale==64 and abs(q)>127:raise ValueError('Downstream weight exceeds SIMD bound')
            quant.append(q)
    if sys.byteorder!='little':quant.byteswap()
    payload=quant.tobytes();native=(checkpoint/'quantised.bin').read_bytes()
    if native[:len(payload)]!=payload or native[len(payload):]!=(b'bullet'*11)[:len(native)-len(payload)] or len(native)!=(len(payload)+63)//64*64:
        raise ValueError('Native/Python quantization or padding mismatch')
    # Bullet raw.bin saves named tensors without transforms, factorizer merge or
    # transposition. For pawnpairs it also omits ppw (included only by a transform).
    raw=array.array('f')
    for name in ['l0w','l0b','l1w','l1b']+(['l2w','l2b','l3w','l3b'] if kind==1 else []):raw.extend(values_by_name[name])
    if sys.byteorder!='little':raw.byteswap()
    if (checkpoint/'raw.bin').read_bytes()!=raw.tobytes():raise ValueError('Native/Python untransformed float mismatch')
    features=7680+(4560 if kind==2 else 0);digest=hashlib.sha256(payload).digest()
    data=b'S6P2N001'+struct.pack('<9I',1,kind,features,H,16 if kind==1 else 0,255,64,400,len(payload))+digest+payload
    with path.open('xb') as f:f.write(data)
    metadata=dict(schema='S6P2N001',architecture=config['architecture'],hidden=256,output_buckets=8,
        features=features,network=str(path),network_sha256=sha(path),payload_sha256=digest.hex(),bytes=len(data),checkpoint=str(checkpoint),receipt_sha256=sha(checkpoint/'receipt.json'))
    with path.with_suffix(path.suffix+'.json').open('x') as f:json.dump(metadata,f,indent=2)
    return metadata


def features(board,white,kind):
    result=base_features(board,white)
    if kind!=2:return result
    flip=(0 if white else 56)^(7 if board.king(white)%8>3 else 0)
    pawns=list(chess.scan_forward(board.pawns))
    for i,a in enumerate(pawns):
        if not 8<=a<56:raise ValueError('Invalid pawn rank')
        for b in pawns[i+1:]:
            if abs(a%8-b%8)>1:continue
            x=(0 if board.color_at(a)==white else 48)+(a^flip)-8
            y=(0 if board.color_at(b)==white else 48)+(b^flip)-8
            lo,hi=sorted((x,y));result.append(7680+hi*(hi-1)//2+lo)
    return sorted(result)


def split_flat(flat,kind):
    count=7680+(4560 if kind==2 else 0);at=count*H
    parts=[flat[:at],flat[at:at+H]];at+=H
    for length in [4096,8]+([8192,16,128,8] if kind==1 else []):parts.append(flat[at:at+length]);at+=length
    if at!=len(flat):raise ValueError('Invalid flat geometry')
    return parts


def evaluate(parts,board,kind,integer):
    weights,bias,head,head_bias=parts[:4];fs=[features(board,c,kind) for c in (board.turn,not board.turn)]
    sums=[[bias[j]+sum(weights[k*H+j] for k in ids) for j in range(H)] for ids in fs]
    top=255 if integer else 1.;act=[max(0,min(top,x))**2 for side in sums for x in side]
    bucket=(len(board.piece_map())-2)//4
    dot=sum(x*y for x,y in zip(act,head[bucket*512:(bucket+1)*512]))
    raw=(trunc(dot,255) if integer else dot)+head_bias[bucket]
    direct=raw;hidden=[]
    if kind==1:
        w,b,ow,ob=parts[4:];hidden=[]
        for j in range(16):
            dot=sum(x*y for x,y in zip(act,w[j*512:(j+1)*512]))
            x=trunc(trunc(dot,255)+b[j],64) if integer else dot+b[j]
            hidden.append(max(0,min(top,x))**2)
        extra=sum(x*y for x,y in zip(hidden,ow[bucket*16:(bucket+1)*16]))
        raw+=(trunc(extra,255) if integer else extra)+ob[bucket]
    score=max(-32511,min(32511,trunc(raw*400,255*64))) if integer else 400*raw
    return dict(score=score,raw=raw if not integer else None,features=fs,accumulators=sums,
                downstream_activations=hidden,residual_link=400*(raw-direct)/(255*64 if integer else 1))
