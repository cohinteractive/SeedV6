"""Negative identity tests using synthetic evidence, not engine measurements."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

def module(name):
    spec=importlib.util.spec_from_file_location(name,Path(__file__).with_name(name+'.py'))
    value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value

runtime=module('analyze-brn-runtime')
matches=module('analyze-brn-matches')

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value),encoding='utf-8')

class MetadataTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.model=self.root/'model';self.model.mkdir()
        (self.model/'selected.model').write_bytes(b'unchanged-weights')
        write(self.model/'result.json',{'kind':'TUPLE','complete':True})
        self.weights=runtime.sha(self.model/'selected.model');self.metadata=runtime.sha(self.model/'result.json')

    def runtime_plan(self):
        plan={'schema':'brn-architecture-runtime-plan-v1','requireMetadataIdentity':True,'rootCount':1,'depth':1,
              'profiles':[{'profile':'candidate','model':str(self.model),'modelIdentity':self.weights,
                           'modelMetadataIdentity':self.metadata,'gain':.25,'data':'fixture','dataPayloadSha256':'data'}],
              'runs':[]}
        for mode in ['measure','transitions']:
            run=self.root/mode
            write(Path(str(run)+'.execution.json'),{'status':'completed','exitCode':0,'elapsedSeconds':1,
                'sourceSha256':{},'compiledSha256':{},'runnerSha256':{},'processMemory':{}})
            row={'arguments':['measure'] if mode=='measure' else [],'modelSha256':self.weights,'gain':.25,
                 'modelMetadataIdentity':self.metadata,'modelIdentityStable':True,'method':'synthetic fixture'}
            if mode=='measure':
                row.update(roots=[{'complete':True,'depth':1,'index':0,'nodes':1,'seconds':.1}],
                           fullRefreshScoreNs=[1]*9,medianNs=1)
            else:
                row.update(datasetManifest={'payloadSha256':'data'},requestSha256='request',
                           verification={'maximumAbsoluteIntegerScoreDifference':0,'requests':1},
                           groups={k:{'requestsPerPass':1,'nanosecondsPerRequestPasses':[1]*9,'medianNs':1}
                                   for k in ['all','nonking','king','capture','promotion','castle','enPassant']})
            write(run/'result.json',row);plan['runs'].append({'profile':'candidate','mode':mode,'run':str(run),'arguments':[]})
        return plan

    def match_plan(self):
        run=self.root/'match'
        write(Path(str(run)+'.execution.json'),{'status':'completed'})
        write(run/'config.json',{'arguments':['match','','','','1','0','25','0','42'],
            'candidateModelSha256':self.weights,'opponentModelSha256':'control','candidateMetadataIdentity':self.metadata,
            'opponentMetadataIdentity':'control','maximumPlies':1024,'protocolVersion':'architecture-match-v2-explicit-cap'})
        game={'score':.5,'termination':'fixture','plies':2,'candidateNodes':1,'opponentNodes':1,
              'candidateSeconds':.1,'opponentSeconds':.1}
        write(run/'pair-00000.json',{'index':0,'identity':'opening','white':game,'black':game})
        write(run/'result.json',{'modelIdentitiesStable':True})
        return {'firstOpeningIndex':0,'pairsPerInitialization':1,'depth':0,'millis':25,'openingSeed':42,
                'maximumPlies':1024,'bootstrapReplicates':100,'requireMetadataIdentity':True,
                'modelMetadataIdentities':{'comparison':{'211':[self.metadata,'control']}},
                'comparisons':{'comparison':{'211':[{'run':str(run)}]}}}

    def test_runtime_requires_reported_and_retained_evaluator_identity(self):
        plan=self.runtime_plan();self.assertTrue(runtime.analyze(plan)['allChecksPassed'])
        path=self.root/'measure'/'result.json';report=runtime.read(path)
        report['modelMetadataIdentity']='different';write(path,report)
        with self.assertRaisesRegex(ValueError,'Frozen evaluator metadata'):runtime.analyze(plan)
        report['modelMetadataIdentity']=self.metadata;write(path,report)
        write(self.model/'result.json',{'kind':'TUPLE_COMPILED','complete':True})
        self.assertEqual(self.weights,runtime.sha(self.model/'selected.model'))
        with self.assertRaisesRegex(ValueError,'Retained evaluator metadata'):runtime.analyze(plan)

    def test_match_requires_frozen_identity_and_completion_stability(self):
        plan=self.match_plan();self.assertTrue(matches.analyze(plan)['comparisons']['comparison']['complete'])
        path=self.root/'match'/'config.json';config=runtime.read(path)
        config['candidateMetadataIdentity']='different';write(path,config)
        with self.assertRaisesRegex(ValueError,'Evaluator metadata differs'):matches.analyze(plan)
        config['candidateMetadataIdentity']=self.metadata;write(path,config)
        write(self.root/'match'/'result.json',{})
        with self.assertRaisesRegex(ValueError,'identity not verified stable'):matches.analyze(plan)
        write(self.root/'match'/'result.json',{'modelIdentitiesStable':True})
        del plan['modelMetadataIdentities']
        with self.assertRaisesRegex(ValueError,'Evaluator metadata differs'):matches.analyze(plan)

    def test_changed_frozen_input_rejects_before_result_aggregation(self):
        runtime_plan=self.runtime_plan();match_plan=self.match_plan()
        path=str(self.model/'result.json')
        for plan in [runtime_plan,match_plan]:plan['frozenFiles']={path:self.metadata}
        self.assertTrue(runtime.analyze(runtime_plan)['allChecksPassed'])
        self.assertTrue(matches.analyze(match_plan)['comparisons']['comparison']['complete'])
        write(self.model/'result.json',{'kind':'TUPLE_COMPILED','complete':True})
        with self.assertRaisesRegex(ValueError,'Frozen runtime input changed'):runtime.analyze(runtime_plan)
        with self.assertRaisesRegex(ValueError,'Frozen match input changed'):matches.analyze(match_plan)

if __name__=='__main__':unittest.main()
