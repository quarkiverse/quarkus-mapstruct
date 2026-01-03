package io.quarkiverse.mapstruct.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.mapstruct.base_model.Address;
import io.quarkiverse.mapstruct.base_model.Contact;
import io.quarkiverse.mapstruct.base_model.AddressMapper;
import io.quarkiverse.mapstruct.base_model.AddressMapperImpl;
import io.quarkiverse.mapstruct.base_model.DefaultEmailCreator;
import io.quarkiverse.mapstruct.base_model.Email;
import io.quarkiverse.mapstruct.base_model.MapperHelper;
import io.quarkiverse.mapstruct.base_model.MapperHelperImpl;
import io.quarkiverse.mapstruct.base_model.ModelBase;
import io.quarkiverse.mapstruct.base_model.PackagePrivateData;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.dev.RecompilationDependenciesBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.index.IndexWrapper;
import io.quarkus.deployment.index.PersistentClassIndex;
import org.mapstruct.Mapper;

class AnnotationDependentClassesProcessorTest {
    private AnnotationDependentClassesProcessor processor;

    @BeforeEach
    void setup() {
        processor = new AnnotationDependentClassesProcessor();
    }

    @AfterEach
    void tearDown() {
        RuntimeUpdatesProcessor.INSTANCE = null;
    }

