package io.github.pixee.maven.operator.java;

import com.github.zafarkhaja.semver.Version;
import io.github.pixee.maven.operator.POMDocument;
import io.github.pixee.maven.operator.Util;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.XPath;
import org.dom4j.tree.DefaultText;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilJ {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilJ.class);
    private static final Pattern PROPERTY_REFERENCE_PATTERN = Pattern.compile("^\\$\\{(.*)}$");

    public static Element addIndentedElement(Element element, POMDocument d, String name) {
        List<Node> contentList = element.content();

        int indentLevel = findIndentLevel(element);

        String prefix = d.getEndl() + StringUtils.repeat(d.getIndent(), 1 + indentLevel);

        String suffix = d.getEndl() + StringUtils.repeat(d.getIndent(), indentLevel);

        if (!contentList.isEmpty() && contentList.get(contentList.size() - 1) instanceof Text) {
            Text lastElement = (Text) contentList.get(contentList.size() - 1);

            if (StringUtils.isWhitespace(lastElement.getText())) {
                contentList.remove(contentList.size() - 1);
            }
        }

        contentList.add(new DefaultText(prefix));

        Element newElement = element.addElement(name);

        contentList.add(new DefaultText(suffix));

        d.setDirty(true);

        return newElement;
    }

    private static int findIndentLevel(Element startingNode) {
        int level = 0;
        Element node = startingNode;

        while (node.getParent() != null) {
            level += 1;
            node = node.getParent();
        }

        return level;
    }

    public static void upgradeVersionNode(
            ProjectModelJ c,
            Element versionNode,
            POMDocument pomDocumentHoldingProperty
    ) {
        if (c.getUseProperties()) {
            String propertyName = propertyName(c, versionNode);

            // Define property
            upgradeProperty(c, pomDocumentHoldingProperty, propertyName);

            versionNode.setText(escapedPropertyName(propertyName));
        } else {
            if (c.getDependency() != null && c.getDependency().getVersion() != null) {
                String nodeText = versionNode.getText();
                String trimmedText = (nodeText != null) ? nodeText.trim() : "";

                if (!trimmedText.equals(c.getDependency().getVersion())) {
                    pomDocumentHoldingProperty.setDirty(true);
                    versionNode.setText(c.getDependency().getVersion());
                }
            }
        }
    }

    private static void upgradeProperty(ProjectModelJ c, POMDocument d, String propertyName) {
        if (d.getResultPom().getRootElement().element("properties") == null) {
            addIndentedElement(d.getResultPom().getRootElement(), d, "properties");
        }

        Element parentPropertyElement = d.getResultPom().getRootElement().element("properties");

        if (parentPropertyElement.element(propertyName) == null) {
            addIndentedElement(parentPropertyElement, d, propertyName);
        } else {
            if (!c.getOverrideIfAlreadyExists()) {

                Pattern propertyReferencePattern = Pattern.compile("\\$\\{" + propertyName + "}");

                Matcher matcher = propertyReferencePattern.matcher(d.getResultPom().asXML());
                int numberOfAllCurrentMatches = 0;

                while (matcher.find()) {
                    numberOfAllCurrentMatches++;
                }

                if (numberOfAllCurrentMatches > 1) {
                    throw new IllegalStateException("Property " + propertyName + " is already defined - and used more than once.");
                }
            }
        }

        Element propertyElement = parentPropertyElement.element(propertyName);

        String propertyText = (propertyElement.getText() != null) ? propertyElement.getText().trim() : "";

        if (c.getDependency() != null && c.getDependency().getVersion() != null && !propertyText.equals(c.getDependency().getVersion())) {
            propertyElement.setText(c.getDependency().getVersion());
            d.setDirty(true);
        }
    }

    private static String escapedPropertyName(String propertyName) {
        return "${" + propertyName + "}";
    }

    static String propertyName(ProjectModelJ c, Element versionNode) {
        String version = versionNode.getTextTrim();

        if (PROPERTY_REFERENCE_PATTERN.matcher(version).matches()) {
            Matcher matcher = PROPERTY_REFERENCE_PATTERN.matcher(version);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        if (c.getDependency() != null) {
            c.getDependency().getArtifactId();
            return "versions." + c.getDependency().getArtifactId();
        }

        return "versions.default";
    }

    public static boolean findOutIfUpgradeIsNeeded(ProjectModelJ c, Element versionNode) {
        String currentVersionNodeText = resolveVersion(c, versionNode.getText());

        Version currentVersion = Version.valueOf(currentVersionNodeText);
        Version newVersion = Version.valueOf(c.getDependency().getVersion());

        boolean versionsAreIncreasing = newVersion.greaterThan(currentVersion);

        return versionsAreIncreasing;
    }

    private static String resolveVersion(ProjectModelJ c, String versionText) {
        if (PROPERTY_REFERENCE_PATTERN.matcher(versionText).matches()) {
            StrSubstitutor substitutor = new StrSubstitutor(c.resolvedProperties());
            String resolvedVersion = substitutor.replace(versionText);
            return resolvedVersion;
        } else {
            return versionText;
        }
    }

    public static String buildLookupExpressionForDependency(DependencyJ dependency) {
        return "/m:project" +
                "/m:dependencies" +
                "/m:dependency" +
                "[./m:groupId[text()='" + dependency.getGroupId() + "'] and " +
                "./m:artifactId[text()='" + dependency.getArtifactId() + "']]";
    }

    public static String buildLookupExpressionForDependencyManagement(DependencyJ dependency) {
        return "/m:project" +
                "/m:dependencyManagement" +
                "/m:dependencies" +
                "/m:dependency" +
                "[./m:groupId[text()='" + dependency.getGroupId() + "'] and " +
                "./m:artifactId[text()='" + dependency.getArtifactId() + "']]";
    }

    public static File which(String path) {
        List<String> nativeExecutables;
        if (SystemUtils.IS_OS_WINDOWS) {
            nativeExecutables = new ArrayList<>();
            nativeExecutables.add("");
            nativeExecutables.add(".exe");
            nativeExecutables.add(".bat");
            nativeExecutables.add(".cmd");
            nativeExecutables.replaceAll(ext -> path + ext);
        } else {
            nativeExecutables = List.of(path);
        }

        String pathContentString = System.getenv("PATH");

        String[] pathElements = pathContentString.split(File.pathSeparator);

        List<File> possiblePaths = new ArrayList<>();
        for (String executable : nativeExecutables) {
            for (String pathElement : pathElements) {
                possiblePaths.add(new File(new File(pathElement), executable));
            }
        }

        Predicate<File> isCliCallable = SystemUtils.IS_OS_WINDOWS
                ? it -> it.exists() && it.isFile()
                : it -> it.exists() && it.isFile() && it.canExecute();

        File result = possiblePaths.stream()
                .filter(isCliCallable)
                .reduce((first, second) -> second) // Find last
                .orElse(null);

        if (result == null) {
            LOGGER.warn("Unable to find mvn executable (execs: {}, path: {})",
                    String.join("/", nativeExecutables),
                    pathContentString);
        }

        return result;
    }

    public static List<Node> selectXPathNodes(Node node, String expression) {
        return Util.INSTANCE.selectXPathNodes(node, expression);
    }

    /*


    public static List<Node> selectXPathNodes(Node node, String expression) {
        try {
            XPath xpath = (XPath) createXPathExpression(expression);
            return xpath.selectNodes(node);
        } catch (Exception e) {
            LOGGER.warn("selectXPathNodes " + e);
            return new ArrayList<>();
        }
    }

    private static Dom4jXPath createXPathExpression(String expression) throws Exception {
        Dom4jXPath xpath = new Dom4jXPath(expression);
        xpath.setNamespaceContext(namespaceContext);
        return xpath;
    }

    private static final SimpleNamespaceContext namespaceContext;

    static {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("m", "http://maven.apache.org/POM/4.0.0");
        namespaceContext = new SimpleNamespaceContext(namespaces);
    }

    */
}
