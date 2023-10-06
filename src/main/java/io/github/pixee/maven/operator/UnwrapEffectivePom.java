package io.github.pixee.maven.operator;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.*;
public class UnwrapEffectivePom extends AbstractVersionCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnwrapEffectivePom.class);

    public boolean execute(ProjectModel pm) {
        try {
            return executeInternal(pm);
        } catch (Exception e) {
            if (e instanceof ModelBuildingException) {
                Ignorable.LOGGER.debug("mbe (you can ignore): ", e);
            } else {
                LOGGER.warn("While trying embedder: ", e);
            }
            return false;
        }
    }

    private boolean executeInternal(ProjectModel pm) throws ModelBuildingException, URISyntaxException {
        EmbedderFacade.EmbedderFacadeRequest request = new EmbedderFacade.EmbedderFacadeRequest(
                pm.isOffline(),
                null,
                pm.getPomFile().getFile(),
                null,
                null
        );

        EmbedderFacade.EmbedderFacadeResponse embedderFacadeResponse = EmbedderFacade.invokeEmbedder(request);

        Set<VersionDefinition> definedVersions = new TreeSet<>(AbstractVersionCommand.VERSION_KIND_COMPARATOR);

        ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

        List<Xpp3Dom> pluginConfigurations = new ArrayList<>();

        for (Plugin plugin : res.getEffectiveModel().getBuild().getPluginManagement().getPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                if (configuration != null) {
                    pluginConfigurations.add(configuration);
                }
            }
        }

        for (Plugin plugin : res.getEffectiveModel().getBuild().getPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                if (configuration != null) {
                    pluginConfigurations.add(configuration);
                }
            }
        }

        for (Xpp3Dom config : pluginConfigurations) {
            for (Map.Entry<String, Kind> entry : AbstractVersionCommand.TYPE_TO_KIND.entrySet()) {
                Xpp3Dom child = config.getChild(entry.getKey());

                if (child != null) {
                    definedVersions.add(new VersionDefinition(entry.getValue(), child.getValue()));
                }
            }
        }

        List<VersionDefinition> definedProperties = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : res.getEffectiveModel().getProperties().entrySet()) {
            if (AbstractVersionCommand.PROPERTY_TO_KIND.containsKey(entry.getKey())) {
                Kind kind = AbstractVersionCommand.PROPERTY_TO_KIND.get(entry.getKey());

                definedProperties.add(new VersionDefinition(kind, (String) entry.getValue()));
            }
        }

        definedVersions.addAll(definedProperties);

        result.addAll(definedVersions);

        return !definedVersions.isEmpty();
    }

}
