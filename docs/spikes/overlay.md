# T-21 내비 위 오버레이 실측

2026-10-29, 시험용 앱 `com.roadahead.spike.overlay`. 제품 앱에는 반영하지 않았다.

**이 AVD에서는 Pleos 설정으로 권한을 켤 수 있고, 내비 위 카드가 P/0과 D/30에서 보인다.** 홈·다른 앱·시험 Activity 종료 뒤에도 남는다. 다만 카드는 내비 창에 붙어 있지 않아 멀티 윈도우에서 다른 앱을 가린다. 실제 차량에서의 허용 및 장시간 생존은 확인하지 않았다.

## 환경과 시험 범위

- 기존 `emulator-5556`, Pleos Connect 2.1.0 / Android 14(API 34), user 10.
- 디스플레이 **2560×1440 px**, **160 dpi(density=1.0)**, 회전 0. 크기·밀도·회전은 바꾸지 않았다.
- 내비 `ai.umos.maps.android.navigation.app/.MainActivity`, 기존의 빈 `connect_empty` 지도. 경로 안내나 TTS를 시작하지 않았다.
- 별도 Gradle 프로젝트 [spikes/overlay](../../spikes/overlay/). Kotlin 2.1.21, AGP 8.7.2, Gradle 8.10.2, JDK 17, minSdk 28 / targetSdk 35. 외부 SDK·API 키·음성 코드·인터넷 권한이 없다.
- 창은 `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`, 300×150dp, `BOTTOM | END`, x/y 여백 24dp, 기본 `LayoutParams.alpha=0.8`.
- 여기서 **전체 화면 내비**는 고정 Driving view를 제외한 **앱 영역 전체**를 내비가 사용하는 상태다. Driving view는 앱이 숨길 수 없는 고정 영역이라는 [Pleos 설명](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)과 구별했다.

## 7개 항목 결과

| 항목 | 결과 | 실측·근거 |
|---|---|---|
| 1. 사용자 권한 설정 | **동작, 표준 Intent는 불가** | `ACTION_MANAGE_OVERLAY_PERMISSION`은 패키지 URI 유무 모두 `No activity found`; 앱에서도 `ActivityNotFoundException`. 그러나 **Pleos 설정 → 앱 → 앱 목록 → T-21 오버레이 시험 → 다른 앱 위에 표시** 스위치가 있다. appops를 `default`로 돌린 상태에서 스위치 OFF는 `canDrawOverlays=false` / user 10 권한 false, ON은 true 및 카드 표시 성공. 즉 adb 전용이 아니다. [앱 목록](overlay/permission-app-list.png) · [OFF](overlay/permission-ui-off.png) · [ON](overlay/permission-ui-on.png) · [로그](overlay/observations.txt). |
| 2. P/0, 전체 앱 영역 내비 | **동작** | 카드와 내비가 함께 보이고 WindowManager의 `isVisible=true`, `shown=true`. [캡처](overlay/p0-no-do-no-service.png) · [창/차량 상태](overlay/p0-no-do-no-service.json). |
| 3. D/30, DO 유무 | **동작, DO 판정은 부분 확인** | Console 목표/조회 30km/h, 계기판도 **30**, Car driving state=2, UXR `DO: true UxR: 80`. DO 없는 MainActivity에서 만든 카드와 `distractionOptimized=true`인 DoActivity에서 만든 카드 모두 내비 위에 유지된다. [DO 없음](overlay/d30-no-do-no-service.png) · [DO 있음](overlay/d30-do-no-service.png). 일반 Activity 자체는 두 경우 모두 시스템의 “주행 중에는 사용할 수 없습니다.” 화면으로 가려졌지만 카드는 남았다([일반](overlay/d30-non-do-activity.png) · [DO](overlay/d30-do-activity.png)). APK의 DO 메타데이터는 확인했으며 시스템에 `not installed from permitted sources NULL` 경고가 있었다. 이 설치 조건에서 DO가 인정된 Activity와의 비교를 주장하지 않는다. |
| 4. 카드 아래 내비 터치 | **동작** | P/0에서 카드 `[2236,1126][2536,1276]` 안의 **(2460,1220)**을 `adb shell input tap`으로 눌렀다. 가려진 내비 설정 버튼이 작동해 차량 설정의 내비게이션 화면이 열리고 카드도 유지됐다. [누르기 전](overlay/touch-before.png) · [후](overlay/touch-after.png), 대응 JSON에도 실제 창 alpha=0.8 기록. 물리 터치패널은 미검증이다. |
| 5. 홈·다른 앱·화면 종료 / 서비스 | **단기 동작, 장기 생존은 부분** | [HOME](overlay/d30-home-no-service.png), 위 설정 화면, 내비 전환에서 유지. `CLEAR_TOP`으로 시험 Intent를 전달한 뒤 Main/Do Activity를 모두 종료하고 **15초 이상 기다린 후에도** 표시됐다. [Activity/서비스 모두 없음](overlay/d30-finished-no-service.json) · [캡처](overlay/d30-finished-no-service.png). 서비스는 단기 표시의 필수조건이 아니었다. `specialUse` FGS도 Activity 종료 후 표시됐으며 `isForeground=true`, `types=40000000`, `sound=null`, `vibrate=null`을 확인했다([상태](overlay/d30-foreground-service.json) · [캡처](overlay/d30-foreground-service.png)). 메모리 부족·장시간·재부팅 복원은 미검증. |
| 6. 내비 옆 멀티 윈도우 | **부분** | 오른쪽 끝 창 바를 왼쪽으로 드래그해 내비를 왼쪽 좁은 창, 차량 설정을 오른쪽 창으로 만들었다. 카드는 **디스플레이 오른쪽 아래 좌표에 그대로 남아 오른쪽 앱을 가린다**. 내비에 자동 부착되지 않는다. [캡처](overlay/multiwindow-nav-left.png). 창 바를 오른쪽으로 돌려 내비 앱 영역 전체 상태로 복원했다. |
| 7. dp 좌표와 여백 | **동작** | density=1이므로 300×150dp=300×150px. 기본 시스템 바 inset을 적용받는 부모는 `[0,90][2560,1300]`, 카드 좌표는 **[2236,1126][2536,1276]**. 오른쪽 24px, 사용 가능 영역 아래 24px, 물리 화면 아래까지는 **164px(140px 하단 바+24px)**다. [WindowManager 원문](overlay/p0-no-do-no-service.json). 아래 코드처럼 px 고정값 대신 density와 `BOTTOM | END`를 사용한다. |

