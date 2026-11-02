# 동행 맞춤 추천 자료 확인

2026-10-29 · Claude. 서울시청(37.5663, 126.9779) 근처에서 한국관광공사 API를 조회했다. 공공데이터포털 키(`api`의 `신호등` 줄)는 요청에만 쓰고 저장·출력하지 않았다. 응답 원문은 [samples.json](travel-companion/samples.json)에 있다.

## 결과

| 서비스 | 활용신청 전 | 신청 뒤 | 쓰는 것 |
|---|---|---|---|
| 국문 관광정보 `KorService2/detailIntro2` | 됨 | 됨 | 쓰지 않음. 근처 12곳에서 `chkpet*` 12곳 모두 빈 칸, `chkbabycarriage*`는 빈 칸이거나 "없음" |
| 반려동물 동반여행 `KorPetTourService2` | 403 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR` | 됨 | `locationBasedList2`, `detailPetTour2` |
| 무장애 여행 정보 `KorWithService2` | 403 같은 오류 | 됨 | `detailWithTour2` |

- v1 이름(`KorPetTourService`, `KorWithService1`)은 `NO_OPENAPI_SERVICE_ERROR`다. v2만 쓴다.
- 반려동물 동반 목록(반경 3 km, 거리순)은 13곳이다. 종류는 관광지(12) 8곳, 레포츠(28) 1곳, 숙박(32) 4곳이다. 12·14·28로 거르면 9곳이 남는다. 가까운 순으로 명동 775 m, 인사동 1,133 m, 익선동 한옥거리 1,368 m, 남산순환나들길 1,486 m, 북악하늘길 스카이웨이 2,593 m이다.
- `detailPetTour2`에서 받은 `acmpyTypeCd` 값은 "일부구역 동반가능"(명동·인사동·익선동)과 "전구역 동반가능"(효자스테이) 두 가지뿐이었다. "동반 불가"가 어떤 값으로 오는지는 보지 못했다.
- 무장애 목록은 같은 반경에 71곳이고 음식점(39)이 섞여 있다. `detailWithTour2`는 자유 문장이다.
  - 덕수궁 대한문(1605981): `stroller="대여가능"`, `lactationroom="수유실 있음"`, `wheelchair="대여가능"`, `restroom="장애인 화장실 있음"`, `exit="주출입구는 턱이 없어 휠체어 접근 가능함"`
  - 대한성공회 서울주교좌성당(127015): `restroom`만 있음
- 장소 번호는 국문 관광정보와 같다(1605981, 127015 확인).
- 그 서비스에 없는 장소를 상세 조회하면 HTTP 200, `resultCode="0000"`, `items: ""`, `totalCount: 0`이다(두 서비스 같음).
- 조회는 약 30회였다(국문 관광정보 약 20, 두 서비스 각 5~6).

## 날씨 (기상청 단기예보)

- 공공데이터포털 `VilageFcstInfoService_2.0/getVilageFcst`: 활용신청 전 403 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`, 신청 뒤 `resultCode="00"`. API허브(`apihub.kma.go.kr` typ02) 쪽은 지금 키로 403 "활용신청이 필요한 API"라 쓰지 않는다.
- 서울시청 격자 60, 127, 2026-10-29 17시 발표: `totalCount=1052`(약 3일 × 14개 항목). 18~23시 `PTY=1`(비), `POP=60`, `TMP` 10~11도. 시연용 원문: [forecast-20261029-1700.json](travel-companion/forecast-20261029-1700.json).
