package com.neo.difflink.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Handles opening the diff viewer when a @DiffLink marker is clicked.
 */
class CompareActionHandler : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // This is called via the menu action, not used for gutter clicks
        // (gutter clicks invoke navigateToComparison directly)
    }

    fun navigateToComparison(
        sourceFile: VirtualFile,
        destinationFile: VirtualFile,
        project: Project
    ) {
        try {
            val contentFactory = DiffContentFactory.getInstance()
            val sourceContent = createDiffContent(contentFactory, project, sourceFile)
            val destContent = createDiffContent(contentFactory, project, destinationFile)

            // Create diff request
            val diffRequest = SimpleDiffRequest(
                "Compare Files",
                sourceContent,
                destContent,
                sourceFile.name,
                destinationFile.name
            )

            // Show diff in IntelliJ's diff viewer
            DiffManager.getInstance().showDiff(project, diffRequest)
        } catch (e: Exception) {
            // Log error (IntelliJ will handle the notification)
            NotificationGroupManager.getInstance()
                .getNotificationGroup("DiffLink")
                .createNotification(
                    "Failed to open comparison: ${e.message}",
                    NotificationType.ERROR
                )
                .notify(project)
        }
    }

    private fun createDiffContent(
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
