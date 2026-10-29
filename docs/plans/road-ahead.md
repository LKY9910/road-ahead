# 앞길 안내 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 이 저장소에서는 Codex가 Orca 작업으로 받아 실행한다(AGENTS.md 작업 흐름).

**Goal:** Pleos 내비 옆에서 앞 교차로 신호 남은 시간과 앞길 도로 위험(살얼음·안개·공사·통제·사고·재난)을 음성과 작은 화면으로 알려 주는 Android 앱과, 기록 데이터를 재생하는 시연 모드를 만든다.

**Architecture:** 판단은 Android에 의존하지 않는 `:core`(순수 JVM)의 `RoadAheadEngine`이 한다. 엔진은 입력(내비 상태, TBT, 위치, 속도, 와이퍼·성에 제거, 내비 음성 중 여부)과 API 응답(원문 JSON/ZIP)을 받아, 매 틱마다 화면 상태와 지금 말할 안내 하나를 돌려준다. `:app`은 실제 SDK·API를 읽는 `LiveFeed`와 기록을 재생하는 `ReplayFeed`를 같은 엔진에 꽂고, 250ms마다 틱을 돌려 Compose 화면과 Pleos TTS로 내보낸다.

**Tech Stack:** Kotlin 2.1.21, JDK 17, Gradle 8.10.2, AGP 8.7.2, kotlinx-coroutines 1.10.2, kotlinx-serialization-json 1.8.1(트리 API만, 플러그인 없음), JUnit 5.11.4, Compose BOM 2025.05.01, Pleos Vehicle 2.3.2 / NaviHelper 3.0.5 / TextToSpeech 3.3.1, AAOS `android.car`.

**Spec:** [docs/DESIGN.md](../DESIGN.md). 근거: [docs/APIS.md](../APIS.md), [docs/spikes/road-ahead.md](../spikes/road-ahead.md). 실행자는 이 계획과 DESIGN을 함께 읽는다.

## Global Constraints

- 모듈과 패키지: `:core` → `com.roadahead.core`, `:app` → namespace·applicationId `com.roadahead`. 앱 이름(가칭) `앞길 안내`.
- Android minSdk 28, compileSdk·targetSdk 35, buildToolsVersion 35.0.0, Java 17, `useLibrary("android.car")`.
- Pleos 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/`는 `exclusiveContent`로 `ai.pleos.playground`, `ai.pleos.playground.security` 그룹만 받는다.
- 버전 조합은 `spikes/road-ahead/`에서 검증된 것을 그대로 쓴다. 래퍼와 `gradle.properties`도 그 폴더에서 복사한다.
- API 키: 환경 변수 `ROAD_AHEAD_SIGNAL_KEY`, `ROAD_AHEAD_DISASTER_KEY`, `ROAD_AHEAD_WEATHER_KEY`가 있으면 쓰고, 없으면 저장소 루트 `api` 파일(gitignore)의 `신호등`, `재난`, `기상` 줄에서 읽어 BuildConfig에 넣는다. 키를 코드·로그·문서·커밋·테스트 자료에 쓰지 않는다. URL을 로그로 남길 때는 키 파라미터 값을 `***`로 바꾼다. 커밋 전 `git diff --cached`에 키 문자열이 없는지 확인한다.
- 안내 문장은 이 계획에 적힌 글자 그대로 쓴다. "통과 가능", "지금 출발", "서두르세요" 같은 표현은 어디에도 넣지 않는다.
- 데이터 나이 상한은 신호 3초, 기상 생산 시각 10분이다. API 오류·빈 응답을 "위험 없음"으로 바꾸지 않는다.
- 시간은 `Instant`(벽시계)와 `monoMs: Long`(단조 시계, 앱에서는 `SystemClock.elapsedRealtime()`)을 인자로 받는다. `:core`에서 시스템 시계를 직접 읽지 않는다. 하루 경계는 `Asia/Seoul` 기준이다.
- 테스트 이름에는 `.` `:` `/` `[` `]` `<` `>`를 쓰지 않는다. JVM 함수 이름 규칙 때문이다(프로젝트1에서 겪음).
- 테스트 자료는 `core/src/test/resources/fixtures/`에 두고, `docs/spikes/road-ahead/samples/`의 키를 지운 표본에서 가져온다.
- 문서·커밋 메시지는 한국어, 코드 식별자·주석은 영어. 커밋 메시지는 `[T-번호] 요약`, 끝에 `Co-Authored-By: Codex <noreply@openai.com>`. `git add <파일>`만 쓴다.
- 소리가 나는 시험(TTS 재생)은 사용자가 허락했을 때만 한다. 필요하면 Orca ask로 묻는다.

## Review Focus

1. **정차 중 GPS 방향 흔들림**: 차가 멈추면 bearing이 튀거나 사라진다. 정차 중에는 마지막으로 움직일 때의 방향을 계속 써야 신호 방향이 바뀌지 않는다. → Task 8 `stopped car keeps last moving bearing`
2. **HTML 오류 페이지·깨진 응답**: 기상 API는 502·503 HTML을, 포털은 GW 오류를 돌려줬다. 파서는 `ApiException`을 던지고, 엔진은 직전 데이터를 유지한다. 나이 상한이 지나면 안내가 사라진다. → Task 2 `html body throws ApiException`, Task 5 `malformed its body throws ApiException`
3. **빈 남은 시간·없는 방향**: `RmndCs`가 빈 문자열이거나 그 방향 필드가 비어 있으면 그 신호는 없는 것으로 본다. 0으로 보지 않는다. → Task 2 `empty remaining becomes null`
4. **경로 안내 중 TBT가 비어 있음**: 경로 안내 중이라도 TBT 목록이 비면 그 교차로는 직진으로 본다. → Task 6 `route mode without turn uses straight`
5. **자정에 호출 한도 초기화**: 하루 4,500회 신호 조회 한도는 한국 시간 자정에 다시 열린다. → Task 8 `signal budget resets at seoul midnight`

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat), gradle/wrapper/  (Task 1)
core/build.gradle.kts                                              (Task 1)
core/src/main/kotlin/com/roadahead/core/
  model/Models.kt          모든 공용 타입                           (Task 1)
  geo/Geo.kt               거리·방위·앞쪽 판정·신호 방향            (Task 1)
  text/Korean.kt           koDistance                               (Task 1)
  signal/SignalParser.kt   신호·교차로 응답 해석, 색 매핑           (Task 2)
  signal/SignalTracker.kt  단위 자가 보정, 남은 시간, 나이 상한     (Task 3)
  drive/StopDetector.kt    정차 판정                                (Task 4)
  hazard/WeatherParser.kt  기상 ZIP·CSV, 요청 시각                  (Task 5)
  hazard/ItsParser.kt      ITS 돌발·재난                            (Task 5)
  advice/SignalAdvisor.kt  A·B 장면 규칙과 문장                     (Task 6)
  advice/HazardAdvisor.kt  C 장면 규칙과 문장                       (Task 6)
  speak/Announcer.kt       한 번만, 우선순위, 내비 음성 대기        (Task 7)
  RoadAheadEngine.kt       조립, 조회 판단·한도, 틱                 (Task 8)
app/                                                               (Task 9~11)
  build.gradle.kts, src/main/AndroidManifest.xml
  src/main/kotlin/com/roadahead/
    MainActivity.kt, ui/RoadAheadScreen.kt, ui/Settings.kt          (Task 9)
    feed/Feed.kt, feed/LiveFeed.kt, feed/Apis.kt                    (Task 10)
    car/PleosNavi.kt, car/GpsFix.kt, car/CarSignals.kt, car/NaviVoice.kt, car/Speech.kt  (Task 10)
    feed/ReplayFeed.kt                                              (Task 11)
  src/main/assets/demo/scene-a.jsonl, scene-b.jsonl, scene-c.jsonl  (Task 11)
tools/make_demo.py                                                  (Task 11)
```

---

### Task 1: 루트 Gradle, `:core` 골격, 공용 타입, 지리 계산

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- Create: `core/build.gradle.kts`, `core/src/main/kotlin/com/roadahead/core/model/Models.kt`, `geo/Geo.kt`, `text/Korean.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/geo/GeoTest.kt`, `text/KoreanTest.kt`

**Interfaces:**
- Produces (`model/Models.kt`, 이름·필드 그대로):

```kotlin
data class GeoPoint(val lat: Double, val lon: Double)
enum class Movement(val label: String, val code: String) { STRAIGHT("직진", "Stsg"), LEFT("좌회전", "Ltsg"), UTURN("유턴", "Utsg") }
enum class Compass8(val prefix: String) { N("nt"), NE("ne"), E("et"), SE("se"), S("st"), SW("sw"), W("wt"), NW("nw") }
enum class LightColor { RED, GREEN, YELLOW, UNKNOWN }
enum class NavMode { FREE, ROUTE, UNKNOWN }
enum class DirectionConvention { APPROACH, HEADING }
data class Turn(val movement: Movement?, val distanceM: Double)          // movement null = 안내하지 않는 회전
data class Fix(val point: GeoPoint, val bearingDeg: Double?, val monoMs: Long)
data class Intersection(val region: String, val id: String, val name: String, val point: GeoPoint) { val key: String get() = "$region:$id" }
data class DirectionSignal(val approach: Compass8, val movement: Movement, val color: LightColor, val raw: Long?)
data class SignalSnapshot(val region: String, val id: String, val produced: java.time.LocalDateTime, val receivedMonoMs: Long, val signals: List<DirectionSignal>) { val key: String get() = "$region:$id" }
enum class HazardKind { ICE, FOG, CONSTRUCTION, ACCIDENT, DISASTER }
enum class Severity(val label: String) { INTEREST("관심"), CAUTION("주의"), DANGER("위험") }
data class Hazard(val key: String, val kind: HazardKind, val severity: Severity, val from: GeoPoint, val to: GeoPoint, val detail: String, val fullClosure: Boolean, val produced: java.time.Instant)
enum class HazardSource { WEATHER, ITS_EVENT, ITS_DISASTER }
enum class Priority { HIGH, SIGNAL, NORMAL }                              // 선언 순서 = 말하는 순서
data class Advice(val key: String, val priority: Priority, val text: String)
class ApiException(val code: String) : Exception(code)
```

