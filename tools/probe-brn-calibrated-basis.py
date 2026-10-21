"""Label-free projection of fixed cubic features onto additive controls.

Fits only a4/8-column linear information diagnostic on2043 consumed rows; no
neural training, checkpoint export, search or owner writes. Reports all panels.
"""
import importlib.util
import itertools
import json
import math
from pathlib import Path
import statistics as st
import struct
import sys
import chess

s=importlib.util.spec_from_file_location('basis',Path(__file__).with_name('probe-brn-fixed-basis-heads.py'))
basis=importlib.util.module_from_spec(s);s.loader.exec_module(basis)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def solve(matrix,rhs):
    a=[list(row)+[v] for row,v in zip(matrix,rhs)];n=len(a)
    for k in range(n):
        pivot=max(range(k,n),key=lambda i:abs(a[i][k]));a[k],a[pivot]=a[pivot],a[k]
        assert abs(a[k][k])>1e-14
        scale=a[k][k];a[k]=[x/scale for x in a[k]]
        for i in range(n):
            if i!=k:
                c=a[i][k];a[i]=[x-c*y for x,y in zip(a[i],a[k])]
    return [row[-1] for row in a]
def eigenvalues(matrix):
    a=[list(row) for row in matrix];n=len(a)
    for _ in range(100*n*n):
        p,q=max(itertools.combinations(range(n),2),key=lambda ij:abs(a[ij[0]][ij[1]]))
        if abs(a[p][q])<1e-13:break
        angle=.5*math.atan2(2*a[p][q],a[q][q]-a[p][p]);c=math.cos(angle);s=math.sin(angle)
        pp,qq,pq=a[p][p],a[q][q],a[p][q]
        a[p][p]=c*c*pp-2*s*c*pq+s*s*qq;a[q][q]=s*s*pp+2*s*c*pq+c*c*qq;a[p][q]=a[q][p]=0
        for k in range(n):
            if k not in (p,q):
                kp,kq=a[k][p],a[k][q];a[k][p]=a[p][k]=c*kp-s*kq;a[k][q]=a[q][k]=s*kp+c*kq
    return sorted(a[i][i] for i in range(n))
def main(admission_path,output):
    admission_path,output=Path(admission_path),Path(output);assert not output.exists();a=read(admission_path)
    assert a['id']=='wave9-calibrated-basis-projection-v1'
    sha=lambda p:basis.codec.sha(p.read_bytes())
    assert sha(Path(__file__))==a['scriptSha256']
    model=a['anchor'];data=basis.codec.payload(model['root'],model['checkpoint'],'network.*');assert basis.codec.sha(data)==model['networkSha256']
    w=struct.unpack('>3849d',basis.codec.decode(data,False,11)[0]);rows=[];bindings={};reference_error=0
    for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
        p=Path(a['predictions'][panel])/(source+'-predictions.jsonl');bindings[str(p)]=sha(p)
        for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
            r=json.loads(line);board=chess.Board(r['fen'])
            if not board.is_valid():continue
            pieces=board.piece_map();material=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
            unary,q,c,_=basis.features(board,w,4);anchor=unary+sum(w[3841+j]*q[j] for j in range(4))
            v=r['predictions'];g=v[a['predictor']+'-zero-heads']-.5*anchor
            reference_error=max(reference_error,abs(g-v['Gen32']))
            x=[1,material/8,(g-material)/4,anchor]
            score=material+.25*(v[a['predictor']]-material)
            rows.append(dict(panel=panel,source=source,index=i,fit=panel=='fresh5' and i<1024,
                affine=x,affineQuadratic=x+q,cubic=c,gradient=basis.fitlib.objective(score,phase,r['target'])[1]))
    assert reference_error<1e-6
    fitting=[r for r in rows if r['fit']];assert len(fitting)==2043
    reports=[]
    for mode in ['affine','affineQuadratic']:
        n=len(fitting[0][mode]);means=[0]+[st.mean(r[mode][k] for r in fitting) for k in range(1,n)]
        scales=[1]+[math.sqrt(st.mean((r[mode][k]-means[k])**2 for r in fitting)) for k in range(1,n)]
        assert min(scales)>1e-12
        standardized=lambda r:[(x-m)/s for x,m,s in zip(r[mode],means,scales)]
        fit_x=[standardized(r) for r in fitting]
        gram=[[st.mean(x[i]*x[j] for x in fit_x) for j in range(n)] for i in range(n)]
        eigen=eigenvalues(gram);ridge=[list(row) for row in gram]
        for k in range(1,n):ridge[k][k]+=1e-8
        coefficients=[solve(ridge,[st.mean(x[i]*r['cubic'][k] for x,r in zip(fit_x,fitting)) for i in range(n)]) for k in range(4)]
        assert eigen[0]>0 and abs(sum(eigen)-sum(gram[i][i] for i in range(n)))<1e-9
        for k,coef in enumerate(coefficients):
            rhs=[st.mean(x[i]*r['cubic'][k] for x,r in zip(fit_x,fitting)) for i in range(n)]
            assert max(abs(sum(v*x for v,x in zip(row,coef))-y) for row,y in zip(ridge,rhs))<1e-9
        panels=[]
        for panel,source in itertools.product(['fresh5','old'],['bt4','cp']):
            selected=[r for r in rows if r['panel']==panel and r['source']==source and r['index']>=1024]
            actual=[r['cubic'] for r in selected];predicted=[[sum(v*x for v,x in zip(coef,standardized(r))) for coef in coefficients] for r in selected]
            components=[]
            for k in range(4):
                ys=[v[k] for v in actual];ds=[v[k]-p[k] for v,p in zip(actual,predicted)];mean=st.mean(ys);variance=st.mean((y-mean)**2 for y in ys)
                components.append(dict(component=k,rawRms=math.sqrt(st.mean(y*y for y in ys)),residualRms=math.sqrt(st.mean(d*d for d in ds)),
                    outOfSampleR2=1-st.mean(d*d for d in ds)/variance,
                    meanGradientTimesResidual=st.mean(r['gradient']*d for r,d in zip(selected,ds))))
            h=a['learnedCubicHeads'];ys=[.125*sum(x*w for x,w in zip(v,h)) for v in actual]
            ds=[.125*sum((x-y)*w for x,y,w in zip(v,p,h)) for v,p in zip(actual,predicted)]
            panels.append(dict(panel=panel,source=source,count=len(selected),components=components,
                learnedCubicRmsPawns=math.sqrt(st.mean(y*y for y in ys)),remainingCubicRmsPawns=math.sqrt(st.mean(d*d for d in ds))))
        reports.append(dict(span=mode,featureMeans=means,featureScales=scales,standardizedGramEigenvalues=eigen,
            standardizedGramCondition=eigen[-1]/eigen[0],projectionCoefficients=coefficients,panels=panels))
    result=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=sha(admission_path),inputs=bindings,
        fitRows=len(fitting),ridge=1e-8,maximumCompiledTrainerReferenceDifference=reference_error,projections=reports,
        limitations=['Label-free information projection on consumed panels; no neural training, exported model or owner writes.',
        'Explained feature variance does not establish predictive relevance, strength, optimal Adam convergence or an architecture defect.',
        'Out-of-sample here means only a held-out half of already consumed diagnostic data; game/source/training overlap remains unknown.',
        'Standardized feature Gram conditioning is not the full loss Hessian or a proof that preconditioning would improve chess.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print('Completed label-free affine/affine-plus-quad projections of cubic features')
if __name__=='__main__':main(*sys.argv[1:])
