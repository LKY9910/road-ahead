"""Build the T-28 TS parking fee asset from cached nationwide JSON pages."""
import argparse
from datetime import datetime
import html
import json
import math
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
from urllib.parse import quote, unquote, urlencode

ROOT = Path(__file__).resolve().parents[1]
HEADER = '#id\tname\tlat\tlon\tbasicMinutes\tbasicFeeWon'
PAGE_SIZE = 100000
ENDPOINT = 'https://apis.data.go.kr/B553881/Parking_v1/PrkOprInfo_v1'
SCENE_D = '04540-11291-00001-00-1'


def integer(value):
    text = str(value).strip()
    return int(text) if re.fullmatch(r'[+-]?[0-9]+', text) else None


def make_rows(facilities, operations):
    candidates, seen = {}, set()
    for item in facilities:
        identity = item.get('prk_center_id')
        if not isinstance(identity, str) or not identity or any(c in identity for c in '\t\r\n') or identity in seen:
            continue
        seen.add(identity)
        name = item.get('prk_plce_nm')
        if not isinstance(name, str) or not name.strip():
            continue
        try:
            lat, lon = float(item.get('prk_plce_entrc_la')), float(item.get('prk_plce_entrc_lo'))
        except (TypeError, ValueError):
            continue
        if not (33 <= lat <= 39 and 124 <= lon <= 132):
            continue
        name = re.sub(r'[\t\r\n]', ' ', name).strip()
        candidates[identity] = (name, lat, lon, item.get('parkng_chrge_se'))
    del seen
    result, seen = {}, set()
    for item in operations:
        identity = item.get('prk_center_id')
        if identity not in candidates or identity in seen:
            continue
        seen.add(identity)
        fee_info = item.get('basic_info')
        if not isinstance(fee_info, dict):
            continue
        minutes = integer(fee_info.get('parking_chrge_bs_time'))
        fee = integer(fee_info.get('parking_chrge_bs_chrge'))
        name, lat, lon, free = candidates[identity]
        if minutes is None or minutes <= 0 or fee is None or (fee == 0 and free != '1'):
            continue
        result[identity] = f'{identity}\t{name}\t{lat}\t{lon}\t{minutes}\t{fee}'
    return [result[identity] for identity in sorted(result)]


def credential():
    value = os.getenv('ROAD_AHEAD_SIGNAL_KEY')
    if value:
        return unquote(value.strip())
    source = ROOT / 'api'
    for line in source.read_text(encoding='utf-8-sig').splitlines() if source.exists() else []:
        match = re.search(r"([A-Za-z0-9%+/=_-]{20,})['\"]?\s*$", line)
        if match and '신호등' in line[:match.start()]:
            return unquote(match[1])
    raise ValueError('missing signal credential in api or ROAD_AHEAD_SIGNAL_KEY')


def redact(text, key):
    for value in {key, quote(key, safe=''), quote(quote(key, safe=''), safe=''), html.escape(key)}:
        text = text.replace(value, '***')
    return text


def load_response(path, field, page):
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    response = data.get('response', data)
    if (str(response.get('resultCode')) != '0' or not isinstance(response.get(field), list)
            or integer(response.get('pageNo')) != page or integer(response.get('numOfRows')) != PAGE_SIZE
            or integer(response.get('totalCount')) is None or int(response['totalCount']) < 0):
        raise ValueError(f'invalid cached {field} page {page}: {path.name}')
    return response


def operation_page(raw, page):
    path = raw / f'parking-operation-national-page-{page:02}.json'
    if path.exists():
        response = load_response(path, 'PrkOprInfo', page)
        print(f'cached operation page {page}: rows={len(response["PrkOprInfo"])} total={response["totalCount"]}', flush=True)
        return path, int(response['totalCount'])
    key = credential()
    params = {'pageNo': page, 'numOfRows': PAGE_SIZE, 'format': 2}
    url = ENDPOINT + '?' + urlencode({**params, 'serviceKey': key})
    requested = datetime.now().astimezone().isoformat()
    start = time.monotonic()
    result = subprocess.run(['curl.exe', '--config', '-', '--silent', '--show-error', '--include',
        '--connect-timeout', '15', '--max-time', '180', '--proto', '=https'],
        input=f'url = "{url}"\n'.encode(), capture_output=True, timeout=185)
    if result.returncode:
        raise RuntimeError(f'operation page {page}: ' + redact(result.stderr.decode(errors='replace'), key))
    headers, body = result.stdout.split(b'\r\n\r\n', 1)
    while body.startswith(b'HTTP/'):
        headers, body = body.split(b'\r\n\r\n', 1)
    status = int(headers.splitlines()[0].split()[1])
    if status != 200:
        raise RuntimeError(f'operation page {page}: HTTP {status}')
    clean_body = redact(body.decode('utf-8-sig'), key)
    try:
        response = json.loads(clean_body)
    except json.JSONDecodeError as error:
        # The gateway can return XML/HTML even when format=2 and HTTP status is 200.
        failure = {'requestedAtKst': requested, 'httpStatus': status,
                   'downloadBytes': len(body), 'body': clean_body}
        raw.mkdir(parents=True, exist_ok=True)
        (raw / f'parking-operation-page-{page:02}-failure.json').write_text(
            json.dumps(failure, ensure_ascii=False), encoding='utf-8')
        raise ValueError(f'operation page {page}: non-JSON HTTP {status} ({len(body)} bytes); redacted failure saved in raw cache') from error
    record = {'requestedAtKst': requested, 'url': ENDPOINT + '?' + urlencode(params) + '&serviceKey=***',
              'httpStatus': status, 'downloadBytes': len(body),
              'durationMs': round((time.monotonic()-start)*1000, 2), 'response': response}
    raw.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.tmp')
    try:
        temporary.write_text(json.dumps(record, ensure_ascii=False, separators=(',', ':'))+'\n', encoding='utf-8')
        checked = load_response(temporary, 'PrkOprInfo', page)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)
    print(f'downloaded operation page {page}: rows={len(checked["PrkOprInfo"])} total={checked["totalCount"]} bytes={len(body)} ms={record["durationMs"]}', flush=True)
    return path, int(checked['totalCount'])


