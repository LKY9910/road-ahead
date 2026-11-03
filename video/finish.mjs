import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { promisify } from 'node:util';
import { loadPlaywright, findChromium } from './render.mjs';

const run = promisify(execFile);
const here = dirname(fileURLToPath(import.meta.url)), out = join(here, 'out');
const raw = join(out, 'raw'), parts = join(out, 'final-parts');
const json = async path => JSON.parse(await readFile(path, 'utf8'));
const hash = async path => createHash('sha256').update(await readFile(path)).digest('hex');
const originalNames = ['01-intro.webm', '02-concept.webm', '04-outro.webm'];
const encode = ['-an', '-r', '30', '-c:v', 'libx264', '-preset', 'slow', '-crf', '20',
  '-maxrate', '2800k', '-bufsize', '5600k', '-pix_fmt', 'yuv420p', '-profile:v', 'high',
  '-level:v', '4.1', '-video_track_timescale', '30000', '-movflags', '+faststart', '-map_metadata', '-1'];

function validateCuts(cuts, evidence) {
  assert.equal(cuts.length, 3);
  assert.deepEqual(cuts.map(cut => cut.scene), ['a', 'c', 'e']);
  const length = cuts.reduce((sum, cut) => sum + cut.duration, 0);
  assert(length >= 20 && length <= 30);
  for (const cut of cuts) {
    assert(Number.isFinite(cut.in) && cut.in >= 0);
    assert(Number.isFinite(cut.duration) && cut.duration > 0);
    assert.equal(cut.duration * 30, Math.round(cut.duration * 30));
    assert(cut.caption.text.length > 0);
    assert(cut.caption.from >= 0 && cut.caption.until <= cut.duration && cut.caption.until > cut.caption.from);
    assert.equal(cut.caption.text, evidence[cut.scene].speeches[cut.speechIndex].text, 'Caption must equal the original speak= text');
    assert.equal(evidence[cut.scene].safety.secretOccurrences, 0);
    assert.equal(evidence[cut.scene].safety.ttsOnStart, 0);
  }
  return length;
}

function selfTest() {
  const cuts = ['a', 'c', 'e'].map(scene => ({scene, in: 0, duration: 9, speechIndex: 0,
    caption: {text: '앱 원문', from: 0, until: 9}}));
  const evidence = Object.fromEntries(cuts.map(cut => [cut.scene, {speeches: [{text: '앱 원문'}],
    safety: {secretOccurrences: 0, ttsOnStart: 0}}]));
  assert.equal(validateCuts(cuts, evidence), 27);
  for (const alter of [c => c[0].caption.text = '고친 문장', c => c[0].in = -1,
    c => c[0].caption.until = 10, c => c[0].duration = 50]) {
    const changed = structuredClone(cuts); alter(changed);
    assert.throws(() => validateCuts(changed, evidence));
  }
  console.log('finish self-test ok: original speech, safe evidence, bounds, 20–30 seconds');
}

async function probe(ffmpeg, file) {
  const result = await run(ffmpeg, ['-hide_banner', '-i', file]).catch(error => error);
  const message = result.stderr || '';
  const duration = message.match(/Duration: (\d+):(\d+):(\d+\.\d+)/);
  const size = message.match(/Video:.*?\b(\d{3,5})x(\d{3,5})\b/);
  const fps = message.match(/(\d+(?:\.\d+)?) fps/);
  assert(duration && size && fps, `Invalid video: ${file}`);
  return {seconds: Number(duration[1])*3600 + Number(duration[2])*60 + Number(duration[3]),
    width: Number(size[1]), height: Number(size[2]), fps: Number(fps[1]),
    h264: /Video: h264/.test(message), audio: /Stream .*Audio:/.test(message), bytes: (await stat(file)).size};
}

