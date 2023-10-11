package io.github.pixee.maven.operator.test;

import io.github.pixee.maven.operator.InvalidPathException;
import io.github.pixee.maven.operator.POMScanner;
import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.ProjectModelFactory;
import org.dom4j.DocumentException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class POMScannerTestJ extends AbstractTestBaseJ {
    private final File currentDirectory = new File(System.getProperty("user.dir"));

    @Test
    public void testBasic() throws Exception {
        File pomFile = getResourceAsFile("sample-child-with-relativepath.xml");

        ProjectModel pmf = POMScanner.scanFrom(pomFile, currentDirectory).build();
    }

    @Test
    public void testTwoLevelsWithLoop() throws Exception {
        File pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels.xml");

        ProjectModel pmf = POMScanner.scanFrom(pomFile, currentDirectory).build();
    }

    @Test
    public void testTwoLevelsWithoutLoop() throws Exception {
        File pomFile = getResourceAsFile("sample-child-with-relativepath-and-two-levels-nonloop.xml");

        ProjectModel pmf = POMScanner.scanFrom(pomFile, currentDirectory).build();

        assertTrue( "There must be two parent pom files", pmf.getParentPomFiles().size() == 2);

        List<String> uniquePaths = pmf.allPomFiles().stream()
                .map(pom -> {
                    try {
                        return pom.getPomPath().toURI().normalize().toString();
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        String uniquePathsAsString = String.join(" ", uniquePaths);

        //LOGGER.info("uniquePathsAsString: " + uniquePathsAsString);

        assertTrue("There must be three unique pom files referenced", uniquePaths.size() == 3);
    }

    @Test
    public void testMultipleChildren() throws DocumentException, IOException, URISyntaxException {
        for (int index = 1; index <= 3; index++) {
            File pomFile = getResourceAsFile("nested/child/pom/pom-" + index + "-child.xml");

            ProjectModel pm = POMScanner.legacyScanFrom(pomFile, currentDirectory).build();

            assertTrue( "There must be at least one parent pom file", pm.getParentPomFiles().size() > 0);

            List<String> uniquePaths = pm.allPomFiles().stream()
                    .map(pom -> {
                        try {
                            return pom.getPomPath().toURI().normalize().toString();
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

            String uniquePathsAsString = String.join(" ", uniquePaths);

            //LOGGER.info("uniquePathsAsString: " + uniquePathsAsString);

            assertTrue("There must be at least two unique pom files referenced", uniquePaths.size() >= 2);
        }
    }

    @Test
    public void testMissingRelativeParentElement() throws DocumentException, IOException, URISyntaxException {
        File pomFile = getResourceAsFile("nested/child/pom/pom-demo.xml");

        ProjectModel pm = POMScanner.legacyScanFrom(pomFile, currentDirectory).build();

        assertTrue( "There must be a single parent pom file", pm.getParentPomFiles().size() == 1);
    }

    @Test
    public void testLegacyWithInvalidRelativePaths() throws DocumentException, IOException, URISyntaxException {
        for (int index = 1; index <= 3; index++) {
            String name = "sample-child-with-broken-path-" + index + ".xml";
            File pomFile = getResourceAsFile(name);

            ProjectModelFactory pmf = POMScanner.legacyScanFrom(pomFile, currentDirectory);

            assert pmf.build().getParentPomFiles().isEmpty();
        }
    }

    @Test
    public void testWithRelativePathEmpty() throws Exception {
        for (int index = 3; index <= 4; index++) {
            File pomFile = getResourceAsFile("pom-multiple-pom-parent-level-" + index + ".xml");

            try {
                ProjectModelFactory pmf = POMScanner.scanFrom(pomFile, currentDirectory);

                assertTrue(pmf.build().getParentPomFiles().size() > 0);
            } catch (Exception e) {
                //LOGGER.info("Exception thrown: " + e);

                if (e instanceof InvalidPathException) {
                    continue;
                }

                throw e;
            }
        }
    }

    @Test
    public void testWithMissingRelativePath() throws DocumentException, IOException, URISyntaxException {
        File pomFile = getResourceAsFile("sample-parent/sample-child/pom-multiple-pom-parent-level-6.xml");

        ProjectModelFactory pmf = POMScanner.legacyScanFrom(pomFile, currentDirectory);

        assertTrue(pmf.build().getParentPomFiles().size() > 0);
    }
}