def facility_paths(raw):
    first = raw / 'parking-facility-100000.json'
    response = load_response(first, 'PrkSttusInfo', 1)
    count = math.ceil(int(response['totalCount']) / PAGE_SIZE)
    paths = [first] + [raw / f'parking-national-page-{page:02}.json' for page in range(2, count+1)]
    for path in paths:
        if not path.is_file():
            raise ValueError(f'missing nationwide facility page: {path.name}')
    return paths


def page_rows(paths, field):
    for page, path in enumerate(paths, 1):
        response = load_response(path, field, page)
        yield from response[field]


def build(raw):
    facilities = facility_paths(raw)
    operations, page = [], 1
    while True:
        path, total = operation_page(raw, page)
        operations.append(path)
        if page * PAGE_SIZE >= total:
            break
        page += 1
    print(f'joining facility pages={len(facilities)} operation pages={len(operations)}', flush=True)
    rows = make_rows(page_rows(facilities, 'PrkSttusInfo'), page_rows(operations, 'PrkOprInfo'))
    output = ROOT / 'app/src/main/assets/parking/lots.tsv'
    if output.exists() and output.read_text(encoding='utf-8-sig').splitlines()[:1] != [HEADER]:
        raise ValueError('existing asset has an unexpected header; refusing to replace it')
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix('.tsv.tmp')
    try:
        temporary.write_text(HEADER+'\n'+'\n'.join(rows)+'\n', encoding='utf-8', newline='\n')
        temporary.replace(output)
    finally:
        temporary.unlink(missing_ok=True)
    print(f'asset rows={len(rows)} bytes={output.stat().st_size} path={output.relative_to(ROOT)}', flush=True)
    scene = next((row for row in rows if row.startswith(SCENE_D+'\t')), None)
    expected = 'included' if scene and scene.split('\t')[1] == '동국제강' and scene.split('\t')[4:] == ['30', '4200'] else 'MISSING_OR_CHANGED'
    print(f'scene D {expected}: {scene or SCENE_D}', flush=True)
    if output.stat().st_size > 5_000_000:
        print('asset exceeds 5MB; do not commit before coordinator decision', flush=True)
    if expected != 'included':
        raise ValueError('expected scene D fee row missing or changed; ask coordinator')


def self_test():
    key = 'synthetic/credential+value='
    variants = [key, quote(key, safe=''), quote(quote(key, safe=''), safe='')]
    clean = redact(' '.join(variants), key)
    assert all(value not in clean for value in variants)
    def facility(identity, name='Parking', lat='37.5', lon='127.0', free='2'):
        return {'prk_center_id': identity, 'prk_plce_nm': name,
                'prk_plce_entrc_la': lat, 'prk_plce_entrc_lo': lon, 'parkng_chrge_se': free}
    def operation(identity, minutes='30', fee='4200'):
        return {'prk_center_id': identity, 'basic_info': {
            'parking_chrge_bs_time': minutes, 'parking_chrge_bs_chrge': fee}}
    facilities = [facility('b', 'Park\tB\r\nNorth'), facility('a', free='1'),
        facility('empty-fee'), facility('paid-zero'), facility('outside', lat='40'),
        facility('nan', lon='NaN'), facility('no-name', name=' '),
        facility('zero-time'), facility('fraction'), facility('first-fee'),
        facility('first-name', name=''), facility('first-name', name='Later'),
        facility('b', name='Duplicate')]
    operations = [operation('b'), operation('a', fee='0'), operation('empty-fee', fee=''),
        operation('paid-zero', fee='0'), operation('outside'), operation('nan'),
        operation('no-name'), operation('zero-time', minutes='0'), operation('fraction', fee='10.5'),
        operation('first-fee', fee=''), operation('first-fee'), operation('first-name'),
        operation('b', fee='9999')]
    with tempfile.TemporaryDirectory() as directory:
        folder = Path(directory)
        for name, key, values in [('facility', 'PrkSttusInfo', facilities), ('operation', 'PrkOprInfo', operations)]:
            (folder / (name+'.json')).write_text(json.dumps({key: values}), encoding='utf-8')
        actual = make_rows(json.loads((folder/'facility.json').read_text())['PrkSttusInfo'],
                           json.loads((folder/'operation.json').read_text())['PrkOprInfo'])
    assert actual == ['a\tParking\t37.5\t127.0\t30\t0',
                      'b\tPark B  North\t37.5\t127.0\t30\t4200'], actual
    print('self-test passed: fee/time/coordinate/name filters, free zero, first ID, sort and TSV sanitation; no network')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--raw', type=Path, default=ROOT / 'spikes/parking-travel/.gradle/raw')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    else:
        try:
            build(args.raw.resolve())
        except (OSError, ValueError, RuntimeError, subprocess.TimeoutExpired) as error:
            parser.exit(1, f'{type(error).__name__}: {error}\n')
