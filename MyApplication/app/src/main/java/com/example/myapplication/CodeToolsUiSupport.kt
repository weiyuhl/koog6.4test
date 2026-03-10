package com.example.myapplication

internal data class CodeToolsUiEntry(
    val entry: CodeToolsEntryDto,
    val depth: Int,
) {
    val displayTitle: String
        get() = buildString {
            append("  ".repeat(depth))
            append(if (entry.kind == "folder") "📁 " else "📄 ")
            append(entry.name.ifBlank { entry.path.substringAfterLast('/', entry.path) })
        }
}

internal fun flattenCodeToolsEntries(root: CodeToolsEntryDto): List<CodeToolsUiEntry> = buildList {
    fun walk(entry: CodeToolsEntryDto, depth: Int) {
        add(CodeToolsUiEntry(entry = entry, depth = depth))
        entry.children.forEach { walk(it, depth + 1) }
    }
    walk(root, 0)
}

internal fun CodeToolsEntryDto.uiActionSummary(): String = buildString {
    append(if (kind == "folder") "目录" else "文件")
    extension?.takeIf { it.isNotBlank() }?.let { append(" · .$it") }
    if (sizeLabels.isNotEmpty()) append(" · ${sizeLabels.joinToString()}")
    if (excerptSnippets.isNotEmpty()) append(" · 命中 ${excerptSnippets.size} 处")
    textContent?.takeIf { it.isNotBlank() }?.let { append(" · ${it.length} chars") }
    append(" · $path")
}

internal fun CodeToolsEntryDto.renderTreeText(depth: Int = 0): String = buildString {
    append("  ".repeat(depth))
    append(if (kind == "folder") "📁 " else "📄 ")
    append(name)
    textContent?.takeIf { it.isNotBlank() }?.let { append(" (${it.length} chars)") }
    children.forEach { child -> appendLine().append(child.renderTreeText(depth + 1)) }
}

internal fun CodeToolsRegexSearchResultDto.renderSearchText(): String =
    if (entries.isEmpty()) "无结果" else entries.joinToString("\n\n") { entry ->
        buildString {
            append(entry.path)
            if (entry.excerptSnippets.isNotEmpty()) {
                entry.excerptSnippets.forEach { snippet ->
                    appendLine()
                    append("  [${snippet.start.line}:${snippet.start.column} - ${snippet.end.line}:${snippet.end.column}] ")
                    append(snippet.text)
                }
            } else if (!entry.textContent.isNullOrBlank()) {
                appendLine()
                append(entry.textContent)
            }
        }
    }