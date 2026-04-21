package com.lianggong.comparenavigator.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Handles opening the diff viewer when a #COMPARE marker is clicked.
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
            // Create diff contents from both files using DiffContentFactory
            val contentFactory = DiffContentFactory.getInstance()
            val sourceContent = contentFactory.create(project, sourceFile)
            val destContent = contentFactory.create(project, destinationFile)

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
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("Compare Navigator")
                .createNotification(
                    "Failed to open comparison: ${e.message}",
                    com.intellij.notification.NotificationType.ERROR
                )
                .notify(project)
        }
    }
}
