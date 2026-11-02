"""Capture the complete Seoul list every 60 seconds for at least 16 minutes."""
import argparse
from datetime import datetime
import json
import time
from probe import capture, OUT


def snapshot(index, prefix):
    records=[]
    start=1
    while True:
        record=capture(f'{prefix}-{index:02}-page-{start}',start,start+999)
        records.append(record)
        response=record.get('response',{}).get('GetParkingInfo',{})
        if response.get('RESULT',{}).get('CODE')!='INFO-000':
            raise ValueError(f'observation {index} did not return a success body')
        total=int(response['list_total_count'])
        if start+999>=total:break
        start+=1000
    return records


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--samples',type=int,default=17)
    parser.add_argument('--interval',type=float,default=60)
    parser.add_argument('--prefix',default=datetime.now().strftime('observe-%Y%m%d-%H%M%S'))
    args=parser.parse_args()
    if args.samples<17 or args.interval<60:
        parser.error('use at least 17 samples and 60-second intervals')
    beginning=time.monotonic()
    metadata=[]
    index_path=OUT/f'{args.prefix}-index.json'
    if index_path.exists():parser.error('observation index already exists')
    print(f'index: {index_path}',flush=True)
    for index in range(args.samples):
        due=beginning+args.interval*index
        while time.monotonic()<due:
            time.sleep(min(20,due-time.monotonic()))
        records=snapshot(index,args.prefix)
        metadata.append({'index':index,'requestedAtKst':records[0]['requestedAtKst'],
                         'labels':[f'{args.prefix}-{index:02}-page-{r["start"]}' for r in records]})
        index_path.write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print(f'observation {index+1}/{args.samples} complete; elapsed={time.monotonic()-beginning:.1f}s',flush=True)
