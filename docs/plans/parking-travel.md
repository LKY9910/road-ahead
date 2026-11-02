# 주차 안내·여행 모드·내비 위 카드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 이 저장소에서는 Codex가 Orca 작업으로 받아 실행한다(AGENTS.md 작업 흐름).

**Goal:** 목적지 가까이에서 주차장 빈자리·요금을, 여행 모드에서는 목적지를 정한 순간 근처 행사·명소를 알려 주고, 모든 안내를 내비 위 오른쪽 아래 카드 한 장(오버레이)으로 보여 준다.

**Architecture:** 판단은 지금처럼 `:core`의 `RoadAheadEngine`이 한다. 새 `ParkingAdvisor`·`TravelAdvisor`가 목적지(`Destination`)와 주차장·관광 목록을 받아 안내와 카드를 만든다. 엔진은 매 틱마다 보여 줄 카드 하나(`Screen.card`)를 고르고, 언제 주차·관광 자료를 받아 와야 하는지 알려 준다. `:app`은 엔진 루프를 Activity에서 foreground service로 옮기고, 카드를 터치를 받지 않는 오버레이 창에 그린다.

**Tech Stack:** 기존 그대로(Kotlin 2.1.21, JDK 17, AGP 8.7.2, coroutines 1.10.2, serialization-json 1.8.1, JUnit 5.11.4, Pleos SDK). 오버레이 카드는 Android View로 그린다. 새 라이브러리를 추가하지 않는다.

**Spec:** [docs/DESIGN.md](../DESIGN.md) 10·11·12절. 근거: [docs/spikes/overlay.md](../spikes/overlay.md)(T-21). 기존 계획 [road-ahead.md](road-ahead.md)의 Global Constraints는 그대로 유효하다.

**범위:** 이 문서는 1부(Task 1~4)다. 주차·관광 API 응답 해석, 실시간 연결(목적지 콜백, API 호출), 시연 장면 D·E는 T-23 실측 결과를 받은 뒤 2부(Task 5~7)로 Claude가 덧붙인다. 1부는 API 필드에 의존하지 않는다.

## Global Constraints

- [road-ahead.md](road-ahead.md)의 Global Constraints 전부(키 취급, 금지 표현, 시간 인자, 테스트 이름, 커밋 규칙, 소리 시험 허락)
- 안내 문장은 이 계획에 적힌 글자 그대로 쓴다. 조사(이/가, 이에요/예요)는 Task 1의 함수로만 붙인다.
- 기준값(DESIGN 10·11절): 주차 알림 시점 남은 거리 1,000 m 또는 남은 시간 180초, 주차장 반경 500 m, 빈자리 나이 상한 10분, 목적지 100 m 안에서는 주차 안내 버림. 행사 반경 5 km·최대 2개, 명소 반경 3 km·최대 3개, P에서 목록 60초.
- 주차·관광 자료가 없거나 실패하면 아무것도 말하지 않는다. "주차장 없음", "행사 없음"이라고 말하지 않는다.
- 경로를 바꾸지 않는다. NaviHelper의 경로 요청 함수를 새로 부르지 않는다.
- 오버레이 창: `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE`, `LayoutParams.alpha = 0.8f`(T-21: 이 이하여야 아래 내비 터치가 통과한다), `Gravity.BOTTOM | Gravity.END`.

## Review Focus

1. **경로 중간에 목적지를 바꿈**: 새 목적지에 대해 주차·여행 안내가 다시 한 번 나와야 하고, 이전 목적지의 주차장 목록을 새 목적지에 쓰면 안 된다. → Task 3 `new destination resets parking and travel`
2. **오래된 빈자리 숫자**: 갱신 10분이 지난 빈자리를 "빈자리 n면"으로 말하면 안 된다. → Task 1 `stale availability is not spoken`
3. **조사**: 이름이 받침 없이 끝나거나 한글이 아닌 글자로 끝날 때 어색한 조사가 붙는다. → Task 1 `particles follow final consonant`
4. **남은 거리 없이 남은 시간만 옴**: 시간만으로도 알림 시점을 판단해야 한다. 둘 다 없으면 알리지 않는다. → Task 3 `parking trigger uses remaining time when distance is missing`
5. **오버레이 권한이 실행 중에 꺼짐**: `addView`·`updateViewLayout`이 예외를 내도 서비스가 죽지 않고, 앱 화면에 권한 안내가 나와야 한다. → Task 4 Step 6

---

## File Structure

```
core/src/main/kotlin/com/roadahead/core/
  model/Models.kt            Destination, ParkingLot, TourKind, TourItem, ParkingCard, TravelLine, Card 추가   (Task 1)
  text/Korean.kt             subject(), copula(), koFee(), koUntil() 추가                                    (Task 1)
  advice/ParkingAdvisor.kt   주차 알림 시점, 주차장 고르기, 문장                                             (Task 1)
  advice/TravelAdvisor.kt    행사·명소 고르기, 문장, 목록                                                    (Task 2)
  RoadAheadEngine.kt         Inputs·Screen 확장, 요청 판단, 카드 고르기                                      (Task 3)
app/src/main/
  AndroidManifest.xml        서비스, 권한                                                                    (Task 4)
  kotlin/com/roadahead/RoadAheadService.kt     엔진 루프(MainActivity에서 옮김), 알림                         (Task 4)
  kotlin/com/roadahead/ui/OverlayCard.kt       오버레이 창과 카드 View                                        (Task 4)
  kotlin/com/roadahead/MainActivity.kt         권한·상태·설정 화면만                                          (Task 4)
  kotlin/com/roadahead/ui/RoadAheadScreen.kt, ui/Settings.kt   상태 화면, 여행 모드 설정                       (Task 4)
```

---

### Task 1: 공용 타입, 조사·요금 문장, 주차 안내 규칙

