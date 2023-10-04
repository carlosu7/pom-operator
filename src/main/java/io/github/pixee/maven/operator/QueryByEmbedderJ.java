package io.github.pixee.maven.operator;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.utils.cli.Commandline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class QueryByEmbedderJ extends AbstractQueryCommandJ {

    public static final String MAVEN_MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryByEmbedderJ.class);

    public void extractDependencyTree(File outputPath, File pomFilePath, ProjectModelJ c) {
        MavenCli mavenCli = new MavenCli();

        MavenCommandLineBuilder cliBuilder = new MavenCommandLineBuilder();

        InvocationRequest invocationRequest = buildInvocationRequest(outputPath, pomFilePath, c);

        String oldMultimoduleValue = System.getProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY);

        System.setProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY, pomFilePath.getParent());

        try {
            Commandline cliBuilderResult = cliBuilder.build(invocationRequest);

            List<String> cliArgsList = Arrays.asList(cliBuilderResult.getCommandline());
            cliArgsList = cliArgsList.subList(1, cliArgsList.size());
            String[] cliArgs = cliArgsList.toArray(new String[0]);

            OutputStream baosOut =
                    LOGGER.isDebugEnabled() ? new ByteArrayOutputStream() : NullOutputStream.NULL_OUTPUT_STREAM;

            OutputStream baosErr =
                    LOGGER.isDebugEnabled() ? new ByteArrayOutputStream() : NullOutputStream.NULL_OUTPUT_STREAM;

            int result = mavenCli.doMain(
                    cliArgs,
                    pomFilePath.getParent(),
                    new PrintStream(baosOut, true),
                    new PrintStream(baosErr, true)
            );

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("baosOut: {}", baosOut);
                LOGGER.debug("baosErr: {}", baosErr);
            }

            if (1 == result && (!outputPath.exists())) {
                throw new InvalidContextExceptionJ();
            }

            if (0 != result) {
                throw new IllegalStateException("Unexpected status code: " + String.format("%02d", result));
            }
        } catch (CommandLineConfigurationException e) {
            // throw new RuntimeException(e);
        } finally {
            if (oldMultimoduleValue != null) {
                System.setProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY, oldMultimoduleValue);
            }
        }
    }


}
