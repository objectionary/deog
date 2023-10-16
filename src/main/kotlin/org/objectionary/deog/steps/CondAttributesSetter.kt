package org.objectionary.deog.steps

import org.objectionary.deog.graph.repr.DGraphCondAttr
import org.objectionary.deog.graph.repr.DGraphCondNode
import org.objectionary.deog.graph.repr.DeogGraph
import org.objectionary.deog.graph.repr.DgNodeCondition
import org.objectionary.deog.util.containsAttr
import org.objectionary.deog.util.getAttr
import org.objectionary.deog.util.getAttrContent
import org.objectionary.deog.util.packageName
import org.w3c.dom.Node

/**
 * Class for processing conditional attributes
 *
 * @property deogGraph graph of the program
 */
class CondAttributesSetter(
    private val deogGraph: DeogGraph
) {
    private val conditions: MutableSet<Node> = mutableSetOf()

    /**
     * Aggregates the process of processing conditional attributes
     */
    fun processConditions() {
        collectConditions()
        processApplications()
    }

    private fun collectConditions() {
        val objects = deogGraph.initialObjects
        for (node in objects) {
            val base = node.getAttrContent("base") ?: continue
            if (base == ".if") {
                conditions.add(node)
            }
        }
    }

    private fun processApplications() {
        conditions.forEach { node ->
            var tmpNode = node.firstChild.nextSibling
            var line = tmpNode.getAttrContent("line")
            val cond: MutableList<Node> = mutableListOf(tmpNode)
            while (tmpNode.nextSibling.nextSibling.getAttrContent("line") == line) {
                cond.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = tmpNode.getAttrContent("line")
            }
            tmpNode = tmpNode.nextSibling.nextSibling
            val fstOption: MutableList<Node> = mutableListOf(tmpNode)
            while (tmpNode.nextSibling.nextSibling.getAttrContent("line") == line) {
                fstOption.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = tmpNode.getAttrContent("line")
            }
            tmpNode = tmpNode.nextSibling.nextSibling
            val sndOption: MutableList<Node> = mutableListOf(tmpNode)
            while (tmpNode.nextSibling.nextSibling.getAttrContent("line") == line) {
                sndOption.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = tmpNode.getAttrContent("line")
            }
            val igCond = DgNodeCondition(cond)
            traverseParents(node.parentNode, igCond.freeVars)
            node.getAttrContent("name")?.let { name ->
                if (name != "@") {
                    deogGraph.dgNodes.add(DGraphCondNode(node, node.packageName(), igCond, fstOption, sndOption))
                    val parent = deogGraph.dgNodes.find { it.body == node.parentNode }
                    parent?.attributes?.add(DGraphCondAttr(name, 0, node, igCond, fstOption, sndOption))
                } else {
                    val parent = deogGraph.dgNodes.find { it.body == node.parentNode }
                    parent?.attributes?.add(DGraphCondAttr(name, 0, node, igCond, fstOption, sndOption))
                }
            }
        }
    }

    private fun traverseParents(node: Node, freeVars: MutableSet<String>) {
        node.getAttr("abstract") ?: return
        var sibling = node.firstChild?.nextSibling
        while (!sibling.containsAttr("base") && !sibling.containsAttr("abstract") && sibling != null) {
            sibling.getAttrContent("name")?.let { freeVars.add(it) }
            sibling = sibling?.nextSibling
        }
        traverseParents(node.parentNode, freeVars)
    }
}
