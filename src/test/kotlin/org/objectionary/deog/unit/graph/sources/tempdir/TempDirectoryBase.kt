package org.objectionary.deog.unit.graph.sources.tempdir

import org.objectionary.deog.sources.SrsTransformed
import org.objectionary.deog.sources.XslTransformer
import org.objectionary.deog.unit.graph.TestBase
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Tests generation of temporary directory for modified xmir files
 */
open class TempDirectoryBase : TestBase {
    private val postfix = "tmp"

    /**
     * Tests different directories formats as input
     *
     * @param testName test name
     */
    @ParameterizedTest
    @CsvSource(
        value = [
            "basic_cycle",
            "basic_dir",
            "basic_tree",
            "closed_cycle",
            "inner",
            "inner_concrete",
            "inner_ordered",
            "multiple_aliases",
            "multiple_closed_cycles",
            "multiple_cycles",
            "multiple_trees",
            "tree",
            "triple_cycle"
        ], ignoreLeadingAndTrailingWhitespace = true
    )
    fun doTest(testName: String) {
        for (i in 0..2) {
            val path = constructInPath(getTestName()) + sep.toString().repeat(i)
            deleteTempDirIfExists(Path.of(path))
            SrsTransformed(Path.of(path), XslTransformer(), postfix).walk()
            checkIfTempDirExists(Path.of(path))
            deleteTempDirIfExists(Path.of(path))
        }
    }

    private fun deleteTempDirIfExists(pathToSource: Path) {
        val strPathToTemp = "${pathToSource}_$postfix"
        val tempDir = File(strPathToTemp)
        if (tempDir.exists() && tempDir.isDirectory) {
            tempDir.deleteRecursively()
        }
    }

    private fun checkIfTempDirExists(pathToSource: Path) {
        val strPathToTemp = "${pathToSource}_$postfix"
        val tempDir = File(strPathToTemp)
        assertTrue { tempDir.exists() }
    }

    override fun constructOutPath(directoryName: String) = ""
}
