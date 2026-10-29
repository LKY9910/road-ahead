package com.roadahead

import com.roadahead.core.model.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DataEventTest {
    @Test fun `collector continues after data errors and propagates cancellation`() {
        val apiError = ApiException("parse")
        val unexpectedError = IllegalStateException("bad event")
        val cancellation = CancellationException("stop")
        val rejected = mutableListOf<Exception>()
        val handled = mutableListOf<Int>()

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking {
                flowOf(0, 1, 2, 3, 4).collect { event ->
                    handleDataEvent({ rejected += it }) {
                        when (event) {
                            0 -> throw apiError
                            1 -> throw unexpectedError
                            3 -> throw cancellation
                            else -> handled += event
                        }
                    }
                }
            }
        }

        assertEquals(listOf(apiError, unexpectedError), rejected)
        assertEquals(listOf(2), handled)
        assertSame(cancellation, thrown)
    }
}
