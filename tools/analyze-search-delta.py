"""SR-001H recorded-evidence analysis only. Never executes a pruning policy.

Run after search-delta-diagnostics.py. Screens strict gap > margin (the task's
explicit gap interpretation) and separately inclusive gap >= margin (its written
<= formula). Percentiles use nearest rank. Repeat visits remain observations,
not independent chess positions. No counterfactual node-saving estimate is made.
"""
from pathlib import Path
import base64, collections, csv, gzip, json, math, re, struct

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/research/search'
PREFIX = "SEARCH_QUIESCENCE_DELTA_SR001H_2026-09-28"
def path(suffix): return EVIDENCE / (PREFIX + "_" + suffix + ".csv")
def read(p):
    with (gzip.open(p, "rt", encoding="utf-8") if str(p).endswith(".gz") else p.open(encoding="utf-8-sig", newline="")) as f:
        return list(csv.DictReader(f))
def write(suffix, rows):
    with path(suffix).open("w", encoding="utf-8", newline="") as f:
        w=csv.DictWriter(f, list(rows[0]), lineterminator="\n"); w.writeheader(); w.writerows(rows)
def quantile(values, q): return sorted(values)[max(0, math.ceil(len(values)*q)-1)] if values else ""
def yes(r, key): return r[key] == "true"
def relevant(r): return yes(r,"raised_alpha") or yes(r,"final_best_move")
def draw(r): return r["selected_line_cause"] in ("stalemate","rule50","repetition","insufficient")

runs=read(path("RUNS")); run_by_id={r["run"]:r for r in runs}
rows=read(Path(str(path("OBSERVATIONS"))+".gz"))
for r in rows:
    for k in ("alpha_gap","alpha","beta","gain","stand_pat","move_score","qply","path_ply"):
        if r[k]: r[k]=int(r[k])
    assert r["alpha_gap"] == r["alpha"]-r["stand_pat"]-r["gain"]
    assert yes(r,"move_completed") and yes(r,"node_completed") and yes(r,"invocation_completed")
    assert yes(r,"raised_alpha") == (r["move_score"]>r["alpha"])
    assert yes(r,"beta_cutoff") == (r["move_score"]>=r["beta"])

# Recorded identities cover values, PVs and every deterministic tree statistic.
g_corpus=read(EVIDENCE/"SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_CORPUS.csv")
retained={r["fixture"]:r for r in g_corpus if r["mode"]=="QSEARCH_BASELINE"}
freeze=0
for r in runs:
    if r["kind"]=="corpus-full" and r["fixture"] in retained:
        old=retained[r["fixture"]]
        for a,b in (("score","score"),("best","best"),("pv","pv"),("qnodes","qnodes"),("max_qply","max_qply"),("completed","completed")):
            assert r[a]==old[b],(r["run"],a,r[a],old[b])
        freeze+=1
g_bench=read(EVIDENCE/"SEARCH_QUIESCENCE_FORCING_SR001G_2026-09-28_BENCHMARK.csv")
frozen_bench=0
for r in runs:
    if r["kind"]!="benchmark":continue
    old=next(b for b in g_bench if b["position"]==r["fixture"] and b["requested_depth"]==r["depth"] and b["mode"]=="QSEARCH_BASELINE" and b["run_set"]=="primary")
    for a,b in (("score","score"),("best","best_move"),("pv","pv"),("qnodes","qnodes"),("normal_nodes","normal_nodes"),("total_nodes","total_nodes"),("max_qply","max_qply"),("completed","completed")):
        assert r[a]==old[b],(r["run"],a,r[a],old[b])
    frozen_bench+=1

groups={"all":rows,"benchmark":[r for r in rows if r["run"].startswith("benchmark:")],
        "corpus_full":[r for r in rows if run_by_id[r["run"]]["kind"]=="corpus-full"],
        "corpus_narrow":[r for r in rows if run_by_id[r["run"]]["kind"] in ("corpus-lower","corpus-upper")]}
