"""T-21 visibility measurements; requires the spike installed and overlay permission allowed."""
import json
import pathlib
import re
import subprocess
import threading
import time
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/spikes/overlay"
PACKAGE = "com.roadahead.spike.overlay"
STOP = threading.Event()


def adb(*args):
    return subprocess.check_output(["adb", "-s", "emulator-5556", *args])


def http(path, method="GET"):
    with urllib.request.urlopen(urllib.request.Request("http://127.0.0.1:8088" + path, method=method), timeout=5) as response:
        return json.load(response)


def pulse():
    while not STOP.is_set():
        try:
            http("/browser/session?client=t21-overlay", "POST")
        except Exception as error:
            print("Console heartbeat:", type(error).__name__, flush=True)
        STOP.wait(0.8)


def state(gear, speed):
    http("/vehicle/controls?speed=0&accelerator=0&brake=0", "PUT")
    http("/vehicle/gear?gear=" + gear, "PUT")
    http(f"/vehicle/controls?speed={speed}&accelerator=0&brake=0", "PUT")
    time.sleep(3)
    actual = http("/vehicle/state")
    assert actual["gear"]["actual"]["value"] == gear
    assert abs(actual["controls"]["speed_kph"] - speed) < 0.1
    assert actual["controls"]["emulator_active"]


def activity(name, op):
    adb("shell", "am", "start", "--user", "10", "-f", "0x14000000", "-n", PACKAGE + "/." + name, "--es", "op", op)
    time.sleep(1)


def navi():
    adb("shell", "am", "start", "--user", "10", "-n", "ai.umos.maps.android.navigation.app/.MainActivity")
    time.sleep(2)


def record(name):
    windows = adb("shell", "dumpsys", "window", "windows").decode("utf-8")
    match = re.search(r"(?ms)^  Window #\d+ Window\{[^\n]* T21Overlay\}:.*?(?=^  Window #|\Z)", windows)
    assert match and "isVisible=true" in match[0] and "shown=true" in match[0], name
    image = adb("exec-out", "screencap", "-p")
    assert image[:8] == bytes.fromhex("89504e470d0a1a0a")
    assert int.from_bytes(image[16:20], "big") == 2560
    assert int.from_bytes(image[20:24], "big") == 1440
    (OUT / (name + ".png")).write_bytes(image)
    console = http("/vehicle/state")
    activities = adb("shell", "dumpsys", "activity", "activities").decode("utf-8")
    evidence = {
        "captured_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
        "gear": console["gear"]["actual"]["value"],
        "console_speed_kph": console["controls"]["speed_kph"],
        "emulator_active": console["controls"]["emulator_active"],
        "overlay_window": match[0],
        "focus": "\n".join(line for line in windows.splitlines() if "mCurrentFocus=" in line or "mFocusedApp=" in line),
        "activities": "\n".join(line for line in activities.splitlines() if PACKAGE in line or "topResumedActivity=" in line),
        "services": adb("shell", "dumpsys", "activity", "services", PACKAGE).decode("utf-8"),
        "car_ux": adb("shell", "dumpsys", "car_service", "--services", "CarDrivingStateService", "CarUxRestrictionsManagerService").decode("utf-8").split("Restriction mode:")[0],
    }
    (OUT / (name + ".json")).write_text(json.dumps(evidence, ensure_ascii=False, indent=2), encoding="utf-8")
    print(name, evidence["gear"], evidence["console_speed_kph"], "PNG/window assertions passed", flush=True)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    heartbeat = threading.Thread(target=pulse, daemon=True)
    heartbeat.start()
    try:
        time.sleep(2)
        state("Park", 0)
        activity("MainActivity", "show")
        navi()
        record("p0-no-do-no-service")
        state("Drive", 30)
        record("d30-no-do-no-service")
        activity("MainActivity", "show")
        record("d30-non-do-activity")
        activity("DoActivity", "show")
        record("d30-do-activity")
        navi()
        record("d30-do-no-service")
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        time.sleep(2)
        record("d30-home-no-service")
        activity("DoActivity", "finish")
        activity("MainActivity", "finish")
        navi()
        time.sleep(15)
        activities = adb("shell", "dumpsys", "activity", "activities").decode("utf-8")
        assert "mActivityComponent=" + PACKAGE not in activities, "Activities were not finished"
        record("d30-finished-no-service")
        activity("DoActivity", "service")
        activity("DoActivity", "finish")
        navi()
        activities = adb("shell", "dumpsys", "activity", "activities").decode("utf-8")
        assert "mActivityComponent=" + PACKAGE not in activities, "FGS activity was not finished"
        record("d30-foreground-service")
    finally:
        try:
            state("Park", 0)
            adb("shell", "am", "force-stop", "--user", "10", PACKAGE)
            restored = http("/vehicle/state")
            assert restored["gear"]["actual"]["value"] == "Park"
            assert restored["controls"]["speed_kph"] == 0
            (OUT / "restored.json").write_text(json.dumps({"gear": "Park", "speed_kph": 0}, indent=2), encoding="utf-8")
            print("Restored Park/0; spike force-stopped", flush=True)
        finally:
            STOP.set()
            heartbeat.join(timeout=6)
            http("/browser/session?client=t21-overlay&closing=1", "POST")
