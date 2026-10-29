"""R052 monitored focused correctness build; no training allocation."""
import subprocess
import time
from baseline import ROOT, admit, sha
from compute_control import atomic


def main():
    started=time.monotonic();samples=[];process=None;error=None;code=None;output=None
    log=ROOT/('docs/research/brn-relational-cglhw/raw/r052-gradle-'+str(time.time_ns())+'.log')
    try:
        samples.append(admit(initial=True))
        output=log.open('x')
        process=subprocess.Popen(['cmd.exe','/d','/c','gradlew.bat','--offline','--no-daemon','--max-workers=1',
            ':research:test','--tests','*TypedRelationPieceNetworkTest','--tests','*MixedTiedPieceNetworkTest','--tests','*TypedRelationCompilationTest',
            ':research:installResearch'],stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW|subprocess.BELOW_NORMAL_PRIORITY_CLASS)
        while True:
            try:
                code=process.wait(timeout=5)
                if code:raise RuntimeError('Gradle exit '+str(code))
                break
            except subprocess.TimeoutExpired:
                samples.append(admit())
                if time.monotonic()-started>180:raise TimeoutError('Bounded build allocation')
    except Exception as exc:
        error=repr(exc);raise
    finally:
        if process is not None and process.poll() is None:
            process.terminate();process.wait(timeout=10)
        if output is not None:output.close()
        atomic(ROOT/('docs/research/brn-relational-cglhw/raw/r052-build-execution-'+str(time.time_ns())+'.json'),
               dict(seconds=time.monotonic()-started,error=error,resources=samples,exit_code=code,
                    direct_child_absent=process is None or process.poll() is not None,runner_sha256=sha(__file__),log=str(log.relative_to(ROOT)),log_sha256=sha(log) if log.exists() else None))


if __name__=='__main__':main()
