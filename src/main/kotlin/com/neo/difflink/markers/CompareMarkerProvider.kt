package com.neo.difflink.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.neo.difflink.actions.CompareActionHandler
import com.neo.difflink.utils.ComparePathResolver

/*
@DiffLink: src/main/kotlin/com/neo/difflink/markers/CompareMarkerProvider.kt
 */
class CompareMarkerProvider : LineMarkerProvider {

    private val pathResolver = ComparePathResolver()
    private val comparePattern = Regex("""@DiffLink:\s*(.+)""")

    data class MarkerParams(
        val source: String,
        val destination: String
    )

    private fun parseMarkerParams(match: MatchResult): MarkerParams {
        // Strip trailing block-comment terminators: "*/" (C-style) and "-->" (HTML/XML).
        val content = match.groupValues[1]
            .trimEnd()
            .removeSuffix("*/").trimEnd()
            .removeSuffix("-->").trim()
        val parts = content.split(",", limit = 2).map { it.trim() }
        val second = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }

        return if (second != null) {
            MarkerParams(source = parts[0], destination = second)
        } else {
            MarkerParams(source = "", destination = parts[0])
        }
    }

    private fun isLanguageAwareFile(file: PsiFile): Boolean {
        // Structured languages (Java, Python, etc.) have PsiComment nodes, so we restrict
        // marker detection to comments. Plain-text and Markdown don't, so we scan their
        // text directly. Markdown is matched by id to avoid a compile-time plugin dep.
        val language = file.language
        if (language == Language.ANY) return false
        if (language == PlainTextLanguage.INSTANCE) return false
        if (language.id == "Markdown") return false
        return true
    }

    private fun isInComment(element: PsiElement): Boolean {
        return element is PsiComment
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val file = element.containingFile

        if (isLanguageAwareFile(file)) {
            if (element !is PsiComment) {
                return null
            }
        } else {
            // Non-language-aware files: only match on leaves so we don't attach duplicate
            // markers to every ancestor PSI node that contains the same text.
            if (element.firstChild != null) return null
        }

        val commentText = element.text ?: return null
        val match = comparePattern.find(commentText) ?: return null

        val params = parseMarkerParams(match)
        val project = element.project

        // Anchor the marker to the actual "@DiffLink:..." match, not the whole element.
        // Otherwise a plain-text leaf that spans the entire file would put the gutter
        // icon on line 1 regardless of where the marker sits.
        val elementStart = element.textRange.startOffset
        val markerRange = TextRange(
            elementStart + match.range.first,
            elementStart + match.range.last + 1
        )

        // Resolve source file (current file if not specified)
        val sourceFile = if (params.source.isNotEmpty()) {
            pathResolver.resolvePath(params.source, project)
        } else {
            ComparePathResolver.ResolveResult.Success(element.containingFile.virtualFile ?: return null)
        }

        // Resolve destination file
        val destResult = pathResolver.resolvePath(params.destination, project)

        return when {
            sourceFile is ComparePathResolver.ResolveResult.Success &&
            destResult is ComparePathResolver.ResolveResult.Success -> {
                createMarker(
                    element = element,
                    markerRange = markerRange,
                    sourcePath = params.source.ifEmpty { element.containingFile.name },
                    destPath = params.destination,
                    sourceFile = sourceFile.file,
                    destinationFile = destResult.file,
                    isError = false
                )
            }
            sourceFile is ComparePathResolver.ResolveResult.Error -> {
                createMarker(
                    element = element,
                    markerRange = markerRange,
                    sourcePath = params.source,
                    destPath = params.destination,
                    errorMessage = sourceFile.message,
                    isError = true
                )
            }
            else -> {
                createMarker(
                    element = element,
                    markerRange = markerRange,
                    sourcePath = params.source.ifEmpty { element.containingFile.name },
                    destPath = params.destination,
                    errorMessage = (destResult as ComparePathResolver.ResolveResult.Error).message,
                    isError = true
                )
            }
        }
    }

    private fun createMarker(
        element: PsiElement,
        markerRange: TextRange,
        sourcePath: String,
        destPath: String,
        sourceFile: VirtualFile? = null,
        destinationFile: VirtualFile? = null,
        errorMessage: String? = null,
        isError: Boolean
    ): LineMarkerInfo<PsiElement> {
        val icon = if (isError) {
            AllIcons.General.Error
        } else {
            AllIcons.Actions.Diff
        }

        val tooltip = if (isError) {
            "DiffLink: $errorMessage"
        } else {
            val displaySource = if (sourcePath.isEmpty()) element.containingFile.name else sourcePath
            "Compare $displaySource with $destPath"
        }

        return LineMarkerInfo(
            element,
            markerRange,
            icon,
            { tooltip },
            { _, elt ->
                if (!isError && sourceFile != null && destinationFile != null) {
                    val handler = CompareActionHandler()
                    handler.navigateToComparison(
                        sourceFile = sourceFile,
                        destinationFile = destinationFile,
                        project = elt.project
                    )
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { if (isError) "DiffLink Error" else "DiffLink" }
        )
    }
}
