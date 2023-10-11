package io.github.pixee.maven.operator.test;

import fun.mike.dmp.DiffMatchPatch;
import fun.mike.dmp.Patch;
import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.ProjectModelFactory;
import io.github.pixee.maven.operator.ProjectModel;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import java.io.File;
import java.net.URLDecoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;

public class AbstractTestBase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

    protected File getResource(String name) throws URISyntaxException {
        return new File(this.getClass().getResource(name).toURI());
    }

    public File getResourceAsFile(String name) throws URISyntaxException {
        File resourceUrl = getResource(name);
        return new File(resourceUrl.toURI());
    }

    protected ProjectModel gwt(String name, ProjectModelFactory pmf) throws Exception {
        return gwt(name, pmf.build());
    }

    protected ProjectModel gwt(String testName, ProjectModel context) throws Exception {

        String resultFile = "pom-" + testName + "-result.xml";
        URL resource = AbstractTestBase.class.getClass().getResource(resultFile);

        if (resource != null) {
            Document outcome = new SAXReader().read(resource);
            POMOperator.modify(context);

            Assert.assertFalse(
                    "Expected and outcome have differences",
                    getXmlDifferences(context.getPomFile().getResultPom(), outcome).hasDifferences()
            );
        } else {
            Path resultFilePath = Paths.get("src/test/resources/", AbstractTestBase.class.getPackage().getName().replace(".", "/"), resultFile);

            POMOperator.modify(context);


            byte[] resultPomBytes = context.getPomFile().getResultPomBytes();

            try {
                Files.write(resultFilePath, resultPomBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return context;
    }


    public static Diff getXmlDifferences(Document original, Document modified) {
        String originalDoc = original.asXML();
        String modifiedDoc = modified.asXML();

        Diff diff = DiffBuilder.compare(Input.fromString(originalDoc))
                .withTest(Input.fromString(modifiedDoc))
                .ignoreWhitespace().checkForSimilar().build();

        LOGGER.debug("diff: " + diff);

        return diff;
    }

    protected String getTextDifferences(Document pomDocument, Document resultPom) throws UnsupportedEncodingException {
        String pomDocumentAsString = pomDocument.asXML();
        String resultPomAsString = resultPom.asXML();

        DiffMatchPatch dmp = new DiffMatchPatch();
        LinkedList<Patch> patches = dmp.patch_make(pomDocumentAsString, resultPomAsString);
        String patchText = dmp.patch_toText(patches);

        return URLDecoder.decode(patchText, "utf-8");
    }
}

