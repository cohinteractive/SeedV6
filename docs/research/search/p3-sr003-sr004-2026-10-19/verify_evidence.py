"""Closeout audit and local raw manifest. Never mutates prior protocols or experiments."""
import hashlib,json,pathlib,subprocess,xml.etree.ElementTree as ET
repo=pathlib.Path(__file__).resolve().parents[4];base=pathlib.Path(__file__).resolve().parent
def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
for name in ['clock-screen-v1.json','clock-screen-v2.json','clock-screen-v3.json']:
    p=json.loads((base/name).read_text())
    assert all(digest(pathlib.Path(f))==h for f,h in p['frozenFiles'].items()),name
p=json.loads((base/'heldout-cost-v1.json').read_text())
assert all(digest(repo/f)==h for f,h in p['sourceHashes'].items())
failed=json.loads((base/'raw/clock-screen-w4-v1/run.json').read_text())
assert failed['status']=='failed' and (base/'raw/clock-screen-w4-v1/games.pgn').stat().st_size==0
failed2=json.loads((base/'raw/clock-screen-w4-v2/run.json').read_text())
assert failed2['status']=='failed' and not (base/'raw/clock-screen-w4-v2/games.pgn').exists()
completed=json.loads((base/'raw/clock-screen-w4-v3/run.json').read_text())
assert completed['status']=='passed' and completed['inputsStable']
assert all(digest(pathlib.Path(f))==h for f,h in completed['filesBefore'].items())
for f in base.glob('*.json'):json.loads(f.read_text())
for f in (base/'raw').glob('*.jsonl'):
    rows=[json.loads(s) for s in f.read_text().splitlines()]
    assert rows[-1].get('status')=='passed',f
validation={}
for name in ['final-search-v1','final-core-uci-v1']:
    summary=json.loads((base/(name+'-summary.json')).read_text());assert summary['exitCode']==0
    totals={k:0 for k in ['tests','failures','errors','skipped']}
    for suite in summary['suites']:
        f=base/suite['path'];assert digest(f)==suite['sha256']
        root=ET.parse(f).getroot()
        for key in totals:totals[key]+=int(root.attrib[key])
    assert totals==summary['totals'] and all(totals[k]==0 for k in ['failures','errors','skipped'])
    validation[name]=totals
files=sorted(f for f in (base/'raw').rglob('*') if f.is_file())
relative=[f.relative_to(repo).as_posix() for f in files]
listed=subprocess.run(['git','check-ignore','--stdin','-z'],cwd=repo,input=('\0'.join(relative)+'\0').encode(),capture_output=True,check=True)
assert set(listed.stdout.decode().strip('\0').split('\0'))==set(relative),'Raw evidence is not all ignored'
assert not subprocess.check_output(['git','ls-files','--',str((base/'raw').relative_to(repo))],cwd=repo).strip()
manifest={'localOnly':True,'availability':'All listed files locally available and hash-verified at closeout; fresh Git checkout does not contain raw evidence.',
          'rawCount':len(files),'rawBytes':sum(f.stat().st_size for f in files),'validation':validation,
          'files':{str(f.relative_to(base)):{'bytes':f.stat().st_size,'sha256':digest(f)} for f in files}}
target=base/'evidence-manifest.json'
if target.exists():raise SystemExit('Refusing to overwrite a closeout manifest')
target.write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({k:v for k,v in manifest.items() if k!='files'}))
