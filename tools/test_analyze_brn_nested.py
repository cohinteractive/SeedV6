"""Selection/provenance rejection tests; synthetic opaque records, not engine tests."""
import hashlib
import copy
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('nested_analysis', Path(__file__).with_name('analyze-brn-nested.py'))
nested = importlib.util.module_from_spec(spec)
spec.loader.exec_module(nested)


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value), encoding='utf-8')


def payload(root, rows):
    root.mkdir(parents=True)
    data = struct.pack('>Qii', 0x533642524e443031, len(rows[0]), len(rows[1])) + b''.join(r for part in rows for r in part)
    (root/'positions.bin').write_bytes(data)
    return hashlib.sha256(data).hexdigest()


def fixture(root):
    large, small = root/'data-large', root/'data-small'
    record = lambda i,p: struct.pack('>i',i)+bytes(52)+bytes([p])
    large_rows = [[record(i,0) for i in range(256)], [record(10000+i,1) for i in range(4096)], [record(20000+i,2) for i in range(2)]]
    small_rows = [large_rows[0][::2], large_rows[1], large_rows[2]]
    large_hash, small_hash = payload(large,large_rows),payload(small,small_rows)
    write(large/'manifest.json', {'payloadSha256':large_hash})
    manifest = {'payloadSha256':small_hash, 'parentManifestSha256':nested.sha(large/'manifest.json'),
                'parent':{'path':str(large),'payloadSha256':large_hash}, 'selectionSeed':123,
                'counts':[128,4096,2], 'selectedTrainingOrdinalsSha256':hashlib.sha256(b''.join(struct.pack('>i',i) for i in range(0,256,2))).hexdigest()}
    for name,part in zip(['training','validation','test'],small_rows):manifest[name+'Sha256']=hashlib.sha256(b''.join(part)).hexdigest()
    write(small/'manifest.json',manifest)
    dataset={'seed':211,'parent':str(large),'subset':str(small),'parentPayloadSha256':large_hash,
             'subsetPayloadSha256':small_hash,'subsetManifestSha256':nested.sha(small/'manifest.json'),
             'subsetSeed':123,'parentCounts':[256,4096,2],'subsetCounts':[128,4096,2]}
    specs={}
    for name,data,count,epochs,digest in [('small',small,128,16,small_hash),('large',large,256,8,large_hash)]:
        run=root/('train-'+name);run.mkdir()
        args=[str(data),str(run),'BRN3',str(count),str(epochs),'211','frozen-v1']
        events=[]
        for epoch in range(epochs+1):
            loss=.8 if epoch==0 else .001 if name=='small' and epoch%2 else .8-.025*epoch
            event={'epoch':epoch,'samples':count*epoch,'updates':count*epoch//128,
                   'trainingSeconds':epoch*.5,'validation':{'positions':4096,'outcomeHalfMse':loss}}
            events.append(event)
            endpoint=run/'gen0' if epoch==0 else run/'epochs'/('epoch-%03d'%epoch)
            endpoint.mkdir(parents=True)
            (endpoint/'selected.model').write_bytes(b'common-gen0' if epoch==0 else f'{name}-{epoch}'.encode())
            metadata={'kind':'BRN3','complete':True,'selectedEpoch':epoch,'selectedModelSha256':nested.sha(endpoint/'selected.model')}
            if epoch:metadata.update(endpoint=event,sourceRun=str(run))
            else:(endpoint/'selected.state').write_bytes(b'common-gen0-state')
            write(endpoint/'result.json',metadata)
        chosen=min(events,key=lambda e:e['validation']['outcomeHalfMse'])['epoch']
        source=run/'gen0' if chosen==0 else run/'epochs'/('epoch-%03d'%chosen)
        (run/'selected.model').write_bytes((source/'selected.model').read_bytes())
        (run/'selected.state').write_bytes(b'parent-selected-state')
        (run/'last.state').write_bytes(b'parent-last-state')
        write(run/'result.json',{'kind':'BRN3','complete':True,'exactNextBatchResume':True,'arguments':['train']+args,
              'datasetManifest':{'payloadSha256':digest},'completedEpochs':epochs,'events':events,'selectedEpoch':chosen,
              'selectedModelSha256':nested.sha(run/'selected.model'),'selectedStateSha256':nested.sha(run/'selected.state'),
              'lastStateSha256':nested.sha(run/'last.state')})
        write(Path(str(run)+'.execution.json'),{'status':'completed','exitCode':0,'command':['java','Main','train']+args,
              'elapsedSeconds':epochs,'sourceSha256':{'trainer':'same'},'compiledSha256':{'trainer.class':'same'},'runnerSha256':{'runner':'same'}})
        specs[name]={'run':str(run),'mode':'train','arguments':args,'dataPayloadSha256':digest}
    bridge_plan=root/'bridge-plan.json';bridge_result=root/'bridge-result.json'
    write(bridge_plan,{'historicalCodeDelta':{k:{'added':{},'removed':{},'changed':{}} for k in ['sourceSha256','compiledSha256','runnerSha256']}})
    write(bridge_result,{'allChecksPassed':True,'planSha256':nested.sha(bridge_plan),'runs':[{'family':'brn','seed':211,'exactModelsAndMetrics':True}]})
    return {'schema':'brn-architecture-nested-plan-v1','smallCount':128,'smallEpochs':16,'largeCount':256,'largeEpochs':8,
            'smallEligibleEpochs':list(range(0,17,2)),'largeEligibleEpochs':list(range(9)), 'datasets':[dataset],
            'runs':[{'family':'brn','seed':211,**specs}], 'referenceBridge':{'plan':str(bridge_plan),'planSha256':nested.sha(bridge_plan),
                        'result':str(bridge_result),'resultSha256':nested.sha(bridge_result)}}


class NestedAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.plan=fixture(Path(self.temp.name))

    def test_odd_epoch_cannot_select_despite_better_loss(self):
        result=nested.analyze(self.plan);row=result['pairs'][0]
        self.assertTrue(result['allChecksPassed'])
        self.assertEqual(1,row['small']['parentSelectedEpoch'])
        self.assertEqual(16,row['small']['selected']['epoch'])
        self.assertFalse(row['small']['selectedEpochIsParentSelection'])
        self.assertEqual([e['samples'] for e in row['large']['curve']],[e['samples'] for e in row['small']['curve']])

    def test_tampered_endpoint_rejected(self):
        run=Path(self.plan['runs'][0]['small']['run'])
        (run/'epochs'/'epoch-002'/'selected.model').write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError,'Endpoint identity'):nested.analyze(self.plan)

    def test_changed_held_out_bytes_rejected_even_with_new_payload_hash(self):
        spec=self.plan['datasets'][0];root=Path(spec['subset']);path=root/'positions.bin'
        data=bytearray(path.read_bytes());data[16+128*57+48]^=1;path.write_bytes(data)
        manifest=nested.read(root/'manifest.json');manifest['payloadSha256']=nested.sha(path)
        write(root/'manifest.json',manifest)
        spec['subsetPayloadSha256']=manifest['payloadSha256'];spec['subsetManifestSha256']=nested.sha(root/'manifest.json')
        with self.assertRaisesRegex(ValueError,'Held-out bytes differ'):nested.audit_subset(spec)

    def test_changed_execution_and_unaccounted_code_rejected(self):
        path=Path(self.plan['runs'][0]['small']['run']+'.execution.json')
        receipt=nested.read(path);original=list(receipt['command']);receipt['command'][-2]='999';write(path,receipt)
        with self.assertRaisesRegex(ValueError,'Executed command'):nested.analyze(self.plan)
        receipt['command']=original;receipt['sourceSha256']['trainer']='changed';write(path,receipt)
        with self.assertRaisesRegex(ValueError,'Unaccounted historical code'):nested.analyze(self.plan)

    def test_changed_initializer_and_selection_opportunity_rejected(self):
        path=Path(self.plan['runs'][0]['small']['run'])/'gen0'/'selected.state'
        path.write_bytes(b'different-initializer')
        with self.assertRaisesRegex(ValueError,'Initializer differs'):nested.analyze(self.plan)
        self.plan['smallEligibleEpochs'].append(15)
        with self.assertRaisesRegex(ValueError,'Unequal selection opportunities'):nested.analyze(self.plan)

    def test_historical_draft_reconciliation_cannot_allow_executable_changes(self):
        baseline={'sourceSha256':{'trainer':'same'},'compiledSha256':{'trainer.class':'same'},'runnerSha256':{'runner':'same'}}
        path='app/src/verification/java/example/Helper.java'
        current=copy.deepcopy(baseline);current['sourceSha256'][path]='new-helper'
        current['compiledSha256']['app/build/classes/java/verification/example/Helper.class']='new-class'
        expected=nested.code_delta(baseline,current)
        historical=copy.deepcopy(baseline);historical['sourceSha256'][path]='old-uncompiled-helper'
        allowed={path:'old-uncompiled-helper'}
        self.assertEqual(nested.code_delta(historical,current),nested.reconciled_code_delta(historical,current,baseline,expected,allowed))
        with self.assertRaisesRegex(ValueError,'Unaccounted historical source draft'):
            nested.reconciled_code_delta(historical,current,baseline,expected,{})
        altered=copy.deepcopy(historical);altered['sourceSha256']['trainer']='changed'
        with self.assertRaisesRegex(ValueError,'Historical shared code changed'):
            nested.reconciled_code_delta(altered,current,baseline,expected,allowed)
        altered=copy.deepcopy(historical);altered['compiledSha256']['app/build/classes/java/verification/example/Helper.class']='old-class'
        with self.assertRaisesRegex(ValueError,'Historical executable inventory changed'):
            nested.reconciled_code_delta(altered,current,baseline,expected,allowed)
        altered=copy.deepcopy(current);altered['sourceSha256']['unrelated']='changed'
        with self.assertRaisesRegex(ValueError,'Unaccounted current code difference'):
            nested.reconciled_code_delta(historical,altered,baseline,expected,allowed)


if __name__=='__main__':
    unittest.main()
