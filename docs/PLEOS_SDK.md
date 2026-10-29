# Pleos Connect SDK 조사 (T-04)

조사일: 2026-10-21 · 작성: Codex · 대상: Windows / Kotlin / Pleos Connect 앱

SDK 기능·권한은 공식 공개 문서를 조사한 결과다. Gradle 의존성 해석과 앱의 실제 API 호출은 아직 검증하지 않았다. T-01b에서 수행한 도구 설치·에뮬레이터 부팅 검증은 8절의 재시도 기록에 따로 적었다. API가 문서에 존재하는 것과 프로젝트에서 사용할 수 있는 것은 구분한다.

## 1. 개발·기획 전에 확인할 결론

- Pleos Connect는 AAOS 기반 차량용 앱 환경이다. 이번 조사는 공동 작업 규칙에 적힌 Vehicle, NaviHelper, FusedLocation, TextToSpeech, SpeechToText 5개 SDK를 대상으로 한다. [SDK 개요](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)
- **공개 신청 안내상 Vehicle State와 NaviHelper는 신청 없이 사용 가능하다. FusedLocation·TTS·STT·Vehicle Control은 ‘준비 중’으로 안내되어 신청할 수 없다.** 개발 시 로그인한 Workspace의 실제 상태와 공모전 별도 제공 범위를 확인해야 한다. API 문서가 있다고 사용할 수 있다고 단정하면 안 된다. [프로젝트 생성 및 SDK 사용 신청](https://document.pleos.ai/docs/pleos-only/create-my-project)
- Vehicle 퍼미션은 App Market 심사에서 사전 협의·승인 범위가 적용된다. ‘SDK 신청 불필요’와 ‘모든 API·퍼미션 사용 승인’은 같은 의미가 아니다. 공모전의 별도 허용 범위는 아직 확인하지 못했다. [SDK 정책](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/), [앱 심사: SDK·API](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)
- 구현 판단 시 capability, 실제 서비스 버전, 권한, API 성공 콜백을 확인한다. 일반 Android 에뮬레이터만으로 차량 서비스 연동을 검증할 수 없다. [Vehicle 샘플 가이드](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)

## 2. 버전과 의존성

2026-10-19자 **한국어 릴리스 노트**와 현재 한국어 모듈 가이드를 기준으로 삼았다. 실제 배포물 및 AVD와의 호환성 확인은 T-05에서 진행한다. [SDK 릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)

| SDK / 환경 | 현재 한국어 문서 기준 | 의존성 확인 수준 |
|---|---|---|
| Vehicle | 2.3.2 | 공식 가져오기 페이지에서 `ai.pleos.playground:Vehicle:2.3.2` 확인 |
| NaviHelper | 3.0.5 | `ai.pleos.playground:NaviHelper:3.0.5` 확인 |
| FusedLocation | 1.1.3 | `ai.pleos.playground:FusedLocation:1.1.3` 확인 |
| TextToSpeech | 3.3.1 | 최신 버전은 릴리스 노트 확인. 최신 가져오기 페이지 본문 수집 실패로 해당 버전 Maven 좌표는 미검증 |
| SpeechToText | 3.3.1 | 최신 버전은 릴리스 노트 확인. 최신 가져오기 페이지 본문 수집 실패로 해당 버전 Maven 좌표는 미검증 |
| Emulator / Virtual Console | 2.1.0 / Console 지원 최소 2.1.0 | 설치 가이드·릴리스 노트 확인 |
| 앱 개발 기준 | minSdk 28 이상, Gradle 8.0 이상, Java 또는 Kotlin | 이 프로젝트는 Kotlin 사용 |

출처: [Vehicle 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup), [NaviHelper 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/importing-sdk), [FusedLocation 가져오기](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/project-setup), [Emulator 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

확인된 저장소와 의존성만 다음처럼 설정한다. 아래 코드는 설정 예시이며 빌드 검증 전이다. 모든 SDK를 추가할 필요는 없다.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
```

```kotlin
// app/build.gradle.kts — include only SDKs used by the app
dependencies {
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    // Confirm Workspace access before enabling this dependency.
    // implementation("ai.pleos.playground:FusedLocation:1.1.3")
}
```

### 문서 간 차이

- 같은 날짜의 [영문 SDK 릴리스 노트](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-261015)는 Vehicle 2.3.1, FusedLocation 1.1.2로 표시되어 한국어 문서와 다르다. 이 문서의 기준 선택은 한국어 가이드·가져오기·릴리스 노트가 서로 일치한다는 근거에 따른다. 실제 바이너리 확인을 대신하지 않는다.
- [이전 환경 설정 경로](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-application-development-environment)에는 Vehicle 2.3.0, NaviHelper 2.2.7, 음성 3.2.1, Fused Location 1.1.1 및 BT 동글 연동 설명이 남아 있다. 현재 링크 체계의 설정 문서를 사용한다.
- 기존 API Reference 개요에는 ADAS가 별도 SDK로 열거되지만, [현재 개발자 가이드 개요](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)는 위 5개만 열거한다. ADAS 별도 SDK의 신규 사용 가능 여부는 미확인이다. Vehicle의 DrivingMode와 Console의 ADAS 영역을 별도 ADAS SDK 사용 승인으로 해석하지 않는다.
- Vehicle 소개의 서비스 기준은 2.3.21, 샘플 가이드는 2.3.22다. 소개 버전을 AVD의 실제 서비스 버전으로 간주하지 않는다. 샘플의 minSdk 23도 제품 앱 요구사항인 28 이상을 대체하지 않는다. [Vehicle 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro), [샘플 환경](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)

## 3. Vehicle SDK

### 제공 데이터와 기능

차량 서비스에 연결해 상태를 조회하거나 변경을 구독하고, 허용된 기능을 제어한다. 다음은 도메인 수준의 제공 범위이며, 모든 차종·영역에서 모든 기능을 지원한다는 의미는 아니다. [Vehicle 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)

| 도메인 | 대표 데이터·기능 |
|---|---|
| Brake / DrivingMode | 브레이크·주차 브레이크, 드라이브 모드·크루즈 상태 |
| CarInfo / Display | 차량 기본 정보, 온도 등 표시 단위 |
| Door / Window / Wiper | 도어 열림·잠금, 창문, 와이퍼 상태·허용 제어 |
| EvBattery / Fuel | 배터리·충전 상태, 전비·연비 등 에너지 정보 |
| HVAC | 공조 온도·풍량·풍향·습도 등 |
| Light / TurnSignal | 조명 및 방향 지시등 |
| Odometer | 누적·트립 거리, 트립 시간, 거리 단위 |
| Safety / Tire | 경고 및 타이어 상태 |
| Seat / SideMirror / Steeringwheel | 좌석 위치·기능, 미러 열선, 핸들 상태·열선 |
| Toll | ETCS 음성 안내 음량 등 |

### 주요 API와 호출 흐름

1. `Vehicle(context)` 생성 → `initialize()` → `getDoor()` 등으로 도메인 객체 획득.
2. `checkXxxCapabilities()`로 기능 접근 수준과 대상 영역·허용 범위를 확인.
3. 현재값은 `getCurrentXxx(onComplete, onFailed)`로 조회. 반환값을 즉시 받는 동기 함수가 아니다.
4. 변경은 `registerXxx(listener)`로 구독하고 `unregisterXxx(listener)`로 해제.
5. 제어는 해당 `setXxx` API를 사용하되, 쓰기 접근 및 사용 권한을 먼저 확인.
6. 비동기 작업과 구독 정리 후 `release()` 호출.

출처: [도메인 객체 획득](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object), [Capability](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability), [Get](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property), [Callback](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback)

구체적으로 확인된 API:

- 도어: `checkDoorPositionCapabilities`, `getCurrentDoorPosition`, `registerDoorPosition`, `unregisterDoorPosition`. 조회 대상 예: `DoorArea(DoorValue.ROW_1_LEFT)`. 콜백 값 `Int?`의 의미와 범위는 capability·해당 버전 명세로 확인한다. [도어 기준 예제](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)
- 표시 단위: `getDisplay().getCurrentTemperatureDisplayUnit()` → `TemperatureUnit?`. [Get 예제](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property)
- 에너지: `getCurrentEvEfficiency`, `registerEvBatteryStateOfCharge`; 차량 식별: `getVINInfo`. [릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)

### 권한

매니페스트 이름은 `pleos.car.permission.` 뒤에 아래 상수를 붙인다. 표는 대표 조회 권한이다. 각 함수의 `@RequiresPermission`이 최종 기준이며, 제어에는 `CONTROL_CAR_*` 계열 권한을 별도로 확인한다. [Vehicle 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)

| 조회 도메인 | 권한 상수 |
|---|---|
| Door | `CAR_DOORS` |
| Window, Wiper | `CAR_WINDOWS` |
| HVAC | `CAR_CLIMATE` |
| DrivingMode, Brake, Odometer | `CAR_DRIVE` |
| EvBattery, Fuel | `CAR_ENERGY` |
| CarInfo, Display | `CAR_INFO` |
| Light, TurnSignal | `CAR_LIGHTS` |
| SideMirror | `CAR_MIRRORS` |
| Safety, Tire | `CAR_SAFETY` |
| Seat | `CAR_SEATS` |
| Toll | `CAR_TOLL` |

### 제약과 오류

- `CapabilityConfig.accessType`은 `NONE`, `READ`, `WRITE`, `READ_WRITE`. 읽기에는 READ 또는 READ_WRITE가 필요하다. 영역별 `AreaBounds`의 최소·최대값을 확인한다. [Capability](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability)
- `onFailed(Exception)`에서 `VehicleException`일 때만 `errorCode`를 사용한다. 미지원 속성·영역, 권한 부족, 연결 실패, 주행 제한, 빈 결과를 구분한다. `null`을 정상 측정값으로 바꾸지 않는다. [오류 처리](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code)
- 최신 릴리스에서 **`VehicleSpeedListener`와 `onSpeedUpdated`가 삭제**되었다. 오래된 속도 구독 예제를 사용하지 않는다. `getCRNInfo`는 deprecated이며 `getVINInfo`로 전환 안내가 있다. [릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)

## 4. NaviHelper SDK

### 제공 데이터·주요 API

Pleos Navigation 앱에 목적지·경유지·경로 요청을 보내고 결과와 주행 정보를 콜백으로 받는다. 앱 안에 독립적인 내비 엔진을 넣는 API로 해석하지 않는다. 진입점 `NaviHelper`를 초기화하고 `NaviHelperEventListener`를 등록한 뒤 사용하며, 종료 시 같은 리스너를 제거하고 `release()`한다. [소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)

| 용도 | 주요 API / 이벤트 |
|---|---|
| 목적지 설정·확인·취소 | `requestRoute(RouteInfo)`, `getDestinationInfo()`, `cancelRoute()` |
| 경로 변경 | `requestReRoute()`, `changeRouteOption()` |
| 경유지 | `addWaypoint()`, `removeWaypoint()`, `requestRouteWithWaypoints()` |
| 경로·화면 요청 | `requestTrip()`, `requestNavigationScreen()` |
| 목적지·경유지·현재 위치 정보 | `onDestinationInfo`, `onWayPointInfo`, `onCurrentLocationInfo` |
| 운행·경로 안내 정보 | `onTripInfo`, `onDrivingInfo`, `onNavigationStatus`, `onTBTInfo`, `onCurrentRoadSpeedLimit` |
| 기타 정보 | 즐겨찾기·최근 목적지·카메라·주의 지점 관련 콜백 |
| 완료·오류 | `onRoutingCompleted()`, `onDestinationArrived()`, `onError()` |

출처: [목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations), [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners), [권한별 API](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions), [추가 API](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)

### 권한과 제약

| 매니페스트 권한 | 용도 |
|---|---|
| `pleos.car.permission.NAVI_ROUTE` | `get*Info()`, `requestTrip()` |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | 경로 설정·취소·재탐색, 경유지 추가·제거 등 |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen()` |

출처: [NaviHelper 권한](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)

- 요청 호출 성공은 경로 탐색 완료를 뜻하지 않는다. 정보 조회도 해당 콜백에서 결과를 받는다. 실제 이벤트는 Navigation 앱 구현과 상태에 따라 다르다. [목적지](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/destinations), [이벤트](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/event-listeners)
- 3.0.5에서 좌표 생성자 순서가 위도→경도로 변경되었다. Kotlin에서는 이름 있는 인자를 사용한다. 목적지·경유지의 잔여값 필드는 `remainDistance` / `remainTime`, 내비 상태는 `NavigationStatus` 기준으로 확인한다. [릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)

## 5. FusedLocation SDK

### 제공 데이터·주요 API

`LocationData`는 위도·경도·고도·진행 방향·신뢰도·타임스탬프를 제공한다. `FusedLocation`을 생성한 뒤 `isSupported()`로 환경을 확인하고 `initialize()`한다. [소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)

| API | 역할 |
|---|---|
| `getLastKnownLocation()` | 저장된 마지막 위치 1회 조회 |
| `registerFusedLocationCallback()` | 위치 변경 수신 |
| `unregisterFusedLocationCallback()` | 같은 리스너로 수신 해제 |
| `addProximityAlert()` / `removeProximityAlert()` | 지정 영역 진입·이탈 알림 등록·해제 |
| `showLocationAccessBlockedPopup()` | 위치 접근 차단 안내 |
| `release()` | 리소스 해제 |

출처: [소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro), [위치 데이터 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)

### 권한과 제약

- 요구 권한: **`pleos.car.permission.FUSED_LOCATION`**. 지원하지 않는 환경에서는 기능을 제공하지 않도록 처리한다. [소개의 권한·지원 환경](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
- 마지막 위치 조회는 새 위치 측정을 요청하지 않는다. 데이터가 없으면 `null`이다. `FusedLocationListener.onChanged(LocationData?)`의 null을 처리하고 `onError(Exception)`을 구현한다. [위치 접근](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)
- 공개 안내상 준비 중이다. 임의의 Android 위치 권한 추가나 NaviHelper 사용 가능 상태가 이 SDK의 접근 허용을 대신하지 않는다. [신청 안내](https://document.pleos.ai/docs/pleos-only/create-my-project)

## 6. TextToSpeech SDK

### 제공 데이터·주요 API

텍스트를 합성해 차량 오디오로 재생한다. 진입점은 `ai.pleos.playground.tts.TextToSpeech`이며 생성 시 Context와 Mode를 전달한다. [TTS 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)

주요 흐름은 `initialize()` → `addEventListener()` → 필요 시 `setLocale()` → 서버 모드 앱 등록 → `speak()` → `stop()` → `removeEventListener()` → `release()`다. 서버 연결 모드는 `OnServerConnectionListener.onConnected()` 이후 합성을 요청한다. [TTS 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)

| Mode | 네트워크·등록 조건 |
|---|---|
| `HYBRID` (기본) | 서버 우선, 연결 실패 시 온디바이스 전환. 서버 이용용 앱 등록 필요 |
| `SERVER` | 서버만 사용. 네트워크·앱 등록 필요, 실패 시 오류 |
| `ON_DEVICE` | 차량 내 엔진. 네트워크 및 서버용 앱 등록 불필요 |

출처: [TTS 모드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)

### 권한과 제약

- **`pleos.car.permission.TTS_SERVICE`**를 선언한다. 초기화·이벤트·앱 등록·합성에 필요하며 `PERMISSION_DENIED` 시 매니페스트와 환경의 허용 상태를 확인한다. 최신 개발자 가이드의 권한 페이지를 수집하지 못해 공식 기존 API Reference의 설명으로 확인했다. [TTS 권한](https://document.pleos.ai/api-reference/connect-sdk-pleos/TextToSpeech/declaring-permissions)
- ON_DEVICE의 서버 등록 면제는 SDK 사용 권한 면제를 의미하지 않는다. SDK 자체는 신청 안내상 준비 중이다. 서버용 Client ID·Client Secret은 Workspace 프로젝트에서 발급되며 저장소에 기록하지 않는다. [신청·프로젝트 정보](https://document.pleos.ai/docs/pleos-only/create-my-project)
- 3.3.1의 초기화 결과 리스너 및 오류 `ErrorType`을 기준으로 구현한다. 과거 문자열 오류 콜백을 그대로 복사하지 않는다. [릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)
- 최신 Maven 의존성과 실제 오디오 재생 가능 여부는 미검증이다. 과거 TTS 설정 페이지에는 `TextToSppech` 오기 및 2.x/3.2.1 예시가 있어 최신 버전 좌표를 추정해서 확정하지 않았다. [과거 설정 예시](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/TextToSpeech/tts-sdk-project-setup), [3.2.1 예시](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/TextToSpeech/importing-sdk)

## 7. SpeechToText SDK

### 제공 데이터·주요 API

음성을 텍스트로 변환해 인식 결과를 전달한다. **인식된 명령의 실제 실행은 수행하지 않는다.** 진입점 `ai.pleos.playground.stt.SpeechToText`에 Context와 Mode를 전달한다. [STT 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)

주요 흐름은 `initialize()` → `addListener(ResultListener)` → 필요 시 `setLocale()` → 서버 모드 앱 등록 → `request()` → `stop()` → `removeListener()` → `release()`다. [STT 소개](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)

| Mode | 네트워크·등록 조건 |
|---|---|
| `HYBRID` (기본) | 서버 인식 우선, 연결 실패 시 온디바이스 전환. 앱 등록 필요 |
| `SERVER` | 음성을 원격 서비스로 전송. 네트워크·앱 등록 필요 |
| `ON_DEVICE` | 차량 내 인식, 네트워크 및 서버용 앱 등록 불필요 |

출처: [STT 모드](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)

### 권한과 제약

- **`pleos.car.permission.STT_SERVICE`**를 선언한다. 초기화·리스너·앱 등록·인식·오디오 전송 관련 서비스에 필요하다. 최신 권한 페이지 수집 실패로 공식 기존 API Reference에서 확인했다. [STT 권한](https://document.pleos.ai/api-reference/connect-sdk-pleos/SpeechToText/declaring-permissions)
- 앱이 별도로 마이크를 직접 캡처하는 구현의 Android 권한은 이 SDK 권한과 구분해 확인해야 한다. 이 조사에서 `RECORD_AUDIO`의 SDK 필수 여부는 확정하지 않았다.
- 3.3.1은 `ResultListener.onError(ErrorType)`, 초기화 결과 리스너, 인식 출처 `InferenceSource`를 제공한다. 최신 타입에 맞춰 구현한다. [릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/sdk-261015)
- 실제 사용 가능 상태와 Maven 좌표는 추가 확인이 필요하다. STT 데모는 AVD 마이크 설정 및 PC 마이크 입력을 함께 점검한다. [신청 안내](https://document.pleos.ai/docs/pleos-only/create-my-project), [Emulator 마이크 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)

## 8. Windows 에뮬레이터 준비 절차

아래 절차는 공식 설치 문서를 요약한 것으로 이 PC에서 실행 완료한 절차는 아니다. [설치 및 실행](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)

1. Android Studio 및 Android Emulator 실행 환경을 준비한다. 앱 minSdk는 28 이상, Pleos 시스템 이미지는 **API 34**다. 두 숫자를 혼동하지 않는다.
2. `Tools > SDK Manager > SDK Update Sites > +`에서 이름 `Pleos Connect System Image`, Windows/Linux URL `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`을 추가한다.
3. `SDK Platforms`에서 `Show Package Details`를 켜고 API 34의 Pleos Connect System Image를 설치한다. Android Studio를 재시작한다.
4. `Tools > Device Manager > Create device > Automotive > Pleos Connect v2.1.0`을 선택한다.
5. Advanced Settings에서 Default boot를 **Cold**, RAM을 **4 GB**로 설정한다. API 34의 `Pleos Connect v2.1.0 APIs`를 선택해 생성·실행한다.

공식 설정 문서는 GPU를 필수로 안내한다. 최소 예시 RTX 3060 Laptop 6GB / RX 6600M 8GB 또는 RX 6700S 8GB, 권장 예시 RTX 4060/5060 Laptop 8GB 이상이다. 이 PC의 충족 여부는 이번 작업에서 확인하지 않았다. **Quick boot에서는 VHAL을 사용할 수 없다.** STT 사용 시 Extended Controls > Microphone에서 호스트 오디오 입력을 활성화한다. [Emulator 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator)

### Virtual Console

실행 중인 AVD에서 다음을 수행하고 PC 브라우저로 `http://127.0.0.1:8088/`에 접속한다.

```powershell
adb devices
adb forward tcp:8088 tcp:8088
```

여러 디바이스가 연결되어 있다면 대상 시리얼을 지정한다.

```powershell
adb -s <emulator-serial> forward tcp:8088 tcp:8088
```

Console은 기어·도어·조명·공조·에너지 등 상태와 이벤트를 조작하는 테스트 도구다. 연결 실패 시 AVD 2.1.0 이상, ADB 연결, 포트 포워딩, 호스트 8088 포트 충돌을 확인한다. Console에 기능 카드가 있다는 사실만으로 앱의 해당 SDK/API 지원을 보장하지 않는다. [Console 셋업](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)

현재 Emulator 릴리스에서 **LLM SDK·CRN·Bluetooth 동글 연동 제공이 종료**되었다. 오래된 설정 설명을 적용하지 않는다. STT·TTS는 그 자체로 LLM 기능을 제공하는 SDK가 아니다. [Emulator 릴리스 노트](https://document.pleos.ai/docs/pleos-only/release-note/emulator-261015)

### T-01b 실행 기록 — 2026-10-21

사용자의 T-04 직후 진행 요청에 따라 환경 구성을 시작했다. **첫 시도에서는 제한된 실행 환경 때문에 보류했다. 이후 재시도 결과는 아래에 별도 기록했다.** 이 표는 첫 시도의 결과이며 현재 호스트의 설치·가속 상태를 나타내지 않는다.

| 항목 | 실제 확인 결과 |
|---|---|
| Android Studio | `C:\Program Files\Android\Android Studio` 존재 |
| 내장 Java | `jbr\bin\java.exe -version`: OpenJDK 25.0.3 |
| Android SDK | `C:\Users\lky09\AppData\Local\Android\Sdk` 존재 |
| 플랫폼 / Build Tools | `platforms\android-37.0`, `build-tools\36.0.0` 존재 |
| Emulator | `emulator.exe -version`: 37.2.12.0 |
| 시스템 이미지 | `system-images\android-37.0`만 확인. Pleos API 34 이미지 없음 |
| AVD | `Medium_Phone.avd`, `Medium_Phone_API_37.0.ini`만 확인 |
| Command-Line Tools | 기본 SDK 경로에 `cmdline-tools` 없음. `sdkmanager` 명령도 검색되지 않음 |
| Pleos 다운로드 | 공식 `crp-sys-img.xml`에 `Invoke-WebRequest -TimeoutSec 15`: 원격 서버 연결 실패 |
| Google 다운로드 | `repository2-3.xml`에 HEAD 요청: 원격 서버 연결 실패 |
| ADB | 기존 `adb.exe version`: `Cannot mkdir '\.android': Permission denied`. 작업 폴더 아래 임시 Android 사용자 경로를 지정한 재시도도 동일. 디바이스 목록 확인 전 실패 |
| 가속 검사 | `emulator.exe -accel-check`: `accel: 6`, `Android Emulator hypervisor driver is not installed on this machine`. 일반 사용자 세션에서 WHPX 포함 재확인 필요 |
| GPU | CIM 조회 접근 거부. 실제 GPU 사양 미확인 |

첫 시도에서는 작업 폴더 내부 쓰기만 허용됐다. 당시에는 기존 사용자 SDK·AVD 위치에 패키지나 설정을 설치하지 않았고, 사용자/시스템 영구 환경 변수도 변경하지 않았다. 시도한 환경 변수는 해당 명령 프로세스 안에서만 적용됐다. `.gradle/pleos-toolchain/android-user`에는 임시 경로만 만들었다. `.gradle/`은 기존 gitignore 대상이다.

**Java 주의:** 내장 JBR 25를 샘플의 Gradle 8.10.2 실행용으로 그대로 지정하지 않는다. Java 25 실행 지원은 Gradle 9.1.0 이상이며, 샘플의 JDK 기준은 17이다. 프로젝트 버전을 바꾸는 대신 기존 샘플 조합 사용 시 JDK 17을 별도로 준비하는 방향을 제안한다. [Gradle Java 호환성](https://docs.gradle.org/current/userguide/compatibility.html), [Pleos 샘플 환경](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app)

#### 설치 권한과 다운로드가 가능한 세션에서 이어갈 순서

1. Android Studio `SDK Manager > SDK Tools`에서 Android SDK Command-line Tools를 설치한다. `sdkmanager`는 이 패키지에 포함된다. 최신 Android 안내는 Android CLI도 권장하지만, T-01b의 기존 도구 흐름은 사용 가능 여부를 확인한 뒤 진행한다. [공식 sdkmanager 안내](https://developer.android.com/tools/sdkmanager)
2. 위 설치 절차대로 Pleos Update Site를 등록하고 API 34 이미지를 설치한다. Pleos 패키지 ID는 저장소 XML을 가져오지 못해 **미확인**이다. `system-images;android-34;...` 값을 추정해 설치 명령으로 고정하지 않는다.
3. 빌드용 JDK 17 경로를 확인한 다음 일반 PowerShell에서 아래처럼 **세션 환경**을 설정한다. 영구 환경 설정은 경로 확인 후 별도로 적용한다.

   ```powershell
   # Replace this with the actual JDK 17 installation directory.
   $pleosJdkDir = 'C:\path\to\jdk-17'
   $pleosSdkDir = 'C:\Users\lky09\AppData\Local\Android\Sdk'
   if (-not (Test-Path -LiteralPath "$pleosJdkDir\bin\java.exe")) {
       throw 'Set pleosJdkDir to an installed JDK 17 directory.'
   }
   $env:JAVA_HOME = $pleosJdkDir
   $env:ANDROID_HOME = $pleosSdkDir
   $env:PATH = "$pleosJdkDir\bin;$pleosSdkDir\platform-tools;$pleosSdkDir\emulator;$pleosSdkDir\cmdline-tools\latest\bin;$env:PATH"
   java -version
   sdkmanager --list
   ```

4. 샘플과 같은 빌드 조합을 채택할 경우 Android SDK Platform 35와 Build Tools 35.0.0을 준비한다. 기존 플랫폼 37을 삭제할 필요는 없다. 아래는 **미실행 재개 명령**이며 라이선스 확인은 설치 세션에서 직접 진행한다.

   ```powershell
   sdkmanager "platform-tools" "emulator" "platforms;android-35" "build-tools;35.0.0"
   ```

5. Pleos AVD를 Cold boot / RAM 4 GB로 생성한다. 일반 사용자 PowerShell에서 `emulator -accel-check`를 재실행하고 사용 가능한 가속기를 확인한다. 필요한 경우 Windows Hypervisor Platform 및 BIOS 가상화 설정을 확인한다. 이 환경에서는 Windows 기능 변경·드라이버 설치·재부팅을 수행하지 않았다. [공식 Windows 가속 설정](https://developer.android.com/studio/run/emulator-acceleration)
6. Pleos AVD 부팅 후 `adb devices`에서 해당 디바이스의 `device` 상태를 확인하고, `adb shell getprop sys.boot_completed`가 `1`인지 확인한다. 여러 디바이스일 때는 `-s <serial>`을 붙인다. Console 8088 포트도 위 절차대로 연결한다.
7. 성공한 패키지 ID·버전, 실제 JDK 경로, AVD명, 가속 검사 및 ADB 결과를 이 절에 추가한 뒤 T-01b를 리뷰대기로 바꾼다. 첫 시도에서는 Pleos 부팅·ADB 연결 성공을 확인하지 못했다.

### T-01b 재시도 — 2026-10-21 22:40 이후, 부팅 검증 성공

권한 제한 해제 후 같은 호스트 사용자(`lky09`)로 재시도하여 설치·Cold boot·ADB 연결·Console 접속을 확인했다. **T-01b 완료 기준을 충족하여 리뷰대기로 넘긴다.** 앱 프로젝트 빌드와 Vehicle/Navi API 호출은 T-05에서 검증한다.

| 항목 | 설치·검증 결과 |
|---|---|
| Android SDK 루트 | `C:\Users\lky09\AppData\Local\Android\Sdk` |
| JDK 17 | Temurin `17.0.20.1+1`, `C:\Users\lky09\AppData\Local\Android\jdks\jdk-17.0.20.1+1` |
| Command-Line Tools | `cmdline-tools\latest`, 배포 리비전 `23.0` |
| SDK 관리 CLI | `android.exe` `1.0.16625370`; 최신 `sdkmanager.bat`는 이 CLI로 위임 |
| 새 빌드 패키지 | `platforms;android-35` revision 2, `build-tools;35.0.0` |
| 기존 도구 | Emulator `37.2.12.0`, ADB `37.0.1-15733141` 유지 |
| Pleos 시스템 이미지 | `system-images;android-34;pleos_car_emulator;x86_64`, 공식 저장소 revision 2 |
| 이미지 위치 | `Sdk\system-images\android-34\pleos_car_emulator\x86_64` |
| AVD | `Pleos_Connect_2_1_API_34`, `C:\Users\lky09\.android\avd\Pleos_Connect_2_1_API_34.avd` |
| 하드웨어 프로필 | 공식 이미지의 `Pleos_Connect_v2_0` (표시 이름: Pleos Connect v2.1.0), 2560×1440 |
| AVD 설정 | RAM 4096 MB, CPU 4개, GPU host, Cold boot, 마이크 입력 활성 |
| 가속 검사 | `accel: 0`, `WHPX(10.0.26200) is installed and usable` |
| GPU / 시간대 | NVIDIA GeForce RTX 3070 확인, AVD `persist.sys.timezone=Asia/Seoul` 설정 |
| 실행 | 창 없이 백그라운드 실행, 포트 5556, 시리얼 `emulator-5556` |
| 부팅·ADB | `sys.boot_completed=1`, bootanim `stopped`, `adb devices -l` 상태 `device` |
| Console | `adb -s emulator-5556 forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088/` HTTP 200 |
| 차량 서비스 | `ai.pleos.playground.service.vehicle` versionName `2.3.22` |
| Navi 서비스 | `ai.pleos.playground.service.navi` versionName `3.0.4` |
| Navigation 앱 | `ai.umos.maps.android.navigation.app` versionName `3.3.2-rc5-hotfix.3` |

#### 설치 과정과 확인한 차이

1. [Pleos 공식 이미지 저장소 XML](https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml)에서 패키지 ID·revision·ZIP URL·크기·SHA-1을 읽었다. ZIP은 공식 서버에서 직접 내려받고 SHA-1을 대조했다. 설치 이미지 압축 해제 크기는 약 18.29 GiB다.
2. Google 공식 저장소의 Windows [Command-Line Tools ZIP](https://dl.google.com/android/repository/commandlinetools-win-16111833_latest.zip)을 `cmdline-tools\latest`에 설치했다. [Adoptium 공식 API](https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse)가 제공한 JDK ZIP과 SHA-256을 대조해 JDK 17을 사용자 영역에 설치했다.
3. Pleos ZIP의 `crp_avd_out_x86_64/` 아래 파일을 공식 패키지 경로에 설치했다. ZIP의 `source.properties`는 revision 1·tag `android-automotive`를 담고 있어 최신 관리 도구가 공식 XML의 패키지 ID와 다르게 추론했다. Android 도구가 생성한 유효한 `package.xml` 스키마를 유지하면서 path·revision·표시명·vendor를 공식 XML 값으로 맞췄다. 이미지 바이너리와 원본 `source.properties`는 바꾸지 않았다. 이후 `avdmanager list avd`에서 경고 없이 올바른 Pleos 대상이 표시됐다.
4. `C:\Users\lky09\.android\repositories.cfg`에 Pleos Update Site URL을 등록했다. Android Studio에서 목록이 바로 갱신되지 않으면 SDK Update Sites에서 같은 URL의 표시 여부를 확인한다.
5. 최신 `sdkmanager.bat`의 세미콜론 패키지 인자 전달이 `platforms`/`android-35`처럼 분리되어 설치되지 않았다. 실제 설치에는 아래 Android CLI 명령을 사용했다. `--ignore-outdated-xmls`는 위의 Pleos 로컬 메타데이터가 다시 잘못 추론되는 것을 방지하기 위해 지정했다.

   ```powershell
   $pleosSdkDir = 'C:\Users\lky09\AppData\Local\Android\Sdk'
   & "$pleosSdkDir\cmdline-tools\latest\bin\android.exe" --sdk $pleosSdkDir sdk --ignore-outdated-xmls install 'platforms/android-35'
   & "$pleosSdkDir\cmdline-tools\latest\bin\android.exe" --sdk $pleosSdkDir sdk --ignore-outdated-xmls install 'build-tools/35.0.0'
   ```

6. `avdmanager`로 AVD를 생성한 뒤 `config.ini`의 RAM 4096 MB, CPU 4, `fastboot.forceColdBoot=yes`, `fastboot.forceFastBoot=no`, GPU host를 설정했다. 기존 Medium Phone AVD와 플랫폼 37은 그대로 두었다.

   ```powershell
   $env:JAVA_HOME = 'C:\Users\lky09\AppData\Local\Android\jdks\jdk-17.0.20.1+1'
   $pleosSdkDir = 'C:\Users\lky09\AppData\Local\Android\Sdk'
   # Already created; do not recreate or overwrite the existing AVD.
   # avdmanager create avd --name Pleos_Connect_2_1_API_34 --package 'system-images;android-34;pleos_car_emulator;x86_64' --device Pleos_Connect_v2_0
   & "$pleosSdkDir\emulator\emulator.exe" -avd Pleos_Connect_2_1_API_34 -no-window -no-snapshot-load -no-snapshot-save -gpu host -memory 4096 -port 5556
   ```

사용자 범위 `JAVA_HOME`, `ANDROID_HOME`, PATH의 JDK/bin·platform-tools·emulator·cmdline-tools/latest/bin을 영구 저장했다. 기존 값은 gitignore 대상 `.gradle/pleos-toolchain/user-environment-before.json`에 백업했다. 시스템 범위 변수는 변경하지 않았다. **새 터미널부터 적용되며 이미 실행 중인 IDE/터미널은 재시작하거나 세션 변수를 적용해야 한다.** Android Studio의 Gradle JDK도 JDK 17로 선택해야 한다.

다운로드 무결성:

| 파일 | 공식 제공 해시와 대조 결과 |
|---|---|
| Pleos ZIP, 4,087,751,964 bytes | SHA-1 `b4f858d5bcc6c71506d7edfc872bcb9e5a11acaa` 일치 |
| Windows Command-Line Tools ZIP | SHA-1 `57d04f2d75eb8e8fffc5000a987e5de4b5a63e9d` 일치 |
| Temurin JDK ZIP | SHA-256 `e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0` 일치 |

검증한 설치 ZIP 3개(합계 약 4.1 GiB)는 `.gradle/pleos-toolchain/`에 남아 있다. 설치 후 캐시 삭제 명령이 자동 검토에서 정책 차단되어 삭제하지 않았다. 설치된 SDK/JDK/AVD와는 별개의 캐시이며 커밋 대상이 아니다.

#### 재실행·접속

현재 headless 인스턴스는 실행 중이다. 새 인스턴스를 동시에 띄우지 말고 아래 연결 확인부터 수행한다.

```powershell
adb devices -l
adb -s emulator-5556 shell getprop sys.boot_completed
adb -s emulator-5556 forward tcp:8088 tcp:8088
# Open http://127.0.0.1:8088/ in a PC browser.
```

화면이 필요한 경우 현재 인스턴스를 종료한 뒤 Android Studio Device Manager에서 `Pleos_Connect_2_1_API_34`를 실행한다. CLI로 실행하려면 아래처럼 `-no-window`를 생략한다. 시리얼은 새 실행 시 달라질 수 있다.

```powershell
adb -s emulator-5556 emu kill
emulator -avd Pleos_Connect_2_1_API_34 -no-snapshot-load -gpu host -memory 4096
```

#### T-05에서 추가 확인할 항목

- 부팅 캡처는 `.gradle/pleos-toolchain/pleos-boot.png`, 후속 캡처는 `pleos-ready.png`에 있다. 차량 홈 UI가 표시됐고 내비는 초기 로딩 후 검색·지도 컨트롤까지 표시됐지만 지도 타일은 비어 있다. Console HTTP 200 및 Navi 서비스 설치가 내비 경로 탐색·시뮬레이션 성공을 보장하지 않는다.
- Android RecognitionService 조회 결과 `ai.umos.ambientai/.service.AmbRecognitionService`, TTS 서비스 조회 결과 `ai.pleos.playground.caas/.service.speech.SpeechService`가 있다. 이는 서비스 선언의 존재 확인이다. 앱에서 `SpeechRecognizer.isRecognitionAvailable()` 및 TTS 엔진 목록/초기화·실제 인식·재생 성공은 아직 검증하지 않았다.
- SDK Maven 의존성·앱 빌드, 도어 조회·구독, NaviHelper 목적지/경로/즐겨찾기 콜백, 마이크 입력·음성 기능은 T-05로 남긴다. Navi Service 3.0.4와 SDK 문서 3.0.5의 실제 호환성도 앱 호출로 확인해야 한다.

### 새 PC 재구성 — 2026-10-21 23:08, 부팅 검증 성공

개발 PC를 바꿨다. 현재 PC의 사용자는 `user`이고, 저장소는 **`C:\dev\hmg-app`**(영문 경로)에 있다. 위 절차를 같은 버전으로 다시 수행했다. 위 표의 `C:\Users\lky09\...` 경로는 이전 PC 기준이다.

| 항목 | 결과 |
|---|---|
| 하드웨어 | Ryzen 9 9900X, RAM 126 GB, NVIDIA RTX 5070 Ti 16 GB |
| 가속 | `emulator -accel-check` → `accel: 0`, WHPX 사용 가능. **이미 켜져 있어 Windows 기능 변경과 재부팅이 필요 없었다** |
| JDK 17 | Temurin `17.0.20.1+1`, `C:\Users\user\AppData\Local\Android\jdks\jdk-17.0.20.1+1` (SHA-256 일치) |
| SDK | `C:\Users\user\AppData\Local\Android\Sdk`. cmdline-tools 23.0 (SHA-1 일치), platform-tools 37.0.1, Emulator 37.2.12.0, `platforms/android-35` r2, `build-tools/35.0.0` |
| Pleos 이미지 | 위와 같은 ZIP, SHA-1 `b4f858d5…` 일치. `Sdk\system-images\android-34\pleos_car_emulator\x86_64` |
| Android Studio | `winget install Google.AndroidStudio` (2026.2.1.8), `C:\Program Files\Android\Android Studio` |
| 환경 변수 | 사용자 범위 `JAVA_HOME`, `ANDROID_HOME`, PATH(JDK bin·platform-tools·emulator·cmdline-tools/latest/bin). 기존 값은 `C:\dev\pleos-downloads\user-environment-before.json`에 백업 |
| AVD | `Pleos_Connect_2_1_API_34`, RAM 4096 MB, CPU 4, GPU host, Cold boot, 마이크 입력, 2560×1440 |
| 검증 | 40초 안에 `sys.boot_completed=1`, bootanim `stopped`, `adb devices -l` → `emulator-5556 device`, Console HTTP 200, Vehicle Service 2.3.22 / Navi Service 3.0.4 / Navigation 3.3.2-rc5-hotfix.3, 시간대 `Asia/Seoul` |

이전 PC와 달랐던 점:

1. **`package.xml`**: 공식 XML 값을 담아 `sys-img2/01` 네임스페이스로 작성하면 Android 도구가 손상된 파일로 판단했다. **`sys-img2/05` 네임스페이스로 작성하고 `obsolete="false"`를 붙이면** 경고 없이 인식된다. 값(path, revision 2, tag `android-automotive`, vendor `pleos`, 표시명)은 공식 XML 그대로다.
2. **`avdmanager create avd`가 `null`만 출력하고 실패했다.** 그래서 AVD를 직접 만들었다. `C:\Users\user\.android\avd\Pleos_Connect_2_1_API_34.ini`에 경로를 적고, `.avd\config.ini`에 이미지의 `devices.xml`(Pleos_Connect_v2_0) 값과 위 설정을 넣었다. `emulator -list-avds`로 인식되고 부팅된다.
3. **AVD 기본 시간대가 `America/Los_Angeles`였다.** `adb -s emulator-5556 shell cmd alarm set-timezone Asia/Seoul`로 바꿨다.
4. 설치 ZIP과 에뮬레이터 로그는 저장소 밖 `C:\dev\pleos-downloads`에 둔다. 필요 없으면 지워도 된다.

실행 명령은 위 "재실행·접속"과 같다. `-no-window`로 띄운 headless 인스턴스가 `emulator-5556`으로 실행 중이다.

## 9. T-05로 넘길 구현·검증 기준 (제안)

설계 확정이 아닌 조사에 따른 구현 제안이다. T-05 목표인 ‘Vehicle 값 하나 표시’에는 **도어 위치 조회**를 우선 검토할 수 있다. 삭제된 속도 리스너에 의존하지 않고 공식 기준 예제와 대조할 수 있기 때문이다.

1. T-01 및 T-01b 완료 후 SDK·Service·AVD 실제 버전을 기록하고 Vehicle 2.3.2 의존성 해석·빌드를 확인한다.
2. `pleos.car.permission.CAR_DOORS`를 선언하고 `Vehicle.initialize()` 후 Door 도메인을 얻는다.
3. `checkDoorPositionCapabilities()`의 읽기 접근과 `ROW_1_LEFT` 영역을 확인한다.
4. `getCurrentDoorPosition()` 값을 화면에 표시한다. 초기화 함수 반환만으로 연결 성공을 표시하지 않는다.
5. 구독을 추가한다면 `DoorPositionListener.onPositionUpdated(Int?)`와 `onFailed(Exception)`을 처리하고 동일 리스너로 해제한다.
6. Virtual Console에서 도어 값을 변경하고 실제 SDK 콜백·화면 반영을 확인한다. 미지원·빈 값·권한 오류도 정상 측정값과 구분해 표시한다.
7. 비동기 작업·구독 종료 후 release하며, 실제 결과와 실행 명령을 HANDOFF에 남긴다.

근거: [Vehicle 샘플 가이드](https://document.pleos.ai/docs/pleos-only/tutorial/vehicle-sdk-sample-app), [Capability](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability), [Get](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/get-property), [Callback](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/callback)

## 10. 미검증 항목과 다음 확인 요청

| 항목 | 확인 담당·시점 |
|---|---|
| Workspace 로그인 후 SDK 실제 상태 및 공모전 전용 권한·제공 범위 | 사용자: 프로젝트 생성 후, 필요하면 10-28 설명회 질의 |
| TTS·STT 최신 Maven 좌표, 권한 페이지와 현행 API 일치 | 해당 기능 채택 시 Codex: 최신 문서/배포물 확인 |
| 호스트 GPU·가상화 및 AVD 실행 가능 여부 | 가상화·Pleos 부팅 확인 완료(T-01b). 실제 앱 기능은 T-05 |
| AVD에 탑재된 실제 Vehicle Service 및 Navi 앱 기능 지원 | 서비스 버전 확인 완료(T-01b); API 기능 동작은 T-05 |
| 실제 위치 입력·음성 입력·서버 연결·언어별 동작 | 해당 기능 접근 권한 확보 후 검증 |
| 아이디어가 준비 중 SDK 또는 종료된 LLM SDK에 필수 의존하는지 | Claude T-02 기획 검토 |

자료 검증 범위: 공식 문서의 API 이름·권한·버전·설정 링크 교차 확인. 앱 소스가 없는 조사 작업이므로 빌드·기기 테스트는 수행하지 않았다. 검색 결과와 이전 경로는 최신 가이드와 구분해 사용했다.

## 11. T-05 실측

측정: **2026-10-27, Codex**, 현재 PC `C:\dev\hmg-app`. 새 `:app` 진단 앱으로 SDK를 직접 호출했다. [환경·권한 조회 원문](spikes/t05/environment.txt). `core`와 포트 구현은 변경하지 않았다. 아래 판정은 이 AVD·SDK 조합에 한정한다. **동작 7 / 부분 6 / 불가 2**이며, 부분 항목 안의 미확인 범위는 따로 표시했다.

### 환경·검증

- JDK 17.0.20.1+1, Gradle 8.10.2, Kotlin 2.1.21, AGP **8.7.2**, Compose BOM 2025.05.01. [Kotlin 공식 호환 표](https://kotlinlang.org/docs/gradle-configure-project.html#apply-the-plugin)의 2.1.20–2.1.21 지원 범위(Gradle 7.6.3–8.12.1 / AGP 7.3.1–8.7.2) 안에서 선택했다. [AGP 8.7 기준](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes): JDK 17, Gradle 최소 8.9, API 35 지원.
- namespace/applicationId `com.ontheway`, minSdk 28, compileSdk/targetSdk 35, Build Tools 35.0.0, `implementation(project(":core"))`. Pleos Maven에서 Vehicle 2.3.2·NaviHelper 3.0.5 다운로드·의존성 해석 성공. NaviHelper 3.0.5의 POM은 `navi-data:3.0.4`, `navi-aidl:3.0.1`을 사용한다.
- AVD `Pleos_Connect_2_1_API_34`, `emulator-5556`, Android 14/API 34, foreground user **10**. Vehicle Service 2.3.22 / Navi Service 3.0.4 / Navigation 3.3.2-rc5-hotfix.3. AVD 설정·이미지는 수정하지 않았다. 작업 중 재부팅하지 않았고 `America/Los_Angeles`였던 시간대를 `cmd alarm set-timezone Asia/Seoul`로 바로잡았다.
- `CAR_DRIVE`, `CAR_ENERGY`, `CAR_SAFETY`, `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` 선언. 일반 권한은 설치 시 granted. Android 음성 조회용 package visibility와 `RECORD_AUDIO`도 선언했다. Telltales는 `CAR_SAFETY: granted=true`인데도 아래 권한 오류가 났다. coordinator 지시로 별도/private 권한 시도는 중단했다.
- 루트 `.\gradlew.bat :app:assembleDebug` **성공**, `.\gradlew.bat :core:test` **성공**(XML 합계 141개, 실패·오류 0). [앱 빌드 로그](spikes/t05/assemble-debug.txt), [core 로그](spikes/t05/core-test.txt). 마지막 core 실행은 기존 성공 결과를 재사용한 UP-TO-DATE이며 이번 작업의 앞선 실행에서 테스트도 수행했다.
- `.\gradlew.bat :app:connectedDebugAndroidTest` **1개 통과**(receiver 정상·비정상 4경우), [계측 로그](spikes/t05/receiver-test.txt). 디버그 APK 설치·실행 성공. APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성되며 커밋하지 않는다.

### 15개 항목 결과

표의 콜백 값·오류는 [callbacks.txt](spikes/t05/callbacks.txt)에 원래 타임스탬프와 함께 발췌했다(같은 항목의 같은 값 반복은 생략). API 존재 여부는 공식 [Brake](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/brake/entry), [EvBattery](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/ev-battery/entry), [Odometer](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/odometer/entry), [Safety](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle/ai.pleos.playground.vehicle.api/safety/entry) 문서와 실제 AAR의 공개 시그니처를 대조했다. 일부 deprecated API는 문서 목록보다 AAR에 더 많이 남아 있다.

| 번호·항목 | 결과 | 근거: 값·콜백·캡처 | 우회 방법 / app 계획에 반영할 제한 |
|---|---|---|---|
| 1. 주차 브레이크(Brake), 정차 대안 | **부분** | Brake 공개 API는 페달뿐: capability `READ`, 현재·콜백 `false`. 페달은 주차 브레이크가 아니다. Safety Telltales에는 `PARKING_BRAKE`·`ELECTRONIC_PARKING_BRAKE` 유형이 있으나 capability가 `SecurityException: checkTelltalesCapabilities need Permission`. 기어는 현재·콜백 `GEAR_PARK`→`GEAR_DRIVE`→`GEAR_PARK` 수신. [api-surface.txt](spikes/t05/api-surface.txt), callbacks의 `01`. | coordinator 결정: **기어 P**를 정차 근거로, CarUxRestrictions를 예비로 검토. Gear API에는 capability가 없고 Google `GEAR_SELECTION` 사용을 권하는 deprecated 경고가 나온다. 실제 속도를 읽는 Vehicle 공개 API는 확인하지 못했으며 cruise target speed를 현재 속도로 쓰면 안 된다. Console D/0km/h·D/30km/h에서 Ux `true/80`, P/0에서 `false/0`(11번). 표시등 권한 우회는 하지 않았다. |
| 2. EV SOC | **동작** | capability `READ`, 현재 `100`, 구독 콜백 `100`→`60`. Console Battery SOC를 60으로 바꾸자 화면 콜백도 60. [변경 전](spikes/t05/vehicle-before.png) / [변경 후](spikes/t05/vehicle-after.png). | `getEvBatteryStateOfCharge`·`registerEvBatteryStateOfCharge` 사용. 최초 현재값과 이후 콜백을 구분한다. `null`은 측정값 없음으로 표시. |
| 3. 주행 가능 거리 | **동작** | Odometer `getRangeRemainingDistance` 현재 `0.0`, `registerRangeRemainingListener` 콜백 `0.0`→`150000.0`. Console Remaining Range 150000m 설정 후 반영, 차량 UI도 150km. `checkRange...Capabilities` API는 없다. callbacks `03`, [병렬 화면](spikes/t05/multiwindow-parked.png). | AAR에 남은 deprecated 조회·구독은 이 AVD에서 동작한다. 신규 구현은 SDK 경고의 Google `RANGE_REMAINING`도 검토하고 단위를 m로 유지. capability API가 없는 것을 `NONE`으로 꾸며 쓰지 않는다. |
| 4. 충전 중 여부 | **부분** | `checkEvChargeSwitchCapabilities=READ_WRITE`, `getEvChargeSwitchState=false`→Console Charge Switch On→다시 조회 `true`. 충전 스위치 구독 API와 실제 전류가 흐르는 charging state API는 EvBattery 공개 표면에서 확인하지 못했다. callbacks `04`. | 스위치 ON만으로 S5의 실제 충전 시작이라고 판정하지 않는다. 실제 충전 상태 API 또는 제공 범위를 추가 확인하고, 확인 전에는 수동 등록·시연 값 등 설계 대안을 Claude가 결정한다. |
| 5. Odometer 트립 시간 | **불가** | 현재/누적/충전 이후 세 capability 모두 `ERROR_NOT_SUPPORTED_PROPERTY`. 읽기 지원을 얻지 못했으므로 get·구독을 진행하지 않았다. Console 48개 카드에는 누적 Odometer 거리는 있지만 트립 시간 입력 카드는 없었다. callbacks `05`. | 정차·주행 전환을 확인한 앱의 Clock으로 연속 주행 시간을 재는 설계 검토. SDK 트립 시간을 정상 0으로 대체하지 않는다. |
| 6. 목적지·경로·시뮬레이션·TBT·장소 필드 | **부분** | `requestRoute`(42dot, 위도 37.48544722/경도 127.03636666) 후 `onRouteStarted(duration=60,distance=50)`, ROUTE_DRIVE, `onDestinationInfo(name=42dot,latitude=37.41289678,longitude=127.09383464,remainDistance=50,remainTime=60)` 수신. 경유 후 거리 90으로 변했고 `onDrivingInfo`, TURN_LEFT/TURN_WAY/TURN_ARRIVE TBT도 수신. `onRoutingCompleted`는 관측하지 못했다. DestinationInfo에는 이름·좌표·POI ID/provider가 있고 **카테고리는 없다**. callbacks `06`, [내비·앱](spikes/t05/multiwindow-parked.png). | **요청 좌표와 실제 목적지 콜백 좌표가 다르다.** 이 AVD의 값으로 S4·S6 장소 매칭을 보장할 수 없다. GPS를 경로 따라 이동시키는 모의주행은 **미확인**: Console 차량 속도 30은 확인했지만 현재 위치는 고정됐고 내비 모의주행 UI를 찾지 못했다. 경로 시작과 경로 재생을 구분한다. 시연은 가짜 NaviPort 경로 재생도 검토. |
| 7. 즐겨찾기·최근 목적지 | **동작** | `onBookmarkInfo` HOME/OFFICE 두 항목, 이름 빈 문자열·좌표 0/0. 초기 최근 목적지 `[]`, 경로 요청 후 `onRecentDestinationInfo`에 42dot·소프트웨어드림센터의 요청 이름·좌표·POI ID 수신. callbacks `07`. | 콜백 수신 성공과 유효한 저장 장소 유무를 구분. 빈 HOME/OFFICE를 단골 장소로 쓰지 않는다. populated 즐겨찾기의 저장·재조회는 이번 시험에서 하지 않았다. |
| 8. 경로 없는 현재 위치 | **동작** | 최초 isRouting=false/SAFE_GUIDING·빈 목적지 상태에서 `getCurrentLocationInfo`→`onCurrentLocationInfo(latitude=37.4127212,longitude=127.094681,address=수정구 시흥동,routeId="")`. callbacks `08`. | 경로 안내 없이도 **조회 요청의 콜백**을 받는다. 경로 없는 자동 연속 위치 push와 실제 GPS 이동은 미확인. 충전 기록 위치를 쓸 때 좌표 유효성·갱신 시점을 확인한다. |
| 9. 경유지 추가 성공·실패·ETA | **부분** | 경로 있을 때 `onWaypointChanged(index=FIRST,cmdType=ADD)`와 `onWayPointInfo` 수신. 기준 remainTime=60, 재탐색 중 빈 목적지 0으로 잠시 변하고 완료 후 remainDistance=90, remainTime=60(**차이 0초**). 경유지 좌표는 0/0. 경로 취소 후 addWaypoint는 `onError(CAUSE_SEARCH_ROUTE)`. callbacks의 16:04~16:05. | 전송 함수 반환을 성공으로 표시하지 않는다. 활성 경로·변경 완료 후 유효한 ETA만 비교하고 재탐색 중 0을 쓰지 않는다. 이 AVD로 양의 우회 시간 증가를 검증하지 못했다. |
| 10. Navi Service 3.0.4 / SDK 3.0.5 | **동작** | `initialize(onConnected)` connected, 위 경로·조회·경유지·오류 콜백에 연결/직렬화 오류 없음. SDK 3.0.5가 navi-data 3.0.4를 의존. callbacks `10`, [실제 API](spikes/t05/api-surface.txt). | 이번에 호출한 API 범위에서 호환. 모든 API·서비스 구현의 호환 보장은 아니다. Kotlin 좌표는 이름 있는 인자로 전달했다. |
| 11. CarUxRestrictions | **동작** | Android `Car` 비동기 연결 후 조회·리스너 등록 성공. P/0에서 `requiresDO=false,active=0`, D/0·30에서 `true,80`, 다시 P/0에서 `false,0`. callbacks `11`, [주행 제한 오버레이](spikes/t05/multiwindow-driving.png). | SDK 기어와 함께 정차·UI 제한 설계 근거. 제한 없음이 모든 실제 차량에서 주차 브레이크 ON과 동치라고 일반화하지 않는다. `useLibrary("android.car")`·uses-library 선언 사용. |
| 12. SpeechRecognizer ko-KR | **부분** | `isRecognitionAvailable=true`. `AmbRecognitionService onCreate/onStartListening` 바인딩·요청 수신, 앱 ko-KR 요청 확인. ready/최종 결과/표준 error code는 오지 않았고 앱이 30초 후 cancel, 서비스 onCancel. callbacks `12`, [서비스 발췌](spikes/t05/voice-service.txt). | coordinator가 호스트 입력 마이크 없음·DirectSoundCapture 초기화 실패를 확인. **실제 한국어 인식 미확인**. 서비스 존재만으로 사용할 수 있다고 판단하지 않는다. 마이크 연결 후 재측정. |
| 13. Android TextToSpeech ko-KR | **불가** | Android `engines=[]`, 초기화 `ERROR(-1)`, `setLanguage(ko-KR)=LANG_NOT_SUPPORTED(-2)`, speak 반환 `ERROR(-1)`, 완료 콜백 없음. pm query-services에는 Pleos SpeechService가 있지만 Android TTS 엔진 목록은 비어 있다. callbacks `13`. | Android 기본 TTS를 제품 기본 구현으로 정하기 어렵다. Pleos TTS 사용 승인·다른 엔진·클라우드 등은 후속 설계/별도 허용 범위 확인. 이번 작업에서 엔진 설치나 별도 음성 SDK 접근은 하지 않았다. |
| 14. 내비 옆 멀티 윈도우 | **동작** | Pleos 앱 영역 오른쪽 세 점 핸들을 왼쪽으로 드래그해 내비 왼쪽·진단 앱 오른쪽 표시. 앱 `inMultiWindowMode=true`, 좁은 창 폭 841dp. [정차 캡처](spikes/t05/multiwindow-parked.png), [주행 캡처](spikes/t05/multiwindow-driving.png). | `resizeableActivity=true`, Compose 화면이 좁아진 창에서도 표시된다. 주행 중 진단 앱은 시스템 오버레이로 제한되므로 제품 화면·DO 적합성은 별도 설계·심사 대상이다. |
| 15. Gleo Interface / T-08 | **부분** | 설치 직후 `schema.json` 읽기 확인, receiver→callback success·fail 및 Gleo 수신 확인. Sleeping 상태의 선제 callback은 Reject. Gleo가 자연어를 해석해 앱을 호출하는 것은 마이크 부재로 **미확인**. | [GLEO.md](GLEO.md)에 공식 형식·실제 schema·(a)~(f)·승인·재현 명령 정리. 앱의 주 음성 경로·선제 발화를 Gleo로 확정하지 않는다. |

### 재현·증거 읽는 법

```powershell
# JDK/SDK 경로는 8절의 현재 PC 경로. 일반적인 실행에는 추가 -P 옵션이 필요 없다.
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :core:test
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.ontheway/.MainActivity
adb -s emulator-5556 forward tcp:8088 tcp:8088
adb -s emulator-5556 shell cmd alarm set-timezone Asia/Seoul
adb -s emulator-5556 logcat -s T05
```

1. PC에서 `http://127.0.0.1:8088/` → Emulation Controls → Battery SOC / Remaining Range / Charge Switch를 바꾼다. 처음 두 값은 콜백으로, Charge Switch는 앱의 다시 조회 버튼으로 확인한다. [공식 Console 가이드](https://document.pleos.ai/docs/pleos-only/app-test-guide/virtual-console-guide).
2. Gear Selection 카드로 P/D를 바꾼다. 속도 30/0 시험은 Console UI가 사용하는 동일 HTTP 경로 `PUT /vehicle/controls?speed=30&accelerator=0&brake=0` 및 speed=0을 사용했다. VHAL 직접 쓰기나 권한 우회는 하지 않았다. 시험 후 P·속도 0으로 돌렸다.
3. 앱의 42dot 경로 요청 → 정보 조회 → 경유지 추가 → 경로 취소 → 경유지 추가로 성공·실패를 확인한다. 첫 SDK 연결·경로 요청 뒤 내비 화면이 앞으로 나오면 앱을 다시 실행해 진단 화면을 본다.
4. 위 PNG는 screencap 원본(각 1MB 미만), 로그는 해당 항목만 발췌했다. `vehicle-before/after`는 초기 앱의 SOC 100→60, `multiwindow-*`는 이후 표시등 진단 추가본이다. 원본 APK·AAR·전체 logcat·개인 키·키스토어는 저장소에 넣지 않았다.

**Claude 요청:** 정차를 주차 브레이크에 의존하던 DESIGN을 기어 P/Ux 기반으로 검토하고, 트립 시간·실제 충전 상태·Android TTS·AVD 내비 좌표·우회 ETA의 제한을 app 계획에 반영한다. 마이크 연결 후 STT/Gleo 활성 대화와 내비 GPS 경로 재생은 추가 실측 대상으로 남긴다.

## 12. T-14 앞길 안내 전제 실측

측정: **2026-10-28, Codex**, 기존 `emulator-5556` / Pleos Connect API 34 / user 10. 시험 코드는 `spikes/road-ahead/`, 전체 9개 항목·시연 후보·검증 명령은 [실측 보고](spikes/road-ahead.md), 외부 API 명세는 [APIS.md](APIS.md)에 있다. 기존 T-05/T-13 결과를 덮어쓰지 않고 추가 측정했다.

| 항목 | 이번 결과·계획에 반영할 점 |
|---|---|
| NaviHelper 3.0.5 | 연결·FREE_DRIVE/ROUTE_DRIVE·현재 위치·heading·TBT·경로 상태 수신. 울산 목적지 요청은 실제 목적지 좌표와 일치했고 경로367km/15,360초를 받았다. `trafficStatus`는 nullable이며 실제 필드명은 `trafficEventList`, CautionPoint 이벤트 타입은 `List<CautionPoint>`다. 돌발 목록은 비어 있었고 주의 지점 콜백은 관측하지 못해 해당 대상의 내비 자체 발화는 미확정. [공개 타입](spikes/road-ahead/navi-api.txt). |
| 위치 이동 | `adb emu geo fix`에 Android GPS는 울산 좌표·속도10.28888→0m/s·accuracy5m로 반응했다. NaviHelper는 성남37.4127212/127.094681·heading0에 고정됐다. GPS 주입에 SDK 위치가 따라오는 모의주행은 이번 AVD에서 검증되지 않았다. GPX 별도 재생은 하지 않았다. 전체 시연은 기록 데이터·위치 재생안을 우선 검토한다. |
| 정차용 속도 | 삭제된 Pleos VehicleSpeedListener 대신 **표준 AAOS CarPropertyManager / PERF_VEHICLE_SPEED**를 사용했다. `android.car.permission.CAR_SPEED`를 선언하고 실제 사용자10에 런타임 허용한 뒤 조회·약5Hz 콜백 성공. D/0은0m/s, D/실제29km/h는8.055555m/s·status0. 허용 전에는 SecurityException과 등록false였다. 센서 timestamp·상태·권한을 확인하고 null/오류를0으로 바꾸지 않는다. [측정 로그](spikes/road-ahead/ux-do-capture.txt). |
| Vehicle 2.3.2 날씨 관련 상태 | 앞Low/뒤High 와이퍼→getCurrentWiperState ON, 복원→OFF. 앞/뒤 성에 제거 false→true→false. 안개등은 capability ERROR_NOT_SUPPORTED_PROPERTY, 조회는 FRONT/REAR_FOG_LIGHTS_STATE 미지원 오류. Console Telltales 표시등을 안개등 속성으로 대체하지 않았다. [API 표면](spikes/road-ahead/vehicle-api.txt), [콜백](spikes/road-ahead/callbacks.txt). |
| D/0 화면 | 일반 Activity는 빨간불 정차와 같은 D/0에서도 시스템 가림 화면(true/80). `distractionOptimized=true`를 Activity에 넣은 시험 변형은 D/0·D/약29에서 보였다. 이는 메타데이터 효과이고 제품 UI 적합성·마켓 승인이 아니다. [일반 D0](spikes/road-ahead/ux-D0.png), [DO D0](spikes/road-ahead/ux-DO-D0.png), [DO 이동](spikes/road-ahead/ux-DO-D30.png). |
| Pleos TTS 3.3.1 | ON_DEVICE 한국어 시작·완료·RMS 수신. 사용자 청음 **들림(음량 작음)**. 6회씩 두 세트 모두 완료. 첫 세트 시작 중앙값7ms/최대18ms, 완료 중앙값2,926ms/최대2,950ms. `setAudioAttributes`로 NAVIGATION_GUIDANCE를 지정한 추가3회는 callback usage도12로 바뀌었다. SDK에는 직접 volume/gain setter가 없고 AudioManager15/15·fixed=true, CAR_CONTROL_AUDIO_VOLUME 거부(-1)였다. 속성 비교 뒤 MEDIA로 복원했다. [청음 기록](spikes/road-ahead/tts-listening.txt), [음량 비교](spikes/road-ahead/tts-volume-trial.txt), [공개 API](spikes/road-ahead/tts-api.txt). Android 기본 TTS로 대체하지 않았다. |
| 음성 겹침 감지 | 일반 앱의 AudioPlaybackCallback에서 내비 usage12/NAVIGATION_GUIDANCE, Pleos TTS usage1/MEDIA, 둘 다 SPEECH를 관측했다. UID/PID는익명화(-1). 동시 started 상태를 받아 자동 직렬화가 안 됨을 확인했다. dumpsys 표본의 ducked player 목록은 비었으나 상대 음량은 청음하지 못했다. 내비 활성 동안 대기하고 종료 뒤 대상·신호·나이를 재검사하는 방식 권고. 겹침 시험은 완료 콜백/타임아웃 처리 지연이 있어 단독 재생 지연과 구분한다. [근거](spikes/road-ahead/audio-overlap.txt). |

Gradle 설정·래퍼는 preset-control에서 복사했다. `android.car`는 컴파일에 `useLibrary`를 쓰되 manifest shared library를 required=false로 해야 이 AVD에 설치됐다(required=true는 INSTALL_FAILED_MISSING_SHARED_LIBRARY). Pleos 저장소를 `exclusiveContent`로 제한할 때 실제 TTS 전이 의존성 그룹 `ai.pleos.playground.security`도 명시해야 `universal-access:0.6.0`이 해석된다. [최종 빌드 로그](spikes/road-ahead/assemble-debug.txt).

외부 API에서는 울산 실시간 신호·중부내륙선 가시거리·ITS 재난/공사/사고 표본을 확보했다. 신호 명세는 센티초지만 울산 응답은1,000값/초로 감소했다. 신호 방향의 진입/진행 정의는 공개 설명으로 구별할 수 없었다. 재난과 일반 돌발은 별도 서비스이며, 기상 키는 공공데이터포털 일반 키와 다른 API허브 키가 필요했다. 설계 수정은 하지 않고 HANDOFF에 판단 요청을 남긴다.

종료 시 Park/0·와이퍼·성에 제거·공조·기본 GPS·Asia/Seoul을 복원하고 시험 앱을 종료했다. 내비 SAFE_GUIDING·빈 목적지를 확인했다. 재부팅·비공개 Binder 호출·VHAL 직접 쓰기·privileged 권한 우회는 하지 않았다. [복원 검증](spikes/road-ahead/restoration-check.txt).
