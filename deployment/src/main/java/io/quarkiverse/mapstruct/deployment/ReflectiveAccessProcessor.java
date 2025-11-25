package io.quarkiverse.mapstruct.deployment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class ReflectiveAccessProcessor {
    @BuildStep
    public ReflectiveClassBuildItem registerMappers(CombinedIndexBuildItem combinedIndexBuildItem) {
        Set<DotName> classNameCollector = new HashSet<>();

        collectFromMapperConfig(combinedIndexBuildItem.getComputingIndex(), classNameCollector);
        collectFromDecoratedWith(combinedIndexBuildItem.getComputingIndex(), classNameCollector);

        collectUpwards(combinedIndexBuildItem.getComputingIndex(), classNameCollector);

        String[] classNames = new String[classNameCollector.size()];
        int i = 0;
        for (DotName dotName : classNameCollector) {
            classNames[i++] = dotName.toString();
        }

        return ReflectiveClassBuildItem.builder(classNames).build();
    }

    private void collectFromMapperConfig(IndexView index, Set<DotName> classNameCollector) {
        Collection<AnnotationInstance> mapperAnnotations = index.getAnnotations(MapstructNames.NAME_MAPPER);

        for (AnnotationInstance annotation : mapperAnnotations) {
            ClassInfo classInfo = annotation.target().asClass();
            classNameCollector.add(classInfo.name());

            AnnotationInstance mapperConfig = null;
            AnnotationValue config = annotation.value("config");
            if (config != null) {
                ClassInfo aClass = index.getClassByName(config.asClass().name());
                if (aClass != null) {
                    mapperConfig = aClass.annotation(MapstructNames.NAME_MAPPER_CONFIG);
                }
            }

            String implementationPackage = readStringValue(index, annotation, mapperConfig, "implementationPackage");
            String implementationName = readStringValue(index, annotation, mapperConfig, "implementationName");

            String generatedMapperClassName = "%s.%s".formatted(implementationPackage, implementationName)
                    .replace("<PACKAGE_NAME>", classInfo.name().packagePrefix())
                    .replace("<CLASS_NAME>", classInfo.name().withoutPackagePrefix());
            classNameCollector.add(DotName.createSimple(generatedMapperClassName));

            List<Type> uses = readClassesArray(annotation, mapperConfig, "uses");
            for (Type use : uses) {
                classNameCollector.add(use.name());
            }
        }
    }

    private void collectFromDecoratedWith(IndexView index, Set<DotName> classNameCollector) {
        Collection<AnnotationInstance> annotations = index.getAnnotations(MapstructNames.NAME_DECORATED_WITH);

        for (AnnotationInstance annotation : annotations) {
            ClassInfo classInfo = annotation.target().asClass();
            if (!classInfo.hasAnnotation(MapstructNames.NAME_MAPPER)) {
                continue;
            }

            Type type = annotation.value().asClass();
            classNameCollector.add(type.name());
        }
    }

    private String readStringValue(IndexView index, AnnotationInstance mapperAnnotation,
            AnnotationInstance mapperConfigAnnotation, String name) {
        {
            AnnotationValue value = mapperAnnotation.value(name);

            if (value != null) {
                return value.asString();
            }
        }

        if (mapperConfigAnnotation != null) {
            AnnotationValue value = mapperConfigAnnotation.value(name);

            if (value != null) {
                return value.asString();
            }
        }

        // taken from AnnotationInstance.valueWithDefault
        ClassInfo definition = index.getClassByName(mapperAnnotation.name());
        MethodInfo method = definition.method(name);
        return method.defaultValue().asString();
    }

    private List<Type> readClassesArray(AnnotationInstance mapperAnnotation,
            AnnotationInstance mapperConfigAnnotation, String name) {
        // https://mapstruct.org/documentation/stable/reference/html/#shared-configurations
        // The @MapperConfig annotation has the same attributes as the @Mapper annotation. Any attributes not given via @Mapper will be inherited from the shared configuration. Attributes specified in @Mapper take precedence over the attributes specified via the referenced configuration class. List properties such as uses are simply combined:

        List<Type> classes = new ArrayList<>();
        {
            AnnotationValue value = mapperAnnotation.value(name);

            if (value != null) {
                classes.addAll(Arrays.asList(value.asClassArray()));
            }
        }

        if (mapperConfigAnnotation != null) {
            AnnotationValue value = mapperConfigAnnotation.value(name);
            if (value != null) {
                classes.addAll(Arrays.asList(value.asClassArray()));
            }
        }

        return classes;
    }

    private void collectUpwards(IndexView index, Set<DotName> classNameCollector) {
        ArrayDeque<DotName> stack = new ArrayDeque<>(classNameCollector);
        while (!stack.isEmpty()) {
            DotName name = stack.poll();

            if (DotName.OBJECT_NAME.equals(name)) {
                continue;
            }

            ClassInfo classInfo = index.getClassByName(name);
            if (classInfo == null) {
                continue;
            }

            classNameCollector.add(name);

            stack.add(classInfo.superClassType().name());
            stack.addAll(classInfo.interfaceNames());
        }
    }
}
