"""SR-001I offline screens only. No Search policy execution or savings estimate.
Nearest-rank percentiles; repeated visits are observations, not independent positions.
H's common fields are compared byte-for-field before any numeric conversion.
"""
from pathlib import Path
import collections, csv, gzip, json, math, re

ROOT=Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/research/search'
PREFIX='SEARCH_QUIESCENCE_POSTMOVE_SR001I_2026-09-28'
H='SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28'
def read(prefix,suffix):
    p=EVIDENCE/(prefix+'_'+suffix+'.csv'+('.gz' if suffix=='OBSERVATIONS' else ''))
    with (gzip.open(p,'rt',encoding='utf-8') if p.suffix=='.gz' else p.open(encoding='utf-8-sig',newline='')) as f:
        yield from csv.DictReader(f)
def write(suffix,rows):
    rows=list(rows)
    with (EVIDENCE/(PREFIX+'_'+suffix+'.csv')).open('w',encoding='utf-8',newline='') as f:
        w=csv.DictWriter(f,list(rows[0]),lineterminator='\n');w.writeheader();w.writerows(rows)
def yes(r,k):return r[k]=='true'
def relevant(r):return yes(r,'raised_alpha') or yes(r,'final_best_move')
def draw(r):return r['selected_line_cause'] in ('stalemate','insufficient','rule50','repetition')
def quantile(v,q):return sorted(v)[max(0,math.ceil(len(v)*q)-1)] if v else ''
def distribution(group,metric,rows):
    v=[int(r[metric]) for r in rows]
    return dict(group=group,metric=metric,count=len(v),minimum=min(v) if v else '',median=quantile(v,.5),
        p90=quantile(v,.9),p95=quantile(v,.95),p99=quantile(v,.99) if len(v)>=100 else '',maximum=max(v) if v else '',positive=sum(x>0 for x in v))

rows=list(read(PREFIX,'OBSERVATIONS'));runs=list(read(PREFIX,'RUNS'));oracles=list(read(PREFIX,'ORACLE'))
common=[r for r in rows if r['fixture']!='capture-to-stalemate']
assert len(common)==195921
for new,old in zip(common,read(H,'OBSERVATIONS'),strict=True):
    assert all(new[k]==v for k,v in old.items()),(new['run'],new['observation'],'H trace changed')
old_runs={r['run']:r for r in read(H,'RUNS')}
identities=[]
for r in runs:
    assert r['identity']=='identical' and yes(r,'completed')
    if r['run'] not in old_runs:continue
    old=old_runs[r['run']]
    fields=('completed_depth','completed','score','best','pv','normal_nodes','qnodes','total_nodes','max_qply','observations','oracle','oracle_nodes')
    assert all(r[k]==old[k] for k in fields),(r['run'],'H run changed')
    identities.append(dict(run=r['run'],off_on='identical',retained_H='identical',score=r['score'],best=r['best'],pv=r['pv'],normal_nodes=r['normal_nodes'],qnodes=r['qnodes'],max_qply=r['max_qply'],completed=r['completed']))
write('IDENTITY',identities)

production=[]
retained={(r['fixture'],r['mode'],r['depth']):r for r in read(H,'IDENTITY')}
for depth in (2,3):
    text=(ROOT/f'app/build/sr001i-clean-depth{depth}.txt').read_text(encoding='utf-16')
    stats={(n,m):(normal,q,total,maxq) for n,m,normal,q,total,maxq in re.findall(
        r'qsearch position=(\S+) mode=(\S+) normal_nodes=(\d+) qnodes=(\d+) total_nodes=(\d+) max_qply=(\d+)',text)}
    pattern=r'position=(\S+) requested=(\d+) completed=(-?\d+) best=(\S+) score=(-?\d+) pv=\[([^\]]*)\] nodes=(\d+) median_ms=([\d.]+) nps=(\d+) ordering=(\S+)'
    found=re.findall(pattern,text);assert len(found)==14
    for name,requested,completed,best,score,pv,nodes,ms,nps,mode in found:
        old=retained[(name,mode,requested)];normal,q,total,maxq=stats[(name,mode)]
        assert requested==completed
        assert (best,score,pv,nodes,normal,q,maxq)==tuple(old[k] for k in ('best','score','pv','total_nodes','normal_nodes','qnodes','max_qply'))
        production.append(dict(fixture=name,mode=mode,depth=depth,score=score,best=best,pv=pv,normal_nodes=normal,qnodes=q,total_nodes=total,max_qply=maxq,
            retained_H_identity='identical',completed=True,retained_G_clean_ms=old['retained_G_clean_ms'],retained_G_nps=old['retained_G_nps'],retained_warmups=old['retained_warmups'],retained_repetitions=old['retained_repetitions']))
