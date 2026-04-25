package com.neo.difflink.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.neo.difflink.actions.CompareActionHandler
import com.neo.difflink.utils.ComparePathResolver

class CompareMarkerProvider : LineMarkerProvider {

    private val pathResolver = ComparePathResolver()
    private val comparePattern = Regex("""@DiffLink:\s*(.+)""")

    data class MarkerParams(val source: String, val destination: String)

    private fun parseMarkerParams(match: MatchResult): MarkerParams {
        val content = match.groupValues[1]
            .trimEnd()
            .removeSuffix("*/").trimEnd()
            .removeSuffix("-->").trim()
        val parts = content.split(",", limit = 2).map { it.trim() }
        val second = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return if (second != null) MarkerParams(parts[0], second) else MarkerParams("", parts[0])
    }

    // Returns true if `lang` is a sub-dialect of XML (e.g. HTML) but not XML itself.
    // HTMLLanguage extends XMLLanguage, so registering both "HTML" and "XML" providers
    // causes both to fire for .html files.  When detected as an XML sub-dialect we skip
    // in this call — the more-specific (HTML) provider handles it instead.
    // Plain .xml files have XMLLanguage as their root, so they are never skipped.
    private fun isXmlSubDialect(lang: Language): Boolean {
        var parent = lang.baseLanguage
        while (parent != null) {
            if (parent.id == "XML") return true
            parent = parent.baseLanguage
        }
        return false
    }

    // Must return null here; all work is done in collectSlowLineMarkers.
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val project = file.project

        // Skip injected language fragments (e.g. JavaScript inside HTML <script> tags).
        if (InjectedLanguageManager.getInstance(project).isInjectedFragment(file)) return

        // Skip XML-sub-dialect files (e.g. HTML) when triggered via the XML registration.
        // The more-specific provider (HTML) will handle those files instead.
        if (isXmlSubDialect(file.language)) return

        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        // Per-batch dedup: each call to collectSlowLineMarkers gets its own HashSet.
        // Two-phase rendering (visible + non-visible batches) passes disjoint element
        // sets, so a line is processed at most once per phase — which is correct.
        // HTML duplicate-icon issue is handled by isXmlSubDialect(): when this provider
        // runs for XML registration on an XML sub-dialect (e.g. HTML), we return early
        // and let the more-specific language registration handle the file.
        val seenLines = HashSet<Int>()

        for (element in elements) {
            // Official best practice: only leaf nodes (no children).
            if (element.firstChild != null) continue
            val range = element.textRange ?: continue
            if (range.length <= 0) continue

            val startLine = document.getLineNumber(range.startOffset)
            val endOffset = (range.endOffset - 1).coerceAtLeast(range.startOffset)
            val endLine = document.getLineNumber(endOffset)

            for (lineNum in startLine..endLine) {
                if (!seenLines.add(lineNum)) continue

                val lineStart = document.getLineStartOffset(lineNum)
                val lineEnd = document.getLineEndOffset(lineNum)
                val lineText = document.charsSequence.subSequence(lineStart, lineEnd).toString()
                val match = comparePattern.find(lineText) ?: continue

                val markerRange = TextRange(
                    lineStart + match.range.first,
                    lineStart + match.range.last + 1
                )

                val params = parseMarkerParams(match)
                val fileName = file.name
                val currentFilePath = file.virtualFile?.path ?: continue
                val sourcePath = pathResolver.expandGitShorthand(params.source, currentFilePath, project.basePath)
                val destinationPath = pathResolver.expandGitShorthand(params.destination, currentFilePath, project.basePath)

                val sourceResult = if (sourcePath.isNotEmpty()) {
                    pathResolver.resolvePath(sourcePath, project)
                } else {
                    ComparePathResolver.ResolveResult.Success(file.virtualFile ?: continue)
                }
                val destResult = pathResolver.resolvePath(destinationPath, project)

                result.add(when {
                    sourceResult is ComparePathResolver.ResolveResult.Error ->
                        createMarker(element, markerRange,
                            sourcePath, destinationPath,
                            errorMessage = sourceResult.message, isError = true)

                    destResult is ComparePathResolver.ResolveResult.Error ->
                        createMarker(element, markerRange,
                            sourcePath.ifEmpty { fileName }, destinationPath,
                            errorMessage = destResult.message, isError = true)

                    else ->
                        createMarker(element, markerRange,
                            sourcePath.ifEmpty { fileName }, destinationPath,
                            source = toDiffSource(sourceResult)!!,
                            destination = toDiffSource(destResult)!!,
                            isError = false)
                })
            }
        }
    }

    private fun toDiffSource(result: ComparePathResolver.ResolveResult): CompareActionHandler.DiffSource? = when (result) {
        is ComparePathResolver.ResolveResult.Success -> CompareActionHandler.DiffSource.FileSource(result.file)
        is ComparePathResolver.ResolveResult.GitContent -> CompareActionHandler.DiffSource.GitContent(result.bytes, result.label, result.fileName)
        is ComparePathResolver.ResolveResult.Error -> null
    }

    private fun createMarker(
        element: PsiElement,
        markerRange: TextRange,
        sourcePath: String,
        destPath: String,
        source: CompareActionHandler.DiffSource? = null,
        destination: CompareActionHandler.DiffSource? = null,
        errorMessage: String? = null,
        isError: Boolean
    ): LineMarkerInfo<PsiElement> {
        val icon = if (isError) AllIcons.General.Error else AllIcons.Actions.Diff
        val tooltip = if (isError) {
            "DiffLink: $errorMessage"
        } else {
            "Compare ${sourcePath.ifEmpty { element.containingFile?.name ?: "" }} with $destPath"
        }
        return LineMarkerInfo(
            element,
            markerRange,
            icon,
            { tooltip },
            { _, elt ->
                if (!isError && source != null && destination != null) {
                    CompareActionHandler().navigateToComparison(source, destination, elt.project)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { if (isError) "DiffLink Error" else "DiffLink" }
        )
    }
}
