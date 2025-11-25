package io.quarkiverse.mapstruct.it.basic;

import java.util.List;

public class Address {
    private String city;

    private String streetName;

    public LocalizationInfo localizationInfo;

    private List<Contact> contacts;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public class LocalizationInfo {
        private String locale;

        public LocalizationInfo2 localizationInfo2;

        public String getLocale() {
            return locale;
        }

        public void setLocale(String locale) {
            this.locale = locale;
        }

        public class LocalizationInfo2 {

        }
    }
}
