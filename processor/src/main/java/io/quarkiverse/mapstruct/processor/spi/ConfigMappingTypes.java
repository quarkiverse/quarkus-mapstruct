package io.quarkiverse.mapstruct.processor.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides access to the set of fully qualified names of types that belong to a
 * {@code @ConfigMapping} interface hierarchy.
 *
 * <p>
 * Types are persisted to a resource file by the annotation processor so they survive
 * across incremental compilations. This class reads from that file.
 */
public final class ConfigMappingTypes {

    public static final String RESOURCE_FILE = "META-INF/quarkus-mapstruct/config-mapping-types";

    private static volatile Path resourceFilePath;
    private static volatile Set<String> cachedTypes;

    private ConfigMappingTypes() {
    }

    /**
     * Sets the path to the resource file. Called by the annotation processor during initialization.
     */
    public static void setResourceFilePath(Path path) {
        resourceFilePath = path;
        cachedTypes = null;
    }

    /**
     * Invalidates the cached types so the next call to {@link #get()} re-reads the file.
     */
    public static void invalidateCache() {
        cachedTypes = null;
    }

    /**
     * Returns the set of fully qualified names of types that belong to a
     * {@code @ConfigMapping} interface hierarchy.
     */
    public static Set<String> get() {
        Set<String> types = cachedTypes;
        if (types != null) {
            return types;
        }
        types = readTypesFromFile();
        cachedTypes = types;
        return types;
    }

    private static Set<String> readTypesFromFile() {
        Path path = resourceFilePath;
        if (path == null || !Files.exists(path)) {
            return Collections.emptySet();
        }
        try {
            Set<String> types = new HashSet<>();
            for (String line : Files.readAllLines(path)) {
                if (!line.isBlank()) {
                    types.add(line.trim());
                }
            }
            return Collections.unmodifiableSet(types);
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }
}
