package com.neo.difflink.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
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
            // Read bytes directly so DiffContentFactory never hits FileDocumentManager,
            // which returns null for files outside the current project's content roots
            // and causes the "Cannot show diff" empty-content fallback.
            val sourceContent = contentFactory.create(
                project,
                String(sourceFile.contentsToByteArray(), sourceFile.charset),
                sourceFile.fileType
            )
            val destContent = contentFactory.create(
                project,
                String(destinationFile.contentsToByteArray(), destinationFile.charset),
                destinationFile.fileType
            )

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
}
