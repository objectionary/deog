package org.objectionary.deog.steps

import org.objectionary.deog.abstract
import org.objectionary.deog.base
import org.objectionary.deog.findRef
import org.objectionary.deog.name
import org.objectionary.deog.packageName
import org.objectionary.deog.repr.DGraphAttr
import org.objectionary.deog.repr.DGraphNode
import org.objectionary.deog.repr.DeogGraph
import org.w3c.dom.Node

typealias Abstracts = MutableMap<String, MutableSet<DGraphNode>>

/**
 * Propagates inner attributes
 */
internal class InnerPropagator(
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
            val name = name(node)
            if (name != null && name == "@") {
                decorators[DGraphNode(node, packageName(node))] = false
            }
            if (abstract(node) != null && name != null) {
                abstracts.getOrPut(name) { mutableSetOf() }.add(DGraphNode(node, packageName(node)))
            }
        }
    }

    @Suppress("MAGIC_NUMBER")
    private fun processDecorators() {
        val repetitions = 5
        // @todo #44:30min this solution is naive, optimize it (see commented out lines)
        //  while (decorators.containsValue(false)) {
        for (i in 0..repetitions) {
            decorators.filter { !it.value }.forEach {
                getBaseAbstract(it.key)
            }
        }
        // }
    }

    /**
     * @return abstract object corresponding to the base attribute of the [key] node
     */
    @Suppress("AVOID_NULL_CHECKS")
    private fun getBaseAbstract(key: DGraphNode) {
        var tmpKey = key.body
        while (base(tmpKey)?.startsWith('.') == true) {
            if (tmpKey.previousSibling.previousSibling == null) {
                break
            }
            tmpKey = tmpKey.previousSibling.previousSibling
        }
        when (base(tmpKey)) {
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
        abstract(node)?.let { return node }
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
        while (name(tmpAbstract.body) != base(key.body)?.substring(1)) {
            tmpAbstract = deogGraph.dgNodes.find { graphNode ->
                tmpAbstract.attributes.find {
                    base(tmpNode)?.substring(1) == name(it.body)
                }?.body == graphNode.body
            } ?: return
            tmpNode = tmpNode?.nextSibling?.nextSibling
        }
        val parent = node.parentNode ?: return
        deogGraph.dgNodes.find { it.body == parent } ?: run {
            deogGraph.dgNodes.add(
                DGraphNode(
                    parent,
                    packageName(parent)
                )
            )
        }
        val dgParent = deogGraph.dgNodes.find { it.body == parent } ?: return
        tmpAbstract.attributes.forEach { graphNode ->
            dgParent.attributes.find { graphNode.body == it.body }
                ?: dgParent.attributes.add(
                    DGraphAttr(
                        graphNode.name,
                        graphNode.parentDistance + 1,
                        graphNode.body
                    )
                )
        }
        deogGraph.connect(dgParent, tmpAbstract)
        return
    }
}
