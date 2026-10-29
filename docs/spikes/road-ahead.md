# T-14 앞길 안내 전제 실측

2026-10-28 · Codex. DESIGN 7절의 9개 항목을 확인했다. 시험 코드만 `spikes/road-ahead/`에 두었으며 제품 앱·설계·프로젝트1은 수정하지 않았다. API 명세는 [APIS.md](../APIS.md), 요청·응답은 [samples](road-ahead/samples/), 재현 가능한 집계는 [api-summary.json](road-ahead/api-summary.json)에 있다.

**계획에 가장 큰 영향:** 실시간 신호가 온 곳은 이번 조회에서 울산뿐이었다. 남은 시간 필드의 공식 단위와 실제 감소량이 10배 다르고 방향의 진입/진행 정의는 미확정이다. Android GPS 이동은 동작하지만 NaviHelper 위치가 따라오지 않아 전체 시연은 **2안(기록 응답·위치 재생)**을 권고한다. 차량 속도·와이퍼·성에 제거·내비 음성 감지는 사용할 근거를 확보했다. 안개등은 이 AVD에서 미지원이었다.

## 환경·방법

- AVD `emulator-5556`, `Pleos_Connect_2_1_API_34`, Android API 34, foreground user 10. 기존 인스턴스를 사용했고 Cold boot하지 않았다. 종료 때 시간대 `Asia/Seoul`을 확인했다.
- JDK 17.0.20.1+1, Gradle 8.10.2, AGP 8.7.2, Kotlin 2.1.21, compile/targetSdk 35, minSdk 28. preset-control의 Gradle 설정·래퍼를 복사했다.
- `Vehicle:2.3.2`, `NaviHelper:3.0.5`, `TextToSpeech:3.3.1`. `exclusiveContent`로 Pleos 그룹과 실제 TTS 전이 의존성 `ai.pleos.playground.security`만 공식 Maven에 연결했다. 후자를 제외하면 `universal-access:0.6.0` 해석이 실패했다.
- Virtual Console `http://127.0.0.1:8088/`, DDS ONLINE. Console 변경 전후 HTTP 상태와 앱 콜백을 대조했다. [시험 전](road-ahead/console-before.json), [와이퍼·성에 제거 ON](road-ahead/console-final-weather-on.json), [복원 후](road-ahead/console-restored.json).
- API 요청 총 **146회**: 신호 128, ITS 재난·돌발 합계 9, 기상 합계 9. 각 500회 미만. 저장 URL·응답에서 `serviceKey`, `apiKey`, `authKey`를 제거했다. 키는 Android 시험 앱에 넣지 않았다.
- 시간은 앱의 `SystemClock.elapsedRealtime()`와 요청기의 단조 시계로 측정했다. PC는 NTP보다 약 **29.88초 느림**(3회 중 2회 응답, 1회 timeout). HTTP Date도 같은 차이를 보였다. [시계 비교](road-ahead/clock-check.txt). 청음 구간 일부 logcat은 GPS 기본 위치 복원 뒤 America/Los_Angeles 시간대로 표시됐다(한국과 16시간 차이). 지연 수치는 벽시계 차이를 사용하지 않는다.

## DESIGN 7절 결과

`부분`은 수신·동작한 범위와 남은 확인을 함께 적었다. 빈 목록이나 미실측을 기능 자체의 불가로 바꾸지 않았다.

