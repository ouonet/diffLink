package com.neo.difflink.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Handles opening the diff viewer when a @DiffLink marker is clicked.
 * @DiffLink: git://1.0.1:src/main/kotlin/com/neo/difflink/utils/ComparePathResolver.kt
 */
// Compare current file (left) with file content from tag v1.0.0 (right)
// @DiffLink: git://1.0.1:src/main/kotlin/com/neo/difflink/utils/ComparePathResolver.kt
class CompareActionHandler(
    private val showDiff: (Project, SimpleDiffRequest) -> Unit = { project, request ->
        DiffManager.getInstance().showDiff(project, request)
    }
) : AnAction() {

    /** Represents a file source for diffing — either a local VirtualFile or git history content. */
    sealed class DiffSource {
        data class FileSource(val file: VirtualFile) : DiffSource()
        data class GitContent(val bytes: ByteArray, val label: String, val fileName: String) : DiffSource()
    }

    override fun actionPerformed(e: AnActionEvent) {
        // This is called via the menu action, not used for gutter clicks
        // (gutter clicks invoke navigateToComparison directly)
    }

    /** Convenience overload for local VirtualFile sources. */
    fun navigateToComparison(sourceFile: VirtualFile, destinationFile: VirtualFile, project: Project) {
        navigateToComparison(DiffSource.FileSource(sourceFile), DiffSource.FileSource(destinationFile), project)
    }

    fun navigateToComparison(
        source: DiffSource,
        destination: DiffSource,
        project: Project
    ) {
        try {
            val contentFactory = DiffContentFactory.getInstance()
            val sourceContent = createDiffContent(contentFactory, project, source)
            val destContent = createDiffContent(contentFactory, project, destination)

            val diffRequest = SimpleDiffRequest(
                "Compare Files",
                sourceContent,
                destContent,
                labelFor(source),
                labelFor(destination)
            )

            showDiff(project, diffRequest)
        } catch (e: Exception) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DiffLink")
                .createNotification(
                    "Failed to open comparison: ${e.message}",
                    NotificationType.ERROR
                )
                .notify(project)
        }
    }

    private fun labelFor(source: DiffSource): String = when (source) {
        is DiffSource.FileSource -> source.file.name
        is DiffSource.GitContent -> source.label
    }

    private fun createDiffContent(
        contentFactory: DiffContentFactory,
        project: Project,
        source: DiffSource
    ): DiffContent = when (source) {
        is DiffSource.FileSource -> createFileContent(contentFactory, project, source.file)
        is DiffSource.GitContent -> {
            val fileType = FileTypeManager.getInstance().getFileTypeByFileName(source.fileName)
            contentFactory.create(project, source.bytes.toString(Charsets.UTF_8), fileType)
        }
    }

    private fun createFileContent(
        contentFactory: DiffContentFactory,
        project: Project,
        file: VirtualFile
    ): DiffContent {
        return try {
            // Prefer file-backed content so the diff viewer can edit and save changes.
            contentFactory.create(project, file)
        } catch (_: Exception) {
            // Fallback for edge cases where file-backed content cannot be created.
            contentFactory.create(
                project,
                String(file.contentsToByteArray(), file.charset),
                file.fileType
            )
        }
    }
}
