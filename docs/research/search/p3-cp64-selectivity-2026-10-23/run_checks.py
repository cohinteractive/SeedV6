"""Preserve exact Gradle exit and JUnit evidence, avoiding PowerShell stderr coercion."""
import hashlib,json,pathlib,shutil,subprocess,sys,time,xml.etree.ElementTree as ET
base=pathlib.Path(__file__).resolve().parent;raw=base/'raw';name=sys.argv[1]
log=raw/(name+'.log');receipt=base/(name+'-summary.json')
if log.exists() or receipt.exists():raise SystemExit('Refusing to overwrite validation')
cmd=['cmd.exe','/c','gradlew.bat','--gradle-user-home','build/migration/gradle-user-home','--no-daemon','--max-workers=1']+sys.argv[2:]
start=time.monotonic()
with log.open('xb') as out:
    run=subprocess.run(cmd,stdout=out,stderr=subprocess.STDOUT,timeout=1800,
        creationflags=subprocess.BELOW_NORMAL_PRIORITY_CLASS|subprocess.CREATE_NO_WINDOW)
results=[]
for project in ['research','engine-core','app-uci']:
    if ':'+project+':test' not in sys.argv:continue
    for f in pathlib.Path(project+'/build/test-results/test').glob('TEST-*.xml'):
        target=raw/(name+'-'+project+'-'+f.name);shutil.copyfile(f,target)
        results.append({'path':str(target.relative_to(base)),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),**ET.parse(f).getroot().attrib})
record={'command':cmd,'exitCode':run.returncode,'elapsedSeconds':time.monotonic()-start,'suites':results,
        'totals':{k:sum(int(x[k]) for x in results) for k in ['tests','failures','errors','skipped']}}
receipt.write_text(json.dumps(record,indent=2)+'\n');print(json.dumps({k:v for k,v in record.items() if k!='suites'}))
raise SystemExit(run.returncode)
