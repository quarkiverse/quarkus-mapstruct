package io.quarkiverse.mapstruct.test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.LogRecord;

import io.quarkiverse.mapstruct.base_model.Address;
import io.quarkiverse.mapstruct.base_model.Contact;
import io.quarkiverse.mapstruct.base_model.AddressMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

class AnnotationDependentClassesDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage("io.quarkiverse.mapstruct.base_model")
                    .addClass(RecompilationDependenciesResource.class) //
            )
            .setLogRecordPredicate(r -> true);

    // mostly meant as smoke test to ensure the recompile-dependencies has any effect
    // more testing is done as Unit Tests in quarkus RecompilationDependenciesProcessorTest and in AnnotationDependentClassesProcessorTest
    @Test
    void testDependencyChangeTriggersRecompilationOfRecompilationTargets() {
        Map<String, Long> originalFileTimes = RestAssured.given().accept(ContentType.JSON)
                .get("/recompile-dependencies/test").then().extract().body()
                .as(Map.class);

        // just a file change to make quarkus hot reload on next rest call
        TEST.modifySourceFile(Contact.class, oldSource -> oldSource.replaceFirst(
                "name;",
                "name; public String name2;"));

        // ContactData -> AddressMapper recompile
        // but not AddressData
        // since not present in builditems

        // First check that both files have been recompiled
        RestAssured.given().accept(ContentType.JSON).get("/recompile-dependencies/test").then()
                .body(Address.class.getSimpleName(), is(originalFileTimes.get(Address.class.getSimpleName())))
                .body(Contact.class.getSimpleName(), greaterThan(originalFileTimes.get(Contact.class.getSimpleName())))
                .body(AddressMapper.class.getSimpleName(),
                        greaterThan(originalFileTimes.get(AddressMapper.class.getSimpleName())));

        // and just to be safe, check that this is also presented to user
        boolean found = false;
        for (LogRecord logRecord : TEST.getLogRecords()) {
            if (logRecord.getLoggerName().equals("io.quarkus.deployment.dev.RuntimeUpdatesProcessor")
                    && (logRecord.getParameters()[0].equals("AddressMapper.class, Contact.class")
                            || logRecord.getParameters()[0].equals("Contact.class, AddressMapper.class"))) {
                found = true;
            }
        }
        Assertions.assertTrue(found, "Did not find a log record from RuntimeUpdatesProcessor for AddressMapper class");
    }

    @ApplicationScoped
    @Path("/recompile-dependencies")
    public static class RecompilationDependenciesResource {

        @GET
        @Path("/test")
        @Produces(MediaType.WILDCARD)
        public String test() throws URISyntaxException, IOException {

            return """
                    {
                        "%s": %s,
                        "%s": %s,
                        "%s": %s
                    }
                    """.formatted(//
                    Address.class.getSimpleName(), fileTime(Address.class), //
                    Contact.class.getSimpleName(), fileTime(Contact.class), //
                    AddressMapper.class.getSimpleName(), fileTime(AddressMapper.class)//
            );
        }

        private long fileTime(Class<?> clazz) throws URISyntaxException, IOException {
            return Files.getLastModifiedTime(pathToClass(clazz))
                    .toMillis();
        }

        private static java.nio.file.Path pathToClass(Class<?> clazz) throws URISyntaxException {
            return java.nio.file.Path
                    .of(clazz.getClassLoader().getResource(ClassLoaderHelper.fromClassNameToResourceName(clazz.getName()))
                            .toURI());
        }
    }
}
