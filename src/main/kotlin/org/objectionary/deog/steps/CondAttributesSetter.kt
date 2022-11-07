package org.objectionary.deog.steps

import org.objectionary.deog.*
import org.objectionary.deog.repr.DGraphCondAttr
import org.objectionary.deog.repr.DGraphCondNode
import org.objectionary.deog.repr.DeogGraph
import org.objectionary.deog.repr.DgNodeCondition
import org.w3c.dom.Node

/**
 * Class for processing conditional attributes
 *
 * @property deogGraph graph of the program
 */
internal class CondAttributesSetter(
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
            val base = base(node) ?: continue
            if (base == ".if") {
                conditions.add(node)
            }
        }
    }

    private fun processApplications() {
        conditions.forEach { node ->
            var tmpNode = node.firstChild.nextSibling
            var line = line(tmpNode)
            val cond: MutableList<Node> = mutableListOf(tmpNode)
            while (line(tmpNode.nextSibling.nextSibling) == line) {
                cond.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = line(tmpNode)
            }
            tmpNode = tmpNode.nextSibling.nextSibling
            val fstOption: MutableList<Node> = mutableListOf(tmpNode)
            while (line(tmpNode.nextSibling.nextSibling) == line) {
                fstOption.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = line(tmpNode)
            }
            tmpNode = tmpNode.nextSibling.nextSibling
            val sndOption: MutableList<Node> = mutableListOf(tmpNode)
            while (line(tmpNode.nextSibling.nextSibling) == line) {
                sndOption.add(tmpNode.nextSibling.nextSibling)
                tmpNode = tmpNode.nextSibling.nextSibling
                line = line(tmpNode)
            }
            val dgCond = DgNodeCondition(cond)
            traverseParents(node.parentNode, dgCond.freeVars)
            name(node)?.let { name ->
                if (name != "@") {
                    deogGraph.dgNodes.add(DGraphCondNode(node, packageName(node), dgCond, fstOption, sndOption))
                    val parent = deogGraph.dgNodes.find { it.body == node.parentNode }
                    parent?.attributes?.add(DGraphCondAttr(name, 0, node, dgCond, fstOption, sndOption))
                } else {
                    val parent = deogGraph.dgNodes.find { it.body == node.parentNode }
                    parent?.attributes?.add(DGraphCondAttr(name, 0, node, dgCond, fstOption, sndOption))
                }
            }
        }
    }

    private fun traverseParents(node: Node, freeVars: MutableSet<String>) {
        abstract(node) ?: return
        var sibling = node.firstChild?.nextSibling
        while (base(sibling) == null && abstract(sibling) == null && sibling != null) {
            name(sibling)?.let { freeVars.add(it) }
            sibling = sibling?.nextSibling
        }
        traverseParents(node.parentNode, freeVars)
    }
}
