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
import org.w3c.dom.Node

/**
 * Finds abstract attribute of the [node]
 *
 * @param node to be analysed
 * @return found abstract name
 */
fun abstract(node: Node?) = node?.attributes?.getNamedItem("abstract")

/**
 * Finds name attribute of the [node]
 *
 * @param node to be analysed
 * @return found name
 */
fun name(node: Node?) = node?.attributes?.getNamedItem("name")?.textContent

/**
 * Finds base attribute of the [node]
 *
 * @param node to be analysed
 * @return found base
 */
fun base(node: Node?) = node?.attributes?.getNamedItem("base")?.textContent

/**
 * Finds ref attribute of the [node]
 *
 * @param node to be analysed
 * @return found ref
 */
fun ref(node: Node?) = node?.attributes?.getNamedItem("ref")?.textContent

/**
 * Finds line attribute of the [node]
 *
 * @param node to be analysed
 * @return found line
 */
fun line(node: Node?) = node?.attributes?.getNamedItem("line")?.textContent

/**
 * Finds pos attribute of the [node]
 *
 * @param node to be analysed
 * @return found pos
 */
fun pos(node: Node?) = node?.attributes?.getNamedItem("pos")?.textContent

/**
 * Finds package name of the [node]
 *
 * @param node to be analysed
 * @return found package name
 */
fun packageName(node: Node?): String {
    val heads = node?.ownerDocument?.getElementsByTagName("head") ?: return ""
    for (i in 0 until heads.length) {
        val head = heads.item(i)
        if (head.textContent.equals("package")) {
            return head.nextSibling.nextSibling.textContent
        }
    }
    return ""
}

/**
 * Finds atom attribute of the [node]
 *
 * @param node to be analysed
 * @return found atom name
 */
fun atom(node: Node?) = node?.attributes?.getNamedItem("atom")?.textContent

/**
 * Either finds an abstract object that the [node] is referring to or looks for the abstract node in the packages
 *
 * @param node node to be handled
 * @param objects list of xml objects
 * @param deogGraph graph
 * @return node found through refs
 */
@Suppress("AVOID_NULL_CHECKS")
fun findRef(
    node: Node?,
    objects: MutableSet<Node>,
    deogGraph: DeogGraph
): Node? {
    val ref = ref(node) ?: return getAbstractViaPackage(base(node), deogGraph)?.body
    objects.forEach {
        if (line(it) == ref) {
            if (abstract(it) != null && packageName(node) == packageName(it)) {
                return it
            }
            if (abstract(it) == null && packageName(node) == packageName(it)) {
                val traversed = walkDotChain(it)
                return if (traversed == null) {
                    findRef(it, objects, deogGraph)
                } else {
                    findRef(traversed, objects, deogGraph)
                }
            }
        }
    }
    return null
}

private fun walkDotChain(
    node: Node
): Node? {
    var sibling = node.nextSibling?.nextSibling
    while (base(sibling)?.startsWith(".") == true) {
        sibling = sibling?.nextSibling
        sibling?.attributes ?: run { sibling = sibling?.nextSibling }
    }
    sibling = sibling?.previousSibling
    sibling?.attributes ?: run { sibling = sibling?.previousSibling }
    return sibling
}

private fun getAbstractViaPackage(baseNodeName: String?, deogGraph: DeogGraph): DGraphNode? {
    val packageName = baseNodeName?.substringBeforeLast('.')
    val nodeName = baseNodeName?.substringAfterLast('.')
    return deogGraph.dgNodes.find { it.name.equals(nodeName) && it.packageName == packageName }
}