positive=[r["alpha_gap"] for r in rows if yes(r,"raised_alpha") and r["alpha_gap"]>0]
margins=sorted(set([0,quantile(positive,.5),quantile(positive,.95),quantile(positive,.99),max(positive)]))
screens=[]; distributions=[]
for name, group in groups.items():
    for label,predicate in (("alpha_raising",lambda r:yes(r,"raised_alpha")),("locally_relevant",relevant),
            ("alpha_raising_positive_gap",lambda r:yes(r,"raised_alpha") and r["alpha_gap"]>0),
            ("locally_relevant_positive_gap",lambda r:relevant(r) and r["alpha_gap"]>0),
            ("selected_draw_line",draw),("selected_mate_line",lambda r:r["selected_line_cause"]=="mate")):
        v=[r["alpha_gap"] for r in group if predicate(r)]
        distributions.append(dict(group=name,population=label,count=len(v),minimum=min(v) if v else "",
            median=quantile(v,.5),p90=quantile(v,.9),p95=quantile(v,.95),p99=quantile(v,.99) if len(v)>=100 else "",
            maximum=max(v) if v else "",positive=sum(x>0 for x in v)))
    for margin in margins:
        for convention in ("strict_gap_gt_margin","inclusive_gap_ge_margin"):
            predicted=[r for r in group if r["alpha_gap"]>margin or convention.startswith("inclusive") and r["alpha_gap"]==margin]
            screens.append(dict(group=name,convention=convention,margin=margin,eligible=len(group),
                predicted_pruned=len(predicted),pruned_pct=round(100*len(predicted)/len(group),4),
                locally_relevant=sum(relevant(r) for r in predicted),alpha_raising=sum(yes(r,"raised_alpha") for r in predicted),
                beta_cutoffs=sum(yes(r,"beta_cutoff") for r in predicted),
                final_best_upper_bounds=sum(yes(r,"final_best_move") and not yes(r,"raised_alpha") for r in predicted),
                selected_mate_lines=sum(r["selected_line_cause"]=="mate" for r in predicted),
                relevant_mate_lines=sum(r["selected_line_cause"]=="mate" and relevant(r) for r in predicted),
                selected_draw_lines=sum(draw(r) for r in predicted),
                relevant_draw_lines=sum(draw(r) and relevant(r) for r in predicted)))
write("DISTRIBUTIONS",distributions);write("MARGINS",screens)

# Keep one witness per distinct observed board/window/move/outcome, with multiplicity.
# Raw observation links retain the complete visits. These tags are descriptive, not safeguards.
witnesses={}
for r in rows:
    if r["alpha_gap"]<=0 or not (yes(r,"raised_alpha") or draw(r) or r["selected_line_cause"]=="mate"):continue
    key=tuple(r[k] for k in ("node_fen","move","alpha","beta","move_score","selected_line_cause"))
    if key in witnesses: witnesses[key]["occurrences"]+=1;continue
    tags=[]
    if yes(r,"en_passant"):tags.append("en-passant")
    if yes(r,"same_target_recapture"):tags.append("same-target-q-recapture")
    if r["see_sign"]=="-1":tags.append("SEE-negative")
    if r["selected_line_cause"]!="static":tags.append(r["selected_line_cause"])
    if "clearance" in r["fixture"]:tags.append("clearance-fixture")
    if "promotion" in r["fixture"]:tags.append("promotion-context-not-promotion-move")
    if not tags:tags.append("ordinary-nonchecking-capture")
    witnesses[key]={k:r[k] for k in ("run","fixture","observation","qply","path_ply","node_fen","move","gain","see_sign","alpha","beta","stand_pat","alpha_gap","move_score","move_bound","raised_alpha","beta_cutoff","best_changed","final_best_move","selected_line_cause","node_final_score")}
    witnesses[key].update(classification=";".join(tags),occurrences=1)
write("WITNESSES",sorted(witnesses.values(),key=lambda r:-r["alpha_gap"]))