async function overlays(cuts) {
  const {chromium} = await loadPlaywright();
  const browser = await chromium.launch({headless: true, executablePath: await findChromium(chromium)});
  try {
    const page = await browser.newPage({viewport: {width: 1920, height: 1080}, deviceScaleFactor: 1});
    await page.route(/^https?:/, route => route.abort());
    // Match the existing concept badge and caption strip; only the badge wording changes.
    await page.setContent(`<style>
      *{box-sizing:border-box} html,body{margin:0;width:1920px;height:1080px;overflow:hidden;background:transparent}
      body{font-family:'Malgun Gothic',sans-serif;color:#fff}
      #label{position:absolute;left:30px;top:22px;padding:8px 15px;border:1px solid #65717c;border-radius:7px;background:#182029e8;font-size:20px;font-weight:700}
      #captions{position:absolute;bottom:0;height:130px;width:100%;display:grid;place-items:center;padding:15px 130px;background:#11171e;border-top:1px solid #3e4852}
      #subtitle{font-size:32px;line-height:1.55;font-weight:700;text-align:center;word-break:keep-all;text-wrap:balance}
      .caption-only #label{visibility:hidden}.caption-only #captions{background:transparent;border-color:transparent}
      </style><div id="label">실제 앱 화면 · 에뮬레이터 기록 재생</div><div id="captions"><div id="subtitle"></div></div>`);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({path: join(parts, 'base.png'), omitBackground: true});
    const layout = [];
    for (const cut of cuts) {
      await page.evaluate(text => {
        document.body.className = 'caption-only'; document.getElementById('subtitle').textContent = text;
      }, cut.caption.text);
      const box = await page.locator('#subtitle').boundingBox();
      assert(box.width <= 1660 && box.height <= 100 && box.y >= 965 && box.y + box.height <= 1065, 'Caption overflow');
      layout.push({scene: cut.scene, caption: box, cardBottomMax: 905, captionStripTop: 950});
      await page.screenshot({path: join(parts, `caption-${cut.scene}.png`), omitBackground: true});
    }
    return layout;
  } finally { await browser.close(); }
}

async function checkOutput(ffmpeg, cuts, seconds, originals, layout) {
  const files = ['03-real.mp4', 'SUDL_내비서_시연.mp4'];
  const metadata = [];
  for (const [index, name] of files.entries()) {
    const info = await probe(ffmpeg, join(out, name));
    assert.equal(info.width, 1920); assert.equal(info.height, 1080); assert.equal(info.fps, 30);
    assert(info.h264 && !info.audio); assert(Math.abs(info.seconds - (seconds + (index ? 105 : 0))) <= .04);
    assert(info.bytes < 50_000_000);
    const decoded = await run(ffmpeg, ['-v', 'error', '-i', join(out, name), '-progress', 'pipe:1', '-f', 'null', '-'], {maxBuffer: 8*1024*1024});
    assert.equal(decoded.stderr.trim(), '', 'Decode errors');
    const counts = [...decoded.stdout.matchAll(/^frame=(\d+)$/gm)].map(match => Number(match[1]));
    assert.equal(counts.at(-1), Math.round(info.seconds * 30));
    const bytes = await readFile(join(out, name));
    assert(bytes.indexOf(Buffer.from('moov')) < bytes.indexOf(Buffer.from('mdat')), 'Fast-start metadata missing');
    metadata.push({file: name, ...info, decodedFrames: counts.at(-1)});
  }
  for (const name of originalNames) assert.equal(await hash(join(out, name)), originals[name], `${name} changed`);
  const playback = await checkPlayback();
  const proof = {metadata, originalSha256: originals, layout, cuts, playback};
  await writeFile(join(out, 'real-validation.json'), JSON.stringify(proof, null, 2) + '\n');
  console.log(JSON.stringify(metadata, null, 2));
}

async function checkPlayback() {
  const {chromium} = await loadPlaywright();
  const browser = await chromium.launch({headless: true, executablePath: await findChromium(chromium)});
  try {
    await writeFile(join(out, 'player-check.html'), '<style>html,body{margin:0;background:#000}video{width:1920px;height:1080px}</style><video muted preload="auto" src="SUDL_내비서_시연.mp4"></video>');
    const page = await browser.newPage({viewport: {width: 1920, height: 1080}});
    await page.route(/^https?:/, route => route.abort());
    await page.goto(pathToFileURL(join(out, 'player-check.html')).href);
    await page.waitForFunction(() => {const v = document.querySelector('video'); return v.error || v.readyState >= 2;});
    const start = await page.evaluate(async () => {
      const v = document.querySelector('video'); if (v.error) throw new Error(`Media error ${v.error.code}`);
      await v.play(); return v.currentTime;
    });
    await page.waitForFunction(start => document.querySelector('video').currentTime > start + .2, start);
    for (const [name, time] of [['intro', 2], ['concept', 19.5], ['signal', 103], ['fog', 112], ['parking', 120], ['outro', 130]]) {
      await page.evaluate(time => new Promise(resolve => {
        const v = document.querySelector('video'); v.pause(); v.addEventListener('seeked', resolve, {once: true}); v.currentTime = time;
      }), time);
      await page.locator('video').screenshot({path: join(out, `player-${name}.png`)});
    }
    return {chromium: browser.version(), h264Played: true, seekedSections: 6};
  } finally { await browser.close(); }
}

