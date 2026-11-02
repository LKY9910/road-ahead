package com.roadahead.ui

import com.roadahead.core.model.Companion
import com.roadahead.ui.CompanionChoice.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CompanionPickerTest {
    @Test fun `none is exclusive`() {
        assertEquals(setOf(NONE), toggleChoice(setOf(CHILD, PET), NONE))
        assertEquals(setOf(NONE), toggleChoice(setOf(NONE), NONE))
        assertEquals(setOf(ELDER), toggleChoice(setOf(NONE), ELDER))
        assertEquals(emptySet<CompanionChoice>(), toggleChoice(setOf(ELDER), ELDER))
    }

    @Test fun `stored companions round trip`() {
        val unanswered: Set<Companion>? = null
        assertEquals(emptySet<CompanionChoice>(), unanswered.toChoices())
        assertEquals(setOf(NONE), emptySet<Companion>().toChoices())
        assertEquals(emptySet<Companion>(), setOf(NONE).toCompanions())
        assertEquals(setOf(Companion.CHILD, Companion.PET), setOf(CHILD, PET).toCompanions())
        assertEquals(setOf(CHILD, PET), setOf(Companion.CHILD, Companion.PET).toChoices())
    }
}
