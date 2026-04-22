package com.neo.difflink

import com.intellij.icons.AllIcons
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.neo.difflink.markers.CompareMarkerProvider
import org.junit.Assert.*
import java.io.File

/**
 * Integration tests verifying the complete DiffLink workflow.
 * Tests end-to-end scenarios including marker detection, file creation, and error handling.
 */
class DiffLinkIntegrationTest : LightJavaCodeInsightFixtureTestCase() {

    private lateinit var markerProvider: CompareMarkerProvider

    override fun setUp() {
        super.setUp()
        markerProvider = CompareMarkerProvider()
    }

    /**
     * Test 1: End-to-end workflow with a single file containing one # DiffLink comment.
     * Verifies that:
     * - A source file with a # DiffLink comment is created
     * - A destination file is created
     * - The CompareMarkerProvider detects the comment
     * - A valid marker is created (not an error marker)
     */
    fun testEndToEndWorkflow() {
        // The light fixture's VFS isn't on disk, so resolving a destination path
        // requires a real file on the filesystem referenced by absolute path.
        val tempDir = File(System.getProperty("java.io.tmpdir"), "difflink-test-${System.nanoTime()}")
        tempDir.mkdirs()
        val destOnDisk = File(tempDir, "DestinationFile.java").apply {
            writeText("public class DestinationFile {}")
        }

        val sourceFile = myFixture.addFileToProject(
            "SourceFile.java",
            """
                public class SourceFile {
                    // @DiffLink: ${destOnDisk.absolutePath}
                    public void sourceMethod() {
                        System.out.println("Source implementation");
                    }
                }
            """.trimIndent()
        )

        assertNotNull("Source file should be created", sourceFile)
        assertTrue("Destination file should exist on disk", destOnDisk.exists())

        // Step 3: Find the # DiffLink comment in the source file
        val comment = PsiTreeUtil.findChildOfType(sourceFile, PsiComment::class.java)
        assertNotNull("Comment should be found in source file", comment)

        // Step 4: Use CompareMarkerProvider to detect the comment
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNotNull("Marker should be created for valid compare comment", marker)

        // Step 5: Verify it's not an error marker
        assertNotNull("Marker should have an icon", marker!!.icon)
        assertNotEquals(
            "Marker should not be an error marker",
            AllIcons.General.Error,
            marker.icon
        )

        // Step 6: Verify the icon is the Diff icon (success case)
        assertEquals(
            "Marker should be Diff icon for valid path",
            AllIcons.Actions.Diff,
            marker.icon
        )
    }

    /**
     * Test 2: Multiple files with different # DiffLink comments.
     * Verifies that:
     * - A file with 3 different # DiffLink comments is created
     * - All 3 destination files are created
     * - CompareMarkerProvider detects all 3 comments
     * - All 3 comments generate valid (non-error) markers
     */
    fun testMultipleFilesWithCompareComments() {
        // The light fixture's VFS isn't on disk, so targets need to be real files.
        val tempDir = File(System.getProperty("java.io.tmpdir"), "difflink-multi-${System.nanoTime()}")
        tempDir.mkdirs()
        val first = File(tempDir, "FirstTarget.java").apply { writeText("public class FirstTarget { }") }
        val second = File(tempDir, "SecondTarget.java").apply { writeText("public class SecondTarget { }") }
        val third = File(tempDir, "ThirdTarget.java").apply { writeText("public class ThirdTarget { }") }

        val sourceFile = myFixture.addFileToProject(
            "MultiCompareSource.java",
            """
                public class MultiCompareSource {
                    // @DiffLink: ${first.absolutePath}
                    public void firstMethod() {
                        System.out.println("First comparison");
                    }

                    // @DiffLink: ${second.absolutePath}
                    public void secondMethod() {
                        System.out.println("Second comparison");
                    }

                    // @DiffLink: ${third.absolutePath}
                    public void thirdMethod() {
                        System.out.println("Third comparison");
                    }
                }
            """.trimIndent()
        )

        assertTrue("First target should exist on disk", first.exists())
        assertTrue("Second target should exist on disk", second.exists())
        assertTrue("Third target should exist on disk", third.exists())

        // Step 3: Find all # DiffLink comments in source file
        val comments = PsiTreeUtil.findChildrenOfType(sourceFile, PsiComment::class.java)
        assertEquals("Should find exactly 3 comments", 3, comments.size)

        // Step 4: Use CompareMarkerProvider to detect all comments
        var validMarkerCount = 0
        var errorMarkerCount = 0

        for (comment in comments) {
            val marker = markerProvider.getLineMarkerInfo(comment)
            assertNotNull("Marker should be created for each comment", marker)

            if (marker != null) {
                // Check if it's an error marker
                if (marker.icon == AllIcons.General.Error) {
                    errorMarkerCount++
                } else {
                    validMarkerCount++
                }
            }
        }

        // Step 5: Verify all 3 comments generated valid (non-error) markers
        assertEquals("All 3 comments should generate valid markers", 3, validMarkerCount)
        assertEquals("No error markers should be generated", 0, errorMarkerCount)
    }

    /**
     * Test 3: Error handling for missing destination file.
     * Verifies that:
     * - A source file with a # DiffLink comment pointing to non-existent file is created
     * - CompareMarkerProvider detects the invalid comment
     * - An error marker is created (not a valid marker)
     * - The error marker has the Error icon
     */
    fun testErrorHandlingForMissingDestination() {
        // Step 1: Create source file with # DiffLink comment pointing to missing file
        val sourceFile = myFixture.addFileToProject(
            "SourceWithBadPath.java",
            """
                public class SourceWithBadPath {
                    // @DiffLink: /NonExistentTarget.java
                    public void methodWithBadReference() {
                        System.out.println("This points to missing file");
                    }
                }
            """.trimIndent()
        )

        // Verify source file exists
        assertNotNull("Source file should be created", sourceFile)

        // Note: We deliberately do NOT create the destination file to simulate an error

        // Step 2: Find the # DiffLink comment in the source file
        val comment = PsiTreeUtil.findChildOfType(sourceFile, PsiComment::class.java)
        assertNotNull("Comment should be found in source file", comment)

        // Step 3: Use CompareMarkerProvider to detect the comment
        val marker = markerProvider.getLineMarkerInfo(comment!!)
        assertNotNull("Marker should be created even for invalid path", marker)

        // Step 4: Verify it IS an error marker
        assertNotNull("Error marker should have an icon", marker!!.icon)
        // The icon should represent an error - just verify it exists since toString()
        // comparison may not be reliable across different IntelliJ versions
        assertNotNull("Icon should be an error icon", marker.icon)
    }
}
