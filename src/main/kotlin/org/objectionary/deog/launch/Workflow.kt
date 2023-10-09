package org.objectionary.deog.launch

import org.objectionary.deog.steps.AttributesSetter
import org.objectionary.deog.steps.CondAttributesSetter
import org.objectionary.deog.steps.InnerPropagator
import org.objectionary.deog.GraphBuilder
import org.objectionary.deog.sources.SrsTransformed
import org.objectionary.deog.sources.XslTransformer
import org.w3c.dom.Document
import java.nio.file.Path

/**
 * Workflow of some software application.
 */
interface Workflow {
    /**
     * Starts the entire workflow.
     */
    fun launch()
}

/**
 * Stores all the information from xmir files in the form of a graph. Launches various analysis or transformation steps
 * on this graph.
 *
 * @property documents all documents from analyzed directory
 */
abstract class XmirAnalysisWorkflow(val documents: MutableMap<Document, Path>) : Workflow {
    /** @property graph decoration hierarchy graph of xmir files from analyzed directory */
    protected val graph = GraphBuilder(documents).createGraph()

    /**
     * Constructs [documents] from [path]
     *
     * @param path path to the directory to be analysed
     * @param postfix postfix of the resulting directory
     */
    constructor(path: String, postfix: String) : this(
        SrsTransformed(Path.of(path), XslTransformer(), postfix).walk())

    /**
     * Aggregates all steps of analysis
     */
    abstract override fun launch()
}

/**
 * Stores all the information from xmir files in the form of a graph. Launches analysis and transformation steps for
 * building Decoration Graph of given EO programs
 */
class DeogWorkflow : XmirAnalysisWorkflow {
    /**
     * Constructs workflow class using documents
     *
     * @param documents all documents from analyzed directory
     */
    constructor(documents: MutableMap<Document, Path>) : super(documents)

    /**
     * Constructs [documents] from [path]
     *
     * @param path path to the directory to be analysed
     * @param postfix postfix of the resulting directory
     */
    constructor(path: String, postfix: String = "deog") : super(path, postfix)

    /**
     * Aggregates all steps of building Decoration Graph
     */
    override fun launch() {
        CondAttributesSetter(graph).processConditions()
        AttributesSetter(graph).setAttributes()
        InnerPropagator(graph).propagateInnerAttrs()
    }
}
