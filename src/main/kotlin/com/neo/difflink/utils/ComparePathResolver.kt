package com.neo.difflink.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Resolves and validates file paths from @DiffLink comments.
 * Path resolution rules:
 * - Relative path (no leading "/"): resolved from the project root, e.g. "src/main/java/File.java"
 * - Absolute path (leading "/"): used as a computer filesystem path, e.g. "/Users/neo/projects/File.java"
 */
class ComparePathResolver {

    sealed class ResolveResult {
        data class Success(val file: VirtualFile) : ResolveResult()
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

            val ioFile = File(absolutePath)
            if (!ioFile.exists()) {
                return ResolveResult.Error("File not found: $absolutePath")
            }
            if (ioFile.isDirectory) {
                return ResolveResult.Error("Path is a directory, not a file: $absolutePath")
            }
            // refreshAndFindFileByIoFile does a synchronous VFS refresh so the
            // VirtualFile is fully registered before we try to read its content.
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)
                ?: return ResolveResult.Error("File not found: $absolutePath")

            return ResolveResult.Success(virtualFile)
        } catch (_: Exception) {
            return ResolveResult.Error("Error resolving path: Unknown error")
        }
    }
}
