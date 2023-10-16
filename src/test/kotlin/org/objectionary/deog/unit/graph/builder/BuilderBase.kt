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
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Base class for graph builder testing
 */
open class BuilderBase : TestBase {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val postfix = "tmp"

    override fun doTest() {
        val path = getTestName()
        val sources = SrsTransformed(Path.of(constructInPath(path)), XslTransformer(), postfix)
        val graph = GraphBuilder(sources.walk()).createGraph()
        val out = ByteArrayOutputStream()
        graph.heads.sortedBy { it.name }.forEach { printOut(it, out, mutableSetOf()) }
        val actual = String(out.toByteArray())
        val bufferedReader: BufferedReader = File(constructOutPath(path)).bufferedReader()
        val expected = bufferedReader.use { it.readText() }
        logger.debug(actual)
        checkOutput(expected, actual)
        try {
            val tmpDir =
                Paths.get("${constructInPath(path)}_$postfix").toString()
            FileUtils.deleteDirectory(File(tmpDir))
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
        }
    }

    override fun constructOutPath(directoryName: String): String =
        "src${sep}test${sep}resources${sep}unit${sep}out${sep}builder$sep$directoryName.txt"

    private fun printOut(
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
            printOut(it, out, nodes)
        }
    }
}
