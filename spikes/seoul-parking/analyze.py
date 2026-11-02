"""Offline analysis of the T-26 captures; never sends API requests."""
import argparse
from collections import Counter, defaultdict
from datetime import datetime, timezone, timedelta
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import statistics
import unicodedata
import xml.etree.ElementTree as ET
from probe import OUT, ROOT

KST = timezone(timedelta(hours=9))
CITY = (37.5663, 126.9779)


def numeric(value):
    try:
        result = float(value)
        return result if math.isfinite(result) else None
    except (TypeError, ValueError):
        return None


def age_seconds(row, requested):
    try:
        updated = datetime.strptime(row['NOW_PRK_VHCL_UPDT_TM'], '%Y-%m-%d %H:%M:%S').replace(tzinfo=KST)
        return (requested-updated).total_seconds()
    except (KeyError, TypeError, ValueError):
        return None


def available(row, requested):
    total = numeric(row.get('TPKCT'))
    occupied = numeric(row.get('NOW_PRK_VHCL_CNT'))
    age = age_seconds(row, requested)
    if (str(row.get('PRK_STTS_YN')) != '1' or age is None or not 0 <= age <= 600
            or total is None or occupied is None or not total.is_integer()
            or not occupied.is_integer() or not 0 <= occupied <= total or total <= 0):
        return None
    return int(total-occupied)


def normalized(name):
    value = re.sub(r'\s+', '', unicodedata.normalize('NFKC', name).casefold())
    value = re.sub(r'\((시|구)\)$', '', value)
    return re.sub(r'(공영주차장|주차장)$', '', value)


def distance(lat, lon):
    a, b = map(math.radians, CITY)
    c, d = map(math.radians, (lat, lon))
    h = math.sin((c-a)/2)**2+math.cos(a)*math.cos(c)*math.sin((d-b)/2)**2
    return 6371008.8*2*math.asin(min(1, math.sqrt(h)))