**Files:**
- Modify: `core/src/main/kotlin/com/roadahead/core/model/Models.kt`
- Modify: `core/src/main/kotlin/com/roadahead/core/text/Korean.kt`
- Create: `core/src/main/kotlin/com/roadahead/core/advice/ParkingAdvisor.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/text/KoreanTest.kt`(있으면 추가), `core/src/test/kotlin/com/roadahead/core/advice/ParkingAdvisorTest.kt`

**Interfaces:**
- Consumes: `GeoPoint`, `NavMode`, `Advice`, `Priority`, `distanceMeters`, `koDistance`, `SignalCard`(RoadAheadEngine.kt)
- Produces (Models.kt):
  ```kotlin
  data class Destination(val key: String, val name: String?, val point: GeoPoint, val remainDistanceM: Double?, val remainTimeSec: Int?)
  data class ParkingLot(val id: String, val name: String, val point: GeoPoint, val available: Int?, val availableProduced: java.time.Instant?, val basicMinutes: Int?, val basicFeeWon: Int?)
  enum class TourKind(val label: String) { FESTIVAL("축제"), ATTRACTION("관광지"), CULTURE("문화시설"), LEISURE("레포츠") }
  data class TourItem(val id: String, val kind: TourKind, val title: String, val point: GeoPoint, val start: java.time.LocalDate?, val end: java.time.LocalDate?)
  data class ParkingCard(val name: String, val available: Int?, val distanceText: String, val feeText: String?)
  data class TravelLine(val title: String, val kind: TourKind, val distanceText: String, val periodText: String?)
  sealed interface Card {
      data class Signal(val signal: SignalCard) : Card
      data class Hazard(val text: String) : Card
      data class Parking(val parking: ParkingCard) : Card
      data class Travel(val lines: List<TravelLine>) : Card
  }
  ```
- Produces (Korean.kt): `fun subject(word: String): String`(word+이/가), `fun copula(word: String): String`(word+이에요/예요), `fun koFee(minutes: Int?, won: Int?): String?`, `fun koUntil(end: LocalDate, today: LocalDate): String`
- Produces (ParkingAdvisor.kt):
  ```kotlin
  data class ParkingConfig(val triggerM: Double = 1_000.0, val triggerSec: Int = 180, val radiusM: Double = 500.0,
      val maxAge: java.time.Duration = java.time.Duration.ofMinutes(10), val dropWithinM: Double = 100.0)
  class ParkingAdvisor(cfg: ParkingConfig = ParkingConfig()) {
      fun due(dest: Destination?, navMode: NavMode): Boolean
      fun pick(dest: Destination, lots: List<ParkingLot>, now: Instant): Pair<ParkingCard, Advice>?
  }
  ```

규칙:
- 조사: 마지막 글자가 한글이면 받침 유무로 고른다(받침 있음 → "이", "이에요"; 없음 → "가", "예요"). 한글이 아니면 받침 없음으로 본다("1.2km예요").
- `koFee`: `won == 0` → `"무료예요"`. `minutes`와 `won`이 모두 있으면 `"{minutes}분에 {won 천 단위 쉼표}원이에요"`. 그 밖에는 null.
- `koUntil`: `end == today` → `"오늘"`, `end == today+1` → `"내일"`, 그 밖에는 `"{M}월 {D}일"`(앞자리 0 없음).
- `due`: `navMode == ROUTE`이고 `dest != null`이며, (`remainDistanceM ≤ triggerM` 또는 `remainTimeSec ≤ triggerSec`) 이고, `remainDistanceM`이 null이거나 `> dropWithinM`일 때 true. 남은 거리·시간이 둘 다 null이면 false.
- `pick`:
  - 목적지에서 `radiusM` 안의 주차장만 본다.
  - 새것 = `availableProduced`가 있고 `now - availableProduced ≤ maxAge`. 실시간 응답에 갱신 시각이 없으므로(T-23 중간 보고) `availableProduced`에는 앱이 응답을 받은 시각을 넣는다(2부 Task 5).
  - 1순위: 새것이면서 `available ≥ 1`인 곳 중 가장 가까운 곳 → 빈자리 문장, `ParkingCard.available = available`
  - 2순위: 새것이 아닌 곳(빈자리 모름) 중 가장 가까운 곳 → 모름 문장, `available = null`. 새것이면서 0면인 곳은 고르지 않는다.
  - 둘 다 없으면 null.
  - 문장(요금이 null이면 요금 문장과 그 앞 공백을 뺀다):
    - 빈자리: `"목적지 근처 {name}에 빈자리 {n}면이 있어요. {fee}"`
    - 모름: `"목적지 근처에 {subject(name)} 있어요. {fee}"`
  - `Advice(key = "parking:${dest.key}", priority = Priority.NORMAL, text)`, `distanceText = "목적지에서 " + koDistance(거리)`, `feeText = koFee(...)`.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
// KoreanTest
@Test fun `particles follow final consonant`() {
    assertEquals("시청 공영주차장이", subject("시청 공영주차장"))
    assertEquals("중앙 파크가", subject("중앙 파크"))
    assertEquals("전망대 공원이에요", copula("전망대 공원"))
    assertEquals("시립 미술관이에요", copula("시립 미술관"))
    assertEquals("1.2km예요", copula("1.2km"))
    assertEquals("ABC예요", copula("ABC"))
}
@Test fun `fee text`() {
    assertEquals("30분에 1,000원이에요", koFee(30, 1000))
    assertEquals("무료예요", koFee(null, 0))
    assertNull(koFee(30, null)); assertNull(koFee(null, null))
}
@Test fun `until text`() {
    val today = LocalDate.of(2026, 10, 29)
    assertEquals("오늘", koUntil(today, today)); assertEquals("내일", koUntil(today.plusDays(1), today))
    assertEquals("11월 2일", koUntil(LocalDate.of(2026, 11, 2), today))
}

