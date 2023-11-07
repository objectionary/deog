/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Olesia Subbotina
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.objectionary.deog.unit.graph.inner

import org.objectionary.deog.graph.GraphBuilder
import org.objectionary.deog.graph.repr.DGraphNode
import org.objectionary.deog.sources.SrsTransformed
import org.objectionary.deog.sources.XslTransformer
import org.objectionary.deog.steps.AttributesSetter
import org.objectionary.deog.steps.InnerPropagator
import org.objectionary.deog.unit.graph.TestBase
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Base class for inner attributes propagation testing
 */
open class InnerTest : TestBase {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val postfix = "tmp"

    @ParameterizedTest
    @CsvSource(value = [
        "basic_dir",
        "condition",
        "creations",
        "inner",
        "inner_concrete",
        "inner_ordered",
        "inner_prop",
        "multiple_aliases",
        "multiple_closed_cycles",
        "multiple_cycles",
        "multiple_trees",
    ], ignoreLeadingAndTrailingWhitespace = true)
    fun doTest(testName: String) {
        if (testName == "condition") {
            Assumptions.assumeFalse(true, "Test ignored: condition")
        }
        val sources = SrsTransformed(Path.of(constructInPath(testName)), XslTransformer(), postfix)
        val graph = GraphBuilder(sources.walk()).createGraph()
        val attributesSetter = AttributesSetter(graph)
        attributesSetter.setAttributes()
        val innerPropagator = InnerPropagator(graph)
        innerPropagator.propagateInnerAttrs()
        val out = ByteArrayOutputStream()
        printOut(out, graph.dgNodes)
        val actual = String(out.toByteArray())
        val bufferedReader: BufferedReader = File(constructOutPath(testName)).bufferedReader()
        val expected = bufferedReader.use { it.readText() }
        checkOutput(expected, actual, "In test: ${Path.of(constructInPath(testName))}")
        try {
            val tmpDir =
                Paths.get("${constructInPath(testName)}_$postfix").toString()
            FileUtils.deleteDirectory(File(tmpDir))
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
        }
    }

    override fun constructOutPath(directoryName: String): String =
        "src${sep}test${sep}resources${sep}unit${sep}out${sep}inner$sep$directoryName.txt"

    private fun printOut(
        out: ByteArrayOutputStream,
        nodes: Set<DGraphNode>
    ) {
        nodes.sortedBy { it.name }.forEach { node ->
            out.write("NODE: ${node.name} ATTRIBUTES:\n".toByteArray())
            node.attributes.forEach { out.write("name=${it.name}, dist=${it.parentDistance}\n".toByteArray()) }
        }
    }
}
