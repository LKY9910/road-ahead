import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const clock = seconds => `${String(Math.floor(seconds/60)).padStart(2,'0')}:${(seconds%60).toFixed(1).padStart(4,'0')}`;
const near = (actual,expected) => assert(Math.abs(actual-expected)<.05,`${actual} != ${expected}`);

export async function verifyPage(browser,capture,{here,frames,url}) {
  const context=await browser.newContext({viewport:{width:1920,height:1080},deviceScaleFactor:1});
  const page=await context.newPage(),errors=[],external=[];
  page.on('pageerror',error=>errors.push(error.message));
  await context.route(/^https?:/,route=>{external.push(route.request().url());return route.abort();});
  try {
    await page.goto(url('concept'));
    await page.evaluate(()=>document.fonts.ready);
    const script=await readFile(resolve(here,'../docs/VIDEO.md'),'utf8');
    const cuesDoc=await readFile(join(here,'CUES.md'),'utf8');
    const api=await page.evaluate(()=>({cues:conceptVideo.cues,durations:conceptVideo.durations,
      scenes:conceptVideo.scenes,introLine:conceptVideo.introLine,travelItems:conceptVideo.travelItems,weatherLine:conceptVideo.weatherLine}));
    assert.deepEqual(api.durations,{intro:15,concept:85,outro:5});
    assert.deepEqual(api.scenes,{E1:[50,59],E2:[59,75],D:[75,85]});
    for(const cue of api.cues) {
      assert(script.includes(cue.text),`대본과 다른 자막: ${cue.text}`);
      assert(cuesDoc.includes(cue.text),`CUES에 없는 자막: ${cue.text}`);
      assert(cuesDoc.includes(`${clock(cue.start)}–${clock(cue.end)}`),'CUES 자막 시각 불일치');
      assert(cue.start>=0 && cue.end<=api.durations[cue.section] && cue.end>cue.start);
    }
    const e1Source=script.split('**장면 E1:')[1].split('**장면 E2:')[0];
    const e2Source=script.split('**장면 E2:')[1].split('**장면 D:')[0];
    const dSource=script.split('**장면 D:')[1].split('## 1번')[0];
    const appQuestion=e1Source.match(/자막\(앱\): "([^"]+)"/)[1];
    const driverAnswer=e1Source.match(/자막\(운전자\): "([^"]+)"/)[1];
    const e2Captions=[...e2Source.matchAll(/^  \d\. "([^"]+)"/gm)].map(x=>x[1]);
    assert.equal(e2Captions.length,3);
    assert.deepEqual(api.cues.filter(c=>c.section==='concept' && c.start>=50).map(c=>c.text),
      [appQuestion,driverAnswer,...e2Captions,dSource.match(/자막: "([^"]+)"/)[1]]);
    const expectedItems=[...e2Source.matchAll(/^  \d\. ([^/\n]+) \/ ([^/\n]+?)(?: \/ \*\*([^\n]+?)\*\*)?\r?$/gm)]
      .map(x=>({name:x[1].trim(),detail:x[2].trim(),reasons:x[3]?x[3].split(' · '):[]}));
    assert.equal(expectedItems.length,3);
    assert.deepEqual(api.travelItems,expectedItems,'E2 항목·이유가 VIDEO.md 원문과 다릅니다.');
    assert(e2Source.includes(`"${api.weatherLine}"`));
    assert(script.includes(api.introLine) && cuesDoc.includes(api.introLine));
    assert.equal(api.cues.filter(c=>c.section==='intro').length,3);
    const navigationBackgrounds=await page.locator('#turn, #arrival').evaluateAll(es=>es.map(e=>getComputedStyle(e).backgroundColor));
    assert.deepEqual(navigationBackgrounds,['rgb(24, 32, 41)','rgb(24, 32, 41)']);

    async function at(section,time) {
      await page.evaluate(({section,time})=>conceptVideo.render(section,time),{section,time});
      return page.evaluate(()=>{
        const $=id=>document.getElementById(id);
        const box=e=>{const r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height,bottom:r.bottom,right:r.right};};
        const visible=e=>!e.hidden && e.getClientRects().length>0 && Number(getComputedStyle(e).opacity)>0;
        const tracked=[...['card-title','card-detail','countdown','parking-spaces','travel-weather','companion-answer','subtitle','headline','introduction'].map($),
          ...document.querySelectorAll('.travel-name,.travel-detail,.travel-reason')];
        const style=e=>getComputedStyle(e);
        return {
          title:$('card-title').textContent,count:$('countdown').dataset.value,prefix:$('countdown').dataset.prefix,
          detail:$('card-detail').textContent,source:$('source').textContent,kind:$('card-label').textContent,
          card:visible($('card')),turn:visible($('turn')),arrival:visible($('arrival')),signal:visible($('signal')),
          seconds:visible($('countdown')),label:visible($('concept-label')),
          labelOnTop:$('concept-label').contains(document.elementFromPoint(45,35)),
          color:$('card').style.getPropertyValue('--signal'),type:$('card').className,
          answer:$('companion-answer').textContent,answerVisible:visible($('companion-answer')),
          gear:$('gear-status').textContent,gearVisible:visible($('gear-status')),
          subtitle:$('subtitle').textContent,speaker:$('subtitle-speaker').textContent,speakerVisible:visible($('subtitle-speaker')),
          speakerFont:parseFloat(style($('subtitle-speaker')).fontSize),subtitleFont:parseFloat(style($('subtitle')).fontSize),
          travel:visible($('travel-list')),travelCount:$('travel-list').children.length,
          names:[...document.querySelectorAll('.travel-name')].map(e=>e.textContent),
          reasons:[...document.querySelectorAll('.travel-reason')].map(e=>({text:e.textContent,color:style(e).color,font:parseFloat(style(e).fontSize)})),
          weather:visible($('travel-weather')),weatherText:$('travel-weather').textContent,weatherColor:style($('travel-weather')).color,
          spaces:$('parking-spaces').textContent,parkingNumber:$('parking-number').textContent,
          parkingFont:parseFloat(style($('parking-number')).fontSize),parkingLabelFont:parseFloat(style(document.querySelector('.parking-label')).fontSize),
          parkingUnitFont:parseFloat(style(document.querySelector('.parking-unit')).fontSize),dot:visible($('parking-dot')),dotRect:box($('parking-dot')),
          arrivalText:$('arrival-time').textContent+' '+$('arrival-distance').textContent,
          map:{...$('map').dataset},cardRect:box($('card')),navRect:box($('navigation')),captionsRect:box($('captions')),
          subtitleRect:box($('subtitle')),speakerRect:box($('subtitle-speaker')),titleRect:box($('card-title')),answerRect:box($('companion-answer')),
          font:style($('card')).fontFamily,radius:parseFloat(style($('card')).borderRadius),padding:style($('card')).padding,
          cardVisibleIcons:[...$('card').querySelectorAll('svg,img,button,input')].filter(visible).length,
          // The legacy countdown has visible font overflow; reference pixels preserve A/B/C.
          overflow:tracked.filter(visible).filter(e=>e.scrollWidth>e.clientWidth+1 ||
            (e.id!=='countdown' && e.scrollHeight>e.clientHeight+1)).map(e=>e.id||e.className),
        };
      });
    }
    for(const [time,count] of [[.8,45],[1.8,44],[4.5,32],[5.5,31],[17.5,5],[18.5,4],[21.5,1]]) {
      const s=await at('concept',time);assert.equal(Number(s.count),count);assert.equal(s.color,'#E5484D');
    }
    const green=await at('concept',22.4);assert.equal(green.color,'#30A46C');assert(!green.seconds);
    assert(!(await at('concept',24)).card);
    const b=await at('concept',25.5);assert.equal(b.count,'118');assert(!b.turn && !b.arrival);assert.equal(b.prefix,'직진 ·');
    for(const [time,title,source] of [[38,'3km 앞 안개 구간','기상청 도로위험기상정보'],[43,'2.5km 앞 공사 구간','ITS 돌발정보'],[47,'3km 앞 살얼음 주의 구간','기상청 도로위험기상정보']]) {
      const s=await at('concept',time);assert.equal(s.title,title);assert.equal(s.source,source);assert.equal(s.color,'#F5A524');
    }
    const question=await at('concept',52);
    assert.equal(question.title,appQuestion);assert.equal(question.kind,'앞길 안내 · 여행');
    assert(!question.turn && !question.arrival && !question.signal && !question.travel && !question.dot);
    assert.equal(question.gear,'기어 P');assert(question.gearVisible);assert.equal(question.map.routed,'false');
    assert.equal(question.answer,'');assert(!question.speakerVisible);assert.equal(question.cardVisibleIcons,0);
    const driver=await at('concept',54.5);
    assert.equal(driver.subtitle,driverAnswer);assert(driver.speakerVisible);assert.equal(driver.speaker,'운전자');
    assert(driver.speakerFont<driver.subtitleFont);
    assert(driver.speakerRect.right<=driver.subtitleRect.x-12,'운전자 표시가 자막 앞에 분리되어야 합니다.');
    assert(driver.speakerRect.y>=driver.captionsRect.y && driver.speakerRect.bottom<=driver.captionsRect.bottom);
    const answer=await at('concept',56);
    assert.equal(answer.answer,'아이 · 반려동물');assert(answer.answerVisible);assert.equal(answer.cardVisibleIcons,0);
    assert.deepEqual(answer.cardRect,question.cardRect,'답변 때문에 카드 위치·크기가 뛰면 안 됩니다.');
    assert(answer.titleRect.bottom<answer.answerRect.y);
    const e=await at('concept',70);
    assert.equal(e.kind,'앞길 안내 · 여행');assert(e.travel && !e.signal && !e.dot && !e.speakerVisible);
    assert.equal(e.travelCount,3);assert.equal(e.weatherText,api.weatherLine);assert(e.weather);
    assert.equal(e.weatherColor,'rgb(201, 209, 217)');
    assert.deepEqual(e.reasons.map(r=>r.text),api.travelItems.filter(i=>i.reasons.length).map(i=>i.reasons.join(' · ')));
    for(const reason of e.reasons){assert.equal(reason.color,'rgb(184, 212, 255)');near(reason.font,20*450/560);}
    assert.equal(e.gear,'기어 P');assert.equal(e.arrivalText,'도착 12:24 · 3.2km');
    const d=await at('concept',79);
    assert.equal(d.title,'중앙 공영주차장');assert.equal(d.kind,'앞길 안내 · 주차');assert(d.dot && !d.signal && !d.travel);
    assert.equal(d.spaces,'빈자리23면');assert.equal(d.parkingNumber,'23');
    near(d.dotRect.width,16*450/560);near(d.parkingFont,96*450/560);near(d.parkingLabelFont,32*450/560);near(d.parkingUnitFont,d.parkingLabelFont);
    assert(d.parkingFont>d.parkingLabelFont*2);
    assert.equal(d.detail,'목적지에서 180m · 30분에 1,000원');assert.equal(d.arrivalText,'도착 12:24 · 1.0km');assert.equal(d.gear,'기어 D');
    const canvasAt=async time=>{await at('concept',time);return page.locator('#map').evaluate(c=>c.toDataURL());};
    assert.equal(await canvasAt(51),await canvasAt(58),'E1 정차 화면이 움직이면 안 됩니다.');
    assert.equal((await at('concept',59)).map.routeProgress,'0');
    assert.notEqual(await canvasAt(59),await canvasAt(61),'E2에서 새 경로를 그려야 합니다.');
    assert.equal(await canvasAt(61),await canvasAt(74),'E2 추천 중 차가 움직이면 안 됩니다.');
    assert.notEqual(await canvasAt(76),await canvasAt(83),'D에서는 차가 목적지로 다가가야 합니다.');

    const times=[.5,4.5,17.5,22.4,25.5,35.5,41.5,46,51,54.3,55.5,58.8,59.4,64.6,70,74.9,75.5,80,84.9];
    for(const cue of api.cues.filter(c=>c.section==='concept'))times.push((cue.start+cue.end)/2);
    for(const time of times) {
      const s=await at('concept',time);assert(s.label && s.card);assert.equal(s.cardRect.width,450);
      assert.equal(s.navRect.right-s.cardRect.right,30);near(s.navRect.bottom-s.cardRect.bottom,30);
      assert(s.cardRect.bottom<=s.captionsRect.y);assert(s.cardRect.y>=80);
      assert(s.subtitleRect.y>=s.captionsRect.y && s.subtitleRect.bottom<=s.captionsRect.bottom);
      assert(s.font.includes('Malgun Gothic'));assert.deepEqual(s.overflow,[],`넘침 at ${time}`);
      if(time<50){assert.equal(s.radius,24);assert.equal(s.padding,'21px 30px 27px');}
      else near(s.radius,32*450/560);
    }
    for(const cue of api.cues) {
      const s=await at(cue.section,(cue.start+cue.end)/2);
      if(cue.section==='concept')assert.equal(s.subtitle,cue.text);
      else assert.deepEqual(s.overflow,[]);
      if(cue.section==='concept')assert.equal(s.speakerVisible,cue.speaker==='driver');
    }
    for(const time of [4,10])assert((await at('concept',time)).labelOnTop);
    const ref=JSON.parse(await readFile(join(here,'abc-reference.json'),'utf8'));
    for(const frame of ref.frames) {
      await at(frame.section,frame.time);
      assert.equal(createHash('sha256').update(await page.screenshot()).digest('hex'),frame.sha256,
        `A/B/C 또는 마무리 프레임 변경: ${frame.section} ${frame.time}`);
    }
    for(const [section,time,file] of [
      ['intro',2,'01-intro.png'],['intro',12,'01-intro-travel.png'],['concept',4.5,'02-scene-a.png'],
      ['concept',27,'02-scene-b.png'],['concept',38,'02-scene-c.png'],['concept',52,'02-scene-e1-question.png'],
      ['concept',56,'02-scene-e1-answer.png'],['concept',70,'02-scene-e2.png'],['concept',79,'02-scene-d.png'],['outro',3,'04-outro.png'],
    ]) {
      assert.deepEqual((await at(section,time)).overflow,[]);
      if(capture){const bytes=await page.screenshot({path:join(frames,file)});if(file==='02-scene-e2.png')await writeFile(join(frames,'02-scene-e.png'),bytes);}
    }
    const intro=await page.locator('#introduction').textContent();
    assert.equal(intro.replace(/\s+/g,' '),api.introLine);assert(intro.includes(',\n'));
    assert(await page.locator('#introduction').evaluate(e=>e.getBoundingClientRect().height<=2*parseFloat(getComputedStyle(e).lineHeight)+1));
    assert.equal(await page.locator('audio,video,iframe').count(),0);
    assert.deepEqual(errors,[]);assert.equal(external.length,0,'외부 네트워크 요청이 있었습니다.');
    console.log(`검증 통과: 자막 원문·CUES 시각, E1 운전자 구별·정차, E2 3항목·이유·날씨·경로, D 숫자 강조, 겹침/넘침 0, A/B/C·마무리 ${ref.frames.length}프레임 동일, 외부 요청 0건.`);
    return {captions:api.cues.length,geometrySamples:times.length,unchangedReferenceFrames:ref.frames.length,externalRequests:0,pageErrors:0};
  } finally {await context.close();}
}
