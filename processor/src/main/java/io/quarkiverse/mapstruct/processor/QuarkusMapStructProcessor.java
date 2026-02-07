package io.quarkiverse.mapstruct.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.mapstruct.ap.MappingProcessor;

import io.quarkiverse.mapstruct.processor.spi.ConfigMappingTypes;

/**
 * A delegating annotation processor that wraps MapStruct's {@link MappingProcessor}.
 * This allows Quarkus-specific customizations to be added around the MapStruct processing.
 *
 * <p>
 * Collected {@code @ConfigMapping} types are persisted to a resource file so they survive
 * across incremental compilations. The {@link ConfigMappingTypes} class reads from
 * this file and is used by the {@code QuarkusAccessorNamingStrategy} SPI.
 */
public class QuarkusMapStructProcessor implements Processor {

    private static final String CONFIG_MAPPING_ANNOTATION = "io.smallrye.config.ConfigMapping";

    private final MappingProcessor delegate = new MappingProcessor();
    private ProcessingEnvironment processingEnv;
    private Path resourceFilePath;
    private final Set<String> collectedTypes = new HashSet<>();

    @Override
    public Set<String> getSupportedOptions() {
        return delegate.getSupportedOptions();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return delegate.getSupportedAnnotationTypes();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return delegate.getSupportedSourceVersion();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;

        try {
            FileObject fo = processingEnv.getFiler()
                    .getResource(StandardLocation.CLASS_OUTPUT, "", ConfigMappingTypes.RESOURCE_FILE);
            resourceFilePath = Path.of(fo.toUri());
        } catch (IOException e) {
            // Cannot determine output path
        }

        ConfigMappingTypes.setResourceFilePath(resourceFilePath);

        // Load types from a previous compilation (incremental builds)
        collectedTypes.addAll(ConfigMappingTypes.get());

        delegate.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        collectConfigMappings(roundEnv);
        writeTypesToFile();
        return delegate.process(annotations, roundEnv);
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
            ExecutableElement member, String userText) {
        return delegate.getCompletions(element, annotation, member, userText);
    }

    private void collectConfigMappings(RoundEnvironment roundEnv) {
        TypeElement configMappingAnnotation = processingEnv.getElementUtils()
                .getTypeElement(CONFIG_MAPPING_ANNOTATION);
        if (configMappingAnnotation == null) {
            return;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(configMappingAnnotation)) {
            if (element instanceof TypeElement typeElement) {
                collectConfigMappingType(typeElement);
            }
        }
    }

    private void collectConfigMappingType(TypeElement typeElement) {
        if (!collectedTypes.add(typeElement.getQualifiedName().toString())) {
            return;
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                collectNestedConfigMappings(method.getReturnType());
            }
        }
    }

    private void collectNestedConfigMappings(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return;
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        Element element = declaredType.asElement();

        // Recurse into type arguments first (e.g. Optional<Nested>, List<Nested>, Map<String, Nested>)
        for (TypeMirror typeArg : declaredType.getTypeArguments()) {
            collectNestedConfigMappings(typeArg);
        }

        // Only collect non-JDK interfaces as config mapping types
        if (element.getKind() == ElementKind.INTERFACE) {
            TypeElement nested = (TypeElement) element;
            String name = nested.getQualifiedName().toString();
            if (!name.startsWith("java.")) {
                collectConfigMappingType(nested);
            }
        }
    }

    private void writeTypesToFile() {
        if (resourceFilePath == null || collectedTypes.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(resourceFilePath.getParent());
            Files.write(resourceFilePath, new TreeSet<>(collectedTypes));
            ConfigMappingTypes.invalidateCache();
        } catch (IOException e) {
            // Silently ignore - types from the current compilation are still available via the cache
        }
    }
}
