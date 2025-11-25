package io.quarkiverse.mapstruct.it.basic;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.mapstruct.factory.Mappers;

@ApplicationScoped
@Path("mapstruct/basic")
public class QuarkusMapstructResource {

    @Path("test")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String hello() {

        Address address = new Address();
        address.setCity("Berlin");
        address.setContacts(List.of(new Contact()));

        return Mappers.getMapper(AddressMapper.class).getClass().getName() + "|"
                + Mappers.getMapperClass(AddressMapper.class).getName();
    }
}
