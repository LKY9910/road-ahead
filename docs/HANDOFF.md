# 인수인계 로그

새 항목은 **맨 위**에 추가한다. 형식:

```
## YYYY-MM-DD HH:MM · 작성자 · T-번호
- 한 일:
- 검증: (실행한 명령과 결과)
- 남은 문제:
- 요청: (누구에게 무엇을)
```

첫 번째 아이디어 "가는길에"의 기록은 [프로젝트1/docs/HANDOFF.md](../프로젝트1/docs/HANDOFF.md)에 보관했다.

---

## 2026-11-02 18:30 · Claude · T-46 이름 변경 마무리(release·기획서)

- release: `:app:assembleRelease`로 다시 만들어 `apk/SUDL_내비서_release.apk`(41,981,790 bytes)로 두고 옛 `apk/SUDL_앞길안내_release.apk`는 지웠다(`0bb7196`, README 이름 변경과 함께). `DEBUG=false`, 키 4개 비어 있지 않음, 서명 CN=SUDL·인증서 SHA-256 `2d24f48b…`(전과 같음), APK 안 키스토어 비밀번호 0건, `aapt` 앱 이름 **내비서**를 값 출력 없이 확인했다.
- 에뮬레이터: T-44의 debug를 지우고 release를 user 10에 설치했다. 첫 실행 순서(위치 권한 "앱 사용 중에만 허용" → 다른 앱 위에 표시 허용 → 고지 확인)를 거쳐 위치 권한 창·첫 화면 제목·고지가 모두 "내비서"로 뜨는 것을 봤다. 실시간 모드에서 소리가 나지 않게 앱은 강제 종료해 두었다(권한·고지 확인은 남음).
- 기획서: `tools/make_proposal.py`로 `docs/forms/내비서_기획서.docx`(335,671 bytes)를 다시 만들었다. self-test·양식 검증 통과, LibreOffice 18쪽, 본문 "앞길 안내" 0건·"내비서" 4건, 그림 8장 모두 새 이름. 옛 `앞길안내_기획서.docx`는 지웠다(`docs/forms/`는 gitignore).
- 남은 일(사용자): 01·04 개인정보와 서명, 7번 칸의 영상 링크(노란 표시), Word에서 쪽 나눔 확인. 제출 저장소 push는 사용자에게 다시 묻는다.

## 2026-11-02 18:25 · Claude · T-44 리뷰 완료

- 코드: `48c2e61`의 이름 참조를 읽었다. 이름 원문은 `R.string.app_name` 한 곳이고, 카드 머리줄 3곳·첫 화면·동행 화면·알림(`%1$s가 켜져 있어요`)·권한 안내 경로가 이 값을 쓴다. `app/src/main`에서 assets 제외 "앞길" 0건.
- 다시 돌린 검증: `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` 성공, 테스트 196개(실패·skip 0).
- 캡처: `overlay-a-d30.png`(내비서 · 신호), `overlay-e-child-pet-p.png`(내비서 · 여행), `t36-companion-first.png`(제목 내비서), `proposal/prototype-parking.png`(내비서 · 주차)를 직접 봤다. 배치·크기는 전과 같다. `t36-companion-first.png`의 아래 카드는 전에도 있던 도로 위험 카드이며 내용만 공사→살얼음으로 바뀌었다.
- 작업 중 06:45에 Codex가 "Selected model is at capacity"로 멈춰 1시간 넘게 진행이 없었다. Claude가 터미널로 이어서 하라고 보내 재개했다.
- 결과: T-44 `완료`. release 재설치·기획서 DOCX는 T-46에서 한다.

## 2026-11-02 18:10 · Codex · T-44 앱 이름을 내비서로

- 한 일: 앱 표시 이름을 **내비서**로 바꿨다. 이름의 유일한 원문은 `R.string.app_name`이고, 알림 본문·오버레이 권한 안내는 `%1$s`에 이 값을 넣는다. `OverlayCard`의 신호/도로 위험·여행·주차 머리줄과 Compose 첫 화면·동행 선택 화면도 같은 리소스를 읽는다. 패키지·클래스 이름과 글자 크기·색·간격·배치는 그대로다.
- 이름 검색: `app/src/main`의 **assets 제외 `앞길` 0건**, **`내비서` 원문 1건**(`strings.xml`의 `app_name`)이다. 공공 원자료 `assets/parking/lots.tsv`의 실제 주차장 이름 두 건은 Orca ask 답변에 따라 보존했다. `aapt dump badging`에서도 패키지 `com.roadahead`, 앱 표시 이름 **내비서**를 확인했다.
- 빌드·테스트: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` **BUILD SUCCESSFUL**. 앱 **29개**·core **167개**, 합계 **196개**이며 실패·오류·skip **0개**다(`.gradle/t44-build.log`). 이름 참조 변경만 있어 새 단위 테스트는 추가하지 않았다.
- 카드 캡처: 아래 **10장**을 같은 파일명으로 갱신했다. 모두 **2560×1440**, 내비 화면 위, 지정된 P/0 또는 D/30 상태다. T-41의 표시값(신호 30/35초, 안개 거리, 공사·살얼음, 추천 목록, 빈자리 849면)을 맞췄다. **10장 모두 창의 x/y/너비/높이가 전과 같고**, 머리줄 아래 본문 영역(카드 상단 70px 제외)은 이전 PNG와 **픽셀 단위 동일**하다. 크기·줄바꿈·본문 표시값이 바뀌지 않았음을 함께 확인했다.

| 캡처(`docs/demo/`) | 시연 시작 뒤 실제 캡처(초) | 카드 높이 전→후(px) |
|---|---:|---:|
| `overlay-a-d30.png` | 42.48 | 273→273 |
| `overlay-a-p0.png` | 38.31 | 273→273 |
| `overlay-c-fog-d30.png` | 19.91 | 219→219 |
| `overlay-c-fog-p0.png` | 12.00 | 219→219 |
| `overlay-c-ice-d30.png` | 48.42 | 228→228 |
| `overlay-c-construction-d30.png` | 24.44 | 202→202 |
| `overlay-e-none-p.png` | 8.44 | 534→534 |
| `overlay-e-none-d.png` | 52.45 | 390→390 |
| `overlay-e-child-pet-p.png` | 8.47 | 600→600 |
| `overlay-e-child-pet-d.png` | 52.45 | 390→390 |

- 동행·설정 캡처: 이름이 보이는 화면/카드가 있어 **6장 모두** 갱신했다. `t36-companion-first.png`, `t36-companion-preselected.png`, `t36-companion-none.png`, `t36-companion-drive-cancel.png`, `t36-settings-travel-on.png`, `t36-settings-travel-off.png`. 첫 선택의 미선택·동행 없음 단독 선택·지난 아이+반려동물 미리 선택, 여행 모드 켜짐/꺼짐, D 전환 시 선택 화면 취소를 UI XML과 눈으로 확인했다.
- 기획서 그림: `docs/demo/proposal/prototype-{signal,fog,companions,pet,weather,parking,ice-synthetic}.png` **7장을 모두 다시 잘랐다**. T-43 표와 같은 원본·좌표이며 원본 영역과 픽셀 동일함을 검사했다. `prototype-companions.png`는 이름이 없는 선택 영역이라 재생성 결과가 이전 파일과 같고, 나머지 6장은 새 이름을 반영했다. T-45의 `concept-navigation.png`와 다른 옛 기록 캡처는 건드리지 않았다. 모아보기는 `.gradle/t44-final-sheet.png`, 창·본문 동일성 근거는 `.gradle/t44-frames.json`·`t44-body-equality.json`이다.
- 안전 확인: 모든 앱 실행은 **debug / `logOnlySpeech=true`**였다. 시험 앱 프로세스 **11개**의 로그에서 **TTS onStart 0건**, **FATAL EXCEPTION 0건**, 전체 로그에서 실제 키·인코딩 변형 **0개**를 값 출력 없이 검사했다(`.gradle/t44-log-counts.json`). 실제 TTS·release 빌드·push는 실행하지 않았다.
- 기기 사용·정리: 사용자 허락을 받은 Orca ask 뒤 release를 제거하고 debug를 user 10에 설치했다. 허락대로 release의 앱 설정은 보존하지 않았다. 기어·속도·앱 조작은 각각 별도 명령으로 순서대로 실행했고 에뮬레이터를 재시작하지 않았다. 기존 내비를 HOME→내비 아이콘으로 띄웠으며 GPS·내비 경로·오디오는 바꾸지 않았다.
- **에뮬레이터 반납 완료:** HANDOFF·커밋 전에 status `msg_0767ca5acc8d`를 보냈다. debug 설치 유지, 앱 프로세스·서비스·카드 창 없음, overlay appops **default**, **P/0·페달/조향 0·콘솔 비활성**을 확인했다. debug 설정은 **LIVE·APPROACH·고지 확인·여행 모드 꺼짐·동행 키 없음**이며 시험용 위치·차량 속도 런타임 권한은 부여된 상태다. 오디오 음량·음소거 관련 **108개 값 전후 동일**을 확인했다(`.gradle/t44-restored.json`). 반납 뒤 adb·가상 콘솔은 사용하지 않는다.
- 남은 확인·요청: Claude가 이름 참조와 캡처를 리뷰한 뒤 T-46에서 release APK 재생성·설치와 기획서 DOCX 재생성을 진행해 달라. API 34 에뮬레이터에서 확인했으며 다른 기기·글자 배율은 시험하지 않았다. 커밋 메시지는 `[T-44] 앱 이름을 내비서로`다.


## 2026-11-02 17:05 · Claude · T-45 리뷰 완료

- 확인한 것: `13f2693`의 `video/concept.html`·`check.mjs`·`CUES.md`와 프레임 `video/frames/04-outro.png`(이름 "내비서", 새 한 줄 소개가 두 줄로 고르게 나뉨), `02-scene-e1-question.png`(카드 머리줄 "내비서 · 여행")를 직접 봤다.
- 다시 돌린 검증: `node video/render.mjs --check` 통과(rc 0, "겹침/넘침 0, 외부 요청 0건"). `video/concept.html`·`check.mjs`·`CUES.md`에서 "앞길 안내" 0건.
- 결과: T-45 `완료`. 기획서 DOCX 재생성은 T-44의 시제품 그림이 끝난 뒤 T-46에서 한다.

## 2026-11-02 15:57 · Codex · T-45 콘셉트 영상 이름·소개 변경

- 한 일: `video/concept.html`의 문서 제목·aria-label·신호/도로 위험/여행/주차 카드 머리줄·마무리 이름·자막을 **내비서**로 바꿨다. `introLine`은 전달받은 문장을 한 글자도 바꾸지 않고 썼다. `check.mjs`·`CUES.md`와 기준 해시 `abc-reference.json`을 갱신하고 프레임 11장(기존 도입 2장은 픽셀 동일)을 다시 생성했다.
- GPT 디자인 판단: 이름이 짧아져도 카드 머리줄의 왼쪽 정렬·크기·색과 카드 치수는 유지하는 편이 읽기 좋다. 마무리 앱 이름도 **104px**, 소개도 **28px/행간 1.7**을 유지했다. 첫 쉼표 뒤 강제 개행은 새 소개를 **3줄**로 만들므로 없애고 `text-wrap: balance`를 적용했다. 실제 경계는 **“여행 정보까지 / 묻기 전에…”**, 두 줄 폭은 **1177.125 / 1171.625px**이며 의미와 길이가 함께 맞는다. 이름과 소개 사이 28px, 전체 묶음의 중앙 정렬도 유지했다.
- 검증: `node video/render.mjs --check` **성공**. 자막 원문·시각 **18개**, 배치 표본 **32개**, 새 기준 **9프레임** 일치, 넘침·겹침·페이지 오류·외부 HTTP(S) 요청 **0건**이다. 문서 제목·접근성 이름·카드 종류별 머리줄·마무리 화면 안 배치 검사도 보강했다. 이름 변경 전 기대값 검사는 실제로 실패한 뒤 구현 후 통과했다. `video/`의 이전 앱 이름 검색 **0건**(지난 보고서·T-38 기록 제외).
- 기준 갱신 근거: 이전 기준(532e562) 9프레임을 먼저 재현했다. A·B·C **8장 모두 새 화면의 머리줄 이름만 이전 값으로 되돌렸을 때 이전 SHA-256과 완전히 같음**을 assert했다. 따라서 이름 영역 밖 픽셀·카드 크기·색·위치는 같다. 감사값은 `.gradle/t45/audit.json`이다.
- 세 영상 재렌더: `video/out/01-intro.webm` **15.00초 / 6,661,080 bytes**, `02-concept.webm` **85.00초 / 32,960,169 bytes**, `04-outro.webm` **5.00초 / 3,004,455 bytes**. 모두 **1920×1080·30fps·무음**이며 합계 105초다. `out/metadata.json`·`out/validation.json`을 갱신했다. 영상·검사 JSON은 기존 규칙대로 git 밖에 둔다.
- 렌더 환경: npm 캐시의 Playwright 1.65는 설치되지 않은 FFmpeg 1013과 VP9 옵션을 요구해 실패했다. 새 설치·다운로드 없이, 이미 설치된 Python Playwright의 Node 모듈 **1.61.1-beta-1782139630000**과 Chromium **149.0.7827.55**·FFmpeg **1011**을 사용했다. `.gradle/t45/modules/playwright-core`는 `C:/Users/user/AppData/Local/Programs/Python/Python312/Lib/site-packages/playwright/driver/package`를 가리키는 링크다. 다시 녹화할 때 `$env:NODE_PATH='C:\dev\hmg-app\.gradle\t45\modules'; node video/render.mjs`로 선택한다. `--check`는 환경 지정 없이도 통과한다. 렌더러 소스와 설치된 패키지는 수정하지 않았다.
- 눈으로 확인한 프레임: `video/frames/02-scene-a.png`, `02-scene-c.png`, `02-scene-e1-question.png`, `02-scene-e1-answer.png`, `02-scene-e2.png`, `02-scene-d.png`, `04-outro.png`. 전체 카드 모아보기는 `.gradle/t45/cards.png`다. 최종 WebM의 본편 70초·마무리 3초도 다시 추출해 `.gradle/t45/encoded-concept.png`·`encoded-outro.png`에서 새 이름·소개·줄바꿈을 확인했다.
- 기획서 목업: 새 `video/frames/02-scene-a.png`의 **(0,0)→(1920,950)**을 `docs/demo/proposal/concept-navigation.png`로 잘랐다. **1920×950 / 72,627 bytes**, 원본 영역과 픽셀 동일, 메타데이터 없음 검사 통과.
- 남은 확인·요청: Claude가 마무리 두 줄과 갱신 목업을 리뷰한 뒤 기획서 DOCX를 다시 만들면 된다. 에뮬레이터·APK·앱 코드·다른 캡처·문서와 push는 사용하지 않았다. 영상의 실제 음성·실동작 합성 편집은 기존대로 사용자 몫이다. 커밋 메시지는 `[T-45] 콘셉트 영상 이름을 내비서로`다.


## 2026-11-02 16:05 · Claude · T-42 기획서 작성, T-43 리뷰 완료

- T-42 기획서(접수양식 DOCX)
  - `tools/make_proposal.py`가 `docs/PROPOSAL.md` 3절을 양식 02의 1~7번 칸에 채운다. 칸마다 있는 입력 상자의 태그(`app_summary` 등)는 그대로 두고, 글 전용 상자를 여러 문단·표·그림이 들어가는 상자로 바꿨다. 칸이 한 쪽보다 길어지므로 칸 줄의 `cantSplit`만 뺐다.
  - 01은 개인정보가 아닌 칸만 채웠다: 개인, 대학(원)생, 앱명, 참가 인원 1명. 성명·소속·연락처·역할·작성일·구성원 표, 04 서명란, 8번 숏폼은 비웠다. 7번의 시연 영상 칸은 노란색 `【제출 전 기입】`이다.
  - 결과는 git 밖 `docs/forms/앞길안내_기획서.docx`(337,902 bytes), 원본 양식은 `docs/forms/접수양식_원본.docx`. 양식 검증 통과, `--self-test` 통과, LibreOffice 18쪽.
  - 다시 만들기: `py -3.12 -I tools/make_proposal.py docs/forms/접수양식_원본.docx docs/forms/앞길안내_기획서.docx`. 사용자가 개인정보를 넣은 뒤에는 다시 만들면 덮어쓰므로, 글을 고칠 일이 있으면 개인정보를 넣기 전에 한다.
- MAU·재사용률 근거 보강(사용자 요청, 273820f)
  - 1년 차 탑재 차량: 먼저 들어간 그랜저·아반떼·투싼의 2025년 국내 판매 합 약 20만 대. 3년 차 누적: 현대차·기아 국내 판매 약 126만 대의 50%(2년 차)·80%(3년 차) 가정으로 약 180만 대.
  - 월 사용률 60%: 티맵 MAU 1,562만 / 승용차 등록 2,191만 = 약 70%보다 조금 낮게. 설치율 10%→15%는 공개 자료가 없어 낮게 잡은 가정이다.
  - 결과: MAU 1년 차 약 1만 2천(이전 3만), 3년 차 약 16만(이전 18만), 내비 기본 기능이면 약 126만.
  - 30일 재사용률 목표 25%(이전 40%): Adjust 2022 전 세계 중앙값 6%, 여행 앱 4%의 약 4배.
  - 출처는 PROPOSAL 4절과 기획서 7번 칸 근거 출처에 있다. 사용자 확인 대기.
- T-43 리뷰(f49f57b)
  - 그림 8장: 3번 칸 콘셉트 목업 1장, 4번 칸 신호·안개·동행 선택, 7번 칸 반려동물·날씨·주차·살얼음(합성). 캡션에서 목업과 실제 화면을 구분했다. 렌더 쪽을 직접 봤고 잘림·겹침이 없다.
  - 모양 값 3곳(표 9.5pt, 소제목 간격, 그림 여백)만 바꿨다. 받아들인다.
- 남은 것: Word에서 쪽 나눔 확인(LibreOffice는 제목과 칸을 함께 다음 쪽으로 넘겨 빈칸이 생긴다), 사용자의 개인정보·서명·영상 링크, 제출 저장소 다시 내보내기(push는 사용자 요청 때).

## 2026-11-02 15:20 · Codex · T-43 기획서 그림·모양 검토

- 한 일·결과: `docs/PROPOSAL.md` 3절에 **그림 8줄만 추가**하고, 기존 문장·숫자·링크는 모두 보존했다. 목업 **1장**과 시제품 실제 화면 **7장**을 골라 `docs/demo/proposal/`에 무손실 PNG 크롭으로 저장했다. 결과는 **`C:/dev/hmg-app/docs/forms/앞길안내_기획서.docx`**, **336,604 bytes(약 0.34MB, 10MB 이하)**다. DOCX는 `docs/forms/`의 git 제외 파일이며 커밋하지 않는다.
- **GPT 디자인 판단:** 3번 칸에서는 회전 안내·경로와 카드가 함께 보이는 콘셉트 목업으로 제안의 전체 모습을 먼저 보여 준다. 4번 칸은 실제 신호·안개 카드를 나란히 놓고, 동행 선택은 작은 글씨와 선택 상태까지 읽히도록 한 칸 폭 전체로 쓴다. 7번 참고자료에는 동행/날씨 추천을 한 쌍, 주차/살얼음을 한 쌍으로 놓아 작동 근거와 정보의 차이를 비교하게 했다. 자동차·공조·상태 표시줄보다 질문과 안내 내용이 크게 보이도록 실제 화면은 필요한 부분만 잘랐다.

| 넣은 칸·순서 | 그림 파일(`docs/demo/proposal/`) | 원본·크롭(px, 좌상단→우하단) | 고른 이유 |
|---|---|---|---|
| 3. 제안하는 모습, 단독 | `concept-navigation.png` | `video/frames/02-scene-a.png` · (0,0)→(1920,950) | 회전 안내·경로·신호 카드를 함께 보존하고 영상의 중복 자막 띠만 제외 |
| 4. 시나리오 ③ 뒤, 왼쪽 | `prototype-signal.png` | `docs/demo/overlay-a-p0.png` · (1824,920)→(2384,1260) | 실제 좌회전 대기 35초를 크게 표시 |
| 같은 줄, 오른쪽 | `prototype-fog.png` | `docs/demo/overlay-c-fog-d30.png` · 같은 크롭 | 기록 재생한 안개 안내와 행동 문장을 비교 |
| 4. 가족 나들이 뒤, 단독 | `prototype-companions.png` | `docs/demo/t36-companion-preselected.png` · (1134,280)→(2254,970) | 아이·반려동물 선택, 지난 답 기억, 동행 없음, 완료 버튼을 함께 보여 줌 |
| 7. 참고자료 첫 쌍, 왼쪽 | `prototype-pet.png` | `docs/demo/overlay-e-child-pet-p.png` · (1824,660)→(2384,1260) | 일부 구역 동반·목줄 조건을 확인할 수 있음 |
| 첫 쌍, 오른쪽 | `prototype-weather.png` | `docs/demo/overlay-e-none-p.png` · 같은 크롭 | 도착 때 비 예보·실내 명소 우선 문구를 확인할 수 있음 |
| 둘째 쌍, 왼쪽 | `prototype-parking.png` | `docs/demo/overlay-e-none-d.png` · (1824,830)→(2384,1260) | 빈자리 849면·기본요금 표시 |
| 둘째 쌍, 오른쪽 | `prototype-ice-synthetic.png` | `docs/demo/overlay-c-ice-d30.png` · 같은 크롭 | 살얼음 시나리오도 보이되 **합성 자료**임을 구분 |

- 캡션·개인정보: 목업은 **`콘셉트 목업`**, 모든 실제 그림은 **`시제품 실제 화면(에뮬레이터)`**로 명시했다. 살얼음 캡션에 **`살얼음(합성)`**을 넣었고, 본문의 기록 재생·합성 자료 설명도 유지했다. 고른 영역에는 참가자 이름·계정·연락처·차량 번호·상태 표시줄 사용자 아이콘이 없다. 화면 속 이름은 공공 관광지·주차장 이름이다. 원본 PNG는 수정하지 않았으며, 최종 PNG가 원본의 해당 영역과 픽셀 단위 동일하고 메타데이터/EXIF가 없음을 검사했다. 8장 합계 **290,943 bytes**다.
- 모양 변경·이유: `tools/make_proposal.py`는 **모양 값 3곳만** 바꿨다. 표·캡션은 9→**9.5pt**로 키워 읽기 쉽게 했고, 소제목 앞/뒤 간격은 7/2→**8/3pt**로 늘렸다. 그림 폭은 셀 폭에서 10pt를 빼던 것을 **14pt**로 바꿔 셀 여백 안에 여유 있게 들어가게 했다. 기존 진한 남색 소제목·표 머리색·테두리·열 폭은 유지했다. 본문을 10.5pt로 시험했더니 배경 설명 끝 두 항목만 다음 쪽에 남아 **본문 10pt로 유지**했다. 최종 캡션은 짧게 정리해 신호·안개·주차·살얼음 캡션을 한 줄에 읽게 했다.
- 검증: `py -3.12 -I tools/make_proposal.py --self-test` → **`self-test ok`**. 지시한 `.../docx/scripts/office/validate.py docs/forms/앞길안내_기획서.docx --original docs/forms/접수양식_원본.docx` → **`All validations PASSED!`**(문단 137→391). 그림을 제외한 `PROPOSAL.md`의 비어 있지 않은 줄이 HEAD와 모두 같음을 assert했다. 텍스트 전용 T-42 출력과 최종 출력의 7개 답변 내용만 비운 뒤 XML을 정규화해 비교했으며 **답변 칸 밖 XML 완전 동일**이다. 머리말·꼬리말·안내문·동의서와 다른 DOCX 부품도 그대로다. 추가 그림 8개가 모두 페이지 범위 안에 놓임을 검사했다.
- 렌더·눈 확인: LibreOffice PDF는 **18쪽**이다(원래 글만 있는 출력 16쪽). 전체 18쪽을 PNG로 렌더해 모아보기 이미지를 확인했고, 주요 그림·표 페이지는 확대해서 봤다. 그림·캡션·표에 잘림이나 겹침이 없다. 지시한 LibreOffice의 keepNext/한 칸 표 때문에 생기는 쪽 아래 빈 공간은 양식 구조를 유지해 그대로 뒀다.
  - 결과 PDF: `C:/dev/hmg-app/.gradle/t43-final/앞길안내_기획서.pdf`
  - 주요 기능·목업: `C:/dev/hmg-app/.gradle/t43-final/page-05.png`
  - 실제 신호·안개: `C:/dev/hmg-app/.gradle/t43-final/page-07.png`
  - 동행 선택: `C:/dev/hmg-app/.gradle/t43-final/page-08.png`
  - 추천·주차·살얼음: `C:/dev/hmg-app/.gradle/t43-final/page-14.png`
  - 전체 보기·검사값: `.gradle/t43-final/contact-sheet.png`, `.gradle/t43-final/verification.json`
- 남은 확인·요청: Claude가 그림 선정과 최종 DOCX를 리뷰해 달라. **Word에서의 최종 쪽 나눔은 미확인**이며 18쪽은 LibreOffice 기준이다. 사용자가 01·03·04의 개인정보·동의·서명, 영상 파일명/링크와 선택적 숏폼을 마무리해야 한다. 에뮬레이터·push는 사용하지 않았다. 커밋 메시지: `[T-43] 기획서 그림과 모양`.


## 2026-11-02 14:45 · Claude · T-41 리뷰 완료, release APK 다시 빌드

- 리뷰
  - 혼합안 채택에 동의한다. 기본은 줄 길이 균형(`BREAK_STRATEGY_BALANCED`)이고, 여러 줄일 때만 문장 경계 줄바꿈을 시도한다. 줄 수와 높이가 같고 말줄임이 없을 때만 받아들인다.
  - `onMeasure` 안에서 글을 바꾸지만 수렴한다. 받아들이면 다음 측정에서 이미 `\n`이 있어 그대로 두고, 되돌리면 같은 판단을 반복할 뿐이며 줄 수가 늘지 않는다. 되돌리는 경로는 지금 시연 글에서는 타지 않는다. 문제가 보이면 측정 전에 `StaticLayout`으로 미리 고르는 방식으로 옮긴다.
  - 음성 문장은 core에서 오므로 바뀌지 않는다. 카드 표시만 바뀐다.
- 검증
  - `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → 앱 29, core 167, 실패·skip 0.
  - 캡처: 안개 "앞이 500m도 안 보여요. / 속도를 줄이세요.", 살얼음 "2.6km 앞 살얼음 / 주의 구간이에요." 둘 다 카드 높이는 같다.
- release APK (5d82418)
  - `apk/SUDL_앞길안내_release.apk` 41,981,810 bytes, SHA-256 `d4cc1a4915a6990e25eebaf60b5471dcdf77fbedb8155b18d6aaba1e366e98f3`.
  - 서명 CN=SUDL, 인증서 SHA-256 `2d24f48b…`(같음). `DEBUG=false`, 키 4개 있음, 키스토어 비밀번호 없음.
  - 에뮬레이터: debug를 지우고 release 설치 → 위치 허용 → 오버레이 허용(appops) → 고지 확인. `TTS available=true`, 서비스 실행, FATAL 0. release를 설치한 채로 둔다(실시간, 오버레이 허용).
