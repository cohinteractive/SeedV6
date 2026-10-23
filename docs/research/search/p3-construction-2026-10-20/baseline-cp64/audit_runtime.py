"""Audit preserved comparison classes against the final native package, without execution."""
import argparse
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--frozen',type=Path,required=True)
    parser.add_argument('--packaged',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    assert not args.output.exists(), 'Preserve prior audit'
    results={}
    allowed={
      'com/ohinteractive/seedv6/search/driver/BaselineEngine.class',
      'com/ohinteractive/seedv6/search/driver/ConventionalNnueEngine.class',
      'com/ohinteractive/seedv6/search/exact/FlatSearchConfig.class'}
    for name in ('engine-core.jar','app-uci.jar'):
        old=args.frozen/name; new=args.packaged/name
        with ZipFile(old) as a, ZipFile(new) as b:
            names_a=set(a.namelist()); names_b=set(b.namelist())
            assert names_a==names_b, (name,'entry-set change',names_a^names_b)
            changed=[n for n in sorted(names_a) if a.read(n)!=b.read(n)]
            classes=[n for n in changed if n.endswith('.class')]
            assert set(classes)<=allowed if name=='engine-core.jar' else not classes
            resources=[n for n in changed if not n.endswith('.class')]
            assert set(resources)<={'com/ohinteractive/seedv6/application-version.properties'} if name=='engine-core.jar' else not resources
            results[name]=dict(frozen_sha256=sha(old),packaged_sha256=sha(new),
                total_classes=sum(n.endswith('.class') for n in names_a),
                changed_classes=classes,changed_resources=resources)
    output=dict(status='PASS',frozen_directory=str(args.frozen),packaged_directory=str(args.packaged),jars=results,
       interpretation='All other core and UCI class bytes match the frozen comparison. Review the named factory identity strings and validation-message-only source diff separately; network resources must remain identical.')
    args.output.write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(output,indent=2))

if __name__=='__main__':main()