| 번호·항목 | 결과 | 근거·수치·제한 |
|---|---|---|
| 1. 신호 API 형식·방향·시간·범위 | **부분** | 맵·신호 API 정상 수신. 서울 지도 2,779건이 있으나 서울·인천·대구 실시간은 각 2회 모두 K3. 전국 신호 399건은 모두 울산, 울산 지도 402건. 공식 센티초와 달리 교차로 1·100의 유효 감소 110구간은 모두 1,000값/초. 관측 위상 재등장 간격 161/181초(약 2초 표본 오차 포함). 방향의 진입/진행 정의는 공개 설명으로 구별 불가. 정상 실시간 118회 지연 중앙값 149.67ms·최대 323.30ms. [집계](road-ahead/api-summary.json), [명세](../APIS.md). |
| 2. 도로위험기상 API·좌표 | **동작** | API허브 키로 중부내륙선 ZIP 3회 수신. 07:05/07:10 UTC 자료의 간격 300초와 FOG 주의 링크 4→0 변화 확인. LINK 572행, 1KM 1,494행. 양끝 경위도·위험 단계 제공, 가시거리 실측 m 값은 없음. 지연 중앙값 265.51ms·최대 328.25ms. 최신 시각 생략 502, 일부 503도 보존. 시즌 밖 FRG 미제공·최초 게시 지연은 미측정. [파일 포맷](road-ahead/weather-format.txt), [표본](road-ahead/samples/), [후보](road-ahead/weather-candidates.csv). |
| 3. 재난 API·공사/사고/통제 | **부분** | disasterInfo는 산불 등 재난 전용. 정상 7회 중앙값 109.74ms·최대 137ms. 30초 간격 6회/150.55초 동안 본문 동일하여 갱신 주기 미확정. 원본은 이미 진화 완료된 사건이었다. 별도 eventInfo를 사용자가 추가 신청한 뒤 수도권 182건(공사153/사고6/기타23) 수신, 1회 1,607.23ms. 통제 종류·종료 시각이 비어 있는 항목이 있어 필드 조합별 처리가 필요. [명세·차이](../APIS.md#3-국토교통부-재난상황정보). |
| 4. 내비 콜백·이동·내비 자체 음성 | **부분** | `FREE_DRIVE`·`ROUTE_DRIVE`, 현재 위치·heading, TBT와 경로 상태 수신. 울산 목적지 요청/반환 좌표는 일치, 경로 367km·15,360초 및 TURN_LEFT 80m 등 실제 목록 수신. `trafficStatus?.trafficEventList=[]`, CautionPoint 콜백 미관측이라 돌발/주의 지점의 자체 발화 여부는 미확정. geo fix로 Android GPS는 울산 좌표·10.28888→0m/s·accuracy 5m가 됐지만 NaviHelper는 성남 `37.4127212,127.094681`, heading 0에 고정. GPX 파일 재생은 별도 시험하지 않음. [콜백](road-ahead/callbacks.txt), [geo 명령](road-ahead/geo-commands.txt), [DO 화면](road-ahead/ux-DO-D0.png), [공개 타입](road-ahead/navi-api.txt). |
| 5. 정차 판정용 차량 속도 | **동작** | 표준 AAOS `CarPropertyManager`의 `PERF_VEHICLE_SPEED` 조회·약 5Hz 콜백 동작, 단위 m/s. `CAR_SPEED` 선언만으로는 SecurityException/등록 false였으나 user 10에 런타임 허용 후 D/0은 0.0, D/약29km/h는 8.055555, status 0. [속도·화면 측정](road-ahead/ux-do-capture.txt), [콜백](road-ahead/callbacks.txt). 실차 센서 노이즈·브레이크 신호와의 교차 검증은 미실측. |
| 6. 와이퍼·안개등·성에 제거 | **부분** | 앞 Low·뒤 High를 켜자 `getCurrentWiperState` OFF→ON, 앞/뒤 `getHvacDefrosterState` false→true, 복원 후 OFF/false 확인. Wiper capability READ, defroster 지원. 안개등 capability는 ERROR_NOT_SUPPORTED_PROPERTY, 조회는 FRONT/REAR_FOG_LIGHTS_STATE가 carPropertyConfig에 없다는 오류. Console의 Telltales 안개등 표시등은 이 속성의 대체 근거로 쓰지 않음. [콜백](road-ahead/callbacks.txt), [ON/복원 상태](road-ahead/console-restored.json), [공개 API](road-ahead/vehicle-api.txt). |
| 7. D/0 화면 | **부분** | 일반 Activity는 P/0에서 보이고 D/0·D/30에서 시스템 가림 화면. baseline UX는 P/0 false/0, D/0·D/30 true/80. coordinator 요청으로 Activity에 `distractionOptimized=true`를 넣은 변형은 D/0·D/약29에서 보였음. 변형 화면의 UX 초기 표시 false/0은 P에서 시작할 때의 값으로, D 상태의 최신 Ux 값이라고 해석하지 않음. 메타데이터 효과 실측이며 제품 화면의 DO 적합성·심사 승인을 뜻하지 않음. [일반 D0](road-ahead/ux-D0.png), [DO D0](road-ahead/ux-DO-D0.png), [DO 이동](road-ahead/ux-DO-D30.png), [baseline 로그](road-ahead/ux-capture.txt), [변형 로그](road-ahead/ux-do-capture.txt). |
| 8. Pleos TTS PC 스피커 | **동작(음량 작음)** | 사용자 청음 결과 ‘들리지만 너무 작다’. ON_DEVICE 한국어 문장을 10초 간격6회씩 두 세트 재생해 12회 모두 시작·완료. 1차 시작 중앙값7ms/최대18ms, 완료 중앙값2,926ms/최대2,950ms. 2차 시작 중앙값5ms/최대6ms, 완료 중앙값2,896.5ms/최대2,936ms. AudioManager는15/15·fixed=true여서 더 올릴 여지가 없었다. 추가 NAVIGATION_GUIDANCE 속성 비교3회는 아래 음량 절차 참조. [청음 기록](road-ahead/tts-listening.txt), [집계](road-ahead/tts-summary.json), [음량 비교](road-ahead/tts-volume-trial.txt). |
| 9. 내비 발화 감지·음성 겹침 | **부분** | 일반 앱의 `registerAudioPlaybackCallback`에서 내비 `usage=12`(NAVIGATION_GUIDANCE), Pleos TTS `usage=1`(MEDIA), 둘 다 content=1(SPEECH) 수신. 내비 활성 시 TTS를 한 번 시작해 usage12+1이 동시에 started, 최대 3개 플레이어를 관찰. UID/PID는 -1로 익명화됨. dumpsys 표본의 ducked player 목록은 비었음. 자동으로 한쪽이 끝날 때까지 대기하지 않았다. PC 무음 시험이라 실제 상대 음량은 미확인. 겹침 시험의 TTS 완료 콜백은 확보하지 못했고 timeout 처리도 지연되어 그 값으로 완료 시간을 산정하지 않음. 단독 재생 6회는 모두 완료. [동시 재생](road-ahead/audio-overlap.txt), [콜백](road-ahead/callbacks.txt). |

화면 캡처는 worker 쪽 자동 승인 검토가 차단하여 Claude가 같은 단계의 Console 설정·기기 내 screencap·adb pull로 수행했다. `capture-ux.ps1`은 PowerShell 구문 검사만 했고 이 파일 자체의 실행 성공을 주장하지 않는다. DO 변형 측정의 이동 속도는 설정 목표 30km/h와 캡처 시점 실제 29km/h를 구별했다.

## 시연 후보

방향 매칭을 검증한 목록이 아니라 **실제 응답을 받은 위치**다. 서울은 목록 존재와 실시간 수신이 달라 우선 후보로 확정할 수 없었다.

| 용도 | 후보 | 좌표(위도, 경도)·근거 |
|---|---|---|
| 신호 우선 1 | 울산 오지벌사거리, 지역 3100000000 / 교차로 1 | `35.512239,129.430752`. 북/동 직진 값이 1,000값/초로 일관되게 감소. 동 직진의 관측 주기 161초. |
| 신호 우선 2 | 울산대학교, 교차로 100 | `35.543293,129.259894`. 북 직진·좌회전 값의 감소 스케일이 일관됨. 북 직진 관측 주기181초. 좌회전 장면의 기록 재생 후보. |
| 신호 보류 | 목화예식장, 교차로 10 | `35.538201,129.331437`. 응답은 있으나 255000 고정과 불규칙 감소가 있어 남은 초 시연에서 제외 권고. |
| 가시거리 | 중부내륙선045, 링크3580008302 및 역방향3580008402 | 예: 시작 `36.362518,128.238118` → 끝 `36.312729,128.220901`. 07:05 UTC의 FOG=2(주의), 07:10에는0. 인접 1km 구간도 제공. 지자체 이름을 좌표만으로 추정하지 않음. [실제 4개 주의 링크](road-ahead/weather-candidates.csv). |
| 공사·사고 | ITS 수도권 범위 내 반환 항목 | 공사·사고 각각 실제 표본 있음. 좌표·통제 문구로 대상을 고를 수 있으나 사건은 종료·변경되므로 촬영 직전 재확인. [정상 eventInfo 표본](road-ahead/samples/20261028-163053-621566-its-event-seoul-authorized.json). |

신호 원본은 `signal-series-00~15`, `signal-cycle-000~099`, 지도는 `signal-map-ulsan`, 가시거리 원본은 `weather-045-explicit`(07:05), `weather-045-0710-after-backoff`, `weather-045-0730` 이름의 표본을 따른다. 파일명 앞에 PC의 실제 수집 시각을 붙였다. 외부 API의 서비스 주기와 표본 파일명의 수집 시각을 혼동하지 않는다.

## 계획에 넘길 권고

**시연 1안:** API 실시간 수신 자체의 시연은 울산 두 교차로와 중부내륙선에서 가능하다. 전체 A/B/C 시연은 NaviHelper 위치의 GPS 추종과 신호 방향의 진입/진행 정의가 해결되어야 한다. 지도 목록만 있는 서울을 실시간 후보로 잡지 않는다. 공사·사고는 재난 API와 별도 서비스이고 추가 신청 뒤 정상 수신했다.

**시연 2안(권고):** 보존한 실제 신호·FOG·ITS 응답과 위치를 같은 시계로 재생한다. 방향 매핑은 검증된 시연 설정으로 명시하고 미확정 매핑을 실제 차량 신호로 주장하지 않는다. 살얼음은 10월 실제 파일이 없어 합성 시나리오임을 표시해야 한다. T-14에서는 제품용 재생 모드를 구현하지 않았다.

**정차:** Pleos에서 삭제된 VehicleSpeedListener 대신 AAOS 속도를 사용한다. `CAR_SPEED`의 런타임 허용, `CarPropertyValue.status=0`, timestamp 신선도를 확인한다. 시험 기준 후보는 절대 속도≤0.1m/s가2초 연속이면 정차, ≥0.5m/s면 즉시 해제, 최신 값이1초 넘게 없거나 오류면 판정 불가다. 값의 물리 단위는 m/s를 유지하고 표시할 때만 km/h로 바꾼다. 이 수치는 실차 보정 완료 기준이 아니다. GPS 위치가 고정된 것을 기본 정차 근거로 쓰면 이번 AVD에서는 움직임을 놓칠 수 있다.

**내비 음성과 겹침:** 이 AVD에서는 TBT 거리 추정보다 사용 용도12의 재생 콜백을 먼저 쓸 근거가 있다. 용도12 플레이어가 하나라도 활성 상태면 우리 발화를 대기시키고, 모두 끝난 뒤 약200ms 여유를 둔 다음 대상·신호·거리·데이터 나이를 다시 검사한다. 지나간 교차로나 바뀐 신호 안내는 버린다. UID가 가려지므로 특정 내비 패키지 식별에 의존하지 말고 navigation 용도 전체를 보수적으로 피한다. 다른 기기에서 용도 정보를 받지 못할 경우 TBT 거리 회피를 예비안으로 둔다. 위험/주의 지점을 내비가 이미 말하는지는 대상이 있는 경로에서 추가 확인할 때까지 중복 발화 정책을 확정하지 않는다.

**음량 추가 확인:** TextToSpeech3.3.1에는 `setAudioAttributes`, `setAudioZoneId`, `setSpeakerType`가 있지만 직접 volume/gain setter는 없다([공개 시그니처](road-ahead/tts-api.txt)). `setAudioAttributes(NAVIGATION_GUIDANCE, SPEECH)`를 지정한3회는 실제 callback에서도 usage12로 바뀌었고 모두 완료됐다. 각 요청 뒤 원래 MEDIA 속성으로 돌리고 엔진을 해제했다. 이 라우팅 변경이 음량 증가를 보장하지는 않는다. 일반 AudioManager는 최대15/15·fixed=true였고, CarAudioManager의 그룹 음량 조절은 일반 앱 공개 표면에 없으며 앱의 CAR_CONTROL_AUDIO_VOLUME 권한도 거부(-1)였다([SDK 표면](road-ahead/car-audio-api.txt), [Android 권한 정의](https://developer.android.com/reference/android/car/Car#PERMISSION_CAR_CONTROL_AUDIO_VOLUME)). hidden API·리플렉션·privileged 권한 우회는 하지 않았다. NAVIGATION 속성을 제품에 택하면 우리 TTS도 usage12가 되므로 자신의 발화 상태를 함께 관리해야 한다. 위 겹침 감지 권고는 기본 MEDIA 설정을 기준으로 한다.

**데이터 나이·단위:** [APIS.md 4절](../APIS.md#4-계획에-반영할-측정-한계권고)의 교차로별 단위 확인, 신호 수신 뒤3초/기상 생산 뒤10분 상한 후보를 검토한다. 수신 시각만으로 서버에 고정된 오래된 응답을 판별할 수 없다. 초기 시계·생산시각의 증가·위상 전환을 함께 확인해야 한다.

## 빌드·검증·재현

```powershell
# JDK/SDK 경로: docs/PLEOS_SDK.md 8절 현재 PC 기준
cd C:\dev\hmg-app\spikes\road-ahead
.\gradlew.bat assembleDebug
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell pm grant --user 10 com.roadahead.spike android.permission.ACCESS_FINE_LOCATION
adb -s emulator-5556 shell pm grant --user 10 com.roadahead.spike android.car.permission.CAR_SPEED
adb -s emulator-5556 shell am start -n com.roadahead.spike/.MainActivity --es op state

cd C:\dev\hmg-app
py -3.12 spikes/road-ahead/probe_api.py --self-test
py -3.12 spikes/road-ahead/analyze_samples.py --self-test
py -3.12 spikes/road-ahead/analyze_samples.py
```

`assembleDebug` 성공, 설치 Success. [최종 빌드 로그](road-ahead/assemble-debug.txt). API 마스킹·인코딩 self-check와 감소량/위상/빈 값/정지 시간 검증 4개가 통과했다. API 집계는 저장된 표본만 읽으며 재호출하지 않는다. API 재호출이 필요하면 `probe_api.py --service signal --endpoint https://apis.data.go.kr/B551982/rti/tl_drct_info --label manual-seoul --param stdgCd=1100000000 --param type=json --param numOfRows=3`처럼 키를 인자로 넘기지 않고 실행한다. 일일 로컬 카운터는 gitignore 대상 `.gradle/api-counts.json`에 있다.

앱의 `query`, `route`, `cancel`, `state`, `tts`는 adb `--es op` 또는 시험 버튼으로 실행한다. `overlap`은 내비 용도12의 재생을 기다려 TTS를 한 번 요청하는 CLI 시험 전용이다. 음성 op는 실제 소리를 내므로 사용자가 허락한 청음 절차에서만 실행한다. 화면은 로그가 많은 진단 UI로 제품에 옮기지 않는다. release APK·제품 재생 모드·실차 시험은 이번 범위에서 만들지 않았다.

## 복원·남은 확인

Park/0, 앞·뒤 와이퍼 Off, 앞·뒤 성에 제거 false, 공조 auto 및 원래 온도·풍량·풍향을 복원하고 **26개 assert**를 통과했다. [복원 결과](road-ahead/restoration-check.txt). 기본 Android GPS 좌표 `37.421998333333335,-122.084`, 속도0을 돌려놓고 시간대도 Asia/Seoul로 복원했다. 내비는 SAFE_GUIDING·빈 목적지를 확인했고 시험 앱 프로세스를 종료했다. 내비 프로세스는 시스템에 의해 재기동될 수 있어 프로세스 부재를 경로 취소의 기준으로 삼지 않았다. PC 소리 설정은 사용자에게 맡겼으며 음소거 해제 명령을 보내지 않았다.

남은 확인은 신호 방향 의미·실제 신호등과의 지연, 서울 등 다른 지역의 실시간 수신, 기상 최신 파일의 게시 지연·API허브 계정 한도, 실제 돌발/주의 지점의 내비 자체 발화, 실차 속도 노이즈·DO 화면 적합성이다. 설계 판단과 제품 구현 계획은 Claude에게 넘긴다.
