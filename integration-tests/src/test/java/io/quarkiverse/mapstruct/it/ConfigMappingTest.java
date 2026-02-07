package io.quarkiverse.mapstruct.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ConfigMappingTest.class)
public class ConfigMappingTest implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "parent.prop", "Value1",
                "parent.childs[0].prop", "Value2",
                "parent.childs[0].childs[0].prop", "Value3",
                "parent.childs[0].childs[1].prop", "Value4",
                "parent.childs[1].prop", "Value5",
                "parent.childs[1].childs[0].prop", "Value6",
                "parent.childs[1].childs[1].prop", "Value7");
    }

    @Test
    public void test() {
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
    public void testBeanStyle() {
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
