package com.neo.difflink.actions

import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import org.mockito.Mockito.mock

class CompareActionHandlerTest : LightJavaCodeInsightFixtureTestCase() {

    fun testNavigateToComparisonCreatesExpectedDiffRequest() {
        var capturedRequest: SimpleDiffRequest? = null
        val handler = CompareActionHandler { _, request ->
            capturedRequest = request
        }

        // Create two test files
        val sourceFile = myFixture.addFileToProject(
            "SourceFile.java",
            """
                public class SourceFile {
                    public void method() {
                        System.out.println("source");
                    }
                }
            """.trimIndent()
        ).virtualFile

        val destinationFile = myFixture.addFileToProject(
            "DestinationFile.java",
            """
                public class DestinationFile {
                    public void method() {
                        System.out.println("destination");
                    }
                }
            """.trimIndent()
        ).virtualFile

        // Ensure files are created
        assertNotNull("Source file should be created", sourceFile)
        assertNotNull("Destination file should be created", destinationFile)
        assertEquals("Source file name should match", "SourceFile.java", sourceFile.name)
        assertEquals("Destination file name should match", "DestinationFile.java", destinationFile.name)

        handler.navigateToComparison(sourceFile, destinationFile, project)

        val request = capturedRequest
        assertNotNull("Diff request should be captured", request)
        assertEquals("Request title should match", "Compare Files", request!!.title)
        assertEquals("Left content title should be source file", "SourceFile.java", request.contentTitles[0])
        assertEquals("Right content title should be destination file", "DestinationFile.java", request.contentTitles[1])
    }

    fun testActionPerformed() {
        val handler = CompareActionHandler()
        // Test that the action can be invoked without errors
        val mockEvent = mock(AnActionEvent::class.java)

        // Call actionPerformed - should not throw exception
        try {
            handler.actionPerformed(mockEvent)
            // Test passes if no exception is thrown
            // Note: Current implementation has empty actionPerformed, but it should not crash
            assertTrue("actionPerformed should complete successfully", true)
        } catch (e: Exception) {
            fail("actionPerformed should not throw exception: ${e.message}")
        }
    }

    fun testNavigateToComparisonSwallowsShowDiffFailure() {
        val handler = CompareActionHandler { _, _ ->
            throw RuntimeException("boom")
        }

        val sourceFile = myFixture.addFileToProject("Source.java", "public class Source { }").virtualFile
        val destFile = myFixture.addFileToProject("Dest.java", "public class Destination { }").virtualFile

        try {
            handler.navigateToComparison(sourceFile, destFile, project)
            assertTrue("navigateToComparison should swallow showDiff failures", true)
        } catch (e: Exception) {
            fail("navigateToComparison should not throw when showDiff fails: ${e.message}")
        }
    }
}
