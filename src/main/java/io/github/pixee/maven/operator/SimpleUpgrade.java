package io.github.pixee.maven.operator;

/**
 * Represents bumping an existing dependency.
 */
public class SimpleUpgrade extends AbstractCommand {

    private static SimpleUpgrade instance;

    private SimpleUpgrade() {
        // Private constructor to prevent instantiation.
    }

    public static SimpleUpgrade getInstance() {
        if (instance == null) {
            instance = new SimpleUpgrade();
        }
        return instance;
    }

    @Override
    public boolean execute(ProjectModel pm) {
        if (pm.getDependency() == null) {
            throw new NullPointerException("Dependency must not be null.");
        }

        String lookupExpressionForDependency = Util.buildLookupExpressionForDependency(pm.getDependency());

        return handleDependency(pm, lookupExpressionForDependency);
    }
}

