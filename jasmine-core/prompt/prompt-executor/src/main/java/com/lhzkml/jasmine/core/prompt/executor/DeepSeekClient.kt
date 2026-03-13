package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.llm.ChatClientException
import com.lhzkml.jasmine.core.prompt.llm.ErrorType
import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.RetryConfig
import com.lhzkml.jasmine.core.prompt.llm.executeWithRetry
import com.lhzkml.jasmine.core.prompt.model.BalanceDetail
import com.lhzkml.jasmine.core.prompt.model.BalanceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * DeepSeek 客户端
 *
 * 余额查询: GET https://api.deepseek.com/user/balance
 * 认证: Authorization: Bearer <TOKEN>
 */
class DeepSeekClient(
    apiKey: String,
    baseUrl: String = DEFAULT_BASE_URL,
    chatPath: String = "/v1/chat/completions",
    retryConfig: RetryConfig = RetryConfig.DEFAULT,
    httpClient: OkHttpClient? = null
) : OpenAICompatibleClient(apiKey, baseUrl, retryConfig, httpClient, chatPath) {

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
    }

    override val provider = LLMProvider.DeepSeek

    @Serializable
    private data class DeepSeekBalanceResponse(
        @SerialName("is_available") val isAvailable: Boolean = false,
        @SerialName("balance_infos") val balanceInfos: List<DeepSeekBalanceItem> = emptyList()
    )

    @Serializable
    private data class DeepSeekBalanceItem(
        val currency: String = "",
        @SerialName("total_balance") val totalBalance: String = "0",
        @SerialName("granted_balance") val grantedBalance: String = "0",
        @SerialName("topped_up_balance") val toppedUpBalance: String = "0"
    )

    override suspend fun getBalance(): BalanceInfo {
        return executeWithRetry(retryConfig) {
            try {
                val httpRequest = Request.Builder()
                    .url("${baseUrl}/user/balance")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    httpClient.newCall(httpRequest).execute()
                }

                if (!response.isSuccessful) {
                    val body = response.body?.string()
                    throw ChatClientException.fromStatusCode(provider.name, response.code, body)
                }

                val body = response.body?.string() ?: throw ChatClientException(provider.name, "响应为空", ErrorType.PARSE_ERROR)
                val result = json.decodeFromString<DeepSeekBalanceResponse>(body)

                BalanceInfo(
                    isAvailable = result.isAvailable,
                    balances = result.balanceInfos.map {
                        BalanceDetail(
                            currency = it.currency,
                            totalBalance = it.totalBalance,
                            grantedBalance = it.grantedBalance,
                            toppedUpBalance = it.toppedUpBalance
                        )
                    }
                )
            } catch (e: ChatClientException) {
                throw e
            } catch (e: Exception) {
                throw ChatClientException(provider.name, "查询余额失败: ${e.message}", ErrorType.UNKNOWN, cause = e)
            }
        }
    }
}
