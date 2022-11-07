package org.objectionary.deog

import org.objectionary.deog.repr.DeogGraph
import com.jcabi.xml.XML
import com.jcabi.xml.XMLDocument
import com.yegor256.xsline.TrClasspath
import com.yegor256.xsline.Xsline
import org.eolang.parser.ParsingTrain
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

private val logger = LoggerFactory.getLogger("org.objectionary.deog.launch.Combiner")
private val sep = File.separatorChar
val documents: MutableMap<Document, String> = mutableMapOf()

/**
 * Aggregates all steps of analysis
 *
 * @param path path to the directory to be analysed
 * @param dirPostfix postfix of the resulting directory, which will be created after the analysis
 */
fun launch(path: String, dirPostfix: String = "deog") {
    documents.clear()
    val graph = buildGraph(path, false, dirPostfix)
    CondAttributesSetter(graph).processConditions()
    val attributesSetter = AttributesSetter(graph)
    attributesSetter.setAttributes()
    val innerPropagator = InnerPropagator(graph)
    innerPropagator.propagateInnerAttrs()
}

/**
 * Builds a single graph for all the files in the provided directory
 *
 * @param path path to the directory with files
 * @param gather if outputs should be gathered
 * @param dirPostfix postfix of the resulting directory
 * @return graph that was built
 */
internal fun buildGraph(
    path: String,
    gather: Boolean = true,
    dirPostfix: String = "deog"
): DeogGraph {
    Files.walk(Paths.get(path))
        .filter(Files::isRegularFile)
        .forEach {
            val tmpPath = createTempDirectories(path, it.toString(), gather, dirPostfix)
            transformXml(it.toString(), tmpPath)
            documents[getDocument(tmpPath)!!] = tmpPath
        }
    val builder = GraphBuilder(documents)
    builder.createGraph()
    return builder.deogGraph
}

/**
 * Creates a new xml by applying several xsl transformations to it
 *
 * @param inFilename to the input file
 * @param outFilename path to the output file
 */
private fun transformXml(
    inFilename: String,
    outFilename: String
) {
    val xmir: XML = XMLDocument(File(inFilename))
    val after = Xsline(
        TrClasspath(
            ParsingTrain().empty(),
            "/org/eolang/parser/add-refs.xsl",
            "/org/eolang/parser/expand-aliases.xsl",
            "/org/eolang/parser/resolve-aliases.xsl"
            // "/org/eolang/parser/wrap-method-calls.xsl"
        ).back()
    ).pass(xmir)
    File(outFilename).outputStream().write(after.toString().toByteArray())
}

/**
 * @param filename source xml filename
 * @return generated Document
 */
private fun getDocument(filename: String): Document? {
    try {
        val factory = DocumentBuilderFactory.newInstance()
        FileInputStream(filename).use { return factory.newDocumentBuilder().parse(it) }
    } catch (e: Exception) {
        logger.error(e.printStackTrace().toString())
    }
    return null
}

private fun createTempDirectories(
    path: String,
    filename: String,
    gather: Boolean = true,
    dirPostfix: String
): String {
    val tmpPath =
        if (gather) {
            "${path.substringBeforeLast(sep)}${sep}TMP$sep${path.substringAfterLast(sep)}_tmp${filename.substring(path.length)}"
        } else {
            "${path.substringBeforeLast(sep)}$sep${path.substringAfterLast(sep)}_$dirPostfix${filename.substring(path.length)}"
        }
    val forDirs = File(tmpPath.substringBeforeLast(sep)).toPath()
    Files.createDirectories(forDirs)
    val newFilePath = Paths.get(tmpPath)
    try {
        Files.createFile(newFilePath)
    } catch (e: Exception) {
        logger.error(e.message)
    }
    return tmpPath
}
