package com.example.myapplication

import java.io.File
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

internal class LocalCodeToolsException(
    val kind: LocalCodeToolsFailureKind,
    override val message: String,
) : IllegalStateException(message)

private data class LocalCodeToolsContext(
    val workspaceRoot: File,
    val allowedRoots: List<File>,
)

internal fun effectiveCodeToolsWorkspaceRoot(config: CodeToolsConfig): String = config.localCodeToolsContext().workspaceRoot.path

internal fun effectiveCodeToolsAllowedPrefixes(config: CodeToolsConfig): List<String> = config.localCodeToolsContext().allowedRoots.map(File::getPath)

internal fun listLocalCodeToolsDirectory(path: String, config: CodeToolsConfig, depth: Int = 1, filter: String? = null): CodeToolsListDirectoryResultDto {
    requireCodeToolsEnabled(config)
    if (depth < 0) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "depth 不能小于 0")
    val root = resolveLocalCodeToolsPath(path, config, mustExist = true, expectDirectory = true)
    return CodeToolsListDirectoryResultDto(root = root.toEntry(depth, filter))
}

internal fun readLocalCodeToolsFile(path: String, config: CodeToolsConfig, startLine: Int = 0, endLine: Int = -1): CodeToolsReadFileResultDto {
    requireCodeToolsEnabled(config)
    if (startLine < 0) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "startLine 不能小于 0")
    if (endLine != -1 && endLine < startLine) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "endLine 必须大于等于 startLine")
    val file = resolveLocalCodeToolsPath(path, config, mustExist = true, expectDirectory = false)
    val fullText = readLocalCodeToolsText(file)
    val lines = fullText.lines()
    val lastIndex = if (endLine == -1) lines.lastIndex else minOf(endLine, lines.lastIndex)
    val visibleText = if (lines.isEmpty() || startLine > lines.lastIndex) "" else lines.subList(startLine, lastIndex + 1).joinToString("\n")
    val warning = if (startLine > 0 || endLine != -1) "已按行范围返回内容" else null
    return CodeToolsReadFileResultDto(file = file.toFileEntry(visibleText), warningMessage = warning)
}

internal fun writeLocalCodeToolsFile(path: String, config: CodeToolsConfig, content: String): CodeToolsWriteFileResultDto {
    requireCodeToolsEnabled(config)
    val file = resolveLocalCodeToolsPath(path, config, mustExist = false, expectDirectory = false)
    file.parentFile?.mkdirs()
    file.writeText(content, StandardCharsets.UTF_8)
    return CodeToolsWriteFileResultDto(file = file.toFileEntry(content))
}

internal fun editLocalCodeToolsFile(path: String, config: CodeToolsConfig, original: String, replacement: String): CodeToolsEditFileResultDto {
    requireCodeToolsEnabled(config)
    if (original.isEmpty()) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "original 不能为空")
    val file = resolveLocalCodeToolsPath(path, config, mustExist = true, expectDirectory = false)
    val current = readLocalCodeToolsText(file)
    if (!current.contains(original)) return CodeToolsEditFileResultDto(applied = false, updatedContent = current, reason = "未找到要替换的原始片段")
    val updated = current.replaceFirst(original, replacement)
    file.writeText(updated, StandardCharsets.UTF_8)
    return CodeToolsEditFileResultDto(applied = true, updatedContent = updated)
}

internal fun regexSearchLocalCodeTools(path: String, config: CodeToolsConfig, regex: String, limit: Int = 25, skip: Int = 0, caseSensitive: Boolean = false): CodeToolsRegexSearchResultDto {
    requireCodeToolsEnabled(config)
    if (limit <= 0) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "limit 必须大于 0")
    if (skip < 0) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "skip 不能小于 0")
    val target = resolveLocalCodeToolsPath(path, config, mustExist = true, expectDirectory = null)
    val regexValue = runCatching { Regex(regex, setOfNotNull(if (caseSensitive) null else RegexOption.IGNORE_CASE)) }
        .getOrElse { throw LocalCodeToolsException(LocalCodeToolsFailureKind.REGEX_SEARCH_FAILURE, it.message ?: "正则表达式无效") }
    val matches = mutableListOf<CodeToolsEntryDto>()
    var skipped = 0
    walkLocalCodeToolsFiles(target).forEach { file ->
        val text = runCatching { readLocalCodeToolsText(file) }.getOrElse { return@forEach }
        val snippets = regexValue.findAll(text).map { match ->
            CodeToolsExcerptSnippetDto(match.value, text.offsetToCodeToolsPosition(match.range.first), text.offsetToCodeToolsPosition(match.range.last + 1))
        }.toList()
        if (snippets.isEmpty()) return@forEach
        if (skipped < skip) {
            skipped += 1
            return@forEach
        }
        matches += file.toFileEntry(text = null, snippets = snippets)
        if (matches.size >= limit) return@forEach
    }
    return CodeToolsRegexSearchResultDto(entries = matches.take(limit), original = regex)
}

