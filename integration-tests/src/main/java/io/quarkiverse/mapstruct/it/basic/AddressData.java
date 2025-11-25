package io.quarkiverse.mapstruct.it.basic;

import java.util.List;

public class AddressData {
    private String city;

    public String streetName;

    private List<ContactData> contacts;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<ContactData> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactData> contacts) {
        this.contacts = contacts;
    }
}
