package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.Dependency;
import org.apache.maven.model.building.ModelBuildingException;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class QueryByResolverJ extends AbstractQueryCommandJ {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryByResolverJ.class);

    @Override
    public void extractDependencyTree(File outputPath, File pomFilePath, ProjectModelJ c) {
        // Not yet implemented
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        EmbedderFacadeJ.EmbedderFacadeRequest req = new EmbedderFacadeJ.EmbedderFacadeRequest(
                pm.getOffline(),
                pm.getRepositoryPath(),
                pm.getPomFile().getFile$pom_operator(),
                filterActiveProfiles(pm.getActiveProfiles()),
                filterExcludedProfiles(pm.getActiveProfiles())
        );

        this.result = new ArrayList<>();

        EmbedderFacadeJ.EmbedderFacadeResponse embedderFacadeResponse;

        try {
            embedderFacadeResponse = EmbedderFacadeJ.invokeEmbedder(req);
        } catch (ModelBuildingException mbe) {
            IgnorableJ.LOGGER.debug("mbe (you can ignore): ", mbe);
            return false;
        }

        org.apache.maven.model.building.ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<org.eclipse.aether.graph.Dependency> deps;
        if (res.getEffectiveModel().getDependencies() != null) {
            deps = new ArrayList<>();
            for (org.apache.maven.model.Dependency dependency : res.getEffectiveModel().getDependencies()) {
                org.eclipse.aether.graph.Dependency aetherDependency = dependencyToArtifact(dependency);
                deps.add(aetherDependency);
            }
        } else {
            deps = new ArrayList<>();
        }

        List<org.eclipse.aether.graph.Dependency> managedDeps;
        if (res.getEffectiveModel().getDependencyManagement() != null) {
            managedDeps = new ArrayList<>();
            for (org.apache.maven.model.Dependency dependency : res.getEffectiveModel().getDependencyManagement().getDependencies()) {
                org.eclipse.aether.graph.Dependency aetherDependency = dependencyToArtifact(dependency);
                managedDeps.add(aetherDependency);
            }
        } else {
            managedDeps = new ArrayList<>();
        }

        CollectRequest collectRequest = new CollectRequest(deps, managedDeps, embedderFacadeResponse.getRemoteRepositories());

        try {
            org.eclipse.aether.collection.CollectResult collectResult = embedderFacadeResponse.getRepositorySystem().collectDependencies(
                    embedderFacadeResponse.getSession(),
                    collectRequest
            );

            List<Dependency> returnList = new ArrayList<>();

            collectResult.getRoot().accept(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    if (node.getDependency() != null) {
                        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
                        returnList.add(new Dependency(
                                dependency.getArtifact().getGroupId(),
                                dependency.getArtifact().getArtifactId(),
                                dependency.getArtifact().getVersion(),
                                dependency.getArtifact().getClassifier(),
                                dependency.getArtifact().getExtension(),
                                dependency.getScope()
                        ));
                    }
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    return true;
                }
            });

            this.result = returnList;

            return true;
        } catch (DependencyCollectionException e) {
            LOGGER.warn("while resolving: ", e);
            return false;
        }
    }

    private static List<String> filterActiveProfiles(Collection<String> profiles) {
        List<String> activeProfiles = new ArrayList<>();
        for (String profile : profiles) {
            if (!profile.startsWith("!")) {
                activeProfiles.add(profile);
            }
        }
        return activeProfiles;
    }

    private static List<String> filterExcludedProfiles(Collection<String> profiles) {
        List<String> excludedProfiles = new ArrayList<>();
        for (String profile : profiles) {
            if (profile.startsWith("!")) {
                excludedProfiles.add(profile.substring(1));
            }
        }
        return excludedProfiles;
    }

    private org.eclipse.aether.graph.Dependency dependencyToArtifact(org.apache.maven.model.Dependency dependency) {
        return new org.eclipse.aether.graph.Dependency(
                new DefaultArtifact(
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getClassifier(),
                        dependency.getType(),
                        dependency.getVersion()
                ),
                dependency.getScope()
        );
    }
}
