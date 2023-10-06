package io.github.pixee.maven.operator;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class POMScannerJ {

    private static final Pattern RE_WINDOWS_PATH = Pattern.compile("^[A-Za-z]:");

    private static final Logger LOGGER = LoggerFactory.getLogger(POMScannerJ.class);

    public static ProjectModelFactoryJ scanFrom(File originalFile, File topLevelDirectory) throws Exception {
        ProjectModelFactoryJ originalDocument = ProjectModelFactoryJ.load(originalFile);

        List<File> parentPoms;
        try {
            parentPoms = getParentPoms(originalFile);
        } catch (Exception e) {
            if (e instanceof ModelBuildingException) {
                IgnorableJ.LOGGER.debug("mbe (you can ignore): ", e);
            } else {
                LOGGER.warn("While trying embedder: ", e);
            }
            return legacyScanFrom(originalFile, topLevelDirectory);
        }

        try {
            List<POMDocumentJ> parentPomDocuments = parentPoms.stream()
                    .map(file -> {
                        try {
                            return POMDocumentFactoryJ.load(file);
                        } catch (IOException | DocumentException e) {

                            return null; // Handle appropriately in your code
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return originalDocument.withParentPomFiles(parentPomDocuments);
        } catch (Exception e) {

            return originalDocument; // Return the original document or handle appropriately
        }

    }

    public static ProjectModelFactoryJ legacyScanFrom(File originalFile, File topLevelDirectory) throws DocumentException, IOException, URISyntaxException {
        POMDocumentJ pomFile = POMDocumentFactoryJ.load(originalFile);
        List<POMDocumentJ> parentPomFiles = new ArrayList<>();

        Queue<Element> pomFileQueue = new LinkedList<>();

        Element relativePathElement = pomFile.getPomDocument().getRootElement().element("parent") != null ?
                pomFile.getPomDocument().getRootElement().element("parent").element("relativePath") : null;

        Element parentElement = pomFile.getPomDocument().getRootElement().element("parent");

        if (relativePathElement != null && StringUtils.isNotEmpty(relativePathElement.getTextTrim())) {
            pomFileQueue.add(relativePathElement);
        } else if (relativePathElement == null && parentElement != null) {
            // Skip trying to find a parent if we are at the root
            if (!originalFile.getParentFile().equals(topLevelDirectory)) {
                DefaultElement newRelativePathElement = new DefaultElement("relativePath");
                newRelativePathElement.setText("../pom.xml");
                pomFileQueue.add(newRelativePathElement);
            }
        }

        Set<String> prevPaths = new HashSet<>();
        POMDocumentJ prevPOMDocument = pomFile;

        while (!pomFileQueue.isEmpty()) {
            Element currentRelativePathElement = pomFileQueue.poll();

            if (StringUtils.isEmpty(currentRelativePathElement.getTextTrim())) {
                break;
            }

            String relativePath = fixPomRelativePath(currentRelativePathElement.getText());

            if (!isRelative(relativePath)) {
                LOGGER.warn("not relative: " + relativePath);
                break;
            }

            if (prevPaths.contains(relativePath)) {
                LOGGER.warn("loop: " + pomFile.getFile() + ", relativePath: " + relativePath);
                break;
            } else {
                prevPaths.add(relativePath);
            }

            Path newPath = POMScannerJ.resolvePath(originalFile, relativePath);

            if (!newPath.toFile().exists()) {
                LOGGER.warn("new path does not exist: " + newPath);
                break;
            }

            if (newPath.toFile().length() == 0) {
                LOGGER.warn("File has zero length: " + newPath);
                break;
            }

            if (!newPath.startsWith(topLevelDirectory.getAbsolutePath())) {
                LOGGER.warn("Not a child: " + newPath + " (absolute: " + topLevelDirectory.getAbsolutePath() + ")");
                break;
            }

            POMDocumentJ newPomFile = POMDocumentFactoryJ.load(newPath.toFile());

            boolean hasParent = newPomFile.getPomDocument().getRootElement().element("parent") != null;
            boolean hasRelativePath = newPomFile.getPomDocument().getRootElement().element("parent")
                    != null && newPomFile.getPomDocument().getRootElement().element("parent").element("relativePath") != null;

            if (!hasRelativePath && hasParent) {
                Element parentElement2 = newPomFile.getPomDocument().getRootElement().element("parent");
                DefaultElement newRelativePathElement = new DefaultElement("relativePath");
                newRelativePathElement.setText("../pom.xml");
                parentElement2.add(newRelativePathElement);
            }

            String myArtifactId = newPomFile.getPomDocument().getRootElement().element("artifactId") != null ?
                    newPomFile.getPomDocument().getRootElement().element("artifactId").getText() : null;

            String prevParentArtifactId = prevPOMDocument.getPomDocument().getRootElement().element("parent") !=
                    null ? prevPOMDocument.getPomDocument().getRootElement().element("parent")
                    .element("artifactId").getText() : null;

            if (myArtifactId == null || prevParentArtifactId == null) {
                LOGGER.warn("Missing previous mine or parent: " + myArtifactId + " / " + prevParentArtifactId);
                break;
            }

            if (!myArtifactId.equals(prevParentArtifactId)) {
                LOGGER.warn("Previous doesn't match: " + myArtifactId + " / " + prevParentArtifactId);
                break;
            }

            parentPomFiles.add(newPomFile);
            prevPOMDocument = newPomFile;

            Element newRelativePathElement = newPomFile.getPomDocument().getRootElement().element("parent") != null ?
                    newPomFile.getPomDocument().getRootElement().element("parent").element("relativePath") : null;

            if (newRelativePathElement != null) {
                pomFileQueue.add(newRelativePathElement);
            }
        }

        return ProjectModelFactoryJ.loadFor(pomFile, parentPomFiles);
    }

    private static File lastFile;

    private static Path resolvePath(File baseFile, String relativePath) {
        File parentDir = baseFile;

        if (parentDir.isFile()) {
            parentDir = parentDir.getParentFile();
        }

        File result = new File(new File(parentDir, relativePath).toURI().normalize().getPath());

        lastFile = result.isDirectory() ? result : result.getParentFile();

        return Paths.get(result.getAbsolutePath());
    }

    private static String fixPomRelativePath(String text) {
        if (text == null) {
            return "";
        }

        String name = new File(text).getName();

        if (name.indexOf('.') == -1) {
            return text + "/pom.xml";
        }

        return text;
    }

    private static boolean isRelative(String path) {
        if (RE_WINDOWS_PATH.matcher(path).matches()) {
            return false;
        }

        return !(path.startsWith("/") || path.startsWith("~"));
    }

    private static List<File> getParentPoms(File originalFile) throws ModelBuildingException {
        EmbedderFacadeJ.EmbedderFacadeResponse embedderFacadeResponse =
                EmbedderFacadeJ.invokeEmbedder(
                        new EmbedderFacadeJ.EmbedderFacadeRequest(true, null, originalFile, null, null)
                );

        ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<Model> rawModels = new ArrayList<>();
        for (String modelId : res.getModelIds()) {
            Model rawModel = res.getRawModel(modelId);
            if (rawModel != null) {
                rawModels.add(rawModel);
            }
        }

        List<File> parentPoms = new ArrayList<>();
        if (rawModels.size() > 1) {
            for (int i = 1; i < rawModels.size(); i++) {
                Model rawModel = rawModels.get(i);
                if (rawModel != null) {
                    File pomFile = rawModel.getPomFile();
                    if (pomFile != null) {
                        parentPoms.add(pomFile);
                    }
                }
            }
        }

        return parentPoms;
    }

}