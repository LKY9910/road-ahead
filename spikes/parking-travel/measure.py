"""Explicit T-23 probe batches; reruns never overwrite captured evidence."""
import argparse
import math
from probe_api import capture, OUT, read_record


def probe(service, op, params, label):
    path = OUT / (label + '.json')
    return read_record(label) if path.exists() else capture(service, op, params, label)


def parking():
    common = {'pageNo': 1, 'numOfRows': 10000, 'format': 2}
    for region, sido, sigungu in [('seoul-jung', '11', '11140'), ('seoul-gangnam', '11', '11680'), ('ulsan-nam', '31', '31140')]:
        for operation in ['PrkSttusInfo', 'PrkOprInfo', 'PrkRealtimeInfo']:
            probe('parking', operation + '_v1', {**common, 'sidoCd': sido, 'sigunguCd': sigungu}, region + '-' + operation)
    probe('parking', 'PrkSttusInfo_v1', {**common, 'numOfRows': 100000}, 'parking-facility-100000')
    for label, params in [('id-filter', {'prk_center_id': '00000-19199-00007-00-1'}),
                          ('invalid-id', {'prk_center_id': 'NOT_A_PARKING_ID'}),
                          ('invalid-region', {'sidoCd': '99', 'sigunguCd': '99999'})]:
        probe('parking', 'PrkRealtimeInfo_v1', {**common, 'numOfRows': 10, **params}, 'parking-realtime-' + label)
    probe('parking', 'PrkSttusInfo', {**common, 'numOfRows': 1}, 'parking-old-operation-error')


def tour():
    common = {'MobileOS': 'ETC', 'MobileApp': 'RoadAheadProbe', '_type': 'json', 'pageNo': 1, 'numOfRows': 1000}
    for region, x, y, area in [('ulsan', '129.297', '35.548', '7'), ('seoul', '126.978', '37.567', '1')]:
        for content in [12, 14, 28, 15]:
            probe('tour', 'locationBasedList2', {**common, 'mapX': x, 'mapY': y, 'radius': 5000 if content == 15 else 3000, 'arrange': 'E', 'contentTypeId': content}, f'tour-{region}-location-{content}')
        probe('tour', 'searchFestival2', {**common, 'areaCode': area, 'eventStartDate': '20260101', 'arrange': 'A'}, f'tour-{region}-festivals-year')
        probe('tour', 'searchFestival2', {**common, 'areaCode': area, 'eventStartDate': '20261029', 'eventEndDate': '20261029'}, f'tour-{region}-festivals-day')


def national():
    first = read_record('parking-facility-100000')
    pages = math.ceil(int(first['response']['totalCount']) / 100000)
    for page in range(2, pages + 1):
        probe('parking', 'PrkSttusInfo_v1', {'pageNo': page, 'numOfRows': 100000, 'format': 2},
              f'parking-national-page-{page:02}')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('batch', choices=['parking', 'tour', 'national'])
    a = p.parse_args()
    {'parking': parking, 'tour': tour, 'national': national}[a.batch]()
