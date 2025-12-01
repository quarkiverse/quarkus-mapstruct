package io.quarkiverse.mapstruct.base_model;

import org.mapstruct.Mapper;

@Mapper
public interface AddressMapper {
    void mapToData(Address address);
}
