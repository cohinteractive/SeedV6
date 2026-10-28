import copy
import math
import unittest
from prepare_fresh import record,select


class FreshPanelTest(unittest.TestCase):
    def test_white_cp_to_stm_and_symmetry_reservations(self):
        a=record(0,'4k3/8/8/8/8/8/8/4K3 b - - 0 1 | 400 | 0.5')
        self.assertAlmostEqual(float(a['line'].split('\t')[1]),1/(1+math.exp(1)))
        b=record(1,'4k3/8/8/8/8/8/8/4K3 w - - 0 1 | -400 | 0.5')
        self.assertEqual(a['alias'],b['alias'])
        with self.assertRaises(ValueError):record(0,'4k3/8/8/8/8/8/8/4K3 w - - 0 1 | NaN | 0.5')

    def test_interruption_resumes_without_reselecting_saved_prefix(self):
        records={i:f'4k3/8/8/8/8/8/{i}P{7-i if i<7 else ""}/4K3 w - - 0 1 | {100*i} | 0.5' for i in range(1,7)}
        # Valid FEN rank strings;file mirrors intentionally collapse several rows.
        order=[1,2,3,4,5,6];excluded={record(1,records[1])['alias']};original=dict(cursor=0,rows=[])
        select(records,order,excluded.__contains__,original,2,lambda s:None,lambda:None,chunk=2)
        saved=[]
        def stop(s):saved.append(copy.deepcopy(s));raise RuntimeError('interrupt at committed checkpoint')
        with self.assertRaises(RuntimeError):select(records,order,excluded.__contains__,dict(cursor=0,rows=[]),2,stop,lambda:None,chunk=2)
        resumed=saved[-1];select(records,order,excluded.__contains__,resumed,2,lambda s:None,lambda:None,chunk=2)
        self.assertEqual(original,resumed)


if __name__=='__main__':unittest.main()
