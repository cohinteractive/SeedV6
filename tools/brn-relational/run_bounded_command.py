"""One supervised, low-priority CPU helper with bounded time and resource checks."""
import argparse
import json
from pathlib import Path
import subprocess
import time
from baseline import ROOT, admit, sha
from compute_control import atomic


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--receipt',type=Path,required=True)
    parser.add_argument('--seconds',type=int,required=True)
    parser.add_argument('command',nargs=argparse.REMAINDER)
    args = parser.parse_args()
    assert 1 <= args.seconds <= 180 and args.command and not args.receipt.exists()
    samples = [admit(initial=True)]
    process = None; error = None; started = time.monotonic()
    try:
        process = subprocess.Popen(args.command,cwd=ROOT,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        while True:
            try:
                code = process.wait(timeout=5)
                if code:raise RuntimeError('Bounded helper exit '+str(code))
                break
            except subprocess.TimeoutExpired:
                samples.append(admit())
                if time.monotonic()-started >= args.seconds:raise RuntimeError('Bounded helper allocation exhausted')
    except Exception as exc:
        error = repr(exc);raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:process.wait(timeout=5)
            except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
        atomic(args.receipt,dict(command=args.command,runner_sha256=sha(Path(__file__)),seconds=time.monotonic()-started,
                                resources=samples,error=error,returncode=process.returncode if process else None,
                                process_absent=process is None or process.poll() is not None))


if __name__ == '__main__':
    main()
