# 공동 작업 규칙 (Claude Code + Codex)

이 저장소는 **현대자동차그룹 차량용 앱 개발 공모전**(https://appdevchallenge.auto-hmg.io/) 출품작이다.
참가자는 1인(사용자)이며, Claude Code와 Codex 두 AI 에이전트가 같은 폴더에서 협업한다.
이 파일은 두 에이전트 모두의 공통 규칙이다. (Codex는 이 파일을, Claude는 `CLAUDE.md`를 통해 이 파일을 읽는다.)

공모전 요약·일정·심사 기준: [docs/CONTEST.md](docs/CONTEST.md)

## 역할

| 담당 | 역할 |
|---|---|
| **사용자** | 최종 결정, 에이전트 간 전달, 설치·계정 등 사람이 해야 하는 작업, 최종 제출 |
| **Claude** | 아이디어·기획서, 아키텍처·설계 문서, 작업 쪼개기(`docs/TASKS.md` 작성), 코드 리뷰 |
| **Codex** | `docs/TASKS.md`에서 자기에게 할당된 작업의 구현, 빌드, 테스트 |

- 역할을 넘는 작업이 필요하면 직접 하지 말고 `docs/HANDOFF.md`에 요청을 남긴다.
- 설계와 다르게 구현해야 할 이유가 생기면 임의로 바꾸지 말고 HANDOFF에 근거와 함께 남긴다.

## 작업 흐름

1. Claude가 `docs/TASKS.md`에 작업을 추가한다. 작업마다 ID(`T-번호`), 담당, 목표, 완료 기준, 관련 파일을 적는다.
2. 담당 에이전트는 시작할 때 상태를 `진행중`으로 바꾸고, 끝나면 `리뷰대기`로 바꾼다.
3. 끝낸 에이전트는 `docs/HANDOFF.md` **맨 위**에 보고를 추가한다. 무엇을 했는지, 어떻게 검증했는지, 남은 문제, 다음 사람에게 요청할 것을 적는다.
4. Claude가 리뷰한 뒤 `완료`로 바꾸거나 수정 요청을 HANDOFF에 남긴다.

상태 값: `대기` → `진행중` → `리뷰대기` → `완료` (막히면 `보류` + 사유)

## 충돌 방지

- 두 에이전트가 같은 작업 트리를 쓴다. **작업 시작 전 `git status`와 `git log -5`로 상대의 변경을 먼저 확인한다.**
- 자기 작업의 "관련 파일"만 수정한다. 남이 맡은 파일을 고쳐야 하면 HANDOFF에 요청한다.
- 상대가 커밋하지 않은 변경은 되돌리거나 덮어쓰지 않는다. (`git checkout -- .`, `git reset --hard`, `git clean` 금지)
- `docs/TASKS.md`와 `docs/HANDOFF.md`는 공용 파일이다. 수정 직전에 다시 읽고, 자기 항목만 바꾼다.

## 커밋

- 작업 단위로 작게 커밋한다. 자기가 바꾼 파일만 `git add <파일>`로 올린다. (`git add -A` 금지)
- 메시지 형식: `[T-번호] 요약` (예: `[T-04] Vehicle SDK로 속도 구독 구현`)
- 커밋 메시지 끝에 작성자 표시를 붙인다.
  - Claude: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
  - Codex: `Co-Authored-By: Codex <noreply@openai.com>`
- 원격 push는 사용자가 요청할 때만 한다.

## 기술 환경

- Android (API 28 이상), Gradle 8.0 이상, **Kotlin**
- Pleos Connect SDK: Vehicle, FusedLocation, NaviHelper, TextToSpeech, SpeechToText
  - 개발 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
  - API 레퍼런스: https://document.pleos.ai/api-reference/connect-sdk-pleos
  - 에뮬레이터 설정: https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator
- 개발 PC: Windows 11, PowerShell. 저장소는 영문 경로 `C:\dev\hmg-app`에 둔다(한글 경로는 Gradle 빌드 문제 위험). 환경 구성 절차와 실제 경로는 `docs/PLEOS_SDK.md` 8절을 따른다.
- SDK 동작은 추측하지 말고 공식 문서로 확인한다. 확인한 내용은 `docs/PLEOS_SDK.md`에 정리해 둘이 공유한다.

## 제출물 규칙 (공모전 요구사항)

- APK: release 빌드, 파일명 `팀명_앱이름_release.apk`, 저장소의 `apk/` 폴더 (용량 초과 시 GitHub Releases)
- `README.md`에 앱 이름과 한 줄 소개, 실행 방법, 테스트 환경과 필요한 설정, APK 위치를 유지한다.
- 비밀 키, 개인정보, 서명 키스토어(`*.jks`, `*.keystore`)는 커밋하지 않는다.

## 언어

문서, 커밋 메시지, 보고는 한국어로 쓴다. 코드 식별자와 주석은 영어로 쓴다.
