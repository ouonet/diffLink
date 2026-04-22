package com.lianggong.comparenavigator.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.lianggong.comparenavigator.actions.CompareActionHandler
import com.lianggong.comparenavigator.utils.ComparePathResolver
import javax.swing.Icon

class CompareMarkerProvider : LineMarkerProvider {

    private val pathResolver = ComparePathResolver()
    private val comparePattern = Regex("""#COMPARE:\s*(.+?)(?:,\s*(.+))?""")

    data class MarkerParams(
        val source: String,
        val destination: String
    )

    private fun parseMarkerParams(match: MatchResult): MarkerParams {
        val firstParam = match.groupValues[1].trim()
        val secondParam = match.groupValues[2].takeIf { it.isNotEmpty() }?.trim()

        return if (secondParam != null) {
            // Two parameters: source and destination explicitly specified
            MarkerParams(source = firstParam, destination = secondParam)
        } else {
            // Single parameter: current file is source, parameter is destination
            MarkerParams(source = "", destination = firstParam)
        }
    }

    private fun isLanguageAwareFile(file: PsiFile): Boolean {
        // Check if the file has associated language support
        // Language.ANY means no language support
        val language = file.language
        return language != com.intellij.lang.Language.ANY
    }

    private fun isInComment(element: PsiElement): Boolean {
        return element is PsiComment
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val file = element.containingFile

        // For language-aware files, only process comments
        // For text files, check any element
        if (isLanguageAwareFile(file)) {
            if (element !is PsiComment) {
                return null
            }
        }

        val commentText = element.text
        val match = comparePattern.find(commentText) ?: return null

        val params = parseMarkerParams(match)
        val project = element.project

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
                    sourcePath = params.source,
                    destPath = params.destination,
                    errorMessage = sourceFile.message,
                    isError = true
                )
            }
            else -> {
                createMarker(
                    element = element,
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
        sourcePath: String,
        destPath: String,
        sourceFile: com.intellij.openapi.vfs.VirtualFile? = null,
        destinationFile: com.intellij.openapi.vfs.VirtualFile? = null,
        errorMessage: String? = null,
        isError: Boolean
    ): LineMarkerInfo<PsiElement> {
        val icon = if (isError) {
            com.intellij.icons.AllIcons.General.Error
        } else {
            com.intellij.icons.AllIcons.Actions.Diff
        }

        val tooltip = if (isError) {
            "Compare Navigator: $errorMessage"
        } else {
            val displaySource = if (sourcePath.isEmpty()) element.containingFile.name else sourcePath
            "Compare $displaySource with $destPath"
        }

        return LineMarkerInfo(
            element,
            element.textRange,
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
            { if (isError) "Compare Navigator Error" else "Compare Navigator" }
        )
    }
}
