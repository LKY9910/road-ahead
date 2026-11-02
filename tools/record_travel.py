"""Record T-34 travel responses once; Python 3.12, standard library only."""

import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import html
import json
import math
import os
from pathlib import Path
import re
import tempfile
import time
from urllib.error import HTTPError, URLError
from urllib.parse import quote, unquote, urlencode
from urllib.request import build_opener, HTTPRedirectHandler

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'tools/demo-src/travel'
KST = timezone(timedelta(hours=9))
PARKING_POINT = (37.5733652, 126.9736652)
SEED_POINT = (37.57200988776222, 126.97630706471843)
SEED_SOURCE = 'https://access.visitkorea.or.kr/ms/detail.do?cotId=4d28e66c-2efa-47d7-881e-403875952880'
FORECAST = ROOT / 'docs/spikes/travel-companion/forecast-20261029-1700.json'
COMMON = dict(MobileOS='AND', MobileApp='RoadAhead', _type='json', pageNo='1')
FACILITIES = {'stroller': 'STROLLER', 'lactationroom': 'LACTATION',
              'babysparechair': 'BABY_CHAIR', 'wheelchair': 'WHEELCHAIR',
              'elevator': 'ELEVATOR', 'restroom': 'ACCESSIBLE_TOILET'}


def credentials(source=ROOT / 'api'):
    keys = {}
    for line in source.read_text(encoding='utf-8-sig').splitlines() if source.exists() else []:
        match = re.search(r"['\"]([A-Za-z0-9%+/=_-]{15,})['\"]", line)
        if not match:
            match = re.search(r"([A-Za-z0-9%+/=_-]{15,})['\"]?\s*$", line)
        if match:
            for service, label in [('tour', '신호'), ('seoul', '서울')]:
                if label in line[:match.start()]:
                    keys[service] = unquote(match[1])
    for service, env in [('tour', 'ROAD_AHEAD_SIGNAL_KEY'), ('seoul', 'ROAD_AHEAD_SEOUL_KEY')]:
        if os.getenv(env):
            keys[service] = unquote(os.environ[env].strip())
    return keys


def scrub(text, secrets):
    for key in secrets:
        for variant in {key, quote(key, safe=''), quote(quote(key, safe=''), safe=''), html.escape(key)}:
            text = text.replace(variant, '***')
    return re.sub(r'(?i)((?:serviceKey|apiKey|authKey)=)[^&\s<>"\']+', r'\1***', text)


