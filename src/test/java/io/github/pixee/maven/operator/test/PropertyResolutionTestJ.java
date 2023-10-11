package io.github.pixee.maven.operator.test;

import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.ProjectModelFactory;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PropertyResolutionTestJ extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyResolutionTest.class);

    @Test
    public void testPropertyResolutionWhenProfileIsDeactivatedForcefully() throws DocumentException, IOException, URISyntaxException {
        Map<String, String> resolvedProperties = resolveWithProfiles("!test-profile");

        assertFalse("foo property must not be there", resolvedProperties.containsKey("foo"));
    }

    @Test
    public void testPropertyResolutionWhenProfileIsMissing() throws DocumentException, IOException, URISyntaxException {
        Map<String, String> resolvedProperties = resolveWithProfiles();

        assertFalse("foo property must not be there", resolvedProperties.containsKey("foo"));
    }

    @Test
    public void testPropertyResolutionWhenProfileIsActivated() throws DocumentException, IOException, URISyntaxException {
        Map<String, String> resolvedProperties = resolveWithProfiles("test-profile");

        assertTrue("foo property must be there", resolvedProperties.containsKey("foo"));
        assertEquals("foo property must be equal to 'bar'", resolvedProperties.get("foo"), "bar");
    }

    private Map<String, String> resolveWithProfiles(String... profilesToUse) throws DocumentException, IOException, URISyntaxException {
        LOGGER.debug("resolving with profiles: " + Arrays.toString(profilesToUse));

        Dependency dependencyToUpgrade = new Dependency(
                "org.dom4j",
                "dom4j",
                "2.0.2",
                null,
                null,
                null
        );
        ProjectModel context = ProjectModelFactory.load(
                POMOperatorTest.class.getResource("pom-1.xml")
        ).withDependency(dependencyToUpgrade).withActiveProfiles(profilesToUse).build();

        LOGGER.debug("Resolved Properties: " + context.resolvedProperties());

        return new HashMap<>(context.resolvedProperties());
    }
}

