"""Recompute T-14 measurements from redacted records; no network or credentials."""
from collections import Counter, defaultdict
from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime
import csv
import io
import json
from pathlib import Path
import statistics
import sys
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "docs/spikes/road-ahead"
KST = timezone(timedelta(hours=9))


def payload(record):
    try:
        return json.loads(record.get("body", ""))
    except (ValueError, TypeError):
        return {}


def items(record):
    body = payload(record).get("body", {})
    value = body.get("items", []) if isinstance(body, dict) else []
    return value.get("item", []) if isinstance(value, dict) else value


def per_second(before, after, field):
    state_field = field.replace("RmndCs", "SttsNm")
    if before.get(state_field) != after.get(state_field):
        return None
    try:
        seconds = (datetime.strptime(after["totDt"], "%Y%m%d%H%M%S") - datetime.strptime(before["totDt"], "%Y%m%d%H%M%S")).total_seconds()
        decrease = int(before[field]) - int(after[field])
        return decrease / seconds if 0 < seconds <= 4 and decrease > 0 else None
    except (ValueError, KeyError, TypeError):
        return None


def self_test():
    before = {"totDt": "20261028162459", "ntStsgRmndCs": "3000", "ntStsgSttsNm": "red"}
    after = {"totDt": "20261028162501", "ntStsgRmndCs": "1000", "ntStsgSttsNm": "red"}
    assert per_second(before, after, "ntStsgRmndCs") == 1000
    assert per_second(before, {**after, "ntStsgSttsNm": "green"}, "ntStsgRmndCs") is None
    assert per_second(before, {**after, "ntStsgRmndCs": ""}, "ntStsgRmndCs") is None
    assert per_second(before, before, "ntStsgRmndCs") is None
    print("self-test: 4 countdown interval/phase/empty/frozen assertions passed")


def analyze():
    records = [(p.name, json.loads(p.read_text(encoding="utf-8"))) for p in sorted((EVIDENCE / "samples").glob("*.json"))]
    endpoints = defaultdict(list)
    for name, record in records:
        endpoints[urlsplit(record["url"]).path].append(record)
    summary = {"endpoints": {}, "signal": {}, "weather": []}
    for endpoint, group in endpoints.items():
        good = [r for r in group if r.get("httpStatus") == 200 and (r.get("files") or payload(r).get("header", {}).get("resultCode") in (0, "0", "K0"))]
        summary["endpoints"][endpoint] = {"requests": len(group), "httpStatuses": dict(Counter(str(r.get("httpStatus", "transportError")) for r in group)),
            "applicationSuccesses": len(good), "successMedianMs": round(statistics.median(r["durationMs"] for r in good), 2) if good else None,
            "successMaxMs": max((r["durationMs"] for r in good), default=None)}
    signal_rows = defaultdict(list)
    for name, record in records:
        if "signal-series-" in name or "signal-cycle-" in name:
            for row in items(record):
                signal_rows[row["crsrdId"]].append((name, record, row))
    map_rows = {x["crsrdId"]: x for name, record in records if "signal-map-ulsan" in name for x in items(record)}
    for ident, group in signal_rows.items():
        result = {"map": map_rows.get(ident), "observations": len(group), "countdownScale": {}, "transitions": {}, "httpDateMinusProducerSeconds": []}
        for field in ("ntStsgRmndCs", "etStsgRmndCs", "ntLtsgRmndCs"):
            rates = [rate for before, after in zip(group, group[1:]) if (rate := per_second(before[2], after[2], field)) is not None]
            result["countdownScale"][field] = {"validDecreases": len(rates), "medianUnitsPerSecond": statistics.median(rates) if rates else None,
                "range": [min(rates), max(rates)] if rates else None}
        for field in ("ntStsgSttsNm", "etStsgSttsNm"):
            changes = []
            previous = None
            for name, record, row in group:
                if "signal-cycle-" not in name:
                    continue
                if row.get(field) != previous:
                    changes.append({"producerTime": row["totDt"], "state": row.get(field), "remainingRaw": row.get(field.replace("SttsNm", "RmndCs")), "sample": name})
                    previous = row.get(field)
            result["transitions"][field] = changes
        for name, record, row in group:
            date = next((v for k, v in record.get("headers", {}).items() if k.lower() == "date"), None)
            if date:
                producer = datetime.strptime(row["totDt"], "%Y%m%d%H%M%S").replace(tzinfo=KST)
                result["httpDateMinusProducerSeconds"].append((parsedate_to_datetime(date) - producer).total_seconds())
        ages = result.pop("httpDateMinusProducerSeconds")
        result["httpDateMinusProducerRangeSeconds"] = [min(ages), max(ages)] if ages else None
        summary["signal"][ident] = result
    for name, record in records:
        for file in record.get("files", []):
            rows = list(csv.DictReader(io.StringIO(file["body"])))
            summary["weather"].append({"sample": name, "file": file["name"], "rows": len(rows),
                "levels": dict(Counter(row.get("FOG", row.get("B_ICE")) for row in rows)),
                "warningExamples": [r for r in rows if r.get("FOG", r.get("B_ICE")) in ("1", "2", "3")][:4]})
    disaster = [(name, r) for name, r in records if "disaster-series-" in name]
    if disaster:
        summary["disasterSeries"] = {"samples": len(disaster), "spanSeconds": round((datetime.fromisoformat(disaster[-1][1]["requestedAtKst"]) - datetime.fromisoformat(disaster[0][1]["requestedAtKst"])).total_seconds(), 2),
            "distinctBodies": len({r.get("body") for name, r in disaster})}
    for name, record in records:
        if "its-event-seoul-authorized" in name:
            summary["eventSnapshot"] = {"sample": name, "count": len(items(record)), "types": dict(Counter(r.get("eventType") for r in items(record)))}
    path = EVIDENCE / "api-summary.json"
    path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)}; {len(records)} requests, no network calls")


if __name__ == "__main__":
    self_test() if "--self-test" in sys.argv else analyze()