// ParkingAdvisorTest: dest = Destination("d1", "시청", GeoPoint(35.5384, 129.3114), 900.0, 120), now = 2026-10-29T05:00:00Z
// lot(m north of dest) helper builds GeoPoint by moving lat
@Test fun `fresh free spaces are spoken`() {
    // lot A 180 m, available 23, produced now-2min, 30분 1000원
    assertEquals(Pair(ParkingCard("시청 공영주차장", 23, "목적지에서 200m", "30분에 1,000원이에요"),
        Advice("parking:d1", Priority.NORMAL, "목적지 근처 시청 공영주차장에 빈자리 23면이 있어요. 30분에 1,000원이에요.")),
        advisor.pick(dest, listOf(lotA), now))
}
@Test fun `stale availability is not spoken`() {
    // lot A produced now-11min, available 23 → 모름 문장, available null
    assertEquals("목적지 근처에 시청 공영주차장이 있어요. 30분에 1,000원이에요.", advisor.pick(dest, listOf(staleA), now)!!.second.text)
    assertNull(advisor.pick(dest, listOf(staleA), now)!!.first.available)
}
@Test fun `full lot is skipped for unknown one`() {
    // lot A fresh 0면 150 m, lot B unknown 400 m "중앙 파크" fee null → "목적지 근처에 중앙 파크가 있어요."
}
@Test fun `lots beyond radius give nothing`() { /* 600 m only → null */ }
@Test fun `free fee`() { /* basicFeeWon 0 → "...빈자리 5면이 있어요. 무료예요." */ }
@Test fun `due rules`() {
    assertTrue(advisor.due(dest, NavMode.ROUTE))
    assertFalse(advisor.due(dest, NavMode.FREE))
    assertFalse(advisor.due(dest.copy(remainDistanceM = 1500.0, remainTimeSec = 300), NavMode.ROUTE))
    assertTrue(advisor.due(dest.copy(remainDistanceM = null, remainTimeSec = 150), NavMode.ROUTE))
    assertFalse(advisor.due(dest.copy(remainDistanceM = null, remainTimeSec = null), NavMode.ROUTE))
    assertFalse(advisor.due(dest.copy(remainDistanceM = 80.0), NavMode.ROUTE))
}
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :core:test` → 컴파일 실패(새 타입·함수 없음)
- [ ] **Step 3: 위 Interfaces와 규칙대로 구현한다**
- [ ] **Step 4: 통과 확인** — `.\gradlew.bat :core:test` → 전부 통과(기존 78개 포함)
- [ ] **Step 5: 커밋** — `git add` 위 파일만, `[T-24] 주차 안내 규칙과 공용 타입`

---

### Task 2: 여행 모드 규칙

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/advice/TravelAdvisor.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/advice/TravelAdvisorTest.kt`

**Interfaces:**
- Consumes: Task 1의 `Destination`, `TourItem`, `TourKind`, `TravelLine`, `subject`, `copula`, `koUntil`, `koDistance`, `distanceMeters`
- Produces:
  ```kotlin
  data class TravelConfig(val festivalM: Double = 5_000.0, val festivalMax: Int = 2, val placeM: Double = 3_000.0, val placeMax: Int = 3)
  class TravelAdvisor(cfg: TravelConfig = TravelConfig()) {
      fun pick(dest: Destination, items: List<TourItem>, arrival: LocalDate, today: LocalDate): Pair<List<TravelLine>, Advice>?
  }
  ```

규칙:
- 행사: `kind == FESTIVAL`, `start`와 `end`가 모두 있고 `start ≤ arrival ≤ end`, 거리 ≤ `festivalM`. 가까운 순 최대 `festivalMax`개.
- 명소: `kind`가 ATTRACTION·CULTURE·LEISURE, 거리 ≤ `placeM`. 가까운 순 최대 `placeMax`개.
- 둘 다 비면 null.
- 문장: 아래 조각을 공백 하나로 잇는다.
  - 첫 행사: `"목적지 근처에서 {koUntil(end, today)}까지 {subject(title)} 열려요. 목적지에서 {copula(koDistance(거리))}."`
  - 둘째 행사: `"{title}도 열려요."`
  - 명소가 있으면: `"가까운 명소는 {이름들을 ", "로 이은 뒤 마지막에 copula}."`. 예: `"가까운 명소는 시립 미술관, 전망대 공원이에요."`
- 목록: 행사(가까운 순) 다음 명소(가까운 순). `TravelLine(title, kind, koDistance(거리), periodText = 행사면 "{koUntil(end, today)}까지" 아니면 null)`
- `Advice("travel:${dest.key}", Priority.NORMAL, text)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
// today = 2026-10-29, arrival = today, dest key "d2"
@Test fun `festival and places`() {
    // festival "강변 가을 축제" 1.2 km, 2026-10-25..2026-11-02; places "시립 미술관"(CULTURE) 0.8 km, "전망대 공원"(ATTRACTION) 2.1 km
    val (lines, advice) = advisor.pick(dest, items, today, today)!!
    assertEquals("목적지 근처에서 11월 2일까지 강변 가을 축제가 열려요. 목적지에서 1.2km예요. 가까운 명소는 시립 미술관, 전망대 공원이에요.", advice.text)
    assertEquals(listOf(TravelLine("강변 가을 축제", TourKind.FESTIVAL, "1.2km", "11월 2일까지"),
        TravelLine("시립 미술관", TourKind.CULTURE, "800m", null), TravelLine("전망대 공원", TourKind.ATTRACTION, "2.1km", null)), lines)
}
@Test fun `festival outside arrival date is skipped`() { /* end = 2026-10-28 → 행사 문장 없음, 명소 문장만 */ }
@Test fun `festival without dates is skipped`() { /* start/end null → 제외 */ }
@Test fun `arrival tomorrow uses tomorrow window`() { /* arrival = 10-30, festival 10-30..10-30 → "내일까지" */ }
@Test fun `nothing nearby gives nothing`() { /* 6 km festival, 4 km place → null */ }
@Test fun `at most two festivals and three places`() { /* 3 festivals + 4 places → lines 5, 둘째 행사는 "{title}도 열려요." */ }
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :core:test` → 컴파일 실패
- [ ] **Step 3: 구현한다**
- [ ] **Step 4: 통과 확인** — `.\gradlew.bat :core:test`
- [ ] **Step 5: 커밋** — `[T-24] 여행 모드 행사·명소 규칙`

