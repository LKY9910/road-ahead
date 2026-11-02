# 여행 모드(동행·날씨 맞춤 추천, 서울 빈자리) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 이 저장소에서는 Codex가 Orca 작업으로 받아 실행한다(AGENTS.md 작업 흐름).

**Goal:** 주차 안내와 목적지 추천을 여행 모드 하나로 묶고, 여행 모드를 켤 때 물어 기억한 동행(아이·어르신·반려동물)과 도착 시각 날씨에 맞춰 명소를 고르며, 서울 시영주차장 두 곳의 실시간 빈자리를 붙인다.

**Architecture:** 판단은 지금처럼 `:core`가 한다. 새 해석기(동반 범위, 편의시설, 단기예보, 서울 주차)와 `TravelAdvisor`의 동행·날씨 순위가 순수 함수로 들어간다. `:app`은 장소마다 상세를 부르는 순서를 `TourSource` 인터페이스 뒤의 `collectTour()`로 묶어, 실시간(`Apis`)과 시연 재생(기록 응답)이 같은 순서를 쓰게 한다.

**Tech Stack:** 기존 그대로(Kotlin 2.1.21, JDK 17, AGP 8.7.2, coroutines 1.10.2, serialization-json 1.8.1, JUnit 5.11.4). 새 라이브러리를 추가하지 않는다.

**Spec:** [docs/DESIGN.md](../DESIGN.md) 10·12절. 근거: [spikes/travel-companion.md](../spikes/travel-companion.md)(동행·날씨 자료), [spikes/seoul-parking.md](../spikes/seoul-parking.md)(T-26). 화면 모양: [design/travel-mode-ui.md](../design/travel-mode-ui.md)(T-32, GPT 안을 Claude가 검토한 뒤 확정).

## Global Constraints

- [road-ahead.md](road-ahead.md)와 [parking-travel.md](parking-travel.md)의 Global Constraints 전부(키 취급, 금지 표현, 시간 인자, 테스트 이름, 커밋 규칙, 소리 시험 허락, 오버레이 창 설정)
- 안내 문장은 이 계획과 DESIGN 10절에 적힌 글자 그대로 쓴다. 조사는 `subject()`, `copula()`, Task 1의 `topic()`으로만 붙인다.
- 기준값: 명소 3 km·최대 3개, 행사 5 km·최대 2개, 반려동물 상세 최대 5곳, 편의시설 상세 최대 10곳, 이유 항목당 최대 2개, 서울 빈자리 나이 0~600초, 주차 반경 500 m, 주차 알림 1,000 m 또는 180초, 강수확률 60 %, 무더위 33 ℃ 이상, 강추위 −5 ℃ 이하.
- **서울 키는 URL 경로에 들어간다.** 로그·예외 메시지·저장 파일에는 `http://openapi.seoul.go.kr:8088/***/json/GetParkingInfo/1/1000/`처럼 가린 형태만 남긴다.
- 상세를 확인하지 못한 곳을 "동반 가능", "유모차 대여" 등으로 말하지 않는다. 실패는 "정보 없음"이지 "없음"이 아니다.
- 동행 질문 음성도 다른 안내와 같이 `logOnlySpeech`(debug)를 따른다. 소리 나는 시험은 사용자 허락 없이 하지 않는다.
- 화면·카드의 보이는 모양은 `docs/design/travel-mode-ui.md`를 따른다. 거기 없는 시각 결정은 혼자 정하지 말고 HANDOFF로 묻는다(사용자 지시: 디자인은 GPT와 상의).

## Review Focus

1. **반려동물 동행인데 동반 범위를 확인 못 한 곳**: 상세 실패·빈 응답·알 수 없는 값인 곳이 "반려동물과 갈 수 있는 명소"에 들어가면 안 된다. → Task 3 `pet companions never include unconfirmed places`
2. **서울 값이 오래됐거나 앞뒤가 안 맞음**: 미연계, 점유 > 총면, 갱신 시각이 미래이거나 600초 넘음 → 빈자리를 말하지 않는다. → Task 4 `seoul availability requires fresh linked counts`
3. **문 닫은 시간**: 평일 19:36의 동국제강(09:00~19:00)을 고르면 안 된다. `2400`과 `0000~2400`을 바르게 다룬다. → Task 4 `closed seoul lot is not picked`, `all day lot is open at midnight`
4. **"턱이 없어"처럼 '없'이 들어간 긍정 문장**: 편의시설로 인정해야 하고, "장애인 주차장 없음"·"대여 불가"는 인정하면 안 된다. → Task 1 `negative facility text is ignored`
5. **서울 키가 로그에 남음**: 경로 안 키를 `maskedUrl()`이 못 가린다. → Task 5 `seoul url is masked in logs`

---

## File Structure

```
core/src/main/kotlin/com/roadahead/core/
  model/Models.kt              Companion, PetScope, Facility, TourData; TravelLine.reasons            (Task 1, 3)
  tour/CompanionParser.kt      parsePetScope(), parseFacilities()                                    (Task 1)
  text/Korean.kt               topic() 은/는                                                         (Task 1)
  forecast/Forecast.kt         kmaGrid(), forecastBase(), parseForecast(), badWeather(), arrivalWeather()  (Task 2)
  advice/TravelAdvisor.kt      petTargets(), facilityTargets(), pick(dest, TourData, …)               (Task 3)
  parking/SeoulParking.kt      SeoulLot, parseSeoulParking(), SEOUL_TO_TS, applySeoul()               (Task 4)
  advice/ParkingAdvisor.kt     lotLabel() "주차장" 붙이기                                             (Task 4)
  RoadAheadEngine.kt           parkingRequest(travelMode), onTourData(), announce()                  (Task 4)
app/
  build.gradle.kts             SEOUL_KEY                                                              (Task 5)
  src/main/res/xml/network_security_config.xml, AndroidManifest.xml   openapi.seoul.go.kr 평문 허용   (Task 5)
  src/main/kotlin/com/roadahead/feed/
    TourSource.kt              interface TourSource, collectTour()                                    (Task 5)
    Apis.kt                    반려동물·무장애·단기예보·서울 주차 호출                                (Task 5)
    Feed.kt, LiveFeed.kt       requestTour(dest, arrival, companions), DataEvent.TourData, 서울 병합  (Task 5)
    ReplayFeed.kt              기록 응답 TourSource, 서울 재생                                         (Task 7)
  src/main/kotlin/com/roadahead/ui/
    Settings.kt                Prefs.companions, 동행 표시·바꾸기                                     (Task 6)
    CompanionPicker.kt         동행 선택 화면, toggleChoice()                                         (Task 6)
    OverlayCard.kt             이유·날씨·빈자리 표시                                                  (Task 6)
  src/main/kotlin/com/roadahead/MainActivity.kt, RoadAheadService.kt   질문 흐름, ACTION_ASK_COMPANIONS (Task 6)
tools/record_travel.py         시연 E 원본 응답 기록(키 없음)                                          (Task 7)
tools/make_demo.py, app/src/main/assets/demo/scene-e.jsonl            시연 E 새 흐름                  (Task 7)
```

