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
    private val comparePattern = Regex("""#COMPARE:\s*(.+)""")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process comments
        if (element !is PsiComment) {
            return null
        }

        // Only process Java files
        if (element.containingFile !is PsiJavaFile) {
            return null
        }

        val commentText = element.text
        val match = comparePattern.find(commentText) ?: return null

        val path = match.groupValues[1].trim()
        val project = element.project

        val resolveResult = pathResolver.resolvePath(path, project)

        return when (resolveResult) {
            is ComparePathResolver.ResolveResult.Success -> {
                val destinationFile = resolveResult.file
                createMarker(
                    element = element,
                    path = path,
                    destinationFile = destinationFile,
                    isError = false
                )
            }
            is ComparePathResolver.ResolveResult.Error -> {
                createMarker(
                    element = element,
                    path = path,
                    errorMessage = resolveResult.message,
                    isError = true
                )
            }
        }
    }

    private fun createMarker(
        element: PsiElement,
        path: String,
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
            "Compare with $path"
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltip },
            { e, elt ->
                if (!isError && destinationFile != null) {
                    val handler = CompareActionHandler()
                    handler.navigateToComparison(
                        sourceFile = elt.containingFile.virtualFile ?: return@LineMarkerInfo,
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
