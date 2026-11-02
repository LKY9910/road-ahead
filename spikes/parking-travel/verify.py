"""Offline checks for T-23 redaction, excerpts, scene selection and source counts."""
import hashlib
import json
from urllib.parse import quote
from analyze import distance, navi_summary
from probe_api import old, ROOT, OUT, RAW, read_record, self_test


def main():
    self_test()
    assert abs(distance(37.567, 126.978, 37.567, 126.978)) < 0.001
    assert 110000 < distance(0, 0, 1, 0) < 112000
    navi = navi_summary()
    assert navi['sdkRoute2StartedMs'] == 5847
    assert navi['searchRoute2StartedMs'] == 5979
    assert navi['favoriteRoute2StartedMs'] == 5961
    assert navi['pollDestinationMs'] == [1011, 1010, 1012]
    assert navi['cancelStatusMs'] == 258 and navi['cancelEmptyDestinationMs'] == 297
    for p in OUT.glob('*.json'):
        d = json.loads(p.read_text(encoding='utf-8'))
        if 'excerpt' in d:
            full = (RAW / p.name).read_text(encoding='utf-8')
            assert hashlib.sha256(full.encode()).hexdigest() == d['fullRecordSha256'], p.name
            record = json.loads(full)
            for path, detail in d['excerpt'].items():
                actual = record
                for key in path.split('.'):
                    actual = actual[key]
                assert len(actual) == detail['receivedRows'], p.name
    scene_d = json.loads((OUT / 'scene-D.json').read_text(encoding='utf-8'))['candidate']
    ids = {scene_d[key]['prk_center_id'] for key in ['facility', 'operation', 'realtime']}
    assert len(ids) == 1 and scene_d['distanceMeters'] <= 500
    assert int(scene_d['realtime']['pkfc_Available_ParkingLots_total']) > 0
    scene_e = json.loads((OUT / 'scene-E.json').read_text(encoding='utf-8'))
    event = scene_e['candidate']['event']
    date = scene_e['date'].replace('-', '')
    assert event['eventstartdate'] <= date <= event['eventenddate']
    assert scene_e['candidate']['distanceMeters'] <= 5000
    files = list((ROOT / 'spikes/parking-travel').rglob('*')) + list((ROOT / 'docs/spikes/parking-travel').rglob('*'))
    files += [ROOT / 'docs/APIS.md', ROOT / 'docs/spikes/parking-travel.md']
    secrets = old.credentials().values()
    checked = 0
    for p in files:
        if not p.is_file() or '.gradle' in p.parts or 'build' in p.parts or '__pycache__' in p.parts or p.suffix in ['.jar', '.png']:
            continue
        data = p.read_text(encoding='utf-8-sig')
        for key in secrets:
            for variant in [key, quote(key, safe=''), quote(quote(key, safe=''), safe='')]:
                assert variant not in data, f'credential found in {p.relative_to(ROOT)}'
        checked += 1
    state = json.loads((ROOT / 'spikes/parking-travel/.gradle/api-counts.json').read_text())
    assert all(counts.get('tour', 0) <= 50 for counts in state.values())
    assert read_record('parking-realtime-id-filter')['response'] == read_record('parking-realtime-invalid-id')['response']
    print(f'T-23 verification passed: excerpts/hashes, scenes D/E, distance, ignored-ID comparison, key scan ({checked} files), tour <= 50 calls')


if __name__ == '__main__':
    main()
