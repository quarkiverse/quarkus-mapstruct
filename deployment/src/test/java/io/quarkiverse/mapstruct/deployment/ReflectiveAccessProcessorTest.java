package io.quarkiverse.mapstruct.deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class ReflectiveAccessProcessorTest {

    interface MapperInterface {

    }

    private ReflectiveAccessProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ReflectiveAccessProcessor();
    }

    @Test
    void testImplementationNameDefault() throws IOException {
        @Mapper
        class A {

        }

        Index index = buildIndex(A.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A");
    }

    @Test
    void testImplementationNameChanged() throws IOException {
        @Mapper(implementationName = "<CLASS_NAME>Implementation", implementationPackage = "<PACKAGE_NAME>.subpackage")
        class A {

        }

        Index index = buildIndex(A.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.subpackage.ReflectiveAccessProcessorTest$AImplementation",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A");
    }

    @Test
    void testImplementationNameOverwritesMapperConfigImplementationName() throws IOException {
        @MapperConfig(implementationName = "<CLASS_NAME>I", implementationPackage = "prefix.<PACKAGE_NAME>")
        class B {

        }

        @Mapper(implementationName = "<CLASS_NAME>Implementation", implementationPackage = "<PACKAGE_NAME>.subpackage", config = B.class)
        class A {

        }

        Index index = buildIndex(A.class, B.class, Mapper.class, MapperConfig.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.subpackage.ReflectiveAccessProcessorTest$AImplementation",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A");
    }

    @Test
    void testImplementationNameFromMapperConfig() throws IOException {
        @MapperConfig(implementationName = "<CLASS_NAME>I", implementationPackage = "prefix.<PACKAGE_NAME>")
        class B {

        }

        @Mapper(config = B.class)
        class A {

        }

        Index index = buildIndex(A.class, B.class, Mapper.class, MapperConfig.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "prefix.io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AI",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A");
    }

    @Test
    void testMapperWithUsesAttribute() throws IOException {
        class B {

        }

        @Mapper(uses = B.class)
        class A {

        }

        Index index = buildIndex(A.class, B.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$B");
    }

    @Test
    void testMapperWithUsesAttributeFromMapperConfig() throws IOException {
        class C {

        }

        class D {

        }

        @MapperConfig(uses = C.class)
        class B {

        }

        @Mapper(config = B.class, uses = D.class)
        class A {

        }

        Index index = buildIndex(A.class, B.class, C.class, D.class, Mapper.class, MapperConfig.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$C",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$D");
    }

    @Test
    void testMultipleMappers() throws IOException {
        @Mapper
        class A {

        }

        @Mapper
        class B {

        }

        Index index = buildIndex(A.class, B.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$BImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$B");
    }

    @Test
    void testMapperWithDecoratedWith() throws IOException {
        class C {

        }

        @Mapper
        @DecoratedWith(C.class)
        class A {

        }

        Index index = buildIndex(A.class, C.class, Mapper.class, MapperConfig.class,
                DecoratedWith.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$C");
    }

    @Test
    void testMapperWithSuperclass() throws IOException {
        class B {

        }

        @Mapper
        class A extends B {

        }

        Index index = buildIndex(B.class, A.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$B");
    }

    @Test
    void testMapperWithInterface() throws IOException {
        @Mapper
        class A implements MapperInterface {

        }

        Index index = buildIndex(MapperInterface.class, A.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$MapperInterface");
    }

    @Test
    void testMapperWithUsesThatHasSuperclass() throws IOException {
        class C {

        }

        class B extends C {

        }

        @Mapper(uses = B.class)
        class A {

        }

        Index index = buildIndex(A.class, B.class, C.class, Mapper.class);

        ReflectiveClassBuildItem reflectiveClassBuildItem = processor.registerMappers(new CombinedIndexBuildItem(index, index));

        assertReflectiveClassBuildItem(reflectiveClassBuildItem,
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$AImpl",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$A",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$B",
                "io.quarkiverse.mapstruct.deployment.ReflectiveAccessProcessorTest$C");
    }

    private void assertReflectiveClassBuildItem(ReflectiveClassBuildItem reflectiveClassBuildItem, String... classNames) {
        assertThat(reflectiveClassBuildItem.getClassNames().stream()
                .map(className -> className.replaceAll("\\$\\d+", "\\$"))
                .toList(),
                containsInAnyOrder(classNames));
    }

    private Index buildIndex(Class<?>... classes) throws IOException {
        assertTrue(classes.length > 0);

        Indexer indexer = new Indexer();

        for (Class<?> clazz : classes) {
            indexer.indexClass(clazz);
        }

        return indexer.complete();
    }
}