```kotlin
fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
params.width = dp(300)
params.height = dp(150)
params.gravity = Gravity.BOTTOM or Gravity.END
params.x = dp(24)
params.y = dp(24)
```

내비가 앱 영역 전체를 쓸 때 앱 창은 대략 `[853,90][2535,1300]`이다. 오버레이 부모는 디스플레이 기준이므로 **2535를 내비 우측 경계로 간주한 상대 좌표 계산이 아니다**. 창 축소·이동에 따라 내비 내부 위치를 유지하려면 별도 설계가 필요하다.

## Android 14 제약과 구현 시 참고

[`TYPE_APPLICATION_OVERLAY` 공식 문서](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)는 시스템이 창 위치·크기·가시성을 바꿀 수 있고 프로세스 중요도를 높여 종료 가능성을 줄인다고 설명한다. 이번 단기 관측은 항상 표시되거나 프로세스가 살아남는다는 보장이 아니다.

Android 12 이후 `FLAG_NOT_TOUCHABLE`만으로 모든 조건에서 터치 통과가 보장되지는 않는다. 단일 비신뢰 오버레이는 최대 가림 불투명도 이하 조건을 확인해야 한다([공식 설명, 기본 한도 0.8](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE)). 이번 시험은 서버 창 정보와 InputDispatcher에서 **실효 alpha=0.8**인 상태다. 추가로 클라이언트에 alpha=1을 요청했을 때도 서버는 0.8로 표시했으므로 불투명도 1인 창의 터치 통과로 해석하지 않았다.

