"""T-23 disposable API recorder; credentials stay in stdin and memory."""
import argparse
import copy
from datetime import datetime
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import time
from urllib.parse import urlencode, urlsplit
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/spikes/parking-travel/samples'
STATE = ROOT / 'spikes/parking-travel/.gradle/api-counts.json'
RAW = ROOT / 'spikes/parking-travel/.gradle/raw'
spec = importlib.util.spec_from_file_location('t14', ROOT / 'spikes/road-ahead/probe_api.py')
old = importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)
BASES = {'parking': 'https://apis.data.go.kr/B553881/Parking_v1/',
         'tour': 'https://apis.data.go.kr/B551011/KorService2/',
         'standard': 'https://api.data.go.kr/openapi/'}


def read_record(label):
    path = RAW / (label + '.json')
    if not path.exists():
        path = OUT / (label + '.json')
    record = json.loads(path.read_text(encoding='utf-8'))
    if 'excerpt' in record:
        raise ValueError('full redacted record required in spike .gradle/raw; refusing to aggregate an excerpt')
    return record


def publish(record, label):
    RAW.mkdir(parents=True, exist_ok=True)
    OUT.mkdir(parents=True, exist_ok=True)
    full = json.dumps(record, ensure_ascii=False, indent=2) + '\n'
    (RAW / (label + '.json')).write_text(full, encoding='utf-8')
    excerpt = copy.deepcopy(record)
    truncated = {}
    def trim(value, path='response'):
        if isinstance(value, list):
            if len(value) > 100:
                truncated[path] = {'receivedRows': len(value), 'savedRows': 100}
                del value[100:]
            for item in value:
                trim(item, path + '[]')
        elif isinstance(value, dict):
            for key, item in value.items():
                trim(item, path + '.' + key)
    trim(excerpt.get('response', {}))
    if truncated:
        excerpt['excerpt'] = truncated
        excerpt['fullRecordSha256'] = hashlib.sha256(full.encode()).hexdigest()
        excerpt['note'] = 'Response arrays excerpted; full redacted record retained in ignored spike .gradle/raw.'
    (OUT / (label + '.json')).write_text(json.dumps(excerpt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def decode(body):
    try:
        return json.loads(body)
    except ValueError:
        root = ET.fromstring(body)
        def unpack(node):
            if not len(node):
                return node.text or ''
            result = {}
            for child in node:
                value = unpack(child)
                if child.tag in result:
                    if not isinstance(result[child.tag], list):
                        result[child.tag] = [result[child.tag]]
                    result[child.tag].append(value)
                else:
                    result[child.tag] = value
            return result
        return {root.tag: unpack(root)}


def capture(service, operation, params, label):
    if not operation.replace('_', '').isalnum() or not label.replace('-', '').replace('_', '').isalnum():
        raise ValueError('simple operation and label required')
    if (OUT / (label + '.json')).exists() or (RAW / (label + '.json')).exists():
        raise ValueError('sample already exists; use a new label')
    keys = old.credentials()
    key = keys.get('signal')
    if not key:
        raise ValueError('missing credential')
    secrets = list(keys.values())
    now = datetime.now(ZoneInfo('Asia/Seoul'))
    counts = json.loads(STATE.read_text()) if STATE.exists() else {}
    day = counts.setdefault(now.date().isoformat(), {})
    if day.get(service, 0) >= (50 if service == 'tour' else 500):
        raise ValueError('measurement call cap reached')
    day[service] = day.get(service, 0) + 1
    STATE.parent.mkdir(parents=True, exist_ok=True)
    STATE.write_text(json.dumps(counts, indent=2), encoding='utf-8')
    url = BASES[service] + operation + '?' + urlencode({**params, 'serviceKey': key})
    record = {'requestedAtKst': now.isoformat(), 'url': old.scrub(url, secrets), 'callNumber': day[service]}
    start = time.perf_counter()
    try:
        result = subprocess.run(['curl.exe', '--config', '-', '--silent', '--show-error', '--include',
            '--connect-timeout', '10', '--max-time', '90', '--proto', '=https'],
            input=f'url = "{url}"\n'.encode(), capture_output=True, timeout=95)
        if result.returncode:
            record['transportError'] = old.scrub(result.stderr.decode(errors='replace'), secrets)
        else:
            headers, body = result.stdout.split(b'\r\n\r\n', 1)
            while body.startswith(b'HTTP/'):
                headers, body = body.split(b'\r\n\r\n', 1)
            record['httpStatus'] = int(headers.splitlines()[0].split()[1])
            record['downloadBytes'] = len(body)
            clean = old.scrub(body.decode('utf-8-sig', errors='replace'), secrets)
            try:
                record['response'] = decode(clean)
            except (ValueError, ET.ParseError):
                record['body'] = clean
    except (OSError, ValueError, subprocess.TimeoutExpired) as error:
        record['transportError'] = old.scrub(str(error), secrets)
    record['durationMs'] = round((time.perf_counter() - start) * 1000, 2)
    record = json.loads(old.scrub(json.dumps(record, ensure_ascii=False), secrets))
    publish(record, label)
    print(f'{label}: HTTP={record.get("httpStatus")} ms={record["durationMs"]} bytes={record.get("downloadBytes")} calls={day[service]}')
    return record


def self_test():
    old.self_test()
    assert decode('{"response":{"body":{"items":[]}}}')['response']['body']['items'] == []
    assert decode('<response><items><item><id>1</id></item><item><id>2</id></item></items></response>')['response']['items']['item'][1]['id'] == '2'
    assert all(urlsplit(base).scheme == 'https' for base in BASES.values())
    print('T-23 JSON/XML/host assertions passed; no network calls')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true')
    parser.add_argument('--service', choices=BASES)
    parser.add_argument('--operation')
    parser.add_argument('--label')
    parser.add_argument('--param', action='append', default=[])
    args = parser.parse_args()
    if args.self_test:
        self_test()
    elif args.service and args.operation and args.label:
        capture(args.service, args.operation, dict(value.split('=', 1) for value in args.param), args.label)
    else:
        parser.error('service, operation and label required')
