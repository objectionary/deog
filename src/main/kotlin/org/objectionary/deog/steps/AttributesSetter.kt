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

package org.objectionary.deog.steps

import org.objectionary.deog.graph.repr.DGraphAttr
import org.objectionary.deog.graph.repr.DGraphNode
import org.objectionary.deog.graph.repr.DeogGraph
import org.objectionary.deog.util.getAttr
import org.objectionary.deog.util.getAttrContent
import org.w3c.dom.Node

/**
 * Sets all default attributes of nodes and propagates attributes through the [deogGraph]
 */
class AttributesSetter(private val deogGraph: DeogGraph) {
    /**
     * Aggregate the process of attributes pushing
     */
    fun setAttributes() {
        setDefaultAttributes()
        pushAttributes()
        // processFreeVars()
    }

    /**
     * Add all already existent attributes to attributes list of the node
     */
    private fun setDefaultAttributes() {
        deogGraph.dgNodes.forEach { node ->
            val attributes = node.body.childNodes
            for (j in 0 until attributes.length) {
                val attr: Node = attributes.item(j)
                attr.getAttr("abstract")?.let {
                    attr.getAttrContent("name")?.let {
                        node.attributes.add(DGraphAttr(attr.getAttrContent("name")!!, 0, attr))
                    }
                }
            }
        }
    }

    /**
     * Push attributes from parents to children
     */
    private fun pushAttributes(): Unit = deogGraph.heads.forEach { dfsPush(it, null, mutableMapOf()) }

    private fun dfsPush(
        node: DGraphNode,
        parent: DGraphNode?,
        visited: MutableMap<DGraphNode, Int>
    ) {
        if (visited[node] == 2) {
            return
        }
        if (visited[node] == 1) {
            visited[node] = 2
        } else {
            visited[node] = 1
        }
        parent?.attributes?.filter { pa ->
            node.attributes.none { na -> na.name == pa.name }
        }?.forEach {
            node.attributes.add(DGraphAttr(it.name, it.parentDistance + 1, it.body))
        }
        node.children.forEach { dfsPush(it, node, visited) }
    }
}
