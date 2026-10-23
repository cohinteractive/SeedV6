"""Fresh development selection for the single gated adaptive formulation."""
import importlib.util
import json
from run_diagnostic_v1 import digest, write_new, ROOT, HERE

spec=importlib.util.spec_from_file_location('baseline',ROOT/'tools/search-baseline.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
assert json.loads((HERE/'adaptive-uci-qualification-v1.json').read_text())['status']=='passed'
binding=json.loads((HERE/'clock-runtime-v2-binding.json').read_text())
assert all(digest(p)==sha for p,sha in binding['files'].items())
assert digest(ROOT/'tools/p3-tt-match.py')==binding['adapterSha256']
book=ROOT.parent/'seedv6/app/build/search-baseline/tools/openings/sr021-bjbraams/legal16-v1.pgn'
assert digest(book)=='eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f'
exclude=[HERE/'clock-development-exclusions.json',HERE/'pilot-v1-openings.selection.json',
         HERE/'development-clock-v1-openings.selection.json',
         ROOT/'docs/research/search/p3-construction-2026-10-20/baseline-cp64/openings.selection.json']
b.select_openings(book,HERE/'adaptive-clock-v1-openings.pgn',32,2026102303,0,exclude)
files={str(p):digest(p) for p in [HERE/'adaptive-clock-v1.json',HERE/'adaptive-clock-v1-openings.pgn',
       HERE/'adaptive-clock-v1-openings.selection.json',HERE/'adaptive-v1-summary.json',
       HERE/'adaptive-uci-qualification-v1.json',ROOT/'tools/p3-tt-match.py',ROOT/'build/research/tools/fastchess.exe',
       ROOT/'build-assets/nnue-candidate/conventional-cp64.cnnue']}
write_new(HERE/'adaptive-clock-v1-binding.json',{'files':files,'runtimeBindingSha256':digest(HERE/'clock-runtime-v2-binding.json'),
    'selectionScriptSha256':digest(__file__),'qualificationGate':'passed'})
print('Adaptive clock protocol admitted',flush=True)