---

### Task 3: 엔진 — 목적지, 자료 요청, 카드 고르기

**Files:**
- Modify: `core/src/main/kotlin/com/roadahead/core/RoadAheadEngine.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/RoadAheadEngineTest.kt`

**Interfaces:**
- Consumes: Task 1·2 전부
- Produces:
  ```kotlin
  data class EngineConfig(..., val parking: ParkingConfig = ParkingConfig(), val travel: TravelConfig = TravelConfig(), val travelListMs: Long = 60_000)
  data class Inputs(val navMode: NavMode, val turn: Turn?, val fix: Fix?, val wiperOn: Boolean, val defrosterOn: Boolean,
      val naviSpeaking: Boolean, val selfSpeaking: Boolean,
      val parked: Boolean = false, val travelMode: Boolean = false, val destination: Destination? = null)
  data class Screen(val signal: SignalCard?, val hazardText: String?, val signalBudgetExhausted: Boolean, val card: Card? = null)
  // RoadAheadEngine
  fun parkingRequest(dest: Destination?, navMode: NavMode): Destination?
  fun onParkingLots(destKey: String, lots: List<ParkingLot>)
  fun tourRequest(dest: Destination?, travelMode: Boolean): Destination?
  fun onTourItems(destKey: String, items: List<TourItem>)
  ```

규칙:
- `parkingRequest`: `ParkingAdvisor.due(dest, navMode)`이고 그 `dest.key`를 아직 요청하지 않았으면 dest를 돌려주고 요청한 것으로 기록한다. 아니면 null. 목적지마다 한 번이다.
- `tourRequest`: `travelMode`이고 `dest != null`이고 그 `dest.key`를 아직 요청하지 않았으면 dest를 돌려주고 기록한다.
- `onParkingLots`·`onTourItems`: 그 목적지 키의 목록으로 저장한다. 다른 키의 목록은 버린다(목적지는 하나만 기억한다).
- `tick`:
  - 주차: `inputs.destination`의 키에 저장된 주차장이 있고 `due`가 true이면 `pick` → 카드 후보와 안내 후보
  - 여행: `inputs.travelMode`이고 그 키의 관광 목록이 있으면 `pick(dest, items, arrival, today)`
    - `today` = `now`의 서울 날짜
    - `arrival` = `now + remainTimeSec`(null이면 0)의 서울 날짜
    - 안내는 주행 중에도 후보에 넣는다.
    - 목록은 `inputs.parked`일 때만 보여 준다. 처음 보여 준 단조 시각부터 `travelListMs` 동안만 보여 주고, 그 사이 `parked`가 false가 되면 그 목적지 목록은 다시 보여 주지 않는다.
  - 안내 후보 = 신호 + 위험 + 주차 + 여행. 지금처럼 `Announcer.pick`으로 하나를 고른다.
  - `Screen.card` 고르는 순서:
    1. 여행 목록(보여 줄 때)
    2. 우선순위 HIGH인 위험
    3. 신호
    4. 나머지 위험
    5. 주차
    6. 없으면 null
  - `signal`·`hazardText`·`signalBudgetExhausted`는 지금과 같다.

- [ ] **Step 1: 실패하는 테스트를 쓴다** (기존 테스트의 `Inputs(...)` 호출은 기본값 덕분에 그대로 컴파일된다)

```kotlin
@Test fun `parking requested once per destination`() {
    val d = Destination("d1", "시청", GeoPoint(35.5384, 129.3114), 900.0, 120)
    assertEquals(d, engine.parkingRequest(d, NavMode.ROUTE)); assertNull(engine.parkingRequest(d, NavMode.ROUTE))
}
@Test fun `parking trigger uses remaining time when distance is missing`() {
    val d = Destination("d1", null, GeoPoint(35.5384, 129.3114), null, 150)
    assertEquals(d, engine.parkingRequest(d, NavMode.ROUTE))
}
@Test fun `parking card and advice after lots arrive`() {
    // onParkingLots("d1", [fresh 23면 lot]) → tick(ROUTE, destination=d) → card == Card.Parking(...), speak.key == "parking:d1"
}
@Test fun `new destination resets parking and travel`() {
    // d1 요청·목록 저장 후 d2(다른 키)로 tick → d1 목록을 쓰지 않음(card null, speak null), parkingRequest(d2) 다시 non-null
}
@Test fun `travel list shows only when parked and for sixty seconds`() {
    // travelMode=true, onTourItems("d2", items) → parked=false tick: speak "travel:d2", card != Travel
    // parked=true tick at t: card is Travel; at t+59_000 Travel; at t+61_000 not Travel
}
@Test fun `travel list closes for good after driving off`() {
    // parked tick (Travel 보임) → parked=false tick → parked=true tick → card != Travel
}
@Test fun `high hazard outranks signal on the card`() { /* HIGH 위험과 신호가 함께 있으면 Card.Hazard */ }
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :core:test`
- [ ] **Step 3: 구현한다**
- [ ] **Step 4: 통과 확인** — `.\gradlew.bat :core:test` 전부 통과
- [ ] **Step 5: 커밋** — `[T-24] 엔진에 목적지·주차·여행과 카드 고르기 추가`

