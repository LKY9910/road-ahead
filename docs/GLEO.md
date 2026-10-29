# Gleo Interface 조사·실측 (T-05 / T-08)

2026-10-27 · Codex · Pleos AVD 2.1.0 / Android 14 / `emulator-5556` / 사용자 10.

**ADB 설치 앱의 스키마 인식과 앱→Gleo 응답 전달은 확인했다. Gleo가 한국어 요청을 해석하여 우리 앱을 호출하는 전체 흐름은 미확인이다.** 호스트에 마이크 입력 장치가 없어 음성 요청을 시험할 수 없었다. 이 결과로 Gleo를 제품의 `VoicePort`로 채택했다고 판단하면 안 된다.

## 공식 명세

- [Core concepts](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/core-concepts): 앱 기능을 function calling으로 연결하며, Gleo Action으로 앱을 부르고 Callback으로 결과를 받는다.
- [App Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/app-capability): `packageName`, `appDescription`, `version`, `actionCalls`를 정의한다. 함수 이름은 대문자와 밑줄, description은 영어다. 함수는 최대 3개이며 JSON Schema의 일부만 지원한다. `component=service`는 Android Service 바인딩이 아니라 **BroadcastReceiver로 전달하는 `sendBroadcast`**다.
- [Action·Callback 코드](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/examples), [라디오 예제](https://document.pleos.ai/docs/pleos-only/app-integration-guide/use-cases/radio-app): Intent action은 함수 이름, 대상은 패키지, 인자는 `ACTION_PARAM` JSON 문자열이다. 설명 본문에는 개별 extra로 변환한다고도 쓰여 있으나 실제 코드 예제에 맞춰 구현·시험했다.
- [Callback Schema](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/action_callback): `functionName`, `status`(`success`/`asking`/`fail`), `message`가 필수다. 선택 `mode`는 `voice`(기본)/`beep`/`silent`, `data`는 JSON 배열을 담은 **문자열**이다. `asking`은 추가 정보·확인 대화를 뜻하고 Action 제한 시간은 7초다. 전용 선제 발화 API는 이 문서들에서 확인하지 못했다.
- [스키마 전달](https://document.pleos.ai/docs/pleos-only/app-integration-guide/guide/deliver-schema): APK의 `assets/schema.json`을 부팅 시 스캔하고 설치·삭제 시 갱신한다고 안내한다. 별도로 스키마 검토·승인 후 활성화한다는 절차도 명시한다. **파일을 읽었다는 사실은 제품 승인이나 모든 호출의 허용을 뜻하지 않는다.**

진단 앱에는 별도 Gleo SDK 의존성이나 전용 권한을 추가하지 않았다. `SEARCH_PLACE`를 수신하는 exported receiver와 DEFAULT category를 선언했다. 비공개 API, 데이터베이스 수정, 서비스 재시작, 승인 우회는 사용하지 않았다. 실제 배포·공모전 승인 절차는 사용자와 담당자 확인이 필요하다.

## 실제로 넣은 App Schema

[원본 schema.json](../프로젝트1/app/src/main/assets/schema.json), [수신·응답 코드](../프로젝트1/app/src/main/kotlin/com/ontheway/GleoReceiver.kt). (프로젝트1 "가는길에"에서 쓴 것)

DESIGN §4의 `search_place(category, conditions, near)`에서 이번 지시의 `category`·`near`만 옮긴 최소 예다. `conditions`와 다른 네 함수는 이번 시험 범위에 넣지 않았다. 장소 검색을 수행하지 않고 지정된 고정 답변을 반환한다.

```json
{
  "packageName": "com.ontheway",
  "appDescription": "Find convenient places along a driving route or near the destination.",
  "invokeWord": "가는길에",
  "version": "0.1.0",
  "actionCalls": [{
    "type": "function",
    "function": {
      "name": "SEARCH_PLACE",
      "description": "Search for a convenience store, cafe, restaurant, pharmacy, charging station or parking lot along the route ahead or near the destination.",
      "component": "service",
      "parameters": {
        "type": "object",
        "properties": {
          "category": {
            "type": "string",
            "enum": ["CONVENIENCE", "CAFE", "RESTAURANT", "PHARMACY", "CHARGER", "PARKING"],
            "description": "The type of place to find."
          },
          "near": {
            "type": "string",
            "enum": ["ROUTE_AHEAD", "DESTINATION"],
            "description": "Search along the route ahead or near the destination."
          }
        },
        "required": ["category", "near"]
      }
    }
  }]
}
```

인자 열거값은 편의점·카페·식당·약국·충전소·주차장에 대응한다. receiver는 JSON 형식, 최대 4096자, 두 열거값을 검증한다. 올바른 인자에는 다음 문자열을 `response` extra에 넣고, 앱 패키지는 별도 `packageName` extra로 보낸다. action은 `ai.pleos.playground.intent.action.APP_ACTION_RESPONSE`다.

```json
{"functionName":"SEARCH_PLACE","status":"success","message":"2km 앞 GS25가 있어요. 들를까요?"}
```

지시대로 진단용 한국어 고정 문장을 썼다. 공식 Callback 가이드는 message의 영어 작성을 요구하므로, 실제 활성 대화에서 한국어 message가 어떻게 처리되는지는 추가 확인 대상이다.

## 에뮬레이터 실측 (a)~(f)

환경: Gleo `ai.umos.ambientai` 4.2.0.15, 기본 voice interaction service `AiAssistantService`, Model Provider 4.1.21-coral-aosp. [근거 로그](spikes/t05/gleo.txt), [앱 콜백 로그](spikes/t05/callbacks.txt), [Gleo 화면](spikes/t05/gleo-assist.png).

| 확인 | 결과 | 근거·한계 |
|---|---|---|
| (a) ADB APK의 스키마 인식 / 재부팅 | **동작**, 재부팅 불필요 | 설치 직후 Gleo PID 2555의 `[Schema] Loaded schema.json from com.ontheway`, `onUpdateContextAppCapabilitySchema` 기록. 설치·재설치 때 반복됐다. 스키마 승인 여부를 나타내는 로그로 해석하지 않는다. |
| (b) Gleo 호출·한국어 요청 | **부분** | Settings 화면에 호출어 `'Gleo'`, History 화면에 “글레오” 예시가 있다. `am start -W -a android.intent.action.ASSIST`는 MainActivity에 Status ok, `cmd voiceinteraction show`는 `AmbVoiceInteractionSession onShow 3`·`ListeningPanelDisplayed Event true`까지 도달했다. ASSIST 구조 수집에는 BadParcelableException도 기록됐다. Settings·History·음성 패널에서 텍스트 입력창을 찾지 못했다. **마이크 부재로 “가는 길에 편의점 찾아줘” 인식 미확인**이며 임의의 비공개 텍스트 extra를 만들어 보내지 않았다. |
| (c) Gleo→우리 receiver·인자 | **미확인** | Gleo가 생성한 호출은 관측하지 못했다. 대신 ADB가 Gleo 예제와 같은 Intent를 보내 `CONVENIENCE`, `ROUTE_AHEAD`를 수신·화면·logcat에 기록한 것은 확인했다. 아래 명령 및 계측 테스트의 호출자는 ADB/테스트이며 Gleo가 아니다. |
| (d) callback message 낭독·표시 | **미확인** | 앱의 success 응답을 Gleo가 `processAppActionResponse`에서 수신한 것은 확인. 활성 요청 없는 `status: Sleeping` 상태에서는 `Reject AppActionResponse`로 거부했고 고정 문장이 표시·낭독되지 않았다. 정상 Gleo Action 이후 응답의 낭독 여부는 이 시험으로 판단할 수 없다. |
| (e) 선제 발화·후속 질문 | **선제 callback 방식 불가 / 후속 질문 미확인** | 잠든 Gleo에 callback만 보내는 방식은 위 거부 로그로 확인. 공식 `status=asking`과 hidden Settings의 “Asking 에서 마이크 열기” 항목은 있지만, 실제 후속 대화·예/아니요 전달은 마이크 부재로 시험하지 않았다. 설정을 바꾸지 않았다. |
| (f) 권한·승인 없이 동작 | **부분** | 디버그 설치·스키마 읽기·receiver 전달·Gleo의 응답 수신까지 별도 신청 없이 됐다. 승인이 필요 없다는 결론은 내릴 수 없다. 이번 거부 로그는 **Sleeping 상태의 응답 거부**이며 스키마 승인 거부로 단정할 수 없다. 전체 호출은 (c)와 같이 미확인이다. |

Windows PowerShell 재현 명령(현재 AVD의 foreground user는 10):

```powershell
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.ontheway/.MainActivity
adb -s emulator-5556 shell am start -W -a android.intent.action.ASSIST
adb -s emulator-5556 shell cmd voiceinteraction show
adb -s emulator-5556 shell cmd voiceinteraction hide
# Remote shell must receive the JSON surrounded by literal single quotes.
adb -s emulator-5556 shell 'am broadcast --user 10 -a SEARCH_PLACE -p com.ontheway --es ACTION_PARAM ''{"category":"CONVENIENCE","near":"ROUTE_AHEAD"}'''
.\gradlew.bat :app:connectedDebugAndroidTest
```

계측 테스트는 APK assets의 함수 이름·component를 읽고, 정상 인자 / 잘못된 category / 필수값 누락 / 잘못된 JSON 네 경우에 실제 receiver 응답의 `success`·`fail`을 검사한다. **1개 테스트, 내부 4경우 통과**. [실행 로그](spikes/t05/receiver-test.txt). 이 테스트는 Gleo의 해석 능력이나 음성 인식을 검증하지 않는다. Gradle 계측 실행은 설치한 시험 앱을 종료·제거하므로 끝난 뒤 APK를 다시 설치한다.

## app 연동 계획에 넘길 판단

스키마 전달과 Android 수신 구현은 사용할 수 있는 근거가 있다. Gleo를 `VoicePort`로 연결하는 설계는 마이크를 연결한 후 (c)·(d)·(e)의 활성 대화 경로와 공모전 승인 범위를 확인한 다음 결정한다. 특히 선제 제안은 잠든 Gleo에 callback을 보내는 방식으로 구현할 수 없다. `core`의 규칙 판단·포트는 이번 작업에서 변경하지 않았다.
