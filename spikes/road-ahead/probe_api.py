"""T-14 disposable API recorder. Run from the repository root; never pass keys as args."""
import argparse
from datetime import datetime
import html
import io
import json
import os
from pathlib import Path
import re
import subprocess
import time
import zipfile
from urllib.error import URLError
from urllib.parse import quote, unquote, urlencode, urlsplit
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[2]
SAMPLES = ROOT / "docs/spikes/road-ahead/samples"
STATE = ROOT / "spikes/road-ahead/.gradle/api-counts.json"
LABELS = {"signal": "신호", "disaster": "재난", "weather": "기상"}
HOSTS = {"signal": {"apis.data.go.kr"}, "weather": {"apihub.kma.go.kr"},
         "disaster": {"openapi.its.go.kr"}}


def credentials():
    keys = {}
    source = ROOT / "api"
    for line in source.read_text(encoding="utf-8-sig").splitlines() if source.exists() else []:
        match = re.search(r"([A-Za-z0-9%+/=_-]{20,})['\"]?\s*$", line)
        if match:
            for service, label in LABELS.items():
                if label in line[:match.start()]:
                    keys[service] = unquote(match[1])
    for service in LABELS:
        value = os.getenv(f"ROAD_AHEAD_{service.upper()}_KEY")
        if value:
            keys[service] = unquote(value)
    return keys


def scrub(value, secrets):
    for key in secrets:
        for variant in {key, quote(key, safe=""), html.escape(key), quote(quote(key, safe=""), safe="")}:
            value = value.replace(variant, "***")
    value = re.sub(r"(?i)((?:serviceKey|apiKey|authKey)=)[^&\s<>\"']+", r"\1***", value)
    value = re.sub(r'(?i)("(?:serviceKey|apiKey|authKey)"\s*:\s*")[^"]*', r'\1***', value)
    return re.sub(r"(?i)(<(?:serviceKey|apiKey|authKey)>)[^<]*", r"\1***", value)


def capture(service, endpoint, params, label, key_name="serviceKey"):
    if urlsplit(endpoint).scheme != "https" or urlsplit(endpoint).hostname not in HOSTS[service]:
        raise ValueError("endpoint must be an approved HTTPS service host")
    if urlsplit(endpoint).query or not re.fullmatch(r"[a-zA-Z0-9_-]+", label):
        raise ValueError("use params for query values and a simple sample label")
    keys = credentials()
    if service not in keys:
        raise ValueError("credential missing; confirm service with coordinator")
    now = datetime.now(ZoneInfo("Asia/Seoul"))
    day = now.date().isoformat()
    counts = json.loads(STATE.read_text()) if STATE.exists() else {}
    bucket = counts.setdefault(day, {})
    if bucket.get(service, 0) >= 480:
        raise ValueError("local daily 480-call safety cap reached")
    bucket[service] = bucket.get(service, 0) + 1
    STATE.parent.mkdir(parents=True, exist_ok=True)
    STATE.write_text(json.dumps(counts, indent=2), encoding="utf-8")
    url = endpoint + "?" + urlencode({**params, key_name: keys[service]})
    record = {"requestedAtKst": now.isoformat(), "url": scrub(url, keys.values())}
    start = time.perf_counter()
    try:
        # Use Windows certificate validation; credentials go through stdin, never argv/files.
        result = subprocess.run(["curl.exe", "--config", "-", "--silent", "--show-error", "--include", "--connect-timeout", "10", "--max-time", "25", "--proto", "=https"],
                                input=f'url = "{url}"\n'.encode(), capture_output=True, timeout=30)
        if result.returncode:
            record["transportError"] = scrub(result.stderr.decode(errors="replace"), keys.values())
        else:
            header, body = result.stdout.split(b"\r\n\r\n", 1)
            while body.startswith(b"HTTP/"):
                header, body = body.split(b"\r\n\r\n", 1)
            record["httpStatus"] = int(header.splitlines()[0].split()[1])
            record["headers"] = {k: v.strip() for line in header.decode(errors="replace").splitlines()[1:] if ":" in line for k, v in [line.split(":", 1)] if k.lower() in {"date", "content-type", "age", "cache-control"}}
            record["downloadBytes"] = len(body)
            if body.startswith(b"PK\x03\x04"):
                with zipfile.ZipFile(io.BytesIO(body)) as archive:
                    if len(archive.infolist()) > 100 or sum(f.file_size for f in archive.infolist()) > 64 * 1024 * 1024:
                        raise ValueError("unexpectedly large archive")
                    record["files"] = [{"name": scrub(f.filename, keys.values()),
                                        "body": scrub(archive.read(f).decode("utf-8-sig", errors="replace"), keys.values())}
                                       for f in archive.infolist() if not f.is_dir()]
            else:
                record["body"] = scrub(body.decode("utf-8", errors="replace"), keys.values())
    except (URLError, TimeoutError, OSError, ValueError, subprocess.TimeoutExpired, zipfile.BadZipFile) as error:
        record["transportError"] = scrub(f"{type(error).__name__}: {error}", keys.values())
    record["durationMs"] = round((time.perf_counter() - start) * 1000, 2)
    SAMPLES.mkdir(parents=True, exist_ok=True)
    path = SAMPLES / f"{now.strftime('%Y%m%d-%H%M%S-%f')}-{label}.json"
    path.write_text(scrub(json.dumps(record, ensure_ascii=False, indent=2), keys.values()) + "\n", encoding="utf-8")
    print(f"{label}: status={record.get('httpStatus', 'transport-error')} ms={record['durationMs']} call={bucket[service]} saved={path.name}")
    return record


def self_test():
    key = "synthetic/secret+value="
    test = f'url?serviceKey={quote(key, safe="")}&x=1 apiKey=unknown <authKey>unknown</authKey> {{"serviceKey":"unknown"}} {key}'
    clean = scrub(test, [key])
    assert key not in clean and quote(key, safe="") not in clean
    assert 'serviceKey=***&x=1' in clean and 'apiKey=***' in clean
    assert '<authKey>***</authKey>' in clean and '"serviceKey":"***"' in clean
    assert unquote(quote(key, safe="")) == key
    print("self-test: 5 redaction/encoding assertions passed; no network calls")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--service", choices=LABELS)
    parser.add_argument("--endpoint")
    parser.add_argument("--label")
    parser.add_argument("--key-name", choices=("serviceKey", "apiKey", "authKey"), default="serviceKey")
    parser.add_argument("--param", action="append", default=[])
    args = parser.parse_args()
    if args.self_test:
        self_test()
    elif args.service and args.endpoint and args.label:
        capture(args.service, args.endpoint, dict(p.split("=", 1) for p in args.param), args.label, args.key_name)
    else:
        parser.error("--service, --endpoint and --label are required")
