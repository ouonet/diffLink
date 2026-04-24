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

            val lineNum = document.getLineNumber(range.startOffset)
            if (!seenLines.add(lineNum)) continue   // already handled in this batch

            val lineStart = document.getLineStartOffset(lineNum)
            val lineEnd   = document.getLineEndOffset(lineNum)
            val lineText  = document.charsSequence.subSequence(lineStart, lineEnd).toString()
            val match     = comparePattern.find(lineText) ?: continue

            val markerRange = TextRange(
                lineStart + match.range.first,
                lineStart + match.range.last + 1
            )

            val params = parseMarkerParams(match)
            val fileName = file.name

            val sourceResult = if (params.source.isNotEmpty()) {
                pathResolver.resolvePath(params.source, project)
            } else {
                ComparePathResolver.ResolveResult.Success(file.virtualFile ?: continue)
            }
            val destResult = pathResolver.resolvePath(params.destination, project)

            result.add(when {
                sourceResult is ComparePathResolver.ResolveResult.Success &&
                    destResult is ComparePathResolver.ResolveResult.Success ->
                    createMarker(element, markerRange,
                        params.source.ifEmpty { fileName }, params.destination,
                        sourceResult.file, destResult.file, isError = false)

                sourceResult is ComparePathResolver.ResolveResult.Error ->
                    createMarker(element, markerRange,
                        params.source, params.destination,
                        errorMessage = sourceResult.message, isError = true)

                else ->
                    createMarker(element, markerRange,
                        params.source.ifEmpty { fileName }, params.destination,
                        errorMessage = (destResult as ComparePathResolver.ResolveResult.Error).message,
                        isError = true)
            })
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
                if (!isError && sourceFile != null && destinationFile != null) {
                    CompareActionHandler().navigateToComparison(sourceFile, destinationFile, elt.project)
                }
            },
            GutterIconRenderer.Alignment.LEFT,
            { if (isError) "DiffLink Error" else "DiffLink" }
        )
    }
}