- Produces (`geo/Geo.kt`): `distanceMeters(a: GeoPoint, b: GeoPoint): Double`, `bearingDegrees(from: GeoPoint, to: GeoPoint): Double`, `angleDiff(a: Double, b: Double): Double`(세 함수는 `프로젝트1/core/src/main/kotlin/com/ontheway/core/geo/Geo.kt`에서 그대로 복사), `compassOf(bearingDeg: Double): Compass8`, `signalDirection(vehicleBearingDeg: Double, convention: DirectionConvention): Compass8`, `aheadDistance(fix: Fix, target: GeoPoint, coneDeg: Double): Double?`, `segmentAheadDistance(fix: Fix, from: GeoPoint, to: GeoPoint, coneDeg: Double, alignDeg: Double): Double?`
- Produces (`text/Korean.kt`): `koDistance(meters: Double): String`. `프로젝트1/core/src/main/kotlin/com/ontheway/core/language/ReplyTemplates.kt`의 같은 이름 함수를 그대로 복사한다.
- Produces (`RoadAheadEngine.kt`, 미리 만듦): Task 8 Interfaces의 데이터 클래스 `Inputs`, `SignalCard`, `Screen`, `TickResult`만 이 Task에서 먼저 만든다. T-16이 `:app` 화면을 병행해서 만들 수 있게 하기 위해서다. 엔진 클래스는 Task 8에서 같은 파일에 추가한다.

- [ ] **Step 1: 루트 Gradle과 `:core` 빌드 파일 만들기**

`spikes/road-ahead/`의 래퍼·`gradle.properties`를 복사한다. `settings.gradle.kts`는 그 폴더의 저장소 설정(exclusiveContent 포함)을 쓰고, `rootProject.name = "RoadAhead"`, `include(":core")`만 둔다(`:app`은 Task 9가 추가한다). `libs.versions.toml`은 spike 카탈로그에 다음을 더해 이 계획 전체에서 쓸 라이브러리를 한 번에 정의한다.
- `kotlinx-serialization-json = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1"`
- `pleos-tts = "ai.pleos.playground:TextToSpeech:3.3.1"`
- `junit-jupiter-api`(5.11.4)
- `compose-ui`, `compose-ui-tooling-preview`(BOM 관리)

`core/build.gradle.kts`는 `kotlin-jvm` 플러그인, `jvmToolchain(17)`, coroutines·serialization-json 의존성, JUnit 5(`useJUnitPlatform()`)를 쓴다.

- [ ] **Step 2: 실패하는 테스트 쓰기**

```kotlin
class GeoTest {
    private val origin = GeoPoint(35.5, 129.3)
    private fun north(m: Double) = GeoPoint(origin.lat + m / 111_195.0, origin.lon)

    @Test fun `compass sectors`() {
        assertEquals(Compass8.N, compassOf(0.0)); assertEquals(Compass8.N, compassOf(337.6))
        assertEquals(Compass8.NE, compassOf(44.0)); assertEquals(Compass8.S, compassOf(180.0))
        assertEquals(Compass8.W, compassOf(270.0))
    }
    @Test fun `approach convention names the side the car comes from`() {
        assertEquals(Compass8.N, signalDirection(180.0, DirectionConvention.APPROACH))
        assertEquals(Compass8.W, signalDirection(90.0, DirectionConvention.APPROACH))
        assertEquals(Compass8.S, signalDirection(180.0, DirectionConvention.HEADING))
    }
    @Test fun `target in front is ahead`() {
        assertEquals(100.0, aheadDistance(Fix(origin, 0.0, 0), north(100.0), 30.0)!!, 1.0)
    }
    @Test fun `target to the side or without bearing is not ahead`() {
        assertNull(aheadDistance(Fix(origin, 90.0, 0), north(100.0), 30.0))
        assertNull(aheadDistance(Fix(origin, null, 0), north(100.0), 30.0))
    }
    @Test fun `segment must run the same way as the car`() {
        val from = north(100.0); val to = north(300.0)
        assertEquals(100.0, segmentAheadDistance(Fix(origin, 0.0, 0), from, to, 30.0, 45.0)!!, 1.0)
        assertNull(segmentAheadDistance(Fix(origin, 0.0, 0), to, from, 30.0, 45.0))
    }
}
class KoreanTest {
    @Test fun `distance wording`() {
        assertEquals("800m", koDistance(780.0)); assertEquals("1km", koDistance(960.0))
        assertEquals("2.5km", koDistance(2_480.0)); assertEquals("3km", koDistance(3_000.0))
    }
}
```

- [ ] **Step 3: 실패 확인** — Run: `.\gradlew.bat :core:test` / Expected: 컴파일 실패(정의 없음)

- [ ] **Step 4: 구현**

`Models.kt`는 위 Interfaces 그대로 쓴다. `Geo.kt`는 다음과 같이 만든다.
- `compassOf`: 0°를 N으로 하고 45° 간격 부채꼴(각 ±22.5°)로 나눈다.
- `signalDirection`: APPROACH면 `compassOf(bearing + 180)`, HEADING이면 `compassOf(bearing)`.
- `aheadDistance`: bearing이 null이거나 `angleDiff(bearing, bearingDegrees(fix.point, target)) > coneDeg`이면 null, 아니면 거리를 돌려준다.
- `segmentAheadDistance`: `angleDiff(bearing, bearingDegrees(from, to)) > alignDeg`이면 null이다. 아니면 from·중점·to 중 `aheadDistance`가 null이 아닌 것의 최솟값을 돌려준다.

- [ ] **Step 5: 통과 확인** — Run: `.\gradlew.bat :core:test` / Expected: BUILD SUCCESSFUL, GeoTest 5개·KoreanTest 1개 통과

- [ ] **Step 6: 커밋** — `git add settings.gradle.kts build.gradle.kts gradle.properties gradle gradlew gradlew.bat core` → `[T-15] 루트 Gradle과 core 골격, 공용 타입, 지리 계산`

---