def immutable(path, value):
    if path.exists():
        if json.loads(path.read_text(encoding='utf-8')) != value:
            raise FileExistsError(f'기존 기록이 다름: {path.name}')
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8', newline='\n') as stream:
        stream.write(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def capture(service, operation, params, label, keys, out=OUT):
    if not re.fullmatch(r'[A-Za-z0-9_-]+', label):
        raise ValueError('simple record label required')
    request = dict(service=service, operation=operation, params=params)
    path = out / f'{label}.json'
    if path.exists():
        record = json.loads(path.read_text(encoding='utf-8'))
        if record['request'] != request:
            raise FileExistsError(f'기존 요청 인자가 다름: {label}')
        return record
    attempt = out / '.requests' / f'{label}.json'
    if attempt.exists():
        raise FileExistsError(f'응답 없는 이전 요청: {label}; 중복 호출 방지를 위해 중단')
    key = keys.get('seoul' if service == 'SeoulParking' else 'tour')
    if not key:
        raise ValueError('필요한 API 키 없음')
    if service == 'SeoulParking':
        url = f'http://openapi.seoul.go.kr:8088/{quote(key, safe="")}/json/GetParkingInfo/1/1000/'
    else:
        url = f'https://apis.data.go.kr/B551011/{service}/{operation}?' + urlencode({**params, 'serviceKey': key})
    record = dict(requestedAtKst=datetime.now(KST).isoformat(), request=request,
                  url=scrub(url, keys.values()))
    # Reserve before I/O: an interrupted request must never be silently repeated.
    immutable(attempt, record)
    begin = time.monotonic()
    try:
        try:
            response = build_opener(NoRedirect()).open(url, timeout=60)
        except HTTPError as error:
            response = error
        with response:
            record['httpStatus'] = response.code
            body = response.read()
        record['downloadBytes'] = len(body)
        clean = scrub(body.decode('utf-8-sig'), keys.values())
        try:
            record['response'] = json.loads(clean)
        except ValueError:
            record['body'] = clean
    except (OSError, URLError, UnicodeError) as error:
        record['error'] = scrub(f'{type(error).__name__}: {error}', keys.values())
    record['durationMs'] = round((time.monotonic() - begin) * 1000, 2)
    record = json.loads(scrub(json.dumps(record, ensure_ascii=False), keys.values()))
    immutable(path, record)
    print(f'{label}: HTTP={record.get("httpStatus")} bytes={record.get("downloadBytes", 0)}', flush=True)
    return record


def tour_rows(record):
    if record.get('httpStatus') != 200:
        raise ValueError('관광 API HTTP 실패; 저장된 기록 확인')
    response = record['response']['response']
    if response['header']['resultCode'] != '0000':
        raise ValueError('관광 API 결과 실패; 저장된 기록 확인')
    items = response['body']['items']
    if items == '':
        return []
    rows = items['item']
    return rows if isinstance(rows, list) else [rows]


def distance(a, b):
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    h = math.sin((lat2-lat1)/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin((lon2-lon1)/2)**2
    return 6371008.8 * 2 * math.asin(min(1, math.sqrt(h)))


def grid(point):
    re_grid = 6371.00877 / 5.0
    slat1, slat2, olon, olat = map(math.radians, (30, 60, 126, 38))
    sn = math.log(math.cos(slat1)/math.cos(slat2)) / math.log(math.tan(math.pi/4+slat2/2)/math.tan(math.pi/4+slat1/2))
    sf = math.tan(math.pi/4+slat1/2)**sn * math.cos(slat1) / sn
    ro = re_grid * sf / math.tan(math.pi/4+olat/2)**sn
    lat, lon = map(math.radians, point)
    ra = re_grid * sf / math.tan(math.pi/4+lat/2)**sn
    theta = (lon-olon+math.pi) % (2*math.pi) - math.pi
    return (math.floor(ra*math.sin(theta*sn)+43+.5),
            math.floor(ro-ra*math.cos(theta*sn)+136+.5))


def nearby(rows, point):
    candidates = []
    seen = set()
    for row in rows:
        if str(row['contenttypeid']) not in {'12', '14', '28'}:
            continue
        lat, lon = float(row['mapy']), float(row['mapx'])
        if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180 and lat != 0 and lon != 0):
            raise ValueError('관광 장소 좌표 오류')
        meters = distance(point, (lat, lon))
        if meters <= 3000 and row['contentid'] not in seen:
            candidates.append({**row, 'distanceMeters': meters})
            seen.add(row['contentid'])
    return sorted(candidates, key=lambda row: row['distanceMeters'])


def facility_targets(places, confirmed_pet):
    return list(dict.fromkeys([r['contentid'] for r in places[:10]] +
        [r['contentid'] for r in places if str(r['contenttypeid']) == '14'][:10] + confirmed_pet))


def pet_scope(rows):
    value = str(rows[0].get('acmpyTypeCd', '')) if rows else ''
    if '전구역' in value:
        return 'ALL'
    if '일부구역' in value:
        return 'PARTIAL'
    return None


def facilities(rows):
    if not rows:
        return []
    row = rows[0]
    result = [facility for field, facility in FACILITIES.items()
              if str(row.get(field, '')).strip() and
              not any(word in str(row[field]) for word in ('없음', '불가'))]
    if any(word in str(row.get(field, '')) for field in ('route', 'exit') for word in ('턱이 없', '경사로')):
        result.append('STEP_FREE')
    return result


def seoul_summary(record):
    data = record['response']['GetParkingInfo']
    if record['httpStatus'] != 200 or data['RESULT']['CODE'] != 'INFO-000':
        raise ValueError('서울 API 실패')
    matches = [r for r in data['row'] if str(r['PKLT_CD']) == '171721']
    if len(matches) != 1:
        raise ValueError('세종로 행이 정확히 1개여야 함')
    row = matches[0]
    updated = datetime.strptime(row['NOW_PRK_VHCL_UPDT_TM'], '%Y-%m-%d %H:%M:%S').replace(tzinfo=KST)
    age = (datetime.fromisoformat(record['requestedAtKst']) - updated).total_seconds()
    total, occupied = float(row['TPKCT']), float(row['NOW_PRK_VHCL_CNT'])
    valid = (str(row['PRK_STTS_YN']) == '1' and 0 <= age <= 600 and
             math.isfinite(total) and math.isfinite(occupied) and total.is_integer() and occupied.is_integer() and 0 <= occupied <= total and total > 0)
    return dict(id='171721', name=row['PKLT_NM'], linked=str(row['PRK_STTS_YN']) == '1',
                total=total, occupied=occupied, updatedAtKst=updated.isoformat(),
                ageSeconds=round(age, 3), fresh=0 <= age <= 600,
                available=int(total-occupied) if valid else None, rows=len(data['row']))


def main():
    keys = credentials()
    params = {**COMMON, 'mapX': f'{SEED_POINT[1]:.3f}', 'mapY': f'{SEED_POINT[0]:.3f}',
              'radius': '3000', 'arrange': 'E', 'numOfRows': '300'}
    record = capture('KorService2', 'locationBasedList2', params, 'places', keys)
    rows = tour_rows(record)
    matches = [r for r in rows if r['title'] == '세종문화회관' and str(r['contenttypeid']) == '14']
    if len(matches) > 1:
        raise ValueError('세종문화회관 관광 좌표가 중복됨')
    # The official accessible-tourism page supplies coordinates even if the API omits the venue.
    destination = matches[0] if matches else dict(title='세종문화회관', contentid=None,
        mapy=SEED_POINT[0], mapx=SEED_POINT[1])
    point = (float(destination['mapy']), float(destination['mapx']))
    if (f'{point[1]:.3f}', f'{point[0]:.3f}') != (params['mapX'], params['mapY']):
        raise ValueError('확정 목적지와 목록 조회 반올림 좌표가 다름')
    meters = distance(point, PARKING_POINT)
    if meters > 500 or grid(point) != (60, 127):
        raise ValueError('목적지의 주차장 거리 또는 예보 격자 조건 실패')
    date = datetime.fromisoformat(record['requestedAtKst']).strftime('%Y%m%d')
    festival = capture('KorService2', 'searchFestival2', {**COMMON, 'eventStartDate': date,
        'eventEndDate': date, 'numOfRows': '1000'}, 'festivals', keys)
    festivals = tour_rows(festival)
    pet_record = capture('KorPetTourService2', 'locationBasedList2',
        {**params, 'numOfRows': '100'}, 'pet-places', keys)
    pets = nearby(tour_rows(pet_record), point)
    places = nearby(rows, point)
    pet_details, scopes = {}, {}
    for row in pets:
        content_id = row['contentid']
        detail = tour_rows(capture('KorPetTourService2', 'detailPetTour2',
            {**COMMON, 'contentId': content_id}, f'pet-{content_id}', keys))
        pet_details[content_id] = bool(detail)
        scope = pet_scope(detail)
        if scope:
            scopes[content_id] = scope
    targets = facility_targets(places, list(scopes))
    facility_details, parsed_facilities = {}, {}
    for content_id in targets:
        detail = tour_rows(capture('KorWithService2', 'detailWithTour2',
            {**COMMON, 'contentId': content_id}, f'facilities-{content_id}', keys))
        facility_details[content_id] = bool(detail)
        parsed_facilities[content_id] = facilities(detail)
    seoul = capture('SeoulParking', 'GetParkingInfo', dict(start=1, end=1000, format='json'), 'seoul-parking', keys)
    forecast = json.loads(FORECAST.read_text(encoding='utf-8'))
    immutable(OUT / FORECAST.name, forecast)
    counts = {}
    for path in sorted((OUT / '.requests').glob('*.json')):
        service = json.loads(path.read_text(encoding='utf-8'))['request']['service']
        counts[service] = counts.get(service, 0) + 1
    summary = dict(recordedAtKst=record['requestedAtKst'],
        destination=dict(id=destination['contentid'], name=destination['title'], lat=point[0], lon=point[1],
            coordinateSource='places.json' if matches else SEED_SOURCE,
            presentInLocationList=bool(matches), seedCoordinateSource=SEED_SOURCE,
            parkingTsPoint=list(PARKING_POINT), parkingDistanceMeters=round(meters, 3), grid=list(grid(point))),
        callsByService={**counts, 'VilageFcstInfoService_2.0': 0},
        places=dict(received=len(rows), eligible=len(places), totalCount=record['response']['response']['body']['totalCount']),
        festivals=dict(received=len(festivals), totalCount=festival['response']['response']['body']['totalCount']),
        pet=dict(received=len(tour_rows(pet_record)), eligible=len(pets), targets=[r['contentid'] for r in pets],
            populatedDetails=sum(pet_details.values()), confirmedScopes=scopes),
        facilities=dict(targets=targets, nearestTen=[r['contentid'] for r in places[:10]],
            nearestCultureTen=[r['contentid'] for r in places if str(r['contenttypeid']) == '14'][:10],
            populatedDetails=sum(facility_details.values()), withRecognizedFacilities=sum(bool(v) for v in parsed_facilities.values()),
            parsed=parsed_facilities),
        seoul=seoul_summary(seoul),
        forecast=dict(file=FORECAST.name, source=FORECAST.relative_to(ROOT).as_posix(),
            sourceSha256=hashlib.sha256(FORECAST.read_bytes()).hexdigest(), requestedAtKst=forecast['requestedAtKst']))
    immutable(OUT / 'summary.json', summary)
    print(json.dumps(summary, ensure_ascii=False, indent=2))


def self_test():
    secret = 'synthetic/path+key='
    assert scrub(f'http://host/{quote(secret, safe="")}/json/ {secret}', [secret]) == 'http://host/***/json/ ***'
    assert grid((37.5663, 126.9779)) == (60, 127)
    assert grid(SEED_POINT) == (60, 127)
    assert distance(SEED_POINT, SEED_POINT) == 0
    assert 200 < distance(SEED_POINT, PARKING_POINT) < 300
    rows = [dict(contentid=str(i), contenttypeid='14' if i >= 12 else '12',
                 title=str(i), mapy=37.572 + i * .0001, mapx=126.976)
            for i in range(24)]
    rows += [dict(rows[0], contentid='food', contenttypeid='39'),
             dict(rows[0], contentid='far', mapy=38)]
    candidates = nearby(rows, SEED_POINT)
    assert len(candidates) == 24
    assert set(facility_targets(candidates, ['pet-only'])) == {str(i) for i in list(range(10)) + list(range(12, 22))} | {'pet-only'}
    assert pet_scope([{'acmpyTypeCd': '일부구역 동반가능'}]) == 'PARTIAL'
    assert pet_scope([{'acmpyTypeCd': '전구역 동반가능'}]) == 'ALL'
    assert pet_scope([{'acmpyTypeCd': '동반불가'}]) is None
    assert facilities([{'stroller': '대여 불가', 'restroom': '없음', 'exit': '턱이 있음'}]) == []
    assert facilities([{'stroller': '대여가능', 'exit': '턱이 없어 접근 가능'}]) == ['STROLLER', 'STEP_FREE']
    row = dict(PKLT_CD='171721', PKLT_NM='test', PRK_STTS_YN='1', TPKCT=1260,
               NOW_PRK_VHCL_CNT=411, NOW_PRK_VHCL_UPDT_TM='2026-10-29 19:58:43')
    record = dict(httpStatus=200, requestedAtKst='2026-10-29T19:59:23+09:00',
                  response={'GetParkingInfo': dict(RESULT={'CODE': 'INFO-000'}, row=[row])})
    assert seoul_summary(record)['available'] == 849
    for changes in [dict(PRK_STTS_YN='0'), dict(NOW_PRK_VHCL_CNT=1261),
                    dict(NOW_PRK_VHCL_CNT=-1), dict(NOW_PRK_VHCL_CNT=411.5),
                    dict(NOW_PRK_VHCL_UPDT_TM='2026-10-29 19:40:00'),
                    dict(NOW_PRK_VHCL_UPDT_TM='2026-10-29 20:00:00')]:
        sample = {**record, 'response': {'GetParkingInfo': dict(RESULT={'CODE': 'INFO-000'}, row=[{**row, **changes}])}}
        assert seoul_summary(sample)['available'] is None
    with tempfile.TemporaryDirectory() as tmp:
        credential_file = Path(tmp) / 'api'
        credential_file.write_text("서울 api = 'synthetic-seoul-value' # comment\n", encoding='utf-8')
        assert credentials(credential_file).get('seoul') == unquote(os.getenv('ROAD_AHEAD_SEOUL_KEY', 'synthetic-seoul-value').strip())
        path = Path(tmp) / 'one.json'
        immutable(path, {'body': 'original'})
        before = path.read_bytes()
        immutable(path, {'body': 'original'})
        try:
            immutable(path, {'body': 'changed'})
            raise AssertionError('existing record was accepted')
        except FileExistsError:
            pass
        try:
            capture('KorService2', 'locationBasedList2', COMMON, '../escape', {}, Path(tmp))
            raise AssertionError('unsafe label was accepted')
        except ValueError:
            pass
        assert path.read_bytes() == before
        request = dict(service='KorService2', operation='locationBasedList2', params=COMMON)
        immutable(Path(tmp) / 'cached.json', dict(request=request, response='original'))
        assert capture('KorService2', 'locationBasedList2', COMMON, 'cached', {}, Path(tmp))['response'] == 'original'
        try:
            capture('KorService2', 'locationBasedList2', {}, 'cached', {}, Path(tmp))
            raise AssertionError('different request was accepted')
        except FileExistsError:
            pass
        immutable(Path(tmp) / '.requests' / 'pending.json', request)
        try:
            capture('KorService2', 'locationBasedList2', COMMON, 'pending', {}, Path(tmp))
            raise AssertionError('interrupted request was retried')
        except FileExistsError:
            pass
    if (OUT / 'summary.json').exists():
        from contextlib import redirect_stdout
        from io import StringIO
        from unittest.mock import patch

        records = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in OUT.glob('*.json')}
        responses = [record for record in records.values() if 'request' in record]
        identities = {json.dumps(record['request'], sort_keys=True) for record in responses}
        assert len(responses) == len(identities) == 35
        assert all(record['httpStatus'] == 200 for record in responses)
        point = (records['summary']['destination']['lat'], records['summary']['destination']['lon'])
        pets = nearby(tour_rows(records['pet-places']), point)
        pet_ids = {row['contentid'] for row in pets}
        assert pet_ids == {name[4:] for name in records if name.startswith('pet-') and name != 'pet-places'}
        confirmed = [content_id for content_id in pet_ids if pet_scope(tour_rows(records['pet-' + content_id]))]
        targets = facility_targets(nearby(tour_rows(records['places']), point), confirmed)
        assert set(targets) == {name[11:] for name in records if name.startswith('facilities-')}
        assert records[FORECAST.stem] == json.loads(FORECAST.read_text(encoding='utf-8'))

        def fingerprint():
            return {p: (hashlib.sha256(p.read_bytes()).hexdigest(), p.stat().st_mtime_ns)
                    for p in OUT.rglob('*') if p.is_file()}

        before = fingerprint()
        with patch(__name__ + '.build_opener', side_effect=AssertionError('cached run used network')):
            with redirect_stdout(StringIO()):
                main()
        assert fingerprint() == before
    print('self-test passed: redaction, grid, distance, targets, details, immutable records; no network')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        main()