---

### Task 4: 내비 위 카드와 foreground service

**Files:**
- Create: `app/src/main/kotlin/com/roadahead/RoadAheadService.kt`, `app/src/main/kotlin/com/roadahead/ui/OverlayCard.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `MainActivity.kt`, `ui/RoadAheadScreen.kt`, `ui/Settings.kt`, `res/values/strings.xml`
- Test: `app/src/test/kotlin/com/roadahead/…` 기존 테스트 유지(`DataEventTest` 등)

**Interfaces:**
- Consumes: Task 3의 `Screen.card`, `Inputs`의 새 필드(이번 Task에서는 `parked`·`travelMode`만 채우고 `destination`은 null. 2부에서 채운다)
- Produces: `RoadAheadService`(시작 Intent extra `logOnlySpeech: Boolean`), `OverlayCard(context).show(card: Card?)`, `Prefs.travelMode: Boolean`(SharedPreferences 키 `travelMode`, 기본 false)

방식:
- **서비스**
  - 지금 `MainActivity`의 `startInputs`·`restartFeed`·틱 루프를 `RoadAheadService`로 옮긴다. 서비스는 자기 `CoroutineScope(SupervisorJob() + Dispatchers.Main)`을 쓴다.
  - Activity는 권한을 확인하고, 서비스를 시작·재시작하고, 상태와 설정을 보여 준다. 설정이 바뀌면 서비스를 다시 시작한다.
  - `Inputs.parked`는 `CarSignals.parked`에서 채운다.
  - 매니페스트: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `SYSTEM_ALERT_WINDOW` 권한. `<service android:name=".RoadAheadService" android:exported="false" android:foregroundServiceType="specialUse">`와 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 값 `"Shows road-ahead guidance cards over the navigation app while driving"`. T-21 시험 앱과 같은 방식이다.
  - 알림: 채널 `road_ahead`, 소리·진동 없음, 내용 `"앞길 안내가 켜져 있어요"`, 동작 버튼 `"끄기"` → 서비스 종료
- **카드**(`OverlayCard`, Android View)
  - 창 규칙은 Global Constraints대로다. `x = y = 40dp`, 너비 600dp, 높이 `WRAP_CONTENT`, 카드가 null이면 `View.GONE`
  - 모양(DESIGN 시안의 2배 크기)
    - 바탕 `#F0111418`, 모서리 32dp, 안쪽 여백 위 28·좌우 40·아래 36dp
    - 위쪽 작은 줄 24sp `#A7B0BA`: `"앞길 안내 · 신호"`, `"· 도로 위험"`, `"· 주차"`, `"· 여행"`
    - 왼쪽 원 88dp: 빨강 `#E5484D`, 초록 `#30A46C`, 노랑 `#F5C518`, 모름 회색 `#6B7280`, 위험 `#F5A524`, 주차 `#3E7BFA`, 여행 `#30A46C`
  - 종류별 내용
    - 신호: 제목 34sp `"{movement.label} 신호"`, 큰 글씨 96sp `"{n}초"`(모르면 `"—"`)
    - 위험: 본문 40sp 흰색 최대 2줄
    - 주차: 제목 34sp 이름, 큰 글씨 96sp `"빈자리 {n}면"`(null이면 생략), 보조 30sp `#C9D1D9` `"{distanceText} · {feeText}"`(fee null이면 거리만)
    - 여행: 줄마다 28sp `"{title} · {kind.label} · {distanceText}"`와 행사면 `" · {periodText}"`, 최대 5줄
  - 카드 내용이 같으면 뷰를 다시 만들지 않는다.
- **앱 화면**(`RoadAheadScreen`)
  - 큰 신호·위험 표시를 없애고, 상태 줄과 아래 안내만 둔다.
  - `Settings.canDrawOverlays(context)`가 false이면 다음을 보여 준다.
    - 안내 문장: `"내비 위에 안내 카드를 띄우려면 Pleos 설정 → 앱 → 앱 목록 → 앞길 안내 → 다른 앱 위에 표시를 켜 주세요."`
    - 버튼 `"앱 정보 열기"`(`ACTION_APPLICATION_DETAILS_SETTINGS`, `package:` URI)
    - 서비스는 시작하지 않는다.
  - 표준 `ACTION_MANAGE_OVERLAY_PERMISSION`은 Pleos에 없다(T-21).
- **설정**: `"여행 모드"` 켜기/끄기 행을 모드 선택 아래에 둔다. 최소 높이 64dp, 글자 24sp, 기존 행과 같은 모양.

- [ ] **Step 1: 서비스로 옮기기** — 동작을 바꾸지 않고 루프만 옮긴다. `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` 성공
- [ ] **Step 2: 오버레이 카드** — 위 규칙대로 그린다. 빌드 성공
- [ ] **Step 3: 앱 화면·설정** — 권한 안내, 여행 모드 행. 빌드 성공
- [ ] **Step 4: 에뮬레이터 확인**(소리 없이)
  - debug APK를 `emulator-5556` user 10에 설치하고 다른 앱 위에 표시를 켠다.
  - `--ez logOnlySpeech true`로 시연 A를 실행하고 내비를 앞으로 가져온다.
  - P/0과 D/30에서 카드가 내비 위 오른쪽 아래에 보이는지 캡처한다. logcat의 `speak=` 순서는 지금과 같아야 한다(빨간불 → 32초 → 곧 좌회전).
  - 카드 아래 내비 버튼이 눌리는지 `input tap`으로 본다.
  - 끝나면 P/0으로 되돌리고 앱을 force-stop한다.
  - Codex 자동 승인 검토에 막히면 ask로 Claude에게 맡긴다.
