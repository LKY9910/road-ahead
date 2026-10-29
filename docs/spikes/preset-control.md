# T-13 프리셋 전제 실측 — 사용자 결정으로 중단

2026-10-28 · Codex · **미완료**. 사용자 중단 결정에 따라 추가 실측·재부팅·청음 요청을 하지 않았다. 이 문서는 중단 시점까지 얻은 결과만 보존한다. 시험 앱은 `spikes/preset-control/`이며 제품 코드로 재사용하지 않는다.

핵심 확인: 공개 SDK로 좌석 방향 이동 명령을 보내면 성공 콜백이 온다. 그러나 Console 내부 위치는 변하는 동안 SDK의 위치 조회·콜백은 0에 머물러 **목표 위치 제어를 검증하지 못했다**. Pleos TTS의 ON_DEVICE 한국어 요청은 초기화·시작·완료·RMS 콜백을 받았다. PC 스피커 청음은 미확인이다.

## 환경과 빌드

- AVD `emulator-5556`, `Pleos_Connect_2_1_API_34`, Android API 34, Console `http://127.0.0.1:8088/`. 기존 인스턴스를 사용했고 Cold boot하지 않았다. 조회한 시간대는 `Asia/Seoul`이다.
- JDK 17.0.20.1+1, Gradle 8.10.2, AGP 8.7.2, Kotlin 2.1.21, compile/targetSdk 35, minSdk 28. 프로젝트1의 래퍼와 버전 카탈로그를 복사했으며 프로젝트1은 수정·빌드하지 않았다.
- `ai.pleos.playground:Vehicle:2.3.2`, `TextToSpeech:3.3.1`, `SpeechToText:3.3.1` 의존성 해석 성공. 저장소는 `https://nexus-playground.pleos.ai/repository/maven-releases/`이다. **STT 의존성 해석은 실행 검증이 아니다.**
- `assembleDebug` 성공, 디버그 APK 설치 `Success`, `com.preset.spike/.MainActivity` 실행과 SDK 호출 로그 확인. [최종 빌드 로그](preset/assemble-debug.txt), [의존성 해석](preset/dependencies.txt).
- UI의 `capabilities`, `state`, `tts` 및 adb `--es op`로 실행한다. `motion`, `target`은 실패 사례를 보존한 시험용이며 현재 환경에서 재실행하지 않는다.

```powershell
cd C:\dev\hmg-app\spikes\preset-control
.\gradlew.bat assembleDebug
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.preset.spike/.MainActivity --es op capabilities
# 앱 실행 중 읽기 시험을 다시 요청할 때:
adb -s emulator-5556 shell am start -n com.preset.spike/.MainActivity -f 0x20000000 --es op state
adb -s emulator-5556 logcat -d -v threadtime -s T13:I
```

## 12개 항목

`미실측`은 중단으로 실행하지 않은 항목이며 `불가`를 뜻하지 않는다. 번호는 HANDOFF 2026-10-28 15:25 요청과 같다. [전체 앱 콜백 원문](preset/callbacks.txt)의 `01`~`11` 접두사와 Android 시각·단조 시계(ms)로 확인한다.

| 번호·항목 | 결과 | 실제 근거와 남은 범위 |
|---|---|---|
| 1. 좌석 capability·현재 위치 | **동작** | 11개 축의 position·move capability와 지원 영역 현재값 조회 완료. 모두 `READ_WRITE`. 아래 영역·범위 표 참조. 이것이 실제 이동 피드백까지 보장하지는 않는다. |
| 2. 앞뒤·등받이 FORWARD/BACKWARD→STOP | **부분** | 앞 두 자리·두 축 모두 FORWARD/BACKWARD/STOP 성공 콜백. SDK 위치는 구독 초기 0 이후 변화가 없었고 조회도 0. Console `/vehicle/state`의 slide는 운전석·동승석 100, recline은 운전석 100·동승석 20으로 변한 순간을 관찰했다. [관찰 발췌](preset/console-motion-observation.txt). SDK 기준 관측 변화율은 0 단위/초이나 **실제 이동 속도를 뜻하지 않는다**. STOP 지연·넘친 양·끝값 실제 동작은 유효하게 측정하지 못했다. 원인은 미확인이다. |
| 3. 목표 위치·두 자리 동시 이동 | **부분** | 두 자리 병렬 호출은 성공했으나 위치를 읽는 루프는 목표 53에서 SDK 최종값 0, 오차 -53으로 실패했다. 앞뒤 운전석 5427ms·동승석 5402ms, 등받이 운전석 5370ms·동승석 5377ms. 약 5초 제한 후 STOP한 결과이며 목표 도달 시간으로 해석하면 안 된다. Console의 두 slide 값 변화는 확인했지만 SDK 피드백에 기반한 동시 목표 제어는 검증하지 못했다. |
| 4. 기어 D에서 좌석 제어 | **미실측** | 기어를 D로 바꾸지 않았다. 거부 여부·오류 코드 미확인. |
| 5. selectSeatMemory | **미실측** | capability·호출 모두 실행하지 않았다. 공개 시그니처 존재만 [API 목록](preset/vehicle-api.txt)에 보존했다. |
| 6. 착석·벨트 | **부분** | occupancy `READ`, belt `READ_WRITE`. 지원은 앞 좌우·2열 좌/중/우 총 5자리. 조회는 다섯 자리 모두 착석 true·벨트 false. 앞 중앙·3열 조회는 area not supported 오류. Console 변경 반영은 시험하지 않았다. Console 초기 occupancy false와 SDK true가 달라 이 값도 일치한다고 결론 내리지 않는다. |
| 7. 열선·통풍 | **미실측** | 설정·되읽기 하지 않았다. 공조·좌석 온도 값 변경 없음. |
| 8. 자리별 공조 | **미실측** | 공개 HVAC 시그니처만 [목록](preset/hvac-api.txt)에 저장했다. capability·설정·되읽기는 실행하지 않았다. |
| 9. 미러·핸들 위치 API | **미실측** | Vehicle AAR의 SideMirror·SteeringWheel 공개 시그니처를 저장했으나 요청한 vehicle-aidl·vehicle-data까지 전체 검증하지 않았다. 비공개 AIDL 직접 호출·리플렉션 사용 없음. |
| 10. Pleos TTS 3.3.1 | **부분** | `TextToSpeech(context, Mode.ON_DEVICE)` → `initialize(OnInitializeListener)` 성공 **18ms** → `addEventListener`·`setLocale(Locale.KOREA)` → `speak("운전석과 동승석 프리셋을 적용했습니다.")`. `onReady`, `onStart` **23ms**, 양수 RMS 5회(3158~5560), `onDone` **4063ms**. 앱 등록·키 없이 수행했고 서버 모드는 시험하지 않았다. PC 스피커 출력·청음은 미확인. |
| 11. Pleos STT 3.3.1 | **미실측** | 의존성 해석·AAR 공개 시그니처 확인까지만 했고 `initialize`·`request`는 호출하지 않았다. 마이크 오류 코드도 미확인이다. |
| 12. 화면 시연·스크린샷 | **미실측** | Console을 브라우저로 열어 DDS ONLINE 전환은 확인했다. 좌석·공조 변화의 화면 시연이나 스크린샷은 남기지 않았다. 2번 근거는 HTTP 상태 응답이며 시각적 시연 증거가 아니다. |