Foreground service를 선택한다면 Android 14 이상 target에서 서비스 유형과 대응 권한을 선언해야 한다. 시험 앱은 `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `foregroundServiceType="specialUse"` 및 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 설명을 선언하고, 보이는 Activity에서 사용자가 시작하는 방식으로 확인했다. 다른 유형에 속하지 않는 유효한 용도의 `specialUse` 사용 및 심사에 관한 [Android 공식 설명](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)을 따른 것이며 제품의 서비스 유형 승인 여부까지 검증한 것은 아니다.

DO 메타데이터만으로 항상 주행 중 허용되는 것은 아니라는 [Car API 설명](https://developer.android.com/reference/android/car/Car#META_DATA_DISTRACTION_OPTIMIZED)도 있다. 이번 DO 화면 제한은 APK 메타데이터 누락이 아니며, 허용 설치 출처 경고가 원인 후보라는 점까지 확인했다. 시스템 허용 목록이나 설치 출처를 변경하지 않았다.

## 빌드·재현

저장소 루트 PowerShell에서 기존 JDK 17과 Android SDK 환경을 사용한다.

```powershell
.\spikes\overlay\gradlew.bat -p spikes/overlay :app:assembleDebug --console=plain
adb -s emulator-5556 install -r --user 10 spikes/overlay/app/build/outputs/apk/debug/app-debug.apk
```

P에서 Pleos 설정 → 앱 → 앱 목록 → T-21 오버레이 시험 → 다른 앱 위에 표시를 켠다. 앱 정보 화면은 아래 Intent로도 열렸지만 이미 실행 중인 설정 앱은 기존 화면만 앞으로 가져오는 경우가 있어 수동 목록 진입이 확실하다.

```powershell
adb -s emulator-5556 shell am start --user 10 -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.roadahead.spike.overlay
# Optional diagnostic alternative; not required when the Pleos switch is enabled.
adb -s emulator-5556 shell appops set --user 10 com.roadahead.spike.overlay SYSTEM_ALERT_WINDOW allow
```

자동 측정은 기존 Console 8088 포워딩을 사용한다. 실행 동안 자기 Console browser heartbeat만 유지하고, P/0 → D/30에서 위 8개 상태의 PNG·창 정보·차량 상태를 저장한다. `finally`에서 P/0 복원, 시험 앱 force-stop, 자기 heartbeat 해제를 수행한다. **자동 승인 검토가 명령을 막으면 우회하지 말고 조정자에게 맡긴다.**

```powershell
py -3.12 -B spikes/overlay/measure.py
```

기본 MainActivity에는 DO가 없고 DoActivity만 DO 메타데이터가 있다. 직접 시험할 때 Intent가 가려진 기존 Activity에 전달되도록 `CLEAR_TOP | NEW_TASK`를 넣는다.

```powershell
adb -s emulator-5556 shell am start --user 10 -f 0x14000000 -n com.roadahead.spike.overlay/.MainActivity --es op show
adb -s emulator-5556 shell am start --user 10 -f 0x14000000 -n com.roadahead.spike.overlay/.DoActivity --es op service
adb -s emulator-5556 shell am start --user 10 -f 0x14000000 -n com.roadahead.spike.overlay/.MainActivity --es op hide
```

`permission`은 표준 오버레이 설정 Intent 시험, `show`는 서비스 없는 표시, `service`는 FGS 표시, `hide`는 창·서비스 종료, `finish`는 Activity 종료다. `status` 등 그 밖의 값은 `canDrawOverlays` 로그만 남긴다.

## 검증·복원·남은 확인

- `:app:assembleDebug`는 **BUILD SUCCESSFUL**. 보강 후 자동 측정 전체가 exit 0이며 8개 상태에서 PNG 2560×1440, 창 visible/shown, Console 기어·속도를 assert했다. 화면 종료 상태에서는 시험 Activity가 없음을 추가 assert했다.
- 표준 권한 Intent 부재, UI OFF/ON과 `canDrawOverlays`, DO APK 메타데이터, 서비스 유형·무음 알림은 로그·APK·dumpsys로 확인했다. 새 제품 코드와 단위 테스트는 추가하지 않았다.
- GPS·차량의 다른 속성·시스템 오디오 설정은 조작하지 않았다. 시험 코드에 음성 기능이 없으며 소리 나는 TTS를 재생하지 않았다. 외부 공공 API 호출·push도 하지 않았다.
- 자동 측정은 P/0 복원 후 종료했다. 이후 최종 사용자 권한 OFF·종료·캡처를 묶은 adb 명령이 자동 승인 검토에서 **`blocked by policy`**로 거부됐다(구체 사유 미제공). 같은 작업을 우회하지 않고 Orca ask로 Claude에게 맡겼으며, Claude가 앱 목록 캡처·스위치 OFF·force-stop을 수행했다. Codex의 후속 읽기 전용 검증도 **Park/0, user 10 권한 false, appops default, 창·서비스 없음, 2560×1440/160dpi 유지**를 확인했다([restored.json](overlay/restored.json)). 시험 앱 설치는 남겨 두었다.
- 남은 확인: 실차의 권한 UI·주행 중 정책·실제 터치, 장시간 및 메모리 회수 후 생존, 제품용 FGS 적합성, 멀티 윈도우에서 내비 위치 추적과 다른 앱을 가리지 않는 표시 정책. 제품 적용 판단은 Claude에게 넘긴다.
