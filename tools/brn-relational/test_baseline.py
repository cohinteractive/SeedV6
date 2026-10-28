"""Protocol guard tests: never infer budget compliance from completed-iteration info."""
import unittest
import baseline
import compare_pairs


class ReceiptTests(unittest.TestCase):
    def test_request_not_iteration_nodes(self):
        lines=['info depth 3 nodes 19 score cp 5 pv e2e4',
               'info string request nodes=100 elapsedNanos=12345 termination=NODE_LIMIT']
        self.assertEqual(baseline.parse_receipt(lines,100)['nodes'],100)
    def test_missing_duplicate_malformed_and_overbudget_fail(self):
        valid='info string request nodes=100 elapsedNanos=12345 termination=NODE_LIMIT'
        for lines in ([],[valid,valid],['info string request nodes=invalid'],
                      [valid.replace('nodes=100','nodes=101')],
                      [valid.replace('nodes=100','nodes=99')],
                      [valid.replace('NODE_LIMIT','TIME_LIMIT')]):
            with self.subTest(lines=lines), self.assertRaises(ValueError):
                baseline.parse_receipt(lines,100)
    def test_early_completed_allowed(self):
        self.assertEqual(baseline.parse_receipt([
            'info string request nodes=2 elapsedNanos=10 termination=COMPLETED'],100)['nodes'],2)
    def test_censoring_does_not_become_draw(self):
        r=baseline.summarize([dict(opening=0,score=1),dict(opening=0,score=None)],2)
        self.assertEqual(r['draws'],0)
        self.assertEqual(r['full_allocation_score_bounds'],[.5,1])
        self.assertNotIn('score',r)
    def test_independent_replay_rejects_wrong_score_and_wrong_budget(self):
        moves=['f2f3','e7e5','g2g4','d8h4']
        game=dict(moves=moves,left_color=True,score=0,result='0-1',receipts=[
            dict(move=m,nodes=10,elapsed_ns=1,termination='NODE_LIMIT') for m in moves])
        plan=dict(nodes=10,max_plies=640);opening=dict(moves='')
        compare_pairs.replay(game,opening,plan)
        game['score']=1
        with self.assertRaises(ValueError):compare_pairs.replay(game,opening,plan)
        game['score']=0;game['receipts'][0]['nodes']=11
        with self.assertRaises(ValueError):compare_pairs.replay(game,opening,plan)


if __name__=='__main__':unittest.main()
