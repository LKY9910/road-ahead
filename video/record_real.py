"""Record one replay at a time; obtain emulator permission before using this tool."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
from urllib.parse import quote, quote_plus, unquote
import xml.etree.ElementTree as ET
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / 'video/out/raw'


def adb(*args):
    return subprocess.check_output(['adb', '-s', 'emulator-5556', *args])


def load(scene):
    return json.loads((RAW / f'real-{scene}.json').read_text(encoding='utf-8'))


def save(scene, value):
    (RAW / f'real-{scene}.json').write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')


def speech_lines(log):
    result = []
    for line in log.splitlines():
        if 'RoadAhead' in line and 'speak=' in line:
            stamp = re.search(r'\d+\.\d+', line.split('speak=', 1)[0])
            assert stamp, 'Missing monotonic timestamp'
            result.append({'deviceMonotonic': float(stamp[0]), 'text': line.split('speak=', 1)[1]})
    return result


def safety():
    log = adb('logcat', '-d', '-v', 'threadtime').decode('utf-8', errors='replace')
    config = next((ROOT / 'app/build/generated/source/buildConfig/debug').rglob('BuildConfig.java')).read_text()
    keys = [json.loads(value) for value in re.findall(r'public static final String \w*KEY\w* = ("(?:[^"\\]|\\.)*")', config)]
    variants = {value for key in keys if key for value in (key, quote(key, safe=''), quote_plus(key), unquote(key)) if value}
    pids = set(json.loads((RAW / 'real-pids.json').read_text()))
    own = [line for line in log.splitlines() if len(line.split()) > 5 and line.split()[2].isdigit() and int(line.split()[2]) in pids]
    result = {'secretOccurrences': sum(log.count(value) for value in variants),
              'ttsOnStart': sum('TTS onStart' in line for line in own),
              'fatal': sum('FATAL EXCEPTION' in line for line in own), 'processes': len(pids)}
    assert result['secretOccurrences'] == result['ttsOnStart'] == result['fatal'] == 0, result
    (RAW.parent / 'real-safety.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prefs', 'record', 'launch', 'collect', 'pulse', 'audit', 'self-test'])
    parser.add_argument('scene', nargs='?', choices=['a', 'c', 'e', 'live'])
    parser.add_argument('--seconds', type=int, default=40)
    args = parser.parse_args()
    if args.action == 'self-test':
        assert speech_lines('[ 1234.567] 42 42 I RoadAhead: speak=앞 신호 빨간불이에요.') == [
            {'deviceMonotonic': 1234.567, 'text': '앞 신호 빨간불이에요.'}]
        assert speech_lines('I Other: speak=ignored') == []
        try:
            speech_lines('I RoadAhead: speak=missing timestamp')
        except AssertionError:
            pass
        else:
            raise AssertionError('Unstamped speech accepted')
        print('record self-test ok')
        return
    RAW.mkdir(parents=True, exist_ok=True)
    if args.action == 'pulse':
        (RAW / 'console.stop').unlink(missing_ok=True)
        while not (RAW / 'console.stop').exists():
            with urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:8088/browser/session?client=t47-real', method='POST'), timeout=5):
                pass
            time.sleep(0.8)
        return
    if args.action == 'audit':
        print(safety())
        return
    assert args.scene, 'A scene is required'
    scene = args.scene
    assert args.action == 'prefs' or scene in ['a', 'c', 'e']
    if args.action == 'prefs':
        root = ET.Element('map')
        ET.SubElement(root, 'string', name='mode').text = 'LIVE' if scene == 'live' else 'DEMO_' + scene.upper()
        ET.SubElement(root, 'string', name='convention').text = 'APPROACH'
        ET.SubElement(root, 'boolean', name='disclaimerSeen', value='true')
        ET.SubElement(root, 'boolean', name='travelMode', value='true' if scene == 'e' else 'false')
        if scene == 'e':
            companions = ET.SubElement(root, 'set', name='companions')
            for choice in ['CHILD', 'PET']:
                ET.SubElement(companions, 'string').text = choice
        (RAW / 'settings.xml').write_bytes(ET.tostring(root, encoding='utf-8', xml_declaration=True))
        print('prepared settings for', scene)
    elif args.action == 'record':
        assert 1 <= args.seconds <= 180
        device_file = f'/sdcard/naviseo-real-{scene}-{time.time_ns()}.mp4'
        info = {'scene': scene, 'deviceFile': device_file, 'recordHostMonotonic': time.monotonic(),
                'recordEpoch': time.time(), 'recordSeconds': args.seconds, 'recordComplete': False}
        save(scene, info)
        subprocess.run(['adb', '-s', 'emulator-5556', 'shell', 'screenrecord', '--size', '1920x1080',
                        '--bit-rate', '12000000', '--time-limit', str(args.seconds), device_file], check=True)
        info = load(scene)
        info['recordComplete'] = True
        save(scene, info)
        print('recorded', scene)
    elif args.action == 'launch':
        info = load(scene)
        info['launchHostMonotonic'] = time.monotonic()
        adb('shell', 'am', 'start', '-W', '--user', '10', '-n', 'com.roadahead/.MainActivity', '--ez', 'logOnlySpeech', 'true')
        info['pid'] = int(adb('shell', 'pidof', 'com.roadahead').strip())
        save(scene, info)
        pid_file = RAW / 'real-pids.json'
        pids = json.loads(pid_file.read_text()) if pid_file.exists() else []
        pid_file.write_text(json.dumps(sorted(set(pids + [info['pid']]))))
        print('launched', scene, 'with logOnlySpeech=true')
    else:
        info = load(scene)
        assert info['recordComplete'], 'Wait for screenrecord to finish before collecting'
        target = RAW / f'real-{scene}.mp4'
        adb('pull', info['deviceFile'], str(target))
        checks = safety()
        log = adb('logcat', '-d', '-v', 'monotonic', '--pid=' + str(info['pid']), '-s', 'RoadAhead:I').decode('utf-8')
        info['firstLogDeviceMonotonic'] = float(re.search(r'\d+\.\d+', log)[0])
        info['speeches'] = speech_lines(log)
        assert info['speeches'], 'No original speech was captured'
        info['sourceBytes'] = target.stat().st_size
        info['sourceSha256'] = hashlib.sha256(target.read_bytes()).hexdigest()
        info['safety'] = checks
        save(scene, info)
        print(json.dumps({'scene': scene, 'speeches': info['speeches'], 'safety': checks}, ensure_ascii=False))


if __name__ == '__main__':
    main()
