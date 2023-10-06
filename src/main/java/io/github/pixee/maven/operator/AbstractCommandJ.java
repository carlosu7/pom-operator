package io.github.pixee.maven.operator;

import org.dom4j.Element;
import org.dom4j.Node;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public abstract class AbstractCommandJ implements CommandJ {

    protected boolean handleDependency(ProjectModelJ pm, String lookupExpression) {
        List<Node> dependencyNodes = UtilJ.selectXPathNodes(pm.getPomFile().getResultPom(), lookupExpression);

        if (1 == dependencyNodes.size()) {
            List<Node> versionNodes = UtilJ.selectXPathNodes(dependencyNodes.get(0), "./m:version");

            if (1 == versionNodes.size()) {
                Element versionNode = (Element) versionNodes.get(0);

                boolean mustUpgrade = true;

                if (pm.getSkipIfNewer()) {
                    mustUpgrade = UtilJ.findOutIfUpgradeIsNeeded(pm, versionNode);
                }

                if (mustUpgrade) {
                    UtilJ.upgradeVersionNode(pm, versionNode, pm.getPomFile());
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean execute(ProjectModelJ pm) throws URISyntaxException, IOException, XMLStreamException {
        return false;
    }

    @Override
    public boolean postProcess(ProjectModelJ c) throws XMLStreamException {
        return false;
    }

    protected File getLocalRepositoryPath(ProjectModelJ pm) {
        File localRepositoryPath = null;

        if (pm.getRepositoryPath() != null) {
            localRepositoryPath = pm.getRepositoryPath();
        } else if (System.getenv("M2_REPO") != null) {
            localRepositoryPath = new File(System.getenv("M2_REPO"));
        } else if (System.getProperty("maven.repo.local") != null) {
            localRepositoryPath = new File(System.getProperty("maven.repo.local"));
        } else {
            localRepositoryPath = new File(
                    System.getProperty("user.home"),
                    ".m2/repository"
            );
        }

        return localRepositoryPath;
    }

}