private fun requireCodeToolsEnabled(config: CodeToolsConfig) {
    if (!config.enabled) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "Code Tools 当前未启用")
}

private fun resolveLocalCodeToolsPath(path: String, config: CodeToolsConfig, mustExist: Boolean, expectDirectory: Boolean?): File {
    val context = config.localCodeToolsContext()
    val raw = path.trim().ifBlank { context.workspaceRoot.path }
    val candidate = File(raw).let { if (it.isAbsolute) it else File(context.workspaceRoot, raw) }.canonicalFile
    if (!context.isAllowed(candidate)) throw LocalCodeToolsException(LocalCodeToolsFailureKind.PATH_VALIDATION_FAILURE, "路径不在允许范围内：${candidate.path}")
    if (mustExist && !candidate.exists()) throw LocalCodeToolsException(LocalCodeToolsFailureKind.FILE_NOT_FOUND, "文件不存在：${candidate.path}")
    if (expectDirectory == true && !candidate.isDirectory) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "目标不是目录：${candidate.path}")
    if (expectDirectory == false && candidate.exists() && !candidate.isFile) throw LocalCodeToolsException(LocalCodeToolsFailureKind.VALIDATION_FAILURE, "目标不是文件：${candidate.path}")
    return candidate
}

private fun CodeToolsConfig.localCodeToolsContext(): LocalCodeToolsContext {
    val workspaceRoot = File(workspaceRoot.ifBlank { "." }).canonicalFile
    val allowedRoots = allowedPathPrefixes.lineSequence().flatMap { it.split(';').asSequence() }.map(String::trim).filter(String::isNotBlank).map { File(it).canonicalFile }.toList().ifEmpty { listOf(workspaceRoot) }
    return LocalCodeToolsContext(workspaceRoot, allowedRoots)
}

private fun LocalCodeToolsContext.isAllowed(file: File): Boolean = allowedRoots.any { root -> file.path == root.path || file.path.startsWith(root.path + File.separator) }

private fun File.toEntry(depth: Int, filter: String?): CodeToolsEntryDto {
    val children = if (isDirectory && depth > 0) listFiles().orEmpty().sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }).filter { it.name.matchesLocalCodeToolsFilter(filter) }.map { it.toEntry(depth - 1, filter) } else emptyList()
    return if (isDirectory) CodeToolsEntryDto(kind = "folder", name = name.ifBlank { path }, path = path, hidden = isHidden, children = children) else toFileEntry(text = null)
}

private fun File.toFileEntry(text: String?, snippets: List<CodeToolsExcerptSnippetDto> = emptyList()): CodeToolsEntryDto = CodeToolsEntryDto(
    kind = "file",
    name = name,
    path = path,
    hidden = isHidden,
    extension = extension.takeIf { it.isNotBlank() },
    contentType = "text/plain",
    sizeLabels = listOf("${length()} B"),
    textContent = text,
    excerptSnippets = snippets,
)

private fun String.matchesLocalCodeToolsFilter(filter: String?): Boolean {
    val value = filter?.trim().orEmpty()
    if (value.isEmpty()) return true
    val regex = value.replace(".", "\\.").replace("*", ".*").replace("?", ".")
    return Regex("^$regex$", RegexOption.IGNORE_CASE).matches(this)
}

private fun walkLocalCodeToolsFiles(target: File): Sequence<File> = if (target.isFile) sequenceOf(target) else target.walkTopDown().filter(File::isFile)

private fun readLocalCodeToolsText(file: File): String = try {
    val decoder = StandardCharsets.UTF_8.newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)
    decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
    decoder.decode(java.nio.ByteBuffer.wrap(file.readBytes())).toString().also {
        if (it.contains('\u0000')) throw LocalCodeToolsException(LocalCodeToolsFailureKind.NON_TEXT_FILE, "文件包含二进制内容：${file.path}")
    }
} catch (_: CharacterCodingException) {
    throw LocalCodeToolsException(LocalCodeToolsFailureKind.NON_TEXT_FILE, "不是 UTF-8 文本文件：${file.path}")
}

private fun String.offsetToCodeToolsPosition(offset: Int): CodeToolsPositionDto {
    val bounded = offset.coerceIn(0, length)
    var line = 1
    var column = 1
    repeat(bounded) { index ->
        if (this[index] == '\n') {
            line += 1
            column = 1
        } else {
            column += 1
        }
    }
    return CodeToolsPositionDto(line, column)
}