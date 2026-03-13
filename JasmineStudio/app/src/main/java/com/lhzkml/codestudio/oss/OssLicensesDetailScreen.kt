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
    
    // 状态管理
    var displayText by remember { mutableStateOf<String?>(null) }
    var licenseSourceUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    // 按需加载许可内容
    LaunchedEffect(entry, directLicenseUrl) {
        isLoading = true
        fetchError = null
        displayText = null
        licenseSourceUrl = null
        
        try {
            when {
                // 情况1: 直接提供的 URL
                directLicenseUrl != null -> {
                    licenseSourceUrl = directLicenseUrl.trim()
                    if (licenseSourceUrl!!.isLicenseUrl()) {
                        fetchLicenseFromUrl(licenseSourceUrl!!)
                            .onSuccess { displayText = it }
                            .onFailure { 
                                fetchError = it.message ?: "获取失败"
                                displayText = null
                            }
                    } else {
                        displayText = directLicenseUrl
                    }
                }
                // 情况2: 从本地资源加载
                entry != null -> {
                    val rawText = OssLicenseLoader.loadLicenseText(context, entry)
                    if (rawText != null && rawText.isLicenseUrl()) {
                        licenseSourceUrl = rawText.trim()
                        fetchLicenseFromUrl(licenseSourceUrl!!)
                            .onSuccess { displayText = it }
                            .onFailure { 
                                fetchError = it.message ?: "获取失败"
                                displayText = null
                            }
                    } else {
                        displayText = rawText
                    }
                }
                else -> {
                    displayText = null
                }
            }
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Bar(
            title = entryName,
            onBackClick = onBack
        )

        val uriHandler = LocalUriHandler.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 库名称
            Text(
                text = entryName,
                fontSize = 16.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 许可来源链接
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
                            url = licenseSourceUrl!!,
                            styles = TextLinkStyles(style = SpanStyle(color = Color(0xFF2196F3))),
                            linkInteractionListener = { uriHandler.openUri(licenseSourceUrl!!) }
                        )) {
                            append(licenseSourceUrl!!)
                        }
                    },
                    style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 内容显示
            when {
                isLoading -> Text(
                    text = "正在加载许可全文…",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.fillMaxWidth()
                )
                fetchError != null && licenseSourceUrl != null -> {
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
                                url = licenseSourceUrl!!,
                                styles = TextLinkStyles(style = SpanStyle(color = Color(0xFF2196F3))),
                                linkInteractionListener = { uriHandler.openUri(licenseSourceUrl!!) }
                            )) {
                                append(licenseSourceUrl!!)
                            }
                        },
                        style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF2196F3)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                displayText != null && displayText!!.isNotBlank() -> {
                    Text(
                        text = "License 全文",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = buildAnnotatedStringWithLinks(displayText!!) { uriHandler.openUri(it) },
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
