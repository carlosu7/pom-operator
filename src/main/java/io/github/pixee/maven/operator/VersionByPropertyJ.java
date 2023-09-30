package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.java.AbstractVersionCommandJ;
import io.github.pixee.maven.operator.java.VersionDefinitionJ;
import io.github.pixee.maven.operator.ProjectModel;
import kotlin.Pair;

import java.util.*;

public class VersionByPropertyJ extends AbstractVersionCommandJ {

    @Override
    public boolean execute(ProjectModel pm) {
        Set<VersionDefinitionJ> definedProperties = new TreeSet<>(VERSION_KIND_COMPARATOR);

        for (Map.Entry<String, List<kotlin.Pair<String, POMDocument>>> entry : pm.getPropertiesDefinedByFile().entrySet()) {
            String propertyName = entry.getKey();
            if (PROPERTY_TO_KIND.containsKey(propertyName)) {
                Kind kind = PROPERTY_TO_KIND.get(propertyName);

                if (kind != null) {
                    definedProperties.add(new VersionDefinitionJ(kind, entry.getValue().get(0).getFirst()));
                }
            }
        }

        result.addAll(definedProperties);

        return !definedProperties.isEmpty();
    }

    private static class Pair<K, V> {
        private final K first;
        private final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public K getFirst() {
            return first;
        }

        public V getSecond() {
            return second;
        }
    }

}