순서: Task 1→2→3→4(core, 한 작업자) → 5 → 6(T-32 확정 뒤) → 7. Task 7의 `record_travel.py`는 Task 1과 동시에 시작할 수 있다(파이썬, 앱 코드와 무관).

작업 번호(`docs/TASKS.md`): **T-33** = Task 1~4, **T-34** = Task 7 Step 1(기록), **T-35** = Task 5, **T-36** = Task 6, **T-37** = Task 7 Step 2~6.

---

### Task 1: 동행 모델과 상세 응답 해석

**Files:**
- Modify: `core/src/main/kotlin/com/roadahead/core/model/Models.kt`, `core/.../text/Korean.kt`
- Create: `core/.../tour/CompanionParser.kt`, `core/src/test/kotlin/com/roadahead/core/tour/CompanionParserTest.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/text/KoreanTest.kt`

**Interfaces:**
- Produces:
  - `enum class Companion { CHILD, ELDER, PET }`
  - `enum class PetScope(val label: String) { ALL("반려동물 동반"), PARTIAL("일부 구역 동반") }`
  - `enum class Facility(val label: String, val companion: Companion)` — 선언 순서가 이유 순서다(T-32): `STROLLER("유모차 대여", CHILD)`, `LACTATION("수유실", CHILD)`, `BABY_CHAIR("유아용 의자", CHILD)`, `STEP_FREE("턱 없는 입구", ELDER)`, `WHEELCHAIR("휠체어 대여", ELDER)`, `ELEVATOR("엘리베이터", ELDER)`, `ACCESSIBLE_TOILET("장애인 화장실", ELDER)`
  - `data class PetInfo(val scope: PetScope, val need: String?)` — `need`는 짧은 필수 조건(예: "목줄 착용")
  - `fun parsePetInfo(json: String): PetInfo?`
  - `fun parseFacilities(json: String): Set<Facility>`
  - `fun topic(word: String): String` — 받침 있으면 "은", 없거나 한글이 아니면 "는"

규칙:
- 두 해석기는 `TourParser.kt`와 같은 머리·`items` 처리를 쓴다. `resultCode != "0000"`·파싱 실패는 `ApiException`, `items: ""`는 `null`/빈 집합이다.
- `parsePetInfo`: `acmpyTypeCd`에 "전구역"이 있으면 `ALL`, "일부구역"이 있으면 `PARTIAL`, 그 밖은 `null`(결과 전체가 `null`). `need`는 `acmpyNeedMtr`를 다듬은 값이 비어 있지 않고 한 줄이며 10자 이하일 때만 그 글자 그대로, 아니면 `null`.
- `parseFacilities`: 칸 → 시설은 `stroller`→STROLLER, `lactationroom`→LACTATION, `babysparechair`→BABY_CHAIR, `wheelchair`→WHEELCHAIR, `elevator`→ELEVATOR, `restroom`→ACCESSIBLE_TOILET. 값이 비어 있지 않고 "없음"·"불가"가 없을 때만 인정한다. STEP_FREE는 `route` 또는 `exit`에 "턱이 없"이나 "경사로"가 있을 때만 인정한다.

- [ ] **Step 1: 실패하는 테스트.** 입력은 [samples.json](../spikes/travel-companion/samples.json)의 응답 본문을 테스트 리소스로 복사해 쓴다.
  - `facilities from barrier free detail`: `KorWithService2-detail-1605981`(덕수궁 대한문) → `setOf(STROLLER, LACTATION, WHEELCHAIR, STEP_FREE, ACCESSIBLE_TOILET)`. `-127015` → `setOf(ACCESSIBLE_TOILET)`. `with-missing` → `emptySet()`
  - `negative facility text is ignored`: `stroller="대여 불가"`, `parking="장애인 주차장 없음"`, `restroom="없음"`, `exit="주출입구에 턱이 있음"` → `emptySet()`. `route="출입구까지 경사로가 설치되어 있음"` → `setOf(STEP_FREE)`
  - `pet info from detail`: `KorPetTourService2-detail-264311`(명동) → `PetInfo(PARTIAL, "목줄 착용")`, `-2695730` → `PetInfo(ALL, null)`, `pet-missing` → `null`, `acmpyTypeCd="동반불가"` → `null`, `acmpyNeedMtr="목줄 착용
배변봉투 지참"`이나 11자 이상 → `need == null`
  - `error result throws`: `resultCode="0003"` → `ApiException`
  - `topic particle`: `topic("덕수궁 대한문") == "덕수궁 대한문은"`, `topic("익선동 한옥거리") == "익선동 한옥거리는"`, `topic("DDP") == "DDP는"`
- [ ] **Step 2:** `.\gradlew.bat :core:test --tests "*CompanionParserTest" --tests "*KoreanTest"` → 컴파일 실패
- [ ] **Step 3:** 위 Interfaces대로 구현
- [ ] **Step 4:** 같은 명령 → 통과, 이어서 `:core:test` 전체 통과
- [ ] **Step 5:** 커밋 `[T-33] 동행 모델과 반려동물·무장애 상세 해석`

---

