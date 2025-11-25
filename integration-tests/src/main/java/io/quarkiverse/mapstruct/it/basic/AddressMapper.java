package io.quarkiverse.mapstruct.it.basic;

import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;

@MapperConfig(uses = { ContactMapper.class })
@Mapper(config = AddressMapper.class)
public interface AddressMapper {
    @Mapping(target = "streetName", ignore = true)
    AddressData mapToData(Address contact);
}