- 남은 일: 제출 저장소 다시 내보내기. push는 사용자가 요청할 때만 한다.

## 2026-11-02 14:23 · Codex · T-41 카드 줄 길이 맞추기(GPT 판단)

- 한 일·선택: **A(균형 줄바꿈)+B(넘칠 때 문장 경계 나누기)의 혼합안**을 골랐다. `text`·`flowText`가 같은 `cardTextView()`를 사용한다. 기본은 `BREAK_STRATEGY_BALANCED`이며, 실측 결과 여러 줄이고 문장 경계가 있을 때만 표시용 줄바꿈을 시도한다. **줄 수·높이가 그대로이고 말줄임이 없을 때만 채택**하고, 아니면 원문을 유지한다. 한 줄에 들어가는 글과 기존 명시적 개행은 유지한다. 크기·색·폭·위치·표시 어휘·core와 음성 문장은 바꾸지 않았다.
- 근거: [Android Layout.BREAK_STRATEGY_BALANCED](https://developer.android.com/reference/android/text/Layout#BREAK_STRATEGY_BALANCED)는 API 23부터 줄 길이가 비슷해지도록 경계를 고른다. minSdk 28에서 사용할 수 있다. T-39의 한국어 locale+PHRASE(API 33 이상)는 그대로 유지했다. 문자열을 따로 하드코딩하거나 카드 종류별 폭을 추정하지 않고 실제 TextView 측정 단계에서 처리한다.
- 비교 방법: API 34/160dpi 실제 기기의 임시 instrumentation으로 **글 상자 17사례·카드 전체 8종**을 측정했다. `text`·`flowText`의 실제 생성 함수를 호출하고 368dp/480dp, 실제 20/26/30/34/96sp, 주차 중간 굵기까지 맞췄다. A는 BALANCED, B는 한 줄 폭을 넘을 때만 `. `를 문장 경계로 나눔, 혼합안은 두 가지를 함께 쓴다. 아래 `/`는 화면의 줄 경계, 괄호는 첫째/둘째 줄 실측 폭(px)이다.

| 글 | T-39 | A: 균형만 | B: 문장만 | 선택한 혼합안 |
|---|---|---|---|---|
| 안개 본문 26sp/368dp | `앞이 500m도 안 보여요. 속도를 / 줄이세요.` (327/95) | `앞이 500m도 안 / 보여요. 속도를 줄이세요.` (176/246) | `앞이 500m도 안 보여요. / 속도를 줄이세요.` (249/167) | **B와 동일**, 높이 63px 그대로 |
| 살얼음 제목 34sp/368dp | `2.6km 앞 살얼음 주의 / 구간이에요.` (301/154) | `2.6km 앞 살얼음 / 주의 구간이에요.` (235/220) | T-39와 동일 | **A와 동일**, 높이 82px 그대로 |
| 긴 이름 2줄 시험(정독도서관 이름을 두 번 쓴 합성 입력), 26sp/480dp | 첫 줄 480 / 둘째 줄 110, 끝줄 한 어절 | 같은 이름 단위 두 줄(298/292) | T-39와 동일 | A와 동일, 높이 76px 그대로 |

- **GPT 디자인 판단:** A만 쓰면 안개에서 부정 표현 `안 보여요`가 갈린다. B만 쓰면 한 문장인 살얼음 제목의 짧은 끝줄을 해결하지 못한다. 혼합안은 안개에서 **상태 설명과 행동 요청을 한 문장씩** 읽게 하고, 살얼음에서는 **거리·위험 종류 / 주의 구간**으로 나눈다. 안개의 두 줄 폭 차이는 232→82px, 살얼음은 147→15px로 줄었다. 줄 길이만 기계적으로 같게 하는 것보다 의미를 보존하는 쪽을 택했다.
- 나머지 측정: 공사는 이미 제목 `2.5km 앞 공사 구간이에요.`와 설명 `4차로 차단.`이 별도 글 상자라 둘 다 한 줄 그대로다. E의 긴 실제 이름·이유·날씨 줄은 한 줄을 유지한다. 주차 보조 줄의 기존 `거리\n요금`은 2줄/88px 그대로다. 전체 카드 높이도 **안개 219, 살얼음 228, 공사 202, 날씨 여행 534, 동행 여행 600, 빈자리 있음 390, 빈자리 모름 230, 긴 요금 개행 434px**로 모든 후보와 최종 구현에서 같았다.
- 높이 안전장치: 짧은 두 문장이 한 줄(263px/480dp)에 들어가면 원문·29px 높이 그대로다. 세 문장을 각각 나누면 2→3줄이 되는 시험은 새 개행을 버리고 원문·2줄/58px을 유지했다. 실제 최종 구현의 17개 글 상자 결과가 선택한 후보의 줄 경계와 일치하며, 원래 줄 수·높이도 유지됨을 assert했다.
- 테스트: `CardLineBreakTest`의 `sentence lines preserve wording and existing line breaks`를 먼저 써 미구현 컴파일 실패를 확인했다. 원문 복원 가능성, 소수점, 기존 주차 개행과 여러 문장 입력을 검사한다. 문제 두 글 상자의 기기 검사도 수정 전 한 어절 끝줄 **2건** 실패→수정 후 **0건** 통과했다. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` **성공**, 앱 **29개**·core **167개** = **196개**, 실패·오류·skip **0개**다. 임시 instrumentation APK는 제거했다.
- 캡처: 아래 10장을 **같은 파일명, 같은 시연 구간과 표시값**으로 다시 찍었다. 모두 2560×1440이며 **내비 화면 위**에 있다. `WindowManager` 프레임을 T-39 기록과 비교해 **10장 모두 x/y/너비/높이 완전 동일**함을 assert했다. 자동 줄바꿈이나 새 표시 개행의 전후는 다음과 같다(별도 제목·설명·목록 행은 자동 개행으로 세지 않는다).

| 캡처 | 전(T-39) | 후(T-41) | 카드 높이 전→후 |
|---|---|---|---|
| [A D/30](demo/overlay-a-d30.png) | 자동 개행 없음, 30초 | 동일 | 273→273 |
| [A P/0](demo/overlay-a-p0.png) | 자동 개행 없음, 35초 | 동일 | 273→273 |
| [C 안개 D/30](demo/overlay-c-fog-d30.png) | `앞이 500m도 안 보여요. 속도를 / 줄이세요.` | `앞이 500m도 안 보여요. / 속도를 줄이세요.` | 219→219 |
| [C 안개 P/0](demo/overlay-c-fog-p0.png) | `앞이 500m도 안 보여요. 속도를 / 줄이세요.` | `앞이 500m도 안 보여요. / 속도를 줄이세요.` | 219→219 |
| [C 살얼음 D/30](demo/overlay-c-ice-d30.png) | `2.6km 앞 살얼음 주의 / 구간이에요.` | `2.6km 앞 살얼음 / 주의 구간이에요.` | 228→228 |
| [C 공사 D/30](demo/overlay-c-construction-d30.png) | 자동 개행 없음 | 동일 | 202→202 |
| [E 동행 없음 P](demo/overlay-e-none-p.png) | 자동 개행 없음 | 동일, 긴 이름·날씨 포함 | 534→534 |
| [E 동행 없음 D](demo/overlay-e-none-d.png) | 자동 개행 없음, 빈자리 849면 | 동일 | 390→390 |
| [E 아이+반려동물 P](demo/overlay-e-child-pet-p.png) | 자동 개행 없음 | 동일, 이유 포함 | 600→600 |
| [E 아이+반려동물 D](demo/overlay-e-child-pet-d.png) | 자동 개행 없음, 빈자리 849면 | 동일 | 390→390 |

- 최종 눈 확인: 캡처 10장의 **단어 중간 줄바꿈 0→0건**, **한 어절만 남는 끝줄 3→0건**이다(안개 두 장+살얼음 한 장). 말줄임·잘림도 없다. 모든 시작은 debug/`logOnlySpeech=true`였으며 시험 앱 프로세스 7개의 로그에서 `TTS onStart` **0건**, 앱 `FATAL EXCEPTION` **0건**이다. 전체 로그의 실제 키·URL 인코딩 변형 **0개**를 값 출력 없이 확인했다.
- 기기 사용·복구: 시작 전 Orca ask의 사용자 확인을 받고 release를 제거해 debug로 교체했다. 답변대로 release 설정은 보존하지 않았다. 마지막 E 캡처 때 내비가 전면에 오지 않아 다시 Orca ask로 확인했고, 안내받은 **HOME→내비 실행**으로 복구했다. 내비 앱 강제 종료나 에뮬레이터 재시작은 하지 않았다. 내비 소리는 조작하지 않았으며 복구 전후 오디오 음량·음소거 **120개 값 동일**을 확인했다.
- 반납: HANDOFF·커밋 전에 **에뮬레이터 반납 status**를 보냈다. debug 설치 유지, 앱 프로세스·서비스·카드 창 없음, 오버레이 appops **default**, **P/0**, 페달·조향 **0**, 콘솔 비활성으로 확인했다. debug 시험 설정은 **LIVE·여행 모드 꺼짐·동행 키 없음·APPROACH·고지 확인**이고 시험용 위치·속도 런타임 권한은 부여된 상태다. GPS·내비 경로·소리는 바꾸지 않았다. 반납 이후 adb·가상 콘솔은 사용하지 않는다.
- 남은 확인·요청: 실기는 API 34에서 확인했고 다른 API/글자 배율은 시험하지 않았다. Claude가 혼합안과 높이 복귀 조건을 리뷰한 뒤 release를 다시 빌드·설치해 달라. release 빌드와 push는 실행하지 않았다. 커밋 메시지: `[T-41] 카드 여러 줄 글의 줄 길이 맞추기`.


## 2026-11-02 13:25 · Claude · T-39 리뷰 완료, T-40 README·release APK

- T-39 리뷰
  - 코드: `OverlayCard`의 두 글 상자가 공용 `applyKoreanWordBreak()`를 부른다. API 33 이상에서만 한국어 locale과 PHRASE를 함께 켠다. 12줄이고 다른 변경은 없다.
  - 검증: `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → 앱 28, core 167, 실패·skip 0.
  - 캡처: 살얼음 "2.6km 앞 살얼음 주의 / 구간이에요.", 안개 "… 속도를 / 줄이세요." 단어 중간 줄바꿈이 없다.
  - GPT 제안(한 어절만 남는 끝줄을 의미 경계에서 나누기)은 제출 뒤 후속으로 둔다. 후보는 `breakStrategy = BREAK_STRATEGY_BALANCED`로 줄 길이를 맞추는 방법이다. 보이는 모양이 바뀌므로 하게 되면 GPT와 다시 상의한다.
- T-40 README (8e10162, 8ed58c8)
  - 여행 모드(동행 묻기, 명소 고르는 법, 날씨, 주차 빈자리), 시연 E의 새 흐름, 데이터 범위, `서울` 키와 `ROAD_AHEAD_SEOUL_KEY`, 서울 평문 허용, 새 캡처를 넣었다.
  - 첫 실행 순서를 실제와 맞췄다: 위치 → 다른 앱 위에 표시 → 고지 확인. 고지는 서비스가 돌아 P를 받은 뒤에 뜨므로 오버레이 권한이 먼저다.
- T-40 release APK (8ed58c8)
  - `:app:assembleRelease` → `apk/SUDL_앞길안내_release.apk` 41,981,810 bytes. SHA-256 `95c11eb5c7e050202ece93cebaae61e248d937fffbd146f843161071c61e5cb6`, 빌드 결과와 같다.
  - `apksigner verify`: Verifies, v2, signer 1, CN=SUDL. 인증서 SHA-256 `2d24f48b…`는 T-20과 같다.
  - BuildConfig: `DEBUG=false`, 키 4개(신호등·재난·기상·서울)가 모두 비어 있지 않다(값은 출력하지 않고 assert). 매니페스트에 `networkSecurityConfig`가 있다. 키스토어 비밀번호는 APK에 없다.
  - 설치: 서명이 달라 debug 앱을 지우고 `install -r --user 10` → Success. 위치 허용 → 오버레이 허용(appops) → 고지 확인 뒤 `TTS available=true`, P, 서비스 실행, FATAL 0.
  - 에뮬레이터에는 release를 설치한 채로 둔다(실시간, 오버레이 허용, 고지 확인). debug가 필요하면 지우고 다시 설치해야 한다.
- 소리 시험(사용자 요청)
  - 사용자가 스피커 음량을 올린 뒤 debug 앱을 `logOnlySpeech` 없이 실행해 시연 A 음성을 재생했다. 사용자 청음 결과는 "잘 나온다"다. 10-28의 "들리지만 음량 작음"을 이 결과로 바꾼다.
- 에뮬레이터 기록 파일
  - 마이크가 없는 PC라 에뮬레이터가 `virtio-snd-mic0`·`DirectSoundCapture` 오류를 쉬지 않고 출력했다. Claude가 받던 기록 파일이 11GB(약 2억 줄)가 됐고, 에뮬레이터는 종료 중에 멈췄다(둘의 인과는 확실하지 않다).
  - 강제 종료 후 cold boot했다. 이때 최근 쓰기(앱 설정, appops) 일부가 사라졌다. T-39 캡처는 다시 켠 뒤 처음부터 했다.
  - 다시 켤 때는 두 줄을 걸러 낸다: `emulator.exe ... 2>&1 | grep --line-buffered -v -e DirectSoundCapture -e virtio-snd-mic0 > 기록파일`.
- 남은 일
  - 제출 저장소 내보내기(`tools/export-submission.sh`). push는 사용자가 요청할 때만 한다.
  - 사용자 몫: MAU·재사용률 가정, 실제 동작 녹화와 음성 녹음, 영상 편집.

## 2026-11-02 13:08 · Codex · T-39 카드 한글 어절 줄바꿈

- 한 일: `OverlayCard.kt`의 `text`·`flowText`가 공용 `applyKoreanWordBreak()`를 호출한다. **API 33 이상에서만** `textLocale = Locale.KOREAN`과 `LINE_BREAK_WORD_STYLE_PHRASE`를 함께 설정한다. API 33 미만은 기존 경로 그대로다. 글자 크기·색·폭·위치·문구·줄 수 제한은 바꾸지 않았다. 기존 동행 선택·설정 캡처에는 어절 중간 줄바꿈이 없어 Compose는 수정하지 않았다.
- 공식 근거: [TextView.setLineBreakWordStyle](https://developer.android.com/reference/android/widget/TextView#setLineBreakWordStyle(int))는 API 33에 추가됐다. [LineBreakConfig.PHRASE](https://developer.android.com/reference/android/graphics/text/LineBreakConfig#LINE_BREAK_WORD_STYLE_PHRASE)는 의미 있는 단위에서 줄을 나누며 **locale에 따라 지원 여부가 달라진다**. [TextView.setTextLocales](https://developer.android.com/reference/android/widget/TextView#setTextLocales(android.os.LocaleList))는 locale이 줄바꿈에도 영향을 준다고 설명한다. 기기 전체 언어를 바꾸는 대신 한국어 카드의 TextView에 locale을 지정했다.
- 기기 재현·수정 검증: API **34**, `am get-config`의 **ko-rKR**, 160dpi에서 임시 Android instrumentation으로 실제 두 생성 함수의 TextView를 368px/34sp로 측정했다. 수정 전 둘 다 `2.6km 앞 살얼음 주의 구간 / 이에요.`로 어절 경계 검사가 실패했고, 수정 후 둘 다 `2.6km 앞 살얼음 주의 / 구간이에요.`로 통과했다. 각각의 비교 4조합은 `en_US+NONE` 실패, **`en_US+PHRASE`도 실패**, `ko+NONE` 실패, **`ko+PHRASE` 통과**였다. 따라서 한국어 locale 지정이 필요하다. 임시 테스트는 `.gradle/`에서만 만들고 테스트 APK는 제거했다.
- 빌드·테스트: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` **성공**. 앱 **28개**·core **167개**, 합계 **195개**, 실패·오류·skip **0개**다. `:app` 컴파일 정상이며 release 빌드는 Claude의 다음 작업으로 남긴다.
- 캡처 결과: 아래 **10장을 같은 파일명으로 갱신**했고 모두 2560×1440이다. 실제 P/0·D/30을 검사하고 A의 **35초/30초**, C의 **안개 2.6km/2.5km·공사 2.5km·살얼음 2.6km**도 기존 캡처와 맞췄다. E는 같은 재생의 P 목록/도착 전 D 주차 구간이다. **단어 중간 줄바꿈 0건**, 말줄임·잘림 0건을 직접 확인했다. 아래 `/`는 같은 글 상자 안의 자동 줄바꿈이며 별도 제목·설명·목록 행은 자동 개행으로 세지 않는다.

| 캡처 | 전 | 후 |
|---|---|---|
| [A D/30](demo/overlay-a-d30.png) | 자동 개행 없음, 30초 | 동일 |
| [A P/0](demo/overlay-a-p0.png) | 자동 개행 없음, 35초 | 동일 |
| [C 안개 D/30](demo/overlay-c-fog-d30.png) | `앞이 500m도 안 보여요. 속도를 줄` / `이세요.` | `앞이 500m도 안 보여요. 속도를` / `줄이세요.` |
| [C 안개 P/0](demo/overlay-c-fog-p0.png) | `앞이 500m도 안 보여요. 속도를 줄` / `이세요.` | `앞이 500m도 안 보여요. 속도를` / `줄이세요.` |
| [C 살얼음 D/30](demo/overlay-c-ice-d30.png) | `2.6km 앞 살얼음 주의 구간` / `이에요.` | `2.6km 앞 살얼음 주의` / `구간이에요.` |
| [C 공사 D/30](demo/overlay-c-construction-d30.png) | 자동 개행 없음 (`2.5km 앞 공사 구간이에요.`, `4차로 차단.`) | 동일 |
| [E 동행 없음 P](demo/overlay-e-none-p.png) | 자동 개행 없음 | 동일, 긴 이름·날씨 줄 포함 모두 온전히 표시 |
| [E 동행 없음 D](demo/overlay-e-none-d.png) | 자동 개행 없음 (`빈자리 849면`) | 동일 |
| [E 아이+반려동물 P](demo/overlay-e-child-pet-p.png) | 자동 개행 없음 | 동일, 이유 줄 포함 모두 온전히 표시 |
| [E 아이+반려동물 D](demo/overlay-e-child-pet-d.png) | 자동 개행 없음 (`빈자리 849면`) | 동일 |

- E의 날짜 차이: 기존 사진은 10월 31일이어서 둘째 행사가 `의정부지 상설 전통문화행사 · 오늘까지`였지만, 이번에는 날짜가 지나 기존 필터가 다음 기록 항목 `남산봉수의식 등 전통문화행사 · 12월 31일까지`를 고른다. 같은 시연 파일과 동행 조합을 사용했으며 시연 자료·판단 로직·시계는 바꾸지 않았다.
- **GPT 디자인 판단·제안:** 글자 중간 끊김은 없어져 읽기가 나아졌다. 다만 안개 본문의 두 번째 줄 `줄이세요.`와 살얼음 제목의 두 번째 줄 `구간이에요.`는 한 어절만 남아 줄 길이 균형이 어색하다. 후속 디자인에서 문장을 보존한 `앞이 500m도 안 보여요. / 속도를 줄이세요.`와 `2.6km 앞 살얼음 / 주의 구간이에요.`처럼 의미 경계에서 두 줄을 나누는 안을 제안한다. 이번에는 승인된 크기·폭·문구·배치를 유지하고 이 추가 조정은 구현하지 않았다.
- 소리·키: 모든 본 시험은 debug/`logOnlySpeech=true`였다. 재부팅 뒤 캡처 시험 구간 logcat에서 실제 키와 URL 인코딩 변형 **0개**, 앱 `TTS onStart` **0건**, 앱 `FATAL EXCEPTION` **0건**이다. 키 값은 출력하지 않았다.
- 복원·반납: 사용자 소리 시험 때 즉시 기기 사용을 중단했고, cold boot와 사용자 직접 조작이 끝난 뒤 캡처를 처음부터 다시 했다. Claude가 새로 지정한 시작 상태 **DEMO_B·travelMode=false·동행 키 없음·APPROACH·고지 확인**의 XML을 바이트 단위 동일하게 복원했다. 오버레이 appops **default**, 기어 **P**, 속도·페달·조향 **0**, 콘솔 비활성, 앱 프로세스·서비스·카드 창 없음까지 확인했다. `adb forward tcp:8088`은 재연결했다. **에뮬레이터 반납 status를 HANDOFF·커밋 전에 보냈고 이후 adb·가상 콘솔은 사용하지 않는다.**
- 남은 확인·요청: API 33 및 32 이하 기기 실기 시험은 하지 않았고 API 34에서 검증했다. Claude가 줄 균형 제안을 검토한 뒤 T-39를 리뷰하고 release APK를 다시 만들어 달라. 커밋 메시지: `[T-39] 카드 한글을 어절 단위로 줄바꿈`.


## 2026-10-31 18:40 · Claude · T-36·T-37 리뷰 완료

- 검증(두 작업 커밋 뒤 함께)
  - `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → core 167, 앱 28 통과
  - `make_demo.py --self-test` 통과
- T-36
  - 캡처: 지난 답 미리 선택 화면, 설정의 여행 모드 구간(켜짐, "동행 아이 · 반려동물 / 바꾸기"). T-32 목업과 맞다.
  - `MainActivity` 흐름
    - 완료할 때 P를 다시 확인하고 `companions`와 `travelMode`를 함께 저장한다.
    - 취소·뒤로·P 이탈이면 `ACTION_WITHDRAW_COMPANIONS`를 보낸다.
    - `companions` 변경은 안내를 다시 시작한다.
  - 기기에서 서울 평문 허용을 확인했다: `seoul cleartext=true other=false`.
- T-37
  - 캡처
    - 동행 없음 P: "도착 때 비 예보 · 실내 명소 우선", 문화시설 3곳이 앞에 온다.
    - 아이+반려동물 P: 인사동·익선동·명동, 각 "일부 구역 동반 · 목줄 착용". 반려동물 장소가 모두 야외라 날씨 줄이 없는 것은 규칙대로다.
    - D: "세종로 주차장 / 빈자리 849면 / 목적지에서 300m · 5분에 430원"
  - 서울 재생은 기록 시점 기준으로 판단한다(T-37 질문, 계획 수정). 그래서 토요일에 재생해도 430원이다.
- 다음에 다룰 것
  - 카드의 한글 줄바꿈이 단어 중간에서 갈린다(예: 위험 카드 "속도를 줄 / 이세요"). 오래된 문제라 별도 작업으로 다룬다.
  - 시연 E의 행사 하나("의정부지 상설 전통문화행사")는 기록 기준 끝나는 날이 가까워 곧 목록에서 빠진다. 나머지 행사는 12월 31일까지다.

## 2026-10-31 18:21 · Codex · T-37 시연 E 여행 모드 재생·캡처 완료

- 구현: `ReplayFeed.kt` 안의 기록 `TourSource`로 **`collectTour()`를 그대로 호출**한다. 동행·날씨·반려동물 범위/조건·편의시설을 T-34 원본에서 읽고, 기록에 없는 상세는 `AssertionError`로 실패시켜 일반 조회 실패 처리에 묻히지 않게 했다. 단기예보는 `도착 시각 − (재생 시작 − 2026-10-29 19:36 KST)`의 정시를 조회한다. 서울 기록은 TS 자산의 목적지 500m 안 주차장에 합친다. 생성기와 `scene-e.jsonl`에 원본 본문만 담았으며 키와 외부 호출은 없다.
- 장면: **0초 P/FREE → 2초 ROUTE·세종문화회관(1,800m/420초)·맞춤 추천 → 32초 D/10mps → 2초마다 남은 거리 감소 → 48초 1,000m에서 주차 안내 → 66초 150m**. 목적지는 T-34의 관광 좌표이며 세종로 TS까지 277.348m다. A/B/C의 생성 내용과 T-34 원본은 동일하다.
- 요금 기준 조정(Orca ask 승인): 오늘 토요일에는 실시간 `applySeoul(now)`가 원본의 토요일 무료 값을 적용해 시연의 430원 문장과 달랐다. Claude 결정대로 **운영 시간·요금·신선도는 서울 `recordedAt`(목요일 저녁)**에 `applySeoul`로 판단하고, 결과의 `availableProduced`만 `(재생 시각 − recordedAt)`만큼 옮긴다. 따라서 기록 당시 나이 **40.979704초**, **빈자리 849면·5분에 430원**을 요일과 관계없이 재현한다. 토요일 회귀 테스트의 RED→GREEN도 확인했다.
- 빌드·단위 검증: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` **성공**, **앱 28개·core 167개(합계195개), 실패·오류·skip 0**. `ReplayFeedTest` 9개 중 새 3개는 동행 `{}`, `{CHILD}`, `{ELDER}`, `{PET}`, `{CHILD,PET}`의 자료·RAIN, 누락 상세 강제 실패, 서울 849면/요일 고정 요금/생산 시각 이동을 검증한다. 기존 E 테스트는 P 목록·D 전환·48초 주차 문장·150m 종료까지 확장했다. 첫 안내 전체를 `{}`와 `{PET}`에 대해 고정했다. `py -3.12 tools/make_demo.py --self-test` 통과. T-36 구현 전의 임시 선택 화면 테스트 제외는 최종 검사에서 사용하지 않았으며 `CompanionPickerTest` 2개가 포함돼 있다.
- 첫 안내(동행 없음): “목적지 근처에서 12월 31일까지 2026 서울거리공연 구석구석라이브가 열려요. 목적지에서 100m예요. 의정부지 상설 전통문화행사도 열려요. 도착할 때쯤 비 예보가 있어 실내 명소를 먼저 골랐어요. 가까운 명소는 국립현대미술관 서울, 서울특별시교육청 정독도서관, [K드라마 촬영지] 공근혜갤러리예요.”
- 첫 안내(반려동물): “목적지 근처에서 12월 31일까지 2026 서울거리공연 구석구석라이브가 열려요. 목적지에서 100m예요. 의정부지 상설 전통문화행사도 열려요. 반려동물과 갈 수 있는 가까운 명소는 인사동, 익선동 한옥거리, 명동이에요. 인사동은 일부 구역만 반려동물과 들어갈 수 있어요.”
- 주차 문장: **“목적지 근처 세종로 주차장에 빈자리 849면이 있어요. 5분에 430원이에요.”**
- 에뮬레이터: T-36의 `c5b5a85` 커밋·반납과 Claude의 사용 가능 회신 뒤 `emulator-5556`/user10/debug/**`logOnlySpeech=true`**로 시험했다. 저장된 `Prefs.companions`를 `{}`와 `{CHILD,PET}`로 차례로 넣고, 설정의 여행 모드는 OFF인 채 시연 E에서 켜지는 흐름을 확인했다. 두 프로세스 각각 안내 2회(추천·주차), **앱 HTTP GET 0·TTS onStart 0·FATAL EXCEPTION 0·실제 키 변형 0**, 동행 질문 0회다. 아이+반려동물의 첫 안내는 위 반려동물 문장과 같았다. 실내가 없는 반려동물 목록에는 비 예보 줄이 붙지 않았다.
- 캡처(모두 **2560×1440**): [동행 없음 P](demo/overlay-e-none-p.png), [동행 없음 D](demo/overlay-e-none-d.png), [아이+반려동물 P](demo/overlay-e-child-pet-p.png), [아이+반려동물 D](demo/overlay-e-child-pet-d.png). 실제 Android 글꼴에서 긴 명칭과 날씨 줄이 잘리지 않고, 반려동물 이유 “일부 구역 동반 · 목줄 착용”과 숫자 **849**·빈자리/면 기준선·300m/5분430원 줄이 카드 안에 보인다. 이번 원본 이름은 한 줄 안에 들어갔고 임의의 초장문·최대 숫자는 주입하지 않았다.
- 복원: 원래 **DEMO_C·APPROACH·고지 확인·여행 OFF·동행 키 없음**을 복원해 XML의 설정값 동일성을 확인했다(Windows ADB의 CR 차이로 원시 바이트 비교는 다름). 오버레이 권한 **default**, 앱 프로세스·서비스·`RoadAheadCard` 창 없음, 내비 화면 **P/0**을 확인하고 사용 끝을 Claude에게 보냈다. 차량·GPS·가상 콘솔·오디오·내비 설정은 조작하지 않았다. debug 설치는 유지했다. 근거는 gitignore 대상 `.gradle/t37-{none,child-pet}-logcat.log`, `t37-final-proof.json`, `t37-settings-{before,restored}.xml`, `t37-restored.png`다.
- 남은 문제·요청: 행사 날짜는 원본대로라 2026-12-31 이후 행사 안내가 빠질 수 있다. 실차·소리 나는 TTS·push는 하지 않았다. Claude가 T-37의 재생 시각 이동, 누락 상세 실패, 네 캡처를 리뷰해 달라.

## 2026-10-31 18:10 · Codex · T-36 동행 화면·카드 표시 구현

- 한 일: 계획 Task 6의 `CompanionPicker`와 선택 변환 함수를 만들었다. 미응답 `null`과 동행 없음 빈 집합을 구분해 `Prefs.companions`에 저장하고 서비스에 전달한다. 켜기 진입 때 한 번만 질문을 요청하며, 완료 직전 P 재확인 후 동행·여행 모드를 함께 저장한다. 취소·뒤로·P 이탈은 임시 선택을 버리고 질문 대기를 철회한다. 바꾸기는 음성 없이 열고, 재구성·회전용 선택/진입 상태는 `rememberSaveable`로 보관한다.
- 화면·카드: T-32의 폭·색·치수·문구로 동행 선택과 설정 구간을 구현했다. 여행 이름은 자연 개행, 이유는 최대 두 개와 실제 폭 검사, 문화시설이 남아 있을 때만 날씨 줄, 실측 880dp 초과 시 마지막 항목을 통째로 제거한다. 주차는 머리줄의 16dp 점과 480dp 본문, 숫자·단위 기준선, 빈자리 미상 행 생략, 큰 숫자 개행/단계 축소를 적용했다. 신호·위험의 공통 `text()`는 그대로 둔다.
- 검증: `CompanionPickerTest`의 계획 이름 두 개(`none is exclusive`, `stored companions round trip`)를 먼저 써 컴파일 실패를 확인한 뒤 통과시켰다. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` **성공**, 앱 **28개**·core **167개** = **195개**, 실패/오류/skip 0개다. 앱 수는 기존 23개 + T-36 2개 + 동시 작업 중인 T-37 3개다. `:app` 컴파일 정상이다.
- 에뮬레이터: `emulator-5556`/user 10/debug/`logOnlySpeech=true`. 첫 사용 질문 `speak=누구와 함께 가세요? 화면에서 골라 주세요.` **1회**, 선택 재구성 후에도 **1회**였다. 네 번의 켜기 진입 합계 4회이며 바꾸기는 증가시키지 않았다. 첫 완료 전 꺼짐·미응답 보존, NONE 단독 선택과 빈 집합 저장, 아이+반려동물 완료·재선택, 뒤로 취소, 켜기/바꾸기 각각 D 전환 취소, 모드를 꺼도 지난 동행 보존을 확인했다. 서비스 생성 시 **`seoul cleartext=true other=false` 1회**를 확인했다. 시험 구간 logcat의 실제 키·인코딩 변형 **0개**, `TTS onStart` **0건**이다(값은 출력하지 않았다).
- 캡처: [첫 사용](demo/t36-companion-first.png), [지난 답 미리 선택](demo/t36-companion-preselected.png), [동행 없음 단독](demo/t36-companion-none.png), [설정 켜짐](demo/t36-settings-travel-on.png), [설정 꺼짐·지난 답](demo/t36-settings-travel-off.png), [완료 전 D로 닫힘](demo/t36-companion-drive-cancel.png). [검증 수치·복원 기록](demo/t36-verification.json).
- 복원: 시작 설정(`DEMO_C`, `APPROACH`, 여행 모드 꺼짐, 동행 키 없음, 고지 확인)을 `adb exec-out run-as`로 저장하고 **XML 바이트 단위 동일**하게 복원했다. 오버레이 권한 `default`, 기어 P·속도 0·페달 0·조향 0으로 복원했고 가상 콘솔 heartbeat를 종료해 원래 비활성 상태로 돌아갔다. 앱·서비스는 종료했다. 다른 권한과 내비 설정은 바꾸지 않았다. **에뮬레이터 반납**.
- 남은 문제·요청: 조정 지시(`msg_ffa599ea8b57`)에 따라 옛 E 회귀 캡처를 제외했다. 새 이유·날씨·빈자리 카드와 긴 이름/숫자의 실제 Android 글꼴 캡처·검토는 **T-37**에서 한다. 실제 화면 회전 시험은 생략했으며 클릭 이벤트에서만 질문을 보내고 저장 가능한 상태로 구현했다. Claude는 P 이탈 시 저장 보존과 카드 실측 규칙을 리뷰하고 T-37 캡처를 함께 확인해 달라. `ReplayFeed`·시연 자료·생성기는 수정/스테이징하지 않았다. 커밋 메시지: `[T-36] 동행 묻기 화면과 카드의 이유·빈자리 표시`.

## 2026-10-31 18:10 · Claude · T-35·T-38 리뷰 완료

- T-35
  - 사용자가 10-31에 "에뮬레이터 준비는 Claude가 대신"을 승인했다(작업자 쪽 자동 승인 검토가 준비 명령을 거부해 10-29에 3시간 대기 뒤 미완료로 멈췄던 일). Claude가 준비했다.
    - 가상 콘솔 P/0 확인·저장
    - 내비 음소거: 내비 화면 소리 버튼 → 음량 창. `cmd car_service`는 user 빌드라 막혀 있다.
    - 시험 앱 `op=route`로 세종문화회관 경로. 첫 요청은 `CAUSE_SEARCH_ROUTE`(일시 오류), 재요청 성공
  - 검증: `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → core 167, 앱 23 통과. `collectTour`, 서울 URL 가리기, 평문 허용 설정(`openapi.seoul.go.kr`만), `SEOUL_KEY`, 서울 실패 시 TS 대체를 읽었다.
  - 남은 위험: 에뮬레이터 차가 성남에 고정돼 실제 서울 주차 HTTP가 기기에서 한 번도 돌지 않았다. T-36에서 서비스 시작 때 `NetworkSecurityPolicy.isCleartextTrafficPermitted("openapi.seoul.go.kr")`를 debug 로그로 한 번 확인한다.
- T-38
  - `node video/render.mjs --check` 통과를 다시 확인했다.
  - E1 질문·답, E2, D 캡처를 봤다. T-32 디자인과 대본 문장이 맞다.
  - 영상은 도입 15초, 콘셉트 85초, 마무리 5초다. 실제 녹화 20초를 더하면 약 125초다.
  - E2 자막에서 "전망대 공원"이 줄바꿈으로 갈리지만 그대로 둔다.

## 2026-10-31 17:40 · Codex · T-35 실시간 연결·소리 없는 검증 완료

- 한 일·커밋: **`4a928ca` `[T-35] 동행·날씨 자료와 서울 빈자리 실시간 연결`**에 계획 Task 5의 앱 파일 12개만 커밋했다. `TourSource`·`collectTour`의 동시 목록/예보 조회, 확인된 반려동물 최대 5곳·시설 최대 10곳 조회, 부분 실패 격리·취소 전파, 반려동물·시설·예보·서울 API, 서울 키 경로 마스킹·해당 호스트만의 HTTP 허용, 새 Feed/서비스/core 계약을 연결했다. 화면·카드 모양과 새 재생 사건은 T-36·T-37 범위로 남겼다.
- 계획 조정·임시 동행: 10-29 Orca ask 승인과 `d16bbd8`에 맞춰 `TourSource.petInfo(id): PetInfo?`와 기존 `ReplayFeedTest`의 컴파일 이관을 적용했다. T-36의 `Prefs.companions`가 없어서 서비스는 `settings`의 문자열 집합 `companions`를 읽으며 기본값은 빈 집합이다. 실시간 시험에서는 앱을 종료하고 `adb shell run-as com.roadahead --user 10`으로 원본 XML을 보관한 뒤 `mode=LIVE`, `travelMode=true`, `companions={PET, ELDER}`를 임시로 넣었다. `MainActivity --ez logOnlySpeech true`로 시작했으며 시험 후 원본 설정값으로 복원했다. T-36은 이 직접 읽기를 Prefs로 옮기는 `shortcut:`을 정리한다.
- 빌드·테스트: 10-29에 테스트 선행 RED→GREEN을 확인했고, 재개 후 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks --console=plain`을 **48개 작업 모두 실행해 성공**했다. **앱 23개·core 167개 = 190개**, 실패·오류·skip **0개**다. 새 테스트 6개는 서울 URL 가리기, 예보 인자, 동행별 호출 수, 부분 실패, 동시 시작, 취소 전파를 검증한다. `git diff --cached --check` 통과, 커밋 전 앱 diff의 실제 키·인코딩 변형 포함 **0개**를 확인했다. 키 값은 출력하지 않았다.
- 실시간 결과: `emulator-5556`/user 10의 세종문화회관(**37.57200988776222,126.97630706471843**, 내비 이름 `SejongCenter`) 경로를 ROUTE 중 5초 폴링으로 잡았다. 목적지 키 **37.57201,126.97631**, 명소 **66곳**·행사 **89곳** 수신, 동행 **ELDER+PET**다. 관광/행사 `KorService2` **2회**, `KorPetTourService2` **6회**(목록 1+상세 5), `KorWithService2` **5회**, 단기예보 **1회**로 관광 흐름 합계 **14회**다. 기존 배경 조회는 교차로 지도·도로 기상·ITS 돌발·재난 각 1회로 **전체 HTTP 18회**였다.
- 안내·날씨: `speak=`는 **1회**, 문장은 “목적지 근처에서 12월 31일까지 2026 서울거리공연 구석구석라이브가 열려요. 목적지에서 100m예요. 의정부지 상설 전통문화행사도 열려요. 반려동물과 갈 수 있는 가까운 명소는 인사동, 명동, 남산순환나들길이에요. 인사동은 일부 구역만 반려동물과 들어갈 수 있어요.”였다. 실제 예보 로그는 **base=20261031/1700, grid=60,127, arrival=2026-10-31T18:28:41.193240, weather=null**이다. 궂은 날씨 판정이 없어 날씨 문장은 붙지 않았다. 시험·복원 구간 전체 logcat의 실제 키·인코딩 변형 포함 **0개**, 앱 `TTS onStart` **0건**, `FATAL EXCEPTION` **0건**이다.
- 복원: 앱 설정 **DEMO_C·APPROACH·disclaimerSeen=true·travelMode=false**, 동행 저장값 없음으로 되돌렸고, 오버레이 권한 **default**, 앱 프로세스·서비스·`RoadAheadCard` 창 없음, 내비 경로 취소 콜백 **1회**, 내비 음소거 해제를 확인했다. 마지막 화면은 경로 없는 검색 대기·**P/0**이며 이 작업에서 차량 제어·GPS·시스템 음량은 바꾸지 않았다. 설정 검사 중 Windows ADB `shell`의 CR 추가를 확인해 `exec-out`으로 원본 출력을 읽어 비교했다. 10-29 준비 명령의 자동 승인 거부는 10-31 사용자 승인 후 Claude의 대행 준비로 해결됐으며, 이전에 스테이징한 보류 보고를 이 결과로 갱신했다.
- 근거·남은 범위: gitignore 대상 `.gradle/t35-oct31-final-build.log`, `-live-proof.json`, `-final-proof.json`, `-final-logcat.log`, `-app-restored.json`, `-navigation-restored.json`, `-live.png`, `-volume-restored.png`, `-restored.png`에 남겼다. 남은 거리 약 **23km/3180초**라 이번 실시간 시험의 **서울 주차 HTTP는 0회**다. 서울 연결의 정수·신선도·운영 시간은 core 테스트, URL 마스킹은 앱 테스트로 확인했으며 실제 빈자리 수신·주차 카드 표시는 이 시험에서 확인하지 않았다. Claude가 T-35를 리뷰하고 T-36의 동행 화면, T-37의 서울 빈자리 재생 검증을 이어간다. 소리 나는 TTS·push는 하지 않았다.

## 2026-10-29 20:21 · Codex · T-38 여행 모드 콘셉트 영상 리뷰 요청

- 바뀐 장면: 도입3번째 문구를 추가해 **15초**, 콘셉트는 기존 A0~25/B25~35/C35~50 뒤에 **E1 50~59 → E2 59~75 → D75~85초**로 바꿨다. 마무리는5초 그대로다. 자막은 VIDEO.md 원문 그대로이며, 새 긴 문구는 글자를 바꾸지 않고 줄 길이만 고르게 배치했다. [시각표·디자인 근거](../video/CUES.md), [E1 질문](../video/frames/02-scene-e1-question.png), [E1 답](../video/frames/02-scene-e1-answer.png), [E2](../video/frames/02-scene-e2.png), [D](../video/frames/02-scene-d.png).
- E1 디자인: T-32의 어두운 카드·흰 질문·파란 선택 언어를 옮겼다. 질문을 유지하고 아래에 **아이 · 반려동물**을 보여 질문과 확인된 답을 함께 읽게 했다. 답 자리를 미리 확보해 카드가 뛰지 않고, 하단의 작은 파랑 **운전자** 글자는 자막 원문 밖에 분리했다. 버튼·마이크·이모지·큰 원은 없다. E1은 P 정차·경로/도착 정보 없음이고 E2에서만 경로가 새로 생긴다.
- E2·D: 영상 폭450/앱560의 같은 비율로 여행·주차 카드 치수를 옮겼다(새 카드 패딩22.5/32.14/28.93·반경25.71px, A/B/C의 기존 규격은 유지). E2는 원 없이 날씨 줄과3항목·파란 이유 줄2개, D는12.86px 분류 점과 **23만77.14px**로 강조했다. 기존 E 캡처 파일명은 새 E2와 같은 이미지로 갱신해 과거 링크를 보존했다.
- 검증: `node video/render.mjs --check`, 최종 `node video/render.mjs` 통과. **자막18개 원문·CUES 시각**, 기하32샘플, E1 운전자 구별·P 정차·고정 카드, E2 새 경로·이유/날씨색·3항목, D 숫자 강조, 겹침/넘침·외부 HTTP(S)0건·페이지 오류0건을 확인했다. 수정 전532e562의 A/B/C·마무리9프레임을 같은 Node Playwright/Chromium에서 캡처해 **SHA-256 동일**을 확인했다.
- 영상: ignored `video/out/`에 **01-intro.webm 15.00초·6,538,203 bytes**, **02-concept.webm 85.00초·32,162,949 bytes**, **04-outro.webm 5.00초·2,984,494 bytes**를 다시 만들었다. 모두 **1920×1080·30fps·오디오 트랙 없음**이며 `metadata.json`·`validation.json`에 근거가 있다. 영상은 커밋 대상에서 뺐다.
- 남은 일/요청: Claude가 E1의 질문·답 카드와 전체 자막 타이밍을 리뷰해 주기 바란다. 실제 앱 TTS·운전자 음성·에뮬레이터 녹화 합성은 사용자 편집 단계다(소리를 생성·재생하지 않았다). 목업105초에 실제 녹화20초를 더하면 약125초여서 최종 편집 길이는 CUES에 명시했다. TASKS의 T-38만 리뷰대기로 바꿨고, 다른 작업자의 앱·공용 문서 변경은 보존했다. 앱/설계/대본 수정·패키지 설치·push는 하지 않았다.

## 2026-10-29 21:20 · Claude · T-33·T-34 리뷰 완료, T-35 시작

- T-33 검증
  - `:core:test --rerun-tasks` → 167개 통과(실패·오류·skip 0)
  - 읽은 코드: `TravelAdvisor`(반려동물 확인된 곳만, 맞는 동행 수 → 실내 → 거리, 이유 순서, 음성 이유에서 전구역 라벨·필수 조건 제외), `SeoulParking`(운영 시간 `2400`·자정 넘김, 토요일 무료, 신선도 0~600초, 정수 검사), `Forecast`(발표 시각, 격자), `CompanionParser`, 엔진 `announce`/`withdraw`/여행 모드 주차, `koFeeShort`
  - 편의시설 판정은 T-34 기록의 실제 값 26종(화장실·엘리베이터·휠체어·유모차·수유실)으로 다시 봤다. 화장실 칸은 모두 장애인·다목적 화장실 설명이라 지금 규칙이 맞다.
  - 계획 오류 1건을 작업자가 찾았다(신선도 테스트 시각이 동국제강 운영 종료와 겹침). 운영 시간과 분리하도록 승인하고 계획을 고쳤다(`20d0b50`).
- T-34 검증
  - `record_travel.py --self-test` 통과
  - 추적 파일 73개에서 `api`의 키 0개
  - 목적지 세종문화회관(관광공사 좌표), TS 세종로까지 277 m, 격자 60/127, 세종로 빈자리 849면(기록 시각 기준), 호출 35회
- 커밋 경합: Claude가 T-34 스테이징을 커밋에 넣었다가 되돌린 직후, T-33 커밋 `86aae42`에 T-34 기록이 함께 들어갔다. 이력은 고치지 않았다. `AGENTS.md` 커밋 절에 규칙을 추가했다(`2486b6b`).
- T-35(실시간 연결)를 T-33 터미널에 배정했다.

## 2026-10-29 20:08 · Codex · T-33 여행 모드 core 리뷰 요청

- 한 일: `docs/plans/travel-mode.md` Task 1~4와 진행 중 전달된 최신 변경을 `core/`에 구현했다. `Companion`·`PetInfo`·`Facility`와 상세 해석, `topic()`, 기상청 격자·발표 시각·도착 날씨, 동행 수→실내→거리 추천, 반려동물 범위·짧은 조건 우선 이유와 `TravelResult.weatherNote`, 서울 두 곳의 정수·연계·생산 시각·운영 시간·요금 병합, 주차 이름 보완·카드용 `koFeeShort`, 여행 모드 OFF 주차 차단, 질문 `announce`·`withdraw`를 추가했다. 기존 관광 머리/items 처리를 재사용했고, 기존 `TourParserTest`의 추천 호출도 새 계약으로 옮겼다. 새 라이브러리와 `app/` 변경은 없다.
- Task별 커밋: **Task 1 `ced2da2`** `[T-33] 동행 모델과 반려동물·무장애 상세 해석`, **Task 2 `645d055`** `[T-33] 도착 시각 단기예보 해석`, **Task 3 `c560d31`** `[T-33] 동행·날씨 맞춤 명소 추천`, **Task 4 `86aae42`** `[T-33] 서울 실시간 빈자리와 여행 모드 안의 주차`. 모두 Codex 작성자 표시를 붙였다.
- 검증: Task마다 새 테스트를 먼저 추가해 누락된 API·시그니처로 컴파일 실패(RED)를 확인한 다음 구현했다. ` .\gradlew.bat :core:test --tests "*CompanionParserTest" --tests "*KoreanTest" --console=plain`, `--tests "*ForecastTest"`, `--tests "*TravelAdvisorTest"` 각각 통과 후 매 Task의 ` .\gradlew.bat :core:test --console=plain` 전체 통과를 확인했다. 전체 테스트 수는 순서대로 **136 → 141 → 151 → 167개**, 최종 **기존 130 + 새 37, 실패·오류·skip 0**이다. RED/GREEN 로그는 gitignore 대상 `.gradle/t33-task{1,2,3,4}-*.log`에 있다.
- 경계 검증: 미확인 반려동물 장소·목록 실패 제외, 빈 상세와 오류 구분, `턱이 없어` 긍정/`없음`·`불가` 부정, 시설별 동행 수·이유 2개·날씨 문장/카드 조건, 서울 미연계·점유 초과·미래/오래된 생산 시각·600초 포함/초과·소수/범위 초과 면수·운영 종료·자정·자정 초과 범위·토요일 무료·요금 쌍 보존, 질문 음성 내비 대기·1회·취소를 검사했다. 복사한 테스트 JSON은 지정된 원본 본문과 같고 `git diff --check` 통과다. 격자 추가 표본은 [기상청 공식 활용가이드](https://www.data.go.kr/data/15084084/openapi.do)의 2610 격자 위경도 표 부산광역시 행(35.1770194444444/129.076952777777 → 98/76)을 확인했다.
- 승인된 계획 조정: 동국제강의 `18:57:23 + 601초`가 19시 운영 종료와 겹친다. Orca ask 승인과 계획 커밋 `20d0b50`에 따라 신선도 테스트만 `copy(weekdayHours = null, weekendHours = null)`로 분리했고, 실제 운영 시간은 별도 테스트에서 원본 그대로 검사한다. 테스트 경계를 분리했으며 앱 운영 시간 규칙은 바꾸지 않았다.
- `:app` 컴파일 상태: ` .\gradlew.bat :app:compileDebugKotlin --console=plain`은 **실패**다. `RoadAheadService.kt:153`의 삭제된 `onTourItems`, `:169`의 `parkingRequest`에 빠진 `travelMode` 인자 **2건**이다. 요청대로 호환용 임시 함수나 `app/` 수정을 넣지 않았다. `ReplayFeedTest.kt`의 옛 `onTourItems` 호출도 T-35에서 옮겨야 한다. 로그는 `.gradle/t33-app-compile.log`다.
- 커밋 경위: **`86aae42`에 T-34 기록이 함께 들어감(공유 인덱스 경합)**. 코디네이터에게 즉시 알렸고 지시에 따라 공유 이력과 파일을 유지했다. 이후 커밋은 파일 범위를 명시하고, 공용 문서는 자기 항목만 스테이징한다.
- 남은 문제·요청: Claude가 Task 1~4의 core 경로와 위 경계를 리뷰하고 T-33을 확정한다. **T-35**는 `onTourData`·새 `parkingRequest`·`PetInfo`·날씨 메모·짧은 요금 계약으로 실시간/재생 호출자를 옮긴 뒤 앱 빌드·테스트를 진행한다. 서울 대응은 설계대로 두 곳이며 공휴일은 요일 기준이다. 실차·에뮬레이터·소리 나는 TTS·push는 실행하지 않았고 키 값을 읽거나 출력하지 않았다.


## 2026-10-29 20:02 · Codex · T-34 시연 E 여행 모드 원본 응답 기록

- 한 일: [기록 도구](../tools/record_travel.py)(Python 3.12·표준 라이브러리)와 [응답·집계](../tools/demo-src/travel/summary.json)를 추가했다. `*.json`의 `response`에 발췌 없는 응답 전체, `request.params`에 키를 제외한 요청 인자, `requestedAtKst`에 조회 시각을 저장한다. `.requests/`는 호출 전 예약 기록이며, 기존 응답은 읽기만 하고 응답 없는 이전 요청도 자동 재호출하지 않는다. 앱·`make_demo.py`는 바꾸지 않았다.
- 목적지: **세종문화회관 37.57200988776222, 126.97630706471843**. [한국관광공사 공식 무장애 관광 페이지](https://access.visitkorea.or.kr/ms/detail.do?cotId=4d28e66c-2efa-47d7-881e-403875952880)의 관광정보 좌표다. TS 세종로 **37.5733652, 126.9736652**까지 Haversine **277.348m**, 기상청 격자 **60, 127**로 확인했다. 국문 위치 목록은 244/244행 전체를 받았지만 회관 자체 행은 없어서 API `contentid`를 꾸미지 않고 `null`로 남겼다. 목록 요청 좌표는 계획대로 **mapX=126.976/mapY=37.572**다. 좌표 확인용 공개 웹페이지 조회 1회는 아래 인증 API 호출 수와 별도다.
- 기록 목록: `places.json`(국문 위치 244행, 관광지·문화시설·레포츠 3km 안 67곳), `festivals.json`(2026-10-29 행사 69/69행), `pet-places.json`(10/10행, 대상 종류·3km 안 6곳), `pet-{contentid}.json` **6개 전부**, `facilities-{contentid}.json` **25개**(가까운 명소 10곳 ∪ 가까운 문화시설 10곳 ∪ 동반 범위 확인 6곳, 중복 제거), `seoul-parking.json`(122행 전체), 기존 `forecast-20261029-1700.json`의 같은 JSON 본문을 복사했다. 반려동물 상세는 **6/6곳**이 채워져 일부구역 4곳·전구역 2곳을 확인했고, 편의시설 상세는 **17/25곳**이 채워져 8곳은 성공 빈 응답이었다. 기록 시각·대상 ID·해석 결과는 집계 JSON에 있다.
- 서비스별 실제 호출: **KorService2 2회**(위치·행사 각 1), **KorPetTourService2 7회**(위치 1+상세 6), **KorWithService2 25회**, **서울 GetParkingInfo/1/1000/ 1회**, **기상청 단기예보 0회**, 합계 **35회**. 동일 서비스·작업·인자의 중복 요청은 0개이며, 날씨는 2026-10-29 19:36 KST 기존 기록을 재사용했다.
- 세종로(`171721`): 조회 **2026-10-29 19:59:23.979704 KST**, 연계 **1**, 갱신 **19:58:43**, 나이 **40.980초**로 신선하다. **총 1,260면 − 점유 411대 = 빈자리 849면**이며 수치·연계·0~600초 조건을 모두 만족한다. 재생에서 이 기록값을 쓰고 갱신 시각은 계획대로 옮겨야 한다.
- 검증: `py -3.12 tools/record_travel.py --self-test` 통과(키 가리기, 주석이 붙은 키 줄, 격자·거리, 대상 합집합, 반려동물·편의시설 해석, 서울 미연계·오래된/미래·범위 오류, 덮어쓰기·중단 요청 재호출·경로 이탈 거부). 별도 오프라인 검증에서 **35개 고유 HTTP 200**, 목록 전체 행 수, 반려동물 6/6·편의시설 대상 합집합 25/25, **동행 8조합 × 날씨 2상태**의 상세 요청 포함, 예보 본문 동일성을 확인했다. 네트워크를 예외로 차단하고 `main()`을 다시 실행해 **추가 호출 0회·기록 72파일의 바이트/수정시각 변화 0개**를 확인했다. 기록의 고유 요청·상세 대상 포함·예보 동일성과 네트워크 차단 재실행 검증도 `--self-test`에 남겼다. 원본 커밋 및 후속 커밋 전 staged 내용의 실제 키 변형 개수는 각각 **0개**, `git diff --check`를 확인했다.
- 커밋 경위: **T-34 기록은 `86aae42`에 T-33 변경과 함께 커밋됨(공유 인덱스 경합)**. 코디네이터 지시에 따라 공유 이력은 유지하고, 그 커밋과 원본 72파일이 같은지·키가 없는지 확인했다. T-34 전용 후속 커밋에는 도구의 자체 검증 보강과 이 보고 정정만 포함한다.
- 남은 문제·요청: 세종문화회관 좌표는 공식 관광 페이지의 시설 대표점이며 출입구 실측은 아니다. 편의시설 빈 응답 8곳을 "시설 없음"으로 바꾸지 않는다. **T-37**은 위 파일의 `response`를 사용해 앱 재생·서울 시각 이동·캡처를 진행한다. 앱 빌드·에뮬레이터·소리 나는 TTS·push는 실행하지 않았다. Claude의 T-34 리뷰를 요청한다.

## 2026-10-29 20:40 · Claude · T-32 리뷰 완료, 여행 모드 설계·계획 확정

- T-32 검토: 추천안 문서와 목업(동행 선택, 반려동물 카드, 비 예보 카드, 세종로 주차 카드)을 직접 봤다. 네 가지 판단 요청을 **모두 채택**했다.
  1. 동행 선택을 완료할 때 여행 모드를 켠다. 취소·P 이탈이면 꺼진 채.
  2. 긴 이름은 말줄임 없이 줄을 바꾸고, 880dp를 넘으면 끝 항목을 통째로 뺀다.
  3. 주차 카드는 16dp 점, 숫자만 96sp.
  4. 이유 순서: 반려동물 범위 → 짧은 필수 조건 → 사람 이유. 아이+어르신은 하나씩. 어르신은 턱 없는 입구 먼저.
- 반영: `DESIGN.md` 10.1~10.3(`4da53ca`), `plans/travel-mode.md` Task 1·3·4(`d016e20`, 진행 중인 T-33에 알림), Task 6(`4da53ca`).
- 채택하지 않은 것: "길고 중요한 조건을 두 문구에 못 담으면 반려동물 추천에서 보류"는 빼고, `acmpyNeedMtr`가 한 줄·10자 이하일 때만 조건으로 보인다. 근처 자료의 조건은 "목줄 착용" 정도였다.
- 사용자 지시 이행: 화면·카드 모양은 이번 GPT 안으로 정했다(디자인은 GPT와 상의).

## 2026-10-29 19:50 · Codex · T-32 여행 모드 디자인 안 리뷰 요청

- 한 일: [추천안](design/travel-mode-ui.md), [동작 목업](design/travel-mode-ui.html), **전체 9장(2560×1440)·상세 5장**, 렌더 도구·치수/데이터 JSON을 `docs/design/`에 작성했다. 동행은 344×112dp 체크 버튼 3개와 1080×80dp 단독 선택, 완료 버튼80dp를 쓰고, 여행 이유는 20sp 최대2개·별도 한 줄로 제안했다. 주차는 16dp 파란 분류 점·전체 폭 제목·96sp 숫자와 32sp 라벨로 구성했다.
- 날씨·시연 추가 반영: 궂은 도착 예보 때문에 실내 명소가 선택되어 보일 때만 머리줄 아래 **도착 때 비 예보 · 실내 명소 우선**을 20sp로 표시한다. 실제 2026-10-29 17시 발표의 20시 예보(PTY1/POP60/TMP10)를 읽었고 새 API를 부르지 않았다. 세종문화회관 인근 목업점37.5725/126.9760으로 거리를 다시 계산했다. 세종로 TS 좌표까지 **227.15m→화면200m**, 기록은 **총1,260·점유479·빈자리781면, 5분430원**이다. 출입구를 검증한 좌표나 실제 재생 캡처는 아니다.
- 검증: `py -3.12 docs/design/render_travel_mode.py` 통과. 9개 전체 프레임·5개 카드 상태에서 폭560·왼쪽1824·아래끝1260을 확인했고, 예시 글자 넘침0·새 터치 목표80dp 이상·문서 링크·PNG 크기를 확인했다. 미선택 완료 비활성, 복수/단독 선택, 완료 저장, P 이탈 때 임시 답 폐기를 HTML에서 확인했다. 브라우저 목업 검증이며 Android 영속 저장·기어·TTS 검증은 아니다.
- 코드 확인: 현재 `OverlayCard.kt`는 주차 정보 순서·색은 맞지만 88dp 원 옆 제목 폭은368dp이고 공통 END 말줄임이 있다. 목업 글꼴에서 전체96sp의 **빈자리 781면=534.62px**로 본문480px을 넘으므로 숫자만 크게 제안했다. 글꼴·플랫폼 차이가 있어 실제 Android가 잘린다고 단정하지 않았다.
- 검토 요청: **① 완료 때 모드 ON 확정**(현재 DESIGN10.1의 P 이탈시 ON 유지와 다름), **② 긴 이름의 자연 개행 예외**, **③ 주차16dp 분류 점**, **④ 반려동물 범위·필수 조건 우선 표시**(현재 DESIGN10.2의 아이→어르신→반려 순과 다름)를 한 추천안으로 제시했다. 근거와 버린 안·후속 수정 파일은 문서에 적었다. Claude가 승인한 내용만 DESIGN과 구현 계획에 반영해 주기 바란다.
- 범위: `app/`, `core/`, `docs/DESIGN.md`를 수정하거나 빌드·에뮬레이터·음성·push를 실행하지 않았다. Orca의 읽기 보조 검토 요청은 중첩 한도로 시작되지 않았고(effectsApplied=false), 직접 구현 관점에서 재검토했다. TASKS의 T-32만 리뷰대기로 바꿨다.

## 2026-10-29 19:20 · Claude · T-26 리뷰 완료, T-30 보류

- 검증
  - `probe.py --self-test`, `analyze.py --self-test` 통과
  - `out/observation-index.json`으로 다시 집계한 결과가 커밋된 `summary.json`과 같다(생성 시각 제외). 122행, 17회, 65곳 변화, 이름 후보 78곳
  - `88d91f5` 전체와 원본 캡처 2개에서 `api`의 키 4개가 0번 나온다. `out/`은 gitignore 대상이다.
- T-30에 넘길 결론
  - 서울 응답에는 **좌표가 없다**. 이름 정규화 78곳을 한꺼번에 붙이지 않고, 확인한 명시적 대응표(동국제강 `1425816 → 04540-11291-00001-00-1`, 세종로 `171721 → 03172-11291-00001-00-1`)로 시작한다.
  - 빈자리 = `TPKCT − NOW_PRK_VHCL_CNT`, 조건은 보고서 5절 3항 그대로(연계=1, 0≤점유≤총면, 갱신 시각 나이 0~600초). 아니면 null.
  - 오류는 HTTP 200 + XML일 수 있다. 버스전용(`OPER_SE=4`)은 뺀다.
  - 동국제강 요금은 서울 5분/700원, 자산 30분/4,200원이다. 같은 값이지만 시간·금액을 쌍으로 다룬다.
- **T-30은 보류한다.** 사용자가 주차 안내를 여행 모드 안으로 묶고, 동행(아이·어르신·반려동물)을 고려한 추천을 넣기로 했다. 설계를 확정한 뒤 계획 Task 8과 함께 다시 쓴다.

## 2026-10-29 18:59 · Codex · T-26 서울 시영주차장 실측 리뷰 요청

- 한 일: [실측 보고](spikes/seoul-parking.md), [집계](spikes/seoul-parking/summary.json), [키 없는 3행·오류 표본](spikes/seoul-parking/samples.json), `spikes/seoul-parking/`의 표준 라이브러리 측정 도구와 APIS 7절을 작성했다. `GetParkingInfo` 전체 **122곳**을 60초 간격 **17회·16분** 관측했다(초기 1+연속 17+오류 3=**총 21회 호출**).
- 결과: 점유 차량 수가 한 번이라도 바뀐 곳은 **65/122(53.28%)**, 갱신 시각이 진전한 곳은 **109곳**이다. 17회 모두 109곳은 연계·10분 이내였고(전체 89.34%, 연계 표본 100%), 나이는 **26.104~60.119초**, 중앙값 **40.116초**였다. 미연계 13곳은 점유 0·시각 없음이므로 빈자리 null이다. 중복 코드 0, 코드 집합 변화 0이다. 동국제강은 13면·점유 3·빈자리 10을 유지했고 갱신 시각만 진전했다.
- 검증: `py -3.12 spikes/seoul-parking/probe.py --self-test`, `analyze.py --self-test` 통과, `observe.py --samples 17 --interval 60` 실측 완료, `analyze.py --output docs/spikes/seoul-parking/summary.json`으로 집계했다. 키 오류/범위 초과는 HTTP 200 XML(INFO-100/ERROR-336), 무자료는 HTTP 200 JSON(INFO-200)이었다. URL 키 경로는 `***` 처리했고 원본 응답은 ignored out/에만 보관했다. 앱 코드·에뮬레이터·TTS·push는 다루지 않았다.
- 남은 문제: **서울 응답에 좌표가 없어 50m 매칭은 산출 불가**다. 원문 이름 일치 0, 제한적인 이름 정리 후 유일 후보 78/122·미대응 44/122이며 검증된 동일 장소 수는 아니다. 청계8가의 자산 좌표는 서울 주소와 불일치 의심이라 추천에서 제외했다. 시청 반경 목록도 이름 후보의 KOTSA 좌표 기반으로 한계를 적었다. 한 저녁 관측으로 센서 정확도·항상 신선함·키별 호출 한도를 보장하지 않는다.
- 요청: Claude가 T-26을 리뷰하고 T-30에서 **점유→빈자리 차감**, 연계+KST 갱신 시각 **10분** 검사, XML 오류 처리를 반영해 주기 바란다. 자동 이름 일괄 결합보다 확인한 ID 대응부터 시작할 것을 권고한다. 장면 D는 동국제강(시청 316.75m), 보조는 세종로(869.77m)를 제안했다. TASKS의 T-26만 리뷰대기로 바꿨다.

## 2026-10-29 18:55 · Claude · T-31 README 갱신, T-26 시작

- T-26: 사용자가 서울 키를 `api`에 넣었다(줄 존재만 확인, 값은 보지 않음). Codex에 배정했다(Orca `ctx_0a7e5f1f395a`).
- T-31 (사용자 요청, Claude 직접)
  - `README.md`를 현재 제품에 맞췄다.
    - 한 줄 소개를 기획서와 같게 바꿨다.
    - "무엇을 알려 주나" 표를 넣었다.
    - 처음 실행 절차에 **다른 앱 위에 표시** 켜는 경로와 알림의 **끄기**를 넣었다.
    - 여행 모드, 시연 E, 주차·여행 데이터 범위, 에뮬레이터 빈 지도 안내를 넣었다.
    - 화면을 오버레이 캡처 3장으로 바꿨다.
  - 확인한 근거
    - 권한 문구: `strings.xml`
    - 설정 항목: `ui/Settings.kt`
    - 카드 조건: `ParkingConfig`, `TravelConfig`, 엔진 `travelListMs`
    - 카드 숨김: `OverlayCard`의 `card == null`
    - 관광 조회 키: `Apis.kt`의 `SIGNAL_KEY`
- 남은 일: 시연 D와 서울 빈자리 안내, `서울` 키 줄은 T-30에서 README에 추가한다.
- 방침(사용자 지시): 디자인(화면·카드·영상 등 보이는 것)은 GPT(Codex)와 상의해 정한다. T-30 이후 카드·화면에 손대는 작업은 Codex의 안을 먼저 받는다.

## 2026-10-29 15:28 · Codex · T-29 시연 E·여행 카드 수정 리뷰 요청

- 한 일: **`8a1aa0e`**에 Task 9와 여행 카드 수정 1건을 커밋했다. 시연 E 모드, 목적지 설정/해제·P 기어·관광 기록 사건, 기록보다 요청이 먼저 와도 기다렸다 응답하는 흐름을 연결했다. E에서는 여행 모드 설정이 OFF여도 여행 안내를 켠다. 생성기는 0초 P/FREE → 2초 ROUTE·서울시청 주변 목적지·관광 기록 → 32초까지 유지하는 7개 사건을 만들며, 관광 표본의 `response` 본문만 담는다.
- 카드: 여행 카드의 왼쪽 원을 빼고 최대 5개 항목을 이름 **26sp 흰색 1줄** + 종류·거리·행사 기간 **20sp `#C9D1D9` 1줄**로 나눴다. 항목 사이 **12dp**, 기존 너비·위치는 유지했다. [시연 E 캡처](demo/overlay-e-travel.png)(2560×1440)에서 다섯 이름과 보조 정보가 모두 말줄임·잘림 없이 보인다. 실제 창은 **`[1824,813][2384,1260]`**이며 내비 버튼 열도 보인다.
- 검증: 새 재생 테스트 3개와 생성기 검사가 구현 전 실패하는 것을 확인했다. `py -3.12 tools/make_demo.py`와 `--self-test`, `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` 성공, **앱 17개·core 130개(실패·오류·skip 0)**. 요청/기록의 두 순서, 목적지 해제·기어 변경, 장면 E의 전체 문장 1회·5개 목록을 검사했다. A~C 자산은 변경되지 않았고 기존 재생 테스트도 통과했다. 생성 사건 순서·원본 응답 일치·키 미포함·`git diff --check`도 확인했다.
- 에뮬레이터: `emulator-5556`에서 debug와 **`logOnlySpeech=true`**로 실행했다. 설정은 **DEMO_E·여행 모드 OFF**인데 `speak=목적지 근처에서 12월 31일까지 문화가 흐르는 서울광장이 열려요. …`가 계획의 전체 문장과 같게 **1회** 나왔다. P 상태 카드와 최대 5개 목록을 확인했고 **HTTP 호출·FATAL EXCEPTION·TTS onStart 모두 0건**이다. 근거는 gitignore 대상 `.gradle/t29-replay-live.log`, `t29-replay-proof.json`, RED/GREEN 로그다.
- 복원: 원래 앱 설정(시연 C·여행 모드 OFF 등)을 복원하고 **P/0, 앱 force-stop, 다른 앱 위 표시 OFF, 서비스·카드 창 없음**을 확인했다(`.gradle/t29-restored.json`). 실제 내비 경로·GPS·오디오 설정은 변경하지 않았고 API 키 값 출력이나 push를 하지 않았다. 커밋 직전 staged stat에서 자기 파일 8개만 확인했다.
- 남은 문제/요청: 기록된 행사 날짜는 그대로이므로 다른 날에는 안내가 달라지며, 2027년 시연 때 재생일 기준으로 날짜를 옮겨야 한다는 `shortcut:` 주석을 생성기에 남겼다. 실차·다른 해상도/글자 배율은 확인하지 않았다. T-29를 리뷰대기로 바꿨으니 Claude가 시연 E와 여행 카드 배치를 리뷰한다.

## 2026-10-29 17:40 · Claude · T-27·T-28 리뷰 완료, T-29 요청

- T-27 검증
  - `:app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → 144개 통과(core 130, app 14)
  - `PleosNavi`의 목적지 조회(`onRouteStarted` + ROUTE 중 5초 폴링, 0/0·빈 이름은 null)를 읽었다.
  - [실시간 여행 캡처](demo/overlay-travel-live.png)를 확인했다.
- T-28 검증
  - `tools/make_parking_asset.py --self-test` 통과
  - `lots.tsv`: 10,793줄(머리말 포함), 6칸 아닌 줄 0, 동국제강 30분 4,200원 포함, 키 문자열 0
- **T-29 요청 (Codex)**: 계획 Task 9(시연 장면 E)와 아래 카드 수정 1건
  - **여행 목록이 잘린다.** 캡처에서 다섯 줄이 모두 "문화가 흐르는 서울광장 · 축제 …"처럼 말줄임표로 끝난다.
  - `OverlayCard`의 여행 카드를 이렇게 바꾼다.
    - 왼쪽 원을 빼고 글자에 카드 폭을 다 쓴다.
    - 항목마다 두 줄을 둔다. 첫 줄은 이름 26sp 흰색 1줄, 둘째 줄은 `"{종류} · {거리}"`와 행사면 `" · {기간}"`을 20sp `#C9D1D9` 1줄로 쓴다. 항목 사이는 12dp 띄운다.
    - 최대 5개는 그대로다.
  - 시연 E 캡처(`docs/demo/overlay-e-travel.png`)에서 다섯 항목의 이름이 잘리지 않는지 확인한다.

## 2026-10-29 15:15 · Codex · T-27 관광·주차 파서와 실시간 연결 리뷰 요청

- 한 일: Task 5는 **`8f222a6`**(관광 JSON·주차 TSV 파서와 테스트·응답 본문만 담은 fixture), Task 6은 **`d1b2565`**(목적지·관광·주차 자산 연결과 실시간 캡처)에 커밋했다. 관광 오류/빈 목록/객체 한 건/깨진 행/제외 종류·행사 날짜, TSV 빈 요금·깨진 행을 처리하고 기록된 장면 E 문장을 그대로 검증했다.
- 연결: `onRouteStarted`와 ROUTE 진입에서 목적지를 조회하고 ROUTE 동안 5초마다 갱신한다. 좌표 5자리의 Locale.ROOT 키·취소 시 null·기어 P를 Feed/엔진에 전달한다. 관광은 목적지 좌표 3자리의 위치 목록과 서울 기준 도착일의 전국 행사 목록을 각각 한 번 호출하며 두 응답이 모두 성공해야 합쳐 전달한다. 주차는 TSV를 한 번 읽고 500m 안만 고른다. ReplayFeed의 목적지/기어 사건은 계획대로 T-29에 남겼다.
- 검증: 순수 함수·파서의 구현 전 실패를 확인한 뒤 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --console=plain` 성공, **앱 14개·core 130개(실패·오류·skip 0)**. 좌표와 날짜 인자를 독일어 Locale에서도 검사했고 기존 시연 A~C 테스트도 유지했다. 최종 `git diff --check`를 확인하고 각 커밋 직전 staged stat에서 자기 파일만 확인했다. RED/GREEN/최종 빌드 로그는 gitignore 대상 `.gradle/t27-*`에 있다.
- 실측: `emulator-5556`에서 LIVE·여행 모드를 켜고 내비 UI로 **서울시청**을 검색/선택했다. 목적지 키 **`37.56697,126.97851`**, 관광 HTTP **2회**(한도 10회 이내), 명소 **66건**·행사 **69건** 수신, `speak=목적지 근처에서 …` **1회**를 확인했다. 약 5초 목적지 갱신과 [P/0 여행 목록 카드](demo/overlay-travel-live.png)(2560×1440)를 확인했다. debug의 `logOnlySpeech=true`와 내비 음소거를 사용했으며 **FATAL EXCEPTION·TTS onStart 0건**이다. 키 값은 출력하지 않았고 URL은 `***`로 가렸다.
- 자산: T-28의 실제 TSV **10,792건·835,751 bytes**를 덮어쓰거나 이 작업에 스테이징하지 않았다. 관광 실측 뒤 최종 APK를 다시 빌드해 포함된 TSV가 저장소 자산과 바이트 단위로 같음을 확인했다(SHA-256 `740159bd0f2905b3733b0b8284da463f2c273f9e4c4412ca156de2e8ee31e8b2`). 이 실측은 여행 안내 확인이며 최종 자산으로 주차 카드를 띄우는 시험·실주행은 하지 않았다. 빈자리는 항상 null이며 서울 실시간 연결은 T-26/Task 8에 남아 있다.
- 복원: 경로 취소 후 내비의 검색 대기 화면을 확인했고 내비 음소거·앱 설정(시연 C, 여행 모드 OFF 등)을 원래 값으로 돌렸다. **P/0, 앱 force-stop, 다른 앱 위 표시 OFF, 서비스·카드 창 없음**을 재확인했다. GPS·시스템 음량을 변경하지 않았고 push하지 않았다. `.gradle/t27-live-proof.json`, `t27-live.log`, `t27-restored.json`에 근거가 있다.
- SDK 근거: 공식 [목적지 조회](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations)와 [이벤트 리스너](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners)를 확인했다. 명시적 조회 후 `onDestinationInfo`를 받으며 구현별 콜백 차이를 고려해 T-23에서 관측되지 않은 `onDestinationChanged`에 의존하지 않는다.
- 남은 문제/요청: 실제 긴 행사·명소 이름에서는 기존 여행 카드의 한 줄 배치가 제목 뒤 종류·거리·기간을 말줄임한다(캡처 첫 네 줄). `OverlayCard.kt`는 이번 범위 밖이므로 그대로 두었고 Claude가 화면 수정 작업을 정해 주기 바란다. Claude가 Task 5~6을 리뷰한 뒤 T-29를 이어 간다. TASKS의 T-27을 리뷰대기로 바꿨다.

## 2026-10-29 15:07 · Codex · T-28 TS 주차장 이름·요금 자산 리뷰 요청

- 한 일: 계획 Task 7의 `tools/make_parking_asset.py`와 `app/src/main/assets/parking/lots.tsv`를 **`fefcc84`**에 커밋했다. 기존 전국 시설 18페이지를 재사용하고 운영정보 `PrkOprInfo_v1`을 페이지당 100,000행으로 **18페이지** 받았다. 이름·국내 좌표·정수 기본시간/요금·0원 무료 조건을 적용하고 양쪽 데이터의 중복 ID는 첫 행을 썼다. ID 순 정렬과 이름의 탭·줄바꿈 정리를 포함한다.
- 자산: **10,792건·835,751 bytes(5MB 이내)**. 동국제강 **`04540-11291-00001-00-1` / 30분 / 4,200원**을 확인했다. 0원 행 **7,013건**은 모두 원본의 첫 시설 행에서 `parkng_chrge_se="1"`임을 별도 대조했고 음수 요금은 0건이다. SHA-256은 **`740159bd0f2905b3733b0b8284da463f2c273f9e4c4412ca156de2e8ee31e8b2`**다.
- 검증: `py -3.12 tools/make_parking_asset.py --self-test`에서 구현 전 실패를 확인한 뒤 결합·제외·중복·정렬·이름 정리·키 가림 검사가 통과했다. 실제 `--raw spikes/parking-travel/.gradle/raw` 실행 후 **네트워크 호출을 차단한 재생성**에서도 운영 캐시 18개를 재사용했고 SHA-256이 같았다. TSV 6열·고유/정렬 ID·좌표 범위·양의 기본시간·정수 요금·동국제강·키 미노출을 검사했다. 로그·검증 스크립트는 gitignore 대상 `.gradle/t28-*`에 있다.
- 다운로드: 10페이지에서 비JSON 응답 1회로 중단됐으나 성공한 1~9페이지를 보존해 재실행했고 남은 페이지를 정상 받았다(**총 19회 요청, 성공 18페이지**). 원본은 `spikes/parking-travel/.gradle/raw/parking-operation-national-page-*.json`에만 저장했으며 키는 curl stdin으로 전달하고 저장 URL은 `***`로 가렸다. 원본을 커밋하지 않았고, 커밋 직전 staged stat에서 도구·자산 두 파일만 확인했다.
- 남은 문제/요청: 요금은 수집 시점의 정적 자료이며 갱신하려면 운영 캐시를 새로 준비해야 한다. Claude가 Task 7을 리뷰하고 T-27에서 이 자산을 사용한다. TASKS의 T-28은 리뷰대기로 바꿨다. 다른 작업자의 코드·변경은 보존했고 에뮬레이터·TTS·push는 사용하지 않았다.

## 2026-10-29 17:00 · Claude · T-23 리뷰 완료, 2부 계획, T-27·T-28 요청

- T-23 리뷰: 보고서 1~8절과 장면 D·E 표본을 확인했다. 목적지 감지는 `onRouteStarted` → `getDestinationInfo()`, 관광은 2회 호출(위치 목록 + 전국 날짜 행사)로 정했다.
- **사용자 결정(2026-10-29): 빈자리 숫자는 서울 열린데이터광장 시영주차장 실시간 API에서만 쓴다.** TS 실시간 빈자리는 위치를 아는 487곳이 10분 동안 거의 바뀌지 않아 쓰지 않는다. TS에서는 이름·좌표·숫자 요금만 쓴다. DESIGN 10·11절을 고쳤다.
- 계획 2부: [plans/parking-travel.md](plans/parking-travel.md) Task 5~9. Task 8(서울 실시간)은 T-26 실측 뒤 덧붙인다.
- 요청
  - **T-27 (Codex)**: 계획 Task 5~6. `core/`, `app/`
  - **T-28 (Codex)**: 계획 Task 7. `tools/make_parking_asset.py`와 `app/src/main/assets/parking/lots.tsv`만 바꾼다. T-27이 같은 파일을 머리말 한 줄로 먼저 만들 수 있으니, 커밋 직전에 다시 읽고 덮어쓴다.
  - **T-29**: T-27 뒤에 시작한다.
  - **T-26**: 사용자가 서울 키를 받은 뒤 시작한다.

## 2026-10-29 13:23 · Codex · T-23 추가 실측: 실시간 값 변화 3회

- 한 일: 전국 `PrkRealtimeInfo_v1`(`numOfRows=5000`)만 **3회** 호출했다. 요청 간격은 **300.012908초·299.988086초**, 매번 같은 고유 ID **4,632개**였다. [보고 8절](spikes/parking-travel.md#8-실시간-값-변화), `summary.json`의 `realtimeVariation`, [전체 ID별 세 값·기존 487개 집합](spikes/parking-travel/realtime-variation-values.json)을 남겼다.
- 결과: **1→2 / 2→3 / 1→3**에서 전체 변화 **326 / 315 / 411개**(**7.04 / 6.80 / 8.87%**), 기존 이름·좌표 **487개 중 1 / 0 / 1개**(**0.21 / 0.00 / 0.21%**)였다. 장면 D 동국제강은 **8→8→8면**이었다.
- 검증: 오프라인 분석 재계산·변화/복귀/누락/분모 0 자체 검사, 원본-발췌 해시·전체 값 대조, 키 미노출 검사와 `git diff --check`를 확인했다. 주차 카운터 **43→46**, 관광 **28 유지**이며 다른 API·에뮬레이터는 사용하지 않았다. 자기 파일만 커밋하며 push하지 않는다.
- 남은 문제/요청: 일부 값이 변하지만 생산시각은 여전히 없어 **10분 신선도를 검증한 결과가 아니다**. 미변화가 미갱신을 뜻하지도 않는다. Claude가 이 수치로 기존 신선도 기준을 판단한다.

## 2026-10-29 13:22 · Codex · T-25 위험 카드 문장 잘림 수정 재리뷰 요청

- 수정: `2efbc56`에서 위험 텍스트의 첫 `". "`를 기준으로 마침표를 포함한 제목(34sp 흰색·최대 2줄)과 나머지 보조 문장(26sp `#C9D1D9`·최대 2줄)을 나눴다. 문장이 하나면 제목만 그린다. 위치·너비·core는 그대로다.
- 검증: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` 성공, 앱 **11개** 통과. debug/`logOnlySpeech=true`의 [안개 D/30](demo/overlay-c-fog-d30.png)·[공사 D/30](demo/overlay-c-construction-d30.png)·[살얼음 D/30](demo/overlay-c-ice-d30.png) 원본 캡처를 직접 확인해 세 문장 모두 말줄임·잘림 없이 보인다. [안개 P/0](demo/overlay-c-fog-p0.png)도 갱신했다. 가로 범위 1824~2384px로 주소 칩·버튼 열 간격을 유지하며 4장 모두 2560×1440이다. `FATAL EXCEPTION`·`TTS onStart`는 0건이다.
- 복원/요청: **P/0, 앱 force-stop, 다른 앱 위에 표시 OFF, 서비스·카드 창 없음**을 재확인했다. GPS·오디오·다른 작업자 변경은 건드리지 않았고 push하지 않았다. Claude가 문장 배치 1건을 재리뷰한다. 로그·측정은 gitignore 대상 `.gradle/t25-hazard-*`에 있다.

## 2026-10-29 16:20 · Claude · T-25 재리뷰: 위치는 좋음, 위험 카드 글자 수정 1건

- 확인: [C 안개 D/30](demo/overlay-c-fog-d30.png)에서 카드가 주소 칩과 내비 버튼 열 사이에 들어갔다. 위치는 좋다.
- 수정 요청 (같은 T-25 작업자, `OverlayCard.kt`만)
  - **위험 카드가 잘린다.** "2.5km 앞 안개 구간이에요. 앞이 500m도 …"처럼 가장 중요한 뒷부분이 말줄임표로 사라진다.
  - 바꿀 것: `Card.Hazard.text`를 첫 문장과 나머지로 나눈다. 첫 `". "` 앞까지(마침표 포함)가 첫 문장이다.
    - 첫 문장: 제목 34sp 흰색 최대 2줄. 예: "2.5km 앞 안개 구간이에요."
    - 나머지: 보조 26sp `#C9D1D9` 최대 2줄. 예: "앞이 500m도 안 보여요. 속도를 줄이세요."
    - 문장이 하나뿐이면 제목만 쓴다.
  - core는 바꾸지 않는다.
  - 시연 C의 안개·공사·살얼음 D/30 캡처를 다시 찍어, 세 문장 모두 잘리지 않는지 확인한다.

## 2026-10-29 13:16 · Codex · T-25 카드 위치 수정 재리뷰 요청

- 수정: `41c99ec`에서 `OverlayCard.kt`의 너비만 **560dp**, x만 **176dp**로 바꿨다(y=40dp 유지). [A P/0](demo/overlay-a-p0.png)·[A D/30](demo/overlay-a-d30.png)·[C 안개 P/0](demo/overlay-c-fog-p0.png)·[C 안개 D/30](demo/overlay-c-fog-d30.png)·[C 살얼음](demo/overlay-c-ice-d30.png)·[설정 버튼 확인](demo/overlay-touch-through.png) 6장을 갱신했다.
- 검증: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` 성공, 앱 11개 테스트 통과. 기존 창 `[1920,987][2520,1260]`의 버튼 열 겹침을 확인한 뒤 새 A 창 **`[1824,987][2384,1260]`**, C 창 **`[1824,1049][2384,1260]`**를 실측했다. 주소 칩과 **45px**, 버튼 열과 **29px** 간격이 있고 캡처에서도 둘 다 보인다. 여섯 창 모두 560px·alpha 0.8·visible, PNG 2560×1440, 무음 FGS를 검사했고 `FATAL EXCEPTION`·`TTS onStart`는 0건이다.
- 복원: debug와 `logOnlySpeech=true`만 사용했다. 최종 **P/0, 앱 force-stop, 다른 앱 위에 표시 OFF, 서비스·카드 창 없음**을 확인했다. GPS·오디오는 바꾸지 않았고 push하지 않았다. 측정·복원 근거는 gitignore 대상 `.gradle/t25-position-*`다.
- 요청: Claude가 위치 수정 1건을 재리뷰한다. 다른 앱 코드와 core는 수정하지 않았고 실차·다른 화면 배치는 검증하지 않았다.

## 2026-10-29 15:45 · Claude · T-25 리뷰: 수정 1건 요청

- 확인
  - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` 성공
  - `OverlayCard.kt`·`RoadAheadService.kt`를 읽었다. 창 규칙, 예외 처리, 권한 끔 처리, logOnlySpeech 제한이 계획과 맞다.
  - [D/30 캡처](demo/overlay-a-d30.png)에서 내비 위 카드와 "좌회전 신호 30초"를 확인했다.
- 수정 요청 (같은 T-25 작업자)
  - **카드가 내비 오른쪽 끝의 음량·설정 버튼 열을 가린다.** 캡처 기준으로 그 열은 x 약 2413~2511px, y 약 1075~1274px다. 터치는 통과하지만 보이지 않으면 완성품처럼 보이지 않는다.
  - 바꿀 것
    - `x`를 `dp(176)`으로 해서 카드 오른쪽 끝이 그 열 왼쪽에 오게 한다.
    - 너비를 `dp(560)`으로 줄여 왼쪽의 주소 칩(x 약 1606~1779px)과도 겹치지 않게 한다.
    - `y`는 그대로 둔다.
  - 시연 A의 P/0·D/30과 시연 C 안개 캡처를 다시 찍어 `docs/demo/overlay-*.png`를 갱신하고, 버튼 열과 주소 칩이 모두 보이는지 확인한다.
- 참고: 커밋 3a20079에는 이 항목 대신 T-23의 미커밋 HANDOFF 보고(13:07)가 실수로 들어갔다. 내용은 T-23이 쓴 그대로다.

## 2026-10-29 13:07 · Codex · T-23 주차·관광 API·목적지 콜백 실측 리뷰 요청

- 한 일: [실측 보고 7개 항목](spikes/parking-travel.md), [API 명세 5·6절](APIS.md), [집계 JSON](spikes/parking-travel/summary.json), [D 표본](spikes/parking-travel/samples/scene-D.json), [E 표본](spikes/parking-travel/samples/scene-E.json), [내비 타임라인](spikes/parking-travel/navi-timeline.txt)을 추가했다. 시험 도구·별도 무음 NaviHelper 앱은 `spikes/parking-travel/`이며 제품 앱·core·DESIGN은 수정하지 않았다. 본체 커밋은 `2fa4c44`다.
- 주차: 최신 기능명은 `Parking_v1/PrkSttusInfo_v1`·`PrkOprInfo_v1`·`PrkRealtimeInfo_v1`이다. 전국 총계 1,737,445에 해당하는 18페이지를 실제 수집한 본문 합계는 **683,877,454 bytes**, 실제 **1,733,740행·고유 1,733,155개**로 총계와 차이가 있다. 이름·유효 좌표의 고유 **305,896개**를 ID/이름/좌표만 직렬화한 gzip은 **7,014,971 bytes**이며 요금·지역코드를 붙인 최종 자산 크기는 아니다. 서버 상한은 문서에 없고 페이지 크기 100,000까지 동작했다.
- 주차 제한: 실시간 **4,632개**에 갱신시각이 없어 **10분 신선도 비율 산출 불가**다. 관리번호 인자는 유효/가짜 값 모두 무시해 개별 조회가 되지 않는다. 실시간→전국 시설 ID 매칭 **878/4,632(18.96%)**, 이름·유효 좌표까지는 **487개**다. 중구 실시간 42·강남 28·울산 남구 0, 운영 기본요금 숫자는 각 72/3,609·62/17,084·61/10,633뿐이다. 금액 공백을 무료로 바꾸면 안 된다. 운영정보에 필드가 있어 조건부 전국주차장표준 API는 호출하지 않았고 두 체계 간 매칭률은 미측정으로 명시했다.
- 관광: 호출 **28회(50회 이내)**. 위치 목록은 거리순·종류·반경이 동작하지만 행사 기간은 없고, 행사 목록에는 기간·좌표가 있으나 거리는 없다. 같은 날짜에 기존 `areaCode=1`은 서울 0건인데 **새 `lDongRegnCd=11`은 19건**, 시청 5km 안 오늘 기간 포함은 **14건**이었다. 울산은 새 코드로도 오늘 0건이다. 전 종류 5km 목록 1회 + 날짜 행사 목록 1회 후 클라이언트 필터링이면 표본에서는 최대 2회에 맞는다.
- 내비: SDK 경로 요청/검색 목적지 선택/즐겨찾기에서 `onRouteStarted`까지 각각 **5,847/5,979/5,961ms**(정차 단일 표본), 동일 목적지 재선택 첫 시험은 **60,543ms**였다. `onDestinationChanged`는 관측하지 못했다. 상세 자동 반복도 관측되지 않았고 명시적 `getDestinationInfo()` 3회는 **1,011/1,010/1,012ms** 뒤 이름·주소·좌표·거리·시간을 반환했다. `cancelRoute` 뒤 **258ms에 FREE_DRIVE**, **297ms에 빈 목적지**였다. 경로 시작과 동시에 onRouteEnded가 오고 ROUTE_GUIDING에서도 isRouting=false여서 이 값만으로 종료를 판정하면 안 된다. 실제 주행·Gleo는 미검증이다.
- 시연: D는 시청에서 **292.11m 동국제강, 빈자리 8/13면, 30분 4,200원**의 실제 응답이다(시각 미제공이라 10분 조건 통과로 표시하면 안 됨). E는 시청에서 **185.79m 문화가 흐르는 서울광장, 2026-05-06~12-31**로 측정일 기간 포함이다. 개별 공연의 당일 시간표·현장 주차 가능성은 검증하지 않았다.
- 검증: 별도 `.\gradlew.bat :app:assembleDebug --console=plain` **33개 작업 실행·BUILD SUCCESSFUL**, `probe_api.py --self-test`, `verify.py`의 발췌 해시·키 미노출·거리·날짜·ID·타임라인 지연·관광 한도 검증 통과, Markdown 상대 링크·`git diff --check` 통과. API는 주차 **43회**·관광 **28회**, 키를 인자·터미널·로그·커밋에 출력하지 않았다. 대용량 키 제거 원본은 gitignore 대상 `spikes/parking-travel/.gradle/raw/`, 커밋 응답 발췌는 약 2.85MB다.
- 복원: T-21 종료 회신 후 내비 UI만 썼고 기어·속도·GPS는 변경하지 않았다. 내비 음소거로 시험해 소리 나는 TTS를 호출하지 않았으며, 경로 취소·빈 목적지·시험 앱 force-stop·시험 즐겨찾기 삭제·음소거 복원 후 **에뮬레이터 사용 끝**을 coordinator에게 보냈다. 다른 작업자의 staging을 보존해 `commit --only`로 자기 파일만 커밋했고 push하지 않았다.
- 요청: Claude가 **10분 기준의 처리**, 실시간 지원 범위/전국 매칭 제한, 새 법정동코드 사용, **경로 확정 이벤트 + 명시적 목적지 조회/폴링**을 설계에 반영할지 결정한다. 주차 검색은 필수 정적 자산(후보 1) + 지역별 실시간 목록 결합을 권고하며, 요금·지역코드 포함 최종 크기는 후속 자산 제작에서 검증한다. TASKS의 T-23은 리뷰대기로 바꿨다.

## 2026-10-29 13:04 · Codex · T-25 내비 위 카드·foreground service 리뷰 요청

- 한 일: 계획 Task 4대로 입력·피드·250ms 엔진 루프를 `RoadAheadService`의 `CoroutineScope(SupervisorJob() + Dispatchers.Main)`으로 옮겼다. Activity는 서비스의 StateFlow 상태와 설정만 보여 주며 내비를 앞으로 가져와도 루프를 유지한다. `specialUse` 서비스·권한·무음 `road_ahead` 알림(끄기 동작)을 추가했다. 모드·방향·여행 모드 변경은 피드를 재시작하고 차량 연결은 유지한다.
- 카드/설정: T-24 Task 3 커밋 `d605d46` 이후 `Screen.card`를 연결했다. 신호·위험·주차·여행을 Android View 한 창으로 그리며 같은 내용은 다시 구성하지 않는다. 너비 600dp, 오른쪽/아래 40dp, alpha 0.8, 비포커스·비터치이며 계획의 색·크기·글자 크기를 적용했다. null 카드는 숨기고 창 추가·갱신·제거 예외를 처리한다. 권한이 없으면 서비스를 시작하지 않고 안내와 앱 정보 버튼을 보여 준다. 여행 모드는 `travelMode` 키/기본 false로 저장하며 64dp·24sp 행이다. `Inputs.parked`는 CarSignals에서, travelMode는 설정에서 채우고 destination은 계획대로 null이다.
- 빌드/단위 검증: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test :app:lintDebug --console=plain` → **BUILD SUCCESSFUL**. 앱 **11개**, core **116개**가 실패·오류·건너뜀 없이 통과했고 기존 DataEvent 오류/취소 테스트도 유지했다. Lint 오류는 없고 기존 의존성 갱신·앱 아이콘·backup 설정 경고만 있다. `git diff --check`도 통과했다. 최종 로그·측정 스크립트·window/FGS 근거는 gitignore 대상 `.gradle/t25-*`, `.gradle/overlay-*.json`에 있다.
- 에뮬레이터/설치: T-23 종료 및 Claude의 사용 가능 회신 후 `emulator-5556`, user 10에서 시험했다. release/debug 서명 불일치로 덮어쓰기가 거부되어 **Claude 승인 후** 기존 시험용 release 설치를 제거하고 debug APK를 설치했다. `apk/`의 release 파일은 그대로다. 위치·차량 속도 권한을 허용하고 앱 정보 버튼 → Pleos 설정의 다른 앱 위에 표시 스위치로 권한을 켰다. 첫 권한 OFF 상태에서는 서비스가 없었다.
- A 결과: 내비가 앞에 있는 상태에서 [P/0](demo/overlay-a-p0.png), [D/30](demo/overlay-a-d30.png) 카드가 유지됐다. 서버 창 속성은 `(40,40)(600xwrap)`, `APPLICATION_OVERLAY`, `alpha=0.8`, `shown=true`다. `--ez logOnlySpeech true`에서 `speak=`는 **앞 신호 빨간불이에요. 속도를 줄이세요. → 좌회전 신호까지 32초 남았어요. → 곧 좌회전 신호로 바뀌어요.** 순서였다. 카드 안 `(2460,1220)`의 내비 설정 버튼을 tap하자 [설정 화면](demo/overlay-touch-through.png)이 열려 터치 통과를 확인했다. FGS는 `isForeground=true`, `types=40000000`, `sound=null`, `vibrate=null`이었다.
- C/권한 결과: [안개 P/0](demo/overlay-c-fog-p0.png), [안개 D/30](demo/overlay-c-fog-d30.png), [살얼음 D/30](demo/overlay-c-ice-d30.png)에서 주황 원·흰 본문 최대 2줄을 확인했다(긴 문장은 말줄임). [처음 권한 없음](demo/overlay-permission-off.png)과 [실행 중 권한 해제 후](demo/overlay-permission-revoked.png) 모두 안내 문장·앱 정보 버튼이 보였다. 해제 전에는 FGS가 실행 중이었고, 해제 후 서비스·카드 창이 없어졌으며 해당 앱 로그의 `FATAL EXCEPTION`과 `TTS onStart`는 0건이었다. 모든 PNG는 2560×1440 원본이다.
- 복원/범위: 마지막 HTTP 상태 **Park/0 km/h**, 앱 force-stop, 서비스·RoadAheadCard 창 없음, 오버레이 권한 OFF다. debug 설치와 시연 C/여행 OFF 설정은 남아 있다. GPS·다른 차량 속성·오디오는 바꾸지 않았고 키 값을 출력하거나 소리 나는 TTS를 재생하거나 push하지 않았다. T-22·T-23·T-24의 파일과 공용 문서의 다른 항목은 보존했다.
- 남은 문제/요청: Claude가 Task 4와 캡처 8장을 리뷰한다. 실제 주차·관광 응답과 목적지 연결·시연 D/E는 계획 2부이며 이번에는 연결하지 않았다. 주차·여행 View 분기의 실제 자료 표시, 실차·물리 터치·장시간 생존은 미검증이다. 제출용 release APK 갱신도 이번 범위 밖이다. T-25는 리뷰대기로 바꿨다.

## 2026-10-29 12:58 · Codex · T-24 목적지 일시 누락 수정 리뷰 요청

- 수정: `selectDestination`은 null 키를 무시하고 다른 null이 아닌 키가 올 때만 초기화한다. 재탐색 중에는 요청 기록·주차/관광 목록·발화 기록·여행 표시 시간을 보존하며, 목적지가 없는 틱에는 주차·여행 카드와 안내를 내지 않는다. 코드 수정은 `RoadAheadEngine.kt`와 그 테스트 파일 두 개뿐이다.
- 검증: `destination briefly missing keeps state`를 먼저 추가하고, null을 취소로 보던 기존 테스트를 기억한 목적지의 응답을 보존하는 경우로 바꿨다. `.\gradlew.bat :core:test --console=plain`에서 **116개 중 해당 2개만 실패**한 뒤 수정해 **116개 모두 통과(실패·오류·건너뜀 0)**했다. A 요청·발화 → null 요청/틱 여러 번 → A 복귀 시 재요청·재발화 없음, 기존 여행 목록의 60초 만료, 누락 중 수신한 응답 보존, 기존 A→B→A 재발화를 확인했다. 로그는 `.gradle/t24-review-null-red.log`, `t24-review-null-green.log`다.
- 남은 문제/요청: Claude가 수정 1건을 재리뷰한다. T-24는 리뷰대기로 복원했다. 앱·SDK·에뮬레이터는 이번에 검증하지 않았고 다른 작업자 변경·API 키·TTS·push는 건드리지 않았다.

## 2026-10-29 15:20 · Claude · T-24 리뷰: 수정 1건 요청

- 확인
  - `.\gradlew.bat :core:test --rerun-tasks` → 115개 통과, 실패 0
  - `ParkingAdvisor`·`TravelAdvisor`·`Korean.kt`·엔진의 카드 순서·`Announcer.forget`을 읽었다. 계획과 맞다.
- 수정 요청 (같은 T-24 작업자)
  - **목적지가 잠깐 비면 상태가 초기화된다.** `selectDestination(null)`이 요청 기록·목록·여행 표시 상태를 지우고 키 기록도 잊는다.
  - 실제 내비는 재탐색 중에 목적지를 잠깐 비운다(T-05 9번: "재탐색 중 빈 목적지 0으로 잠시 변하고"). 그러면 같은 목적지가 돌아올 때 주차·여행 안내를 한 번 더 말한다.
  - 바꿀 것: `null`은 "바뀜"으로 보지 않는다. null이면 상태를 그대로 두고, 다른 **null이 아닌** 키가 올 때만 초기화한다. 카드와 안내는 지금처럼 `inputs.destination`이 null이면 나오지 않는다.
  - 테스트 `destination briefly missing keeps state`를 추가한다: A 요청·발화 → null tick 몇 번 → A tick → 다시 요청되지 않고 다시 말하지 않는다. A→B→A 재발화 테스트는 그대로 통과해야 한다.

## 2026-10-29 12:53 · Codex · T-24 주차·여행 판단 로직 리뷰 요청

- 한 일/커밋: 계획 Task 1~3을 `e68dd4f`(주차 안내·공용 타입), `121fac1`(여행 행사·명소), `d605d46`(목적지·요청·카드 선택 엔진)으로 나눠 커밋했다. `Destination`, `ParkingLot`, `TourItem`·`TourKind`, `ParkingCard`, `TravelLine`, `Card`와 조사·요금·날짜 함수를 추가했다. `Screen.card`, `Inputs.parked/travelMode/destination` 및 새 EngineConfig 값에는 기존 호출을 유지하는 기본값을 뒀다. T-25가 사용할 Task 3 커밋은 Orca heartbeat로 조정자에게 알렸다.
- 주차/여행: 주차는 ROUTE, 1,000m 또는 180초, 100m 이내 버림을 적용한다. 목적지 500m 안의 새 빈자리(수신 시각 기준 10분 이내)를 먼저 고르고, 없으면 오래됐거나 시각 없는 곳에서 숫자를 빼고 안내한다. 새것이면서 0면인 곳은 고르지 않는다. 여행은 서울 도착 예정일의 행사 5km/2개, 명소 3km/3개를 거리순으로 고르며 계획의 문장과 조사·요금 표기를 쓴다.
- 엔진: 목적지별 주차·관광 요청을 각 한 번 내고, 현재 목적지와 다른 키의 늦은 응답은 버린다. 목적지 변경 시 요청 표시·목록·여행 타이머를 초기화한다. 처음 응답이 tick보다 먼저 오면 그 키를 받아들이고, A→B→A도 새 요청으로 처리한다. 여행 음성은 주행 중에도 후보가 되고 목록은 P에서 처음 표시한 단조 시각부터 60초만 보이며 D로 나가면 같은 목적지에는 다시 열지 않는다. 카드 순서는 여행 목록 → HIGH 위험 → 신호 → 나머지 위험 → 주차다. 기존 signal/hazardText/호출 예산과 음성 우선순위는 유지했다.
- 조정자 승인 범위: `koFee` 자체에는 마침표를 넣지 않고 완성 안내문에는 마침표를 붙인다. 60초 안 A→B→A에서도 다시 안내하도록 **추가 승인받은 Announcer 파일 2개**에 `forget(key)`와 회귀 테스트를 추가했다. 목적지가 바뀔 때 이전 `parking:*`, `travel:*`만 잊고 신호·위험 발화 기록과 내비 음성 간격은 유지한다. 계획 파일은 수정하지 않았다.
- 검증: Task마다 테스트를 먼저 작성해 `:core:test`가 새 타입·함수 부재로 `compileTestKotlin`에서 실패하는 것을 확인한 뒤 구현했다. `forget` 미구현 실패도 별도로 확인했다. 최종 `.\gradlew.bat :core:test --console=plain` → **BUILD SUCCESSFUL, 115개 통과(기존 78개 + 새 37개), 실패·오류·건너뜀 0**. Task 1은 90개, Task 2는 98개였다. 새 테스트는 계획의 이름·값을 유지하며 10분 경계·반경·만차/모름·시간만 있는 목적지·도착 날짜·표시 개수·목적지 교체/취소/늦은 응답·A→B→A 재발화·신호/위험 중복 억제 보존·60초/출발 시 닫힘·카드 우선순위를 확인한다. 기존 엔진 테스트 본문이 그대로이고 core가 검증한 커밋과 일치함도 검사했다. `git diff --check`, 커밋 전 staged 범위·공백 검사도 통과했다. 로그·실행 기록은 gitignore 대상 `.gradle/t24-*`다.
- 남은 문제/미검증: core 범위의 알려진 실패는 없다. 구현자 자체 점검을 마쳤으며 Claude의 독립 리뷰를 요청한다. 앱 빌드·에뮬레이터·SDK·실제 API·시연 D/E 연결은 이번 작업에서 수행하지 않았다(T-25 및 계획 2부 범위). API 키 값을 출력하지 않았고, 소리 나는 TTS·push·에뮬레이터 조작도 하지 않았다. 다른 작업자의 app/video/spikes와 공용 문서의 다른 항목은 보존했다.
- 요청: Claude가 `113b44b..d605d46`의 **core 변경만** 계획 Task 1~3 및 Review Focus 1~4와 비교해 리뷰하고 T-24를 완료로 바꾼다. T-25는 `Card`를 그리는 작업을 이어 갈 수 있다. TASKS의 T-24만 리뷰대기로 바꿨다.

## 2026-10-29 12:49 · Codex · T-22 리뷰 수정 2건 완료

- 수정: 마무리와 `CUES.md`의 소개를 VIDEO.md의 새 문장으로 교체했다. 화면에서는 쉼표 뒤 두 줄(28px)로 표시한다. 회전·도착 정보 카드 배경을 `#182029` 불투명 색으로 바꿔 지도 이름이 비치지 않게 했다. `render.mjs`는 소개를 VIDEO.md·CUES.md와 비교하고, 표시 문장·두 줄 배치·카드 불투명도도 검사한다.
- 검증: 새 VIDEO.md 기준 검사에서 옛 소개로 실패한 뒤 수정해 `node video/render.mjs --check`가 통과했다. `node video/render.mjs`로 세 영상과 캡처를 갱신했으며 **10/72/5초·1920×1080·30fps·무음** 검증을 통과했다. A·E·마무리 캡처와 실제 WebM의 A 4.5초·마무리 3초를 확인했다. `node --check video/render.mjs`, `git diff --check -- video`도 통과했다.
- 갱신 캡처: [A](../video/frames/02-scene-a.png), [C](../video/frames/02-scene-c.png), [D](../video/frames/02-scene-d.png), [E](../video/frames/02-scene-e.png), [마무리](../video/frames/04-outro.png). 영상은 기존 `video/out/`에 있고 커밋하지 않는다.
- 남은 문제/요청: Claude가 수정 2건을 재리뷰한다. TASKS의 T-22는 리뷰대기로 되돌렸다. 다른 작업자 파일은 수정하지 않았으며 소리 재생·패키지 설치·push는 하지 않았다.

## 2026-10-29 14:40 · Claude · T-22 리뷰: 수정 2건 요청

- 확인
  - `node video/render.mjs --check` → 통과(5장면, 카드 규격, 자막 겹침, 외부 요청 0건)
  - `video/out/metadata.json` → 10초/72초/5초, 1920×1080, 30fps, 무음
  - 캡처 A·E를 직접 봤다. 카드·자막·경로·"콘셉트 영상" 표시 모두 좋다.
- 수정 요청 (같은 T-22 작업자)
  1. **마무리 한 줄 소개가 옛 문장이다**("내비 옆에서"). 기획 방향이 바뀌었다. [VIDEO.md](VIDEO.md) 4번에 적은 새 문장으로 바꾼다: "Pleos 내비 위 카드 한 장과 음성 한 번으로, 앞길의 신호·도로 위험부터 목적지 주차와 여행 정보까지 묻기 전에 알려 주는 내비 기능 개선 제안과 그 시제품 앱입니다." 줄바꿈이 필요하면 쉼표 뒤에서 나눈다. `CUES.md`의 한 줄 소개도 같은 문장으로 바꾼다.
  2. **회전 안내 카드 아래로 지도 글자가 비친다**. 장면 A·E 캡처에서 "시청 방면 좌회전" 아래에 흐린 지도 이름이 겹쳐 보인다. 회전 안내·도착 정보 카드가 덮는 자리에는 지도 이름을 두지 않거나, 카드 바탕을 불투명하게 한다.
- 수정 뒤 `node video/render.mjs`로 다시 렌더하고, `--check` 통과와 캡처 갱신을 보고한다.

## 2026-10-29 12:41 · Codex · T-22 장면 D·E 포함 최종 콘셉트 영상 리뷰 요청

- 한 일: 코디네이터의 추가 지시(`VIDEO.md`, 6c91748)에 따라 기존 A·B·C 뒤에 **D 주차 10초·E 여행 12초**를 이어 붙였다. D는 파란 원 안 흰 P, 중앙 공영주차장·빈자리 23면·목적지에서 180m·30분 1,000원과 도착 12:24 · 1.0km를 표시한다. E는 초록 원 안 어두운 깃발, 대본의 축제·문화시설·관광지 목록 3개, 기어 P 정차 상태를 표시하며 목적지 설정 직후 경로선이 1.8초에 걸쳐 새로 그려진다. 장소명·자막은 대본의 예시와 문장을 그대로 썼다.
- 최종 산출물: `video/out/01-intro.webm` **10.00초**, `02-concept.webm` **72.00초**(A 25+B 10+C 15+D 10+E 12), `04-outro.webm` **5.00초**. 모두 **1920×1080·30fps·무음**, 영상은 계속 gitignore 대상이며 커밋하지 않는다. 사용자 실제 동작 녹화 20초를 합치면 약 107초다. [video/CUES.md](../video/CUES.md)에 D·E의 자막 시작·종료, 목록 원문, 캡처·검사 설명을 추가했다.
- 검증: `node video/render.mjs --check` 성공. 기존 검증에 D의 주차 수치·도착 정보·파란 P 중앙 배치, E의 초록 깃발·목록 3개·세로 확장·새 경로 생성·정차(62초와 70초 지도 픽셀 동일), D·E 자막의 대본 일치·카드 여백·자막 겹침·가로 넘침 검사를 추가했다. `node video/render.mjs`를 다시 실행해 최종 세 파일 재생성과 10/72/5초·해상도·fps·오디오 없음 검사를 통과했다. 완성된 WebM도 Chromium에서 **54초 D·64초 E**를 직접 디코딩해 눈으로 확인했다. CUES의 캡처 링크 7개, `node --check video/render.mjs`, `git diff --check`도 통과했다.
- 최종 캡처 7장: [도입](../video/frames/01-intro.png), [A 신호 대기](../video/frames/02-scene-a.png), [B 경로 없음](../video/frames/02-scene-b.png), [C 도로 위험](../video/frames/02-scene-c.png), [D 주차](../video/frames/02-scene-d.png), [E 여행](../video/frames/02-scene-e.png), [마무리](../video/frames/04-outro.png). 기존 캡처 5장은 동일하며 D·E 2장을 추가했다.
- 남은 문제/범위: 실제 TTS 녹음·청음, 에뮬레이터 녹화와 최종 편집은 사용자 작업이다. 소리 재생·API 키 읽기/출력·push는 하지 않았다. 패키지 설치 없이 기존 도구를 썼고, 25→30fps는 프레임 복제다. 설치된 Playwright가 끝에 붙이는 1초 정지 화면에 맞춘 잘라내기 기준은 도구 버전 변경 시 다시 확인해야 한다.
- 요청: Claude가 A–E 영상·시각표·캡처를 리뷰한 뒤 T-22를 완료로 바꾼다. TASKS의 T-22만 리뷰대기로 되돌렸고 다른 작업자의 공용 파일 변경은 보존했다. 기본 구현은 0ee317b, 이번에는 D·E 확장만 별도 커밋한다.

## 2026-10-29 14:10 · Claude · T-21 리뷰 완료, T-24·T-25 요청

- T-21 검증: `.\spikes\overlay\gradlew.bat -p spikes/overlay :app:assembleDebug` 성공, 보고서와 캡처를 확인했다. 결론: P/0·D/30에서 내비 위 카드가 보이고, Pleos 설정에서 권한을 켤 수 있고, 창 alpha 0.8에서 아래 내비 터치가 통과한다. 그래서 DESIGN 12절의 오버레이 방향으로 간다.
- 사용자가 DESIGN 10~12절(주차, 여행 모드, 화면·일정)을 승인했다(2026-10-29).
- T-23 중간 보고: 실시간 주차 응답에 갱신 시각이 없다. 빈자리의 "10분 이내"는 앱이 받은 시각 기준으로 바꿨다(DESIGN 10절, 계획 Task 1).
- 요청
  - **T-24 (Codex)**: [plans/parking-travel.md](plans/parking-travel.md) Task 1~3. `core`만 수정한다.
  - **T-25 (Codex)**: 같은 계획의 Task 4. 서비스 옮기기(Step 1)는 바로 시작할 수 있다. 카드 그리기(Step 2)는 T-24 Task 3 커밋(`Screen.card`) 뒤에 한다. `app/`만 수정한다.
  - 2부(Task 5~7: 응답 해석, 실시간 연결, 시연 D·E)는 T-23이 끝나면 Claude가 계획에 덧붙인다.

## 2026-10-29 12:35 · Codex · T-21 내비 위 오버레이 실측 리뷰 요청

- 한 일: 별도 [spikes/overlay/](../spikes/overlay/) 프로젝트에 `com.roadahead.spike.overlay` 시험 앱을 만들었다. `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`, 300×150dp, 오른쪽·아래 24dp, alpha=0.8이다. DO 없는 MainActivity와 DO 메타데이터가 있는 DoActivity, 서비스 없는 창과 무음 `specialUse` foreground service를 비교했다. 제품 `app/` 및 T-22·T-23 관련 파일은 수정하지 않았다.
- 결과: [docs/spikes/overlay.md](spikes/overlay.md)에 7개 항목과 Android 공식 근거·재현 명령을 정리했다. **Pleos 설정 → 앱 → 앱 목록 → 시험 앱 → 다른 앱 위에 표시** 스위치로 권한 ON/OFF가 된다. 표준 `ACTION_MANAGE_OVERLAY_PERMISSION`은 처리 Activity가 없지만 `APPLICATION_DETAILS_SETTINGS`와 Pleos 수동 경로는 동작하므로 adb 전용이 아니다. appops default에서 OFF/ON에 따른 `canDrawOverlays=false/true`와 카드 표시를 확인했다([앱 목록](spikes/overlay/permission-app-list.png), [OFF](spikes/overlay/permission-ui-off.png), [ON](spikes/overlay/permission-ui-on.png)).
- 표시·터치: P/0과 D/30에서 카드가 내비 위에 유지됐다([P](spikes/overlay/p0-no-do-no-service.png), [D/DO 없음](spikes/overlay/d30-no-do-no-service.png), [D/DO 있음](spikes/overlay/d30-do-no-service.png)). 계기판도 30km/h, Car driving state 2, UXR DO=true/80이었다. 일반·DO 시험 Activity는 둘 다 주행 중 가림 화면이 됐지만 카드는 남았다. APK DO 메타데이터는 있고 시스템에 `not installed from permitted sources NULL` 경고가 있어 **DO가 실제 인정된 Activity와의 비교는 미확정**이다. 카드 안 (2460,1220)의 내비 설정 버튼에 adb tap을 주자 설정 화면이 열렸다([전](spikes/overlay/touch-before.png), [후](spikes/overlay/touch-after.png)). 서버 창/InputDispatcher의 실효 alpha는 0.8이며 물리 터치는 미검증이다.
- 수명·좌표: HOME·다른 앱 전환, **시험 Activity·서비스 모두 없는 상태에서 15초 이상** 창이 유지됐다. `specialUse` FGS도 Activity 종료 후 동작하며 `isForeground=true`, `types=40000000`, `sound=null`이다. 단기 표시에는 FGS가 필수는 아니었고 장기 생존은 미검증이다. 160dpi이므로 300×150dp=300×150px, 실제 카드는 `[2236,1126][2536,1276]`, 하단 바 140px를 제외한 아래·오른쪽 여백은 24px다. **멀티 윈도우에서 내비를 왼쪽 좁은 창으로 줄여도 카드는 오른쪽 다른 앱을 가린다**([캡처](spikes/overlay/multiwindow-nav-left.png)). 창이 내비에 자동 부착되는 방식이 아니다.
- 검증: `.\spikes\overlay\gradlew.bat -p spikes/overlay :app:assembleDebug --console=plain` → BUILD SUCCESSFUL. `py -3.12 -B spikes/overlay/measure.py` 전체 exit 0, 8개 상태의 PNG 크기·창 visible/shown·Console 기어/속도를 assert했고 종료 상태의 시험 Activity 부재를 추가 확인했다. 캡처 14장(모두 2560×1440), JSON 상태/서비스 근거, 보고서 로컬 링크, Python 구문, `git diff --check`를 검증했다. 공식 Android 14 FGS 유형·권한과 터치 제약은 결과 문서에 링크했다.
- 복원·자동 승인 검토: 자동 측정은 P/0으로 복원했다. 마지막 권한 OFF·종료·캡처를 묶은 adb 명령이 **`blocked by policy`**로 거부돼 우회하지 않고 Orca ask로 Claude에게 맡겼다(구체 사유 미제공). Claude가 앱 목록 캡처·권한 OFF·force-stop을 수행했다. Codex도 이후 읽기 전용으로 **Park/0, user 10 권한 false, appops default, 시험 창·서비스 없음, 2560×1440/160dpi 유지**를 확인했다([최종 상태](spikes/overlay/restored.json)). GPS와 다른 차량 속성·시스템 오디오는 조작하지 않았다. T-23 사용 시작 뒤에는 에뮬레이터를 더 조작하지 않는다.
- 남은 문제/범위: 실차의 권한·주행 정책, 물리 터치, 메모리 회수·장시간·재부팅, 제품 FGS 적합성, 내비 창 추적은 미검증이다. 외부 공공 API 호출·키 값 출력·소리 나는 TTS 재생·push는 하지 않았다. 시험 앱 설치는 남겨 두었고 창과 권한은 껐다.
- 요청: Claude가 7개 항목을 리뷰하고 내비 창에 맞춘 표시 정책을 결정한다. 새 Pleos 사실의 `docs/PLEOS_SDK.md` 반영도 Claude에게 요청한다(해당 파일은 T-21 관련 파일 범위 밖이라 수정하지 않음). TASKS의 T-21은 리뷰대기이며 자기 행만 선별 커밋한다. T-22·T-23의 작업 트리 상태 변경은 그대로 보존했다.

## 2026-10-29 12:32 · Codex · T-22 콘셉트 애니메이션 리뷰 요청

- 한 일: `video/concept.html` 한 장에 외부 리소스 없이 열리는 지도·도로·건물·경로선·움직이는 차와 장면 A/B/C, 도입·마무리를 구현했다. `Malgun Gothic`, 오른쪽 아래 카드 너비 450px·여백 30px·반경 24px·지정 패딩/색/글자 크기를 적용했다. 콘셉트 표시는 컷 전환 중에도 유지하고, 자막은 카드와 겹치지 않는 하단 130px에 둔다. 대본 자막 문장은 그대로이며 신호는 1초마다 감소하고 초록 전환·좌회전 후 카드가 사라진다. 장면 A의 시간 압축 컷과 위험별 출처도 시각표에 기록했다.
- 산출물: [video/CUES.md](../video/CUES.md)에 파일별 자막 시작·종료, 재생·재렌더·검사 명령을 적었다. `video/out/01-intro.webm` **10.00초**, `02-concept.webm` **50.00초**, `04-outro.webm` **5.00초**가 생성되어 있다. 세 파일 모두 **1920×1080·30fps·무음**이며, `.gitignore`에는 지시대로 `video/out/` 한 줄만 추가했다. 영상과 `metadata.json`은 커밋하지 않는다.
- 도구: Node **24.18.0**, 기존 npm `_npx` 캐시의 Playwright, 기존 Chromium **1228**, 번들 FFmpeg **1011**만 사용했다. Playwright가 요구하는 새 Chromium이 없어 설치된 실행 파일을 찾도록 했으며 패키지는 설치하지 않았다. Playwright 원본 25fps는 프레임 복제로 30fps로 변환한다. 이 설치본이 끝에 붙이는 1초 정지 화면을 제외해 대본 시각을 맞췄다. Playwright 버전을 바꾸면 이 잘라내기 기준을 다시 확인해야 한다.
- 검증: `node video/render.mjs --check` 성공. VIDEO.md 자막 일치, README 한 줄 소개 일치, 45→44·32→31·5→4·1→초록, 좌회전 뒤 카드 사라짐, B의 경로 장식 숨김, 위험 3종/출처, 카드 실측·가로 넘침·자막 겹침·컷 중 콘셉트 표시·외부 네트워크 요청 0건을 검사했다. 최종 `node video/render.mjs`를 다시 실행해 세 파일 재생성·길이·해상도·fps·오디오 없음 검사를 통과했다. 생성된 WebM도 Chromium에서 직접 디코딩해 도입·A·B·C·마무리 프레임을 확인했고, A의 4.5초에서 카드와 자막 모두 32초임을 확인했다. `node --check video/render.mjs`, `git diff --check`, 출력 파일 gitignore 확인도 통과했다.
- 캡처 5장: [도입](../video/frames/01-intro.png), [장면 A](../video/frames/02-scene-a.png), [장면 B](../video/frames/02-scene-b.png), [장면 C](../video/frames/02-scene-c.png), [마무리](../video/frames/04-outro.png). 캡처는 커밋 대상이다.
- 남은 문제/범위: 실제 음성 녹음·청음, 3번 에뮬레이터 녹화와 최종 영상 편집은 사용자 작업이다. 이번에는 Android 앱을 수정하거나 빌드하지 않았고 API 키 파일을 읽거나 값을 출력하지 않았다. 소리 나는 TTS 재생과 push는 하지 않았다. 다른 작업자의 T-21 및 설계 변경은 수정하지 않았다.
- 요청: Claude가 영상 3개와 캡처·시각표를 리뷰한 뒤 T-22를 완료로 바꾼다. 사용자는 CUES 기준으로 실제 앱 TTS와 에뮬레이터 녹화를 합친다. TASKS의 T-22만 리뷰대기로 바꿨다.

## 2026-10-29 13:20 · Claude · T-23 요청: 주차·관광 API와 목적지 콜백 실측

사용자 결정(2026-10-29): 주차 안내를 먼저 만들고, 이어서 여행 모드를 만든다. 설계 초안은 [DESIGN.md](DESIGN.md) 10·11절이다. 사용자는 같은 공공데이터포털 계정으로 두 API의 활용 신청을 마쳤다. 인증키는 루트 `api` 파일의 `신호등` 줄과 같은 값이다.

**T-23 [시험용, 버릴 코드]** `spikes/parking-travel/`에 실측 도구를 둔다. 항목:
1. 한국교통안전공단 주차정보 API(`https://apis.data.go.kr/B553881/Parking_v1`)
   - 세 기능(시설정보 `PrkSttusInfo`, 운영정보, 실시간 정보)의 실제 경로·요청 변수·응답 필드
   - 필드: 관리번호, 이름, 좌표, 주차면 수, 실시간 빈자리, 갱신 시각, 요금
   - 실시간 정보를 관리번호로 조회할 수 있는지, 지역 단위로만 되는지
   - 지연 시간과 오류 코드
2. 같은 API의 범위
   - 전국 시설정보 총 건수와 페이지 크기 상한, 전부 내려받을 때의 크기(앱 자산으로 넣을 수 있는지)
   - 실시간 정보가 있는 주차장 수, 그중 갱신 시각이 10분 이내인 비율
   - 표본 지역: 서울 중구·강남구, 울산 남구
3. 요금
   - 운영정보에 기본시간·기본요금이 있는지
   - 없으면 전국주차장정보표준데이터 API의 경로·필드와, 두 데이터를 관리번호로 맞출 수 있는지(맞는 비율)
4. 한국관광공사 국문 관광정보(`https://apis.data.go.kr/B551011/KorService2`)
   - 위치기반 목록(`mapX`, `mapY`, `radius`, `contentTypeId`, 거리순 정렬)과 행사정보 조회(`eventStartDate`)의 실제 경로·변수·응답 필드
   - 행사에 좌표와 기간이 같이 오는지, 거리 필드가 있는지
   - 표본: 울산 태화강국가정원 주변, 서울시청 주변
   - **하루 1,000회 한도**이므로 이번 실측에서 50회 이내로 쓴다.
5. NaviHelper `onDestinationInfo`
   - 경로 안내 중 목적지 이름·주소·좌표·`remainDistance`·`remainTime`이 오는지, 얼마나 자주 오는지
   - 경로 종료(`cancelRoute`) 때 `NavigationStatus`가 어떻게 바뀌는지
   - **목적지를 새로 정한 순간** 우리 앱이 바로 알 수 있는지(사용자 결정 2026-10-29: 여행 모드는 목적지를 정하면 알려 준다). 내비 화면 검색으로 정할 때, 즐겨찾기로 정할 때, SDK `requestRoute`로 정할 때 각각 콜백 종류와 지연을 잰다. 음성(Gleo)으로 정하는 경우는 에뮬레이터에 마이크가 없어 시험하지 않는다.
   - 에뮬레이터 내비 차 위치는 성남에 고정이므로 성남 근처 목적지로 시험한다.
6. 시연 자료 후보
   - 장면 D: 실시간 빈자리가 실제로 오는 주차장이 500 m 안에 있는 목적지 1곳
   - 장면 E: 오늘 기간에 드는 행사가 5 km 안에 있는 목적지 1곳(없으면 가장 가까운 날짜)
   - 두 곳의 실제 응답을 키를 지운 표본으로 저장한다.
7. 권고
   - 목적지 주변 주차장 찾기 방식(DESIGN.md 10절의 후보 1·2 중)과 그 근거 수치
   - 설계 기준값(500 m, 10분, 5 km, 3 km)이 실제 자료에 맞는지

- 결과는 `docs/spikes/parking-travel.md`(동작/부분/불가·근거·수치)에 쓴다. API 명세는 `docs/APIS.md`에 키 없이 추가하고, 표본은 `docs/spikes/parking-travel/samples/`에 둔다.
- 키는 출력·로그·커밋하지 않는다. URL의 키는 `***`로 가린다.
- 소리는 내지 않는다. 에뮬레이터 내비 경로는 끝나면 취소한다.
- Codex 자동 승인 검토에 막힌 명령은 우회하지 말고 ask로 Claude에게 맡긴다.

## 2026-10-29 12:45 · Claude · T-21·T-22 요청: 오버레이 실측, 콘셉트 애니메이션

사용자 결정(2026-10-29): 시연 영상은 "완성된 제품처럼 보이는 모습"을 목표로 한다. 완성 모습은 "콘셉트 영상" 표시를 단 목업 애니메이션으로, 실제 동작은 에뮬레이터 녹화로 보여 준다. 애니메이션은 Codex가 만든다.

**Claude가 확인한 에뮬레이터 사실**
- 내비 지도 스타일이 `connect_empty`라서 도로·건물이 그려지지 않는다.
- 내비 속 차 위치는 성남에 고정된다. 내비가 에뮬레이터 GPS의 NMEA를 `checksum mismatch: expected=47`로 모두 버린다. 확인값이 맞는 `geo nmea`를 넣어도 같았다.
- 내비 설정(3.3.2)에 모의 주행이 없다.
- Android GPS(`adb emu geo fix`)와 가상 콘솔 속도·기어는 움직일 수 있다.

**T-21 [시험용, 버릴 코드] 내비 위 오버레이 실측**

`spikes/overlay/`에 작은 앱(패키지 `com.roadahead.spike.overlay`)을 만든다. `TYPE_APPLICATION_OVERLAY` 창(300×150dp 정도, 오른쪽 아래, `FLAG_NOT_FOCUSABLE|FLAG_NOT_TOUCHABLE`)에 "오버레이 시험" 글자를 띄운다. 항목:
1. 권한: `SYSTEM_ALERT_WINDOW`를 사용자가 Pleos 설정 화면에서 켤 수 있는가(`ACTION_MANAGE_OVERLAY_PERMISSION` 화면이 뜨는가), 아니면 `adb shell appops set --user 10 <패키지> SYSTEM_ALERT_WINDOW allow`로만 되는가
2. P에서 전체 화면 내비 위에 카드가 보이는가(캡처)
3. 가상 콘솔 D·30km/h에서 카드가 계속 보이는가, 시스템이 가리거나 없애는가(캡처). DO 메타데이터 유무에 따라 다른지도 확인
4. 카드가 떠 있어도 내비 화면 터치가 되는가
5. 앱 화면을 닫아도(홈, 다른 앱) 카드가 남는가. foreground service가 필요한가, Android 14 제약과 필요한 서비스 유형·권한
6. 내비 옆 멀티 윈도우 상태에서 카드 위치
7. 회전·화면 크기 변화 없이 2560×1440에서 dp 좌표로 오른쪽 아래 여백을 정하는 방법

- 결과는 `docs/spikes/overlay.md`(동작/부분/불가·근거·수치), 캡처는 `docs/spikes/overlay/`에 둔다.
- 소리는 내지 않는다. 에뮬레이터 상태(기어·속도·GPS)는 끝나면 P/0으로 되돌린다.
- Codex 자동 승인 검토에 막힌 adb 명령은 우회하지 말고 ask로 Claude에게 맡긴다.

**T-22 콘셉트 애니메이션**
- 대본과 카드 규격은 [VIDEO.md](VIDEO.md)를 그대로 따른다. 자막 문장은 바꾸지 않는다.
- 만드는 방법(Claude 결정)
  - `video/concept.html`: 외부 네트워크 없이 열리는 HTML·CSS·JS 한 장. 글꼴은 `Malgun Gothic`
  - `video/render.mjs`: 이미 설치된 Node 24와 Playwright Chromium의 `recordVideo`로 1920×1080 webm을 만든다. 새 패키지는 설치하지 않는다. Playwright 모듈을 못 찾으면 HANDOFF에 남기고 ask한다.
  - 결과 `video/out/01-intro.webm`, `02-concept.webm`, `04-outro.webm`은 커밋하지 않는다. `.gitignore`에 `video/out/` 한 줄만 추가한다.
  - `video/CUES.md`: 구간별로 몇 초에 어떤 자막(=음성 문장)이 나오는지 적은 시각표
- 장면 A·B·C와 1·4번 구간에서 1장씩 캡처를 `video/frames/`에 커밋하고 HANDOFF에 링크한다.
- 검증: 렌더 명령을 다시 실행해 세 파일이 생기고 길이가 VIDEO.md 범위 안인지 확인한다.

## 2026-10-29 12:20 · Claude · T-20 리뷰 완료, 사용자 방향 변경: 내비 위 오버레이

- T-20 검증:
  - `.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :core:test --rerun-tasks` → 성공
  - 커밋된 APK에 `apksigner verify --print-certs` → `CN=SUDL`
  - 서명 비밀(`*.jks`, `keystore.properties`)이 git에 없음을 확인했다.
  - README의 공모전 필수 4항목을 확인했다.
- 사용자 결정(2026-10-29): 따로 뜨는 앱 화면보다 **기존 내비 화면 위, 빈 오른쪽 아래에 오버레이 카드**로 보이게 한다. 시연은 "내비에 추가된 기능처럼 보이고 느껴지게" 만들고, 기획서는 "Pleos 내비 기능 개선 제안"으로 쓴다.
  - 레이아웃 시안(장면 A·B·C, 비공개): https://claude.ai/artifact/MHDq2ZbGdjvmJYFFKxkCzG
  - 미확인: Pleos에서 외부 앱 오버레이(`SYSTEM_ALERT_WINDOW`)가 내비 위에 뜨는지, 주행(D) 중 시스템이 가리는지. 사용자 시안 확인 뒤 실측할 예정이다.
- 제출용 저장소 갱신은 오버레이 방향이 정해진 뒤에 한다.

## 2026-10-29 11:57 · Codex · T-20 release 서명 APK·README 리뷰 요청

- 한 일: `app/build.gradle.kts`에 루트 `keystore.properties`가 있을 때만 적용되는 release 서명을 추가했고 `isMinifyEnabled = false`로 명시했다. 파일이 없을 때는 unsigned release 빌드가 성공하는 것도 실제 확인했다. 기존 BuildConfig API 키 주입은 유지했다.
- 키 보관: 저장소 밖 `%USERPROFILE%\.road-ahead\release.jks`를 `keytool`로 생성했다(JKS, RSA 3072, SHA256withRSA, 유효기간 10,000일, DN은 **CN=SUDL**만). 무작위 비밀번호와 키스토어 경로는 gitignore 대상 루트 `keystore.properties`에만 저장했다. API 키와 비밀번호 값은 출력하거나 보고에 넣지 않았다.
- 제출 APK: [apk/SUDL_앞길안내_release.apk](../apk/SUDL_앞길안내_release.apk), **41,513,862 bytes**(100MB 미만). 원본 `app/build/outputs/apk/release/app-release.apk`와 SHA-256이 일치한다. APK SHA-256은 `f2011a6ef41b785126359055461da0974601e42971c5f361bb70482d479ef7f3`이다.
- 서명 검증: Build Tools 35.0.0의 `apksigner verify --verbose --print-certs` → **Verifies**, v2 서명 유효, signer 1개, 인증서 DN **CN=SUDL**. 인증서 SHA-256은 `2d24f48bfc84cf46d7cc293285d0143a0927326dbdb680f73c01650c10682efa`다. release `DEBUG=false`, API 키 3개 포함 여부, APK에 키스토어 비밀번호가 없는 것, 변경 문서에 API 키·비밀번호가 없는 것을 값을 출력하지 않는 assert로 확인했다.
- 빌드·단위 검증: `.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :core:test --console=plain` → **BUILD SUCCESSFUL**. 앱 **11개**·core **78개**, 실패·오류·건너뜀 0이며 단위 테스트 작업은 코드 변경이 없어 기존 성공 결과를 UP-TO-DATE로 재사용했다. `git diff --check`와 README 상대 링크 검사도 통과했다. 새 제품 코드나 단위 테스트는 추가하지 않았다.
- 설치: 기존 debug 앱의 저장된 모드가 DEMO_B라 우선 force-stop하고 설정을 gitignore 대상 `.gradle/t20-debug-settings-before.xml`에 보관한 뒤 기존 debug 앱을 제거했다. README와 같은 `adb -s emulator-5556 install -r --user 10 ".\apk\SUDL_앞길안내_release.apk"` → **Success**. 새 설치로 기본 LIVE/APPROACH를 사용하며, 사전 차량 상태는 Park/0 km/h, GPS는 기본 마운틴뷰/속도 0이었다. 차량·GPS 값은 조작하지 않았다.
- 실행 확인(Claude 직접 확인): Codex의 권한 부여·실시간 실행·UI 수집을 묶은 명령은 실행 전 자동 승인 검토에서 `blocked by policy`로 차단됐다(구체 사유 미제공). Orca ask로 요청한 뒤 Claude가 `monkey`로 런처 실행, 위치 권한의 **앱 사용 중에만 허용**, 첫 공공 데이터 안내의 **확인**을 직접 수행했다. 차량 속도 권한 창은 따로 뜨지 않았고, 상태 줄 **실시간**, MainActivity 유지, **FATAL EXCEPTION 0건·TTS onStart 0건**을 확인한 뒤 force-stop했다. `pm grant`는 실행하지 않았다. Codex도 이후 logcat과 user 10의 위치·CAR_SPEED 허용 상태를 읽기만 했다. 시연 재생과 소리 나는 TTS 재생은 하지 않았다.
- README: 지정한 한 줄 소개, APK 위치·설치 명령, debug 서명 충돌 시 제거 방법, 실제 첫 실행 권한·안내, P의 시연 A/B/C와 합성 살얼음 표시, 실시간 데이터 범위, Pleos AVD/API 34·콘솔 P/0, JDK 17, 값 없는 API 키·서명 설정 형식, 소스 빌드 명령과 기존 화면 2장을 반영했다.
- 남은 문제/미검증: 실제 음성 청음·시연 재생·실차·API 28 기기는 이번에 검증하지 않았다. 서명 키와 `keystore.properties`는 커밋하지 않으므로 사용자가 별도로 보관해야 한다. API 키는 사용자 결정대로 APK에 포함되며 대회 후 교체 대상이다. push하지 않았다.
- 요청: Claude가 T-20 diff와 README를 리뷰하고 TASKS를 완료로 바꾼 뒤, `tools/export-submission.sh`로 제출용 저장소를 갱신한다. TASKS의 T-20은 리뷰대기로 바꿨다.

## 2026-10-29 11:55 · Claude · T-20 요청: release 서명 APK와 README

사용자 결정(2026-10-29): 앱 이름은 가칭 **"앞길 안내"** 그대로, **개인** 참가, 제출용 저장소는 새 이력의 비공개 저장소 `LKY9910/road-ahead`(Claude가 만들고 첫 내보내기를 push함). 첨부자료에 실명을 쓰면 안 되므로 APK 파일명에는 별칭 SUDL을 쓴다.

**1. release 서명**
- 키스토어는 저장소 밖 `%USERPROFILE%\.road-ahead\release.jks`에 `keytool`로 만든다. 비밀번호는 무작위로 정한다.
- 경로와 비밀번호는 루트 `keystore.properties`(이미 gitignore)에만 둔다. 터미널·로그·HANDOFF에 비밀번호를 출력하지 않는다.
- 인증서 DN은 `CN=SUDL` 하나만 쓴다. 실명·이메일·소속을 넣지 않는다(APK에서 누구나 볼 수 있다).
- `app/build.gradle.kts`: `keystore.properties`가 있을 때만 release 서명 설정을 붙인다. 파일이 없어도 빌드는 실패하지 않아야 한다.
- `isMinifyEnabled = false`로 둔다. R8이 Pleos SDK나 직렬화 코드를 깨뜨릴 위험을 피한다.
- API 키는 지금처럼 BuildConfig로 넣는다(사용자 결정: 키를 넣고 대회 뒤에 교체).

**2. APK**
- `:app:assembleRelease` 결과를 `apk/SUDL_앞길안내_release.apk`로 복사해 커밋한다.
- 100MB를 넘으면 커밋하지 말고 HANDOFF에 남긴다.
- `apksigner verify --print-certs`로 서명과 `CN=SUDL`을 확인한다.

**3. 실행 확인**
- `emulator-5556`에 설치하고, 심사위원이 쓸 설치 명령 그대로 확인한다.
- release에서는 `logOnlySpeech`가 꺼져 소리가 난다. 따라서 **실시간 모드로만** 실행해 첫 화면과 `FATAL EXCEPTION` 0건을 확인한다.
- 시연 장면 재생은 하지 않는다(소리 재생은 사용자 허락이 필요하다).

**4. README.md** (한국어, 실명·키 값 금지). 공모전 필수 4항목:
- 앱 이름과 한 줄 소개. 한 줄 소개는 그대로 쓴다: "내 차가 갈 방향의 신호가 얼마나 남았는지, 앞길에 살얼음·안개·공사가 있는지를 내비 옆에서 미리 말해 주는 차량용 안내 앱입니다."
- 실행 방법
  - APK 설치
  - 처음 실행할 때의 권한·안내
  - 시연 모드: P에서 설정 → 시연 A·B·C. 장면별로 무엇이 나오는지 한 줄씩. 장면 C의 살얼음은 합성 자료라는 점도 적는다.
  - 실시간 모드의 데이터 범위: 신호는 울산, 기상은 고속도로 045, ITS는 전국.
- 테스트 환경과 필요한 설정
  - Pleos Connect 에뮬레이터 이름·API 수준, 가상 콘솔의 기어·속도
  - 소스 빌드 시 JDK 17
  - 키 설정: 루트 `api` 파일의 줄 형식(라벨만, 값 없이) 또는 환경변수 3개
  - `keystore.properties` 형식(값 없이)
- APK 위치: `apk/SUDL_앞길안내_release.apk`
- 화면 캡처 1~2장은 `docs/demo/`의 기존 PNG를 링크한다.

**5. 검증과 커밋**
- 실행: `.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :core:test`
- 커밋은 `[T-20]`으로 하고 push하지 않는다. 제출용 저장소 갱신은 Claude가 리뷰 뒤 `tools/export-submission.sh`로 한다.

## 2026-10-29 11:36 · Claude · 사용자 결정: API 키 교체 시점

- 결정: 노출된 API 키 3개(신호등·재난·기상)는 **대회가 끝난 뒤** 폐기하고 재발급한다. 제출용 release APK에는 키를 넣어 실시간 모드가 동작하게 한다.
- 근거: APK에 든 키는 디컴파일로 꺼낼 수 있어 지금 바꿔도 노출은 같다. 세 API 모두 무료라 최악의 경우도 호출 한도 소진이나 계정 제재 정도다.
- 요청(Claude, 11-15 이후): 사용자에게 키 3개 재발급과 루트 `api` 파일 갱신을 알린다.

## 2026-10-28 19:48 · Claude · T-19 리뷰 완료

- 검증:
  - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → core 78개, app 11개 통과
  - Claude 캡처 3장(`docs/demo/t19-empty-live.png`, `t19-scene-a.png`, `t19-scene-c-fog.png`)에서 다음을 확인했다.
    - 남은 초·위험 문장 크기
    - 빈 화면 문구
    - 큰 설정 버튼
  - `handleDataEvent`가 `CancellationException`만 다시 던지는 것을 확인했다(`DataEventTest`).
- 남은 일:
  - release 서명 APK와 README(앱 이름 결정 필요)
  - 시연 영상(소리 포함 녹화는 PC에서 사용자)
  - 기획서 마무리(T-18)
  - 제출용 GitHub 저장소의 실명 문제

## 2026-10-28 19:45 · Codex · T-19 화면 다듬기·데이터 처리 안정성 리뷰 요청

- 한 일: 위험 문장을 **36sp·최대 2줄**, 신호 남은 초를 **96sp**, 동작 이름을 **32sp**, 상태 줄을 **18sp**, P에서만 보이는 설정 버튼을 **최소 높이 64dp**로 바꿨다. 신호 색 원은 요청값인 **48dp**를 유지했다. 신호·위험이 모두 없으면 가운데에 **"앞길에 알려 드릴 신호나 위험이 없어요"**를 **24sp·onSurfaceVariant** 색으로 표시한다. 안내 문장·label·첫 실행 안내 문구는 그대로다.
- 데이터 처리: `MainActivity.kt`의 사건별 예외 경계를 `handleDataEvent`로 뺐다. `CancellationException`은 같은 객체로 다시 던지고, 그 밖의 `Exception`은 기존처럼 클래스 이름만 로그로 남긴 뒤 다음 사건을 받는다. 오류 메시지나 응답 본문을 로그에 추가하지 않았다.
- 회귀 검증: 새 앱 테스트 **1개**는 API 오류와 일반 오류 이후 다음 입력이 처리되고, 취소 이후 입력은 처리되지 않으며 취소가 거부 로그로 넘어가지 않는지 확인한다. 기존 `ApiException`만 잡는 동작을 추출한 상태에서 `IllegalStateException`으로 테스트가 실패한 뒤 예외 경계를 수정해 통과했다.
- 빌드·단위 검증: 추가 설정 화면 수정까지 반영한 `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` → **BUILD SUCCESSFUL**, 앱 **11개**·core **78개**, 실패·오류·건너뜀 **0**. core 테스트 작업은 기존 성공 결과로 UP-TO-DATE였다. `git diff --check`도 통과했다. 로그와 UI XML은 gitignore 대상 `.gradle/t19-*`에 있다.
- Claude 캡처 확인: Orca ask로 요청한 [A 대기 화면](demo/t19-scene-a.png), [C 안개 화면](demo/t19-scene-c-fog.png), [실시간 빈 화면](demo/t19-empty-live.png)을 Claude가 저장하고 확인했다. A의 빨간 원·좌회전·25초가 커졌고, C의 안개 문장이 크게 보이며, 빈 화면 문구가 가운데 흐린 색으로 보인다는 회신을 받았다. 세 PNG는 **Claude 소유이며 Claude가 별도 커밋**한다. A 안내 순서는 빨간불 → 32초 → 곧 좌회전으로 정상이라는 회신도 받았다.
- 추가 요청 반영: 위 ask 회신에서 Claude가 설정 화면의 라디오 항목과 닫기 버튼도 키우도록 요청했다. 각 라디오 항목과 닫기 버튼을 **최소 높이 64dp·글자 24sp**로 변경했다. 최종 APK를 `emulator-5556`, user 10에 재설치하고 `--ez logOnlySpeech true`로 실행했다. density 160에서 UI XML의 항목 행 `[877,174][2511,238]`과 닫기 버튼 `[877,606][969,670]` 높이가 각각 **64px(64dp)**임을 확인했다. 추가 수정은 설정 화면에만 해당하며 위 세 캡처의 주행 화면과 같다.
- 복원·실행 확인: 설정 UI에서 **실시간·진입 방향**으로 되돌렸고 SharedPreferences의 `LIVE/APPROACH/disclaimerSeen=true`와 빈 화면·실시간 상태 줄을 확인한 뒤 force-stop했다. 최종 설치 후 프로세스 로그에서 `TTS onStart`·`FATAL EXCEPTION`은 **각 0건**이다. 차량·GPS 입력은 조작하지 않았다.
- 남은 문제/미검증: 실제 TTS 청음·실차·좁은 멀티윈도우의 실제 두 줄 줄바꿈은 검증하지 않았다. core·tools·시연 자산은 수정하지 않았고 push하지 않았다.
- 요청: Claude가 T-19 코드와 설정 화면 추가 수정을 리뷰하고 캡처 세 장을 별도 커밋한다. TASKS의 T-19는 리뷰대기로 바꿨다.

## 2026-10-28 19:36 · Claude · T-17 리뷰 완료, T-19 요청

- 검증:
  - `py -3.12 -I tools\make_demo.py --self-test` 통과
  - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → core 78개, app 10개 통과
  - 시연 자산 3개에 `serviceKey`/`apiKey`/`authKey` 문자열 0건. `api` 키 값은 추적 파일·이력 모두 0건
  - 장면 A·B·C를 에뮬레이터에서 직접 캡처했다(`docs/demo/`). 안내 순서가 계획과 같다.
    - A: 8초 빨간불 → 37초 "좌회전 신호까지 32초" → 63초 "곧 좌회전 신호로 바뀌어요"
    - B: "앞 신호 빨간불이에요."
    - C: 안개 → 공사("2.5km 앞 공사 구간이에요. 4차로 차단.") → 살얼음(합성 label)
- 요청(Codex, T-19): 시연 영상 전에 화면을 다듬는다. 값은 Claude 결정이다.
  1. 위험 문장은 36sp 이상, 두 줄까지 보인다. 신호 카드는 남은 초 96sp, 동작 이름 32sp, 색 원 48dp로 한다.
  2. 빈 화면(신호·위험 없음)에는 가운데에 "앞길에 알려 드릴 신호나 위험이 없어요"를 24sp, 흐린 색으로 띄운다.
  3. 상태 줄(모드·label·음성 불가·한도 소진)은 18sp 이상이다.
  4. 설정 버튼은 높이 64dp 이상이다(차량 터치 대상). 주행 중에는 지금처럼 숨긴다.
  5. `MainActivity`의 `DataEvent` 처리에서 `ApiException`이 아닌 예외도 잡는다. `CancellationException`은 다시 던지고, 나머지는 로그 후 계속한다. 수집 코루틴이 멈추지 않게 하기 위해서다. 테스트 가능한 형태로 빼서 테스트 1개를 추가한다.
  6. 안내 문장·label 문구는 바꾸지 않는다.

## 2026-10-28 19:33 · Codex · T-17 시연 모드 리뷰 요청

- 한 일: `tools/make_demo.py`와 A·B·C JSONL 자산, `ReplayFeed`, `MainActivity` 모드 분기를 구현했다. 재생은 실제 단조 시각 기준이고, 속도는 마지막 값을 200ms마다 내보낸다. 신호의 수신 시각은 사건을 내보낼 때 찍으며, 기상·ITS의 생산 시각은 재생 시점으로 맞춘다(코드 주석에도 명시). 장면 끝에는 마지막 입력 상태와 속도를 유지하며, 신호의 3초 신선도 제한은 엔진 그대로 적용된다. core와 API 어댑터는 수정하지 않았다.
- 자료: A는 `signal-cycle-015~054`의 교차로 100, B는 `015~037`의 교차로 1을 각각 2초 간격 원래 순서로 쓴다. 지도는 `signal-map-ulsan`에서 해당 한 건만 남긴다. C 안개는 `weather-045-explicit`의 링크 3580008302 FOG=2 원문 행, 공사는 `its-event-seoul-authorized`의 `evt:2240032401:20261028091822:공사` 한 건이다. 살얼음 행만 같은 링크 좌표에 B_ICE=2를 합성했다. 표본의 요청 URL·키 메타데이터는 자산에 옮기지 않았다.
- Claude 승인 변경: 첫 안내를 10초 안에 보장하기 위해 A의 출발 거리는 400m에서 **380m**로 당기고 10m/s는 유지했다(35초에 30m 앞 정차). C label은 승인된 안개·공사 문구와 계획의 합성 살얼음 문구 그대로다. 생산 시각 변경 사실은 화면 대신 이 보고와 코드에 남긴다. 시연 엔진은 APPROACH로 고정하고 설정 화면에 방향 해석은 실시간 모드에만 적용됨을 표시했다. A 캡처 피드백에 따라 시연 상태 줄에는 모드 이름 없이 label만 표시한다.
- 빌드·단위 검증: `py -3.12 -I tools\make_demo.py` → `demo scenes generated`, `py -3.12 -I tools\make_demo.py --self-test` → `self-test passed`. 다시 생성한 세 파일의 SHA-256이 같고, 두 실행의 stdout은 위 고정 문구뿐임을 검사했다. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` → BUILD SUCCESSFUL, 앱 **10개**·core **78개**, 실패·오류·건너뜀 0. 새 앱 테스트 3개는 엔진을 통한 전체 장면 문장·순서·첫 안내 10초 제한, 생산 시각 변경, 마지막 속도 반복·중지, boolean 입력·null 회전/방향을 확인한다. ReplayFeed가 없을 때 컴파일 실패를 먼저 확인한 뒤 구현했다.
- 에뮬레이터: 기존 `emulator-5556`, user 10에 APK를 설치했다. 전 장면 `--ez logOnlySpeech true`로 실행해 아래 문장 순서를 assert로 확인했다. 세 로그에 `TTS onStart`, API `GET`, `FATAL EXCEPTION`은 모두 0건이다. API 추가 호출도 0회다. 첫 안내는 A·B의 교차로 수신 로그, C의 첫 tick 로그를 기준으로 계산했다(250ms 틱 오차).

| 장면 | 첫 안내 | 뒤 안내 시각·문장 | 전체 길이 |
|---|---|---|---|
| A | 약 8.02초: 앞 신호 빨간불이에요. 속도를 줄이세요. | 약 37.23초: 좌회전 신호까지 32초 남았어요. → 약 63.16초: 곧 좌회전 신호로 바뀌어요. | 78초 |
| B | 약 8.23초: 앞 신호 빨간불이에요. | 추가 발화 없음 | 44초 |
| C | 약 5.14초: 3km 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요. | 약 18.23초: 2.5km 앞 공사 구간이에요. 4차로 차단. → 약 41.12초: 3km 앞 살얼음 주의 구간이에요. 속도를 줄이세요. | 52초 |

- 화면 캡처: 장면마다 Orca ask로 Claude에게 요청했고 Claude가 화면을 확인했다. A [scene-a-1.png](demo/scene-a-1.png)·[scene-a-2.png](demo/scene-a-2.png)는 좌회전 빨간불·감소하는 초, B [scene-b.png](demo/scene-b.png)는 직진 빨간불과 중복 없는 label, C [scene-c-fog.png](demo/scene-c-fog.png)·[scene-c-ice.png](demo/scene-c-ice.png)는 안개·살얼음 경고와 기록 자료·합성 label이다. A 캡처는 상태 줄 중복 수정 전이다. PNG 파일은 Claude 소유이며 이번 커밋에 넣지 않는다.
- 재현: 설치 뒤 `adb -s emulator-5556 shell am start -n com.roadahead/.MainActivity --ez logOnlySpeech true`로 실행하고, P 상태의 설정에서 시연 A/B/C를 고른 뒤 닫는다(선택 시 장면 시작). 재시작은 같은 실행 명령으로 새 Intent를 보내면 된다. `adb -s emulator-5556 logcat -v monotonic -s RoadAhead AndroidRuntime`에서 `speak=`를 확인한다. A는 35~60초, B는 8~40초, C는 안개 5~17초·공사 18~35초·살얼음 41초 이후에 화면을 확인한다. 로그·빌드 출력·보안/문장/복원 assert 스크립트는 gitignore 대상 `.gradle/t17-*`다.
- 복원: 차량·GPS는 재생기가 실제 값을 바꾸지 않았다. 종료 후 Console `/vehicle/state`에서 Park/0, 앞·뒤 와이퍼 Off, 앞·뒤 성에 제거 false를 assert로 확인했다. Android GPS는 기본 `37.421998,-122.084000`, 속도 0이고 시간대는 Asia/Seoul이다. 앱 설정은 기존 LIVE/APPROACH/첫 안내 확인 상태로 복원한 뒤 force-stop했다. 콜드 부팅은 하지 않았다.
- 남은 문제/미검증: 실제 TTS 청음·실차·실제 신호 방향 의미는 검증하지 않았다. 신호·안개·공사는 10월 28일 기록 자료이고 살얼음은 합성이므로 현재 도로 상황을 뜻하지 않는다. push하지 않았다.
- 요청: Claude가 T-17 구현과 승인 변경을 리뷰하고 캡처 파일을 별도로 커밋한다. TASKS의 T-17은 리뷰대기로 바꿨다.

## 2026-10-28 19:19 · Claude · T-15, T-16 리뷰 완료

- 검증:
  - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test --rerun-tasks` → 성공. core 78개, app 7개 통과(실패 0), 디버그 APK 48MB
  - `api` 키 3개를 추적 파일 전체와 `git log -p --all`에서 찾았고 0건이었다.
  - 에뮬레이터에 설치해 `--ez logOnlySpeech true`로 실행했다(P 화면 캡처). 제목 "앞길 안내", 상태 "실시간", 설정 버튼이 보인다. 기본 GPS 위치(마운틴뷰)라 신호·위험은 없다.
- 코드 확인:
  - T-15: 재수정 `702382b`(전부 깨진 응답만 예외, 200초 상한)
  - T-16:
    - 키를 가린 URL 로그, 리다이렉트 차단
    - 신호 호출 수를 날짜별로 저장·복원
    - 기상 produced = 요청 tm
    - TTS 15초 콜백 시간 제한, 용도 12 내비 음성 감지
    - GpsFix 규칙(5m 이상 이동 방향, 미만이면 마지막 bearing)
- 다음에 다룰 것(T-17 이후):
  - 빈 화면 상태 문구
  - 상태 줄과 설정 버튼 크기(차량 터치 대상)
  - `DataEvent` 처리 중 `ApiException`이 아닌 예외가 나면 수집 코루틴이 멈출 수 있다.
- 다음: T-17(시연 모드)을 Codex에게 맡긴다.

## 2026-10-28 19:16 · Codex · T-16 앱 골격·실시간 입력 리뷰 요청

- 한 일: Task 9를 `2d08393`(앱 골격과 주행 중 화면), Task 10을 `ff17958`(실제 입력 연결과 실시간 루프)으로 나눠 커밋했다. 중복 GPS 콜백·정차 방향 유지 후속 수정은 `da9b622`다. namespace/applicationId는 `com.roadahead`다. DO 화면·P 전용 설정·첫 실행 안내·권한 요청, 환경 변수 우선 BuildConfig 키, 내비/GPS/AAOS 속도/와이퍼·성에 제거/내비 음성/Pleos TTS/API 어댑터와 250ms 엔진 루프를 연결했다. 엔진 루프는 Task 8 커밋 `c0d3747` 이후 연결했다. core는 수정하지 않았고 루트 Gradle은 `include(":app")` 한 줄만 추가했다.
- 빌드·단위 검증: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test`에서 BUILD SUCCESSFUL. 앱 7개·core 78개가 실패/오류/건너뜀 0이다. 앱 테스트는 키 인코딩·URL 마스킹·UTC 기상 생산 시각, 한국 자정 호출 수 초기화, GPS 방향을 검사한다. GPS 회귀 테스트는 provider 우선 및 중복 콜백의 두 기존 동작에서 각각 실패한 뒤 수정 후 통과했다. 기상 `produced`에는 성공한 요청 `tm`을 UTC Instant로 변환한 값을 전달한다.
- 에뮬레이터 검증: `emulator-5556`, user 10에 APK 설치 Success. 위치·CAR_SPEED 권한 허용, P/0 첫 안내와 확인 후 재표시 안 됨, D/0 앱 표시·설정 숨김을 UI XML과 로그로 확인했다. 모드 4개·방향 2개 표시와 방향 설정 저장, 설정을 연 채 D로 바꿀 때 닫힘도 확인했다. 화면 캡처는 하지 않았다.
- 실시간 재현: `adb -s emulator-5556 shell am start -n com.roadahead/.MainActivity --ez logOnlySpeech true`로 실행한다. Console browser session을 활성화한 뒤 Drive/36km/h를 주고, `adb -s emulator-5556 emu geo fix 129.259894 <위도> 20 10 19.4384`에서 위도를 `35.543293 + (400 - 10*i)/111195`로 두어 i=0~19를 1초 간격으로 주입했다. `RoadAhead` 로그에서 교차로 402건, 신호 K0, `Screen(signal=SignalCard(...))`, 빨간불 카운트다운과 초록불 전환을 확인했다. 마지막 재시험에서는 첫 SignalCard 이후 남향 주행과 정차 구간 전체에 signal=null이 없고 Park 이후에도 SignalCard가 갱신됨을 assert로 확인했다. 앞 와이퍼 Low·앞 성에 제거 ON/OFF에 각각 `wiperOn`·`defrosterOn` true→false가 나왔다. `logOnlySpeech`는 debug에서만 실제 재생을 막으며, TTS 초기화 available=true와 안내 문장 로그를 확인했고 onStart는 0건이다.
- API 호출 수: 이번 T-16은 **72회**(실시간 신호 37, 교차로 7, ITS 돌발 10·재난 10, 기상 8)다. GET 로그를 중복 제거하고 첫 수집 출력의 초기 3회를 합산했으며, 앱의 저장된 신호 호출 수도 37이다. 같은 날 T-14의 146회를 합치면 **218회**이고 서비스별 누계는 신호·교차로 172, ITS 29, 기상 17로 모두 500회 미만이다. 재실행 시 신호 호출 수가 이어지는 것도 확인했다. 각 커밋 전 staged diff의 실제 키 문자열 부재와 `git diff --check`를 검사했다.
- 계획과 다른 점(Claude 승인): 에뮬레이터가 `hasBearing=true, bearing=0`을 고정 제공해 원래 남향 시험에서 조회가 시작되지 않았다. `geo nmea`의 180°도 반영되지 않았다. ask 지시에 따라 **5m 이상 이동하면 좌표 방향을 우선**, 그 미만은 마지막으로 내보낸 bearing을 유지하고, 마지막 bearing이 없을 때만 provider bearing 또는 null을 쓰도록 두 차례 승인 변경했다. 중복 콜백에서 화면이 숨던 현상은 후속 변경으로 해결했고 원래 남향 20회와 정차 시험을 통과했다. 또 한국 날짜별 신호 호출 수를 SharedPreferences에 저장하고 엔진 생성 시 `recordSignalPoll`로 복원한다. 최대 4,500회 반복이며 엔진 복원 API가 생기면 바꾸도록 `shortcut:` 주석을 남겼다.
- 복원·근거: 앱을 force-stop하고 Park/0, 앞·뒤 와이퍼 Off, 앞·뒤 성에 제거 false, 기본 GPS `37.421998333333335,-122.084`·속도 0, Asia/Seoul로 복원했다. Console은 브라우저 세션이 없으면 `emulator_inactive`를 반환하므로 시험 중 해당 페이지의 `/browser/session` heartbeat를 유지했다. 로그·UI XML·집계 스크립트는 gitignore 대상 `.gradle/t16-*`에 있다. Console 성에 제거 시험의 잘못된 파라미터(`front_defroster_on`)가 400으로 거부된 뒤 실제 필드 `front_defroster`로 재시험했고 최종 ON/OFF와 복원을 확인했다.
- 남은 문제/미검증: 실제 신호 방향 의미·실차·API 28 기기·실제 TTS 청음/겹침은 이번에 검증하지 않았다. 첫 키 파일 형식 확인 중 마스킹 오류로 도구 출력에 키가 노출된 사실은 즉시 조정자에게 보고했고, 교체는 사용자 판단을 기다린다. 키 값은 이번 커밋과 보고에 없다. push하지 않았다.
- 요청: Claude가 T-16과 GPS 방향 유지 후속 수정을 리뷰한다. T-17은 `Feed`/`DataEvent`를 구현하고 `MainActivity.restartFeed`에서 ReplayFeed로 분기하면 된다. 현재 비 LIVE 모드는 "시연 준비 중"만 표시하며 실제 재생은 구현하지 않았다. TASKS의 T-16은 리뷰대기로 바꿨다.

## 2026-10-28 19:13 · Codex · T-15 재수정 리뷰 요청

- 한 일: 신호·교차로·ITS 돌발·ITS 재난·기상 CSV에서 항목/데이터 행이 하나 이상이고 전부 해석 예외로 실패하면 `ApiException("parse")`를 던지도록 바꿨다. 정상 항목이 섞이면 기존처럼 깨진 항목만 건너뛴다. 신호의 null·숫자 등 비객체 항목도 실패로 센다. 기상의 알 수 없는 단계는 실패로, 단계 0·빈 값은 의도적 제외로 구분했다. `SignalTracker.reading`은 계산 결과가 200초를 넘으면 색을 유지하고 초만 null로 둔다. 공개 함수 시그니처는 유지했다.
- 경계 확인: Orca ask로 조정자가 **의도적 제외가 하나라도 섞이면 빈 목록도 정상 결과로 반환**하라고 확인했다. 대상 외 돌발·재난, 종료 재난, 기상 0·빈 단계는 실패 횟수에 포함하지 않는다.
- 검증: 요청한 테스트 5개를 먼저 추가해 `.\gradlew.bat :core:test`에서 **78개 중 새 5개만 실패**하는 것을 확인했다. 구현 후 실패가 4→2→1→0으로 줄었고, 최종 같은 명령은 **BUILD SUCCESSFUL, 78개 통과(실패/오류/건너뜀 0)**다. 기존 73개 테스트 본문은 바꾸지 않았다. 새 테스트에는 전부 깨진 교차로·비객체 신호/ITS 항목, FOG/ICE의 알 수 없는 단계·잘못된 좌표·짧은 행, 빈 응답·의도적 제외와 실패의 혼합, 배율 1000의 raw 255000, 계산 결과 201초/200초 경계를 포함했다. `git diff --check`도 통과했다.
- 남은 문제/미검증: core 범위의 알려진 실패는 없다. app·SDK·에뮬레이터·실차는 이번에 검증하지 않았다. 외부 API 호출과 push는 하지 않았다. T-16의 app 변경과 루트 Gradle 파일은 수정하거나 커밋에 포함하지 않는다.
- 요청: Claude가 두 수정과 위 경계를 재검토한다. TASKS의 T-15는 리뷰대기로 바꿨다.

## 2026-10-28 19:07 · Claude · T-15 재수정 요청 (자동 보안 검토 반영)

- 배경: `0355eb2` 커밋에 대한 자동 보안 검토가 4건을 지적했다. 검토 결과 아래 두 가지를 고친다.
- 수정 요청(Codex):
  1. **전부 깨진 응답은 예외로 처리**: 지금은 모든 항목·행이 깨진 응답이 빈 목록이 된다. 그러면 직전 위험 목록이 "위험 없음"으로 바뀐다. DESIGN의 "API 오류나 빈 응답을 위험 없음으로 바꾸지 않는다"에 어긋난다.
     - 신호, 교차로, ITS 돌발, ITS 재난, 기상 CSV 파서에 같은 규칙을 둔다. 항목·데이터 행이 1개 이상인데 해석 중 예외로 버린 것이 전부이고 성공이 0개면 `ApiException("parse")`를 던진다.
     - 일부러 거르는 것은 실패로 세지 않는다. 대상 유형이 아닌 돌발·재난, 끝난 재난, 단계 0이나 빈 값이 그렇다.
     - 기상의 알 수 없는 단계 값은 실패로 센다.
     - 테스트 4개: `all broken signal items throw`, `all broken its events throw`, `all broken disasters throw`, `all broken weather rows throw`
  2. **남은 시간 상한**: 계산한 남은 시간이 200초를 넘으면 `remainingSec = null`로 둔다. 실측 주기가 161~181초이고, `255000` 고정값이 있었다.
     - 테스트: `implausible remaining is unknown`. 배율 1000에서 255000 → 색은 유지하고 초는 null이어야 한다.
- 반영하지 않는 지적: 위상이 바뀔 때 배율 초기화. 배율은 신호제어기의 성질이고, 계획 테스트 `scale survives a phase change`가 의도한 동작이다. 위 상한이 멈춘 값 위험을 줄인다.
- T-15를 진행중으로 되돌렸다.

## 2026-10-28 19:03 · Claude · T-15 완료

- 검증: 후속 수정 `0355eb2`를 확인했다. 최근 3개 표본으로 배율을 계산하고, 깨진 항목·행만 건너뛴다. `.\gradlew.bat :core:test --rerun-tasks`에서 73개가 통과했고 실패는 0이다.
- T-15를 완료로 바꿨다. 남은 것은 T-16(앱 연결)과 T-17(시연 모드)이다.

## 2026-10-28 19:00 · Codex · T-15 리뷰 수정 2건 재검토 요청

- 한 일: `SignalTracker`가 현재 위상의 최근 3개 표본만 보관해 배율을 계산하도록 바꿨다. 신호·교차로, ITS 돌발·재난, 기상 CSV는 깨진 항목·행만 건너뛰고 앞뒤 정상 항목은 유지한다. JSON·CSV 헤더 등 응답 전체의 형식 오류와 API 오류 코드는 기존처럼 `ApiException`으로 남겼다. 공개 함수 시그니처는 바꾸지 않았다.
- 검증: 요청한 회귀 테스트 4개를 먼저 추가하고 `.\gradlew.bat :core:test`에서 **73개 중 새 4개만 실패**하는 것을 확인했다. 항목별 수정 후 실패가 3→2→1→0개로 줄었으며, 최종 같은 명령은 **BUILD SUCCESSFUL, 73개 통과(실패/오류/건너뜀 0)**다. 기존 69개 테스트 본문은 유지했다. 새 테스트는 정상 3개→튄 값→정상 3개의 배율 복구, 깨진 신호·교차로 항목, 좌표 없는 ITS 돌발·재난, FOG·ICE의 알 수 없는 단계·잘못된 좌표·짧은 행을 검사한다. `git diff --check`도 통과했다.
- 남은 문제/미검증: core 범위의 알려진 실패는 없다. app·실시간 API·SDK·에뮬레이터·실차는 이번에 검증하지 않았다. 외부 API 호출, 소리가 나는 시험, push는 하지 않았다. T-16의 app 변경과 루트 Gradle 파일은 건드리지 않았다.
- 요청: Claude가 리뷰 요청 두 건을 재검토하고 T-15 완료 여부를 정한다. TASKS의 T-15는 리뷰대기로 바꿨다.

## 2026-10-28 18:55 · Claude · T-15 리뷰: 수정 요청 2건

- 검증:
  - `.\gradlew.bat :core:test --rerun-tasks` → 69개 통과, 실패 0
  - core 본체 13개 파일(약 600줄)을 계획과 대조했다. 문장, 기준값, 키 형식, 우선순위, 정차 보정이 계획과 일치한다. 정차 조회 보정(`effectiveFix` 공유)도 맞다.
- 수정 요청(Codex, T-15 후속):
  1. **신호 단위 보정 창**: `SignalTracker`가 현재 위상의 표본 전체로 배율을 다시 계산한다. 그래서 위상 중간에 이상한 쌍이 하나만 끼어도(값 튐, 위상 놓침으로 같은 색 재시작) 그 위상이 끝날 때까지 남은 초가 사라진다.
     - **최근 3개 표본만**으로 계산하게 바꾼다.
     - 테스트 `one glitch recovers after three good samples`를 추가한다. 정상 3개, 튄 값 1개, 정상 3개를 넣으면 다시 배율이 잡혀야 한다.
  2. **항목 하나가 깨졌을 때 응답 전체를 버리지 않기**: 지금은 신호의 `totDt`가 비었거나, ITS의 좌표가 비었거나, 기상 단계 값이 이상하면 응답 전체가 `ApiException("parse")`가 된다. 실제 공공 데이터에서는 한 건만 깨져도 그 지역·노선 전체 안내가 사라진다.
     - 깨진 항목·행만 건너뛴다. JSON이 아니거나 오류 코드인 경우는 지금처럼 예외로 둔다.
     - 테스트 3개를 추가한다: `bad signal item is skipped`, `its event without coordinates is skipped`, `weather row with unknown level is skipped`
- 참고(T-16에 전달): 실시간 기상 `Hazard.produced`는 받은 시각이 아니라 요청한 `tm`(생산 시각)이어야 10분 상한이 맞다.
- 계획 변경: T-16에서 에뮬레이터 GPS가 `hasBearing=true, bearing=0`으로 고정되는 것이 확인됐다. 그래서 `GpsFix` 규칙을 바꿨다. 5m 이상 움직이면 이동 방향을 쓰고, 그 미만이면 provider 값을 쓴다. 계획 Task 10에 반영했다.

## 2026-10-28 18:52 · Codex · T-15 core Task 1–8 리뷰 요청

- 한 일: 루트 Gradle 8.10.2와 순수 JVM `:core`(`com.roadahead.core`)를 만들었다. 공용 타입·지리 계산, 신호/교차로·기상 ZIP/CSV·ITS 파서, 신호 단위 보정·만료, 정차 판정, A/B/C 안내 규칙, 발화 중복·우선순위·내비 음성 대기, 조회 주기·한국 날짜별 한도를 조립했다. Task 1에서 `Inputs`, `SignalCard`, `Screen`, `TickResult`를 제공했고, Task 1/8 커밋 직후 코디네이터에 해시를 보냈다.
- 검증: 각 Task에서 계획의 테스트를 먼저 작성하고 미정의 심볼에 의한 컴파일 실패를 확인한 뒤 구현·통과·커밋했다. 최종 `.\gradlew.bat :core:test` → **BUILD SUCCESSFUL, 69개 통과(실패/오류 0)**. 계획 68개(Task별 6, 7, 7, 5, 10, 21, 5, 7)와 승인된 회귀 테스트 1개다.
- 자료·기준값: 테스트 이름 68개가 계획과 일치함을 검사했다. 표본 5개 본문을 원본과 대조했고, FOG 주의 4행·공사/사고 159건을 확인했다. 픽스처는 키를 제거한 `docs/spikes/road-ahead/samples/` 본문이며 원문을 유지했다. 모든 커밋 전 `git diff --cached`에 실제 API 키 및 URL/HTML 인코딩 변형이 없고 공백 오류가 없음을 검사했다. core의 시스템 시계 직접 호출과 금지 안내 문구도 없음을 확인했다. 외부 API 호출은 0회다.
- 계획과 다른 점: 코디네이터의 Orca ask 승인으로 **`shouldPollSignals`에도 정차 시 마지막 주행 bearing 및 누락 bearing 보정을 적용**했다. 원래 계획은 `tick`만 보정해, 정차 중 GPS 방향이 사라지거나 옆으로 튀면 조회가 멎고 3초 후 신호 화면이 사라질 수 있었다. 두 경로가 `effectiveFix`를 공유하며, 추가한 `stopped car keeps polling with last moving bearing`은 변경 전 1개 실패 → 변경 후 전체 69개 통과다. 원래 기대값·문장·공용 인터페이스는 바꾸지 않았다.
- Task별 커밋: 1 `c36e991`, 2 `71ef670`, 3 `ece8748`, 4 `f4780e2`, 5 `d839f30`, 6 `1a8e398`, 7 `660e8f8`, 8 `c0d3747`. 정차 조회 보정과 이 보고는 후속 T-15 커밋이다.
- 남은 문제/미검증: core 범위의 알려진 미해결 실패는 없다. 앱·실시간 SDK/API 연결·에뮬레이터 화면·TTS·실차는 T-16 이후의 검증 범위다. 실제 신호 방향 정의와 하드웨어 기준값은 기존 미확정 전제를 유지한다. 소리가 나는 시험과 push는 하지 않았다.
- 요청: Claude가 계획의 Review Focus 5개와 정차 조회 보정을 리뷰하고 T-15 상태를 확정한다. T-16은 같은 공개 인터페이스로 엔진을 연결한다. app과 Task 1 뒤 settings는 수정하지 않았으며, 동료의 진행 중 변경도 포함하지 않았다.

## 2026-10-28 18:02 · Claude · 구현 계획, T-15~T-18 배분

- 한 일: [plans/road-ahead.md](plans/road-ahead.md)에 구현 계획 Task 1–11을 썼고, TASKS에 T-15~T-18을 추가했다.
  - T-15: core Task 1–8
  - T-16: 앱 골격·실시간 입력, Task 9–10
  - T-17: 시연 모드, Task 11
  - T-18: 기획서, Claude
- DESIGN에 계획 중 정한 두 가지를 반영했다.
  - 앞길은 경로 안내 중에도 진행 방향 부채꼴로 판정한다(경로 좌표가 없음).
  - TBT 거리 기반 내비 음성 회피 예비안은 이번에 만들지 않는다.
- 요청(Codex): T-15부터 시작한다.
  - T-16은 Task 1 커밋 뒤 함께 진행할 수 있다. 루트 Gradle 파일은 Task 1이 만들고, T-16은 `settings.gradle.kts`에 `:app`만 추가한다.
  - 안내 문장은 계획의 글자 그대로 쓴다. 소리가 나는 시험은 사용자 허락 뒤에만 한다.

## 2026-10-28 17:52 · Claude · T-14 리뷰 완료, DESIGN 반영

- 한 일: T-14를 리뷰하고 완료로 바꿨다. 결과를 [DESIGN.md](DESIGN.md)에 반영했다.
  - 위치는 Android GPS, 내비에서는 상태·TBT만 받는다.
  - 신호 방향은 진입 방향으로 가정하고 설정으로 바꿀 수 있게 한다.
  - 신호 단위는 자가 보정, 데이터 나이 상한은 신호 3초·기상 10분.
  - 신호는 앞길 1 km 안에서만 2초 간격으로 조회하고 하루 4,500회에서 멈춘다.
  - 정차는 AAOS 속도로 판정하고, 내비 음성과 겹치면 용도 12 콜백으로 기다린다.
  - 날씨 판단은 와이퍼·성에 제거만 쓴다(안개등 미지원).
  - ITS 돌발·재난 두 서비스를 쓴다.
  - 화면은 DO 메타데이터를 쓰고, 시연은 기록 재생 모드로 한다.
- 검증:
  - `spikes/road-ahead`에서 `.\gradlew.bat assembleDebug --rerun-tasks` 성공(APK 29.7MB).
  - `py -3.12 -I probe_api.py --self-test`(5개), `analyze_samples.py --self-test`(4개) 통과.
  - `api` 파일의 키 3개를 추적 파일 전체와 `git log -p --all`에서 찾았고 0건이었다.
  - 화면 캡처 5장(P0, D0, D30, DO-D0, DO-D30)은 Claude가 직접 찍었다.
- 보충: 사용자 청음에서 NAVIGATION_GUIDANCE 용도의 3회 재생은 기존 MEDIA와 **체감 음량이 같았다**. Codex 보고 이후에 받은 답이라 [road-ahead.md](spikes/road-ahead.md)에는 빠져 있다.
- 남은 문제:
  - 신호 방향 정의(제공처 문의 가능)
  - 내비가 주의 지점·돌발을 스스로 말하는지
  - 기상 게시 지연과 API허브 한도
- 다음: Claude가 구현 계획을 쓴다.

## 2026-10-28 17:29 · Codex · T-14 실측 보고

- 한 일: [API 명세](APIS.md), [9개 항목 실측·권고](spikes/road-ahead.md), [표본·집계](spikes/road-ahead/api-summary.json), Pleos SDK 12절과 독립 시험 앱/수집 도구를 작성했다. API 146회(신호128, ITS합계9, 기상9)로 각각500회 미만이다. 울산 오지벌사거리·울산대학교와 중부내륙선 FOG 주의 링크를 시연 후보로 골랐다. 사용자가 ITS 돌발상황정보를 추가 신청한 뒤 공사153·사고6·기타23건을 받았다. 화면 캡처는 Claude가 수행했다.
- 핵심 결과: 울산 신호는 공식 센티초 설명과 다르게1,000값/초로 감소하며 후보의 관측 주기는161/181초(약2초 표본 오차)였다. 서울·인천·대구 실시간은 무자료였다. 신호 방향의 진입/진행 정의는 미확정이다. Android GPS 주입은 동작하지만 NaviHelper 위치는 성남에 고정되어 전체 시연은2안(기록 응답·위치 재생)을 권고한다. 기상은 공공데이터포털 일반 키가 아닌 API허브 키가 필요했고, 실제 ZIP·구간 좌표·5분 간격 값 변화를 확인했다.
- Pleos 결과: 표준 AAOS CAR_SPEED를 user10에 허용한 뒤0/8.055555m/s를 수신했다. 와이퍼·앞뒤 성에 제거는 반영, 안개등은 미지원이었다. 일반 Activity는 D/0에서도 가려지고 DO 메타데이터 변형은 보였다. 내비 usage12와 기본 Pleos TTS usage1의 동시 재생을 관찰해 명시적 발화 대기가 필요하다. 사용자 청음은 **들리지만 음량 작음**이다. SDK 직접 volume/gain setter는 없고 AudioManager15/15·fixed=true, CarAudio 그룹 제어 권한은 거부였다. 공개 AudioAttributes의 NAVIGATION 용도 비교3회는 완료했고 속성을 MEDIA로 복원했다.
- 검증: `spikes/road-ahead/gradlew.bat assembleDebug` 성공·설치 Success([빌드 로그](spikes/road-ahead/assemble-debug.txt)); `py -3.12 spikes/road-ahead/probe_api.py --self-test` 마스킹/인코딩5개 통과; `analyze_samples.py --self-test` 감소량·위상·빈 값·동일 시각4개 통과; `analyze_samples.py`146개 표본 집계 성공. 사용자 요청 청음6회씩2세트 모두 시작·완료, NAVIGATION 속성 비교3회도 완료했다. 원래 Console 상태·시간대·시험 앱 종료 등26개 복원 assert 통과([복원](spikes/road-ahead/restoration-check.txt)). 문서 로컬 링크·SDK1~11절 불변·현재 파일198개 키 문자열 검사 통과. `git diff --cached` 키 문자열·인코딩 변형 검사와 공백 검사도 통과했다([검증 기록](spikes/road-ahead/validation.txt)).
- 설계와 다른 점/요청(Claude): 신호 단위·방향 정의의 제한, 재난과 일반 돌발 API 분리, CAR_SPEED 런타임 허용, DO 메타데이터 효과, GPS와 NaviHelper 위치 분리, 안개등 미지원, TTS 용도·작은 음량을 계획에 반영해 리뷰한다. Pleos 전이 의존성 때문에 exclusiveContent에 `ai.pleos.playground.security`를 추가했다. DESIGN/AGENTS/CLAUDE/README/CONTEST/GLEO와 프로젝트1은 고치지 않았다.
- 남은 확인: 실제 신호등과의 동기·방향 매핑, 기상 최신 게시 지연/API허브 계정 한도, 실제 돌발·주의 지점의 내비 자체 발화, 실차 속도 노이즈·DO 적합성이다. NAVIGATION 속성 변경이 더 크게 들리는지는 청음 비교 회신 전이라 확정하지 않는다. 제품용 재생 모드·실차·release APK는 만들지 않았다. 내비 경로를 취소하고 시험 앱을 종료했으며 PC 소리 설정은 변경하지 않았다. push하지 않았다.

## 2026-10-28 15:58 · Claude · T-14 요청

- 한 일: 새 아이디어 "앞길 안내"(신호 남은 시간 + 도로 위험 알림)의 설계 초안을 [DESIGN.md](DESIGN.md)에 썼다(7cc9d2d). 사용자가 API 키 3개를 저장소 루트 `api` 파일에 넣었다. 이 파일은 `.gitignore`의 `/api`로 제외되어 있다. 신호등 키와 기상 키는 같은 공공데이터포털 인증키다.
- 요청(Codex): DESIGN.md 7절 9개 항목을 실측한다. 계획이 API 형식에 가장 크게 좌우되므로 1~3(API)을 먼저 한다.
  - API 3개(신호등 실시간 정보, 재난상황정보, 기상청 도로위험 기상정보):
    - 정확한 서비스 이름, 엔드포인트, 요청 파라미터, 우리가 쓸 응답 필드
    - 신호 8방향이 진입 방향인지 진행 방향인지, 남은 시간 단위
    - 갱신 주기: 같은 대상을 짧은 간격으로 여러 번 조회해 값이 바뀌는 간격을 잰다.
    - 응답 지연(중앙값·최대), 호출 제한, 오류 코드
    - 지원 지역이나 교차로 목록을 받는 방법
  - 시연 후보: 신호 데이터가 실제로 오는 교차로 2~3곳(서울 우선)과 가시거리 정보가 오는 고속도로 구간을 고른다. 응답 표본을 `docs/spikes/road-ahead/samples/`에 저장한다.
  - 4~9: 에뮬레이터 내비 콜백과 위치 이동, 정차 판정, 와이퍼·안개등·성에 제거, 빨간불 정차 때 화면, TTS 소리, 내비 음성 겹침.
- 키 규칙:
  - 키는 `api` 파일이나 환경 변수에서만 읽는다.
  - 키를 코드, 문서, 로그, 표본, 커밋 메시지, HANDOFF에 쓰지 않는다. 저장하는 URL·응답에서는 `serviceKey`·`apiKey` 값을 `***`로 바꾼다.
  - 공공데이터포털 키가 아직 승인 전이거나 어떤 서비스에 신청됐는지 모르겠으면 추측하지 말고 묻는다.
  - 호출은 API별로 하루 500회 안쪽으로 한다.
- 제약:
  - 코드는 버릴 시험용으로 `spikes/road-ahead/`에 만든다. Android 앱이 필요하면 `spikes/preset-control/`의 Gradle 설정을 복사한다.
  - `프로젝트1/`은 읽기만 한다.
  - DESIGN.md는 고치지 않는다. 설계와 다른 결과가 나오면 HANDOFF에 근거와 함께 적는다.

## 2026-10-28 15:35 · Codex · T-13 사용자 중단

- 한 일: **사용자 결정으로 중단**했다. 독립 시험 앱과 [중단 시점 실측 보고](spikes/preset-control.md)를 보존한다. 11개 좌석 축 capability·현재값, 앞 두 자리 앞뒤/등받이 방향 명령, 목표 루프, 착석·벨트 조회, Pleos TTS ON_DEVICE를 시험했다. 좌석 방향 명령은 성공하지만 SDK 위치는 0에 머물고 Console 내부 값만 변했다. TTS 한국어는 초기화 18ms·시작 23ms·완료 4063ms 및 RMS 콜백을 수신했다.
- 검증: `cd C:\dev\hmg-app\spikes\preset-control; .\gradlew.bat assembleDebug` 성공([로그](spikes/preset/assemble-debug.txt)), `adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk` → Success, `adb -s emulator-5556 shell am start -n com.preset.spike/.MainActivity --es op capabilities` 실행·콜백 확인. 자세한 재현 명령과 증거는 보고서에 있다. 앞 두 자리 slide/recline을 Console 시험 엔드포인트로 STOP·초기 0으로 복원했고 공조는 변경하지 않았다. HTTP 전후 비교 14개 assert 통과([복원 결과](spikes/preset/restoration-check.txt)); 재부팅하지 않았다.
- 남은 문제: 좌석 피드백 불일치 원인 미확인, 목표 53은 네 조합 모두 최종 SDK 0·오차 -53. 이동 속도·STOP 지연·넘침을 유효하게 재지 못했다. D단·메모리·Console 착석/벨트 변경·열선/통풍·공조·미러/핸들 전체 API 검증·STT initialize/request·화면 시연은 미실측이다. TTS PC 청음도 미확인이다. 미실행 제어 코드는 제거했고 실패 재현용 motion/target 루프는 제품에 재사용하면 안 된다.
- 요청: Claude가 T-13을 보류로 정리한다. 중단 지시에 따라 `docs/PLEOS_SDK.md` 12절과 TASKS 상태는 추가 수정·커밋하지 않는다(시작 시 진행중으로 변경한 미커밋 상태가 남아 있다). push하지 않았다.

## 2026-10-28 15:25 · Claude · T-13 요청

- 한 일: 새 아이디어(운전자·동승자별 프리셋을 한 번에 적용, 음성으로도 호출)의 전제를 확인한다. 에뮬레이터(emulator-5556)에서 `pleos.car.permission.CONTROL_CAR_SEATS`, `CONTROL_CAR_CLIMATE`, `TTS_SERVICE`, `STT_SERVICE`의 protectionLevel이 `normal`이다(`pm list permissions -f`). 포털 신청 안내는 Vehicle Control·TTS·STT를 "준비 중"으로 표시한다. Android의 `android.car.permission.CONTROL_CAR_MIRRORS`·`CONTROL_STEERING_WHEEL`은 `signature|privileged`라 쓸 수 없다. 차량 HAL 목록에는 `MIRROR_Y/Z_POS`, `STEERING_WHEEL_HEIGHT/DEPTH_POS`가 있다.
- 요청(Codex): 아래 12개를 에뮬레이터에서 실측한다. 좌석 영역은 운전석·조수석, 지원되면 뒷좌석까지.
  1. 좌석 capability: 앞뒤·높이·등받이·쿠션 기울기·깊이·머리받침(높이·각도·앞뒤)·허리(앞뒤·측면)·벨트 높이의 위치 조회와 `checkMoveSeatXxx` 결과(accessType, 영역, 최소·최대).
  2. 좌석 움직이기: 운전석·조수석 각각 앞뒤와 등받이를 `FORWARD`→`STOP`, `BACKWARD`→`STOP`. 위치 콜백 값 변화, 이동 속도(단위/초), STOP 뒤 멈출 때까지 지연과 넘친 양, 끝값에서의 동작.
  3. 목표 위치 맞추기: 위치를 보며 목표에서 STOP하는 짧은 루프로 앞뒤·등받이를 목표값에 맞추고 오차와 걸린 시간 기록. 운전석과 조수석을 동시에 움직일 수 있는지.
  4. 기어 D에서 좌석 움직임이 거부되는지(Virtual Console로 기어 변경). 거부되면 오류 코드.
  5. `selectSeatMemory`: capability와 호출 결과.
  6. 착석·벨트 착용: 자리별 조회와 Virtual Console 변경 반영 여부.
  7. 시트 열선·통풍: 자리별 설정과 되읽기.
  8. 공조: 자리별 온도, 풍량, 풍향, 내·외기, 에어컨의 capability, 설정, 되읽기. 정확한 API 이름은 Vehicle 2.3.2에서 확인.
  9. 미러 각도·핸들 높이: Vehicle·vehicle-aidl·vehicle-data에 위치를 다루는 공개 API가 정말 없는지 확인. 비공개 경로(AIDL 직접 호출, 리플렉션)는 있는지만 적고 쓰지 않는다.
  10. Pleos TextToSpeech 3.3.1(Maven에 배포됨): 의존성 좌표, 초기화, 모드, 한국어 문장 낭독, 시작 지연, 완료 콜백, PC 스피커로 소리가 나는지.
  11. Pleos SpeechToText 3.3.1: 초기화와 `request()`까지. PC에 마이크가 없으므로 오류 코드나 동작을 기록.
  12. 시연: Virtual Console이나 에뮬레이터 화면에 좌석·공조 변화가 보이는지 스크린샷. 안 보이면 그렇다고 적는다.
- 제약: 코드는 버릴 시험용으로 `spikes/preset-control/`에 독립 Gradle 프로젝트로 만든다(래퍼·버전 조합은 `프로젝트1/`에서 복사). `프로젝트1/`은 읽기만 하고 그 안에서 빌드하지 않는다(한글 경로). 에뮬레이터는 이미 실행 중이며, 콜드 부팅했다면 `cmd alarm set-timezone Asia/Seoul`. 키·개인정보 커밋 금지.

## 2026-10-28 · Claude · 프로젝트 전환

- 한 일: 사용자 결정으로 "가는길에"를 멈추고 새 아이디어(운전자·동승자별 차량 자세 프리셋)로 다시 시작한다. 지금까지의 코드·Gradle 파일·기획 문서를 `git mv`로 `프로젝트1/`에 옮겼다. 공용 자료(`AGENTS.md`, `CLAUDE.md`, `README.md`, `docs/CONTEST.md`, `docs/PLEOS_SDK.md`, `docs/GLEO.md`, `docs/spikes/t05/`)는 루트에 남겼다.
- Vehicle SDK 2.3.2 라이브러리 확인 결과(새 아이디어의 전제):
  - Seat: 앞뒤·높이·등받이 각도·쿠션 기울기·깊이·머리받침·허리 지지·안전벨트 높이의 현재 위치 조회와 구독, `moveSeatXxx(area, FORWARD/BACKWARD/STOP)` 제어, `selectSeatMemory(area, n)`, 착석·벨트 착용 조회, 열선·통풍 제어. 좌석 영역(`SeatArea`)별이라 동승석도 대상이다. 위치를 직접 지정하는 API는 없어서 목표 위치까지 움직이고 멈추는 방식이 된다. 제어 권한 `pleos.car.permission.CONTROL_CAR_SEATS`.
  - SideMirror: 열선만 있다. 거울 각도 조회·제어 없음.
  - SteeringWheel: 열선과 조향 각도 조회만 있다. 핸들 높이·거리 조회·제어 없음.
- 검증: Gradle 캐시의 `Vehicle-2.3.2.aar`를 `javap`로 확인. 에뮬레이터에서 실제로 좌석이 움직이는지, 제3자 앱에 제어 권한이 주어지는지는 아직 확인하지 않았다.
- 남은 문제: 프로젝트1의 Gradle 설정·래퍼는 `프로젝트1/`에 있다. 새 앱을 만들 때 필요한 것을 루트로 복사한다(버전 조합은 `프로젝트1/gradle/libs.versions.toml`에서 확인된 것).
