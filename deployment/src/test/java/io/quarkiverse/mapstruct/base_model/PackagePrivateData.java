package io.quarkiverse.mapstruct.base_model;

public class PackagePrivateData {
    Address address;
    protected Contact contact;

    Address help2Address() {
        return null;
    }

    protected Contact help3Contact() {
        return null;
    }

    void help2(Address d) {
    }

    protected void help3(Contact e) {
    }
}
