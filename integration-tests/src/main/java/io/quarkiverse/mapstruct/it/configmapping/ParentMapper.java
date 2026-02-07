package io.quarkiverse.mapstruct.it.configmapping;

import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta-cdi")
public interface ParentMapper {
    ParentModel map(Parent config);

    ParentModel map(ParentBeanStyle config);
}