    @Test
    void testNoAnnotations() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Collections.emptySet();
        Index emptyIndex = new Indexer().complete();
        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(emptyIndex, emptyIndex);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(0, RecompilationDependenciesBuildItems.size());
    }

    @Test
    void fullTest() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        Index index = buildIndex(Address.class, Mapper.class, Contact.class, AddressMapper.class, AddressMapperImpl.class,
                DefaultEmailCreator.class, Email.class, MapperHelper.class, MapperHelperImpl.class, ModelBase.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());

        RecompilationDependenciesBuildItem RecompilationDependenciesBuildItem = RecompilationDependenciesBuildItems.get(0);

        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItem
                .getClassToRecompilationTargets();
        assertEquals(14, dependencies.size());
        assertAffectedClasses(dependencies, ModelBase.class, AddressMapper.class);
        assertAffectedClasses(dependencies, DefaultEmailCreator.class, AddressMapper.class);
        assertAffectedClasses(dependencies, Address.class, AddressMapper.class);
        assertAffectedClasses(dependencies, Email.class, DefaultEmailCreator.class, AddressMapper.class);
        assertAffectedClasses(dependencies, Contact.class, AddressMapper.class);
        assertAffectedClasses(dependencies, MapperHelper.class, AddressMapper.class);
        assertAffectedClasses(dependencies, MapperHelperImpl.class, AddressMapper.class);
        assertAffectedClasses(dependencies, Address.LocalizationInfo.class, AddressMapper.class);
    }

    /**
     * Test that circular dependencies between annotated classes do not cause infinite loops.
     */
    @Test
    void testCyclicAPMarkedClasses() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        Index index = buildIndex(CyclicApMarked1.class, CyclicApMarked2.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(3, dependencies.size());
        assertAffectedClasses(dependencies, CyclicApMarked1.class, CyclicApMarked2.class);
        assertAffectedClasses(dependencies, CyclicApMarked2.class, CyclicApMarked1.class);
    }

    @Mapper
    class CyclicApMarked1 {
        private CyclicApMarked2 cyclicApMarked2;
    }

    @Mapper
    class CyclicApMarked2 {
        private CyclicApMarked1 cyclicApMarked1;
    }

    @Test
    void testAPMarkedAnnotationRecomputed() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        Index index = buildIndex(Address.class, AddressMapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(0, RecompilationDependenciesBuildItems.size());

        combinedIndexBuildItem = new CombinedIndexBuildItem(index,
                new IndexWrapper(index, Mapper.class.getClassLoader(), new PersistentClassIndex()));
        RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
    }

    @Test
    void testConfiguredAnnotationIsInRealityAClass() throws IOException {

        class D {
        }
        class C extends D {
        }
        class B extends C {
        }
        @Mapper
        record A(B b) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, Mapper.class);

        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(A.class.getName());
        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(0, RecompilationDependenciesBuildItems.size());
    }

    @interface APMarker {

    }

    @Test
    void testNonClassAnnotated() throws IOException {

        class A {
            @APMarker
            String field;

            @APMarker
            void method() {
            }
        }

        Index index = buildIndex(A.class, APMarker.class);

        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());
        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(0, RecompilationDependenciesBuildItems.size());
    }

    @Test
    void testInheritanceOfReferencedPublicType() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        class D {
        }
        class C extends D {
        }
        class B extends C {
        }
        @Mapper
        record A(B b) {
        }

        Index index = buildIndex(D.class, C.class, A.class, B.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(5, dependencies.size());
        assertAffectedClasses(dependencies, B.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, D.class, A.class);
    }

    @Test
    void testGenericParameterizedType() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        record D() {
        }
        record C() {
        }
        record B() {
        }

        @Mapper
        record A(List<B> bs, Map<C, List<D>> map) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(8, dependencies.size());
        assertAffectedClasses(dependencies, D.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testWildcardType() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        class D {
        }
        class C {
        }
        class B {
        }

        @Mapper
        record A(List<? extends C> bs, Map<? super D, B> map) {
        }

        Index index = buildIndex(D.class, C.class, B.class, A.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(7, dependencies.size());
        assertAffectedClasses(dependencies, D.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testTypeVariable() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        class B {
        }

        @Mapper
        class A {
            public <T extends B> void helper(T variable) {
            }
        }

        Index index = buildIndex(B.class, A.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(2, dependencies.size());
        assertAffectedClasses(dependencies, B.class, A.class);
    }

    @Test
    void testArrayType() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        class C {
        }
        class B {
        }

        @Mapper
        class A {
            public <T extends B> void helper(T[] array, C[] array2) {
            }
        }

        Index index = buildIndex(C.class, A.class, B.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();
        assertEquals(3, dependencies.size());
        assertFalse(dependencies.containsKey(DotName.createSimple(B[].class.getName())));
        assertFalse(dependencies.containsKey(DotName.createSimple(C[].class.getName())));
        assertAffectedClasses(dependencies, B.class, A.class);
        assertAffectedClasses(dependencies, C.class, A.class);
    }

    @Test
    void testPublicTypeCollectionVisibilityCheck() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());
        class F {
        }
        class E {
        }
        class D {
        }
        class C {
        }
        class B {
            private C c;
            D d;
            protected E e;
            public F f;
        }

        class B2 {
            private C help1() {
                return null;
            }

            D help2() {
                return null;
            }

            protected E help3() {
                return null;
            }

            public F help4() {
                return null;
            }
        }

        class B3 {
            private void help1(C c) {
            }

            void help2(D d) {
            }

            protected void help3(E e) {
            }

            public void help4(F f) {
            }
        }

        @Mapper
        class A {
            B b;

            PackagePrivateData packagePrivateData;
        }

        @Mapper
        class A2 {
            B2 b2;
        }

        @Mapper
        class A3 {
            B3 b3;
        }

        Index index = buildIndex(A.class, A2.class, A3.class, Mapper.class, B.class, B2.class, B3.class, C.class, D.class,
                E.class, F.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();

        // C is private, should not be included
        assertFalse(dependencies.containsKey(DotName.createSimple(C.class)));
        // Address and Contact are in different packages compared to the annoated classes, and should not be included
        assertFalse(dependencies.containsKey(DotName.createSimple(Address.class)));
        assertFalse(dependencies.containsKey(DotName.createSimple(Contact.class)));
        // F is public
        // E is protected, and can see package private
        // D is package private
        assertAffectedClasses(dependencies, F.class, A.class, A2.class, A3.class);
        assertAffectedClasses(dependencies, E.class, A.class, A2.class, A3.class);
        assertAffectedClasses(dependencies, D.class, A.class, A2.class, A3.class);
    }

    @Test
    void testReferencedTypeInheritanceNotEvaluated() throws IOException {
        AnnotationDependentClassesProcessor.ANNOTATIONS = Set.of(Mapper.class.getName());

        class D {
        }

        class B extends D implements TestInheritanceOfReferencedTypesC {
        }

        @Mapper
        class A {
            TestInheritanceOfReferencedTypesC c;

            D d;
        }

        Index index = buildIndex(TestInheritanceOfReferencedTypesC.class, A.class, B.class, Mapper.class);

        CombinedIndexBuildItem combinedIndexBuildItem = new CombinedIndexBuildItem(index, index);
        List<RecompilationDependenciesBuildItem> RecompilationDependenciesBuildItems = processor
                .discoverAnnotationDependentClasses(combinedIndexBuildItem);

        assertEquals(1, RecompilationDependenciesBuildItems.size());
        Map<DotName, Set<DotName>> dependencies = RecompilationDependenciesBuildItems.get(0)
                .getClassToRecompilationTargets();

        assertFalse(dependencies.containsKey(DotName.createSimple(B.class.getName())));
        assertAffectedClasses(dependencies, TestInheritanceOfReferencedTypesC.class, A.class);
        assertAffectedClasses(dependencies, D.class, A.class);
    }

    interface TestInheritanceOfReferencedTypesC {
    }

    private void assertAffectedClasses(Map<DotName, Set<DotName>> dependencies, Class<?> clazz,
            Class<?>... affectedClasses) {
        DotName className = DotName.createSimple(clazz.getName());
        assertTrue(dependencies.containsKey(className));
        assertEquals(affectedClasses.length, dependencies.get(className).size());
        for (Class<?> affectedClass : affectedClasses) {
            DotName affectedClassName = DotName.createSimple(affectedClass.getName());
            assertTrue(dependencies.get(className).contains(affectedClassName));
        }
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
