package io.quarkiverse.mapstruct.it.basic;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ContactMapper {
    ContactData mapToContact(Contact contact);
}