write('PRODUCTION_IDENTITY',production)

for r in rows:
    assert all(yes(r,k) for k in ('move_completed','node_completed','invocation_completed'))
    for k in ('alpha','beta','stand_pat','gain','alpha_gap','move_score','post_move_static','static_gap','continuation_gain','child_nodes'):
        if r[k]!='':r[k]=int(r[k])
primary=[r for r in rows if yes(r,'primary_eligible')]
terminals=[r for r in rows if not yes(r,'primary_eligible')]
write('TERMINAL_CHILDREN',terminals)
for r in primary:
    assert r['static_gap']==r['alpha']-r['post_move_static']
    assert r['continuation_gain']==r['move_score']-r['post_move_static']<=0
    if r['static_gap']>=0:
        assert r['child_nodes']==1 and yes(r,'child_stand_pat_cutoff') and r['continuation_gain']==0
        assert not yes(r,'raised_alpha') and not yes(r,'beta_cutoff')

# Full oracle rows include terminal-child protection separately. Duplicated positions
# may be linked only when the exact value, static value and relationship agree.
def key(r):return tuple(r[k] for k in ('fixture','node_fen','qply','path_ply','move'))
exact={}
for r in oracles:
    if key(r) in exact:
        assert all(r[k]==exact[key(r)][k] for k in ('move_exact_score','node_exact_best','relation_to_best','post_move_static','child_kind'))
    exact[key(r)]=r
links=[]
for r in rows:
    o=exact.get(key(r))
    if o is None:continue
    assert str(r['post_move_static'])==o['post_move_static'] and r['child_kind']==o['child_kind']
    value=int(o['move_exact_score'])
    if r['move_bound']=='UPPER':assert value<=r['move_score']
    elif r['move_bound']=='LOWER':assert value>=r['move_score']
    else:assert value==r['move_score']
    if yes(r,'primary_eligible'):assert value<=r['post_move_static']
    r['oracle_relation']=o['relation_to_best'];r['oracle_score']=int(o['move_exact_score'])
    r['oracle_unique_draw']=o['unique_draw'];r['oracle_unique_mate']=o['unique_mate']
    links.append(dict(run=r['run'],observation=r['observation'],fixture=r['fixture'],node_fen=r['node_fen'],qply=r['qply'],path_ply=r['path_ply'],move=r['move'],alpha=r['alpha'],beta=r['beta'],child_kind=r['child_kind'],
        post_move_static=r['post_move_static'],static_gap=r['static_gap'],alpha_gap=r['alpha_gap'],move_score=r['move_score'],move_bound=r['move_bound'],
        move_exact_score=o['move_exact_score'],node_exact_best=o['node_exact_best'],relation=o['relation_to_best'],unique_draw=o['unique_draw'],unique_mate=o['unique_mate'],
        exact_continuation_gain=o['continuation_gain'],exact_above_visited_alpha=int(o['move_exact_score'])>r['alpha']))
write('ORACLE_LINKS',links)

groups={'all':primary,'alpha_raising':[r for r in primary if yes(r,'raised_alpha')],
    'beta_cutoff':[r for r in primary if yes(r,'beta_cutoff')],
    'locally_relevant':[r for r in primary if relevant(r)],
    'alpha_raising_positive_gap':[r for r in primary if yes(r,'raised_alpha') and r['static_gap']>0],
    'locally_relevant_positive_gap':[r for r in primary if relevant(r) and r['static_gap']>0],
    'en_passant':[r for r in primary if yes(r,'en_passant')],
    'SEE_negative':[r for r in primary if r['see_sign']=='-1'],
    'same_target_recapture':[r for r in primary if yes(r,'same_target_recapture')],
    'clearance_fixture':[r for r in primary if 'clearance' in r['fixture']],
    'selected_mate_line':[r for r in primary if r['selected_line_cause']=='mate'],
    'selected_draw_line':[r for r in primary if draw(r)],
    'benchmark':[r for r in primary if r['run'].startswith('benchmark:')],
    'corpus':[r for r in primary if r['run'].startswith('corpus:')]}
dist=[distribution(g,m,rs) for g,rs in groups.items() for m in ('static_gap','continuation_gain','alpha_gap')]
dist.append(distribution('H_aligned_alpha_raising_positive_material_gap','alpha_gap',
    [r for r in primary if yes(r,'raised_alpha') and r['alpha_gap']>0]))
for rel in ('all','uniquely-best','equal-best','below'):
    o=[r for r in oracles if yes(r,'primary_eligible') and (rel=='all' or r['relation_to_best']==rel)]
    dist.append(distribution('oracle_'+rel,'continuation_gain',o))