### Task 2: 도착 시각 날씨

**Files:**
- Create: `core/.../forecast/Forecast.kt`, `core/src/test/kotlin/com/roadahead/core/forecast/ForecastTest.kt`

**Interfaces:**
- Produces:
  - `fun kmaGrid(point: GeoPoint): Pair<Int, Int>` — 기상청 격자(람베르트 정각원추). 상수 `RE=6371.00877`, `GRID=5.0`, `SLAT1=30.0`, `SLAT2=60.0`, `OLON=126.0`, `OLAT=38.0`, `XO=43`, `YO=136`
  - `fun forecastBase(nowKst: LocalDateTime): LocalDateTime` — 02·05·08·11·14·17·20·23시 중 `nowKst`보다 10분 이상 앞선 가장 최근 정시
  - `data class HourForecast(val pty: Int?, val pop: Int?, val tmp: Double?)`
  - `fun parseForecast(json: String): Map<LocalDateTime, HourForecast>` — `resultCode != "00"`이면 `ApiException`
  - `enum class BadWeather(val word: String) { RAIN("비"), RAIN_SNOW("비나 눈"), SNOW("눈"), SHOWER("소나기"), HEAT("무더위"), COLD("강추위") }`
  - `fun badWeather(hour: HourForecast?): BadWeather?` — `PTY` 1→RAIN, 2→RAIN_SNOW, 3→SNOW, 4→SHOWER; 그다음 `POP ≥ 60`→RAIN; 그다음 `TMP ≥ 33`→HEAT, `TMP ≤ −5`→COLD; 아니면 null
  - `fun arrivalWeather(forecast: Map<LocalDateTime, HourForecast>, arrival: LocalDateTime): BadWeather?` — `arrival`을 정시로 내린 값으로 찾는다

- [ ] **Step 1: 실패하는 테스트**
  - `grid for seoul city hall`: `kmaGrid(GeoPoint(37.5663, 126.9779)) == 60 to 127`. 기상청 격자 위경도 표에서 한 곳을 더 골라 같은 식으로 넣는다(출처를 테스트 주석에).
  - `forecast base time`: `19:36 → 17:00`, `17:09 → 14:00`, `17:10 → 17:00`, `2026-10-30T02:05 → 2026-10-29T23:00`
  - `rain at arrival from recorded forecast`: [forecast-20261029-1700.json](../spikes/travel-companion/forecast-20261029-1700.json)의 `response` → `arrivalWeather(map, 2026-10-29T19:40) == RAIN`, 응답에 없는 시각 → `null`
  - `bad weather thresholds`: `(0, 60, 20.0) → RAIN`, `(0, 59, 32.9) → null`, `(0, 10, 33.0) → HEAT`, `(0, 10, -5.0) → COLD`, `(3, 90, -6.0) → SNOW`, `null → null`
- [ ] **Step 2:** `.\gradlew.bat :core:test --tests "*ForecastTest"` → 실패
- [ ] **Step 3:** 구현
- [ ] **Step 4:** 통과, `:core:test` 전체 통과
- [ ] **Step 5:** 커밋 `[T-33] 도착 시각 단기예보 해석`

---

### Task 3: 동행·날씨 맞춤 추천

**Files:**
- Modify: `core/.../model/Models.kt`, `core/.../advice/TravelAdvisor.kt`
- Test: `core/src/test/kotlin/com/roadahead/core/advice/TravelAdvisorTest.kt`(기존 테스트는 새 시그니처로 옮기고 기대 문장은 그대로)

**Interfaces:**
- Consumes: Task 1의 `Companion`, `PetScope`, `PetInfo`, `Facility`, `topic()`; Task 2의 `BadWeather`
- Produces:
  - `data class TourData(val places: List<TourItem>, val festivals: List<TourItem>, val companions: Set<Companion>, val petPlaces: List<TourItem>? = null, val petInfo: Map<String, PetInfo> = emptyMap(), val facilities: Map<String, Set<Facility>> = emptyMap(), val weather: BadWeather? = null)` — `petPlaces`는 반려동물 동행일 때만 채우고, 목록 조회가 실패하면 `null`이다.
  - `TravelLine`에 `val reasons: List<String> = emptyList()` 추가(라벨 문자열, 최대 2개)
  - `fun TravelAdvisor.petTargets(dest: Destination, petPlaces: List<TourItem>): List<String>` — 관광지·문화시설·레포츠, 3 km 안, 가까운 순 5개의 id
  - `fun TravelAdvisor.facilityTargets(dest: Destination, places: List<TourItem>, companions: Set<Companion>, weather: BadWeather?, confirmedPet: List<TourItem>?): List<String>` — `CHILD`·`ELDER`가 없으면 빈 목록. `PET`이면 `confirmedPet`(최대 5). 아니면 3 km 안 명소 중 날씨가 궂으면 문화시설을 가까운 순으로 먼저, 나머지를 가까운 순으로 채워 10개
  - `data class TravelResult(val lines: List<TravelLine>, val advice: Advice, val weatherNote: String?)` — `weatherNote`는 카드 머리줄 아래 한 줄(T-32): `"도착 때 ${weather.word} 예보 · 실내 명소 우선"`, 날씨 문장과 같은 조건일 때만
  - `fun TravelAdvisor.pick(dest: Destination, data: TourData, arrival: LocalDate, today: LocalDate): TravelResult?`
  - `Card.Travel`에 `val weatherNote: String? = null` 추가(엔진이 `TravelResult.weatherNote`를 넘긴다)

순위(명소):
1. 후보: `PET`이면 `petPlaces` 중 관광지·문화시설·레포츠, 3 km 안, `petInfo`에 값이 있는 곳만(`petPlaces == null`이면 후보 없음). 아니면 `places` 중 같은 조건.
2. 정렬 키: 맞는 동행 수(선택한 `CHILD`·`ELDER` 중 그 동행의 `Facility`가 하나라도 있는 수) 내림차순 → 날씨가 궂으면 문화시설 먼저 → 거리.
3. 위에서 3개.
4. 이유(최대 2개, 같은 문구 반복 없음, T-32 순서):
   - `PET`이면 먼저 동반 범위 라벨, 그다음 `need`가 있으면 `need`, 자리가 남으면 아래 사람 동행 이유.
   - `CHILD`와 `ELDER`가 함께면 아이 시설 첫 번째 하나 → 어르신 시설 첫 번째 하나. 한쪽에만 있으면 그쪽에서 둘까지.
   - 하나만이면 그 동행의 시설을 `Facility` 선언 순서로 둘.

