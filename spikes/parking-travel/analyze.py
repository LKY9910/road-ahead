"""Recompute T-23 evidence from redacted records and publish bounded excerpts."""
from datetime import datetime
import gzip
import json
import math
import re
import statistics
import zlib
from urllib.parse import urlsplit
from probe_api import OUT, ROOT, read_record, publish

REPORT = ROOT / 'docs/spikes/parking-travel'


def distance(lat, lon, lat2, lon2):
    a, b = math.radians(lat), math.radians(lat2)
    d = math.sin((b-a)/2)**2 + math.cos(a)*math.cos(b)*math.sin(math.radians(lon2-lon)/2)**2
    return 6371000 * 2 * math.asin(min(1, math.sqrt(d)))


def rows(label, key):
    response = read_record(label)['response']
    if key == 'tour':
        items = response['response']['body'].get('items', {})
        return items.get('item', []) if isinstance(items, dict) else []
    return response.get(key, [])


def write(name, data):
    (REPORT / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def full_national():
    labels = ['parking-facility-100000'] + [f'parking-national-page-{p:02}' for p in range(2, 19)]
    if not all((OUT / (label + '.json')).exists() for label in labels):
        return None
    ids, eligible = set(), set()
    realtime_ids = {x['prk_center_id'] for x in rows('parking-realtime-national', 'PrkRealtimeInfo')}
    matched_realtime = set()
    count, wire, compact_size, gzip_size = 0, 0, 0, 0
    compressor = zlib.compressobj(wbits=31)
    pages = []
    def feed(data):
        nonlocal compact_size, gzip_size
        compact_size += len(data)
        gzip_size += len(compressor.compress(data))
    feed(b'[')
    for label in labels:
        record = read_record(label)
        response = record.get('response', {})
        assert response.get('resultCode') == '0', label
        data = response['PrkSttusInfo']
        count += len(data)
        wire += record['downloadBytes']
        pages.append({'label': label, 'rows': len(data), 'totalCount': int(response['totalCount']), 'wireBytes': record['downloadBytes']})
        for x in data:
            identity = x['prk_center_id']
            ids.add(identity)
            if identity in realtime_ids:
                matched_realtime.add(identity)
            if identity in eligible or not x['prk_plce_nm'].strip():
                continue
            try:
                lat, lon = float(x['prk_plce_entrc_la']), float(x['prk_plce_entrc_lo'])
            except ValueError:
                continue
            if not (-90 <= lat <= 90 and -180 <= lon <= 180) or (lat == 0 and lon == 0):
                continue
            packed = json.dumps([identity, x['prk_plce_nm'], lat, lon], ensure_ascii=False, separators=(',', ':')).encode()
            if eligible:
                feed(b',')
            feed(packed)
            eligible.add(identity)
    feed(b']')
    gzip_size += len(compressor.flush())
    return {'pages': pages, 'receivedRows': count, 'uniqueIds': len(ids), 'duplicateRows': count-len(ids),
            'wireBytes': wire, 'namedValidCoordinateUniqueRows': len(eligible),
            'compactNamedJsonBytes': compact_size, 'compactNamedGzipBytes': gzip_size,
            'realtimeMatchedIds': len(matched_realtime), 'realtimeTotalIds': len(realtime_ids),
            'realtimeMatchedNamedValidIds': len(eligible & realtime_ids),
            'note': 'All 18 pages fetched sequentially, not a transactional snapshot. Compact rows contain ID/name/lat/lon only, no fees or region codes.'}


def navi_summary():
    path = REPORT / 'navi-timeline.txt'
    if not path.exists():
        return None
    events = []
    for line in path.read_text(encoding='utf-8').splitlines():
        match = re.match(r'(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})', line)
        if match:
            timestamp = datetime.strptime('2026-' + match[1], '%Y-%m-%d %H:%M:%S.%f')
            events.append((timestamp, line))
    def latency(start, finish):
        t = next(time for time, line in events if start in line)
        end = next(time for time, line in events if time >= t and finish in line)
        return round((end-t).total_seconds()*1000)
    return {'sdkRoute2StartedMs': latency('REQUEST_ROUTE latitude=', ' ROUTE_STARTED '),
            'searchRoute2StartedMs': latency('HOST_MARK ui-search2-destination-click', ' ROUTE_STARTED '),
            'favoriteRoute1StartedMs': latency('HOST_MARK favorite-select-click', ' ROUTE_STARTED '),
            'favoriteRoute2StartedMs': latency('HOST_MARK favorite2-select-click', ' ROUTE_STARTED '),
            'pollDestinationMs': [latency('OP query label=poll-' + str(i), ' DESTINATION ') for i in range(1, 4)],
            'cancelStatusMs': latency('OP cancel label=final-cancel', ' NAV_STATUS FREE_DRIVE'),
            'cancelEmptyDestinationMs': latency('OP cancel label=final-cancel', ' DESTINATION '),
            'destinationChangedObserved': any(' DESTINATION_CHANGED' in line for _, line in events),
            'note': 'Stationary emulator observations. UI host marks include ADB delivery overhead; not a timing guarantee.'}


def main():
    summary = {'measuredAtKst': datetime.now().astimezone().isoformat(), 'regions': {}}
    summary['navi'] = navi_summary()
    all_facility = rows('parking-facility-100000', 'PrkSttusInfo')
    total = int(read_record('parking-facility-100000')['response']['totalCount'])
    compact = [[x['prk_center_id'], x['prk_plce_nm'], x['prk_plce_entrc_la'], x['prk_plce_entrc_lo']]
               for x in all_facility if x['prk_plce_nm'].strip() and x['prk_plce_entrc_la'] and x['prk_plce_entrc_lo']]
    compact_bytes = json.dumps(compact, ensure_ascii=False, separators=(',', ':')).encode()
    raw_bytes = read_record('parking-facility-100000')['downloadBytes']
    summary['nationalFacility'] = {'totalCount': total, 'sampleRows': len(all_facility),
        'sampleUniqueIds': len({x['prk_center_id'] for x in all_facility}), 'sampleDownloadBytes': raw_bytes,
        'projectedWireBytes': round(raw_bytes / len(all_facility) * total),
        'validNamedSampleRows': len(compact), 'sampleCompactNamedBytes': len(compact_bytes),
        'sampleCompactNamedGzipBytes': len(gzip.compress(compact_bytes, mtime=0)),
        'projectedCompactNamedBytes': round(len(compact_bytes) / len(all_facility) * total),
        'projectedCompactNamedGzipBytes': round(len(gzip.compress(compact_bytes, mtime=0)) / len(all_facility) * total),
        'projectionWarning': 'Extrapolation from first 100000 rows, not a nationwide download; excludes fees.'}
    national_rt = rows('parking-realtime-national', 'PrkRealtimeInfo')
    summary['nationalRealtime'] = {'rows': len(national_rt), 'uniqueIds': len({x['prk_center_id'] for x in national_rt}),
        'positiveRows': sum(int(x['pkfc_Available_ParkingLots_total']) > 0 for x in national_rt),
        'fieldUnion': sorted({k for x in national_rt for k in x}),
        'freshnessKnownRows': 0, 'freshWithinTenMinutesRatio': None,
        'note': 'No production/update timestamp in any record; 0 known is not 0% fresh.'}
    national = full_national()
    if national:
        summary['fullNationalFacility'] = national
    for region in ['seoul-jung', 'seoul-gangnam', 'ulsan-nam']:
        suffix = '' if region == 'seoul-jung' else '-single-page'
        facilities = rows(region + '-PrkSttusInfo' + suffix, 'PrkSttusInfo')
        operations = rows(region + '-PrkOprInfo' + suffix, 'PrkOprInfo')
        realtime = rows(region + '-PrkRealtimeInfo', 'PrkRealtimeInfo')
        ids = {x['prk_center_id'] for x in facilities}
        summary['regions'][region] = {'facilityRows': len(facilities), 'uniqueIds': len(ids),
            'facilityReportedTotal': int(read_record(region + '-PrkSttusInfo' + suffix)['response']['totalCount']),
            'namedRows': sum(bool(x['prk_plce_nm'].strip()) for x in facilities),
            'coordinateRows': sum(bool(x['prk_plce_entrc_la'] and x['prk_plce_entrc_lo']) for x in facilities),
            'operationRows': len(operations), 'operationMatches': sum(x['prk_center_id'] in ids for x in operations),
            'numericBasicFeeRows': sum(x.get('basic_info', {}).get('parking_chrge_bs_chrge', '').isdigit() for x in operations),
            'realtimeRows': len(realtime), 'realtimeMatches': sum(x['prk_center_id'] in ids for x in realtime),
            'realtimePositiveRows': sum(int(x['pkfc_Available_ParkingLots_total']) > 0 for x in realtime)}
    facilities = {x['prk_center_id']: x for x in rows('seoul-jung-PrkSttusInfo', 'PrkSttusInfo')}
    operations = {x['prk_center_id']: x for x in rows('seoul-jung-PrkOprInfo', 'PrkOprInfo')}
    candidates = []
    for realtime in rows('seoul-jung-PrkRealtimeInfo', 'PrkRealtimeInfo'):
        facility = facilities.get(realtime['prk_center_id'])
        if not facility or not facility['prk_plce_entrc_la'] or not facility['prk_plce_entrc_lo']:
            continue
        d = distance(37.567, 126.978, float(facility['prk_plce_entrc_la']), float(facility['prk_plce_entrc_lo']))
        if d <= 500 and int(realtime['pkfc_Available_ParkingLots_total']) > 0:
            candidates.append({'distanceMeters': round(d, 2), 'facility': facility,
                               'operation': operations[realtime['prk_center_id']], 'realtime': realtime})
    candidates.sort(key=lambda x: x['distanceMeters'])
    assert candidates
    write('samples/scene-D.json', {'destination': {'name': '서울시청 주변', 'latitude': 37.567, 'longitude': 126.978},
        'note': 'Three original API rows joined by prk_center_id; timestamp/freshness unavailable. Not a live freshness guarantee.',
        'sourceSamples': ['seoul-jung-PrkSttusInfo.json', 'seoul-jung-PrkOprInfo.json', 'seoul-jung-PrkRealtimeInfo.json'],
        'candidate': candidates[0]})
    summary['sceneD'] = {'distanceMeters': candidates[0]['distanceMeters'], 'within500Meters': len(candidates)}
    summary['tour'] = {}
    for region, lat, lon in [('seoul', 37.567, 126.978), ('ulsan', 35.548, 129.297)]:
        attractions = {str(k): len(rows(f'tour-{region}-location-{k}', 'tour')) for k in [12, 14, 28, 15]}
        all_nearby = rows('tour-' + region + '-location-all', 'tour')
        events = rows('tour-' + region + '-ldong-today', 'tour')
        nearby = []
        for event in events:
            if not event.get('mapy') or not event.get('mapx'):
                continue
            d = distance(lat, lon, float(event['mapy']), float(event['mapx']))
            if d <= 5000 and event['eventstartdate'] <= '20261029' <= event['eventenddate']:
                nearby.append({'distanceMeters': round(d, 2), 'event': event})
        nearby.sort(key=lambda x: x['distanceMeters'])
        summary['tour'][region] = {'locationByType': attractions, 'allTypesWithin5km': len(all_nearby),
            'allowedTypesWithin3kmFromCombined': sum(x.get('contenttypeid') in ['12', '14', '28'] and float(x.get('dist') or 'inf') <= 3000 for x in all_nearby),
            'oldAreaTodayEvents': len(rows(f'tour-{region}-festivals-day', 'tour')),
            'legalDongTodayEvents': len(events), 'eventsWithin5kmToday': len(nearby),
            'nearestToday': nearby[:2]}
        if region == 'seoul':
            assert nearby
            write('samples/scene-E.json', {'destination': {'name': '서울시청 주변', 'latitude': lat, 'longitude': lon},
                'date': '2026-10-29', 'sourceSample': 'tour-seoul-ldong-today.json',
                'note': 'Original event row plus client-side haversine distance; no dist field in festival response.',
                'candidate': nearby[0]})
    summary['calls'] = {}
    for service, pathpart in [('parking', 'Parking_v1'), ('tour', 'KorService2')]:
        records = []
        for p in OUT.glob('*.json'):
            if p.stem.startswith('scene-'):
                continue
            d = read_record(p.stem)
            if pathpart in urlsplit(d.get('url', '')).path:
                records.append(d)
        times = [d['durationMs'] for d in records]
        summary['calls'][service] = {'count': len(records), 'medianMs': statistics.median(times), 'maxMs': max(times)}
    write('summary.json', summary)
    for p in list(OUT.glob('*.json')):
        if p.stem.startswith('scene-'):
            continue
        record = read_record(p.stem)
        publish(record, p.stem)
    print(json.dumps({k: v for k, v in summary.items() if k != 'tour'}, ensure_ascii=False, indent=2))
    print('tour counts', {k: {n: v for n, v in d.items() if n != 'nearestToday'} for k, d in summary['tour'].items()})


if __name__ == '__main__':
    main()
