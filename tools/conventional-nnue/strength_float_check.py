"""Fixed held-out float/quantized comparison independent of the GPU implementation.

Diagnostic subset only; reads retained optimizer and exported integer bytes.
The same evenly spaced subset is used for every network, without label selection.
"""
import argparse
import array
import hashlib
import json
import math
from pathlib import Path
import sys
import chess
import compute_gate
from export_network import checkpoint_weights, merged_weights, features, evaluate, verify_quantization
from compute_control import verify_checkpoint
import training_lifecycle


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('checkpoint',type=Path);p.add_argument('network',type=Path)
    p.add_argument('validation',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();compute_gate.require()
    verify_checkpoint(a.checkpoint);training_lifecycle.verify_model(a.network,'nnue')
    state=checkpoint_weights(a.checkpoint/'optimiser_state/weights.bin');hidden=len(state['l0b']);heads=len(state['l1b'])
    weights=merged_weights(state);bias=weights[7680*hidden:7681*hidden];head=weights[7681*hidden:]
    payload=a.network.read_bytes()[72:]
    verify_quantization(payload,a.checkpoint/'optimiser_state/weights.bin')
    quant=array.array('h');quant.frombytes(payload)
    if sys.byteorder!='little':quant.byteswap()
    lines=a.validation.read_text().splitlines();count=min(1024,len(lines))
    if count==0:raise ValueError('Empty holdout')
    float_mse=quant_mse=error2=0.;max_error=0.
    for i in range(count):
        if i%32==0:compute_gate.require()
        fen,target,_=lines[i*len(lines)//count].split('|');board=chess.Board(fen.strip())
        label=float(target)*(1 if board.turn else -1);target=1/(1+math.exp(-label/400))
        bucket=0 if heads==1 else (len(board.piece_map())-2)//4
        raw=head[heads*2*hidden+bucket]
        for side,color in enumerate((board.turn,not board.turn)):
            indices=features(board,color)
            for j in range(hidden):
                value=bias[j]+sum(weights[k*hidden+j] for k in indices)
                raw+=max(0.,min(1.,value))**2*head[bucket*2*hidden+side*hidden+j]
        integer=evaluate(quant,board,hidden)['score']
        fp=1/(1+math.exp(-raw));qp=1/(1+math.exp(-integer/400))
        float_mse+=(fp-target)**2;quant_mse+=(qp-target)**2
        error=integer-400*raw;error2+=error*error;max_error=max(max_error,abs(error))
    result=dict(positions=count,hidden=hidden,float_sigmoid_mse=float_mse/count,quantized_sigmoid_mse=quant_mse/count,
                quantization_rms_link_error=math.sqrt(error2/count),quantization_max_abs_link_error=max_error,
                network_sha256=hashlib.sha256(a.network.read_bytes()).hexdigest(),
                validation_sha256=hashlib.sha256(a.validation.read_bytes()).hexdigest(),
                limitation='Diagnostic evenly spaced subset; CPU float uses independently merged binary32 weights and double accumulation. GPU parity is a separate probe check.')
    with a.output.open('x',encoding='utf-8') as stream:json.dump(result,stream,indent=2);stream.write('\n')
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