문장 순서: 행사(기존 그대로) → 날씨 → 명소 → 이유.
- 날씨: `weather != null`이고 고른 명소에 문화시설이 하나라도 있을 때만 `"도착할 때쯤 ${weather.word} 예보가 있어 실내 명소를 먼저 골랐어요."`
- 명소: `PET`이면 `"반려동물과 갈 수 있는 가까운 명소는 ${copula(이름들)}."`, 아니면 기존 `"가까운 명소는 ${copula(이름들)}."`
- 이유: 첫 명소의 이유 중 `PetScope.ALL` 라벨과 `need`가 아닌 첫 이유를 문장으로: STROLLER "유모차를 빌릴 수 있어요", LACTATION "수유실이 있어요", BABY_CHAIR "유아용 의자가 있어요", WHEELCHAIR "휠체어를 빌릴 수 있어요", STEP_FREE "입구에 턱이 없어요", ELEVATOR "엘리베이터가 있어요", ACCESSIBLE_TOILET "장애인 화장실이 있어요", PARTIAL "일부 구역만 반려동물과 들어갈 수 있어요" → `"${topic(이름)} ${문장}."`

- [ ] **Step 1: 실패하는 테스트.** 장소는 서울시청 근처 실제 이름과 거리를 쓴다(목적지 `GeoPoint(37.5663, 126.9779)`).
  - `child prefers stroller place`: 경성 부민관 폭탄 의거지 171 m(시설 없음), 덕수궁 대한문 176 m(`STROLLER, LACTATION, WHEELCHAIR, STEP_FREE, ACCESSIBLE_TOILET`), 대한성공회 서울주교좌성당 207 m(`ACCESSIBLE_TOILET`), 동행 `{CHILD}` → 문장 `"가까운 명소는 덕수궁 대한문, 경성 부민관 폭탄 의거지, 대한성공회 서울주교좌성당이에요. 덕수궁 대한문은 유모차를 빌릴 수 있어요."`, 대한문 `reasons == listOf("유모차 대여", "수유실")`
  - `elder prefers accessible places`: 같은 자료, `{ELDER}` → 순서 대한문, 성공회, 부민관. 대한문 이유 `["턱 없는 입구", "휠체어 대여"]`, 성공회 `["장애인 화장실"]`, 문장 끝 `"덕수궁 대한문은 입구에 턱이 없어요."`
  - `child and elder count matches`: `{CHILD, ELDER}` → 대한문(2) 먼저, 성공회(1), 부민관(0). 대한문 이유 `["유모차 대여", "턱 없는 입구"]`, 성공회 `["장애인 화장실"]`
  - `pet companions never include unconfirmed places`: `petPlaces` = 명동 775 m(`PetInfo(PARTIAL, "목줄 착용")`), 인사동 1,133 m(`PetInfo(PARTIAL, null)`), 익선동 한옥거리 1,368 m(`PetInfo(PARTIAL, null)`), 남산순환나들길 1,486 m(`petInfo`에 없음) → `"반려동물과 갈 수 있는 가까운 명소는 명동, 인사동, 익선동 한옥거리예요. 명동은 일부 구역만 반려동물과 들어갈 수 있어요."`, 명동 이유 `["일부 구역 동반", "목줄 착용"]`, 인사동 `["일부 구역 동반"]`, 남산순환나들길은 목록에 없다
  - `pet and child reasons`: `{PET, CHILD}`, 명동에 `STROLLER`도 있음 → 명동 이유 `["일부 구역 동반", "목줄 착용"]`(사람 이유는 자리가 없다), 인사동에 `STROLLER` → `["일부 구역 동반", "유모차 대여"]`
  - `pet list failure speaks no places`: `{PET}`, `petPlaces = null`, 행사 1개 → 행사 문장만, 명소 줄 없음
  - `rain puts indoor first`: 동행 없음, `weather = RAIN`, 공원(관광지) 300 m, 박물관(문화시설) 900 m, 미술관(문화시설) 1,400 m, 광장(관광지) 500 m → 순서 박물관, 미술관, 공원. 문장 `"도착할 때쯤 비 예보가 있어 실내 명소를 먼저 골랐어요. 가까운 명소는 박물관, 미술관, 공원이에요."`, `weatherNote == "도착 때 비 예보 · 실내 명소 우선"`
  - `bad weather without indoor adds no weather sentence`: 관광지만 → 날씨 문장 없음, `weatherNote == null`
  - `facility targets`: 동행 `{}` → 빈 목록. `{CHILD}`, `RAIN`, 문화시설 3곳·관광지 12곳 → 앞 3개가 문화시설, 총 10개. `{PET, CHILD}` → `confirmedPet`의 id
  - `pet targets take five closest of allowed kinds`: 축제(`FESTIVAL`)·3 km 밖 제외, 남은 7곳 중 가까운 5개
- [ ] **Step 2:** `.\gradlew.bat :core:test --tests "*TravelAdvisorTest"` → 실패
- [ ] **Step 3:** 구현. `RoadAheadEngine`은 이 Task에서 컴파일만 맞춘다(`onTourItems`를 `TourData(items 나눔, festivals, companions = emptySet())`로 감싸는 임시 연결). Task 4에서 바꾼다.
- [ ] **Step 4:** 통과, `:core:test` 전체 통과
- [ ] **Step 5:** 커밋 `[T-33] 동행·날씨 맞춤 명소 추천`

---

### Task 4: 서울 빈자리, 여행 모드 안의 주차, 질문 음성

