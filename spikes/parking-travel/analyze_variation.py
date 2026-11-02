"""Offline T-23 variation analysis. This script never calls an API."""
import argparse
from datetime import datetime
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / 'docs/spikes/parking-travel/realtime-variation-values.json'
SUMMARY = ROOT / 'docs/spikes/parking-travel/summary.json'
SCENE_D = '04540-11291-00001-00-1'


def compare(values, cohort, first, second):
    comparable = {identity for identity, series in values.items()
                  if series[first] is not None and series[second] is not None}
    changed = {identity for identity in comparable if values[identity][first] != values[identity][second]}
    named = comparable & cohort
    named_changed = changed & cohort
    return {'interval': f'{first+1}->{second+1}',
            'comparableIds': len(comparable), 'changedIds': len(changed),
            'changedRatio': len(changed)/len(comparable) if comparable else None,
            'cohortComparableIds': len(named), 'cohortChangedIds': len(named_changed),
            'cohortChangedRatio': len(named_changed)/len(named) if named else None,
            'cohortMissingInPair': len(cohort-named)}


def self_test():
    values = {'a': [1, 2, 1], 'b': [3, 3, 3], 'c': [4, None, 5]}
    one = compare(values, {'a', 'c'}, 0, 1)
    total = compare(values, {'a', 'c'}, 0, 2)
    assert (one['comparableIds'], one['changedIds'], one['changedRatio']) == (2, 1, 0.5)
    assert (one['cohortComparableIds'], one['cohortChangedIds'], one['cohortMissingInPair']) == (1, 1, 1)
    assert (total['changedIds'], total['cohortChangedIds'], total['cohortComparableIds']) == (1, 1, 2)
    assert compare({'a': [None, None, None]}, {'a'}, 0, 1)['changedRatio'] is None
    print('variation self-test passed: changed/reverted/missing/zero-denominator cases; no network calls')


def main():
    evidence = json.loads(EVIDENCE.read_text(encoding='utf-8'))
    samples, values = evidence['samples'], evidence['availabilityById']
    cohort = set(evidence['originalNamedCoordinateCohortIds'])
    assert len(samples) == 3 and len(cohort) == 487
    for series in values.values():
        assert len(series) == 3 and all(value is None or type(value) is int for value in series)
    times = [datetime.fromisoformat(sample['requestedAtKst']) for sample in samples]
    intervals = [(b-a).total_seconds() for a, b in zip(times, times[1:])]
    assert all(240 <= seconds <= 360 for seconds in intervals), 'requests must be about five minutes apart'
    for index, sample in enumerate(samples):
        assert sample['httpStatus'] == 200 and sample['resultCode'] == '0'
        assert sample['numOfRowsRequested'] == 5000
        assert sum(series[index] is not None for series in values.values()) == sample['uniqueIds']
    result = {'apiCalls': 3, 'operation': 'PrkRealtimeInfo_v1', 'numOfRows': 5000,
              'samples': samples, 'requestIntervalsSeconds': intervals,
              'originalNamedCoordinateCohortSize': len(cohort),
              'comparisons': [compare(values, cohort, a, b) for a, b in [(0, 1), (1, 2), (0, 2)]],
              'sceneD': {'prk_center_id': SCENE_D, 'availability': values.get(SCENE_D, [None]*3)},
              'evidence': 'realtime-variation-values.json',
              'interpretation': 'Value changes are observable; production timestamps and absolute freshness remain unknown. Unchanged rows do not prove stale data.'}
    summary = json.loads(SUMMARY.read_text(encoding='utf-8'))
    summary['realtimeVariation'] = result
    SUMMARY.write_text(json.dumps(summary, ensure_ascii=False, indent=2)+'\n', encoding='utf-8', newline='\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        main()
