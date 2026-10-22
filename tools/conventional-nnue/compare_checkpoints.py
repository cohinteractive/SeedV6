"""Report GPU continuation differences without asserting bitwise reproducibility."""
import argparse
import json
from pathlib import Path
from export_network import checkpoint_weights


def compare(left,right):
    result={}
    for name in ('weights','momentum','velocity'):
        a=checkpoint_weights(left/'optimiser_state'/f'{name}.bin')
        b=checkpoint_weights(right/'optimiser_state'/f'{name}.bin')
        maximum=sumsq=0.;changed=n=0
        for key in a:
            for x,y in zip(a[key],b[key]):
                difference=abs(x-y);maximum=max(maximum,difference)
                sumsq+=difference*difference;changed+=x!=y;n+=1
        result[name]=dict(max_abs=maximum,rms=(sumsq/n)**.5,changed=changed,count=n)
    result['quantised_equal']=(left/'quantised.bin').read_bytes()==(right/'quantised.bin').read_bytes()
    return result


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('left',type=Path);p.add_argument('right',type=Path);p.add_argument('output',type=Path)
    a=p.parse_args();result=compare(a.left,a.right)
    with a.output.open('x') as out:json.dump(result,out,indent=2)
    print(json.dumps(result,indent=2))
