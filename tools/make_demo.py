"""Build offline demo scenes from the scrubbed T-14 samples."""

import argparse
import csv
import io
import json
import math
from pathlib import Path
from urllib.parse import quote, unquote

ROOT = Path(__file__).resolve().parents[1]
SAMPLES = ROOT / "docs/spikes/road-ahead/samples"
OUTPUT = ROOT / "app/src/main/assets/demo"
SIGNAL_LABEL = "시연: 기록한 울산 신호 재생 · 방향 가정: 진입 방향"
FOG_LABEL = "시연: 10월 28일 기록한 중부내륙선 안개 자료 재생"
EVENT_LABEL = "시연: 10월 28일 기록한 수도권 공사 자료 재생"
ICE_LABEL = "합성 시나리오: 살얼음 자료는 11월 15일부터 제공돼 예시로 만들었어요"


def sample(pattern):
    return json.loads(next(SAMPLES.glob(pattern)).read_text(encoding="utf-8"))


def compact(value):
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def page(pattern, intersection):
    root = json.loads(sample(pattern)["body"])
    root["body"]["items"]["item"] = [
        row for row in root["body"]["items"]["item"] if str(row["crsrdId"]) == intersection
    ]
    root["body"]["totalCount"] = len(root["body"]["items"]["item"])
    assert root["body"]["totalCount"] == 1
    return compact(root)


def event(t, kind, **fields):
    return dict(t=t, kind=kind, **fields)


def signal_scene(intersection, route, count):
    mapping = page("*signal-map-ulsan*.json", intersection)
    point = json.loads(mapping)["body"]["items"]["item"][0]
    lat, lon = float(point["mapCtptIntLat"]), float(point["mapCtptIntLot"])
    bearing = 180 if route else 270
    rows = [event(0, "label", text=SIGNAL_LABEL), event(0, "nav", mode="ROUTE" if route else "FREE"),
            event(0, "map", body=mapping), event(0, "speed", mps=10.0)]
    # The coordinator moved the start to 380 m so the first advice fits within 10 s.
    for t in range(0, (count - 1) * 2000 + 1, 200):
        distance = max(30.0, 380.0 - t / 100)
        dlat = distance / 111_195.0 if route else 0
        dlon = 0 if route else distance / (111_195.0 * math.cos(math.radians(lat)))
        rows.append(event(t, "fix", lat=lat + dlat, lon=lon + dlon, bearing=bearing))
        if route:
            rows.append(event(t, "turn", movement="LEFT", distanceM=distance))
        if t == 35_000:
            rows.append(event(t, "speed", mps=0.0))
    for i in range(count):
        rows.append(event(i * 2000, "signals", body=page(f"*signal-cycle-{15 + i:03}.json", intersection)))
    return sorted(rows, key=lambda row: row["t"])


def csv_text(header, row):
    out = io.StringIO(newline="")
    writer = csv.writer(out, lineterminator="\n")
    writer.writerow(header)
    writer.writerow(row)
    return out.getvalue()


def hazard_scene():
    files = sample("*weather-045-explicit.json")["files"]
    fog_csv = next(f["body"] for f in files if "_LINK_FOG_" in f["name"])
    reader = csv.reader(io.StringIO(fog_csv))
    header = next(reader)
    row = next(row for row in reader if row[header.index("LINK_ID")] == "3580008302")
    assert row[header.index("FOG")] == "2"
    start = (float(row[header.index("F_LAT")]), float(row[header.index("F_LON")]))
    end = (float(row[header.index("T_LAT")]), float(row[header.index("T_LON")]))
    dy = (end[0] - start[0]) * 111_195.0
    dx = (end[1] - start[1]) * 111_195.0 * math.cos(math.radians(start[0]))
    length = math.hypot(dx, dy)
    bearing = math.degrees(math.atan2(dx, dy)) % 360
    rows = [event(0, "label", text=FOG_LABEL), event(0, "nav", mode="FREE"),
            event(0, "speed", mps=100.0), event(0, "weather", hazard="FOG", csv=csv_text(header, row))]
    for offset in (0, 36_000):
        if offset:
            rows.extend([event(offset, "label", text=ICE_LABEL),
                         event(offset, "events", body=compact({"header": {"resultCode": "0"}, "body": {"items": []}})),
                         event(offset, "weather", hazard="ICE", csv=csv_text(header + ["B_ICE"], row + ["2"]))])
        for t in range(0, 10_001, 200):
            ratio = (3500 - t / 10) / length
            rows.append(event(offset + t, "fix", lat=start[0] - (end[0] - start[0]) * ratio,
                              lon=start[1] - (end[1] - start[1]) * ratio, bearing=bearing))
    its = json.loads(sample("*its-event-seoul-authorized.json")["body"])
    construction = next(x for x in its["body"]["items"] if x["linkId"] == "2240032401" and x["eventType"] == "공사")
    its["body"]["items"] = [construction]
    rows.extend([event(18_000, "label", text=EVENT_LABEL),
                 event(18_000, "fix", lat=float(construction["coordY"]) - 2500 / 111_195.0,
                       lon=float(construction["coordX"]), bearing=0.0),
                 event(18_000, "weather", hazard="FOG", csv=",".join(header) + "\n"),
                 event(18_000, "events", body=compact(its)), event(52_000, "speed", mps=0.0)])
    return sorted(rows, key=lambda row: row["t"])


def generate():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, rows in zip("abc", (signal_scene("100", True, 40), signal_scene("1", False, 23), hazard_scene())):
        (OUTPUT / f"scene-{name}.jsonl").write_text("".join(compact(row) + "\n" for row in rows), encoding="utf-8")
    self_test()


def self_test():
    secrets = []
    key_file = ROOT / "api"
    if key_file.exists():
        for line in key_file.read_text(encoding="utf-8-sig").splitlines():
            if "=" in line:
                value = line.split("=", 1)[1].strip().strip("\"'")
                if value:
                    secrets.extend((value, unquote(value), quote(unquote(value), safe="")))
    for scene in "abc":
        path = OUTPUT / f"scene-{scene}.jsonl"
        text = path.read_text(encoding="utf-8")
        assert not any(token.lower() in text.lower() for token in ("serviceKey", "apiKey", "authKey")), "credential field found"
        assert not any(value in text for value in secrets), "credential value found"
        events = [json.loads(line) for line in text.splitlines()]
        assert events and events[0]["t"] == 0
        times = [event["t"] for event in events]
        assert all(isinstance(t, int) and t >= 0 for t in times)
        assert times == sorted(times) and times[-1] <= 90_000


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        print("self-test passed")
    else:
        generate()
        print("demo scenes generated")


if __name__ == "__main__":
    main()
