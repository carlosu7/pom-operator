package io.github.pixee.maven.operator.test

import com.google.common.io.Files
import io.github.pixee.maven.operator.POMDocumentFactoryJ
import io.github.pixee.maven.operator.POMOperatorJ
import io.github.pixee.maven.operator.ProjectModelFactoryJ
import io.github.pixee.maven.operator.ProjectModelJ
import io.github.pixee.maven.operator.QueryTypeJ
import io.github.pixee.maven.operator.WrongDependencyTypeExceptionJ
import io.github.pixee.maven.operator.DependencyJ
import io.github.pixee.maven.operator.kotlin.POMDocument
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class POMOperatorMultipomTest : AbstractTestBase() {
    @Test(expected = WrongDependencyTypeExceptionJ::class)
    fun testWithParentAndChildMissingPackaging() {
        val parentResource = getResource("parent-and-child-parent-broken.xml")

        val parentPomFiles = listOf(POMDocumentFactoryJ.load(parentResource))

        val parentPom = ProjectModelFactoryJ.load(
            parentResource,
        ).withParentPomFiles(parentPomFiles)

        gwt(
            "parent-and-child",
            parentPom.withDependency(DependencyJ.fromString("org.dom4j:dom4j:2.0.3"))
        )
    }

    @Test(expected = WrongDependencyTypeExceptionJ::class)
    fun testWithParentAndChildWrongType() {
        val parentResource = getResource("parent-and-child-child-broken.xml")

        val parentPomFile = POMDocumentFactoryJ.load(getResource("parent-and-child-parent.xml"))

        val parentPomFiles = listOf(parentPomFile)

        val parentPom = ProjectModelFactoryJ.load(
            parentResource,
        ).withParentPomFiles(parentPomFiles)

        gwt(
            "parent-and-child-wrong-type",
            parentPom.withDependency(DependencyJ.fromString("org.dom4j:dom4j:2.0.3"))
        )
    }

    @Test
    fun testWithMultiplePomsBasicNoVersionProperty() {
        val parentPomFile = getResource("sample-parent/pom.xml")

        val projectModelFactory = ProjectModelFactoryJ
            .load(
                getResource("sample-child-with-relativepath.xml")
            )
            .withParentPomFiles(listOf(POMDocumentFactoryJ.load(parentPomFile)))
            .withUseProperties(false)

        System.out.println(DependencyJ.fromString("org.dom4j:dom4j:2.0.3"))

        val result = gwt(
            "multiple-pom-basic-no-version-property",
            projectModelFactory.withDependency(DependencyJ.fromString("org.dom4j:dom4j:2.0.3"))
        )

        validateDepsFrom(result)

        assertTrue(result.allPomFiles().size == 2, "There should be two files")
        assertTrue(result.allPomFiles().all { it.dirty }, "All files were modified")
    }

    @Test
    fun testWithMultiplePomsBasicWithVersionProperty() {
        val parentPomFile = getResource("sample-parent/pom.xml")

        val sampleChild = getResource("sample-child-with-relativepath.xml")

        val parentPom = ProjectModelFactoryJ.load(
            sampleChild
        ).withParentPomFiles(listOf(POMDocumentFactoryJ.load(parentPomFile)))
            .withUseProperties(true)

        val result = gwt(
            "multiple-pom-basic-with-version-property",
            parentPom.withDependency(DependencyJ.fromString("org.dom4j:dom4j:2.0.3"))
        )

        validateDepsFrom(result)

        assertTrue(result.allPomFiles().size == 2, "There should be two files")
        assertTrue(result.allPomFiles().all { it.dirty }, "All files were modified")

        val parentPomString = String(result.parentPomFiles.first().resultPomBytes, Charsets.UTF_8)
        val pomString = String(result.pomFile.resultPomBytes, Charsets.UTF_8)

        val version = result.dependency!!.version!!

        assertTrue(
            parentPomString.contains("versions.dom4j>${version}"),
            "Must contain property with version set on parent pom"
        )
        assertFalse(pomString.contains(version), "Must not have reference to version on pom")
    }

    fun validateDepsFrom(context: ProjectModelJ) {
        val resultFiles = copyFiles(context)

        resultFiles.entries.forEach {
            System.err.println("from ${it.key.pomPath} -> ${it.value}")
        }

        val pomFile = resultFiles.entries.first().value

        val dependencies = POMOperatorJ.queryDependency(
            ProjectModelFactoryJ.load(pomFile)
                .withQueryType(QueryTypeJ.UNSAFE)
                .build()
        )

        val foundDependency = dependencies.contains(context.dependency!!)

        assertTrue(
            foundDependency,
            "Dependency ${context.dependency!!} must be present in context $context ($dependencies)"
        )
    }

    fun copyFiles(context: ProjectModelJ): Map<POMDocument, File> {
        var commonPath = File(context.pomFile.pomPath!!.toURI()).canonicalFile

        for (p in context.parentPomFiles) {
            commonPath = File(
                File(p.pomPath!!.toURI()).canonicalPath.toString()
                    .commonPrefixWith(commonPath.canonicalPath)
            )
        }

        val commonPathLen = commonPath.parent.length

        val tmpOutputDir = Files.createTempDir()

        val result = context.allPomFiles().map { p ->
            val pAsFile = File(p.pomPath!!.toURI())

            val relPath = pAsFile.canonicalPath.substring(1 + commonPathLen)

            val targetPath = File(tmpOutputDir, relPath)

            if (!targetPath.parentFile.exists()) {
                targetPath.parentFile.mkdirs()
            }

            targetPath.writeBytes(p.resultPomBytes)

            p to targetPath
        }.toMap()

        return result
    }
}
