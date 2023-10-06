package io.github.pixee.maven.operator;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.collections4.CollectionUtils;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Facade for the POM Operator
 */
public class POMOperator {

    /**
     * Bump a Dependency Version on a POM.
     *
     * @param projectModel Project Model (Context) class
     */
    public static boolean modify(ProjectModel projectModel) throws URISyntaxException, IOException, XMLStreamException {
        return Chain.createForModify().execute(projectModel);
    }

    /**
     * Public API - Query for all the artifacts referenced inside a POM File.
     *
     * @param projectModel Project Model (Context) Class
     */
    public static Collection<Dependency> queryDependency(ProjectModel projectModel) throws URISyntaxException, IOException, XMLStreamException {
        return queryDependency(projectModel, Collections.emptyList());
    }

    /**
     * Public API - Query for all the versions mentioned inside a POM File.
     *
     * @param projectModel Project Model (Context) Class
     */
    public static Optional<VersionQueryResponse> queryVersions(ProjectModel projectModel) throws URISyntaxException, IOException, XMLStreamException {
        Set<VersionDefinition> queryVersionResult = queryVersions(projectModel, Collections.emptyList());

        if (queryVersionResult.size() == 2) {
            if (queryVersionResult.stream().anyMatch(it -> it.getKind() == Kind.RELEASE)) {
                throw new IllegalStateException("Unexpected queryVersionResult Combination: " + queryVersionResult);
            }

            VersionDefinition queryVersionSource = queryVersionResult.stream()
                    .filter(it -> it.getKind() == Kind.SOURCE)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing source version"));

            VersionDefinition queryVersionTarget = queryVersionResult.stream()
                    .filter(it -> it.getKind() == Kind.TARGET)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing target version"));

            Version mappedSourceVersion = mapVersion(queryVersionSource.getValue());
            Version mappedTargetVersion = mapVersion(queryVersionTarget.getValue());

            return Optional.of(new VersionQueryResponse(mappedSourceVersion, mappedTargetVersion));
        }

        if (queryVersionResult.size() == 1) {
            List<VersionDefinition> queryVersionResultList = CollectionUtils.isNotEmpty(queryVersionResult) ? queryVersionResult.stream().toList() : Collections.emptyList();
            Version mappedVersion = mapVersion(queryVersionResultList.get(0).getValue());

            VersionQueryResponse returnValue = new VersionQueryResponse(mappedVersion, mappedVersion);

            return Optional.of(returnValue);
        }

        return Optional.empty();
    }


    /**
     * Given a version string, formats and returns it as a semantic version object.
     *
     * Versions starting with "1." are appended with ".0".
     *
     * Other versions are appended with ".0.0".
     *
     * @return mapped version
     */
    public static Version mapVersion(String version) {
        String fixedVersion = version + (version.startsWith("1.") ? ".0" : ".0.0");
        return Version.valueOf(fixedVersion);
    }

    /**
     * Internal Use (package-wide) - Query for dependencies mentioned on a POM.
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    public static Collection<Dependency> queryDependency(ProjectModel projectModel, List<Command> commandList) throws URISyntaxException, IOException, XMLStreamException {
        Chain chain = Chain.createForDependencyQuery(projectModel.getQueryType());

        executeChain(commandList, chain, projectModel);

        AbstractQueryCommand lastCommand = null;
        for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
            if (chain.getCommandList().get(i) instanceof AbstractQueryCommand) {
                lastCommand = (AbstractQueryCommand) chain.getCommandList().get(i);
                if (lastCommand.getResult() != null) {
                    break;
                }
            }
        }

        if (lastCommand == null) {
            return Collections.emptyList();
        }

        return lastCommand.getResult();
    }

    /**
     * Internal Use (package-wide) - Query for versions mentioned on a POM.
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    public static Set<VersionDefinition> queryVersions(ProjectModel projectModel, List<Command> commandList) throws URISyntaxException, IOException, XMLStreamException {
        Chain chain = Chain.createForVersionQuery(projectModel.getQueryType());

        executeChain(commandList, chain, projectModel);

        AbstractVersionCommand lastCommand = null;
        for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
            if (chain.getCommandList().get(i) instanceof AbstractVersionCommand) {
                lastCommand = (AbstractVersionCommand) chain.getCommandList().get(i);
                if (lastCommand.result != null && !lastCommand.result.isEmpty()) {
                    break;
                }
            }
        }

        if (lastCommand == null) {
            return Collections.emptySet();
        }

        return lastCommand.result;
    }


    private static void executeChain(List<Command> commandList, Chain chain, ProjectModel projectModel) throws URISyntaxException, IOException, XMLStreamException {
        if (!commandList.isEmpty()) {
            chain.getCommandList().clear();
            chain.getCommandList().addAll(commandList);
        }

        chain.execute(projectModel);
    }


}
