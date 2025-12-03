package io.quarkiverse.mapstruct.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class MapperServiceProviderProcessor {

    private static final Logger LOG = Logger.getLogger(MapperServiceProviderProcessor.class);

    @BuildStep
    List<ServiceProviderBuildItem> registerMapperServiceProviders(CombinedIndexBuildItem index) {

        // Register all META-INF/services entries for ServiceLoader-based mappers
        // This enables native mode support for mappers with custom implementationName

        List<ServiceProviderBuildItem> result = new ArrayList<>();
        // Find all @Mapper annotated interfaces to determine which ones might use ServiceLoader
        for (AnnotationInstance mapperAnnotation : index.getComputingIndex().getAnnotations(MapstructNames.NAME_MAPPER)) {
            String mapperClassName = mapperAnnotation.target().asClass().name().toString();

            // This automatically reads META-INF/services/{mapperClassName} if it exists
            result.add(ServiceProviderBuildItem.allProvidersFromClassPath(mapperClassName));
            LOG.tracef("Registered ServiceLoader providers for mapper: %s", mapperClassName);
        }

        return result;
    }
}
