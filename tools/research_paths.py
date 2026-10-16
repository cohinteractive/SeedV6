"""Current research runtime paths; immutable historical Git paths stay at call sites.

Build with :research:installResearch. No legacy application installation is used.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OWNERS = ('engine-core', 'engine-training', 'research')


def runtime_paths():
    return [str(ROOT / 'research/build/install/research/lib/*')]


def source_files():
    return sorted(p for owner in OWNERS for p in (ROOT / owner / 'src/main/java').rglob('*.java'))


def compiled_files():
    return sorted(p for owner in OWNERS for p in (ROOT / owner / 'build/classes/java/main').rglob('*.class'))
