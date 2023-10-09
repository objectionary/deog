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
import org.objectionary.deog.steps.processClosedCycles
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.nio.file.Path

typealias GraphAbstracts = MutableMap<String, MutableSet<Node>>

/**
 * Builds decoration hierarchy graph
 *
 * @todo #29:30m/DEV Refactor this class. Current version of GraphBuilder from ddr repository looks nicer.
 * It need to be simple copy-pasted with some fixes.
 */
class GraphBuilder(private val documents: MutableMap<Document, Path>) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
    private val abstracts: GraphAbstracts = mutableMapOf()

    /**
     * Graph of the program to be analysed
     */
    val deogGraph = DeogGraph()

    /**
     * Aggregates the process of graph creation:
     * Constructs inheritance graph, sets heads and leaves and processes cycles
     *
     * @return created graph
     */
    fun createGraph(): DeogGraph {
        try {
            constructInheritance()
            setLeaves()
            deogGraph.leaves.forEach { setHeads(it, mutableMapOf()) }
            val thinnedOutHeads: MutableSet<DGraphNode> = mutableSetOf()
            deogGraph.heads.forEach {
                val found = mutableListOf(false)
                thinOutHeads(it, thinnedOutHeads, mutableSetOf(), found)
                if (!found[0]) {
                    thinnedOutHeads.add(it)
                }
            }
            deogGraph.heads.clear()
            thinnedOutHeads.forEach { deogGraph.heads.add(it) }
            processClosedCycles(deogGraph)
        } catch (e: Exception) {
            logger.error(e.printStackTrace().toString())
        }
        return deogGraph
    }
    @Suppress("PARAMETER_NAME_IN_OUTER_LAMBDA")
    private fun abstracts(objects: MutableList<Node>, packageName: String) =
        objects.forEach {
            val name = name(it)
            if (abstract(it) != null && name != null) {
                abstracts.getOrPut(name) { mutableSetOf() }.add(it)
                deogGraph.dgNodes.add(DGraphNode(it, packageName))
            }
        }

    private fun getAbstractViaRef(
        baseName: String?,
        baseRef: String?
    ): Node? =
        if (baseName != null && abstracts.contains(baseName)) {
            abstracts[baseName]!!.find {
                line(it) == baseRef
            }
        } else {
            null
        }

    private fun getAbstractViaPackage(baseNodeName: String?): DGraphNode? {
        val packageName = baseNodeName?.substringBeforeLast('.')
        val nodeName = baseNodeName?.substringAfterLast('.')
        return deogGraph.dgNodes.find { it.name.equals(nodeName) && it.packageName == packageName }
    }

    private fun constructInheritance() {
        documents.forEach {
            val objects: MutableList<Node> = mutableListOf()
            val docObjects = it.key.getElementsByTagName("o")
            val packageName = packageName(docObjects.item(0))
            for (i in 0 until docObjects.length) {
                objects.add(docObjects.item(i))
            }
            abstracts(objects, packageName)
            deogGraph.initialObjects.addAll(objects)
        }
        for (node in deogGraph.initialObjects) {
            val name = name(node) ?: continue
            if (name == "@") {
                // check that @ attribute's base has an abstract object in this program
                val baseNodeName = base(node)
                val baseNodeRef = ref(node)
                val abstractBaseNode =
                    getAbstractViaRef(baseNodeName, baseNodeRef) ?: getAbstractViaPackage(baseNodeName)?.body
                abstractBaseNode?.let {
                    val parentNode = node.parentNode ?: return
                    deogGraph.dgNodes.find { it.body.attributes == parentNode.attributes }
                        ?: run { deogGraph.dgNodes.add(DGraphNode(parentNode, packageName(parentNode))) }
                    val igChild = deogGraph.dgNodes.find { it.body.attributes == parentNode.attributes }!!
                    deogGraph.dgNodes.find { it.body.attributes == abstractBaseNode.attributes }
                        ?: run { deogGraph.dgNodes.add(DGraphNode(abstractBaseNode, packageName(abstractBaseNode))) }
                    val dgParent = deogGraph.dgNodes.find { it.body.attributes == abstractBaseNode.attributes }!!
                    deogGraph.connect(igChild, dgParent)
                }
            }
        }
    }

    private fun checkNodes(node: DGraphNode): DGraphNode? =
        deogGraph.dgNodes.find { it.body.attributes == node.body.attributes }

    private fun setLeaves() =
        deogGraph.dgNodes.filter { it.children.isEmpty() }.forEach { deogGraph.leaves.add(it) }

    private fun setHeads(
        node: DGraphNode,
        visited: MutableMap<DGraphNode, Boolean>
    ) {
        if (visited.containsKey(node) || node.parents.isEmpty()) {
            deogGraph.heads.add(node)
        } else {
            visited[node] = true
            node.parents.forEach { setHeads(it, visited) }
        }
    }

    private fun thinOutHeads(
        node: DGraphNode,
        toBeRemoved: MutableSet<DGraphNode>,
        visited: MutableSet<DGraphNode>,
        found: MutableList<Boolean>
    ) {
        if (found[0] || toBeRemoved.contains(node)) {
            found[0] = true
            return
        }
        if (visited.contains(node)) {
            toBeRemoved.add(node)
            found[0] = true
            return
        }
        visited.add(node)
        node.children.forEach { thinOutHeads(it, toBeRemoved, visited, found) }
    }
}
