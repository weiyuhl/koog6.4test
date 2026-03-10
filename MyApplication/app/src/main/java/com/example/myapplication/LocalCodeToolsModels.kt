package com.example.myapplication

data class CodeToolsConfig(
    val enabled: Boolean,
    val workspaceRoot: String,
    val allowedPathPrefixes: String,
)

enum class CodeToolsOperationKind { LIST_DIRECTORY, READ_FILE, WRITE_FILE, EDIT_FILE, REGEX_SEARCH }

enum class LocalCodeToolsFailureKind {
    VALIDATION_FAILURE,
    PATH_VALIDATION_FAILURE,
    FILE_NOT_FOUND,
    NON_TEXT_FILE,
    PATCH_APPLY_FAILURE,
    REGEX_SEARCH_FAILURE,
    UNKNOWN,
}

data class CodeToolsCapability(
    val operation: CodeToolsOperationKind,
    val displayName: String,
    val description: String,
    val writesData: Boolean,
)

data class CodeToolsPositionDto(val line: Int, val column: Int)

data class CodeToolsExcerptSnippetDto(
    val text: String,
    val start: CodeToolsPositionDto,
    val end: CodeToolsPositionDto,
)

data class CodeToolsEntryDto(
    val kind: String,
    val name: String,
    val path: String,
    val hidden: Boolean,
    val extension: String? = null,
    val contentType: String? = null,
    val sizeLabels: List<String> = emptyList(),
    val textContent: String? = null,
    val excerptSnippets: List<CodeToolsExcerptSnippetDto> = emptyList(),
    val children: List<CodeToolsEntryDto> = emptyList(),
)

data class CodeToolsListDirectoryResultDto(val root: CodeToolsEntryDto)

data class CodeToolsReadFileResultDto(val file: CodeToolsEntryDto, val warningMessage: String? = null)

data class CodeToolsWriteFileResultDto(val file: CodeToolsEntryDto)

data class CodeToolsEditFileResultDto(val applied: Boolean, val updatedContent: String? = null, val reason: String? = null)

data class CodeToolsRegexSearchResultDto(val entries: List<CodeToolsEntryDto>, val original: String)

internal fun defaultCodeToolsCapabilities(): List<CodeToolsCapability> = listOf(
    CodeToolsCapability(CodeToolsOperationKind.LIST_DIRECTORY, "__list_directory__", "列出 Android 本地工作区目录。", false),
    CodeToolsCapability(CodeToolsOperationKind.READ_FILE, "__read_file__", "读取 Android 本地文本文件，可选行范围。", false),
    CodeToolsCapability(CodeToolsOperationKind.WRITE_FILE, "__write_file__", "写入 Android 本地文本文件。", true),
    CodeToolsCapability(CodeToolsOperationKind.EDIT_FILE, "edit_file", "在 Android 本地文本文件中执行单次精确替换。", true),
    CodeToolsCapability(CodeToolsOperationKind.REGEX_SEARCH, "__search_contents_by_regex__", "在 Android 本地文件或目录中执行正则搜索。", false),
)