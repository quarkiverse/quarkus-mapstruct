package io.quarkiverse.mapstruct.base_model;

public class AddressMapperImpl implements AddressMapper {
    private MapperHelperImpl mapperHelper = new MapperHelperImpl();

    private DefaultEmailCreator defaultEmailCreator;

    @Override
    public void mapToData(Address contact) {
    }
}
