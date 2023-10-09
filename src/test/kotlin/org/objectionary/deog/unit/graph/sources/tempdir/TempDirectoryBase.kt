package org.objectionary.deog.unit.graph.sources.tempdir

import org.objectionary.deog.sources.SrsTransformed
import org.objectionary.deog.sources.XslTransformer
import org.objectionary.deog.unit.graph.TestBase
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Tests generation of temporary directory for modified xmir files
 */
open class TempDirectoryBase : TestBase {
    private val postfix = "tmp"
    override fun doTest() {
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
