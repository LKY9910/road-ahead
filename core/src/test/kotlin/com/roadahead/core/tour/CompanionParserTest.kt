package com.roadahead.core.tour

import com.roadahead.core.model.*
import com.roadahead.core.model.Facility.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompanionParserTest {
    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name.json")!!.readText()
    private fun detail(fields: String) = """{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[{$fields}]}}}}"""

    @Test fun `facilities from barrier free detail`() {
        assertEquals(setOf(STROLLER, LACTATION, WHEELCHAIR, STEP_FREE, ACCESSIBLE_TOILET),
            parseFacilities(fixture("KorWithService2-detail-1605981")))
        assertEquals(setOf(ACCESSIBLE_TOILET), parseFacilities(fixture("KorWithService2-detail-127015")))
        assertEquals(emptySet<Facility>(), parseFacilities(fixture("with-missing")))
    }

    @Test fun `negative facility text is ignored`() {
        assertEquals(emptySet<Facility>(), parseFacilities(detail(""""stroller":"대여 불가","parking":"장애인 주차장 없음","restroom":"없음","exit":"주출입구에 턱이 있음"""")))
        assertEquals(setOf(STEP_FREE), parseFacilities(detail(""""route":"출입구까지 경사로가 설치되어 있음"""")))
        assertEquals(setOf(STEP_FREE), parseFacilities(detail(""""exit":"턱이 없어 출입 가능"""")))
    }

    @Test fun `pet info from detail`() {
        assertEquals(PetInfo(PetScope.PARTIAL, "목줄 착용"), parsePetInfo(fixture("KorPetTourService2-detail-264311")))
        assertEquals(PetInfo(PetScope.ALL, null), parsePetInfo(fixture("KorPetTourService2-detail-2695730")))
        assertNull(parsePetInfo(fixture("pet-missing")))
        assertNull(parsePetInfo(detail(""""acmpyTypeCd":"동반불가"""")))
    }

    @Test fun `pet need stays short and single line`() {
        for (need in listOf("목줄 착용\\n배변봉투 지참", "12345678901", "")) {
            assertEquals(PetInfo(PetScope.ALL, null),
                parsePetInfo(detail("\"acmpyTypeCd\":\"전구역 동반가능\",\"acmpyNeedMtr\":\"$need\"")))
        }
    }

    @Test fun `error result throws`() {
        val error = """{"response":{"header":{"resultCode":"0003"}}}"""
        for (parse in listOf(::parsePetInfo, ::parseFacilities)) {
            assertThrows(ApiException::class.java) { parse(error) }
            assertThrows(ApiException::class.java) { parse("not json") }
        }
    }
}
