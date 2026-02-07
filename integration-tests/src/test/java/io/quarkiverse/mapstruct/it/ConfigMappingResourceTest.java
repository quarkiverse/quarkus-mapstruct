package io.quarkiverse.mapstruct.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigMappingResourceTest {

    @Test
    void test() {
        given()
                .when().get("/mapstruct/configmapping/test")
                .then()
                .statusCode(200)
                .body("prop", is("Value1"))
                .body("childs[0].prop", is("Value2"))
                .body("childs[0].childs[0].prop", is("Value3"))
                .body("childs[0].childs[1].prop", is("Value4"))
                .body("childs[1].prop", is("Value5"))
                .body("childs[1].childs[0].prop", is("Value6"))
                .body("childs[1].childs[1].prop", is("Value7"));
    }

    @Test
    void testBeanStyle() {
        given()
                .when().get("/mapstruct/configmapping/test-bean-style")
                .then()
                .statusCode(200)
                .body("prop", is("Value1"))
                .body("childs[0].prop", is("Value2"))
                .body("childs[0].childs[0].prop", is("Value3"))
                .body("childs[0].childs[1].prop", is("Value4"))
                .body("childs[1].prop", is("Value5"))
                .body("childs[1].childs[0].prop", is("Value6"))
                .body("childs[1].childs[1].prop", is("Value7"));
    }
}
