package org.objectionary.deog.steps

import org.objectionary.deog.graph.repr.DGraphAttr
import org.objectionary.deog.graph.repr.DGraphNode
import org.objectionary.deog.graph.repr.DeogGraph
import org.objectionary.deog.util.containsAttr
import org.objectionary.deog.util.findRef
import org.objectionary.deog.util.getAttr
import org.objectionary.deog.util.getAttrContent
import org.objectionary.deog.util.packageName
import org.w3c.dom.Node

typealias Abstracts = MutableMap<String, MutableSet<DGraphNode>>

/**
 * Propagates inner attributes
 */
class InnerPropagator(
    private val deogGraph: DeogGraph
) {
    private val decorators: MutableMap<DGraphNode, Boolean> = mutableMapOf()
    private val abstracts: Abstracts = mutableMapOf()

    /**
     * Propagates attributes of objects that are defined not in the global scope, but inside other objects
     */
    fun propagateInnerAttrs() {
        collectDecorators()
        processDecorators()
    }

    private fun collectDecorators() {
        val objects = deogGraph.initialObjects
        for (node in objects) {
            val name = node.getAttrContent("name")
            if (name != null && name == "@") {
                decorators[DGraphNode(node, node.packageName())] = false
            }
            if (node.containsAttr("abstract") && name != null) {
                abstracts.getOrPut(name) { mutableSetOf() }.add(DGraphNode(node, node.packageName()))
            }
        }
    }

    /**
     * @todo #34:30min this solution is naive, optimize it (see snippet below)
     *   while (decorators.containsValue(false)) { ... }
     *   (this todo has moved here from ddr repository)
     */
    @Suppress("MAGIC_NUMBER")
    private fun processDecorators() {
        val repetitions = 5
        for (i in 0..repetitions) {
            decorators.filter { !it.value }.forEach {
                getBaseAbstract(it.key)
            }
        }
    }

    /**
     * @return abstract object corresponding to the base attribute of the [key] node
     */
    @Suppress("AVOID_NULL_CHECKS")
    private fun getBaseAbstract(key: DGraphNode) {
        var tmpKey = key.body
        while (tmpKey.getAttrContent("base")?.startsWith('.') == true) {
            if (tmpKey.previousSibling.previousSibling == null) {
                break
            }
            tmpKey = tmpKey.previousSibling.previousSibling
        }
        when (tmpKey.getAttrContent("base")) {
            "^" -> {
                val abstract = tmpKey.parentNode.parentNode
                resolveAttrs(tmpKey, abstract, key)
            }

            "$" -> {
                val abstract = tmpKey
                resolveAttrs(tmpKey, abstract, key)
            }

            else -> {
                val abstract = resolveRefs(tmpKey) ?: return
                resolveAttrs(tmpKey, abstract, key)
            }
        }
    }

    /**
     * Finds an actual definition of an object that was copied into given [node]
     */
    private fun resolveRefs(node: Node): Node? {
        node.getAttr("abstract")?.let { return node }
        return findRef(node, deogGraph.initialObjects, deogGraph)
    }

    /**
     * Goes back through the chain of dot notations and propagates
     * all the attributes of the applied node into the current object
     */
    private fun resolveAttrs(
        node: Node,
        abstract: Node,
        key: DGraphNode
    ) {
        var tmpAbstract = deogGraph.dgNodes.find { it.body == abstract } ?: return
        var tmpNode: Node? = node.nextSibling.nextSibling ?: return
        while (tmpAbstract.body.getAttrContent("name") != key.body.getAttrContent("base")?.substring(1)) {
            tmpAbstract = deogGraph.dgNodes.find { graphNode ->
                tmpAbstract.attributes.find {
                    tmpNode.getAttrContent("base")?.substring(1) == it.body.getAttrContent("name")
                }?.body == graphNode.body
            } ?: return
            tmpNode = tmpNode?.nextSibling?.nextSibling
        }
        val parent = node.parentNode ?: return
        deogGraph.dgNodes.find { it.body == parent } ?: run {
            deogGraph.dgNodes.add(
                DGraphNode(
                    parent,
                    parent.packageName()
                )
            )
        }
        val dgParent = deogGraph.dgNodes.find { it.body == parent } ?: return
        tmpAbstract.attributes.forEach { graphNode ->
            dgParent.attributes.find { graphNode.body == it.body }
                ?: dgParent.attributes.add(DGraphAttr(graphNode.name, graphNode.parentDistance + 1, graphNode.body))
        }
        deogGraph.connect(dgParent, tmpAbstract)
        return
    }
}