# Decode the immutable HCE material blob exactly as EvalTuning does; no score conversion.
s=(ROOT/'app/src/main/java/com/ohinteractive/seedv6/core/EvalTuning.java').read_text()
v=struct.unpack('>125H',gzip.decompress(base64.b64decode(re.search(r'MATERIAL_DATA\s*=\s*"([^"]+)',s)[1])))
domains=[]
for i,(piece,exchange) in enumerate(zip(("queen","rook","bishop","knight","pawn"),(975,500,330,320,100))):
    a=v[25*i:25*(i+1)]
    domains.append(dict(domain=piece,ordering_and_SEE=exchange,HCE_min=min(a),HCE_max=max(a),HCE_opening=a[0],HCE_endgame=a[-1],
        score_range="HCE total +/-30000",interpretation="Approximately comparable HCE units; phase/positional changes are not bounded by exchange value",source="core/Eval.java;core/EvalTuning.java"))
for name,range_,meaning,source in (
    ("ExactEvaluator","+/-32511 nonmate","Side-to-move integer and mate separation only; no material-unit calibration contract","search/exact/ExactEvaluator.java"),
    ("NNUE V1","+/-32511","Uncalibrated bounded tanh output times32511; magnitude round/minimum1; explicitly not centipawns","search/evaluation/NnueScoreMapping.java"),
    ("BRN0/1/2","+/-32511","Uncalibrated bounded WDL/value output times32511; magnitude round/minimum1; not centipawns","search/evaluation/BrnScoreMapping.java;SearchEvaluation.java"),
    ("mate","32512..32768 and negative band","Actual root/path ply; material addition is not a mate-distance bound","search/tt/TranspositionScores.java")):
    domains.append(dict(domain=name,ordering_and_SEE="",HCE_min="",HCE_max="",HCE_opening="",HCE_endgame="",score_range=range_,interpretation=meaning,source=source))
write("DOMAINS",domains)

# A small deterministic exact-state selection for component/oracle follow-up.
selected=[]
for fixture in ("nonchecking-clearance","ep-winning","ep-defended","three-capture-xray","nonchecking-capture-dead-draw","promotion-stalemate-resource"):
    selected.append(max((r for r in rows if r["fixture"]==fixture and yes(r,"raised_alpha") and r["alpha_gap"]>0),key=lambda r:r["alpha_gap"]))
seen=set()
for r in sorted(groups["benchmark"],key=lambda r:-r["alpha_gap"]):
    key=(r["node_fen"],r["move"])
    if yes(r,"raised_alpha") and key not in seen:
        selected.append(r);seen.add(key)
        if len(seen)==5:break
selected.append(next(r for r in rows if yes(r,"raised_alpha") and r["alpha_gap"]>0 and draw(r) and r["fixture"]!="nonchecking-capture-dead-draw"))
with (ROOT/'app/build/sr001h-selected.tsv').open('w',encoding='utf-8',newline='') as f:
    for r in selected:f.write('\t'.join(str(r[k]) for k in ('run','fixture','observation','qply','path_ply','node_fen','move','alpha','beta','alpha_gap','move_score'))+'\n')

summary=dict(observations=len(rows),runs=len(runs),frozen_corpus=freeze,frozen_benchmarks=frozen_bench,
    oracle_status=dict(collections.Counter(r['oracle'] for r in runs if r['kind']=='corpus-full')),
    margins=margins,distinct_witnesses=len(witnesses),positive_alpha_raises=len(positive),
    positive_raise_classes={"ep":sum(yes(r,'en_passant') and yes(r,'raised_alpha') and r['alpha_gap']>0 for r in rows),
        "same_target_recapture":sum(yes(r,'same_target_recapture') and yes(r,'raised_alpha') and r['alpha_gap']>0 for r in rows),
        "negative_see":sum(r['see_sign']=='-1' and yes(r,'raised_alpha') and r['alpha_gap']>0 for r in rows)})
