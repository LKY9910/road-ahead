import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { createRequire } from 'node:module';
import { mkdir, readdir, readFile, rm, stat, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { promisify } from 'node:util';
import { verifyPage } from './check.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const out = join(here, 'out');
const frames = join(here, 'frames');
const require = createRequire(import.meta.url);
const run = promisify(execFile);
const viewport = { width: 1920, height: 1080 };
const sections = [
  { name: 'intro', file: '01-intro.webm', duration: 15 },
  { name: 'concept', file: '02-concept.webm', duration: 85 },
  { name: 'outro', file: '04-outro.webm', duration: 5 },
];

export async function loadPlaywright() {
  for (const name of ['playwright', 'playwright-core']) {
    try { return require(name); } catch (error) { if (error.code !== 'MODULE_NOT_FOUND') throw error; }
  }
  const cache = process.env.npm_config_cache || (process.env.LOCALAPPDATA && join(process.env.LOCALAPPDATA, 'npm-cache'));
  if (cache) {
    const entries = await readdir(join(cache, '_npx'), { withFileTypes: true }).catch(() => []);
    for (const entry of entries.filter(e => e.isDirectory()).sort((a,b) => a.name.localeCompare(b.name))) {
      for (const name of ['playwright', 'playwright-core']) {
        try { return require(join(cache, '_npx', entry.name, 'node_modules', name)); }
        catch (error) { if (error.code !== 'MODULE_NOT_FOUND') throw error; }
      }
    }
  }
  throw new Error('설치된 Playwright 모듈을 찾지 못했습니다. 새로 설치하지 말고 HANDOFF에 남긴 뒤 담당자에게 문의하세요.');
}

async function findFfmpeg() {
  const root = process.env.PLAYWRIGHT_BROWSERS_PATH || (process.env.LOCALAPPDATA && join(process.env.LOCALAPPDATA, 'ms-playwright'));
  if (root) {
    const entries = await readdir(root, { withFileTypes: true }).catch(() => []);
    for (const entry of entries.filter(e => e.isDirectory() && e.name.startsWith('ffmpeg-')).sort((a,b) => b.name.localeCompare(a.name))) {
      const file = join(root, entry.name, process.platform === 'win32' ? 'ffmpeg-win64.exe' : 'ffmpeg-linux');
      if (await stat(file).catch(() => null)) return file;
    }
  }
  throw new Error('Playwright와 함께 설치된 FFmpeg를 찾지 못했습니다. 담당자에게 문의하세요.');
}

export async function findChromium(chromium) {
  const expected=chromium.executablePath();
  if(await stat(expected).catch(()=>null)) return expected;
  const root=process.env.PLAYWRIGHT_BROWSERS_PATH || (process.env.LOCALAPPDATA && join(process.env.LOCALAPPDATA,'ms-playwright'));
  if(root && process.platform==='win32') {
    const entries=await readdir(root,{withFileTypes:true}).catch(()=>[]);
    for(const entry of entries.filter(e=>e.isDirectory() && /^chromium-\d+$/.test(e.name)).sort((a,b)=>b.name.localeCompare(a.name,undefined,{numeric:true}))) {
      const file=join(root,entry.name,'chrome-win64','chrome.exe');
      if(await stat(file).catch(()=>null)) return file;
    }
  }
  throw new Error('설치된 Chromium을 찾지 못했습니다. 새로 설치하지 말고 담당자에게 문의하세요.');
}

function url(section) {
  const target = pathToFileURL(join(here, 'concept.html'));
  target.search = new URLSearchParams({ section, paused: '1' }).toString();
  return target.href;
}

async function probe(ffmpeg, file) {
  const result = await run(ffmpeg,['-hide_banner','-i',file], { maxBuffer: 1024*1024 }).catch(error => error);
  const stderr = result.stderr || '';
  const duration = stderr.match(/Duration: (\d+):(\d+):(\d+\.\d+)/);
  const size = stderr.match(/Video:.*?\b(\d{3,5})x(\d{3,5})\b/);
  const fps = stderr.match(/(\d+(?:\.\d+)?) fps/);
  assert(duration && size && fps, `영상 메타데이터 확인 실패: ${file}`);
  return { duration: Number(duration[1])*3600+Number(duration[2])*60+Number(duration[3]), width:Number(size[1]), height:Number(size[2]), fps:Number(fps[1]), audio:/Stream .*Audio:/.test(stderr) };
}

async function main() {
  assert(process.argv.length <= 3 && (!process.argv[2] || process.argv[2] === '--check'), '사용법: node video/render.mjs [--check]');
  const checkOnly=process.argv.includes('--check');
  const { chromium }=await loadPlaywright();
  const ffmpeg=checkOnly ? null : await findFfmpeg();
  if(!checkOnly) { await mkdir(out,{recursive:true}); await mkdir(frames,{recursive:true}); }
  const browser=await chromium.launch({headless:true,executablePath:await findChromium(chromium)});
  try {
    const checks=await verifyPage(browser,!checkOnly,{here,frames,url});
    if(checkOnly) return;
    const metadata=[];
    for(const section of sections) {
      console.log(`${section.file} 녹화 시작 (${section.duration}초)`);
      const context=await browser.newContext({ viewport, deviceScaleFactor:1, recordVideo:{ dir:join(out,'raw'), size:viewport } });
      const page=await context.newPage(), video=page.video();
      const errors=[];
      page.on('pageerror',error=>errors.push(error.message));
      await context.route(/^https?:/,route=>{ errors.push('외부 네트워크 요청'); return route.abort(); });
      try {
        await page.goto(url(section.name));
        await page.evaluate(()=>document.fonts.ready);
        await page.evaluate(name=>conceptVideo.play(name),section.name);
      } finally { await context.close(); }
      assert.deepEqual(errors,[]);
      const raw=await video.path(), info=await probe(ffmpeg,raw);
      assert(info.duration >= section.duration, '원본 녹화가 예상 길이보다 짧습니다.');
      const target=join(out,section.file);
      // shortcut: This recorder appends a one-second tail; revisit the trim when changing Playwright versions.
      const leadIn=Math.max(0,info.duration-section.duration-1);
      await run(ffmpeg,['-hide_banner','-loglevel','error','-y','-ss',leadIn.toFixed(3),'-i',raw,'-t',String(section.duration),'-an','-r','30','-c:v','libvpx','-b:v','8M','-deadline','realtime','-cpu-used','4',target],{maxBuffer:1024*1024});
      const actual=await probe(ffmpeg,target);
      assert.equal(actual.width,1920); assert.equal(actual.height,1080); assert.equal(actual.fps,30); assert(!actual.audio);
      assert(Math.abs(actual.duration-section.duration)<=.15,`영상 길이 불일치: ${section.file}`);
      await rm(raw);
      metadata.push({file:section.file,...actual,bytes:(await stat(target)).size});
      console.log(`${section.file}: ${actual.duration.toFixed(2)}초, 1920×1080, 30fps, 무음`);
    }
    await writeFile(join(out,'metadata.json'),JSON.stringify(metadata,null,2)+'\n','utf8');
    await writeFile(join(out,'validation.json'),JSON.stringify(checks,null,2)+'\n','utf8');
  } finally { await browser.close(); }
}

if(process.argv[1] && resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  main().catch(error=>{ console.error(error.message); process.exitCode=1; });
}
