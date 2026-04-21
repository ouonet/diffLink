package com.lianggong.comparenavigator.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Resolves and validates file paths from #COMPARE comments.
 * Paths are resolved relative to the project root.
 */
class ComparePathResolver {

    sealed class ResolveResult {
        data class Success(val file: com.intellij.openapi.vfs.VirtualFile) : ResolveResult()
        data class Error(val message: String) : ResolveResult()
    }

    fun resolvePath(path: String, project: Project): ResolveResult {
        // Trim whitespace
        val trimmedPath = path.trim()

        // Validate path is not empty
        if (trimmedPath.isEmpty()) {
            return ResolveResult.Error("Path cannot be empty")
        }

        // Check for directory traversal attempts
        if (trimmedPath.contains("..") || trimmedPath.contains("~")) {
            return ResolveResult.Error("Path traversal not allowed")
        }

        // Ensure path starts with /
        val normalizedPath = if (trimmedPath.startsWith("/")) {
            trimmedPath
        } else {
            "/$trimmedPath"
        }

        // Reject external URLs or file:// schemes
        if (normalizedPath.startsWith("http") || normalizedPath.startsWith("file://")) {
            return ResolveResult.Error("External URLs not allowed")
        }

        try {
            // Get project root
            val baseDir = project.basePath ?: return ResolveResult.Error("Project root not found")

            // Construct absolute path
            val absolutePath = baseDir + normalizedPath

            // Look up virtual file
            val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$absolutePath")

            return if (virtualFile != null && virtualFile.exists()) {
                ResolveResult.Success(virtualFile)
            } else {
                ResolveResult.Error("File not found: $normalizedPath")
            }
        } catch (e: Exception) {
            return ResolveResult.Error("Error resolving path: ${e.message}")
        }
    }
}
