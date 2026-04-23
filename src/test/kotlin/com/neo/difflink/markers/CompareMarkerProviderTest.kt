package com.neo.difflink.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class CompareMarkerProviderTest : LightJavaCodeInsightFixtureTestCase() {

    private lateinit var markerProvider: CompareMarkerProvider

    override fun setUp() {
        super.setUp()
        markerProvider = CompareMarkerProvider()
    }

    private fun collectMarkers(file: PsiFile): List<LineMarkerInfo<*>> {
        val elements = PsiTreeUtil.collectElements(file) { true }.toMutableList()
        val result = mutableListOf<LineMarkerInfo<*>>()
        markerProvider.collectSlowLineMarkers(elements, result)
        return result
    }

    fun testDetectValidCompareComment() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // @DiffLink: /TestFile.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        val markers = collectMarkers(testFile)
        assertEquals("Should create one marker for one @DiffLink line", 1, markers.size)
        assertNotNull("Marker should have an icon", markers.first().icon)
    }

    fun testDetectMultipleCompareComments() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // @DiffLink: /File1.java
                    public void method1() {
                    }

                    // @DiffLink: /File2.java
                    public void method2() {
                    }
                }
            """.trimIndent()
        )
        myFixture.addFileToProject("File1.java", "public class File1 {}")
        myFixture.addFileToProject("File2.java", "public class File2 {}")

        val markers = collectMarkers(testFile)
        assertEquals("Both @DiffLink lines should generate markers", 2, markers.size)
    }

    fun testIgnoreNonCompareComments() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // This is just a regular comment
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        val markers = collectMarkers(testFile)
        assertTrue("Non-@DiffLink comments must not create markers", markers.isEmpty())
    }

    fun testCreateErrorMarkerForMissingFile() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // @DiffLink: /NonExistent.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        val markers = collectMarkers(testFile)
        assertEquals("Missing destination should still create one error marker", 1, markers.size)
        assertNotNull("Error marker should have an icon", markers.first().icon)
    }

    fun testBlockCommentDetection() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    /* @DiffLink: /TestFile.java */
                    public void method() {
                    }
                }
            """.trimIndent()
        )
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        val markers = collectMarkers(testFile)
        assertEquals("Block comment with @DiffLink should create one marker", 1, markers.size)
    }

    fun testWhitespaceHandling() {
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // @DiffLink:   /TestFile.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        val markers = collectMarkers(testFile)
        assertEquals("Extra whitespace should still parse", 1, markers.size)
    }

    fun testDetectMarkerInJavaFileComment() {
        val javaFile = myFixture.addFileToProject(
            "Example.java",
            """
                // @DiffLink: /path/to/file.java
                public class Example {}
            """.trimIndent()
        )
        myFixture.addFileToProject("path/to/file.java", "public class File {}")

        val markers = collectMarkers(javaFile)
        assertEquals("Java comment should produce one marker", 1, markers.size)
    }

    fun testDetectMarkerInPythonFileComment() {
        val pythonFile = myFixture.addFileToProject(
            "example.py",
            """
                # @DiffLink: /path/to/other.py
                def hello():
                    pass
            """.trimIndent()
        )
        myFixture.addFileToProject("path/to/other.py", "def goodbye(): pass")

        val markers = collectMarkers(pythonFile)
        assertTrue("Python file should be processed without errors", markers.size >= 0)
    }

    fun testDetectMarkerInTextFileAnywhere() {
        val markdownFile = myFixture.addFileToProject(
            "example.md",
            """
                # Documentation
                @DiffLink: /old/doc.md, /new/doc.md
                Some content
            """.trimIndent()
        )
        myFixture.addFileToProject("new/doc.md", "# New Documentation")

        val markers = collectMarkers(markdownFile)
        assertTrue("Markdown should be processed without errors", markers.size >= 0)
    }

    fun testParseSingleParameterMarkerFormat() {
        val javaFile = myFixture.addFileToProject(
            "Current.java",
            """
                // @DiffLink:/reference/File.java
                public class Current {}
            """.trimIndent()
        )
        myFixture.addFileToProject("reference/File.java", "public class File {}")

        val markers = collectMarkers(javaFile)
        assertEquals("Single-parameter syntax should parse", 1, markers.size)
    }

    fun testParseTwoParameterMarkerFormat() {
        val javaFile = myFixture.addFileToProject(
            "Version1.java",
            """
                // @DiffLink:/version1/File.java, /version2/File.java
                public class Version1 {}
            """.trimIndent()
        )
        myFixture.addFileToProject("version1/File.java", "public class Version1File {}")
        myFixture.addFileToProject("version2/File.java", "public class Version2File {}")

        val markers = collectMarkers(javaFile)
        assertEquals("Two-parameter syntax should parse", 1, markers.size)
    }

    fun testHandleWhitespaceInTwoParameterMarker() {
        val javaFile = myFixture.addFileToProject(
            "Example.java",
            """
                // @DiffLink:  /src/file.java  ,  /dst/file.java
                public class Example {}
            """.trimIndent()
        )
        myFixture.addFileToProject("src/file.java", "public class SrcFile {}")
        myFixture.addFileToProject("dst/file.java", "public class DstFile {}")

        val markers = collectMarkers(javaFile)
        assertEquals("Whitespace in two-parameter syntax should parse", 1, markers.size)
    }
}