### 좌석 capability 세부값

각 행은 `checkSeatXxxPositionCapabilities`, `checkMoveSeatXxxCapabilities`를 모두 호출했다. 머리받침 높이 조회 함수의 실제 이름은 `getCurrentSeatHeadrestHeightAftPosition`이다. 범위의 물리 단위(mm·도 등)는 이번 실측으로 확정하지 않았다.

| 축 | position·move 접근 | 지원 영역 | 위치 범위 | 조회 초기값 |
|---|---|---|---|---|
| ForeAft, BackrestAngle, Height, Tilt | READ_WRITE | 앞 좌우·2열 좌우 | 0..100 | 모두 0 |
| Depth | READ_WRITE | 앞 좌우 | -10..10 | 0 |
| HeadrestHeight, HeadrestAngle, HeadrestForeAft | READ_WRITE | 앞 좌우 | -10..10 | 0 |
| LumbarForeAft, LumbarSideSupport | READ_WRITE | 앞 좌우 | -10..10 | 0 |
| BeltHeight | READ_WRITE | 앞 좌우 | 0..10 | 10 |

2번의 FORWARD→STOP 구간은 약 660~682ms, BACKWARD→STOP은 약 658~664ms였다. 네 조합 모두 `before=0, stopValue=0, final=0`, 앱 계산 `speed=0, over=0, lastChangeAfterStopMs=null`이다. **변화 피드백 자체가 없어서 over=0은 넘침이 없다는 증거가 아니며 null도 즉시 정지를 뜻하지 않는다.** 끝값 목표 100 시험은 네 조합 모두 최종 0, 오차 -100, 약 5.38~5.40초 제한 종료였다.

## 복원과 종료

SDK 위치가 0인 동안 Console 내부 위치가 달라졌으므로 앱의 `RESTORE ... final=0` 로그만으로 복원 완료를 판정하지 않았다. 앱을 종료하고 Console의 기존 `/vehicle/seat` 시험 엔드포인트에 앞 두 자리 slide·recline별 `kind=move&value=0`(STOP), `kind=position&value=0`을 PUT했다. 새로운 API·서비스나 비공개 Binder 경로는 사용하지 않았다.

[시험 전 HTTP 상태](preset/console-before.json)와 [복원 후 HTTP 상태](preset/console-restored.json)의 `seat.positions`, `seat.moves`가 정확히 같다. `active_count=0`, 기어 Park 유지, 공조의 전원·auto·AC·dual·recirc·풍량·자리별 설정도 시험 전과 같다. [복원 assert 결과](preset/restoration-check.txt). SDK·Console 불일치의 원인은 조사·수정하지 않았으며 재부팅하지 않았다.

## 후속 판단에 남길 것

- 프리셋에 안전하게 넣을 수 있는 좌석·공조 항목은 **이번 중단 실측만으로 확정할 수 없다**. capability 성공과 실제 값 반영을 구분해야 한다.
- 좌석 목표는 공개 API에 직접 위치 지정이 없어 이동·피드백·STOP 방식이 필요하지만, 현재 AVD에서 그 피드백을 검증하지 못했다. 이 시험 루프를 제품에 옮기면 안 된다.
- 음성 출력 후보는 키 없이 콜백까지 검증된 **Pleos TTS ON_DEVICE**다. 다른 앱에서 사용하려면 PC 청음과 실제 출력 경로를 별도 검증해야 한다. 음성 프리셋 호출은 STT 미실측으로 판단 보류다.

공식 문서와 바이너리 확인 근거: [Seat API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/seat/entry), [HVAC API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/h-v-a-c/entry), [TTS 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/importing-sdk), [초기화](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/initialization-and-release), [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners), [모드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro). 실행 결과는 위 로컬 로그에 근거한다.
