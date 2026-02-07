package io.quarkiverse.mapstruct.it.configmapping;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

@ApplicationScoped
@Path("mapstruct/configmapping")
public class ConfigMappingResource {

    @Inject
    ParentMapper mapper;

    private SmallRyeConfig buildConfig() {
        MapBackedConfigSource source = new MapBackedConfigSource("ConfigMappingResource",
                Map.of(
                        "parent.prop", "Value1",
                        "parent.childs[0].prop", "Value2",
                        "parent.childs[0].childs[0].prop", "Value3",
                        "parent.childs[0].childs[1].prop", "Value4",
                        "parent.childs[1].prop", "Value5",
                        "parent.childs[1].childs[0].prop", "Value6",
                        "parent.childs[1].childs[1].prop", "Value7")) {
        };

        return new SmallRyeConfigBuilder()
                .withSources(source)
                .withMapping(Parent.class)
                .withMapping(ParentBeanStyle.class)
                .build();
    }

    @Path("test")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ParentModel hello() {
        SmallRyeConfig smallRyeConfig = buildConfig();
        ParentModel model = mapper.map(smallRyeConfig.getConfigMapping(Parent.class));

        return model;
    }

    @Path("test-bean-style")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ParentModel helloBeanStyle() {
        SmallRyeConfig smallRyeConfig = buildConfig();
        ParentModel model = mapper.map(smallRyeConfig.getConfigMapping(ParentBeanStyle.class));

        return model;
    }
}
