package ru.ifmo.rain.maksimov.bank;

public class RemotePerson implements Person {
    private final int passportId;
    private final String firstName;
    private final String lastName;

    public RemotePerson(int passportId, String firstName, String lastName) {
        this.passportId = passportId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public int getPassportId() {
        return passportId;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }
}
