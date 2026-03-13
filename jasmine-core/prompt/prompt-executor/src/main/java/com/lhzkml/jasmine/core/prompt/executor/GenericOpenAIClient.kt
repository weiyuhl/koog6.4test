package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.RetryConfig
import okhttp3.OkHttpClient

/**
 * 通用 OpenAI 兼容客户端
 * 用于支持任意 OpenAI 兼容的供应商
 */
class GenericOpenAIClient(
    providerName: String,
    apiKey: String,
    baseUrl: String,
    chatPath: String = "/v1/chat/completions",
    retryConfig: RetryConfig = RetryConfig.DEFAULT,
    httpClient: OkHttpClient? = null
) : OpenAICompatibleClient(apiKey, baseUrl, retryConfig, httpClient, chatPath) {

    override val provider = LLMProvider.Custom(providerName)
}
