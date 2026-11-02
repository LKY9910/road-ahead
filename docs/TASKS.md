# 작업 보드

규칙은 [AGENTS.md](../AGENTS.md)의 "작업 흐름"을 따른다. 상태: `대기` / `진행중` / `리뷰대기` / `완료` / `보류`

첫 번째 아이디어 "가는길에"의 작업은 [프로젝트1/docs/TASKS.md](../프로젝트1/docs/TASKS.md)에 보관했다.

## 마일스톤

| 기한 | 목표 |
|---|---|
| 11-14 | 제출 (마감 11-15 하루 전) |

## 작업

| ID | 상태 | 담당 | 작업 | 완료 기준 | 관련 파일 |
|---|---|---|---|---|---|
| T-13 | 보류 | Codex | **[시험용, 버릴 코드]** (보류: 2026-10-28 사용자 결정으로 프리셋 아이디어를 접음. 중단 시점 결과는 `docs/spikes/preset-control.md`, Pleos TTS ON_DEVICE 한국어 동작 확인) 프리셋 앱 전제 실측: 좌석(운전석·동승석) 움직이기, 자리별 공조, 미러·핸들 API 유무, Pleos TTS·STT. 항목 12개는 HANDOFF 2026-10-28 15:25 Claude 항목 | `docs/spikes/preset-control.md`에 12개 항목 결과(동작/부분/불가)·근거·수치, `docs/PLEOS_SDK.md` 12절 요약, 빌드·실행 방법을 HANDOFF에 기록 | `spikes/preset-control/`, `docs/spikes/preset-control.md`, `docs/spikes/preset/`, `docs/PLEOS_SDK.md` |
| T-14 | 완료 | Codex | **[시험용, 버릴 코드]** 앞길 안내 전제 실측: 신호·재난상황·기상 API 형식과 지연, 에뮬레이터 내비 콜백·위치 이동, 정차 판정, 와이퍼·안개등, 주행 중 화면, TTS 소리, 내비 음성 겹침. 항목은 [DESIGN.md](DESIGN.md) 7절, 요청은 HANDOFF 2026-10-28 15:58 Claude 항목 | `docs/APIS.md`에 API 3개 명세(키 없이), `docs/spikes/road-ahead.md`에 7절 9개 항목 결과(동작/부분/불가)·근거·수치, `docs/PLEOS_SDK.md` 12절 요약, 시연용 응답 표본(키 제거), HANDOFF 보고 | `spikes/road-ahead/`, `docs/APIS.md`, `docs/spikes/road-ahead.md`, `docs/spikes/road-ahead/`, `docs/PLEOS_SDK.md` |
| T-15 | 완료 | Codex | core 판단 로직: [plans/road-ahead.md](plans/road-ahead.md) Task 1–8. 루트 Gradle, 공용 타입·지리 계산, 신호·교차로 해석, 신호 단위 자가 보정, 정차 판정, 기상·ITS 해석, 안내 규칙, 말하기 순서, 엔진 | 각 Task의 테스트가 계획의 값 그대로 있고 `.\gradlew.bat :core:test` 통과. Task마다 커밋 | 루트 Gradle 파일, `core/` |
| T-16 | 완료 | Codex | 앱 골격과 실시간 입력: [plans/road-ahead.md](plans/road-ahead.md) Task 9–10. Task 1이 커밋되면 T-15와 함께 시작할 수 있다(Task 8의 데이터 클래스 4개를 먼저 받거나 만든다) | `:app:assembleDebug` 성공, 에뮬레이터에서 D/0 화면 표시와 실시간 신호 수신 로그. HANDOFF에 확인 방법과 API 호출 수 | `settings.gradle.kts`(`:app` 추가만), `app/` |
| T-17 | 완료 | Codex | 시연 모드: [plans/road-ahead.md](plans/road-ahead.md) Task 11 (T-15, T-16 이후) | `tools/make_demo.py --self-test` 통과, 장면 A·B·C의 안내 문장이 계획 순서대로 logcat에 나옴, 장면별 화면 캡처 | `tools/`, `app/src/main/assets/demo/`, `app/.../feed/ReplayFeed.kt` |
| T-18 | 진행중 | Claude | 접수양식(DOCX) 분석과 기획서 초안 (프로젝트1 T-03을 새 아이디어로 다시) | 기획서 항목과 작성 방향, 초안이 `docs/PROPOSAL.md`에 있음 | `docs/PROPOSAL.md` |
| T-19 | 완료 | Codex | 화면 다듬기와 데이터 처리 안정성: HANDOFF 2026-10-28 19:36 Claude 항목의 6가지 | `:app:assembleDebug :app:testDebugUnitTest :core:test` 성공, 장면 A·C 화면 캡처(Claude에게 요청)에서 글자 크기·빈 화면 문구 확인, HANDOFF 보고 | `app/src/main/kotlin/com/roadahead/ui/`, `MainActivity.kt`, `app/src/main/res/` |
| T-20 | 완료 | Codex | release 서명 APK와 README: HANDOFF 2026-10-29 11:55 Claude 항목 | `apk/SUDL_앞길안내_release.apk`가 release 서명으로 만들어지고 인증서에 실명이 없음, 에뮬레이터에 설치해 실행 확인, README에 공모전 필수 4항목, `:app:assembleRelease :app:testDebugUnitTest :core:test` 성공, HANDOFF 보고 | `app/build.gradle.kts`, `apk/`, `README.md` |
| T-21 | 완료 | Codex | **[시험용, 버릴 코드]** 내비 위 오버레이 실측: HANDOFF 2026-10-29 12:45 Claude 항목의 7가지 | `docs/spikes/overlay.md`에 7개 항목 결과(동작/부분/불가)·근거, 캡처, HANDOFF 보고 | `spikes/overlay/`, `docs/spikes/overlay.md`, `docs/spikes/overlay/` |
| T-22 | 완료 | Codex | 콘셉트 애니메이션: [VIDEO.md](VIDEO.md)의 1·2·4번 구간. HANDOFF 2026-10-29 12:45 Claude 항목 | `video/out/`에 01-intro·02-concept·04-outro 영상, `video/CUES.md` 시각표, 장면별 캡처 1장씩 HANDOFF에 링크 | `video/`, `.gitignore`(`video/out/` 한 줄 추가만) |
| T-23 | 완료 | Codex | **[시험용, 버릴 코드]** 주차·관광 API와 목적지 콜백 실측: HANDOFF 2026-10-29 13:20 Claude 항목의 7가지 | `docs/spikes/parking-travel.md`에 7개 항목 결과·근거·수치, `docs/APIS.md`에 API 2종 명세(키 없이), 키를 지운 응답 표본, HANDOFF 보고 | `spikes/parking-travel/`, `docs/spikes/parking-travel.md`, `docs/spikes/parking-travel/`, `docs/APIS.md` |
| T-24 | 완료 | Codex | 주차·여행 판단 로직: [plans/parking-travel.md](plans/parking-travel.md) Task 1~3 | 계획의 테스트가 값 그대로 있고 `.\gradlew.bat :core:test` 통과. Task마다 커밋 | `core/` |
| T-25 | 완료 | Codex | 내비 위 안내 카드와 foreground service: [plans/parking-travel.md](plans/parking-travel.md) Task 4 (T-24 Task 3 이후 카드 그리기) | `:app:assembleDebug :app:testDebugUnitTest :core:test` 성공, 시연 A·C에서 P/0·D/30 내비 위 카드 캡처, 권한 끔 때 비정상 종료 없음, HANDOFF 보고 | `app/`, `docs/demo/overlay-*.png` |
| T-26 | 완료 | Codex | **[시험용, 버릴 코드]** 서울 시영주차장 실시간 API 실측(서울 열린데이터광장 키 필요, 사용자 신청 후 시작): 필드(좌표·요금·현재 주차 대수·갱신 시각), 갱신 주기, 서울시청 주변 표본 | `docs/spikes/seoul-parking.md`, `docs/APIS.md` 7절, 키 없는 표본, HANDOFF 보고 | `spikes/seoul-parking/`, `docs/spikes/seoul-parking*`, `docs/APIS.md` |
| T-27 | 완료 | Codex | 관광 응답·주차 자산 해석과 실시간 연결: [plans/parking-travel.md](plans/parking-travel.md) Task 5~6 | 계획 테스트 통과(`:core:test`), `:app:assembleDebug :app:testDebugUnitTest` 성공, 에뮬레이터에서 여행 모드 실시간 안내 로그·캡처, HANDOFF 보고 | `core/`, `app/` |
| T-28 | 완료 | Codex | TS 주차장 이름·요금 자산 도구: [plans/parking-travel.md](plans/parking-travel.md) Task 7 | `--self-test` 통과, `lots.tsv` 건수·크기·동국제강 포함 보고, 5MB 이하 | `tools/make_parking_asset.py`, `app/src/main/assets/parking/lots.tsv` |
| T-29 | 완료 | Codex | 시연 장면 E(여행 모드): [plans/parking-travel.md](plans/parking-travel.md) Task 9 (T-27 이후)와 여행 카드 두 줄 표시(HANDOFF 17:40) | make_demo `--self-test`, 빌드·테스트 성공, 시연 E 안내 문장과 카드 캡처 | `app/src/main/kotlin/com/roadahead/feed/ReplayFeed.kt`, `ui/Settings.kt`, `RoadAheadService.kt`, `tools/make_demo.py`, `app/src/main/assets/demo/scene-e.jsonl`, `ui/OverlayCard.kt` |
| T-31 | 완료 | Claude | README 갱신(사용자 요청): 오버레이 권한 켜는 법, 여행 모드, 시연 E, 주차·여행 데이터 범위, 빈 지도 안내, 오버레이 캡처. 시연 D·서울 빈자리는 T-30에서 추가 | 문서 내용이 코드(권한 문구·설정·카드 조건)와 일치 | `README.md` |
| T-32 | 완료 | Codex | **디자인 안(GPT 상의, 앱 코드 변경 없음)**: 여행 모드의 동행 선택 화면, 여행 목록 카드의 동행 이유 줄, 서울 빈자리가 들어간 주차 카드 | `docs/design/travel-mode-ui.md`에 항목별 치수·색·순서·근거, 항목마다 목업 이미지 1장 이상, HANDOFF 보고 | `docs/design/` |
| T-33 | 완료 | Codex | 여행 모드 core: [plans/travel-mode.md](plans/travel-mode.md) Task 1~4 (동행·상세 해석, 단기예보, 동행·날씨 맞춤 추천, 서울 빈자리·여행 모드 안 주차·질문 음성) | Task별 계획 테스트 통과, `:core:test` 전체 통과, HANDOFF 보고 | `core/` |
| T-34 | 완료 | Codex | 시연 E 원본 응답 기록: [plans/travel-mode.md](plans/travel-mode.md) Task 7 Step 1 (`tools/record_travel.py`) | 세종문화회관(세종로 주차장 500 m 안) 기록, 키 0개, 호출 수 보고, 덮어쓰기 없음 | `tools/record_travel.py`, `tools/demo-src/travel/` |
| T-35 | 완료 | Codex | 여행 모드 실시간 연결: [plans/travel-mode.md](plans/travel-mode.md) Task 5 (T-33 뒤) | 계획 테스트 통과, 빌드·테스트 성공, 에뮬레이터 로그(소리 없음, 키 0개) | `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/`, `app/src/main/kotlin/com/roadahead/feed/`, `RoadAheadService.kt` |
| T-36 | 완료 | Codex | 동행 묻기 화면과 카드 표시: [plans/travel-mode.md](plans/travel-mode.md) Task 6 (T-32 확정·T-35 뒤) | 계획 테스트 통과, 에뮬레이터 캡처(질문 로그 1회, 단독 선택, P 이탈) | `app/src/main/kotlin/com/roadahead/ui/`, `MainActivity.kt`, `RoadAheadService.kt` |
| T-37 | 완료 | Codex | 시연 E 여행 모드: [plans/travel-mode.md](plans/travel-mode.md) Task 7 Step 2~6 (T-34·T-35 뒤) | 동행 5조합 재생 테스트, 세종로 빈자리 문장, `make_demo --self-test`, 캡처, 외부 호출 0 | `tools/make_demo.py`, `app/src/main/kotlin/com/roadahead/feed/ReplayFeed.kt`, `app/src/main/assets/demo/scene-e.jsonl` |
| T-38 | 완료 | Codex | **콘셉트 영상 갱신(GPT 디자인)**: [VIDEO.md](VIDEO.md)의 새 여행 모드 장면 E1(음성으로 동행 묻기)·E2(동행·날씨 맞춤 추천)·D(빈자리) 순서와 도입 3번째 줄. 카드는 [design/travel-mode-ui.md](design/travel-mode-ui.md) 기준 | `node video/render.mjs --check` 통과, 영상 3개 다시 렌더, `CUES.md`·캡처 갱신, 외부 네트워크 0 | `video/` |
| T-39 | 완료 | Codex | **카드 한글 줄바꿈(GPT 디자인 판단)**: 내비 위 카드 글이 단어 중간에서 갈린다(시연 C 살얼음 "구간 / 이에요"). API 33 이상에서 어절 단위로 줄을 바꾼다. 크기·색·배치는 바꾸지 않는다 | 빌드·테스트 성공, 기존 `docs/demo/overlay-*.png` 다시 캡처해 단어 중간 줄바꿈 0건, 어색한 줄(한 어절만 남는 줄 등)은 GPT 판단을 HANDOFF에 | `app/src/main/kotlin/com/roadahead/ui/OverlayCard.kt`, `docs/demo/overlay-*.png` |
| T-40 | 완료 | Claude | README 여행 모드 반영과 release APK 다시 빌드(사용자 요청) | 문서가 코드·실제 첫 실행 순서와 일치, release 서명·키 4개·설치·실행 확인 | `README.md`, `apk/SUDL_앞길안내_release.apk` |
| T-41 | 완료 | Codex | **카드 여러 줄 글의 줄 길이 맞추기(GPT 디자인 판단, 사용자 요청)**: T-39 뒤 한 어절만 남는 끝줄(안개 "… 속도를 / 줄이세요.", 살얼음 "… 주의 / 구간이에요."). 줄 길이 균형(`BREAK_STRATEGY_BALANCED`)과 문장 경계 줄바꿈을 기기에서 비교해 고르고 구현한다. 글자 크기·색·폭·배치·음성 문장은 바꾸지 않는다 | 빌드·테스트 성공, 기존 `docs/demo/overlay-*.png` 10장 다시 캡처, 단어 중간 줄바꿈 0건, 선택 근거와 줄 단위 전후 표를 HANDOFF에 | `app/src/main/kotlin/com/roadahead/ui/OverlayCard.kt`, `docs/demo/overlay-*.png` |
| T-42 | 완료 | Claude | 기획서(접수양식 DOCX) 작성(사용자 요청): PROPOSAL.md 3절을 양식 02의 8칸에 채우는 도구, 01의 개인정보 아닌 칸 | 양식 검증 통과, 칸마다 내용 들어감, 개인정보·서명 칸 비움, 결과는 git 밖 `docs/forms/` | `tools/make_proposal.py`, `docs/PROPOSAL.md` |
| T-43 | 완료 | Codex | **기획서 화면 자료와 모양 검토(GPT 디자인 판단)**: 기획서에 넣을 화면(시제품 캡처·콘셉트 영상 프레임)을 골라 자르고 위치·캡션을 정한다. 렌더한 페이지를 보고 글자·간격·표 모양을 다듬는다. 글 내용은 바꾸지 않는다 | `make_proposal.py --self-test`, 양식 검증 통과, 렌더 페이지 PNG 확인, DOCX 10MB 이하, 근거를 HANDOFF에 | `docs/demo/proposal/`, `docs/PROPOSAL.md`(그림 줄만), `tools/make_proposal.py`(모양 값만) |
