package com.lhzkml.codestudio

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
        assertEquals("gpt-4o-mini", Provider.OPENAI.defaultModelId)
        assertTrue(Provider.OPENAI.requiresApiKey)
        assertTrue(Provider.OPENAI.isSupportedOnAndroid)
    }

    @Test
    fun bedrock_is_listed_but_marked_unsupported_on_android() {
        assertEquals("AWS Bedrock", Provider.BEDROCK.displayName)
        assertFalse(Provider.BEDROCK.requiresApiKey)
        assertFalse(Provider.BEDROCK.isSupportedOnAndroid)
    }
}