**Files:**
- Create: `core/.../parking/SeoulParking.kt`, `core/src/test/kotlin/com/roadahead/core/parking/SeoulParkingTest.kt`
- Modify: `core/.../advice/ParkingAdvisor.kt`, `core/.../RoadAheadEngine.kt`
- Test: `ParkingAdvisorTest.kt`, `core/src/test/kotlin/com/roadahead/core/RoadAheadEngineTest.kt`

**Interfaces:**
- Consumes: Task 3 `TourData`
- Produces:
  - `data class SeoulLot(val code: String, val totalSpaces: Int?, val occupied: Int?, val linked: Boolean, val updated: LocalDateTime?, val busOnly: Boolean, val weekdayHours: IntRange?, val weekendHours: IntRange?, val saturdayFree: Boolean, val basicMinutes: Int?, val basicFeeWon: Int?)` — 시간 범위는 하루 중 분(0~1440)이고 `first`는 여는 시각, `last`는 닫는 시각이다. 열려 있음은 `first ≤ t < last`(끝 ≤ 시작이면 `t ≥ first || t < last`). 칸 대응: `PKLT_CD`, `TPKCT`, `NOW_PRK_VHCL_CNT`, `PRK_STTS_YN == "1"`, `NOW_PRK_VHCL_UPDT_TM`, `OPER_SE == "4"`, `WD_OPER_BGNG_TM`~`WD_OPER_END_TM`, `WE_OPER_BGNG_TM`~`WE_OPER_END_TM`, `SAT_CHGD_FREE_SE == "N"`, `BSC_PRK_HR`, `BSC_PRK_CRG`
  - `fun parseSeoulParking(body: String): List<SeoulLot>` — JSON `GetParkingInfo.RESULT.CODE == "INFO-000"` → 행, `INFO-200` → 빈 목록, 그 밖(XML 포함) → `ApiException`. `13.0`처럼 오는 수는 정수일 때만 `Int`
  - `val SEOUL_TO_TS: Map<String, String> = mapOf("1425816" to "04540-11291-00001-00-1", "171721" to "03172-11291-00001-00-1")`
  - `fun applySeoul(lots: List<ParkingLot>, seoul: List<SeoulLot>, now: Instant, zone: ZoneId = ZoneId.of("Asia/Seoul")): List<ParkingLot>`
  - `fun lotLabel(name: String): String` — 이름에 "주차"가 없으면 `"$name 주차장"`
  - `fun koFeeShort(minutes: Int?, won: Int?): String?` (`text/Korean.kt`) — 카드용 짧은 요금: `0원 → "무료"`, 둘 다 있으면 `"{분}분에 {원}원"`(천 단위 쉼표), 아니면 `null`. `ParkingCard.feeText`는 이것을 쓴다(T-32: "목적지에서 200m · 5분에 430원"). 음성은 기존 `koFee()` 그대로
  - `RoadAheadEngine.parkingRequest(dest: Destination?, navMode: NavMode, travelMode: Boolean): Destination?`
  - `RoadAheadEngine.onTourData(destKey: String, data: TourData)` (`onTourItems` 대신)
  - `RoadAheadEngine.announce(advice: Advice)` — 한 번 말할 안내를 넣는다. 다음 틱부터 다른 안내와 같이 `Announcer`를 거치고(내비가 말하는 중이면 기다림), 말한 뒤 지운다
  - `RoadAheadEngine.withdraw(key: String)` — 아직 말하지 않은 `announce` 안내를 지운다(동행 선택 화면이 닫히면 질문을 취소, T-32)

`applySeoul` 규칙(DESIGN 10.3):
- `SEOUL_TO_TS`로 짝이 있는 TS 주차장에만 적용한다. 서울 행이 없으면 그 주차장은 그대로 둔다.
- 버스전용이면 뺀다. 지금 시각(KST)이 운영 시간 밖이면 뺀다. 월~금은 `weekdayHours`, 토·일은 `weekendHours`. `2400`은 1440분, `0000~2400`은 하루 종일, 끝 ≤ 시작이면 자정을 넘는 범위다. 시간이 `null`이면 연 것으로 본다.
- 빈자리 = `totalSpaces − occupied`는 `linked`, 둘 다 값 있음, 총면 > 0, 0 ≤ 점유 ≤ 총면, `updated`가 있고 `now − updated`가 0~600초일 때만. 이때 `availableProduced = updated`(KST). 아니면 `available = null`, `availableProduced = null`.
- 요금: 토요일이고 `saturdayFree`면 `basicFeeWon = 0`. 아니면 서울 `basicMinutes`·`basicFeeWon`이 둘 다 있으면 그 쌍, 없으면 TS 값.

- [ ] **Step 1: 실패하는 테스트.** 서울 입력은 [seoul-parking/samples.json](../spikes/seoul-parking/samples.json)의 `normalExcerpt.response`(18:57:23 KST, 세종로·남대문 화물·동국제강)를 리소스로 쓴다.
  - `parse seoul sample`: 3행. 동국제강 `totalSpaces 13, occupied 3, linked true, weekdayHours 540..1140, basicMinutes 5, basicFeeWon 700`. 남대문 화물 `linked false, updated null`
  - `seoul error bodies`: `error-invalid-key`(XML `INFO-100`) → `ApiException`, `no-data`(`INFO-200`) → 빈 목록
  - `seoul availability requires fresh linked counts`: 동국제강을 `copy(weekdayHours = null, weekendHours = null)`로 운영 시간 검사와 분리한 뒤(19:00 종료와 겹치지 않게, T-33 질문으로 수정), now = `2026-10-29T18:57:23+09:00` → `available 10`. 갱신 시각 + 600초 → 값 있음, + 601초 → `null`. `linked=false` → `null`. `occupied=14` → `null`. `updated = now + 5초` → `null`
  - `closed seoul lot is not picked`: 2026-10-29(목) 19:36 KST → 동국제강이 결과에 없다. 18:59 → 있다
  - `all day lot is open at midnight`: 세종로(`0000~2400`) 00:30 → 있다
  - `saturday free fee`: 2026-10-31(토) 12:00, `saturdayFree=true` → `basicFeeWon 0` → ParkingAdvisor 문장에 "무료예요"
  - `unmapped and bus rows are ignored`: 대응표 밖 코드는 아무 TS 주차장도 바꾸지 않는다. 버스전용 행이 붙은 TS 주차장은 빠진다
  - `card fee is short`: `koFeeShort(5, 430) == "5분에 430원"`, `koFeeShort(30, 4200) == "30분에 4,200원"`, `koFeeShort(null, 0) == "무료"`, `koFeeShort(null, null) == null`. 세종로 주차 카드 `feeText == "5분에 430원"`
  - `lot label adds parking word`: `"동국제강" → "동국제강 주차장"`, `"시청 공영주차장"` 그대로. 기존 ParkingAdvisor 테스트 기대 문장을 이 규칙으로 고친다(예: `"목적지 근처 동국제강 주차장에 빈자리 10면이 있어요. 5분에 700원이에요."`)
  - `parking needs travel mode`: `travelMode=false`면 `parkingRequest`가 `null`이고 틱에 주차 카드·안내가 없다
  - `announce waits for navi and speaks once`: `announce(Advice("companions:ask:1", NORMAL, "누구와 함께 가세요? 화면에서 골라 주세요."))` → 내비가 말하는 틱에는 `speak == null`, 다음 조용한 틱에 그 안내, 그다음 틱 `null`
  - `withdrawn question is not spoken`: `announce` 뒤 내비가 말하는 동안 `withdraw("companions:ask:1")` → 이후 틱에서 말하지 않는다