def distribution(values):
    ordered = sorted(values)
    if not ordered:return {'count': 0}
    def percentile(p):return round(ordered[math.ceil(p*(len(ordered)-1))], 3)
    return {'count':len(ordered),'min':round(ordered[0],3), 'p50':round(statistics.median(ordered),3),
            'p90':percentile(.9),'p95':percentile(.95),'max':round(ordered[-1],3)}


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def analyze(index_path):
    index = read_json(index_path)
    snapshots = []
    sizes, durations = [], []
    for item in index:
        rows = []
        for label in item['labels']:
            record = read_json(OUT/(label+'.json'))
            response = record['response']['GetParkingInfo']
            assert response['RESULT']['CODE'] == 'INFO-000'
            rows.extend(response['row'])
            sizes.append(record['downloadBytes'])
            durations.append(record['durationMs'])
        assert len(rows) == int(response['list_total_count'])
        snapshots.append((datetime.fromisoformat(item['requestedAtKst']), rows))
    first_time, first_rows = snapshots[0]
    last_time, last_rows = snapshots[-1]
    ids = [{r['PKLT_CD'] for r in rows} for _, rows in snapshots]
    values, stamps = defaultdict(set), defaultdict(set)
    ages, linked_ages, intervals = [], [], []
    observations = []
    conflicts = []
    for requested, rows in snapshots:
        groups = defaultdict(list)
        for row in rows:
            pk = row['PKLT_CD']
            groups[pk].append(row)
            value = numeric(row.get('NOW_PRK_VHCL_CNT'))
            if value is not None:values[pk].add(value)
            stamps[pk].add(row.get('NOW_PRK_VHCL_UPDT_TM'))
            age = age_seconds(row, requested)
            if age is not None:
                ages.append(age)
                if str(row['PRK_STTS_YN']) == '1':linked_ages.append(age)
        for pk, group in groups.items():
            if len({json.dumps(r,sort_keys=True) for r in group}) > 1:conflicts.append(pk)
        observations.append({'requestedAtKst':requested.isoformat(), 'rows':len(rows),'uniqueIds':len(groups),
            'duplicateRows':len(rows)-len(groups),'freshAvailableRows':sum(available(r,requested) is not None for r in rows),
            'within10MinutesRows':sum(age_seconds(r,requested) is not None and 0 <= age_seconds(r,requested) <= 600 for r in rows),
            'linkedRows':sum(str(r.get('PRK_STTS_YN')) == '1' for r in rows)})
    for (a,_),(b,_) in zip(snapshots,snapshots[1:]):intervals.append((b-a).total_seconds())
    timestamp_steps = []
    adjacent_changes = []
    for (_, left), (_, right) in zip(snapshots, snapshots[1:]):
        before = {r['PKLT_CD']:r for r in left}
        count = 0
        for row in right:
            old = before[row['PKLT_CD']]
            if numeric(old.get('NOW_PRK_VHCL_CNT')) != numeric(row.get('NOW_PRK_VHCL_CNT')):count += 1
            try:
                delta = (datetime.strptime(row['NOW_PRK_VHCL_UPDT_TM'],'%Y-%m-%d %H:%M:%S')
                    - datetime.strptime(old['NOW_PRK_VHCL_UPDT_TM'],'%Y-%m-%d %H:%M:%S')).total_seconds()
                if delta > 0:timestamp_steps.append(delta)
            except ValueError:pass
        adjacent_changes.append(count)
    fields = {}
    for field in first_rows[0]:
        column = [r.get(field) for r in first_rows]
        fields[field] = {'present':sum(field in r for r in first_rows),
            'nonempty':sum(v is not None and str(v).strip() != '' for v in column),
            'asteriskPlaceholder':sum(v == '*' for v in column),
            'finiteNumeric':sum(numeric(v) is not None for v in column)}
    codes = {}
    for code,name in [('PKLT_TYPE','PRK_TYPE_NM'),('OPER_SE','OPER_SE_NM'),('PRK_STTS_YN','PRK_STTS_NM'),
        ('PAY_YN','PAY_YN_NM'),('NGHT_PAY_YN','NGHT_PAY_YN_NM'),('SAT_CHGD_FREE_SE','SAT_CHGD_FREE_NM'),
        ('LHLDY_CHGD_FREE_SE','LHLDY_CHGD_FREE_SE_NAME'),('SHRN_PKLT_YN','SHRN_PKLT_YN')]:
        codes[code] = [{'value':a,'meaning':b,'rows':n} for (a,b),n in Counter((r[code],r[name]) for r in first_rows).items()]
    with (ROOT/'app/src/main/assets/parking/lots.tsv').open(encoding='utf-8') as f:
        lots = list(csv.DictReader(f,delimiter='\t'))
    exact, norm = defaultdict(list), defaultdict(list)
    for lot in lots:
        exact[lot['name']].append(lot)
        norm[normalized(lot['name'])].append(lot)
    matches, unmatched, ambiguous = [], [], []
    for row in last_rows:
        candidates = exact[row['PKLT_NM']] or norm[normalized(row['PKLT_NM'])]
        if len(candidates) == 1:
            lot = candidates[0]
            matches.append({'seoulId':row['PKLT_CD'],'name':row['PKLT_NM'],'address':row['ADDR'],
                'kotsaId':lot['#id'],'kotsaName':lot['name'],'lat':float(lot['lat']),'lon':float(lot['lon']),
                'cityHallDistanceM':round(distance(float(lot['lat']),float(lot['lon'])),2),
                'method':'exactName' if exact[row['PKLT_NM']] else 'normalizedNameCandidate',
                'total':row['TPKCT'],'occupied':row['NOW_PRK_VHCL_CNT'],
                'available':available(row,last_time),'updatedAt':row['NOW_PRK_VHCL_UPDT_TM'],
                'ageSeconds':age_seconds(row,last_time),'linked':row['PRK_STTS_YN'],
                'basicMinutes':row['BSC_PRK_HR'],'basicFeeWon':row['BSC_PRK_CRG'],
                'additionalMinutes':row['ADD_PRK_HR'],'additionalFeeWon':row['ADD_PRK_CRG']})
            if row['PKLT_CD']=='1512125':
                matches[-1]['locationCaution']='KOTSA longitude 126.9695164 conflicts with Seoul address in Heungin-dong; exclude from nearby recommendations pending coordinate verification'
        elif not candidates:unmatched.append({'id':row['PKLT_CD'],'name':row['PKLT_NM'],'address':row['ADDR']})
        else:ambiguous.append({'id':row['PKLT_CD'],'name':row['PKLT_NM'],'candidates':[c['#id'] for c in candidates]})
    nearby = sorted((m for m in matches if m['cityHallDistanceM'] <= 1000),key=lambda m:m['cityHallDistanceM'])
    changed = sorted(pk for pk, vs in values.items() if len(vs)>1)
    errors = {}
    for label in ['error-invalid-key','error-range','no-data']:
        path = OUT/(label+'.json')
        if path.exists():
            record = read_json(path)
            if 'response' in record:
                result = record['response']['RESULT']; fmt = 'JSON'
            else:
                root = ET.fromstring(record['body'])
                result = {e.tag:e.text for e in root}; fmt = 'XML'
            errors[label] = {'httpStatus':record['httpStatus'],'format':fmt,'result':result,'url':record['url']}
    summary = {'observedFrom':first_time.isoformat(),'observedTo':last_time.isoformat(),
        'durationSeconds':(last_time-first_time).total_seconds(),'snapshots':len(snapshots),'requestCount':len(sizes),
        'requestIntervalSeconds':distribution(intervals),'downloadBytes':distribution(sizes),'requestDurationMs':distribution(durations),
        'rows':len(first_rows),'uniqueIds':len(ids[0]),'idsStable':all(s==ids[0] for s in ids),'fields':fields,'codes':codes,
        'conflictingDuplicateIds':sorted(set(conflicts)),'observations':observations,
        'changedCount':len(changed),'changedPercent':100*len(changed)/len(ids[0]),'changedIds':changed,
        'timestampsChangedCount':sum(len(v)>1 for v in stamps.values()),'ageSeconds':distribution(ages),
        'positiveTimestampStepSeconds':distribution(timestamp_steps),'adjacentChangedCounts':adjacent_changes,
        'linkedAgeSeconds':distribution(linked_ages),'parseableTimestampSamples':len(ages),
        'within10MinutesSamples':sum(0<=a<=600 for a in ages),'futureTimestampSamples':sum(a<0 for a in ages),
        'allRowSamples':sum(len(rows) for _,rows in snapshots),
        'kotsaRows':len(lots),'kotsaSha256':hashlib.sha256((ROOT/'app/src/main/assets/parking/lots.tsv').read_bytes()).hexdigest(),
        'exactNameMatches':sum(m['method']=='exactName' for m in matches),
        'uniqueNormalizedCandidates':sum(m['method']=='normalizedNameCandidate' for m in matches),
        'matchedCandidates':matches,'ambiguousCandidates':ambiguous,'unmatched':unmatched,
        'nearCityHallNameCandidates':nearby,'coordinateMatchAvailable':False,'errors':errors,
        'dongkukSeries':[{'requestedAtKst':t.isoformat(),**{k:r[k] for k in ['PKLT_CD','TPKCT','NOW_PRK_VHCL_CNT','NOW_PRK_VHCL_UPDT_TM','PRK_STTS_YN']}}
            for t,rows in snapshots for r in rows if r['PKLT_CD']=='1425816']}
    return summary


