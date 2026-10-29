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