async function main() {
  assert(process.argv.length <= 3 && [undefined, '--self-test', '--check'].includes(process.argv[2]));
  if (process.argv[2] === '--self-test') return selfTest();
  const ffmpeg = (await run('py', ['-3.12', '-I', '-c', 'import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())'])).stdout.trim();
  const cuts = await json(join(here, 'real-cuts.json'));
  const evidence = Object.fromEntries(await Promise.all(['a', 'c', 'e'].map(async scene => [scene, await json(join(raw, `real-${scene}.json`))])));
  const seconds = validateCuts(cuts, evidence);
  // shortcut: These cuts describe the saved takes; update real-cuts.json after recording new takes.
  for (const cut of cuts) {
    const source = join(raw, `real-${cut.scene}.mp4`);
    assert.equal((await stat(source)).size, evidence[cut.scene].sourceBytes);
    assert.equal(await hash(source), evidence[cut.scene].sourceSha256, 'Recording differs from its evidence');
  }
  const originals = Object.fromEntries(await Promise.all(originalNames.map(async name => [name, await hash(join(out, name))])));
  if (process.argv[2] === '--check') {
    const previous = await json(join(out, 'real-validation.json'));
    assert.deepEqual(previous.originalSha256, originals);
    assert.deepEqual(previous.cuts, cuts);
    return checkOutput(ffmpeg, cuts, seconds, originals, previous.layout);
  }
  await mkdir(parts, {recursive: true});
  const layout = await overlays(cuts);
  const inputs = [];
  for (const cut of cuts) {
    const input = join(raw, `real-${cut.scene}.mp4`), info = await probe(ffmpeg, input);
    assert.equal(info.width, 1920); assert.equal(info.height, 1080);
    assert(cut.in + cut.duration <= info.seconds, 'Cut exceeds its recording');
    inputs.push('-i', input);
  }
  inputs.push('-loop', '1', '-framerate', '30', '-i', join(parts, 'base.png'));
  for (const cut of cuts) inputs.push('-loop', '1', '-framerate', '30', '-i', join(parts, `caption-${cut.scene}.png`));
  const filters = ['[3:v]split=3[badge0][badge1][badge2]'];
  cuts.forEach((cut, i) => {
    filters.push(`[${i}:v]trim=start=${cut.in}:duration=${cut.duration},setpts=PTS-STARTPTS,crop=1260:622:640:352,scale=1920:948:flags=lanczos,pad=1920:1080:0:0:color=0x11171e,setsar=1,fps=30[v${i}]`);
    filters.push(`[v${i}][badge${i}]overlay=shortest=1[b${i}]`);
    filters.push(`[b${i}][${4+i}:v]overlay=shortest=1:enable='between(t,${cut.caption.from},${cut.caption.until})'[c${i}]`);
  });
  filters.push('[c0][c1][c2]concat=n=3:v=1:a=0[result]');
  await run(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', ...inputs, '-filter_complex', filters.join(';'),
    '-map', '[result]', ...encode, join(out, '03-real.mp4')], {maxBuffer: 4*1024*1024});
  for (const name of originalNames) await run(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', '-i', join(out, name),
    ...encode, join(parts, name.replace('.webm', '.mp4'))], {maxBuffer: 4*1024*1024});
  const ordered = [join(parts, '01-intro.mp4'), join(parts, '02-concept.mp4'), join(out, '03-real.mp4'), join(parts, '04-outro.mp4')];
  await writeFile(join(parts, 'concat.txt'), ordered.map(file => `file '${file.replaceAll('\\', '/').replaceAll("'", "'\\''")}'`).join('\n') + '\n');
  await run(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', '-f', 'concat', '-safe', '0', '-i', join(parts, 'concat.txt'),
    '-map', '0:v:0', '-c', 'copy', '-an', '-movflags', '+faststart', '-map_metadata', '-1', join(out, 'SUDL_내비서_시연.mp4')]);
  const stills = [['signal-early', 3], ['signal-later', 7], ['fog', cuts[0].duration + 3], ['parking', cuts[0].duration + cuts[1].duration + 3]];
  for (const [name, time] of stills) await run(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', '-ss', String(time),
    '-i', join(out, '03-real.mp4'), '-frames:v', '1', join(here, 'frames', `03-${name}.png`)]);
  await checkOutput(ffmpeg, cuts, seconds, originals, layout);
}

main().catch(error => {console.error(error.stderr || error.stack || error.message); process.exitCode = 1;});
