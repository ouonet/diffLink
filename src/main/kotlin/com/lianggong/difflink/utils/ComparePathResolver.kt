package com.lianggong.difflink.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * Resolves and validates file paths from #DiffLink comments.
 * Path resolution rules:
 * - Relative path (no leading "/"): resolved from the project root, e.g. "src/main/java/File.java"
 * - Absolute path (leading "/"): used as a computer filesystem path, e.g. "/Users/neo/projects/File.java"
 */
class ComparePathResolver {

    sealed class ResolveResult {
        data class Success(val file: com.intellij.openapi.vfs.VirtualFile) : ResolveResult()
        data class Error(val message: String) : ResolveResult()
    }

    fun resolvePath(path: String, project: Project): ResolveResult {
        val trimmedPath = path.trim()

        if (trimmedPath.isEmpty()) {
            return ResolveResult.Error("Path cannot be empty")
        }

        if (trimmedPath.contains("..") || trimmedPath.contains("~")) {
            return ResolveResult.Error("Path traversal not allowed")
        }

        if (trimmedPath.startsWith("http") || trimmedPath.startsWith("file://")) {
            return ResolveResult.Error("External URLs not allowed")
        }

        try {
            val absolutePath = if (trimmedPath.startsWith("/")) {
                // Computer absolute path — use directly
                trimmedPath
            } else {
                // Relative path — resolve from project root
                val baseDir = project.basePath
                    ?: return ResolveResult.Error("Project root not found")
                "$baseDir/$trimmedPath"
            }

            val virtualFile = VirtualFileManager.getInstance()
                .findFileByUrl("file://$absolutePath")

            return if (virtualFile != null && virtualFile.exists()) {
                ResolveResult.Success(virtualFile)
            } else {
                ResolveResult.Error("File not found: $absolutePath")
            }
        } catch (_: Exception) {
            return ResolveResult.Error("Error resolving path: Unknown error")
        }
    }
}
