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

package org.objectionary.deog.unit.graph.builder

import org.objectionary.deog.graph.GraphBuilder
import org.objectionary.deog.graph.repr.DGraphNode
import org.objectionary.deog.sources.SrsTransformed
import org.objectionary.deog.sources.XslTransformer
import org.objectionary.deog.unit.graph.TestBase
import org.apache.commons.io.FileUtils
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.ByteArrayOutputStream
import java.nio.file.Path

/**
 * Base class for graph builder testing
 */
open class BuilderTest : TestBase {
    private val postfix = "tmp"

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
    fun `graph builder test`(testName: String) {
        val sources = SrsTransformed(constructInPath(testName), XslTransformer(), postfix)
        val graph = GraphBuilder(sources.walk()).createGraph()
        val actual = stringOutput(graph.heads)
        val expected = constructOutPath(testName).toFile().bufferedReader().use { it.readText() }
        checkOutput(expected, actual, "In test: ${constructInPath(testName)}")
        FileUtils.deleteDirectory(sources.resPath.toFile())
    }

    override fun constructOutPath(directoryName: String): Path =
        Path.of("src${sep}test${sep}resources${sep}unit${sep}out${sep}builder$sep$directoryName.txt")

    private fun stringOutput(
        heads: MutableSet<DGraphNode>
    ): String {
        val out = ByteArrayOutputStream()
        heads.sortedBy { it.name }.forEach { printOutRecursive(it, out, mutableSetOf()) }
        return String(out.toByteArray())
    }

    private fun printOutRecursive(
        node: DGraphNode,
        out: ByteArrayOutputStream,
        nodes: MutableSet<DGraphNode>
    ) {
        out.write("NODE: name=\"${node.name}\"\n".toByteArray())
        if (!nodes.contains(node)) {
            nodes.add(node)
        } else {
            return
        }
        node.children.sortedBy { it.name }.forEach {
            out.write("${node.name} CHILD:\n".toByteArray())
            printOutRecursive(it, out, nodes)
        }
    }
}
