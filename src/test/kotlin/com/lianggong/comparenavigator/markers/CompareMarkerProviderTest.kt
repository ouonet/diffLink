package com.lianggong.comparenavigator.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.junit.Assert.*

class CompareMarkerProviderTest : LightJavaCodeInsightFixtureTestCase() {

    private lateinit var markerProvider: CompareMarkerProvider

    override fun setUp() {
        super.setUp()
        markerProvider = CompareMarkerProvider()
    }

    fun testDetectValidCompareComment() {
        // Create a test file with a valid compare comment
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // #COMPARE: /TestFile.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        // Create the destination file
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        // Find the comment in the file
        val comment = PsiTreeUtil.findChildOfType(testFile, PsiComment::class.java)
        assertNotNull("Comment should be found in test file", comment)

        // Get the marker for this comment
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNotNull("Marker should be created for valid compare comment", marker)
        assertNotNull("Marker should have an icon", marker!!.icon)
    }

    fun testDetectMultipleCompareComments() {
        // Create a test file with multiple compare comments
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // #COMPARE: /File1.java
                    public void method1() {
                    }

                    // #COMPARE: /File2.java
                    public void method2() {
                    }
                }
            """.trimIndent()
        )

        // Create destination files
        myFixture.addFileToProject("File1.java", "public class File1 {}")
        myFixture.addFileToProject("File2.java", "public class File2 {}")

        // Find all comments
        val comments = PsiTreeUtil.findChildrenOfType(testFile, PsiComment::class.java)
        assertEquals("Should find 2 comments", 2, comments.size)

        // Check that both comments generate markers
        var markerCount = 0
        for (comment in comments) {
            val marker = markerProvider.getLineMarkerInfo(comment)
            if (marker != null) {
                markerCount++
            }
        }
        assertEquals("Both comments should generate markers", 2, markerCount)
    }

    fun testIgnoreNonCompareComments() {
        // Create a test file with a non-compare comment
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

        // Find the comment
        val comment = PsiTreeUtil.findChildOfType(testFile, PsiComment::class.java)
        assertNotNull("Comment should be found", comment)

        // Get marker - should be null for non-compare comments
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNull("Marker should not be created for non-compare comments", marker)
    }

    fun testCreateErrorMarkerForMissingFile() {
        // Create a test file with a compare comment pointing to non-existent file
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // #COMPARE: /NonExistent.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        // Find the comment
        val comment = PsiTreeUtil.findChildOfType(testFile, PsiComment::class.java)
        assertNotNull("Comment should be found", comment)

        // Get the marker - should be an error marker
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNotNull("Error marker should be created for missing file", marker)
        assertNotNull("Error marker should have an icon", marker!!.icon)
        // The icon should be the error icon
        assertEquals("Icon name should indicate error", "AllIcons.General.Error", marker.icon.toString())
    }

    fun testBlockCommentDetection() {
        // Create a test file with a block comment containing compare directive
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    /* #COMPARE: /TestFile.java */
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        // Create destination file
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        // Find the block comment
        val comments = PsiTreeUtil.findChildrenOfType(testFile, PsiComment::class.java)
        assertTrue("Should find at least one comment", comments.isNotEmpty())

        // Check if any comment generates a marker
        var markerFound = false
        for (comment in comments) {
            val marker = markerProvider.getLineMarkerInfo(comment)
            if (marker != null) {
                markerFound = true
                break
            }
        }
        assertTrue("Block comment with compare directive should generate marker", markerFound)
    }

    fun testWhitespaceHandling() {
        // Create a test file with various whitespace in the compare directive
        val testFile = myFixture.addFileToProject(
            "Test.java",
            """
                public class Test {
                    // #COMPARE:   /TestFile.java
                    public void method() {
                    }
                }
            """.trimIndent()
        )

        // Create destination file
        myFixture.addFileToProject("TestFile.java", "public class TestFile {}")

        // Find the comment
        val comment = PsiTreeUtil.findChildOfType(testFile, PsiComment::class.java)
        assertNotNull("Comment should be found", comment)

        // Get the marker - whitespace should be handled correctly
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNotNull("Marker should be created despite extra whitespace", marker)
        assertNotNull("Marker should have an icon", marker!!.icon)
    }
}
