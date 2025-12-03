package io.quarkiverse.mapstruct.it.basic;

import org.mapstruct.Mapper;

@Mapper(implementationName = "<CLASS_NAME>_")
public interface ServiceProviderMapper {
    EmailData mapToEmailData(Email email);
}
