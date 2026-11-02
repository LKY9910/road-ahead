"""T-26 Seoul API recorder. Credential-bearing URLs only go to curl stdin."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import re
import subprocess
import time
from urllib.parse import quote, unquote

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / 'out'


def credential():
    value = os.getenv('ROAD_AHEAD_SEOUL_KEY')
    if value:
        return unquote(value.strip())
    for line in (ROOT/'api').read_text(encoding='utf-8-sig').splitlines():
        quoted = re.search(r"서울\s*api\s*=\s*['\"]([^'\"]+)['\"]", line, re.I)
        if quoted:
            return unquote(quoted[1].strip())
        match = re.search(r"([A-Za-z0-9%+/_=-]{15,})['\"]?\s*$", line)
        if match and re.search(r'서울\s*api', line[:match.start()], re.I):
            return unquote(match[1])
    raise ValueError('Seoul credential missing')


def scrub(text, key):
    for variant in {key, quote(key, safe=''), quote(quote(key, safe=''), safe='')}:
        text = text.replace(variant, '***')
    return text


def capture(label, start=1, end=1000, service='GetParkingInfo', invalid_key=False):
    if not re.fullmatch(r'[A-Za-z0-9_-]+', label) or not service.isalnum():
        raise ValueError('simple labels and service names required')
    path = OUT/(label+'.json')
    if path.exists():
        raise ValueError('capture already exists')
    key = credential()
    actual = 'T26_INVALID_KEY' if invalid_key else key
    endpoint = 'http://openapi.seoul.go.kr:8088'
    url = f'{endpoint}/{quote(actual,safe="")}/json/{service}/{start}/{end}/'
    record = {'requestedAtKst': datetime.now().astimezone().isoformat(),
              'url': f'{endpoint}/***/json/{service}/{start}/{end}/',
              'start': start, 'end': end, 'service': service}
    begin = time.monotonic()
    try:
        result = subprocess.run(['curl.exe','--config','-','--silent','--show-error','--include',
            '--connect-timeout','10','--max-time','40','--proto','=http,https'],
            input=f'url = "{url}"\n'.encode(),capture_output=True,timeout=45)
        if result.returncode:
            raise RuntimeError(scrub(result.stderr.decode(errors='replace'),key))
        headers, body = result.stdout.split(b'\r\n\r\n',1)
        while body.startswith(b'HTTP/'):
            headers,body=body.split(b'\r\n\r\n',1)
        record['httpStatus']=int(headers.splitlines()[0].split()[1])
        record['downloadBytes']=len(body)
        clean=scrub(body.decode('utf-8-sig',errors='replace'),key)
        try: record['response']=json.loads(clean)
        except ValueError: record['body']=clean
    except (OSError,ValueError,RuntimeError,subprocess.TimeoutExpired) as error:
        record['error']=scrub(str(error),key)
    record['durationMs']=round((time.monotonic()-begin)*1000,2)
    OUT.mkdir(parents=True,exist_ok=True)
    serialized=scrub(json.dumps(record,ensure_ascii=False,indent=2),key)+'\n'
    path.write_text(serialized,encoding='utf-8',newline='\n')
    print(f'{label}: HTTP={record.get("httpStatus")} bytes={record.get("downloadBytes")} ms={record["durationMs"]}',flush=True)
    return record


def self_test():
    key='synthetic/path+key='
    assert scrub(f'http://host/{quote(key,safe="")}/json/ {key}',key)=='http://host/***/json/ ***'
    print('self-test passed: path and raw key redaction; no network')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test',action='store_true')
    parser.add_argument('--label')
    parser.add_argument('--start',type=int,default=1)
    parser.add_argument('--end',type=int,default=1000)
    parser.add_argument('--service',default='GetParkingInfo')
    parser.add_argument('--invalid-key',action='store_true')
    args=parser.parse_args()
    if args.self_test:self_test()
    elif args.label:capture(args.label,args.start,args.end,args.service,args.invalid_key)
    else:parser.error('label required')