- [ ] **Step 5: 시연 C 확인** — 위험 카드(주황 원, 2줄)가 보이는지 캡처한다.
- [ ] **Step 6: 권한 끔 확인** — 실행 중에 다른 앱 위에 표시를 끄면, 서비스가 비정상 종료(`FATAL EXCEPTION`)되지 않고 앱 화면에 권한 안내가 나와야 한다. `addView`·`updateViewLayout`·`removeView`를 감싸 예외를 잡고 카드 표시를 멈춘다.
- [ ] **Step 7: 검증과 커밋**
  - `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 성공
  - 캡처를 `docs/demo/overlay-*.png`로 저장한다.
  - `[T-25] 내비 위 안내 카드와 foreground service`로 커밋한다.

---

## 2부 (2026-10-29, T-23 결과 반영)

근거: [spikes/parking-travel.md](../spikes/parking-travel.md), [APIS.md](../APIS.md) 5·6절, DESIGN 10·11절(같은 날 갱신). **TS 실시간 빈자리는 쓰지 않는다.** 빈자리는 서울 실시간 API(Task 8, T-26 실측 뒤 추가)에서만 온다. 2부의 Task 5~7·9는 서울 키 없이 진행할 수 있다.

### Task 5: 관광 응답과 주차 자산 해석

**Files:**
- Create: `core/src/main/kotlin/com/roadahead/core/tour/TourParser.kt`, `core/src/main/kotlin/com/roadahead/core/parking/ParkingAsset.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/tour/TourParserTest.kt`, `core/src/test/kotlin/com/roadahead/core/parking/ParkingAssetTest.kt`
- Fixtures: `core/src/test/resources/fixtures/tour-seoul-location-all.json`, `tour-seoul-ldong-today.json`
  - `docs/spikes/parking-travel/samples/`의 같은 이름 파일에서 `response` 값만 꺼내 저장한다. 키·요청 URL은 넣지 않는다.

**Interfaces:**
- Produces:
  ```kotlin
  fun parseTourPlaces(json: String): List<TourItem>       // locationBasedList2
  fun parseTourFestivals(json: String): List<TourItem>    // searchFestival2
  fun parseParkingAsset(tsv: String): List<ParkingLot>    // assets/parking/lots.tsv
  ```

규칙:
- 관광 공통
  - `response.header.resultCode`가 `"0000"`이 아니면 `ApiException("result")`
  - JSON이 아니면(HTML 오류 등) `ApiException("parse")`
  - `response.body.items`가 빈 문자열이면 빈 목록(정상 무자료)이다.
  - `items.item`은 배열 또는 객체 하나일 수 있다.
  - `mapy`→위도, `mapx`→경도(문자열). 좌표가 숫자가 아니거나 0이면 그 행은 깨진 행이다.
  - 행이 하나 이상인데 전부 깨졌으면 `ApiException("parse")`. 일부만 깨졌으면 그 행만 건너뛴다.
- `parseTourPlaces`
  - `contenttypeid` 12→ATTRACTION, 14→CULTURE, 28→LEISURE
  - 다른 종류(식당 39 등)는 깨진 행이 아니라 대상 아님으로 조용히 뺀다.
  - `start`·`end`는 null
- `parseTourFestivals`
  - 모든 행을 FESTIVAL로 본다.
  - `eventstartdate`·`eventenddate`(yyyyMMdd)가 없거나 형식이 틀리면 깨진 행
- id = `contentid`, title = `title`
- `parseParkingAsset`
  - 줄마다 탭으로 나뉜 6칸: `id`, `name`, `lat`, `lon`, `basicMinutes`, `basicFeeWon`
  - 빈 칸은 null이다.
  - `available`과 `availableProduced`는 null이다.
  - 첫 줄이 `#`로 시작하면 머리말로 건너뛴다. 칸 수가 틀린 줄은 건너뛴다.

- [ ] **Step 1: 실패하는 테스트**

```kotlin
@Test fun `places keep only attractions culture and leisure`() {
    val items = parseTourPlaces(fixture("tour-seoul-location-all.json"))
    assertTrue(items.all { it.kind in setOf(TourKind.ATTRACTION, TourKind.CULTURE, TourKind.LEISURE) })
    assertTrue(items.any { it.title == "경성 부민관 폭탄 의거지" && it.kind == TourKind.ATTRACTION })
}
@Test fun `festival has dates and position`() {
    val seoulSquare = parseTourFestivals(fixture("tour-seoul-ldong-today.json")).single { it.title == "문화가 흐르는 서울광장" }
    assertEquals(LocalDate.of(2026, 5, 6), seoulSquare.start); assertEquals(LocalDate.of(2026, 12, 31), seoulSquare.end)
    assertEquals(37.5654, seoulSquare.point.lat, 0.001); assertEquals(126.9788, seoulSquare.point.lon, 0.001)
}
@Test fun `empty items string is no data`() { /* {"response":{"header":{"resultCode":"0000"},"body":{"items":"","totalCount":0}}} → emptyList */ }
@Test fun `single item object is accepted`() { /* items.item = {...} 객체 하나 → 1개 */ }
@Test fun `error result throws`() { /* resultCode "0022" → ApiException("result") */ }
@Test fun `html body throws`() { /* "<html>..." → ApiException("parse") */ }
@Test fun `scene e sentence from recorded samples`() {
    // 장면 E 기대 문장 고정(2026-10-29 기준). Task 9의 시연 확인과 같은 값이다.
    val dest = Destination("37.56700,126.97800", "서울시청 주변", GeoPoint(37.567, 126.978), 3500.0, 720)
    val items = parseTourPlaces(fixture("tour-seoul-location-all.json")) + parseTourFestivals(fixture("tour-seoul-ldong-today.json"))
    val day = LocalDate.of(2026, 10, 29)
    assertEquals("목적지 근처에서 12월 31일까지 문화가 흐르는 서울광장이 열려요. 목적지에서 200m예요. 서울 왕궁수문장 교대의식도 열려요. " +
        "가까운 명소는 경성 부민관 폭탄 의거지, 대한성공회 서울주교좌성당, 덕수궁 대한문이에요.",
        TravelAdvisor().pick(dest, items, day, day)!!.second.text)
}
@Test fun `parking asset rows`() {
    val lots = parseParkingAsset("#id\tname\tlat\tlon\tmin\twon\n04540-11291-00001-00-1\t동국제강\t37.5672159\t126.981303\t30\t4200\nX\t무료 주차장\t37.5\t127.0\t\t0\nbroken line\n")
    assertEquals(listOf(ParkingLot("04540-11291-00001-00-1", "동국제강", GeoPoint(37.5672159, 126.981303), null, null, 30, 4200),
        ParkingLot("X", "무료 주차장", GeoPoint(37.5, 127.0), null, null, null, 0)), lots)
}
```