write('DISTRIBUTIONS',dist)

# No positive protective gap exists for an alpha raise. Additional screen margins
# therefore come from *non-improving* positive gaps, not a claimed safety calibration.
positive=[r['static_gap'] for r in primary if r['static_gap']>0]
local_max=max(r['static_gap'] for r in primary if relevant(r))
margins=sorted(set([0,quantile(positive,.5),quantile(positive,.9),local_max,local_max+1]))
screens=[]
for predictor,metric,ms in [('post_static','static_gap',margins),('material_delta','alpha_gap',[0,67,269,354,520])]:
    for margin in ms:
        for inclusive in (False,True):
            selected=[r for r in primary if r[metric]>margin or inclusive and r[metric]==margin]
            screens.append(dict(predictor=predictor,convention='inclusive_ge' if inclusive else 'strict_gt',margin=margin,eligible=len(primary),predicted_pruned=len(selected),pruned_pct=round(100*len(selected)/len(primary),4),
                alpha_raises=sum(yes(r,'raised_alpha') for r in selected),beta_cutoffs=sum(yes(r,'beta_cutoff') for r in selected),
                final_best_fail_low=sum(yes(r,'final_best_move') and not yes(r,'raised_alpha') for r in selected),
                mate_lines=sum(r['selected_line_cause']=='mate' for r in selected),relevant_mate=sum(relevant(r) and r['selected_line_cause']=='mate' for r in selected),
                draw_lines=sum(draw(r) for r in selected),relevant_draw=sum(relevant(r) and draw(r) for r in selected),
                oracle_best=sum(r.get('oracle_relation') in ('uniquely-best','equal-best') for r in selected),
                oracle_uniquely_best=sum(r.get('oracle_relation')=='uniquely-best' for r in selected),
                oracle_above_alpha=sum(r.get('oracle_score',-99999)>r['alpha'] for r in selected),
                existing_child_stand_pat_cutoffs=sum(yes(r,'child_stand_pat_cutoff') for r in selected)))
write('MARGINS',screens)

# The twelve H component/oracle witnesses, plus exact/terminal/bound diagnostics.
by_id={(r['run'],r['observation']):r for r in rows}
witnesses=[]
def witness(r,classification):
    witnesses.append({**{k:r[k] for k in ('run','fixture','observation','node_fen','qply','path_ply','move','see_sign','en_passant','alpha','beta','stand_pat','gain','alpha_gap','post_move_static','static_gap','move_score','move_bound','continuation_gain','raised_alpha','beta_cutoff','final_best_move','node_final_score','selected_line_cause','child_kind','child_nodes','child_stand_pat_cutoff')},'classification':classification,
        'oracle_relation':r.get('oracle_relation',''),'oracle_exact_value':r.get('oracle_score','')})
for h in read(H,'WITNESS_VALUES'):
    r=by_id[(h['run'],h['observation'])]
    witness(r,'retained-H-witness')
    witnesses[-1]['oracle_relation']=h['relation_to_best'];witnesses[-1]['oracle_exact_value']=h['exact_move_score']
witness(min(primary,key=lambda r:r['continuation_gain']),'largest-negative-gain; later mate against capturer; bound-only')
witness(max((r for r in primary if relevant(r)),key=lambda r:r['static_gap']),'largest-final-best-fail-low-gap; returned-bound relevance')
for r in terminals:witness(r,'protected-immediate-'+r['child_kind'])
write('WITNESSES',witnesses)

summary=dict(runs=len(runs),H_runs_identical=len(identities),H_observations_identical=len(common),observations=len(rows),primary=len(primary),
    immediate_children=dict(collections.Counter(r['child_kind'] for r in terminals)),
    oracle_matched=sum(r['oracle']=='matched' for r in runs),oracle_rejected=sum(r['oracle']=='guard-rejected-not-exact' for r in runs),
    oracle_move_rows=len(oracles),oracle_primary=sum(yes(r,'primary_eligible') for r in oracles),oracle_links=len(links),
    positive_continuation_gains=sum(r['continuation_gain']>0 for r in primary),margins=margins,
    strict_zero_existing_cutoffs=sum(r['static_gap']>0 for r in primary),inclusive_zero_existing_cutoffs=sum(r['static_gap']>=0 for r in primary),
    alpha_raising=sum(yes(r,'raised_alpha') for r in primary),beta_cutoffs=sum(yes(r,'beta_cutoff') for r in primary),max_qply=max(int(r['max_qply']) for r in runs))
print(json.dumps(summary,indent=2))
