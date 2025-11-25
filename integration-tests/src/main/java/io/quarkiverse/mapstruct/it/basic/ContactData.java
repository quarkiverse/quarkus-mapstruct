package io.quarkiverse.mapstruct.it.basic;

public class ContactData {
    private String name;

    private EmailData email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmailData getEmail() {
        return email;
    }

    public void setEmail(EmailData email) {
        this.email = email;
    }
}