- [ ] **Step 2: 실패 확인** — `.\gradlew.bat :core:test`
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `.\gradlew.bat :core:test`(기존 116개 포함)
- [ ] **Step 5: 커밋** — `[T-27] 관광 응답과 주차 자산 해석`

---

### Task 6: 실시간 연결 — 목적지, 관광 호출, 주차 자산

**Files:**
- Modify: `app/src/main/kotlin/com/roadahead/car/PleosNavi.kt`, `feed/Feed.kt`, `feed/LiveFeed.kt`, `feed/Apis.kt`, `feed/ReplayFeed.kt`, `RoadAheadService.kt`
- Create: `app/src/main/assets/parking/lots.tsv`(Task 7 결과가 오기 전에는 머리말 한 줄만 둔다)
- Test: `app/src/test/…` 기존 유지. 새 순수 함수가 생기면 그 테스트 하나

**Interfaces:**
- Consumes: Task 5 파서, 엔진 `parkingRequest`·`tourRequest`·`onParkingLots`·`onTourItems`
- Produces:
  ```kotlin
  // Feed
  val destination: StateFlow<Destination?>
  val parked: StateFlow<Boolean>
  fun requestParking(dest: Destination)
  fun requestTour(dest: Destination, arrival: LocalDate)
  // DataEvent
  data class ParkingLots(val destKey: String, val lots: List<ParkingLot>) : DataEvent
  data class TourItems(val destKey: String, val items: List<TourItem>) : DataEvent
  ```

방식:
- **PleosNavi**
  - `onRouteStarted`가 오거나 `navMode`가 ROUTE가 되면 `getDestinationInfo()`를 부른다. ROUTE인 동안 5초마다 다시 부른다(자기 코루틴).
  - `onDestinationInfo(info)` 처리
    - `latitude`·`longitude`가 0이거나 `name`이 비면 `destination = null`
    - 그 밖에는 `Destination(key = "%.5f,%.5f".format(Locale.ROOT, lat, lon), name, GeoPoint(lat, lon), remainDistance.toDouble(), remainTime)`
  - `onRouteCancelled`이면 null로 둔다.
  - `onDestinationChanged`는 이 AVD에서 오지 않았으므로 의지하지 않는다(T-23).
- **LiveFeed**
  - `destination` = navi의 값, `parked` = `CarSignals.parked`
  - `requestParking(dest)`
    - 처음 한 번 `assets/parking/lots.tsv`를 읽어 메모리에 둔다.
    - 목적지에서 500 m 안의 주차장만 골라 `DataEvent.ParkingLots(dest.key, 그 목록)`을 낸다. 네트워크는 쓰지 않는다(빈자리는 Task 8).
  - `requestTour(dest, arrival)`: 공공데이터포털 키(`BuildConfig.SIGNAL_KEY`, 같은 일반 키)로 2회 호출한다.
    - 공통: `serviceKey`, `MobileOS=AND`, `MobileApp=RoadAhead`, `_type=json`, `pageNo=1`
    - `locationBasedList2`: `mapX`=경도·`mapY`=위도(소수 셋째 자리 반올림), `radius=3000`, `arrange=E`, `numOfRows=300`
    - `searchFestival2`: `eventStartDate`=`eventEndDate`=도착일 `yyyyMMdd`, `numOfRows=1000`, 지역 인자 없음
    - 둘 다 성공하면 `DataEvent.TourItems(dest.key, 명소 + 행사)`를 낸다. 하나라도 실패하면 클래스 이름만 로그로 남기고 아무것도 내지 않는다.
    - URL 로그의 키는 `***`로 가린다(기존 `Apis.kt` 방식).
- **ReplayFeed**: 이번 Task에서는 컴파일만 되게 둔다. `destination`과 `parked`는 기존 값 그대로(null/false)이고 `request*`는 아무것도 하지 않는다. 시연 사건은 Task 9에서 만든다.
- **서비스 루프**
  - `Inputs.destination = feed.destination.value`
  - `Inputs.parked = feed.parked.value`(지금의 `signals.parked` 대신)
  - `engine.parkingRequest(dest, navMode)?.let(feed::requestParking)`
  - `engine.tourRequest(dest, travelMode)?.let { feed.requestTour(it, 도착일) }`
    - 도착일 = `Instant.now() + remainTimeSec`의 서울 날짜. 엔진과 같은 식이다.
  - 새 `DataEvent` 두 개를 엔진의 `onParkingLots`·`onTourItems`로 넘긴다.

