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
