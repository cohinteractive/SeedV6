"""Reconcile frozen binary provenance without rewriting either runtime."""
import json,zipfile
from run_observation_v1 import HERE, digest, write_new

old=HERE/'raw/runtime-v4';new=HERE/'raw/runtime-cpu-delivery-v1';details={}
for path in sorted(p for p in old.rglob('*') if p.is_file()):
    rel=path.relative_to(old);other=new/rel;assert other.is_file()
    if digest(path)==digest(other):continue
    assert path.suffix=='.jar',str(rel)
    with zipfile.ZipFile(path) as a,zipfile.ZipFile(other) as b:
        ak={n for n in a.namelist() if not n.endswith('/')};bk={n for n in b.namelist() if not n.endswith('/')}
        changed={n:{'before':a.read(n).decode(),'after':b.read(n).decode()} for n in ak&bk if a.read(n)!=b.read(n)}
        assert not ak-bk
        assert set(changed)<={'com/ohinteractive/seedv6/application-version.properties'}
        assert bk-ak<={'com/ohinteractive/seedv6/search/exact/Cp64CpuDelivery.class'}
        details[str(rel)]={'sha256Before':digest(path),'sha256After':digest(other),'changedEntries':changed,'addedEntries':sorted(bk-ak)}
assert {str(p.relative_to(new)) for p in new.rglob('*') if p.is_file()}=={str(p.relative_to(old)) for p in old.rglob('*') if p.is_file()}
write_new(HERE/'runtime-delivery-parity-v1.json',{'status':'passed','comparison':details,
    'conclusion':'All existing executable class/resource bytes equal except version provenance properties; one research-only measurement class added. Both versions build73. Frozen runtime-v4 was compiled before commit2b4d537 and embeds prior5e4dee7 plus uncommitted-changes marker; its external manifest binds laterHEAD2b4d537. Do not rewrite that historical identity.',
    'validationLimit':'Initial whole-file equality assertion failed on the two jars; this entry-level audit resolves the difference, not a claim the jars were byte-equal.',
    'lineEndingReview':'git diff --cached --check flags CRLF in the frozen whole-opening source and selection receipt. git -c core.whitespace=cr-at-eol diff --cached --check passes. Preserve frozen CRLF bytes and hashes; no normalization.'})
print('Executable parity passed; embedded precommit provenance preserved explicitly.')