- [ ] **Step 1: 구현** — `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 성공
- [ ] **Step 2: 에뮬레이터 확인**(소리 없음, `logOnlySpeech`)
  - 설정에서 여행 모드를 켜고 실시간 모드로 둔다.
  - 내비 검색으로 "서울시청"을 목적지로 정한다.
  - logcat에서 확인할 것
    - 목적지 키
    - 관광 호출 2회 성공
    - `speak=목적지 근처에서 …` 한 번
    - P에서 여행 목록 카드(캡처 `docs/demo/overlay-travel-live.png`)
  - 관광 호출은 이 확인에서 10회를 넘기지 않는다.
  - 끝나면 경로를 취소하고 P/0으로 되돌린 뒤 앱을 force-stop하고 다른 앱 위에 표시를 끈다.
- [ ] **Step 3: 커밋** — `[T-27] 목적지·관광 실시간 연결과 주차 자산 읽기`

---

### Task 7: TS 주차 자산 만들기 도구

**Files:**
- Create: `tools/make_parking_asset.py`
- Output: `app/src/main/assets/parking/lots.tsv`

방식:
- 입력: T-23이 내려받은 전국 시설 원본 폴더(`spikes/parking-travel/.gradle/raw/`, 인자 `--raw`)
- 운영정보: 전국 `PrkOprInfo_v1`(`numOfRows=100000`, 페이지 끝까지)를 같은 폴더에 내려받는다. 이미 있으면 다시 받지 않는다.
  - 키는 루트 `api`의 `신호등` 줄 또는 `ROAD_AHEAD_SIGNAL_KEY`에서 읽는다. 출력·로그에 넣지 않고 URL은 `***`로 가린다.
- 결합: `prk_center_id`로 시설과 운영을 맞춘다. 아래를 모두 만족하는 주차장만 남긴다.
  - 이름이 비지 않음
  - 좌표가 숫자이고 대한민국 범위 안(위도 33~39, 경도 124~132)
  - `parking_chrge_bs_time`이 양의 정수
  - `parking_chrge_bs_chrge`가 정수
  - 0원은 시설 `parkng_chrge_se == "1"`(무료)일 때만 남긴다. 빈 문자열 요금은 버린다.
- 같은 ID가 여러 번 나오면 첫 행을 쓴다.
- 출력: 머리말 `#id\tname\tlat\tlon\tbasicMinutes\tbasicFeeWon` 다음에 ID 순으로 적는다. 이름 안의 탭·줄바꿈은 공백으로 바꾼다.
- 끝에 건수와 파일 크기를 출력한다. 장면 D의 `04540-11291-00001-00-1 동국제강 30분 4,200원`이 들어 있는지 확인해 출력한다.
- `--self-test`: 작은 가짜 시설·운영 JSON으로 결합 규칙(빈 요금 제외, 0원 무료 조건, 범위 밖 좌표 제외)을 검사한다.

- [ ] **Step 1**: `py -3.12 tools/make_parking_asset.py --self-test` 통과
- [ ] **Step 2**: 실제 실행. 건수·크기·동국제강 포함을 HANDOFF에 적는다. 5MB를 넘으면 커밋하지 말고 ask로 묻는다.
- [ ] **Step 3: 커밋** — `[T-28] TS 주차장 이름·요금 자산`. 도구와 `lots.tsv`만 올린다. 원본은 올리지 않는다.

---

### Task 8: 서울 시영주차장 실시간 빈자리 (T-26 실측 뒤 Claude가 덧붙인다)

---

### Task 9: 시연 장면 E

**Files:**
- Modify: `app/src/main/kotlin/com/roadahead/feed/ReplayFeed.kt`, `ui/Settings.kt`(`AppMode.DEMO_E("시연 E")`), `RoadAheadService.kt`, `tools/make_demo.py`
- Create: `app/src/main/assets/demo/scene-e.jsonl`

방식:
- **ReplayFeed 새 사건**
  - `{"kind":"dest","name":…,"lat":…,"lon":…,"remainM":…,"remainSec":…}` → `destination`. 키 규칙은 Task 6과 같다. `{"kind":"dest","clear":true}`는 null이다.
  - `{"kind":"gear","parked":true|false}` → `parked`
  - `{"kind":"tour","places":"<본문>","festivals":"<본문>"}`
    - 기록해 둔 관광 응답이다.
    - `requestTour`가 불리면 저장해 둔 이 응답을 Task 5 파서로 읽어 `TourItems(dest.key, …)`를 낸다. 요청 → 응답 흐름을 그대로 탄다.
    - 요청이 사건보다 먼저 오면 사건이 올 때 낸다.
- **서비스**: `prefs.mode == DEMO_E`이면 여행 모드를 켠 것으로 본다.
- **scene-e.jsonl**(make_demo로 생성)
  - 0초: label `"시연 E · 여행 모드 · 서울시청 주변 행사·명소(2026-10-29 기록)"`, gear P, nav FREE
  - 2초: nav ROUTE, dest(서울시청 주변, 37.567, 126.978, 3500, 720), tour(표본 두 개의 `response` 본문)
  - 이후 30초 유지
- 기대
  - 2026-10-29에 재생하면 Task 5의 `scene e sentence from recorded samples` 문장이 그대로 나온다.
  - 다른 날에는 행사 기간에 따라 달라진다. "문화가 흐르는 서울광장"은 12월 31일까지라 그때까지는 첫 행사로 나온다.
  - ponytail 주석: 2027년 시연이면 행사 날짜를 재생일 기준으로 옮긴다.
- **make_demo.py**: 장면 E를 만들고, `--self-test`에 E의 사건 순서 검사를 추가한다.

- [ ] **Step 1**: `py -3.12 tools/make_demo.py --self-test`, `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 성공
- [ ] **Step 2: 에뮬레이터**
  - 시연 E를 `logOnlySpeech`로 실행해 `speak=` 문장과 P 상태의 여행 목록 카드를 확인한다.
  - 캡처는 `docs/demo/overlay-e-travel.png`에 둔다.
  - 끝나면 P/0, force-stop, 권한 끔으로 되돌린다.
- [ ] **Step 3: 커밋** — `[T-29] 시연 장면 E(여행 모드)`
