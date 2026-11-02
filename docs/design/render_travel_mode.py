"""Render T-32 static design mockups; never starts the app or emulator."""
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

HERE=Path(__file__).resolve().parent
SCREENS={'companions':'companion-selection.png','none':'companion-none.png',
    'first':'companion-first.png','settings':'companion-settings.png',
    'travel-child':'travel-child.png','travel-pet':'travel-pet.png','travel-weather':'travel-weather.png',
    'parking-available':'parking-available.png','parking-unknown':'parking-unknown.png'}


def render():
    results=[]
    with sync_playwright() as p:
        browser=p.chromium.launch(headless=True,args=['--allow-file-access-from-files'])
        page=browser.new_page(viewport={'width':2560,'height':1440},device_scale_factor=1)
        for screen,filename in SCREENS.items():
            page.goto(HERE.joinpath('travel-mode-ui.html').as_uri()+'?screen='+screen)
            page.evaluate('document.fonts.ready')
            assert page.evaluate("document.fonts.check('26px RoadNoto')")
            measurements=page.evaluate('''() => {
                const box=e=>{let r=e.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height}};
                const measure=e=>({text:e.textContent,...box(e),font:getComputedStyle(e).fontSize,
                    scrollWidth:e.scrollWidth,clientWidth:e.clientWidth,scrollHeight:e.scrollHeight,clientHeight:e.clientHeight});
                return {viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},
                    card:document.querySelector('.overlay-card')?box(document.querySelector('.overlay-card')):null,
                    text:[...document.querySelectorAll('.weather-context,.travel-title,.travel-meta,.reason,.parking-title,.parking-meta span,.availability,.availability-label,.availability-number,.availability-unit')].map(measure),
                    targets:[...document.querySelectorAll('button')].map(e=>({text:e.textContent.trim(),...box(e)}))};
            }''')
            for line in measurements['text']:
                assert line['scrollWidth']<=line['clientWidth']+1,line
                assert line['scrollHeight']<=line['clientHeight']+1,line
            for target in measurements['targets']:
                assert target['width']>=80 and target['height']>=80,target
            if measurements['card']:
                card=measurements['card']
                assert card['x']==1824 and card['width']==560 and card['y']+card['height']==1260,card
            page.screenshot(path=str(HERE/filename),full_page=False)
            if screen in ['travel-child','travel-pet','travel-weather','parking-available','parking-unknown']:
                page.locator('.overlay-card').screenshot(path=str(HERE/filename.replace('.png','-detail.png')))
            results.append({'screen':screen,'file':filename,**measurements})
        page.goto(HERE.joinpath('travel-mode-ui.html').as_uri()+'?screen=first')
        assert page.locator('[data-action="confirm"]').is_disabled()
        page.locator('[data-choice="child"]').click()
        page.locator('[data-choice="pet"]').click()
        assert set(page.evaluate('design.state.draft'))=={'child','pet'}
        page.locator('[data-choice="none"]').click()
        assert page.evaluate('design.state.draft')==['none']
        page.locator('[data-choice="elder"]').click()
        assert page.evaluate('design.state.draft')==['elder']
        page.evaluate('design.setParked(false)')
        assert page.locator('[data-choice]').count()==0
        assert page.evaluate('design.state.saved')==[] and not page.evaluate('design.state.travel')
        page.goto(HERE.joinpath('travel-mode-ui.html').as_uri()+'?screen=companions')
        page.locator('[data-action="confirm"]').click()
        assert page.evaluate('design.state.travel')
        assert set(page.evaluate('design.state.saved'))=={'child','pet'}
        page.locator('[data-action="change"]').click()
        page.locator('[data-choice="none"]').click()
        page.evaluate('design.setParked(false)')
        assert set(page.evaluate('design.state.saved'))=={'child','pet'} and page.evaluate('design.state.travel')
        page.evaluate('document.fonts.ready')
        font_measurements=page.evaluate('''() => {
            const c=document.createElement('canvas').getContext('2d');
            return [[96,'빈자리 10면'],[96,'빈자리 781면'],[34,'동국제강 공영주차장(시)'],
                [30,'목적지에서 200m · 5분에 430원']].map(([size,text])=>{
                c.font=`${size}px RoadNoto`;return {text,fontSize:size,width:c.measureText(text).width};});
        }''')
        browser.close()
    (HERE/'mockup-measurements.json').write_text(json.dumps({'screens':results,'fontMeasurements':font_measurements,'prototypeChecks':'passed'},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'rendered {len(results)} full frames and 5 card details; prototype interaction and geometry checks passed')


if __name__=='__main__':render()