def self_test():
    t=datetime(2026,10,29,18,0,tzinfo=KST)
    row={'TPKCT':13.0,'NOW_PRK_VHCL_CNT':3.0,'NOW_PRK_VHCL_UPDT_TM':'2026-10-29 17:55:00','PRK_STTS_YN':'1'}
    assert available(row,t)==10
    for change in [{'PRK_STTS_YN':'0'},{'NOW_PRK_VHCL_CNT':14},{'NOW_PRK_VHCL_CNT':-1},
        {'NOW_PRK_VHCL_CNT':3.5},{'TPKCT':0},{'NOW_PRK_VHCL_UPDT_TM':'2026-10-29 17:49:59'},
        {'NOW_PRK_VHCL_UPDT_TM':'2026-10-29 18:00:01'},{'NOW_PRK_VHCL_UPDT_TM':''}]:
        assert available(row|change,t) is None,change
    assert normalized('동국제강 공영주차장(시)') == normalized('동국제강')
    assert normalized('청계천(북1) 주차장') != normalized('청계천(남) 주차장')
    assert distance(*CITY)==0 and 310 < distance(37.5672159,126.981303) < 320
    print('analysis self-test passed; no network')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--index',type=Path,default=OUT/'observation-index.json')
    parser.add_argument('--output',type=Path,default=OUT/'summary.json')
    parser.add_argument('--self-test',action='store_true')
    args=parser.parse_args()
    if args.self_test:self_test()
    else:
        summary=analyze(args.index)
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
        print(json.dumps({k:summary[k] for k in ['snapshots','durationSeconds','rows','changedCount','exactNameMatches','uniqueNormalizedCandidates']},ensure_ascii=False))
