package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.java.UtilJ
import io.github.pixee.maven.operator.Util.selectXPathNodes
import io.github.pixee.maven.operator.java.CommandJ
import org.dom4j.Element

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a dependencyManagement/ section as well)
 */
val SimpleInsert = object : CommandJ {
    override fun execute(pm: ProjectModel): Boolean {
        val dependencyManagementNodeList =
            pm.pomFile.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")

        val dependenciesNode = if (dependencyManagementNodeList.isEmpty()) {
            val newDependencyManagementNode =
                UtilJ.addIndentedElement(pm.pomFile.resultPom.rootElement, pm.pomFile, "dependencyManagement")

            val dependencyManagementNode =
                UtilJ.addIndentedElement(newDependencyManagementNode, pm.pomFile, "dependencies")

            dependencyManagementNode
        } else {
            (dependencyManagementNodeList.first() as Element).element("dependencies")
        }

        val dependencyNode = appendCoordinates(dependenciesNode, pm)

        val versionNode = UtilJ.addIndentedElement(dependencyNode, pm.pomFile, "version")

        UtilJ.upgradeVersionNode(pm, versionNode, pm.pomFile)

        val dependenciesNodeList =
            pm.pomFile.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.isEmpty()) {
            UtilJ.addIndentedElement(pm.pomFile.resultPom.rootElement, pm.pomFile, "dependencies")
        } else if (dependenciesNodeList.size == 1) {
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        appendCoordinates(rootDependencyNode, pm)

        return true
    }

    /**
     * Creates the XML Elements for a given dependency
     */
    private fun appendCoordinates(
        dependenciesNode: Element,
        c: ProjectModel
    ): Element {
        val dependencyNode = UtilJ.addIndentedElement(dependenciesNode, c.pomFile, "dependency")

        val groupIdNode = UtilJ.addIndentedElement(dependencyNode, c.pomFile, "groupId")

        val dep = c.dependency!!

        groupIdNode.text = dep.groupId

        val artifactIdNode = UtilJ.addIndentedElement(dependencyNode, c.pomFile, "artifactId")

        artifactIdNode.text = dep.artifactId

        return dependencyNode
    }

    override fun postProcess(c: ProjectModel): Boolean = false
}