- [ ] **Step 2:** `.\gradlew.bat :core:test` → 실패
- [ ] **Step 3:** 구현. 엔진의 임시 `onTourItems`를 지운다.
- [ ] **Step 4:** `:core:test` 전체 통과
- [ ] **Step 5:** 커밋 `[T-33] 서울 실시간 빈자리와 여행 모드 안의 주차`

---

### Task 5: 실시간 연결

**Files:**
- Create: `app/src/main/kotlin/com/roadahead/feed/TourSource.kt`, `app/src/main/res/xml/network_security_config.xml`, `app/src/test/kotlin/com/roadahead/feed/TourSourceTest.kt`
- Modify: `app/build.gradle.kts`, `AndroidManifest.xml`, `feed/Apis.kt`, `feed/Feed.kt`, `feed/LiveFeed.kt`, `feed/ReplayFeed.kt`·`ReplayFeedTest.kt`(컴파일만), `RoadAheadService.kt`
- Test: `app/src/test/kotlin/com/roadahead/feed/ApisTest.kt`

**Interfaces:**
- Consumes: Task 1~4 전부
- Produces:
  - `interface TourSource { suspend fun places(point: GeoPoint): List<TourItem>; suspend fun festivals(date: LocalDate): List<TourItem>; suspend fun petPlaces(point: GeoPoint): List<TourItem>; suspend fun petInfo(id: String): PetInfo?; suspend fun facilities(id: String): Set<Facility>; suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? }`
  - `suspend fun collectTour(source: TourSource, dest: Destination, arrival: LocalDateTime, companions: Set<Companion>, advisor: TravelAdvisor = TravelAdvisor()): TourData`
  - `Feed.requestTour(dest: Destination, arrival: LocalDateTime, companions: Set<Companion>)`, `DataEvent.TourData(val destKey: String, val data: com.roadahead.core.model.TourData)`(`DataEvent.TourItems` 대신)
  - `internal fun seoulUrl(key: String): String`, `internal fun maskedSeoulUrl(url: String): String`
  - `BuildConfig.SEOUL_KEY` ← `apiKey("ROAD_AHEAD_SEOUL_KEY", "서울")`

`collectTour` 순서와 실패:
1. 동시에: `places`, `festivals(arrival.toLocalDate())`, `weather`, `PET`이면 `petPlaces`. 각각 실패하면 빈 목록 / 빈 목록 / `null` / `null`.
2. `PET`이고 `petPlaces != null`이면 `petTargets`마다 `petInfo`(실패는 없음으로).
3. `facilityTargets(…, confirmedPet = 동반 범위가 확인된 반려동물 장소)`마다 `facilities`(실패는 없음으로).
4. `TourData`로 묶는다.

`Apis` 호출(공공데이터포털 키 `SIGNAL_KEY`, 공통 인자는 `tourCommonParams()`):
- `KorPetTourService2/locationBasedList2`: `tourPlaceParams(point)`와 같은 좌표·반경·정렬, `numOfRows=100`. 해석은 `parseTourPlaces`(관광지·문화시설·레포츠만 남는다)
- `KorPetTourService2/detailPetTour2`, `KorWithService2/detailWithTour2`: `contentId`
- 단기예보 `https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst`: `pageNo=1`, `numOfRows=300`, `dataType=JSON`, `base_date`·`base_time` = `forecastBase(지금 KST)`, `nx`·`ny` = `kmaGrid(point)`
- 서울: `seoulUrl(BuildConfig.SEOUL_KEY)` = `http://openapi.seoul.go.kr:8088/{key}/json/GetParkingInfo/1/1000/`. 로그는 `maskedSeoulUrl`. 키가 비면 부르지 않는다.

`LiveFeed`:
- `requestTour`: `collectTour(LiveTourSource(apis), …)` → `DataEvent.TourData`. 로그에 호출 수와 동행·날씨를 남긴다(키·URL 없이).
- `requestParking`: 500 m 안 TS 주차장 중 `SEOUL_TO_TS`의 값이 있고 `SEOUL_KEY`가 있을 때만 서울을 1회 부르고 `applySeoul(nearby, seoul, Instant.now())`. 서울 실패면 TS 목록 그대로.
- `RoadAheadService`: `requestTour(dest, 지금 + remainTime의 KST LocalDateTime, prefs.companions ?: emptySet())`, `parkingRequest(dest, navMode, travelMode)`, `DataEvent.TourData → engine.onTourData`.

