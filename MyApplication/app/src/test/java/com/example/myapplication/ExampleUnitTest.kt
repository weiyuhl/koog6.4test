package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun openai_defaults_are_exposed() {
        assertEquals("gpt-4o-mini", KoogProvider.OPENAI.defaultModelId)
        assertTrue(KoogProvider.OPENAI.requiresApiKey)
        assertTrue(KoogProvider.OPENAI.isSupportedOnAndroid)
    }

    @Test
    fun bedrock_is_listed_but_marked_unsupported_on_android() {
        assertEquals("AWS Bedrock", KoogProvider.BEDROCK.displayName)
        assertFalse(KoogProvider.BEDROCK.requiresApiKey)
        assertFalse(KoogProvider.BEDROCK.isSupportedOnAndroid)
    }
}