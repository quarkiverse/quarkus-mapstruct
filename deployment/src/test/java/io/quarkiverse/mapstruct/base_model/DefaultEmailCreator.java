package io.quarkiverse.mapstruct.base_model;

import org.mapstruct.Mapper;

@Mapper
public interface DefaultEmailCreator {
    Email createDefault();
}