print(json.dumps(summary,indent=2))
for r in distributions:
    if r['group']=='all':print(r)
for r in screens:
    if r['group']=='all' and r['convention'].startswith('strict'):print(r)

# Link visited corpus moves to fully enumerated values, without relabelling bounds.
oracle=read(path('ORACLE'));exact_index=collections.defaultdict(list)
for r in oracle:exact_index[(r['fixture'],r['node_fen'],int(r['qply']),r['move'])].append(r)
links=[];ambiguous=0
for r in rows:
    if not r['run'].startswith('corpus:'):continue
    matches=exact_index.get((r['fixture'],r['node_fen'],r['qply'],r['move']),[])
    if not matches:continue
    if len({(m['move_exact_score'],m['node_exact_best'],m['relation_to_best']) for m in matches})!=1:
        ambiguous+=1;continue
    m=matches[0];true=int(m['move_exact_score']);observed=r['move_score']
    assert (true<=observed if observed<=r['alpha'] else true>=observed if observed>=r['beta'] else true==observed),(r,m)
    links.append(dict(run=r['run'],fixture=r['fixture'],observation=r['observation'],qply=r['qply'],node_fen=r['node_fen'],
        move=r['move'],alpha=r['alpha'],beta=r['beta'],alpha_gap=r['alpha_gap'],observed_move_score=observed,observed_bound=r['move_bound'],
        exact_move_score=true,exact_node_best=m['node_exact_best'],relation_to_best=m['relation_to_best'],
        unique_mate=m['unique_mate'],unique_draw=m['unique_draw'],selected_line_cause=m['selected_line_cause']))
write('ORACLE_LINKS',links)
print('oracle links',len(links),'ambiguous histories not linked',ambiguous,'positive-gap',sum(r['alpha_gap']>0 for r in links),
      'positive-gap true alpha raises',sum(r['alpha_gap']>0 and r['exact_move_score']>r['alpha'] for r in links))

# Independent production JVM harness records (no shadow class on its classpath).
identity=[]
for depth in (2,3):
    text=(ROOT/f'app/build/sr001h-clean-depth{depth}.txt').read_text(encoding='utf-16')
    # Windows PowerShell redirection is UTF-16; parsing remains explicit.
    stats={(n,m):(int(normal),int(q),int(total),int(maxq)) for n,m,normal,q,total,maxq in re.findall(
        r'qsearch position=(\S+) mode=(\S+) normal_nodes=(\d+) qnodes=(\d+) total_nodes=(\d+) max_qply=(\d+)',text)}
    pattern=r'position=(\S+) requested=(\d+) completed=(-?\d+) best=(\S+) score=(-?\d+) pv=\[([^\]]*)\] nodes=(\d+) median_ms=([\d.]+) nps=(\d+) ordering=(\S+)'
    found=re.findall(pattern,text);assert len(found)==14,len(found)
    for name,requested,completed,best,score,pv,nodes,ms,nps,mode in found:
        old=next(b for b in g_bench if b['position']==name and b['requested_depth']==requested and b['mode']==mode and b['run_set']=='primary')
        normal,q,total,maxq=stats[(name,mode)]
        assert (completed,best,score,pv,nodes)==tuple(old[k] for k in ('completed_depth','best_move','score','pv','total_nodes'))
        assert (normal,q,total,maxq)==tuple(int(old[k]) for k in ('normal_nodes','qnodes','total_nodes','max_qply'))
        identity.append(dict(fixture=name,mode=mode,depth=requested,score=score,best=best,pv=pv,normal_nodes=normal,qnodes=q,
            total_nodes=total,max_qply=maxq,retained_G_identity='identical',production_vs_shadow='identical' if mode=='QSEARCH_BASELINE' else 'production-control',
            retained_G_clean_ms=old['median_ms'],retained_G_nps=old['nps'],retained_warmups=old['warmups'],retained_repetitions=old['repetitions']))
write('IDENTITY',identity)
print('independent production JVM / G identity rows',len(identity))