- [ ] **Step 1: 실패하는 테스트**
  - `seoul url is masked in logs`: `maskedSeoulUrl(seoulUrl("abc123")) == "http://openapi.seoul.go.kr:8088/***/json/GetParkingInfo/1/1000/"`, 결과에 `"abc123"`이 없다
  - `collect tour call counts`: 호출을 세는 가짜 `TourSource`. 동행 `{}` → places·festivals·weather 각 1, 상세 0. `{CHILD}` → 상세 `facilities` ≤ 10, `petPlaces` 0. `{PET, ELDER}` → `petPlaces` 1, `petInfo` ≤ 5, `facilities`는 확인된 곳 수만큼
  - `collect tour survives failures`: `petPlaces`가 예외 → `TourData.petPlaces == null`. `weather` 예외 → `null`. `facilities` 하나가 예외 → 그 id만 빠진다
  - `forecast params`: base·격자 인자가 위 규칙대로 들어간다(서울시청, 19:36 → `20261029`/`1700`/60/127)
- [ ] **Step 2:** `.\gradlew.bat :app:testDebugUnitTest` → 실패
- [ ] **Step 3:** 구현. `network_security_config.xml`은 `<domain-config cleartextTrafficPermitted="true"><domain includeSubdomains="false">openapi.seoul.go.kr</domain></domain-config>`만 둔다.
- [ ] **Step 4:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 통과
- [ ] **Step 5: 에뮬레이터 확인(소리 없음).** debug, `logOnlySpeech=true`, 여행 모드 켬, 동행은 시험용으로 설정값을 직접 넣는다(adb로 SharedPreferences 쓰기 또는 Task 6 전 임시 방법, 방법을 HANDOFF에 적는다). 내비에서 서울 목적지(세종문화회관)를 정해 `speak=`·호출 수 로그를 확인한다. 로그와 `logcat`에 키 문자열이 0개인지 확인한다.
- [ ] **Step 6:** 커밋 `[T-35] 동행·날씨 자료와 서울 빈자리 실시간 연결`

---

### Task 6: 동행 묻기 화면과 카드 표시

**T-32 디자인은 2026-10-29 Claude 검토로 확정했다.** 모양·치수·문구·넘침 규칙은 [design/travel-mode-ui.md](../design/travel-mode-ui.md) 1~3절을 그대로 따른다. 그 문서의 Claude 판단 요청 네 가지(완료 때 활성화, 긴 이름 자연 개행·880dp 상한, 주차 16dp 점, 반려동물 조건 먼저)는 모두 채택했다.

**Files:**
- Create: `app/src/main/kotlin/com/roadahead/ui/CompanionPicker.kt`, `app/src/test/kotlin/com/roadahead/ui/CompanionPickerTest.kt`
- Modify: `ui/Settings.kt`, `MainActivity.kt`, `RoadAheadService.kt`, `ui/OverlayCard.kt`

**Interfaces:**
- Consumes: Task 4 `RoadAheadEngine.announce`, Task 3 `TravelLine.reasons`, Task 2 `BadWeather`(카드에 보일지는 T-32)
- Produces:
  - `Prefs.companions: Set<Companion>?` — `null`은 한 번도 답하지 않음. 저장 키 `"companions"`(이름 문자열 집합), 빈 집합은 "동행 없음"
  - `enum class CompanionChoice { CHILD, ELDER, PET, NONE }`
  - `fun toggleChoice(selected: Set<CompanionChoice>, tapped: CompanionChoice): Set<CompanionChoice>` — NONE을 누르면 항상 `{NONE}`(이미 있어도 그대로), 다른 것을 누르면 NONE을 빼고 그것을 켜고 끈다
  - `fun Set<CompanionChoice>.toCompanions(): Set<Companion>`, `fun Set<Companion>?.toChoices(): Set<CompanionChoice>` — `null` → 빈 집합, 빈 집합 → `{NONE}`
  - `RoadAheadService.ACTION_ASK_COMPANIONS` — 받으면 `engine.announce(Advice("companions:ask:$n", Priority.NORMAL, "누구와 함께 가세요? 화면에서 골라 주세요."))`, `n`은 서비스 안에서 1씩 늘린다
  - `RoadAheadService.ACTION_WITHDRAW_COMPANIONS` — 받으면 마지막 질문 키로 `engine.withdraw`

흐름(DESIGN 10.1, T-32 1절):
- 설정의 여행 모드 스위치가 끔 상태에서 눌리면 **아직 `travelMode`를 바꾸지 않고** 선택 화면(켜기 진입)을 연다. 이때 한 번만 `ACTION_ASK_COMPANIONS`를 보낸다(재구성·회전으로 다시 보내지 않는다).
- 선택 완료(선택이 비어 있지 않을 때만)를 누르면 P인지 다시 확인하고, P면 `companions`와 `travelMode = true`를 함께 저장하고 설정으로 돌아간다. P가 아니면 저장하지 않는다. `savePrefs`의 재시작 조건에 `companions` 변경을 넣는다.
- 취소·뒤로·P 이탈이면 화면을 닫고 `ACTION_WITHDRAW_COMPANIONS`를 보낸다. 켜기 진입이면 꺼진 채, 바꾸기 진입이면 켜진 채로 두고 지난 답은 그대로다.
- 스위치를 끄면 바로 `travelMode = false`를 저장한다. 저장한 동행은 지우지 않는다.
- 설정: 켜져 있으면 지금 동행과 "바꾸기", 꺼져 있으면 "지난 동행"만(바꾸기 숨김). 바꾸기는 음성 없이 같은 화면을 연다.

- [ ] **Step 1: 실패하는 테스트** `CompanionPickerTest`
  - `none is exclusive`: `toggleChoice({CHILD, PET}, NONE) == {NONE}`, `toggleChoice({NONE}, NONE) == {NONE}`, `toggleChoice({NONE}, ELDER) == {ELDER}`, `toggleChoice({ELDER}, ELDER) == {}`
  - `stored companions round trip`: `null.toChoices() == {}`, `emptySet<Companion>().toChoices() == {NONE}`, `{NONE}.toCompanions() == {}`, `{CHILD, PET}.toCompanions() == {CHILD, PET}`
