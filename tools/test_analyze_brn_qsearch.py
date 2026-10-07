import copy
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('qsearch',Path(__file__).with_name('analyze-brn-qsearch.py'))
qsearch=importlib.util.module_from_spec(spec);spec.loader.exec_module(qsearch)


class QsearchAnalysisTest(unittest.TestCase):
    def completed(self):
        return {'index':0,'fen':'recorded-board','completed':True,'nodes':100,'qnodes':75,'maximumQply':8,
                'seconds':.01,'nodeGuardReached':False,'timeGuardReached':False,'otherAbort':False,'score':12,'move':'0'}

    def aborted(self):
        row=self.completed();row.update(index=1,completed=False,nodes=100000,qnodes=99950,seconds=1.1,
                                       maximumQply=80,nodeGuardReached=True,timeGuardReached=True)
        del row['score'];del row['move'];return row

    def test_aborted_tail_is_included_in_all_attempt_metrics(self):
        r=qsearch.summarize([self.completed(),self.aborted()])
        self.assertEqual(2,r['attemptedRoots']);self.assertEqual(1,r['completedRoots'])
        self.assertEqual(100100,r['allAttemptTotals']['nodes'])
        self.assertEqual(1.1,r['allAttemptDistributions']['seconds']['maximum'])
        self.assertEqual(1,r['guardCounts']['nodeGuardReached']);self.assertEqual(1,r['guardCounts']['timeGuardReached'])

    def test_rejects_completed_scores_attached_to_guard_failure(self):
        row=self.aborted();row['score']=0
        with self.assertRaises(ValueError):qsearch.validate_root(row)

    def test_rejects_invalid_counts_time_and_completion_accounting(self):
        for key,value in [('nodes',-1),('qnodes',101),('seconds',float('nan')),('completed',1),('move',str(2**64)),('otherAbort',True)]:
            row=self.completed();row[key]=value
            with self.assertRaises(ValueError):qsearch.validate_root(row)
        row=self.aborted();row['nodeGuardReached']=row['timeGuardReached']=False
        with self.assertRaises(ValueError):qsearch.validate_root(row)

    def test_rejects_missing_or_duplicate_root_allocation(self):
        with self.assertRaises(ValueError):qsearch.summarize([])
        row=self.completed()
        with self.assertRaises(ValueError):qsearch.summarize([row,copy.deepcopy(row)])


if __name__=='__main__':unittest.main()
