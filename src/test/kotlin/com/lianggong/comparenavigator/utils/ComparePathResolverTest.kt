package com.lianggong.comparenavigator.utils

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Assert.*

class ComparePathResolverTest : LightJavaCodeInsightFixtureTestCase() {

    private lateinit var resolver: ComparePathResolver

    override fun setUp() {
        super.setUp()
        resolver = ComparePathResolver()
    }

    fun testResolveValidPath() {
        // Create a test file in the project
        myFixture.addFileToProject("TestFile.java", "public class Test {}")

        val result = resolver.resolvePath("/TestFile.java", project)
        assertTrue("Should resolve existing file", result is ComparePathResolver.ResolveResult.Success)
    }

    fun testResolveNonExistentFile() {
        val result = resolver.resolvePath("/NonExistent.java", project)
        assertTrue("Should return error for missing file", result is ComparePathResolver.ResolveResult.Error)
        assertEquals("File not found: /NonExistent.java", (result as ComparePathResolver.ResolveResult.Error).message)
    }

    fun testRejectDirectoryTraversal() {
        val result = resolver.resolvePath("/../etc/passwd", project)
        assertTrue("Should reject directory traversal", result is ComparePathResolver.ResolveResult.Error)
        assertTrue((result as ComparePathResolver.ResolveResult.Error).message.contains("traversal"))
    }

    fun testRejectHomeDirectory() {
        val result = resolver.resolvePath("~/secret.txt", project)
        assertTrue("Should reject home directory", result is ComparePathResolver.ResolveResult.Error)
    }

    fun testRejectEmptyPath() {
        val result = resolver.resolvePath("", project)
        assertTrue("Should reject empty path", result is ComparePathResolver.ResolveResult.Error)
    }

    fun testRejectExternalUrls() {
        val result = resolver.resolvePath("http://example.com/file.java", project)
        assertTrue("Should reject HTTP URLs", result is ComparePathResolver.ResolveResult.Error)
    }

    fun testNormalizePath() {
        myFixture.addFileToProject("src/Test.java", "public class Test {}")

        // Test both with and without leading /
        val result1 = resolver.resolvePath("/src/Test.java", project)
        val result2 = resolver.resolvePath("src/Test.java", project)

        assertTrue("Should resolve with leading /", result1 is ComparePathResolver.ResolveResult.Success)
        assertTrue("Should resolve without leading /", result2 is ComparePathResolver.ResolveResult.Success)
    }

    fun testTrimWhitespace() {
        myFixture.addFileToProject("Test.java", "public class Test {}")

        val result = resolver.resolvePath("  /Test.java  ", project)
        assertTrue("Should trim whitespace and resolve", result is ComparePathResolver.ResolveResult.Success)
    }
}