- [ ] **Step 2:** `:app:testDebugUnitTest` → 실패
- [ ] **Step 3:** 구현(화면·카드는 T-32 안 그대로)
- [ ] **Step 4:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 통과
- [ ] **Step 5: 에뮬레이터 확인(소리 없음, `logOnlySpeech=true`).** 각각 캡처를 `docs/demo/`에 남긴다.
  - 설정에서 여행 모드 스위치를 누르면 선택 화면이 뜨고 로그에 `speak=누구와 함께 가세요? 화면에서 골라 주세요.`가 1회, 완료 전에는 여행 모드가 꺼져 있음
  - 지난 답 미리 선택, NONE 단독 선택, 완료 전 기어 D → 화면 닫힘·모드 꺼짐·저장값 그대로, 바꾸기 진입에서 D → 모드 켜짐 유지
  - 실제 Android 글꼴로 T-32 예시(긴 이름 개행, 빈자리 781면 기준선, 날씨 줄)가 넘치지 않는지
  - 여행 목록 카드의 이유 줄, 주차 카드 빈자리(시연 E 또는 가짜 자료)
- [ ] **Step 6:** 커밋 `[T-36] 동행 묻기 화면과 카드의 이유·빈자리 표시`

---

### Task 7: 시연 E "여행 모드"

**Files:**
- Create: `tools/record_travel.py`, `tools/demo-src/travel/`(키 없는 기록)
- Modify: `tools/make_demo.py`, `app/src/main/kotlin/com/roadahead/feed/ReplayFeed.kt`, `app/src/main/assets/demo/scene-e.jsonl`
- Test: `app/src/test/kotlin/com/roadahead/feed/ReplayFeedTest.kt`, `py -3.12 tools/make_demo.py --self-test`

**기록(`record_travel.py`, Task 1과 동시에 시작 가능)**
- 목적지: TS 세종로 주차장 좌표(`37.5733652, 126.9736652`)에서 500 m 안의 명소. 세종문화회관을 우선 검토하고, 관광정보의 좌표로 거리를 계산해 보고서에 적는다.
- 기록할 것(키를 지운 응답 본문과 요청 인자, 조회 시각 KST):
  - `KorService2/locationBasedList2`(Task 5 인자), `searchFestival2`(기록 날짜)
  - `KorPetTourService2/locationBasedList2`, 그리고 관광지·문화시설·레포츠 3 km 안 **전부**의 `detailPetTour2`
  - `KorWithService2/detailWithTour2`: 3 km 안 명소의 가까운 순 10곳 ∪ 문화시설 가까운 순 10곳 ∪ 동반 범위가 확인된 반려동물 장소 전부. 그래서 시연에서 어떤 동행·날씨를 골라도 `facilityTargets`가 기록 안에 있다.
  - 서울 `GetParkingInfo/1/1000/` 1회
- 날씨는 새로 부르지 않고 [forecast-20261029-1700.json](../spikes/travel-companion/forecast-20261029-1700.json)(조회 2026-10-29 19:36 KST)을 쓴다. 세종문화회관이 같은 격자(60, 127)인지 `kmaGrid`와 같은 식으로 확인한다.
- 같은 응답을 두 번 받지 않는다. 다시 실행하면 이미 있는 기록을 덮어쓰지 않는다(T-23 도구와 같은 원칙). 호출 수를 보고한다.

**재생**
- 새 사건 `{"kind":"tour", …}`는 위 기록 전부를 담는다. `ReplayFeed`는 이것으로 `TourSource`를 만들고 `collectTour()`를 그대로 부른다. 기록에 없는 상세를 찾으면 예외로 실패시킨다(테스트에서 드러나게).
  - `weather(point, arrival)`: 기록 예보에서 `arrival − (재생 시작 − 2026-10-29T19:36 KST)`의 정시 값을 쓴다.
- 새 사건 `{"kind":"seoul", "body":…, "recordedAt":…}`: `requestParking`이 오면 TS 자산의 500 m 안 주차장에 `applySeoul(…, now = recordedAt)`을 적용해 운영 시간·토요일 무료·신선도를 **기록 시점** 기준으로 판단하고, 결과의 `availableProduced`만 `(재생 시각 − recordedAt)`만큼 옮긴다. 그래서 어느 요일에 재생해도 같은 문장이 나온다(T-37 질문, 2026-10-31).
- 장면 흐름(DESIGN 10.4): 0초 P·FREE → 2초 ROUTE와 목적지(남은 1,800 m·420초)와 `tour` → P 목록(최대 60초 전에 D로) → 32초 기어 D·속도 → 2초마다 남은 거리 줄임 → 1,000 m 이하에서 주차 안내 → 150 m에서 끝. 정확한 시각은 만드는 사람이 정하고 생성기 자체 검사에 넣는다.

- [ ] **Step 1 (T-34):** `record_travel.py` 실행, 기록과 호출 수를 HANDOFF에 보고, 커밋 `[T-34] 시연 E 여행 모드 원본 응답 기록`
- [ ] **Step 2: 실패하는 테스트** `ReplayFeedTest`
  - `travel scene works for every companion set`: 동행 `{}`, `{CHILD}`, `{ELDER}`, `{PET}`, `{CHILD, PET}` 각각 `TourData`가 나오고 기록 밖 상세를 찾지 않는다. 날씨는 `RAIN`
  - `travel scene parking uses seoul availability`: 1,000 m 이하에서 `"목적지 근처 세종로 주차장에 빈자리 {n}면이 있어요. 5분에 430원이에요."`({n}은 기록값), 외부 호출 0
  - 첫 문장 전체를 동행 `{}`와 `{PET}`에 대해 기록 결과로 고정해 적는다
- [ ] **Step 3:** 구현, `py -3.12 tools/make_demo.py`로 `scene-e.jsonl` 생성, `--self-test` 통과
- [ ] **Step 4:** `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :core:test` 통과
- [ ] **Step 5: 에뮬레이터(소리 없음).** 동행 `{}`와 `{CHILD, PET}`로 시연 E를 돌려 P 목록 카드와 D 주차 카드를 캡처한다(`docs/demo/overlay-e-*.png`). HTTP 호출 0건 로그.
- [ ] **Step 6:** 커밋 `[T-37] 시연 E 여행 모드(동행·날씨·서울 빈자리)`