### Task 2: 신호·교차로 응답 해석

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/signal/SignalParser.kt`
- Create fixtures:
  - `signal-ulsan.json`: `docs/spikes/road-ahead/samples/*-signal-series-00.json`의 `body` 문자열을 그대로 옮긴다.
  - `signal-map-ulsan.json`: `*-signal-map-ulsan*.json`의 `body`
  - `signal-k3.json`: `{"header":{"resultCode":"K3","resultMsg":"NODATA_ERROR"}}`
- Test: `core/src/test/kotlin/com/roadahead/core/signal/SignalParserTest.kt`

**Interfaces:**
- Consumes: Task 1 `SignalSnapshot`, `DirectionSignal`, `Intersection`, `ApiException`, `Compass8.prefix`, `Movement.code`
- Produces:
  - `parseSignalPage(json: String, receivedMonoMs: Long): List<SignalSnapshot>`
  - `parseIntersectionPage(json: String): List<Intersection>`
  - `parseTotalCount(json: String): Int`
  - `colorOf(stateName: String): LightColor`

- [ ] **Step 1: 실패하는 테스트 쓰기**

```kotlin
class SignalParserTest {
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `ulsan page parses intersections and directions`() {
        val page = parseSignalPage(fixture("signal-ulsan.json"), receivedMonoMs = 500)
        assertEquals(3, page.size)
        val first = page.first()
        assertEquals("3100000000:1", first.key)
        assertEquals(LocalDateTime.of(2026, 10, 28, 16, 6, 47), first.produced)
        assertEquals(500, first.receivedMonoMs)
        assertTrue(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.RED, 10_000) in first.signals)
        assertTrue(DirectionSignal(Compass8.E, Movement.LEFT, LightColor.RED, 111_000) in first.signals)
    }
    @Test fun `empty remaining becomes null`() {
        val json = """{"header":{"resultCode":"K0"},"body":{"items":{"item":[{"stdgCd":"3100000000","crsrdId":"9","totDt":"20261028160647","ntStsgSttsNm":"stop-And-Remain","ntStsgRmndCs":""}]}}}"""
        assertEquals(listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.RED, null)), parseSignalPage(json, 0).single().signals)
    }
    @Test fun `no data code gives empty list`() { assertEquals(emptyList<SignalSnapshot>(), parseSignalPage(fixture("signal-k3.json"), 0)) }
    @Test fun `other result code throws`() {
        assertEquals("K99", assertThrows<ApiException> { parseSignalPage("""{"header":{"resultCode":"K99"}}""", 0) }.code)
    }
    @Test fun `html body throws ApiException`() { assertThrows<ApiException> { parseSignalPage("<html>502 Bad Gateway</html>", 0) } }
    @Test fun `map page gives names and points`() {
        val ojibeol = parseIntersectionPage(fixture("signal-map-ulsan.json")).first { it.id == "1" }
        assertEquals("오지벌사거리", ojibeol.name)
        assertEquals(35.512239, ojibeol.point.lat, 1e-6); assertEquals(129.430752, ojibeol.point.lon, 1e-6)
    }
    @Test fun `state names map to colors`() {
        assertEquals(LightColor.RED, colorOf("stop-And-Remain")); assertEquals(LightColor.RED, colorOf("stop-Then-Proceed"))
        assertEquals(LightColor.GREEN, colorOf("protected-Movement-Allowed")); assertEquals(LightColor.GREEN, colorOf("pre-Movement"))
        assertEquals(LightColor.YELLOW, colorOf("permissive-clearance")); assertEquals(LightColor.UNKNOWN, colorOf("dark"))
    }
}
```

- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat :core:test --tests "*SignalParserTest"` / Expected: 컴파일 실패

- [ ] **Step 3: 구현**

`Json.parseToJsonElement`(트리 API)를 쓴다. JSON이 아니면 `ApiException("parse")`를 던진다.

`parseSignalPage`:
- `header.resultCode`가 `K0`이면 `body.items.item` 배열을 읽고, `K3`이면 빈 목록이다. 그 밖의 코드는 그 코드로 `ApiException`을 던진다.
- 항목마다 `Compass8` × `Movement` 조합으로 `"${prefix}${code}SttsNm"`과 `"${prefix}${code}RmndCs"`를 읽는다. 상태 이름이 비어 있으면 그 조합은 건너뛴다. 남은 시간 문자열이 비어 있으면 `raw = null`이다.
- `produced`는 `totDt`(`yyyyMMddHHmmss`)를 읽는다.

`colorOf`는 DESIGN 5절 "신호 상태 이름과 색"의 표 그대로 대응한다.

`parseIntersectionPage`는 `crsrdNm`, `mapCtptIntLat`, `mapCtptIntLot`를 읽는다. `parseTotalCount`는 `body.totalCount`를 읽고, 없으면 0이다.

- [ ] **Step 4: 통과 확인** — Run: 같은 명령 / Expected: 7개 통과

- [ ] **Step 5: 커밋** — `[T-15] 신호·교차로 응답 해석`

---

### Task 3: 신호 추적 — 단위 자가 보정, 남은 시간, 나이 상한

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/signal/SignalTracker.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/signal/SignalTrackerTest.kt`

**Interfaces:**
- Consumes: Task 1 `SignalSnapshot`, `Compass8`, `Movement`, `LightColor`
- Produces:
  - `data class SignalReading(val color: LightColor, val remainingSec: Double?)`
  - `class SignalTracker(staleAfterMs: Long = 3_000)`, 메서드는 다음과 같다.
    - `fun accept(s: SignalSnapshot)`
    - `fun reading(key: String, approach: Compass8, movement: Movement, nowMonoMs: Long): SignalReading?`
    - `fun liveKeys(): Set<String>`

- [ ] **Step 1: 실패하는 테스트 쓰기**

```kotlin
class SignalTrackerTest {
    private val t0 = LocalDateTime.of(2026, 10, 28, 16, 6, 47)
    private fun snap(sec: Long, raw: Long?, color: LightColor = LightColor.RED, mono: Long = sec * 1000) =
        SignalSnapshot("3100000000", "1", t0.plusSeconds(sec), mono, listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, color, raw)))
    private fun SignalTracker.read(now: Long) = reading("3100000000:1", Compass8.N, Movement.STRAIGHT, now)

    @Test fun `three samples at 1000 per second calibrate`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000), snap(2, 8_000)).forEach(t::accept)
        val r = t.read(2_500)!!
        assertEquals(LightColor.RED, r.color); assertEquals(7.5, r.remainingSec!!, 0.01)
    }
    @Test fun `100 per second scale also calibrates`() {
        val t = SignalTracker(); listOf(snap(0, 1_000), snap(1, 900), snap(2, 800)).forEach(t::accept)
        assertEquals(8.0, t.read(2_000)!!.remainingSec!!, 0.01)
    }
    @Test fun `two samples give color without seconds`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000)).forEach(t::accept)
        assertEquals(SignalReading(LightColor.RED, null), t.read(1_000))
    }
    @Test fun `frozen value never calibrates`() {
        val t = SignalTracker(); listOf(snap(0, 255_000), snap(1, 255_000), snap(2, 255_000)).forEach(t::accept)
        assertNull(t.read(2_000)!!.remainingSec)
    }
    @Test fun `stale after three seconds without newer production time`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000), snap(2, 8_000)).forEach(t::accept)
        t.accept(snap(2, 8_000, mono = 4_000))          // same totDt again: ignored, does not refresh
        assertNotNull(t.read(5_000)); assertNull(t.read(5_001))
    }
    @Test fun `scale survives a phase change`() {
        val t = SignalTracker(); listOf(snap(0, 3_000), snap(1, 2_000), snap(2, 1_000)).forEach(t::accept)
        t.accept(snap(3, 30_000, LightColor.GREEN))
        assertEquals(SignalReading(LightColor.GREEN, 30.0), t.read(3_000))
    }
    @Test fun `live keys list intersections seen`() {
        val t = SignalTracker(); t.accept(snap(0, 1_000)); assertEquals(setOf("3100000000:1"), t.liveKeys())
    }
}
```

- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat :core:test --tests "*SignalTrackerTest"` / Expected: 컴파일 실패

- [ ] **Step 3: 구현**

교차로 키마다 마지막 `produced`와 "생산 시각이 늘어난 마지막 수신 시각"(`lastAdvanceMono`)을 둔다. `produced`가 늘지 않은 스냅숏은 통째로 무시한다.

(키, 방향, 동작) 계열마다 다음을 둔다.
- **현재 위상 표본 목록**: `produced`, `raw`, 수신 시각. 색이 바뀌면 비운다.
- **확인된 배율**: 1000 또는 100. 위상이 바뀌어도 유지한다.

표본이 3개 이상이면 연속 쌍마다 `rate = (raw[i] - raw[i+1]) / 생산 시각 차(초)`를 구해 배율을 다시 정한다.
- 모두 850..1150이면 1000
- 모두 85..115이면 100
- 그 밖이면 배율을 지운다(null).

`reading`:
- `nowMonoMs - lastAdvanceMono > staleAfterMs`이면 null이다.
- 배율이 있고 최근 raw가 있으면 `remainingSec = raw/배율 - (now - 수신 시각)/1000`이다. 0 아래로는 내려가지 않는다.

- [ ] **Step 4: 통과 확인** — Expected: 7개 통과

- [ ] **Step 5: 커밋** — `[T-15] 신호 단위 자가 보정과 남은 시간`

---

### Task 4: 정차 판정

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/drive/StopDetector.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/drive/StopDetectorTest.kt`

**Interfaces:**
- Produces:
  - `enum class Motion { MOVING, STOPPED, UNKNOWN }`
  - `class StopDetector(stopBelowMps: Double = 0.1, stopForMs: Long = 2_000, moveAboveMps: Double = 0.5, staleMs: Long = 1_000)`, 메서드는 다음과 같다.
    - `fun onSpeed(mps: Double, monoMs: Long)`
    - `fun motion(nowMonoMs: Long): Motion`

- [ ] **Step 1: 실패하는 테스트 쓰기**

```kotlin
class StopDetectorTest {
    @Test fun `two seconds below threshold means stopped`() {
        val d = StopDetector(); d.onSpeed(3.0, 0)
        (1..10).forEach { d.onSpeed(0.0, it * 200L) }          // 0 m/s from 200 to 2000
        assertEquals(Motion.MOVING, d.motion(2_100))           // 1.9 s below
        d.onSpeed(0.0, 2_200); assertEquals(Motion.STOPPED, d.motion(2_200))
    }
    @Test fun `half a meter per second moves at once`() {
        val d = StopDetector(); (0..10).forEach { d.onSpeed(0.0, it * 200L) }
        d.onSpeed(0.6, 2_300); assertEquals(Motion.MOVING, d.motion(2_300))
    }
    @Test fun `between thresholds keeps previous state`() {
        val d = StopDetector(); d.onSpeed(3.0, 0); d.onSpeed(0.3, 200); assertEquals(Motion.MOVING, d.motion(200))
    }
    @Test fun `no sample for over a second is unknown`() {
        val d = StopDetector(); d.onSpeed(3.0, 0); assertEquals(Motion.UNKNOWN, d.motion(1_001))
    }
    @Test fun `first slow sample alone is unknown`() {
        val d = StopDetector(); d.onSpeed(0.3, 0); assertEquals(Motion.UNKNOWN, d.motion(0))
    }
}
```

- [ ] **Step 2~4:** 실패 확인 → 구현 → 통과 확인(5개).

구현 방식:
- 0.1 m/s 이하가 처음 시작된 시각을 기억한다.
- 그 상태가 2초 이어지면 STOPPED, 0.5 m/s 이상이면 바로 MOVING이다.
- 그 사이 값이면 이전 상태를 유지한다. 처음 상태는 UNKNOWN이다.
- 마지막 표본에서 1초가 넘게 지나면 UNKNOWN이다.

- [ ] **Step 5: 커밋** — `[T-15] 정차 판정`

---

### Task 5: 도로 위험 응답 해석 — 기상, ITS 돌발·재난

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/hazard/WeatherParser.kt`, `hazard/ItsParser.kt`
- Create fixtures:
  - `weather-045-link-fog.csv`: `*-weather-045-explicit.json`의 `files`에서 `_LINK_FOG_` 파일 본문
  - `its-event.json`: `*-its-event-seoul-authorized.json`의 본문
  - `its-disaster.json`: `*-disaster-current.json`의 본문
- Test: `core/src/test/kotlin/com/roadahead/core/hazard/WeatherParserTest.kt`, `ItsParserTest.kt`

**Interfaces:**
- Consumes: Task 1 `Hazard`, `HazardKind`, `Severity`, `GeoPoint`, `ApiException`
- Produces:
  - `parseWeatherZip(zip: ByteArray, produced: Instant): List<Hazard>`
  - `parseWeatherCsv(csv: String, kind: HazardKind, produced: Instant): List<Hazard>`
  - `weatherRequestTimes(now: Instant): List<String>`
  - `parseItsEvents(json: String, produced: Instant): List<Hazard>`
  - `parseItsDisasters(json: String, produced: Instant): List<Hazard>`

- [ ] **Step 1: 실패하는 테스트 쓰기**

```kotlin
class WeatherParserTest {
    private val at = Instant.parse("2026-10-28T07:05:00Z")
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `fog link rows become hazards`() {
        val fog = parseWeatherCsv(fixture("weather-045-link-fog.csv"), HazardKind.FOG, at)
        assertEquals(4, fog.count { it.severity == Severity.CAUTION })
        val link = fog.first { it.key == "fog:3580008302" }
        assertEquals(GeoPoint(36.362518, 128.238118), link.from); assertEquals(GeoPoint(36.312729, 128.220901), link.to)
        assertEquals(at, link.produced)
    }
    @Test fun `level zero is not a hazard`() {
        val csv = "LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,FOG\n1,a,b,128.0,36.0,128.1,36.1,0\n2,a,b,128.0,36.0,128.1,36.1,3\n"
        assertEquals(listOf("fog:2" to Severity.DANGER), parseWeatherCsv(csv, HazardKind.FOG, at).map { it.key to it.severity })
    }
    @Test fun `zip reads link files only`() {
        val bytes = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FOG/R045_LINK_FOG_202610280705.csv")); z.write("LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,FOG\n7,a,b,128.0,36.0,128.1,36.1,2\n".toByteArray())
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FOG/R045_1KM_FOG_202610280705.csv")); z.write("LINK_ID,S_NUM,F_LON,F_LAT,T_LON,T_LAT,FOG,FLAG,X_GRID,Y_GRID\n7,1,128.0,36.0,128.1,36.1,3,1,1,1\n".toByteArray())
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FRG/R045_LINK_FRG_202611150705.csv")); z.write("LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,B_ICE\n8,a,b,128.0,36.0,128.1,36.1,1\n".toByteArray())
        } }.toByteArray()
        assertEquals(setOf("fog:7", "ice:8"), parseWeatherZip(bytes, at).map { it.key }.toSet())
    }
    @Test fun `request times walk back from the latest five minute slot`() {
        assertEquals(listOf("202610280710", "202610280705", "202610280700"), weatherRequestTimes(Instant.parse("2026-10-28T07:13:20Z")))
    }
}
class ItsParserTest {
    private val at = Instant.parse("2026-10-28T07:30:00Z")
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `events keep construction and accidents`() {
        val events = parseItsEvents(fixture("its-event.json"), at)
        assertEquals(159, events.size)                                  // 공사 153 + 교통사고 6
        val first = events.first { it.key == "evt:2240032401:20261028091822:공사" }
        assertEquals(HazardKind.CONSTRUCTION, first.kind); assertEquals(Severity.CAUTION, first.severity)
        assertEquals(GeoPoint(37.45781, 126.785426), first.from); assertEquals(first.from, first.to)
        assertEquals("4차로 차단", first.detail); assertFalse(first.fullClosure)
    }
    @Test fun `full closure is danger`() {
        val json = """{"header":{"resultCode":0},"body":{"items":[{"eventType":"공사","startDate":"20261028090000","coordX":"127.0","coordY":"37.5","linkId":"9","lanesBlockType":"4","lanesBlocked":"","message":""}]}}"""
        val h = parseItsEvents(json, at).single(); assertTrue(h.fullClosure); assertEquals(Severity.DANGER, h.severity)
    }
    @Test fun `ended wildfire is skipped`() { assertEquals(emptyList<Hazard>(), parseItsDisasters(fixture("its-disaster.json"), at)) }
    @Test fun `active underpass flooding is a disaster`() {
        val json = """{"header":{"resultCode":0},"body":{"items":[{"eventId":"E1","eventType":"D03","status":"1","locationInfo":"127.01 37.51,127.02 37.52"}]}}"""
        val h = parseItsDisasters(json, at).single()
        assertEquals("dis:E1", h.key); assertEquals("지하차도 침수", h.detail); assertEquals(Severity.DANGER, h.severity)
        assertEquals(GeoPoint(37.51, 127.01), h.from); assertEquals(GeoPoint(37.52, 127.02), h.to)
    }
    @Test fun `malformed its body throws ApiException`() { assertThrows<ApiException> { parseItsEvents("<html></html>", at) } }
    @Test fun `failed result code throws`() {
        assertEquals("4009", assertThrows<ApiException> { parseItsEvents("""{"header":{"resultCode":4009,"resultMsg":"x"}}""", at) }.code)
    }
}
```

- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat :core:test --tests "*ParserTest"` / Expected: 새 테스트 컴파일 실패

- [ ] **Step 3: 구현**

**기상 CSV**
- 열은 헤더 이름으로 찾는다. 단계 열은 FOG면 `FOG`, ICE면 `B_ICE`다.
- 단계 1·2·3 → INTEREST·CAUTION·DANGER로 바꾼다. 0과 빈 값은 건너뛴다.
- 키는 `fog:{LINK_ID}` 또는 `ice:{LINK_ID}`, from은 `(F_LAT, F_LON)`, to는 `(T_LAT, T_LON)`이다.
- `detail = ""`, `fullClosure = false`

**기상 ZIP**: `java.util.zip.ZipInputStream`으로 엔트리 이름에 `_LINK_FOG_`가 있으면 FOG, `_LINK_FRG_`가 있으면 ICE로 읽는다. 나머지(1KM 등)는 무시한다.

**요청 시각**: UTC 기준으로 5분 단위로 내림한 시각과 그 전 두 시각을 `yyyyMMddHHmm`으로 만든다.

**ITS 공통**: `header.resultCode`가 숫자든 문자열이든 0이 아니면 그 값으로 `ApiException`을 던진다. JSON이 아니면 `ApiException("parse")`다. 항목은 `body.items`다.

**ITS 돌발**
- `eventType` "공사" → CONSTRUCTION, "교통사고" → ACCIDENT. 그 밖은 건너뛴다.
- 위치는 `(coordY, coordX)` 한 점이다(from = to).
- 키는 `evt:{linkId}:{startDate}:{eventType}`이다. `eventId`가 없어서다.
- `detail = lanesBlocked.trim()`
- `fullClosure = lanesBlockType == "4" || lanesBlocked.contains("전면")`
- 심각도는 fullClosure면 DANGER, 아니면 CAUTION이다.

**ITS 재난**
- `eventType`과 `detail` 대응: D03 "지하차도 침수", D04 "하천 범람", D06 "땅꺼짐", D07 "산불". 그 밖은 건너뛴다.
- 끝난 상태는 건너뛴다. `status`를 문자열로 비교한다.
  - D06에서 "3"
  - D07에서 "2"나 "3"
- `locationInfo` "경도 위도,경도 위도,..."의 첫 점을 from, 마지막 점을 to로 쓴다.
- 키 `dis:{eventId}`, 심각도 DANGER

- [ ] **Step 4: 통과 확인** — Expected: 10개 통과. 픽스처 기대값(4행, 159건)이 실제 파일과 다르면 픽스처를 다시 확인한다. 기대값을 고치려면 HANDOFF에 근거를 남긴다.

- [ ] **Step 5: 커밋** — `[T-15] 기상·ITS 응답 해석`

---

### Task 6: 안내 규칙 — 신호(A·B)와 도로 위험(C)

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/advice/SignalAdvisor.kt`, `advice/HazardAdvisor.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/advice/SignalAdvisorTest.kt`, `HazardAdvisorTest.kt`

**Interfaces:**
- Consumes: Task 1 타입과 `aheadDistance`, `segmentAheadDistance`, `signalDirection`, `koDistance`; Task 3 `SignalTracker`, `SignalReading`; Task 4 `Motion`
- Produces:

```kotlin
data class SignalConfig(val approachRangeM: Double = 300.0, val stopLineM: Double = 60.0, val coneDeg: Double = 30.0,
    val turnMatchM: Double = 40.0, val waitSoonSec: Double = 5.0, val waitStartMinSec: Double = 8.0,
    val defaultApproachMps: Double = 8.0, val convention: DirectionConvention = DirectionConvention.APPROACH)
data class SignalView(val intersection: Intersection, val movement: Movement, val reading: SignalReading, val distanceM: Double)
class SignalAdvisor(cfg: SignalConfig = SignalConfig()) {
    fun view(navMode: NavMode, turn: Turn?, fix: Fix?, intersections: List<Intersection>, tracker: SignalTracker, nowMonoMs: Long): SignalView?
    fun advices(view: SignalView?, navMode: NavMode, motion: Motion, speedMps: Double?): List<Advice>
}
data class HazardConfig(val announceM: Double = 3_000.0, val coneDeg: Double = 30.0, val alignDeg: Double = 45.0,
    val weatherMaxAge: java.time.Duration = java.time.Duration.ofMinutes(10))
class HazardAdvisor(cfg: HazardConfig = HazardConfig()) {
    fun advices(hazards: List<Hazard>, fix: Fix?, badWeather: Boolean, now: Instant): List<Advice>
}
```

규칙 (구현이 그대로 따를 것)

**`view`**
- bearing 있는 fix가 필요하다. 후보는 `tracker.liveKeys()`에 있는 교차로 중 `aheadDistance(fix, point, coneDeg) ≤ approachRangeM`인 것이고, 그중 가장 가까운 것을 고른다.
- 동작은 이렇게 정한다.
  - ROUTE이고 turn이 있으며 `|turn.distanceM - 거리| ≤ turnMatchM`이면 `turn.movement`를 쓴다. 이때 movement가 null이면 view도 null이다.
  - 그 밖의 경우(ROUTE에서 turn이 없거나 거리가 안 맞을 때, FREE, UNKNOWN)는 STRAIGHT다.
- 방향은 `signalDirection(bearing, convention)`이고, `reading`이 null이면 view도 null이다.

**`advices`**: view가 null이면 빈 목록이다. 키 앞부분은 `sig:{intersection.key}`이다. `eta = 거리 / max(speedMps ?: defaultApproachMps, 1.0)`.
- motion이 MOVING 또는 UNKNOWN일 때
  - RED이고 (`remainingSec == null` 또는 `remainingSec > eta`)이면
    - ROUTE: 키 `…:red`, "앞 신호 빨간불이에요. 속도를 줄이세요."
    - FREE·UNKNOWN: 키 `…:red`, "앞 신호 빨간불이에요."
  - ROUTE이고 GREEN이나 YELLOW이며 `remainingSec`이 있고 `remainingSec < eta`이면 키 `…:soonred`, "앞 신호가 곧 빨간불로 바뀌어요. 속도를 줄이세요."
- motion이 STOPPED일 때: ROUTE이고, 거리 ≤ `stopLineM`이고, RED이며, `remainingSec`이 있을 때만 안내한다.
  - `remainingSec ≥ waitStartMinSec`: 키 `…:wait`, "{movement.label} 신호까지 {remainingSec 내림}초 남았어요."
  - `remainingSec ≤ waitSoonSec`: 키 `…:soon`, "곧 {movement.label} 신호로 바뀌어요."
- FREE에서 STOPPED이면 빈 목록이다.
- 모든 신호 안내의 우선순위는 SIGNAL이다.

**`HazardAdvisor.advices`**
- 날씨(ICE·FOG)
  - `now - produced > weatherMaxAge`이면 버린다.
  - INTEREST는 `badWeather`일 때만 남긴다.
  - 거리는 `segmentAheadDistance(fix, from, to, coneDeg, alignDeg)`다.
- ITS(CONSTRUCTION·ACCIDENT·DISASTER): 거리는 `aheadDistance(fix, from, coneDeg)`와 `aheadDistance(fix, to, coneDeg)` 중 작은 값이다.
- 거리가 null이거나 `announceM`보다 멀면 버린다.
- 키는 `"${hazard.key}:${severity}"`다.
- 우선순위: 날씨 DANGER, `fullClosure`, DISASTER는 HIGH, 나머지는 NORMAL이다.
- 문장(`d = koDistance(거리)`):
  - ICE: "$d 앞 살얼음 ${severity.label} 구간이에요. 속도를 줄이세요."
  - FOG INTEREST: "$d 앞 옅은 안개 구간이에요. 앞이 1km도 안 보여요."
  - FOG CAUTION: "$d 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요."
  - FOG DANGER: "$d 앞 짙은 안개 구간이에요. 앞이 200m도 안 보여요. 속도를 줄이세요."
  - CONSTRUCTION: fullClosure면 "$d 앞 도로가 전면 통제됐어요.", detail이 있으면 "$d 앞 공사 구간이에요. $detail.", 없으면 "$d 앞 공사 구간이에요."
  - ACCIDENT: "$d 앞에서 사고가 났어요."
  - DISASTER: "$d 앞 $detail 위험이 있어요."

- [ ] **Step 1: 실패하는 테스트 쓰기**

공통 준비: 울산대학교 교차로 `Intersection("3100000000","100","울산대학교",GeoPoint(35.543293,129.259894))`. 차는 그 북쪽 200m에서 남쪽(bearing 180)으로 다가온다. 진입 방향 가정이므로 신호 방향은 N이다. 트래커에는 Task 3의 `snap` 같은 도우미로 방향 N 표본 3개를 넣는다(배율 1000).

```kotlin
// SignalAdvisorTest
@Test fun `route red approach asks to slow down`()          // ROUTE, Turn(LEFT, 205.0), N-LEFT RED 40s, MOVING 10.0 → [Advice("sig:3100000000:100:red", SIGNAL, "앞 신호 빨간불이에요. 속도를 줄이세요.")]
@Test fun `free red approach only names the red light`()     // FREE, N-STRAIGHT RED 40s, MOVING → text "앞 신호 빨간불이에요."
@Test fun `route green ending before arrival warns soon red`() // ROUTE LEFT, N-LEFT GREEN 5s, 200m, 10 m/s → key "…:soonred", text "앞 신호가 곧 빨간불로 바뀌어요. 속도를 줄이세요."
@Test fun `red that turns green before arrival is not announced`() // FREE, RED 10s, 200m, 10 m/s (eta 20) → empty
@Test fun `stopped at red tells seconds to the left signal`()  // ROUTE LEFT, 30m, STOPPED, N-LEFT RED 25.4s → "좌회전 신호까지 25초 남았어요."
@Test fun `stopped near change says it will change soon`()    // 같은 조건, 4.0s → "곧 좌회전 신호로 바뀌어요."
@Test fun `free stopped stays quiet`()                         // FREE, STOPPED, RED → empty
@Test fun `right turn at the intersection gets no signal`()   // ROUTE, Turn(null, 200.0) → view == null
@Test fun `turn far beyond means straight here`()             // ROUTE, Turn(LEFT, 900.0) → view.movement == STRAIGHT
@Test fun `route mode without turn uses straight`()           // ROUTE, turn = null → view.movement == STRAIGHT
@Test fun `uncalibrated red still names the color but no seconds`() // 표본 2개: FREE MOVING → red 안내, ROUTE STOPPED → empty
@Test fun `nearest of two intersections ahead wins`()          // 120m·250m 앞 두 교차로 → view.intersection.id 가까운 쪽
@Test fun `heading convention reads the travel direction`()   // convention HEADING, bearing 180 → S 방향 신호를 읽음
// HazardAdvisorTest (차: 중부내륙선 링크 3580008302의 from에서 2km 앞쪽 뒤, 링크 방향 bearing)
@Test fun `caution fog two km ahead`()      // → "2km 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요.", key "fog:3580008302:CAUTION", NORMAL
@Test fun `interest fog needs bad weather`() // badWeather false → empty; true → "옅은" 문장
@Test fun `old weather is dropped`()          // produced 11분 전 → empty
@Test fun `opposite carriageway is ignored`() // 같은 링크를 to→from 방향으로 달림 → empty
@Test fun `four km away is not yet`()         // 4km → empty
@Test fun `construction with lanes`()         // detail "4차로 차단", 1.2km → "1.2km 앞 공사 구간이에요. 4차로 차단."
@Test fun `full closure and disaster are high`() // 전면 통제 → "…앞 도로가 전면 통제됐어요." HIGH; D03 → "…앞 지하차도 침수 위험이 있어요." HIGH
@Test fun `danger ice is high`()              // ICE DANGER 2.9km → "2.9km 앞 살얼음 위험 구간이에요. 속도를 줄이세요." HIGH
```

각 테스트의 단언은 주석에 적힌 값 그대로다(키·문장·우선순위 전체를 `assertEquals`로 비교).

- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat :core:test --tests "*AdvisorTest"` / Expected: 컴파일 실패
- [ ] **Step 3: 구현** — 위 규칙 그대로.
- [ ] **Step 4: 통과 확인** — Expected: 21개 통과
- [ ] **Step 5: 커밋** — `[T-15] 신호·도로 위험 안내 규칙`

---

### Task 7: 말하기 순서 — 한 번만, 우선순위, 내비 음성 대기

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/speak/Announcer.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/speak/AnnouncerTest.kt`

**Interfaces:**
- Consumes: Task 1 `Advice`, `Priority`
- Produces: `class Announcer(naviGapMs: Long = 200, forgetAfterMs: Long = 60_000)`, 메서드는 `fun pick(valid: List<Advice>, naviSpeaking: Boolean, selfSpeaking: Boolean, nowMonoMs: Long): Advice?`

**규칙**
- 다음 중 하나면 null이다.
  - `naviSpeaking`이나 `selfSpeaking`이 true
  - 내비 음성이 마지막으로 활성이던 시각에서 `naviGapMs`가 지나지 않음
- 그 밖이면 `valid` 중 아직 말하지 않은 키를 고른다. 우선순위(HIGH → SIGNAL → NORMAL)가 먼저고, 같으면 목록 순서다. 고른 키는 말한 것으로 기록한다.
- 말한 키 중 `valid`에 있는 것은 마지막으로 유효했던 시각을 갱신한다. 그 시각에서 `forgetAfterMs`보다 오래 지난 키(초과, 같으면 유지)는 기록에서 지운다. 같은 교차로를 나중에 다시 지나면 다시 말할 수 있게 하기 위해서다.

- [ ] **Step 1: 실패하는 테스트 쓰기**

```kotlin
class AnnouncerTest {
    private val sig = Advice("sig:a:red", Priority.SIGNAL, "s"); private val high = Advice("fog:1:DANGER", Priority.HIGH, "h")
    private val normal = Advice("evt:1:NORMAL", Priority.NORMAL, "n")

    @Test fun `highest priority first then the next`() {
        val a = Announcer()
        assertEquals(high, a.pick(listOf(normal, sig, high), false, false, 0))
        assertEquals(sig, a.pick(listOf(normal, sig, high), false, false, 10))
        assertEquals(normal, a.pick(listOf(normal, sig, high), false, false, 20))
        assertNull(a.pick(listOf(normal, sig, high), false, false, 30))
    }
    @Test fun `waits for navigation voice and a short gap`() {
        val a = Announcer()
        assertNull(a.pick(listOf(sig), naviSpeaking = true, selfSpeaking = false, nowMonoMs = 0))
        assertNull(a.pick(listOf(sig), false, false, 150))
        assertEquals(sig, a.pick(listOf(sig), false, false, 200))
    }
    @Test fun `own speech blocks the next one`() { assertNull(Announcer().pick(listOf(sig), false, true, 0)) }
    @Test fun `advice that expired while waiting is never spoken`() {
        val a = Announcer(); assertNull(a.pick(listOf(sig), true, false, 0))
        assertNull(a.pick(emptyList(), false, false, 500))
    }
    @Test fun `key can return after a minute away`() {
        val a = Announcer(); assertEquals(sig, a.pick(listOf(sig), false, false, 0))
        assertNull(a.pick(listOf(sig), false, false, 1_000))       // still valid: not repeated, last valid = 1_000
        assertNull(a.pick(emptyList(), false, false, 61_000))      // exactly 60 s: still remembered
        assertNull(a.pick(emptyList(), false, false, 61_001))      // over 60 s: forgotten
        assertEquals(sig, a.pick(listOf(sig), false, false, 61_002))
    }
}
```

- [ ] **Step 2~4:** 실패 확인 → 구현 → 통과 확인(5개)
- [ ] **Step 5: 커밋** — `[T-15] 말하기 순서와 내비 음성 대기`

---

### Task 8: 엔진 — 조립, 조회 판단, 호출 한도, 틱

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/RoadAheadEngine.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/RoadAheadEngineTest.kt`

**Interfaces:**
- Consumes: Task 2~7 전부
- Produces:

```kotlin
data class EngineConfig(val signal: SignalConfig = SignalConfig(), val hazard: HazardConfig = HazardConfig(),
    val signalPollMs: Long = 2_000, val signalPollRangeM: Double = 1_000.0, val signalDailyLimit: Int = 4_500,
    val zone: java.time.ZoneId = java.time.ZoneId.of("Asia/Seoul"))
data class Inputs(val navMode: NavMode, val turn: Turn?, val fix: Fix?, val wiperOn: Boolean, val defrosterOn: Boolean,
    val naviSpeaking: Boolean, val selfSpeaking: Boolean)
data class SignalCard(val movement: Movement, val color: LightColor, val remainingSec: Int?, val distanceM: Int)
data class Screen(val signal: SignalCard?, val hazardText: String?, val signalBudgetExhausted: Boolean)
data class TickResult(val screen: Screen, val speak: Advice?)
class RoadAheadEngine(cfg: EngineConfig = EngineConfig()) {
    fun onIntersections(list: List<Intersection>)
    fun onSignalPage(json: String, receivedMonoMs: Long)          // parseSignalPage + tracker.accept; ApiException is rethrown
    fun onHazards(source: HazardSource, hazards: List<Hazard>)     // replaces that source's list
    fun onSpeed(mps: Double, monoMs: Long)
    fun shouldPollSignals(fix: Fix?, nowMonoMs: Long, now: Instant): Boolean
    fun recordSignalPoll(nowMonoMs: Long, now: Instant)
    fun tick(inputs: Inputs, nowMonoMs: Long, now: Instant): TickResult
}
```

**규칙**

`shouldPollSignals`가 true가 되려면 다음이 모두 맞아야 한다.
- bearing 있는 fix가 있다.
- 신호를 받은 교차로(`liveKeys`) 중 하나가 `signalPollRangeM` 안 앞쪽에 있다. 아직 받은 신호가 하나도 없으면 교차로 목록 전체를 후보로 본다.
- 마지막 조회에서 `signalPollMs`가 지났다.
- 오늘(`zone` 기준 날짜) 조회 수가 `signalDailyLimit`보다 적다.

`tick`의 방향 처리:
- motion이 STOPPED이면 `inputs.fix`의 bearing 대신 마지막으로 MOVING일 때 받은 bearing을 쓴다.
- bearing이 null인 fix도 그 bearing으로 채운다.

`tick`의 결과:
- `badWeather = wiperOn || defrosterOn`
- 안내 후보 = 신호 안내 + 위험 안내. `speak = announcer.pick(...)`
- `screen.signal`은 view가 있을 때 만든다. 남은 초는 내림, 거리는 반올림이다.
- `screen.hazardText`는 위험 안내 중 우선순위가 가장 높은 것의 문장이다.
- `signalBudgetExhausted`는 오늘 조회 수가 한도에 이르렀는지다.

- [ ] **Step 1: 실패하는 테스트 쓰기**

신호 JSON은 테스트 안의 작은 도우미로 만든다. `signalJson(id = "100", totDt = "20261028160647", "ntLtsg" to ("stop-And-Remain" to "40000"))` 형태로 Task 2 형식을 따른다.

```kotlin
@Test fun `scene A from approach to waiting`()
// ROUTE, Turn(LEFT, …) 교차로 거리와 일치. 1초 간격 신호 3개(40000→39000→38000)를 넣는다.
// 200m 앞에서 10 m/s로 MOVING: speak 문장 "앞 신호 빨간불이에요. 속도를 줄이세요."
// 다음 틱: speak null(한 번만)
// 30m 앞에서 0 m/s로 2초 이상: speak "좌회전 신호까지 {n}초 남았어요."(n은 그 시점 내림값), screen.signal.color RED
// 신호 표본을 계속 넣어 남은 4초: speak "곧 좌회전 신호로 바뀌어요."
@Test fun `navigation voice delays and a stale advice is dropped`()
// wait 안내가 생긴 순간 naviSpeaking=true로 막는다. 남은 시간이 8초 아래로 떨어진 뒤 풀리면
// wait는 말하지 않고, 조건이 맞으면 soon만 나온다.
@Test fun `stopped car keeps last moving bearing`()
// MOVING bearing 180으로 교차로를 본 뒤 STOPPED에서 fix bearing 90(또는 null) → screen.signal은 그대로 같은 교차로·방향
@Test fun `signal budget resets at seoul midnight`()
// 2026-10-28T14:59:00Z(한국 23:59)에 recordSignalPoll 4,500회 → shouldPoll false, screen.signalBudgetExhausted true
// 2026-10-28T15:00:01Z(한국 00:00:01) → shouldPoll true
@Test fun `poll only when a known intersection is within a km ahead and two seconds passed`()
// 1.2km → false, 800m → true, recordSignalPoll 후 1.9초 → false, 2초 → true
@Test fun `stale signals disappear from the screen`()
// 마지막 새 totDt 수신 후 3.1초 → screen.signal == null
@Test fun `weather danger is announced before a signal`()
// FOG DANGER 위험(HIGH)과 신호 red(SIGNAL)가 동시에 유효 → 첫 speak는 안개, 다음 틱에 신호
```

- [ ] **Step 2~4:** 실패 확인 → 구현 → 통과 확인(7개). 마지막으로 `:core:test` 전체를 돌린다. Expected: 전부 통과.
- [ ] **Step 5: 커밋** — `[T-15] 엔진 조립과 조회 한도`

---

### Task 9: `:app` 골격 — 매니페스트, 키, 화면, 첫 실행 안내

**Files:**
- Modify: `settings.gradle.kts`(`include(":app")` 추가)
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/roadahead/MainActivity.kt`, `ui/RoadAheadScreen.kt`, `ui/Settings.kt`, `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 1 `Screen`, `SignalCard`, `LightColor`, `Movement`(Task 8의 `Screen`을 쓴다. Task 8 전이면 같은 이름의 데이터 클래스가 core에 있어야 하므로, Task 8의 데이터 클래스 4개(`Inputs`, `SignalCard`, `Screen`, `TickResult`)만 먼저 만들어 커밋해도 된다)
- Produces:
  - `@Composable fun RoadAheadScreen(screen: Screen, status: AppStatus, parked: Boolean, onOpenSettings: () -> Unit)`
  - `data class AppStatus(val modeLabel: String, val speechAvailable: Boolean, val note: String?)`
  - `enum class AppMode { LIVE, DEMO_A, DEMO_B, DEMO_C }`
  - `data class Prefs(val mode: AppMode, val convention: DirectionConvention, val disclaimerSeen: Boolean)`와 SharedPreferences 읽기·쓰기 함수

**결정 사항**

빌드:
- `app/build.gradle.kts`: Compose 플러그인, `buildConfig = true`, `useLibrary("android.car")`
- 의존성: Pleos Vehicle·NaviHelper·TTS, coroutines, `project(":core")`
- `buildConfigField` 3개: `SIGNAL_KEY`, `DISASTER_KEY`, `WEATHER_KEY`. 값은 Global Constraints의 순서(환경 변수 → `api` 파일)로 읽는다. 없으면 빈 문자열이다.

매니페스트:
- 권한
  - `pleos.car.permission.CAR_WINDOWS`(와이퍼), `CAR_CLIMATE`(성에 제거), `TTS_SERVICE`, `NAVI_ROUTE`
  - `android.car.permission.CAR_SPEED`, `android.permission.ACCESS_FINE_LOCATION`, `android.permission.INTERNET`
- `<uses-library android:name="android.car" android:required="false"/>`
- Activity: `resizeableActivity="true"`, `launchMode="singleTop"`, `<meta-data android:name="distractionOptimized" android:value="true"/>`

화면(DO 원칙):
- 신호 카드: 동작 이름, 색 원, 남은 초(64sp 이상)
- 그 아래 위험 문장 한 줄(24sp 이상), 맨 아래 상태 한 줄(모드, 음성 사용 불가 여부, 호출 한도 소진)
- 주행 중(`parked == false`)에는 버튼을 숨긴다. 설정 버튼은 parked일 때만 보인다.

설정 화면(Settings):
- 모드 4개(실시간, 시연 A, 시연 B, 시연 C)
- 방향 해석 2개(진입 방향, 진행 방향)

첫 실행 안내:
- parked일 때 한 번 띄운다.
- 문장 그대로: "신호와 도로 위험 정보는 공공 데이터를 바탕으로 한 참고용 정보예요. 실제 신호와 도로 상황을 먼저 확인하세요." 버튼 "확인"

런타임 권한: 위치와 `CAR_SPEED`를 `ActivityResultContracts.RequestMultiplePermissions`로 요청한다.

- [ ] **Step 1:** 위 파일을 만든다. MainActivity는 고정된 `Screen(SignalCard(LEFT, RED, 25, 30), "2km 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요.", false)`를 띄운다.
- [ ] **Step 2: 빌드** — Run: `.\gradlew.bat :app:assembleDebug :core:test` / Expected: BUILD SUCCESSFUL
- [ ] **Step 3: 에뮬레이터 확인**
  1. `adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk` → Success
  2. 앱을 실행하고, 권한 허용과 첫 실행 안내가 뜨는지 본다.
  3. Virtual Console로 기어를 D/0으로 바꾼다. 화면이 가려지지 않고, 설정 버튼이 사라져야 한다.
  4. 화면 캡처가 막히면 Orca ask로 Claude에게 요청한다(T-14와 같은 방식).
- [ ] **Step 4: 커밋** — `[T-16] 앱 골격과 주행 중 화면`

---

### Task 10: 실제 입력 연결 — 내비, GPS, 속도, 와이퍼·성에 제거, 내비 음성, TTS, API, 실시간 루프

**Files:**
- Create: `app/src/main/kotlin/com/roadahead/feed/Feed.kt`, `feed/LiveFeed.kt`, `feed/Apis.kt`, `car/PleosNavi.kt`, `car/GpsFix.kt`, `car/CarSignals.kt`, `car/NaviVoice.kt`, `car/Speech.kt`
- Modify: `MainActivity.kt`(고정 화면 → 엔진 루프)

**Interfaces:**
- Consumes: Task 8 `RoadAheadEngine`, `Inputs`, `TickResult`; Task 2·5 파서와 `weatherRequestTimes`; Task 9 화면·`Prefs`
- Produces (Task 11이 같은 인터페이스를 구현한다):

```kotlin
sealed interface DataEvent {
    data class Signals(val json: String, val receivedMonoMs: Long) : DataEvent
    data class Intersections(val json: String) : DataEvent
    data class Weather(val hazards: List<Hazard>) : DataEvent
    data class Events(val hazards: List<Hazard>) : DataEvent
    data class Disasters(val hazards: List<Hazard>) : DataEvent
}
interface Feed {
    val navMode: StateFlow<NavMode>; val turn: StateFlow<Turn?>; val fix: StateFlow<Fix?>
    val speed: SharedFlow<Pair<Double, Long>>          // m/s, monoMs
    val wiperOn: StateFlow<Boolean>; val defrosterOn: StateFlow<Boolean>; val naviSpeaking: StateFlow<Boolean>
    val data: SharedFlow<DataEvent>
    val label: StateFlow<String?>                       // 시연 장면 표시용, 실시간은 null
    fun start(scope: CoroutineScope, engine: RoadAheadEngine)
    fun stop()
}
class Speech(context: Context) { val available: StateFlow<Boolean>; val speaking: StateFlow<Boolean>; fun speak(text: String); fun release() }
```

**결정 사항**

호출 방법은 `spikes/road-ahead/app/src/main/kotlin/com/roadahead/spike/MainActivity.kt`와 `SpeechProbe.kt`에서 그대로 옮긴다.

어댑터:
- `PleosNavi`
  - `NavigationStatus.ROUTE_DRIVE` → ROUTE, `FREE_DRIVE` → FREE, 그 밖 → UNKNOWN
  - `onTBTInfo` 목록의 첫 항목으로 `Turn`을 만든다. `TURN_STRAIGHT`/`TURN_LEFT`/`TURN_UTURN`은 대응 Movement, 그 밖은 null이다. 목록이 비면 `turn = null`이다.
- `GpsFix`
  - `LocationManager.GPS_PROVIDER`를 1초 간격으로 받는다.
  - 직전 위치에서 5m 이상 움직였으면 provider bearing 대신 `bearingDegrees(직전, 현재)`를 쓴다.
  - 5m 미만이면 마지막으로 내보낸 bearing을 그대로 쓴다. 마지막 bearing이 없을 때만 `hasBearing()`이면 provider 값을 쓰고, 아니면 null이다.
  - (T-16에서 바꿈: 에뮬레이터 GPS가 `hasBearing=true, bearing=0`으로 고정됐다. 같은 좌표가 중복으로 들어와 provider 0이 섞이면 화면이 순간 사라졌다)
- `CarSignals`
  - `Car.createCar` → `CarPropertyManager.registerCallback(PERF_VEHICLE_SPEED, 5Hz)`. 값은 m/s, `status == 0`인 값만 쓴다.
  - Pleos Vehicle로 와이퍼 상태와 앞·뒤 성에 제거를 5초마다 읽는다. 둘 중 하나라도 켜짐이면 true다.
- `NaviVoice`: `AudioManager.registerAudioPlaybackCallback`. 활성 설정 중 `audioAttributes.usage == AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`가 하나라도 있으면 true다.
- `Speech`
  - Pleos TextToSpeech ON_DEVICE, ko-KR. 재생 용도는 기본값(MEDIA) 그대로 둔다.
  - 초기화에 실패하면 `available = false`다. 이때 화면 상태 줄에 "음성 안내를 쓸 수 없어요"를 보인다.
  - `speaking`은 onStart~onDone 동안 true다.

`Apis` (`HttpURLConnection`, `Dispatchers.IO`, 연결·읽기 시간 제한 10초):
- 신호: `https://apis.data.go.kr/B551982/rti/tl_drct_info`
- 교차로: `.../crsrd_map_info`
  - 신호·교차로 공통 파라미터: `serviceKey`, `stdgCd`, `pageNo`, `numOfRows=500`, `type=json`
  - 교차로는 `parseTotalCount`로 페이지를 끝까지 넘긴다.
- 기상: `https://apihub.kma.go.kr/api/typ04/url/road_file_down.php?roadNum&tm&authKey`. `weatherRequestTimes`를 차례로 시도해, 200이고 ZIP(`PK` 시작)이면 멈춘다.
- ITS 돌발: `https://openapi.its.go.kr:9443/eventInfo`
  - 파라미터: `apiKey`, `type=all`, `eventType=all`, `minX`·`maxX`·`minY`·`maxY`, `getType=json`
- ITS 재난: `https://openapi.its.go.kr:9443/disasterInfo`
  - 파라미터: `apiKey`, `category=D`, `eventType=all`, `startDate`·`endDate`(오늘, 한국 날짜 `yyyyMMdd`), 좌표 범위, `getType=json`
- 로그에는 URL의 키 값을 `***`로 바꿔 남긴다.

`LiveFeed` 일정:
- 시작할 때: 설정 지역(`signalRegions = listOf("3100000000")`)의 교차로 목록을 받는다.
- 신호: `engine.shouldPollSignals`가 true일 때 지역별로 한 번 조회하고, `recordSignalPoll`을 부른다.
- 기상: 5분마다 설정 노선(`weatherRoads = listOf("045")`)을 조회한다.
- ITS: 2분마다 현재 위치 ±0.1° 범위를 조회한다.
- 실패하면 로그만 남기고 다음 주기를 기다린다. 기상은 실패 후 60초 뒤에 다시 시도한다.

`MainActivity` 루프 (`lifecycleScope`):
- feed 흐름을 엔진에 넣는다(`onSpeed`, `onSignalPage`, `onIntersections`, `onHazards`).
- 250ms마다 `tick`을 돌려 화면을 갱신한다. `speak`가 있으면 `Speech.speak`.
- `parked`는 Vehicle 기어 P 여부다. 기어 구독은 `프로젝트1/app/src/main/kotlin/com/ontheway/VehicleProbe.kt`의 방식을 쓴다.
- 엔진은 `RoadAheadEngine(EngineConfig(signal = SignalConfig(convention = prefs.convention)))`로 만든다. 설정을 바꾸면 엔진을 새로 만든다.
- 모드가 LIVE면 `LiveFeed`를 쓴다. `AppStatus.note`에는 `feed.label` 값을 넣는다.

- [ ] **Step 1:** 어댑터와 `LiveFeed`, 루프를 구현한다.
- [ ] **Step 2: 빌드** — Run: `.\gradlew.bat :app:assembleDebug :core:test` / Expected: BUILD SUCCESSFUL
- [ ] **Step 3: 에뮬레이터 확인** (소리 없이: 사용자가 PC를 음소거했는지 Orca ask로 먼저 확인하거나, TTS는 로그만 확인)
  1. 설치 후 실시간 모드로 실행한다.
  2. `adb emu geo fix`로 울산대학교 북쪽 400m에서 교차로 쪽으로 1초에 10m씩 20번 옮긴다.
  3. logcat에서 다음을 확인한다.
     - 교차로 목록 수신(약 400건)
     - 신호 조회 성공(`K0`)
     - 엔진 화면 상태에 `signal != null`이 나오는지
  4. Virtual Console로 와이퍼를 켜면 `wiperOn=true` 로그가 나오는지 본다.
  5. API 호출 수를 HANDOFF에 적는다(신호는 하루 500회 안쪽).
- [ ] **Step 4: 커밋** — `[T-16] 실제 입력 연결과 실시간 루프`

---

### Task 11: 시연 모드 — 기록 재생, 장면 A·B·C

**Files:**
- Create: `tools/make_demo.py`, `app/src/main/assets/demo/scene-a.jsonl`, `scene-b.jsonl`, `scene-c.jsonl`, `app/src/main/kotlin/com/roadahead/feed/ReplayFeed.kt`
- Modify: `MainActivity.kt`(모드가 DEMO_*이면 `ReplayFeed(scene)`)

**Interfaces:**
- Consumes: Task 10 `Feed`, `DataEvent`; Task 5 `parseWeatherCsv`, `parseItsEvents`
- Produces: `class ReplayFeed(context: Context, asset: String) : Feed`

**장면 파일 형식** (한 줄에 사건 하나, `t`는 장면 시작부터의 ms)

```
{"t":0,"kind":"label","text":"시연: 기록한 울산 신호 재생 · 방향 가정: 진입 방향"}
{"t":0,"kind":"nav","mode":"ROUTE"}          {"t":0,"kind":"turn","movement":"LEFT","distanceM":400}
{"t":0,"kind":"fix","lat":35.5469,"lon":129.2599,"bearing":180}    {"t":0,"kind":"speed","mps":10.0}
{"t":0,"kind":"map","body":"<crsrd_map_info 원문>"}               {"t":1000,"kind":"signals","body":"<tl_drct_info 원문>"}
{"t":…,"kind":"wiper","on":true}  {"t":…,"kind":"defrost","on":false}  {"t":…,"kind":"navvoice","on":true}
{"t":…,"kind":"weather","hazard":"FOG","csv":"<LINK CSV 원문>"}   {"t":…,"kind":"events","body":"<eventInfo 원문>"}
```

**`ReplayFeed` 동작**
- 자산을 읽어 시작 시각(단조 시계) 기준으로 사건을 내보낸다.
- `signals` 사건의 `receivedMonoMs`는 내보내는 순간의 단조 시각이다.
- `weather`·`events` 사건의 `produced`는 내보내는 순간의 `Instant.now()`로 바꿔 파싱한다. 화면 label에 그 사실이 이미 적혀 있다.
- `speed`는 200ms마다 마지막 값을 다시 내보낸다. 실제 차량처럼 5Hz에 가깝게 하기 위해서다.
- 장면이 끝나면 마지막 상태를 유지한다.

**`tools/make_demo.py` (Python 3.12, 표준 라이브러리만)**: `docs/spikes/road-ahead/samples/`의 키를 지운 표본으로 세 장면을 만든다. 위치 궤적은 직선 보간으로 만든다.
- **장면 A**: 울산대학교(교차로 100)
  - `signal-cycle-*` 표본 중 교차로 100의 북쪽 신호를 2초 간격 원래 순서대로 쓴다. 각 표본 본문은 교차로 100 한 건만 남기도록 줄인다.
  - 차는 북쪽 400m에서 남쪽으로 10m/s로 다가와 30m 앞에서 선다. TBT는 LEFT이고, 거리는 교차로까지의 거리다.
  - 정차 시점은 북 좌회전이 빨간불이고 남은 시간이 20초 이상 남은 표본에 맞춘다.
- **장면 B**: 오지벌사거리(교차로 1)
  - 자유 주행으로 동쪽 직진 신호가 빨간불인 구간에 맞춰 다가간다.
  - 진입 방향 가정이므로 차는 동쪽에서 서쪽으로(bearing 270) 다가간다.
- **장면 C**
  - 중부내륙선 링크 3580008302의 FOG 주의 CSV 행을 쓰고, 차는 링크 방향으로 3.5km 앞에서 다가간다.
  - 이어 ITS 표본의 공사 항목(`evt:2240032401:…`) 근처로 위치를 옮긴다(label 변경).
  - 끝으로 합성 살얼음 행(`B_ICE=2`, 같은 링크 좌표)을 넣는다. label은 "합성 시나리오: 살얼음 자료는 11월 15일부터 제공돼 예시로 만들었어요"다.
- 장면마다 처음 10초 안에 첫 안내가 나오게 시각을 맞춘다. 전체 길이는 장면마다 90초 안쪽이다.
- `--self-test`는 다음을 확인한다.
  - 세 파일의 각 줄이 JSON이고 `t`가 줄지 않는지
  - `serviceKey`/`apiKey`/`authKey` 문자열과 `api` 파일의 키 값이 없는지

- [ ] **Step 1:** `make_demo.py`를 만들어 장면 파일을 생성한다. Run: `py -3.12 -I tools\make_demo.py` 다음 `py -3.12 -I tools\make_demo.py --self-test` / Expected: "self-test passed"
- [ ] **Step 2:** `ReplayFeed`와 모드 전환을 구현한다. Run: `.\gradlew.bat :app:assembleDebug :core:test` / Expected: BUILD SUCCESSFUL
- [ ] **Step 3: 에뮬레이터 확인** (TTS 소리는 사용자 허락 뒤, 아니면 logcat의 speak 문장으로 확인)

  장면마다 logcat에 다음 문장이 순서대로 나와야 한다.

  | 장면 | 나와야 하는 문장 |
  |---|---|
  | A | "앞 신호 빨간불이에요. 속도를 줄이세요." → "좌회전 신호까지 n초 남았어요." → "곧 좌회전 신호로 바뀌어요." |
  | B | "앞 신호 빨간불이에요." |
  | C | 안개 문장 → 공사 문장 → 살얼음 문장 |

  장면마다 화면 캡처 한 장을 Claude에게 요청한다.
- [ ] **Step 4: 커밋** — `[T-17] 시연 모드와 장면 A·B·C`

---

## 이 계획에서 뺀 것 (나중에 필요하면 추가)

- **TBT 거리 기반 내비 음성 회피(예비안)**: 재생 용도 정보가 오지 않는 환경이 확인되면 Announcer 입력에 추가한다.
- **실시간 모드의 지역·노선 자동 선택**: 지금은 설정값(`signalRegions`, `weatherRoads`)이다. 지역이 늘면 위치로 고르는 방식을 만든다.
- **포그라운드 서비스**: 지금은 Activity가 화면에 있을 때만 동작한다. 멀티 윈도우로 내비 옆에 띄워 두는 사용 방식에 맞췄다.
- **내비 돌발 목록(`RouteStateInfo.trafficStatus`)과 고정 주의 지점 사용**: 실측에서 둘 다 비어 있었고, 내비가 스스로 말하는지도 모른다. 공사·사고·재난은 ITS로 다룬다. 값이 오는 것이 확인되면 `HazardSource`에 추가한다.
- **경로 좌표를 쓴 "경로 앞쪽" 판정**: NaviHelper가 경로 좌표를 주지 않는다. 경로 안내 중에도 진행 방향 부채꼴로 앞쪽을 판정한다.
- **release 서명·APK 이름·README**: 별도 작업으로 만든다. 팀명은 **SUDL**이므로 파일명은 `SUDL_{앱이름}_release.apk`다(앱 이름 확정 후).
