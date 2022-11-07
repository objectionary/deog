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

package org.objectionary.deog

import org.objectionary.deog.repr.DGraphNode
import org.objectionary.deog.repr.DeogGraph

/**
 * Processes decoration cycles
 *
 * @param deogGraph graph to be analysed
 * @throws IllegalStateException if after the analysis it turns out that not all nodes of the graph were traversed
 */
@Throws(IllegalStateException::class)
internal fun processClosedCycles(deogGraph: DeogGraph) {
    val reached: MutableMap<DGraphNode, Boolean> = mutableMapOf()
    deogGraph.dgNodes.forEach { reached[it] = false }
    deogGraph.heads.forEach { dfsReachable(it, reached) }
    for (entry in reached) {
        if (!entry.value) {
            deogGraph.heads.add(entry.key)
            traverseCycles(entry.key, reached)
        }
    }
    if (deogGraph.dgNodes.size != reached.filter { it.value }.size) {
        throw IllegalStateException(
            "Not all nodes of inheritance graph were reached.\n " +
                    "Graph size: ${deogGraph.dgNodes.size}, reached nodes: ${reached.filter { it.value }.size}"
        )
    }
}

private fun dfsReachable(
    node: DGraphNode,
    reached: MutableMap<DGraphNode, Boolean>
) {
    if (reached[node] == true) {
        return
    } else {
        reached[node] = true
    }
    node.children.forEach { dfsReachable(it, reached) }
}

private fun traverseCycles(
    node: DGraphNode,
    reached: MutableMap<DGraphNode, Boolean>
) {
    if (reached[node]!!) {
        return
    }
    reached[node] = true
    node.children.forEach { traverseCycles(it, reached) }
}
