package com.lhzkml.codestudio.oss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private val URL_PATTERN = Regex("https?://[^\\s<>\"']+")

/** 判断字符串是否为可获取的许可 URL */
private fun String.isLicenseUrl(): Boolean =
    trim().let { s -> s.startsWith("http://", true) || s.startsWith("https://", true) }

/** 许可 URL 获取结果缓存，避免重复请求 */
private val licenseFetchCache = mutableMapOf<String, Result<String>>()

/** 在后台线程从 URL 获取许可全文（带缓存）。http 会优先尝试 https 以绕过 Android 明文流量限制 */
private suspend fun fetchLicenseFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
    val key = url.trim()
    licenseFetchCache.getOrPut(key) {
        val httpsUrl = if (key.startsWith("http://", true)) "https://" + key.substring(7) else key
        runCatching {
            URL(httpsUrl).openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 20_000
            }.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.recoverCatching {
            // https 失败时回退到原始 http（需 usesCleartextTraffic）
            if (httpsUrl != key) {
                URL(key).openConnection().apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                }.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else throw it
        }
    }
}

private fun buildAnnotatedStringWithLinks(
    text: String,
    uriHandler: (String) -> Unit
): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var lastEnd = 0
        URL_PATTERN.findAll(text).forEach { match ->
            append(text.substring(lastEnd, match.range.first))
            withLink(LinkAnnotation.Url(
                    url = match.value,
                    styles = TextLinkStyles(style = SpanStyle(color = Color(0xFF2196F3))),
                    linkInteractionListener = { uriHandler(match.value) }
                )) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        append(text.substring(lastEnd))
    }
}

@Composable
fun OssLicensesDetailScreen(
    entryName: String,
    entry: OssLicenseEntry?,
    directLicenseUrl: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rawLicense = remember(entry, directLicenseUrl) {
        when {
            directLicenseUrl != null -> directLicenseUrl
            entry != null -> OssLicenseLoader.loadLicenseText(context, entry)
            else -> null
        }
    }

    // 当原始内容为 URL 时，运行时获取许可全文
    var fetchedText by remember(rawLicense) { mutableStateOf<String?>(null) }
    var fetchError by remember(rawLicense) { mutableStateOf<String?>(null) }
    var isLoading by remember(rawLicense) { mutableStateOf(false) }

    LaunchedEffect(rawLicense) {
        fetchedText = null
        fetchError = null
        if (rawLicense != null && rawLicense.isLicenseUrl()) {
            isLoading = true
            fetchLicenseFromUrl(rawLicense)
                .onSuccess { fetchedText = it }
                .onFailure { fetchError = it.message ?: "获取失败" }
            isLoading = false
        }
    }

    // 最终展示的许可正文：优先使用获取到的全文，否则用原始内容
    val displayText = fetchedText ?: rawLicense
    val licenseSourceUrl = if (rawLicense != null && rawLicense.isLicenseUrl()) rawLicense.trim() else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Bar(
            title = entryName,
            onBackClick = onBack
        )

        // 正文区域：库名、许可来源链接、License 全文
        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 库名称（醒目展示）
            Text(
                text = entryName,
                fontSize = 16.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 许可来源（当内容来自 URL 时显示可点击链接）
            if (licenseSourceUrl != null) {
                Text(
                    text = "许可来源：",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
                BasicText(
                    text = buildAnnotatedString {
                        withLink(LinkAnnotation.Url(
                            url = licenseSourceUrl,
                            styles = TextLinkStyles(style = SpanStyle(color = Color(0xFF2196F3))),
                            linkInteractionListener = { uriHandler.openUri(licenseSourceUrl) }
                        )) {
                            append(licenseSourceUrl)
                        }
                    },
                    style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // License 全文 或 加载/错误状态
            when {
                isLoading -> Text(
                    text = "正在加载许可全文…",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
                fetchError != null && displayText == rawLicense && licenseSourceUrl != null -> {
                    val url = licenseSourceUrl
                    Text(
                        text = "无法在线获取许可全文。请点击下方链接在浏览器中查看：",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = buildAnnotatedString {
                            withLink(LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(style = SpanStyle(color = Color(0xFF2196F3))),
                                linkInteractionListener = { uriHandler.openUri(url) }
                            )) {
                                append(url)
                            }
                        },
                        style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF2196F3)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                displayText != null && displayText.isNotBlank() -> {
                    Text(
                        text = "License 全文",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = buildAnnotatedStringWithLinks(displayText) { uriHandler.openUri(it) },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            color = Color(0xFF333333)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> Text(
                    text = "无法加载许可内容",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
