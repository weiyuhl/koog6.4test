package com.example.myapplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeToolsUiSupportTest {
    @Test
    fun flatten_code_tools_entries_preserves_preorder_and_depth() {
        val tree = CodeToolsEntryDto(
            name = "workspace",
            path = "d:/koog",
            kind = "folder",
            hidden = false,
            children = listOf(
                CodeToolsEntryDto(name = "README.md", path = "d:/koog/README.md", kind = "file", hidden = false, extension = "md"),
                CodeToolsEntryDto(
                    name = "app",
                    path = "d:/koog/app",
                    kind = "folder",
                    hidden = false,
                    children = listOf(CodeToolsEntryDto(name = "Main.kt", path = "d:/koog/app/Main.kt", kind = "file", hidden = false, extension = "kt")),
                ),
            ),
        )

        val flattened = flattenCodeToolsEntries(tree)

        assertEquals(listOf("d:/koog", "d:/koog/README.md", "d:/koog/app", "d:/koog/app/Main.kt"), flattened.map { it.entry.path })
        assertEquals(listOf(0, 1, 1, 2), flattened.map { it.depth })
        assertTrue(flattened.first().displayTitle.contains("📁"))
        assertTrue(flattened[1].displayTitle.contains("📄"))
    }

    @Test
    fun ui_action_summary_contains_kind_size_and_path() {
        val file = CodeToolsEntryDto(
            name = "Example.kt",
            path = "d:/koog/app/Example.kt",
            kind = "file",
            hidden = false,
            extension = "kt",
            sizeLabels = listOf("12 lines"),
            excerptSnippets = listOf(CodeToolsExcerptSnippetDto("fun main()", CodeToolsPositionDto(1, 1), CodeToolsPositionDto(1, 10))),
            textContent = "fun main() = Unit",
        )

        val summary = file.uiActionSummary()

        assertTrue(summary.contains("文件"))
        assertTrue(summary.contains(".kt"))
        assertTrue(summary.contains("12 lines"))
        assertTrue(summary.contains("命中 1 处"))
        assertTrue(summary.contains("d:/koog/app/Example.kt"))
    }
}