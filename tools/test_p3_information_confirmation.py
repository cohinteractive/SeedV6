import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('p3_confirmation_test',Path(__file__).with_name('p3-information-confirmation.py'))
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)


class BindingTests(unittest.TestCase):
    def test_phase3_session_is_separate_and_requires_packaging_before_mutation(self):
        self.assertEqual('p3-information-match.py',c.t.adapter_name({'adapter':'p3-information-match.py'}))
        with self.assertRaisesRegex(ValueError,'Package the Phase 3'):
            c.t.prepare('unused','unused')

    def test_each_frozen_runtime_is_checked_without_substituting_candidate_off(self):
        arguments={'baseline':'old','candidate':'new'}
        trees={'old':{'core':'old-sha'},'new':{'core':'new-sha'}}
        with patch.object(c.t,'tree',side_effect=lambda path:trees[path]):
            c.m.verify_runtime({'runtimeTrees':{'baseline':trees['old'],'candidate':trees['new']}},arguments)
            with self.assertRaisesRegex(ValueError,'Wrong runtime: baseline'):
                c.m.verify_runtime({'runtimeTrees':{'baseline':trees['new'],'candidate':trees['new']}},arguments)
            c.m.verify_runtime({'runtimeTree':trees['old']},{'baseline':'old','candidate':'old'})
            with self.assertRaisesRegex(ValueError,'Wrong runtime: candidate'):
                c.m.verify_runtime({'runtimeTree':trees['old']},arguments)

    def protocol(self):
        dependencies=[c.__file__,c.m.__file__,c.m.rules.__file__,c.m.t.__file__,c.m.b.__file__]
        return {'design':'p3-information-paired-stages-v1','family':'P3-O1O2','executorSha256':c.t.digest(c.__file__),
                'frozenFiles':{str(Path(p).resolve()):c.t.digest(p)for p in dependencies},
                'looks':[128,512],'contrastIndex':1,'familyAlpha':.05,'minimumPairsEachSideForNormal':20,
                'requiredSensitivitySigma':2.5,'futilityPowerThreshold':.1,'stages':[{'pairs':128},{'pairs':384}],
                'expectedAdapter':'p3-information-match.py','expectedArguments':{'baseline_policy':'0','candidate_policy':'7',
                    'baseline_workers':1,'candidate_workers':1,'tc':'10+0.1','concurrency':1,'background':True},
                'runtimeTrees':{'baseline':{'lib\\engine-core.jar':c.BASELINE_CORE},'candidate':{'lib\\engine-core.jar':'candidate-sha'}}}

    def verify(self,p):
        with patch.object(c.t,'read',return_value=p):return c.verify('unused')

    def test_both_domains_bind_the_shared_executor_and_actual_champion(self):
        p=self.protocol();self.assertEqual(p,self.verify(p))
        p['contrastIndex']=2;p['expectedArguments'].update(candidate_policy='4',baseline_workers=4,candidate_workers=4)
        self.assertEqual(p,self.verify(p));self.assertIs(c.m.verify,c.verify)
        self.assertIs(c.run,c.m.run);self.assertIs(c.assess,c.m.assess)

    def test_swapped_binary_and_unbound_rule_are_rejected(self):
        p=self.protocol();p['runtimeTrees']['baseline']=p['runtimeTrees']['candidate']
        with self.assertRaisesRegex(ValueError,'champion binary'):self.verify(p)
        p=self.protocol();del p['frozenFiles'][str(Path(c.m.rules.__file__).resolve())]
        with self.assertRaisesRegex(ValueError,'Unbound shared'):self.verify(p)
        p=self.protocol();p['frozenFiles'][str(Path(c.m.__file__).resolve())]='changed'
        with self.assertRaisesRegex(ValueError,'Frozen evidence changed'):self.verify(p)

    def test_wrong_clock_workers_schedule_or_statistical_rule_are_rejected(self):
        for changes in ({'looks':[64,128]},{'contrastIndex':True},{'familyAlpha':.1},
                        {'minimumPairsEachSideForNormal':10},{'stages':[{'pairs':128},{'pairs':512}]}):
            with self.subTest(changes=changes):
                p=self.protocol();p.update(changes)
                with self.assertRaises(ValueError):self.verify(p)
        for changes in ({'tc':'1+0.01'},{'concurrency':2},{'background':False},
                        {'baseline_workers':4,'candidate_workers':4},{'baseline_policy':'4'}):
            with self.subTest(changes=changes):
                p=self.protocol();p['expectedArguments'].update(changes)
                with self.assertRaises(ValueError):self.verify(p)
        p=self.protocol();p['contrastIndex']=2;p['expectedArguments'].update(baseline_workers=4,candidate_workers=4)
        with self.assertRaisesRegex(ValueError,'policy/worker'):self.verify(p)


if __name__=='__main__':unittest.main()
